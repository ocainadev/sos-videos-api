package com.sosvideos.terms.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.terms.dto.TermRequest;
import com.sosvideos.terms.dto.TermResponse;
import com.sosvideos.terms.service.TermService;
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
@RequestMapping("/api/admin/terms")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Termos", description = "Publicação e edição de termos de uso")
@SecurityRequirement(name = "bearerAuth")
public class TermAdminController {

    private final TermService termService;

    @PostMapping
    @Operation(summary = "Cria um novo termo (se active=true, desativa todos os outros)")
    public ResponseEntity<ApiResponse<TermResponse>> create(@Valid @RequestBody TermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(termService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita um termo existente")
    public ResponseEntity<ApiResponse<TermResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TermRequest request) {
        return ResponseEntity.ok(ApiResponse.success(termService.update(id, request)));
    }

    @GetMapping
    @Operation(summary = "Lista todos os termos")
    public ResponseEntity<ApiResponse<List<TermResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(termService.listAll()));
    }
}
