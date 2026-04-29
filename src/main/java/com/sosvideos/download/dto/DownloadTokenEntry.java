package com.sosvideos.download.dto;

import com.sosvideos.download.enums.DownloadType;

import java.time.Instant;

/**
 * Immutable entry stored in the short-lived token map.
 * Carries all the data needed to perform the actual download
 * when the one-time token is consumed by the public endpoint.
 */
public record DownloadTokenEntry(
        Long videoId,
        Long userId,
        DownloadType type,
        Instant expiresAt
) {}
