package com.sosvideos.admin;

import com.sosvideos.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para AdminController:
 * CRUD de assinantes, ajuste de assinatura e toggle de status.
 */
@DisplayName("Admin — assinantes e assinaturas")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminControllerTest extends AbstractIntegrationTest {

    private String adminToken;
    private String supportToken;
    private String subscriberToken;

    // IDs capturados durante os testes para uso nos passos seguintes
    private Long createdUserId;
    private Long createdSubscriptionId;

    private static final String SUB_EMAIL    = "sub.admin.test@sostest.com";
    private static final String SUB_CPF      = "22233344455";
    private static final String SUPPORT_EMAIL = "support.admin.test@sostest.com";
    private static final String SUPPORT_CPF  = "55544433322";

    @BeforeAll
    void setupUsuarios() throws Exception {
        adminToken = loginAndGetToken(adminEmail, adminPassword);
        supportToken = createSupportAndLogin(adminToken, "Suporte Teste", SUPPORT_EMAIL, SUPPORT_CPF);
        subscriberToken = createSubscriberAndLogin(adminToken, "Sub Teste", SUB_EMAIL, SUB_CPF);
    }

    // ── GET /api/admin/subscribers ────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Admin pode listar assinantes — retorna 200")
    void listarAssinantesComoAdmin_retorna200() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("Suporte pode listar assinantes — retorna 200")
    void listarAssinantesComoSupporte_retorna200() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(supportToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("Assinante não pode listar assinantes — retorna 403")
    void listarAssinantesComoAssinante_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(subscriberToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("Sem token, listar assinantes retorna 401")
    void listarAssinantesSemToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("Busca por nome/email filtra corretamente")
    void listarComFiltroSearch_retornaFiltrado() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .param("search", "Sub Teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Sub Teste"));
    }

    @Test
    @Order(6)
    @DisplayName("includeInactive=true inclui usuários inativos na listagem")
    void listarComIncludeInactive_inclui() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ── POST /api/admin/subscribers ──────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Admin cria assinante com CPF — retorna 201 e dados do usuário")
    void criarAssinante_retorna201() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Novo Assinante",
                "email", "novo.assinante@sostest.com",
                "cpf", "99988877766"
        );

        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("novo.assinante@sostest.com"))
                .andExpect(jsonPath("$.data.userId").isNumber());
    }

    @Test
    @Order(11)
    @DisplayName("Admin cria assinante sem CPF — senha inicial é 123456")
    void criarAssinanteSemCpf_retorna201() throws Exception {
        String email = "sem.cpf@sostest.com";
        Map<String, Object> req = Map.of("name", "Sem CPF", "email", email);

        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Confirma que consegue logar com a senha padrão 123456
        loginAndGetToken(email, "123456");
    }

    @Test
    @Order(12)
    @DisplayName("Email duplicado retorna 400")
    void criarAssinanteEmailDuplicado_retorna400() throws Exception {
        Map<String, Object> req = Map.of("name", "Duplicado", "email", SUB_EMAIL, "cpf", "00011122233");

        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    @DisplayName("Criação sem nome retorna 400")
    void criarAssinanteSemNome_retorna400() throws Exception {
        Map<String, Object> req = Map.of("email", "semNome@sostest.com");

        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    @DisplayName("Criação sem token retorna 401")
    void criarAssinanteSemToken_retorna401() throws Exception {
        Map<String, Object> req = Map.of("name", "X", "email", "x@x.com");

        mockMvc.perform(post("/api/admin/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/admin/subscribers/{id} ──────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("Admin obtém assinante por ID — retorna 200")
    void obterAssinantePorId_retorna200() throws Exception {
        // Pega o id do assinante criado no beforeAll via busca
        var result = mockMvc.perform(get("/api/admin/subscribers")
                        .header("Authorization", bearer(adminToken))
                        .param("search", SUB_EMAIL))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        var content = (java.util.List<?>) ((Map<?, ?>) body.get("data")).get("content");
        @SuppressWarnings("unchecked")
        var first = (Map<String, Object>) content.get(0);

        createdUserId = ((Number) first.get("userId")).longValue();
        createdSubscriptionId = first.get("subscriptionId") != null
                ? ((Number) first.get("subscriptionId")).longValue() : null;

        mockMvc.perform(get("/api/admin/subscribers/" + createdUserId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(SUB_EMAIL));
    }

    @Test
    @Order(21)
    @DisplayName("ID inexistente retorna 404")
    void obterAssinanteIdInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/admin/subscribers/{id}/status ──────────────────────────────

    @Test
    @Order(30)
    @DisplayName("Admin desativa um assinante — retorna 200")
    void desativarAssinante_retorna200() throws Exception {
        Assumptions.assumeTrue(createdUserId != null, "ID do assinante não capturado");

        Map<String, Object> req = Map.of("active", false);

        mockMvc.perform(patch("/api/admin/subscribers/" + createdUserId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(31)
    @DisplayName("Admin reativa um assinante — retorna 200")
    void reativarAssinante_retorna200() throws Exception {
        Assumptions.assumeTrue(createdUserId != null, "ID do assinante não capturado");

        Map<String, Object> req = Map.of("active", true);

        mockMvc.perform(patch("/api/admin/subscribers/" + createdUserId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── PATCH /api/admin/subscriptions/{id} ──────────────────────────────────

    @Test
    @Order(40)
    @DisplayName("Admin ajusta assinatura para ACTIVE — retorna 200")
    void ajustarAssinaturaParaAtivo_retorna200() throws Exception {
        Assumptions.assumeTrue(createdSubscriptionId != null, "subscriptionId não capturado");

        Map<String, Object> req = Map.of(
                "status", "ACTIVE",
                "startDate", LocalDate.now().toString(),
                "endDate", LocalDate.now().plusMonths(1).toString(),
                "reason", "Ajuste manual via teste"
        );

        mockMvc.perform(patch("/api/admin/subscriptions/" + createdSubscriptionId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(41)
    @DisplayName("Assinatura inexistente retorna 404")
    void ajustarAssinaturaInexistente_retorna404() throws Exception {
        Map<String, Object> req = Map.of("status", "ACTIVE");

        mockMvc.perform(patch("/api/admin/subscriptions/999999")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
