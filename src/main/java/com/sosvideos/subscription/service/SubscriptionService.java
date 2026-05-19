package com.sosvideos.subscription.service;

import com.sosvideos.common.AppConstants;

import com.sosvideos.exception.SubscriptionRequiredException;
import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.enums.SubscriptionStatus;
import com.sosvideos.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final List<SubscriptionStatus> ACCESS_STATUSES =
            List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED);

    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public Optional<Subscription> findAccessible(Long userId) {
        // Retorna a assinatura com maior endDate. Usar List evita
        // NonUniqueResultException caso o usuário tenha assinaturas duplicadas.
        return subscriptionRepository
                .findAccessibleByUserId(userId, ACCESS_STATUSES, AppConstants.today())
                .stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Subscription requireAccess(Long userId) {
        return findAccessible(userId)
                .orElseThrow(() -> new SubscriptionRequiredException(
                        "Sua assinatura está inativa ou expirada. Renove para continuar."));
    }

}
