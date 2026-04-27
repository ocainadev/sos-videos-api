package com.sosvideos.auth.dto;

import java.time.LocalDate;

public record MeResponse(
        Long userId,
        String name,
        String email,
        String cpf,
        String role,
        boolean firstLoginPendingPasswordChange,
        boolean hasAcceptedActiveTerm,
        String subscriptionStatus,
        LocalDate subscriptionEndDate
) {}
