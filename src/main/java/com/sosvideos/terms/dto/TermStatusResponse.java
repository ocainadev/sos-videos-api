package com.sosvideos.terms.dto;

public record TermStatusResponse(
        boolean hasActiveTerm,
        boolean accepted,
        Long termId,
        String termVersion,
        String termTitle
) {}
