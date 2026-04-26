package com.sosvideos.tutorial.dto;

import java.time.LocalDateTime;

public record TutorialResponse(
        Long id,
        String title,
        String description,
        String contentType,
        String url,
        boolean active,
        int sortOrder,
        LocalDateTime createdAt
) {}
