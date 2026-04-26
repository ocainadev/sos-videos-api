package com.sosvideos.terms.entity;

import com.sosvideos.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "term_acceptances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(nullable = false)
    private LocalDateTime acceptedAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @PrePersist
    private void prePersist() {
        if (acceptedAt == null) {
            acceptedAt = LocalDateTime.now();
        }
    }
}
