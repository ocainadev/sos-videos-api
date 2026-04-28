package com.sosvideos.tutorial.dto;

import java.time.LocalDateTime;

public record TutorialResponse(
        Long id,
        String title,
        String description,
        String contentType,
        String url,
        String thumbnailUrl,
        boolean active,
        int sortOrder,
        LocalDateTime createdAt
) {}
