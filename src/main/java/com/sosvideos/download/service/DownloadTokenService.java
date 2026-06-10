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

@Slf4j
@Service
public class DownloadTokenService {

    private static final long TTL_SECONDS = 60;

    /** In-memory store — no persistence needed given the short TTL. */
    private final ConcurrentHashMap<String, DownloadTokenEntry> tokens = new ConcurrentHashMap<>();

    // ── Generation
    public String generateToken(Long videoId, Long userId, DownloadType type) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new DownloadTokenEntry(videoId, userId, type, Instant.now().plusSeconds(TTL_SECONDS)));
        log.debug("Download token issued: videoId={} userId={} type={}", videoId, userId, type);
        return token;
    }

    // ── Consumption
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

    // ── Cleanup
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
