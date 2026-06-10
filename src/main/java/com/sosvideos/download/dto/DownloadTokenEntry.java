package com.sosvideos.download.dto;

import com.sosvideos.download.enums.DownloadType;

import java.time.Instant;

public record DownloadTokenEntry(
        Long videoId,
        Long userId,
        DownloadType type,
        Instant expiresAt
) {}
