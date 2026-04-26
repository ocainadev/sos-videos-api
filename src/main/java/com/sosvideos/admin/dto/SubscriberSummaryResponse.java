package com.sosvideos.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriberSummaryResponse(
        Long userId,
        String name,
        String email,
        String cpf,
        boolean userActive,
        LocalDateTime userCreatedAt,

        Long subscriptionId,
        String subscriptionStatus,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate,
        LocalDateTime lastPaymentDate,
        String kiwifySubscriptionId,
        String kiwifyCustomerId,

        boolean hasAcceptedActiveTerm
) {}
