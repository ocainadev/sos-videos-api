package com.sosvideos.download;

import com.sosvideos.AbstractIntegrationTest;
import com.sosvideos.video.entity.Video;
import com.sosvideos.video.repository.VideoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o fluxo de download baseado em token:
 * <ol>
 *   <li>POST /api/videos/{id}/download/token → gera token (JWT obrigatório)</li>
 *   <li>GET  /api/download/{token}           → valida token e redireciona (302) para Google Drive</li>
 * </ol>
 *
 * O GoogleDriveService é mockado pela base — nenhum I/O externo.
 * Vídeos são persistidos diretamente via VideoRepository.
 */
@DisplayName("Download — geração de token e redirect para Google Drive")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DownloadControllerTest extends AbstractIntegrationTest {

    @Autowired
    private VideoRepository videoRepository;

    private String adminToken;
    private Long videoId;

    private static final String FAKE_DRIVE_URL =
            "https://www.googleapis.com/drive/v3/files/drive-file-id-test-123?alt=media&access_token=fake";

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
     * Reconfigura buildTempDownloadUrl() antes de cada método.
     */
    @BeforeEach
    void setupMock() throws Exception {
        when(googleDriveService.buildTempDownloadUrl(anyString()))
                .thenReturn(FAKE_DRIVE_URL);
    }

    // ── POST /api/videos/{id}/download/token ─────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Admin gera token para vídeo existente — retorna 200 com token de 32 chars")
    void generateToken_comoAdmin_retorna200ComToken() throws Exception {
        String response = mockMvc.perform(post("/api/videos/" + videoId + "/download/token")
                        .header("Authorization", bearer(adminToken))
                        .param("type", "SINGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String token = (String) data.get("token");
        assertThat(token).hasSize(32); // UUID sem hífens = 32 hex chars
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
    @DisplayName("Token válido — retorna 302 com Location apontando para o Google Drive")
    void downloadByToken_tokenValido_retorna302ComLocationDoDrive() throws Exception {
        String token = obtainToken(adminToken, videoId, "SINGLE");

        mockMvc.perform(get("/api/download/" + token))
                .andExpect(status().isFound())                          // HTTP 302
                .andExpect(header().string("Location", FAKE_DRIVE_URL)); // redirect para o Drive
    }

    @Test
    @Order(5)
    @DisplayName("Token inválido — retorna 410 Gone")
    void downloadByToken_tokenInvalido_retorna410() throws Exception {
        mockMvc.perform(get("/api/download/tokencompleto_invalido_aqui0000"))
                .andExpect(status().isGone());
    }

    @Test
    @Order(6)
    @DisplayName("Token é de uso único — segunda tentativa retorna 410")
    void downloadByToken_usoUnico_segundaTentativaRetorna410() throws Exception {
        String token = obtainToken(adminToken, videoId, "SINGLE");

        // Primeira requisição consome o token (302 redirect)
        mockMvc.perform(get("/api/download/" + token))
                .andExpect(status().isFound());

        // Segunda requisição deve ser rejeitada
        mockMvc.perform(get("/api/download/" + token))
                .andExpect(status().isGone());
    }

    @Test
    @Order(7)
    @DisplayName("Token BATCH — retorna 302 com Location do Drive")
    void generateToken_tipoBatch_retorna302() throws Exception {
        String token = obtainToken(adminToken, videoId, "BATCH");

        mockMvc.perform(get("/api/download/" + token))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("googleapis.com")));
    }

    // ── Controle de acesso ────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Endpoint público não bloqueia com 401 ou 403 para token inválido")
    void publicEndpoint_naoBloqueia401ou403_paraTokenInvalido() throws Exception {
        // Spring Security não deve bloquear o path (é público).
        // A resposta correta é 410 (lógica de negócio), nunca 401/403.
        var result = mockMvc.perform(get("/api/download/qualquer-token-aqui"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
        assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
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
