package com.sosvideos.download.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.download.dto.BatchDownloadListResponse;
import com.sosvideos.download.dto.DownloadPrep;
import com.sosvideos.download.enums.DownloadType;
import com.sosvideos.download.service.DownloadService;
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

@Slf4j
@RestController
@RequestMapping("/api/videos")
@PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Download de vídeos (individual e encadeado por data)")
@SecurityRequirement(name = "bearerAuth")
public class DownloadController {

    private final DownloadService downloadService;
    private final GoogleDriveService googleDriveService;

    /**
     * Streams a single video file directly from Google Drive.
     * The Drive file ID is never exposed to the client.
     *
     * @param type SINGLE (default) or BATCH when called as part of a chained download session.
     */
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

        // 1. Validate access + record download (committed before streaming)
        DownloadPrep prep = downloadService.prepareDownload(id, principal.getId(), type);

        // 2. Build streaming body — Drive link never leaves the server
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

    /**
     * Returns the ordered list of videos for a given date.
     * The frontend uses this list to chain individual download calls sequentially:
     * for each item, call GET /api/videos/{id}/download?type=BATCH
     */
    @GetMapping("/date/{date}/download-all")
    @Operation(
            summary = "Lista vídeos de uma data para download encadeado",
            description = "Retorna a lista ordenada de vídeos do dia. " +
                    "O frontend chama GET /api/videos/{id}/download?type=BATCH para cada item em sequência."
    )
    public ResponseEntity<ApiResponse<BatchDownloadListResponse>> getBatchList(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {

        BatchDownloadListResponse response = downloadService.getBatchList(date, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
