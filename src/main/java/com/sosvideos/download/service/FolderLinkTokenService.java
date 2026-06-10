package com.sosvideos.download.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FolderLinkTokenService {

    private static final long TTL_SECONDS = 300; // 5 minutos

    private record Entry(String folderId, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> tokens = new ConcurrentHashMap<>();

    // ── Geração
    public String generateToken(String folderId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Entry(folderId, Instant.now().plusSeconds(TTL_SECONDS)));
        log.debug("Folder link token issued: folderId={}", folderId);
        return token;
    }

    // ── Validação
    public Optional<String> validateToken(String token) {
        Entry entry = tokens.get(token);
        if (entry == null) {
            log.warn("Folder link token not found: {}", token);
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            log.warn("Folder link token expired: {}", token);
            return Optional.empty();
        }
        return Optional.of(entry.folderId());
    }

    // ── Limpeza
    @Scheduled(fixedDelay = 60_000)
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int before = tokens.size();
        tokens.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        int removed = before - tokens.size();
        if (removed > 0) log.debug("Purged {} expired folder link tokens", removed);
    }
}
