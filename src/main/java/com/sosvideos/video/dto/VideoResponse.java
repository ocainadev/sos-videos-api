package com.sosvideos.video.dto;

import java.time.LocalDate;

public record VideoResponse(
        Long id,
        String title,
        LocalDate publishDate,
        String mimeType,
        Long fileSize,
        boolean alreadyDownloaded
) {}
