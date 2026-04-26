package com.sosvideos.drive.entity;

import com.sosvideos.common.BaseEntity;
import com.sosvideos.drive.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drive_folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveFolder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private LocalDate publishDate;

    @Column(nullable = false, length = 255)
    private String driveFolderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.PENDING;

    private LocalDateTime lastSyncedAt;
}
