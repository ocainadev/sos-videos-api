package com.sosvideos.admin.controller;

import com.sosvideos.admin.dto.CreateSubscriberRequest;
import com.sosvideos.admin.dto.PageResponse;
import com.sosvideos.admin.dto.SubscriberSummaryResponse;
import com.sosvideos.admin.dto.SubscriptionAdjustRequest;
import com.sosvideos.admin.dto.UserStatusRequest;
import com.sosvideos.admin.service.AdminService;
import com.sosvideos.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
@RequiredArgsConstructor
@Tag(name = "Admin — Assinantes", description = "Gestão de assinantes e assinaturas")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // ── Subscribers ───────────────────────────────────────────────────────────

    @PostMapping("/subscribers")
    @Operation(summary = "Cria um novo assinante manualmente")
    public ResponseEntity<ApiResponse<SubscriberSummaryResponse>> createSubscriber(
            @Valid @RequestBody CreateSubscriberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminService.createSubscriber(request)));
    }

    @GetMapping("/subscribers")
    @Operation(summary = "Lista assinantes com paginação, busca e filtro de ativos")
    public ResponseEntity<ApiResponse<PageResponse<SubscriberSummaryResponse>>> listSubscribers(
            @Parameter(description = "Busca por nome ou email")   @RequestParam(required = false)            String search,
            @Parameter(description = "Inclui inativos na lista")  @RequestParam(defaultValue = "false")      boolean includeInactive,
            @Parameter(description = "Página (0-based)")          @RequestParam(defaultValue = "0")          int page,
            @Parameter(description = "Tamanho da página")         @RequestParam(defaultValue = "20")         int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(adminService.listSubscribers(search, includeInactive, pageable)));
    }

    @GetMapping("/subscribers/{id}")
    @Operation(summary = "Detalhes de um assinante")
    public ResponseEntity<ApiResponse<SubscriberSummaryResponse>> getSubscriber(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSubscriber(id)));
    }

    @PatchMapping("/subscribers/{id}/status")
    @Operation(summary = "Ativa ou desativa um usuário assinante")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        adminService.toggleUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Status do usuário atualizado."));
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @PatchMapping("/subscriptions/{id}")
    @Operation(
            summary = "Ajuste manual de assinatura",
            description = "Permite ao admin ou suporte alterar status, startDate e endDate de uma assinatura. " +
                    "O campo 'reason' é opcional e registrado apenas no log."
    )
    public ResponseEntity<ApiResponse<Void>> adjustSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionAdjustRequest request) {
        adminService.adjustSubscription(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Assinatura ajustada com sucesso."));
    }
}
