package com.sosvideos.drive.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.drive.dto.DriveFolderRequest;
import com.sosvideos.drive.dto.DriveFolderResponse;
import com.sosvideos.drive.service.DriveFolderService;
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
@RequestMapping("/api/admin/drive-folders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Drive Folders", description = "Gestão de pastas do Google Drive por data")
@SecurityRequirement(name = "bearerAuth")
public class DriveFolderAdminController {

    private final DriveFolderService driveFolderService;

    @PostMapping
    @Operation(summary = "Cadastra uma pasta do Drive para uma data")
    public ResponseEntity<ApiResponse<DriveFolderResponse>> register(
            @Valid @RequestBody DriveFolderRequest request) {
        DriveFolderResponse response = driveFolderService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Sincroniza vídeos da pasta com o banco de dados")
    public ResponseEntity<ApiResponse<DriveFolderResponse>> sync(@PathVariable Long id) {
        DriveFolderResponse response = driveFolderService.sync(id);
        return ResponseEntity.ok(ApiResponse.success("Sincronização concluída.", response));
    }

    @GetMapping
    @Operation(summary = "Lista todas as pastas cadastradas")
    public ResponseEntity<ApiResponse<List<DriveFolderResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(driveFolderService.listAll()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma pasta e desativa os vídeos associados")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        driveFolderService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Pasta removida com sucesso."));
    }

}
