package com.sosvideos.subscription.entity;

import com.sosvideos.common.BaseEntity;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import com.sosvideos.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_sub_user_status_end", columnList = "user_id, status, endDate"),
        @Index(name = "idx_sub_user_created",    columnList = "user_id, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime lastPaymentDate;

    private LocalDate nextDueDate;

    @Column(length = 255)
    private String kiwifySubscriptionId;

    @Column(length = 255)
    private String kiwifyCustomerId;

    /**
     * Subscription is "active" when status=ACTIVE and within the paid period.
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
                && endDate != null
                && !LocalDate.now().isAfter(endDate);
    }

    /**
     * User has access when status is ACTIVE or CANCELED and endDate has not passed.
     * Refund and expiry remove access immediately.
     */
    public boolean hasAccess() {
        if (status == SubscriptionStatus.REFUNDED || status == SubscriptionStatus.EXPIRED) {
            return false;
        }
        return endDate != null && !LocalDate.now().isAfter(endDate);
    }
}
