package com.sosvideos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosvideos.drive.scheduler.DriveFolderCleanupScheduler;
import com.sosvideos.drive.service.GoogleDriveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para todos os testes de integração.
 * <p>
 * Usa o perfil "test" que aponta para o banco PostgreSQL local (sosvideos_test)
 * com {@code create-drop}, garantindo schema limpo a cada inicialização do contexto.
 * <p>
 * GoogleDriveService e DriveFolderCleanupScheduler são mockados para evitar I/O externo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // Evita chamadas reais ao Google Drive
    @MockitoBean
    GoogleDriveService googleDriveService;

    // Evita que o scheduler de limpeza rode durante os testes
    @MockitoBean
    DriveFolderCleanupScheduler driveFolderCleanupScheduler;

    @Value("${app.admin.email}")
    protected String adminEmail;

    @Value("${app.admin.password}")
    protected String adminPassword;

    // ── Helpers ──────────────────────────────────────────────────────────────

    protected String loginAndGetToken(String email, String password) throws Exception {
        Map<String, String> payload = Map.of("email", email, "password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        return (String) data.get("token");
    }

    /**
     * Cria um assinante via API de admin e retorna o token JWT dele.
     * Senha inicial = 6 primeiros dígitos do CPF.
     */
    protected String createSubscriberAndLogin(String adminToken, String name,
                                               String email, String cpf) throws Exception {
        Map<String, Object> req = Map.of("name", name, "email", email, "cpf", cpf);
        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        String digits = cpf.replaceAll("\\D", "");
        return loginAndGetToken(email, digits.substring(0, Math.min(6, digits.length())));
    }

    /**
     * Cria um usuário SUPPORT via API de admin e retorna o token JWT dele.
     * Senha inicial = 6 primeiros dígitos do CPF.
     */
    protected String createSupportAndLogin(String adminToken, String name,
                                            String email, String cpf) throws Exception {
        Map<String, Object> req = Map.of(
                "name", name, "email", email, "cpf", cpf, "role", "SUPPORT");
        mockMvc.perform(post("/api/admin/subscribers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        String digits = cpf.replaceAll("\\D", "");
        return loginAndGetToken(email, digits.substring(0, Math.min(6, digits.length())));
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
