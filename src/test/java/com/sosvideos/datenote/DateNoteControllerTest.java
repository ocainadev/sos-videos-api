package com.sosvideos.datenote;

import com.sosvideos.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para DateNote:
 * - Admin: GET + PUT (upsert) por data
 * - Controle de acesso: sem token → 401, SUBSCRIBER → 403
 */
@DisplayName("DateNote — links por data")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DateNoteControllerTest extends AbstractIntegrationTest {

    private String adminToken;
    private String subscriberToken;

    private static final String DN_SUB_EMAIL = "dn.sub@sostest.com";
    private static final String DN_SUB_CPF   = "77788899900";
    private static final String TEST_DATE    = LocalDate.now().toString();

    @BeforeAll
    void setup() throws Exception {
        adminToken      = loginAndGetToken(adminEmail, adminPassword);
        subscriberToken = createSubscriberAndLogin(adminToken, "DN Assinante", DN_SUB_EMAIL, DN_SUB_CPF);
    }

    // ── GET (vazio por padrão) ────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Admin GET data sem nota → retorna content vazio")
    void adminGet_semNota_retornaVazio() throws Exception {
        mockMvc.perform(get("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(TEST_DATE))
                .andExpect(jsonPath("$.content").value(""));
    }

    // ── PUT (upsert) ──────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Admin PUT cria/atualiza nota com links — retorna 200")
    void adminPut_criaOuAtualiza_retorna200() throws Exception {
        String content = "Acesse os materiais em https://drive.google.com/exemplo\nDúvidas: https://wa.me/5511999999999";

        mockMvc.perform(put("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(TEST_DATE))
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    @Order(3)
    @DisplayName("Admin GET após PUT — retorna conteúdo salvo")
    void adminGet_aposUpsert_retornaConteudo() throws Exception {
        mockMvc.perform(get("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("drive.google.com")));
    }

    @Test
    @Order(4)
    @DisplayName("Admin PUT segunda vez — atualiza sem duplicar")
    void adminPut_segundaVez_atualiza() throws Exception {
        mockMvc.perform(put("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Novo conteúdo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Novo conteúdo"));

        // Garante que há apenas uma entrada para esta data
        mockMvc.perform(get("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Novo conteúdo"));
    }

    @Test
    @Order(5)
    @DisplayName("Admin PUT com content vazio — aceita e salva")
    void adminPut_contentVazio_aceita() throws Exception {
        mockMvc.perform(put("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(""));
    }

    // ── Controle de acesso ────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /admin/date-notes sem token → 401")
    void adminGet_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/date-notes/" + TEST_DATE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("PUT /admin/date-notes sem token → 401")
    void adminPut_semToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/admin/date-notes/" + TEST_DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "test"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("PUT /admin/date-notes como SUBSCRIBER → 403")
    void adminPut_comoSubscriber_retorna403() throws Exception {
        mockMvc.perform(put("/api/admin/date-notes/" + TEST_DATE)
                        .header("Authorization", bearer(subscriberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "test"))))
                .andExpect(status().isForbidden());
    }
}
