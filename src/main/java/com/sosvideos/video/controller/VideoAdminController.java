package com.sosvideos.video.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.video.dto.VideoResponse;
import com.sosvideos.video.dto.VideoStatusRequest;
import com.sosvideos.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/videos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Vídeos", description = "Gestão de vídeos sincronizados")
@SecurityRequirement(name = "bearerAuth")
public class VideoAdminController {

    private final VideoService videoService;

    @GetMapping
    @Operation(summary = "Lista todos os vídeos (admin)")
    public ResponseEntity<ApiResponse<List<VideoResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(videoService.listAllForAdmin()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa ou desativa um vídeo")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable Long id,
            @Valid @RequestBody VideoStatusRequest request) {
        videoService.toggleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Status do vídeo atualizado."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove permanentemente um vídeo")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        videoService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Vídeo removido."));
    }
}
