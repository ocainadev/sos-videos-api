package com.sosvideos.terms.dto;

import java.time.LocalDateTime;

public record TermResponse(
        Long id,
        String title,
        String content,
        String version,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
