package com.sosvideos.tutorial.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.tutorial.dto.TutorialResponse;
import com.sosvideos.tutorial.service.TutorialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriber/tutorials")
@PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Subscriber — Tutoriais", description = "Central de tutoriais e onboarding")
@SecurityRequirement(name = "bearerAuth")
public class TutorialSubscriberController {

    private final TutorialService tutorialService;

    @GetMapping
    @Operation(summary = "Lista todos os tutoriais ativos")
    public ResponseEntity<ApiResponse<List<TutorialResponse>>> listAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(tutorialService.listActiveForSubscriber(principal.getId())));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retorna um tutorial pelo ID",
            description = "Suporta link direto para compartilhamento. Ex: /api/subscriber/tutorials/3"
    )
    public ResponseEntity<ApiResponse<TutorialResponse>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(tutorialService.findByIdForSubscriber(id, principal.getId())));
    }
}
