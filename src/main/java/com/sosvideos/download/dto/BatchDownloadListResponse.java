package com.sosvideos.download.dto;

import java.time.LocalDate;
import java.util.List;

public record BatchDownloadListResponse(
        LocalDate date,
        int totalVideos,
        List<VideoDownloadItem> videos
) {
    public record VideoDownloadItem(
            Long id,
            String title,
            String mimeType,
            Long fileSize
    ) {}
}
