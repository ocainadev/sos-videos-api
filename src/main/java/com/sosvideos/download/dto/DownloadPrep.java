package com.sosvideos.download.dto;

/**
 * Internal record returned by DownloadService.prepareDownload().
 * Contains only what the controller needs to stream the file.
 * driveFileId is NEVER exposed to the client.
 */
public record DownloadPrep(
        String driveFileId,
        String title,
        String mimeType,
        Long fileSize
) {}
