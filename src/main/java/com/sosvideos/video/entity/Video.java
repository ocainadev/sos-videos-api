package com.sosvideos.video.entity;

import com.sosvideos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_videos_publish_date", columnList = "publishDate"),
        @Index(name = "idx_videos_active_publish_date", columnList = "active, publishDate")
})
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

    @Column(nullable = false)
    private LocalDate publishDate;

    @Column(length = 100)
    private String mimeType;

    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
