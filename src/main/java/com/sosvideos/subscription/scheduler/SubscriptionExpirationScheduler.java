package com.sosvideos.subscription.scheduler;

import com.sosvideos.common.AppConstants;
import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import com.sosvideos.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Roda diariamente e marca como EXPIRED todas as assinaturas ACTIVE
 * cuja data de validade já passou.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    // Roda todo dia à meia-noite horário de São Paulo
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, AppConstants.today());

        if (expired.isEmpty()) {
            log.debug("SubscriptionExpirationScheduler: nenhuma assinatura para expirar.");
            return;
        }

        expired.forEach(s -> s.setStatus(SubscriptionStatus.EXPIRED));
        subscriptionRepository.saveAll(expired);

        log.info("SubscriptionExpirationScheduler: {} assinatura(s) marcadas como EXPIRED.", expired.size());
    }
}
