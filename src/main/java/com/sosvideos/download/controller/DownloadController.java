package com.sosvideos.download.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.download.dto.BatchDownloadListResponse;
import com.sosvideos.download.dto.DownloadPrep;
import com.sosvideos.download.enums.DownloadType;
import com.sosvideos.download.service.DownloadService;
import com.sosvideos.download.service.DownloadTokenService;
import com.sosvideos.drive.service.GoogleDriveService;
import com.sosvideos.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN', 'SUPPORT')")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Download de vídeos (individual e encadeado por data)")
@SecurityRequirement(name = "bearerAuth")
public class DownloadController {

    private final DownloadService downloadService;
    private final DownloadTokenService downloadTokenService;
    private final GoogleDriveService googleDriveService;

    @GetMapping("/{id}/download")
    @Operation(
            summary = "Download individual de um vídeo",
            description = "Faz stream do arquivo via proxy. Use type=BATCH quando chamado em sequência pelo frontend."
    )
    public ResponseEntity<StreamingResponseBody> downloadVideo(
            @PathVariable Long id,
            @Parameter(description = "Tipo do download: SINGLE (padrão) ou BATCH")
            @RequestParam(defaultValue = "SINGLE") DownloadType type,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 1. Validate access + record download
        DownloadPrep prep = downloadService.prepareDownload(id, principal, type);

        // 2. Build streaming body
        StreamingResponseBody body = outputStream -> {
            log.debug("Streaming video id={} for userId={}", id, principal.getId());
            googleDriveService.streamFile(prep.driveFileId(), outputStream);
        };

        // 3. Build response headers
        String encodedName = URLEncoder.encode(prep.title(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(prep.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        if (prep.fileSize() != null && prep.fileSize() > 0) {
            builder = builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(prep.fileSize()));
        }

        return builder.body(body);
    }

    @PostMapping("/{id}/download/token")
    @Operation(
            summary = "Gera token de download de curta duração",
            description = "Retorna um token de uso único (60 s) para o endpoint público GET /api/download/{token}."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> generateDownloadToken(
            @PathVariable Long id,
            @Parameter(description = "Tipo do download: SINGLE (padrão) ou BATCH")
            @RequestParam(defaultValue = "SINGLE") DownloadType type,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Validate access before issuing the token (throws if not allowed)
        downloadService.validateVideoAccess(id, principal);

        String token = downloadTokenService.generateToken(id, principal.getId(), type);
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
    }

    @GetMapping("/date/{date}/download-all")
    @Operation(
            summary = "Lista vídeos de uma data para download encadeado",
            description = "Retorna a lista ordenada de vídeos do dia. " +
                    "O frontend chama GET /api/videos/{id}/download?type=BATCH para cada item em sequência."
    )
    public ResponseEntity<ApiResponse<BatchDownloadListResponse>> getBatchList(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {

        BatchDownloadListResponse response = downloadService.getBatchList(date, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
