package com.sosvideos.video.entity;

import com.sosvideos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String driveFileId;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Column(columnDefinition = "BYTEA")
    private byte[] thumbnailData;

    @Column(length = 50)
    private String thumbnailMimeType;

    @Column(nullable = false)
    private LocalDate publishDate;

    @Column(length = 100)
    private String mimeType;

    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime expiresAt;
}
