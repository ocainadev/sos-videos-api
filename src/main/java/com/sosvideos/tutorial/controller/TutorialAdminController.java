package com.sosvideos.tutorial.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.tutorial.dto.TutorialRequest;
import com.sosvideos.tutorial.dto.TutorialResponse;
import com.sosvideos.tutorial.dto.TutorialStatusRequest;
import com.sosvideos.tutorial.service.TutorialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tutorials")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Tutoriais", description = "CRUD de tutoriais")
@SecurityRequirement(name = "bearerAuth")
public class TutorialAdminController {

    private final TutorialService tutorialService;

    @PostMapping
    @Operation(summary = "Cria um novo tutorial")
    public ResponseEntity<ApiResponse<TutorialResponse>> create(
            @Valid @RequestBody TutorialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tutorialService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita um tutorial existente")
    public ResponseEntity<ApiResponse<TutorialResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TutorialRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tutorialService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa ou desativa um tutorial")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable Long id,
            @Valid @RequestBody TutorialStatusRequest request) {
        tutorialService.toggleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Status do tutorial atualizado."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um tutorial")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tutorialService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tutorial excluído."));
    }

    @GetMapping
    @Operation(summary = "Lista todos os tutoriais (incluindo inativos)")
    public ResponseEntity<ApiResponse<List<TutorialResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(tutorialService.listAllForAdmin()));
    }
}
