package com.sosvideos.kiwify.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.config.AppProperties;
import com.sosvideos.kiwify.service.KiwifyWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Recebimento de webhooks externos")
public class KiwifyWebhookController {

    private final KiwifyWebhookService kiwifyWebhookService;
    private final AppProperties appProperties;

    /** Maximum accepted payload size: 64 KB. Protects against memory exhaustion attacks. */
    private static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    @PostMapping("/kiwify")
    @Operation(summary = "Recebe eventos da Kiwify (compra, renovação, cancelamento, reembolso)")
    public ResponseEntity<ApiResponse<Void>> receiveKiwify(
            @RequestBody String payload,
            HttpServletRequest request) {

        if (payload != null && payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            log.warn("Kiwify webhook rejected: payload too large ({} bytes), IP={}",
                    payload.length(), request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error("Payload excede o tamanho máximo permitido."));
        }

        if (!isSignatureValid(payload, request)) {
            log.warn("Kiwify webhook rejected: invalid signature from IP={}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Assinatura inválida."));
        }

        log.info("Kiwify webhook received, payload length={}", payload.length());
        kiwifyWebhookService.process(payload);
        return ResponseEntity.ok(ApiResponse.ok("Evento recebido."));
    }

    /**
     * Valida o token do webhook da Kiwify.
     *
     * A Kiwify envia o segredo como query param ?token=... na URL.
     * O HTTPS garante que a query string é encriptada em trânsito.
     * A comparação usa MessageDigest.isEqual (constant-time) para evitar timing attacks.
     *
     * SECURITY: secret ausente → rejeita sempre (fail-closed).
     */
    private boolean isSignatureValid(String payload, HttpServletRequest request) {
        String secret = appProperties.getKiwify().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.error("KIWIFY_WEBHOOK_SECRET não configurado — todas as requisições serão rejeitadas.");
            return false;
        }

        String receivedToken = request.getParameter("token");
        if (receivedToken == null || receivedToken.isBlank()) {
            log.warn("Kiwify webhook sem token na URL, IP={}", request.getRemoteAddr());
            return false;
        }

        // Comparação em tempo constante — evita timing attack
        return MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                receivedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
