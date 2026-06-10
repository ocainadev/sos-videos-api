package com.sosvideos.download.dto;

public record DownloadPrep(
        String driveFileId,
        String title,
        String mimeType,
        Long fileSize
) {}
