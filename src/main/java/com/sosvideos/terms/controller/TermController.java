package com.sosvideos.terms.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.terms.dto.TermResponse;
import com.sosvideos.terms.dto.TermStatusResponse;
import com.sosvideos.terms.service.TermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Termos", description = "Consulta e aceite do termo obrigatório")
@SecurityRequirement(name = "bearerAuth")
public class TermController {

    private final TermService termService;

    @GetMapping("/api/terms/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retorna o conteúdo do termo ativo")
    public ResponseEntity<ApiResponse<TermResponse>> getActiveTerm() {
        return ResponseEntity.ok(ApiResponse.success(termService.getActiveTerm()));
    }

    @PostMapping("/api/terms/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aceita o termo ativo (idempotente)")
    public ResponseEntity<ApiResponse<Void>> accept(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        termService.acceptActiveTerm(
                principal.getId(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.ok("Termo aceito com sucesso."));
    }

    @GetMapping("/api/subscriber/term-status")
    @PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
    @Operation(summary = "Verifica se o assinante aceitou o termo ativo")
    public ResponseEntity<ApiResponse<TermStatusResponse>> getTermStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(termService.getTermStatus(principal.getId())));
    }
}
