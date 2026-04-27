package com.sosvideos.auth;

import com.sosvideos.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para AuthController:
 * login, /me e troca de senha.
 */
@DisplayName("Auth — login, /me e change-password")
class AuthControllerTest extends AbstractIntegrationTest {

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    @DisplayName("Login com credenciais válidas retorna 200 e token JWT")
    void loginComCredenciaisValidas_retornaTokenJwt() throws Exception {
        Map<String, String> req = Map.of("email", adminEmail, "password", adminPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.email").value(adminEmail));
    }

    @Test
    @DisplayName("Login com senha errada retorna 401")
    void loginComSenhaErrada_retorna401() throws Exception {
        Map<String, String> req = Map.of("email", adminEmail, "password", "senha-errada");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login com email desconhecido retorna 401")
    void loginComEmailDesconhecido_retorna401() throws Exception {
        Map<String, String> req = Map.of("email", "naoexiste@test.com", "password", "qualquer");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login sem email retorna 400")
    void loginSemEmail_retorna400() throws Exception {
        Map<String, String> req = Map.of("email", "", "password", adminPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login com email em formato inválido retorna 400")
    void loginComEmailInvalido_retorna400() throws Exception {
        Map<String, String> req = Map.of("email", "nao-e-um-email", "password", adminPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me com token válido retorna dados do usuário")
    void getMeComTokenValido_retornaDadosDoUsuario() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(adminEmail))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /me sem token retorna 401")
    void getMeSemToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /me com token malformado retorna 401")
    void getMeComTokenMalformado_retorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/auth/change-password ────────────────────────────────────────

    @Test
    @DisplayName("Troca de senha com dados válidos retorna 200")
    void trocaSenhaComDadosValidos_retorna200() throws Exception {
        // Cria usuário específico para este teste (evita efeito colateral no admin)
        String adminToken = loginAndGetToken(adminEmail, adminPassword);
        String userEmail = "changepass@sostest.com";
        String cpf = "11100011100";
        String initialPassword = cpf.substring(0, 6); // 6 primeiros dígitos

        createSubscriberAndLogin(adminToken, "Troca Senha User", userEmail, cpf);

        Map<String, String> req = Map.of(
                "currentPassword", initialPassword,
                "newPassword", "novaSenha@123"
        );

        String userToken = loginAndGetToken(userEmail, initialPassword);

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Troca de senha com senha atual errada retorna 4xx")
    void trocaSenhaComSenhaAtualErrada_retornaErro() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);

        Map<String, String> req = Map.of(
                "currentPassword", "senhaErrada@123",
                "newPassword", "novaSenha@123"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Troca de senha com nova senha muito curta retorna 400")
    void trocaSenhaComNovaSenhaCurta_retorna400() throws Exception {
        String token = loginAndGetToken(adminEmail, adminPassword);

        Map<String, String> req = Map.of(
                "currentPassword", adminPassword,
                "newPassword", "123"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Troca de senha sem token retorna 401")
    void trocaSenhaSemToken_retorna401() throws Exception {
        Map<String, String> req = Map.of(
                "currentPassword", adminPassword,
                "newPassword", "novaSenha@123"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
