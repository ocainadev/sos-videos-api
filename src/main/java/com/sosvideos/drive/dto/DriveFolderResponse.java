package com.sosvideos.drive.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DriveFolderResponse(
        Long id,
        LocalDate publishDate,
        String driveFolderId,
        String syncStatus,
        LocalDateTime lastSyncedAt,
        LocalDateTime createdAt,
        Integer syncedVideoCount
) {}
