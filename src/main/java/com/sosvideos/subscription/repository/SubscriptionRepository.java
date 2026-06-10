package com.sosvideos.subscription.repository;

import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Subscription> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SubscriptionStatus status);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.user.id = :userId
              AND s.status = :status
              AND s.endDate >= :today
            ORDER BY s.endDate DESC
            """)
    List<Subscription> findActiveByUserId(@Param("userId") Long userId,
                                          @Param("status") SubscriptionStatus status,
                                          @Param("today") LocalDate today);

    boolean existsByKiwifySubscriptionId(String kiwifySubscriptionId);

    Optional<Subscription> findByKiwifySubscriptionId(String kiwifySubscriptionId);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.user.id = :userId
              AND s.status IN :statuses
              AND s.endDate >= :today
            ORDER BY s.endDate DESC
            """)
    List<Subscription> findAccessibleByUserId(@Param("userId") Long userId,
                                              @Param("statuses") Collection<SubscriptionStatus> statuses,
                                              @Param("today") LocalDate today);

    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.user.id IN :userIds
              AND s.createdAt = (
                  SELECT MAX(s2.createdAt) FROM Subscription s2 WHERE s2.user.id = s.user.id
              )
            """)
    List<Subscription> findLatestByUserIds(@Param("userIds") Set<Long> userIds);
}
