package com.sosvideos.download;

import com.sosvideos.AbstractIntegrationTest;
import com.sosvideos.video.entity.Video;
import com.sosvideos.video.repository.VideoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * Testes de integração para o fluxo de download baseado em token:
 * <ol>
 *   <li>POST /api/videos/{id}/download/token → gera token (JWT obrigatório)</li>
 *   <li>GET  /api/download/{token}           → faz streaming (público, token = auth)</li>
 * </ol>
 *
 * O GoogleDriveService é mockado pela base — nenhum I/O externo.
 * Vídeos são persistidos diretamente via VideoRepository.
 */
@DisplayName("Download — geração de token e streaming público")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DownloadControllerTest extends AbstractIntegrationTest {

    @Autowired
    private VideoRepository videoRepository;

    private String adminToken;
    private Long videoId;

    private static final String FAKE_CONTENT = "fake-video-bytes";

    @BeforeAll
    void setup() throws Exception {
        adminToken = loginAndGetToken(adminEmail, adminPassword);

        // Persiste um vídeo de teste diretamente no banco (uma única vez)
        Video video = videoRepository.save(Video.builder()
                .title("Vídeo de Teste Download")
                .driveFileId("drive-file-id-test-123")
                .publishDate(LocalDate.now())
                .mimeType("video/mp4")
                .fileSize(1024L)
                .active(true)
                .build());
        videoId = video.getId();
    }

    /**
     * @MockitoBean reseta os mocks após CADA teste.
     * Por isso o doAnswer precisa ser reconfigurado antes de cada método.
     */
    @BeforeEach
    void setupMock() throws Exception {
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            try {
                out.write(FAKE_CONTENT.getBytes());
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }).when(googleDriveService).streamFile(eq("drive-file-id-test-123"), any(OutputStream.class));
    }

    // ── POST /api/videos/{id}/download/token ─────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Admin gera token para vídeo existente — retorna 200 com token")
    void generateToken_comoAdmin_retorna200ComToken() throws Exception {
        String response = mockMvc.perform(post("/api/videos/" + videoId + "/download/token")
                        .header("Authorization", bearer(adminToken))
                        .param("type", "SINGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // token deve ter 32 caracteres (UUID sem hífens)
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String token = (String) data.get("token");
        assertThat(token).hasSize(32);
    }

    @Test
    @Order(2)
    @DisplayName("Gerar token sem JWT — retorna 401")
    void generateToken_semJwt_retorna401() throws Exception {
        mockMvc.perform(post("/api/videos/" + videoId + "/download/token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("Gerar token para vídeo inexistente — retorna 4xx")
    void generateToken_videoInexistente_retorna4xx() throws Exception {
        mockMvc.perform(post("/api/videos/999999/download/token")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().is4xxClientError());
    }

    // ── GET /api/download/{token} ─────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Download com token válido — retorna 200 e faz streaming do arquivo")
    void downloadByToken_tokenValido_retorna200EStream() throws Exception {
        // 1. Obtém token
        String token = obtainToken(adminToken, videoId, "SINGLE");

        // 2. StreamingResponseBody é executado de forma assíncrona no Spring MVC.
        //    Primeiro dispara a requisição e aguarda o async iniciar...
        var asyncResult = mockMvc.perform(get("/api/download/" + token))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 3. ...depois processa o dispatch assíncrono e valida o resultado completo.
        byte[] body = mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("filename")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(body)).isEqualTo(FAKE_CONTENT);
    }

    @Test
    @Order(5)
    @DisplayName("Download com token inválido — retorna 410 Gone")
    void downloadByToken_tokenInvalido_retorna410() throws Exception {
        mockMvc.perform(get("/api/download/tokencompleto_invalido_aqui0000"))
                .andExpect(status().isGone());
    }

    @Test
    @Order(6)
    @DisplayName("Token é de uso único — segunda tentativa retorna 410")
    void downloadByToken_usoUnico_segundaTentativaRetorna410() throws Exception {
        String token = obtainToken(adminToken, videoId, "SINGLE");

        // Primeira requisição consome o token
        mockMvc.perform(get("/api/download/" + token))
                .andExpect(status().isOk());

        // Segunda requisição deve ser rejeitada
        mockMvc.perform(get("/api/download/" + token))
                .andExpect(status().isGone());
    }

    @Test
    @Order(7)
    @DisplayName("Token BATCH gerado com sucesso — streaming retorna 200")
    void generateToken_tipoBatch_retorna200() throws Exception {
        String token = obtainToken(adminToken, videoId, "BATCH");

        var asyncResult = mockMvc.perform(get("/api/download/" + token))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    // ── Controle de acesso (token generation) ────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Endpoint público GET /api/download/{token} não bloqueia com 403 para token inválido")
    void publicEndpoint_naoBloqueia403_paraTokenInvalido() throws Exception {
        // O Spring Security não deve retornar 403 (o path é público)
        // A resposta correta é 410 (lógica de negócio, não segurança)
        var result = mockMvc.perform(get("/api/download/qualquer-token-aqui"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String obtainToken(String jwt, Long vidId, String type) throws Exception {
        String response = mockMvc.perform(post("/api/videos/" + vidId + "/download/token")
                        .header("Authorization", bearer(jwt))
                        .param("type", type))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return (String) data.get("token");
    }
}
