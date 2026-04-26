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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Recebimento de webhooks externos")
public class KiwifyWebhookController {

    private final KiwifyWebhookService kiwifyWebhookService;
    private final AppProperties appProperties;

    @PostMapping("/kiwify")
    @Operation(summary = "Recebe eventos da Kiwify (compra, renovação, cancelamento, reembolso)")
    public ResponseEntity<ApiResponse<Void>> receiveKiwify(
            @RequestBody String payload,
            HttpServletRequest request) {

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
     * Validates the Kiwify webhook signature.
     * Kiwify sends an HMAC-SHA1 of the raw body in the "X-Kiwify-Signature" header,
     * using the webhook secret as the key.
     *
     * <p>SECURITY: if the secret is not configured the request is always rejected —
     * failing open would allow unauthenticated subscription manipulation.</p>
     */
    private boolean isSignatureValid(String payload, HttpServletRequest request) {
        String secret = appProperties.getKiwify().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.error("KIWIFY_WEBHOOK_SECRET not configured — all webhook requests will be rejected. "
                    + "Set the environment variable to enable webhook processing.");
            return false;
        }

        // HMAC-SHA1 header (Kiwify standard)
        String sigHeader = request.getHeader("X-Kiwify-Signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            log.warn("Kiwify webhook received with no X-Kiwify-Signature header from IP={}",
                    request.getRemoteAddr());
            return false;
        }

        return verifyHmacSha1(payload, secret, sigHeader.trim());
    }

    private boolean verifyHmacSha1(String payload, String secret, String expectedHex) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().parseHex(expectedHex);
            return MessageDigest.isEqual(computed, expected);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }
}
