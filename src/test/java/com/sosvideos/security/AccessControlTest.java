package com.sosvideos.security;

import com.sosvideos.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de controle de acesso por roles.
 *
 * Verifica que:
 * - Endpoints sem token retornam 401
 * - Endpoints admin-only retornam 403 para SUPPORT e SUBSCRIBER
 * - Endpoints exclusivos de ADMIN retornam 403 mesmo para SUPPORT
 * - Endpoint público (webhook) não exige token
 */
@DisplayName("Segurança — controle de acesso por roles")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessControlTest extends AbstractIntegrationTest {

    private String adminToken;
    private String supportToken;
    private String subscriberToken;

    private static final String AC_SUB_EMAIL  = "ac.sub@sostest.com";
    private static final String AC_SUB_CPF    = "33366699900";
    private static final String AC_SUP_EMAIL  = "ac.sup@sostest.com";
    private static final String AC_SUP_CPF    = "11166699900";

    @BeforeAll
    void setup() throws Exception {
        adminToken     = loginAndGetToken(adminEmail, adminPassword);
        supportToken   = createSupportAndLogin(adminToken, "AC Suporte", AC_SUP_EMAIL, AC_SUP_CPF);
        subscriberToken = createSubscriberAndLogin(adminToken, "AC Assinante", AC_SUB_EMAIL, AC_SUB_CPF);
    }

    // ── 401: sem token ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/subscribers sem token → 401")
    void adminSubscribers_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /subscriber/videos sem token → 401")
    void subscriberVideos_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/subscriber/videos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /subscriber/tutorials sem token → 401")
    void subscriberTutorials_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/subscriber/tutorials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /subscriber/term-status sem token → 401")
    void termStatus_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/subscriber/term-status"))
                .andExpect(status().isUnauthorized());
    }

    // ── 403: role errado em /api/admin/** ─────────────────────────────────────

    @Test
    @DisplayName("SUBSCRIBER em /admin/subscribers → 403")
    void adminSubscribers_comoSubscriber_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(subscriberToken)))
                .andExpect(status().isForbidden());
    }

    // ── 403: endpoints exclusivos de ADMIN rejeitam SUPPORT ──────────────────

    @Test
    @DisplayName("SUPPORT em GET /admin/videos → 403 (admin-only)")
    void adminVideos_comoSupporte_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/videos")
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPPORT em GET /admin/tutorials → 403 (admin-only)")
    void adminTutorials_comoSupporte_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/tutorials")
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPPORT em GET /admin/drive-folders → 403 (admin-only)")
    void adminDriveFolders_comoSupporte_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/drive-folders")
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPPORT em GET /admin/terms → 403 (admin-only)")
    void adminTerms_comoSupporte_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/terms")
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isForbidden());
    }

    // ── Endpoints que SUPPORT pode acessar (inclui gestão de assinantes) ─────

    @Test
    @DisplayName("SUPPORT em POST /admin/subscribers → 201 (permitido)")
    void createSubscriber_comoSupporte_retorna201() throws Exception {
        var req = java.util.Map.of("name", "Novo Assinante", "email", "novo.sub@sostest.com", "cpf", "44455566677");
        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", bearer(supportToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("SUPPORT em PATCH /admin/subscribers/{id}/status → 200 (permitido)")
    void toggleStatus_comoSupporte_retorna200() throws Exception {
        // Cria um assinante para depois alterar o status
        String subToken = createSubscriberAndLogin(adminToken, "Toggle Sub", "toggle.sub@sostest.com", "55566677788");
        // Busca o ID
        var listResult = mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(supportToken))
                        .param("search", "toggle.sub@sostest.com"))
                .andReturn();
        @SuppressWarnings("unchecked")
        var body = objectMapper.readValue(listResult.getResponse().getContentAsString(), java.util.Map.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) body.get("data");
        @SuppressWarnings("unchecked")
        var content = (java.util.List<java.util.Map<String, Object>>) data.get("content");
        Long userId = ((Number) content.get(0).get("userId")).longValue();

        mockMvc.perform(patch("/api/admin/subscribers/" + userId + "/status")
                        .header("Authorization", bearer(supportToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SUBSCRIBER em GET /admin/videos → 403")
    void adminVideos_comoSubscriber_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/videos")
                        .header("Authorization", bearer(subscriberToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPPORT em GET /admin/subscribers → 200 (permitido)")
    void adminSubscribers_comoSupporte_retorna200() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isOk());
    }

    // ── Endpoint público: webhook não exige token ─────────────────────────────

    @Test
    @DisplayName("POST /webhooks/kiwify sem token → não é 403 (endpoint público, Spring Security não bloqueia)")
    void webhookKiwify_semToken_naoRetorna403() throws Exception {
        // O webhook pode retornar 401 por assinatura inválida (lógica de negócio),
        // mas o Spring Security não deve bloquear com 403 — o path é público no SecurityConfig.
        // O que NÃO pode acontecer é 403 por ausência de JWT.
        var result = mockMvc.perform(post("/api/webhooks/kiwify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        assertNotEquals(403, result.getResponse().getStatus(),
                "Endpoint público de webhook não deve ser bloqueado com 403 pelo Spring Security");
    }

    // ── Endpoint público: login ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login sem token → não é 403 (endpoint público)")
    void login_semToken_naoRetorna403() throws Exception {
        Map<String, String> req = Map.of("email", "x@x.com", "password", "senha");
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        assertNotEquals(403, result.getResponse().getStatus(),
                "Endpoint público de login nunca deve retornar 403");
    }
}
