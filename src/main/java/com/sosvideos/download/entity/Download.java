package com.sosvideos.download.entity;

import com.sosvideos.download.enums.DownloadType;
import com.sosvideos.user.entity.User;
import com.sosvideos.video.entity.Video;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "downloads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Download {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private LocalDateTime downloadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DownloadType downloadType;

    @PrePersist
    private void prePersist() {
        if (downloadedAt == null) {
            downloadedAt = LocalDateTime.now();
        }
    }
}
