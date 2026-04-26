package com.sosvideos.download.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Returned by GET /api/videos/date/{date}/download-all.
 * The frontend iterates over 'videos' and calls GET /api/videos/{id}/download
 * for each one sequentially — no ZIP is generated server-side.
 */
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
