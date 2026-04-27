package com.sosvideos.auth.dto;

import java.time.LocalDate;

public record LoginResponse(
        String token,
        Long userId,
        String name,
        String email,
        String role,
        boolean hasAcceptedActiveTerm,
        String subscriptionStatus,
        LocalDate subscriptionEndDate
) {}
