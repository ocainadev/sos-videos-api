package com.sosvideos.download.service;

import com.sosvideos.download.dto.DownloadTokenEntry;
import com.sosvideos.download.enums.DownloadType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues single-use, short-lived download tokens.
 *
 * <p>The client (frontend) first requests a token via an authenticated endpoint,
 * then redirects the browser to the public download URL — no JWT or blob required.
 * The browser receives the file natively, which is the only reliable approach on
 * mobile (avoids loading the entire file into JS/browser memory).</p>
 *
 * <ul>
 *   <li>Tokens expire after {@value TTL_SECONDS} seconds.</li>
 *   <li>Each token is consumed on first use (single-use).</li>
 *   <li>Expired tokens are purged every minute by a scheduler.</li>
 * </ul>
 */
@Slf4j
@Service
public class DownloadTokenService {

    private static final long TTL_SECONDS = 60;

    /** In-memory store — no persistence needed given the short TTL. */
    private final ConcurrentHashMap<String, DownloadTokenEntry> tokens = new ConcurrentHashMap<>();

    // ── Generation ──────────────────────────────────────────────────────────────

    /**
     * Generates a new one-time download token for the given video and user.
     *
     * @return a 32-character hex token (UUID without hyphens)
     */
    public String generateToken(Long videoId, Long userId, DownloadType type) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new DownloadTokenEntry(videoId, userId, type, Instant.now().plusSeconds(TTL_SECONDS)));
        log.debug("Download token issued: videoId={} userId={} type={}", videoId, userId, type);
        return token;
    }

    // ── Consumption ─────────────────────────────────────────────────────────────

    /**
     * Validates and atomically removes the token (single-use).
     *
     * @return the entry, or empty if the token is unknown or already expired
     */
    public Optional<DownloadTokenEntry> validateAndConsume(String token) {
        DownloadTokenEntry entry = tokens.remove(token);
        if (entry == null) {
            log.warn("Download token not found or already used: {}", token);
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            log.warn("Download token expired: {}", token);
            return Optional.empty();
        }
        log.debug("Download token consumed: videoId={} userId={}", entry.videoId(), entry.userId());
        return Optional.of(entry);
    }

    // ── Cleanup ─────────────────────────────────────────────────────────────────

    /** Removes expired tokens from memory every 60 seconds. */
    @Scheduled(fixedDelay = 60_000)
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int before = tokens.size();
        tokens.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        int removed = before - tokens.size();
        if (removed > 0) {
            log.debug("Purged {} expired download tokens", removed);
        }
    }
}
