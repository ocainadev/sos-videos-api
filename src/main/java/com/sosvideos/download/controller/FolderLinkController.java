package com.sosvideos.download.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.download.service.FolderLinkTokenService;
import com.sosvideos.drive.entity.DriveFolder;
import com.sosvideos.drive.repository.DriveFolderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Link temporário para pasta do Drive")
public class FolderLinkController {

    private final FolderLinkTokenService folderLinkTokenService;
    private final DriveFolderRepository driveFolderRepository;

    @PostMapping("/videos/date/{date}/folder-link")
    @PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN', 'SUPPORT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Gera link temporário para pasta do Drive (5 min)",
            description = "Valida o acesso do usuário e retorna um token de 5 minutos. " +
                    "O frontend abre GET /api/folder-link/{token} em nova aba."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateFolderLink(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        DriveFolder folder = driveFolderRepository.findByPublishDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pasta do Drive não encontrada para a data " + date + "."));

        String token = folderLinkTokenService.generateToken(folder.getDriveFolderId());

        log.info("Folder link generated: date={} folderId={}", date, folder.getDriveFolderId());

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "token",     token,
                "expiresIn", 300
        )));
    }

    @GetMapping("/folder-link/{token}")
    @Operation(
            summary = "Redireciona para pasta do Drive via token temporário",
            description = "Endpoint público. Valida o token (5 min, multi-uso) e " +
                    "redireciona o browser para a pasta do Google Drive."
    )
    public ResponseEntity<Void> accessFolderLink(@PathVariable String token) {

        String folderId = folderLinkTokenService.validateToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.GONE, "Link expirado ou inválido."));

        String driveUrl = "https://drive.google.com/drive/folders/" + folderId + "?usp=sharing";

        log.info("Folder link accessed — folderId={}", folderId);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, driveUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
