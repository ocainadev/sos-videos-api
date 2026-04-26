package com.sosvideos.kiwify.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(length = 255)
    private String externalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @PrePersist
    private void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
