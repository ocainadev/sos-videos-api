package com.sosvideos.auth.dto;

import java.time.LocalDate;

public record MeResponse(
        Long userId,
        String name,
        String email,
        /** CPF mascarado — ex: ***.***.***-00. Nunca expor o CPF completo via API. */
        String cpfMasked,
        String role,
        boolean firstLoginPendingPasswordChange,
        boolean hasAcceptedActiveTerm,
        String subscriptionStatus,
        LocalDate subscriptionEndDate
) {
    /**
     * Mascara um CPF, retornando apenas os 2 últimos dígitos visíveis.
     * Entrada aceita com ou sem formatação: "12345678901" ou "123.456.789-01".
     * Resultado: ***.***.***-01
     */
    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) return "***.***.***-**";
        return "***.***.***-" + digits.substring(9);
    }
}
