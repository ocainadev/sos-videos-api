package com.sosvideos.download.controller;

import com.sosvideos.download.dto.DownloadPrep;
import com.sosvideos.download.dto.DownloadTokenEntry;
import com.sosvideos.download.service.DownloadService;
import com.sosvideos.download.service.DownloadTokenService;
import com.sosvideos.drive.service.GoogleDriveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Public (no JWT) endpoint that streams a video file for a valid one-time token.
 *
 * <p>This controller is intentionally unauthenticated. Security relies on the
 * token being short-lived (60 s) and single-use. The token is issued by
 * {@code POST /api/videos/{id}/download/token} which IS JWT-secured.</p>
 *
 * <p>Why public? The browser must be able to navigate directly to this URL
 * (e.g. via {@code window.location.href} or an anchor click) without any
 * JavaScript fetch — which is the only way to trigger a native file download
 * on mobile browsers without loading the entire file into JS memory.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Endpoint público de download por token")
public class PublicDownloadController {

    private final DownloadTokenService downloadTokenService;
    private final DownloadService downloadService;
    private final GoogleDriveService googleDriveService;

    @GetMapping("/{token}")
    @Operation(
            summary = "Download de vídeo via token de uso único",
            description = "Streams the video file. The token is consumed on first use and expires in 60 s."
    )
    public ResponseEntity<StreamingResponseBody> downloadByToken(@PathVariable String token) {

        DownloadTokenEntry entry = downloadTokenService.validateAndConsume(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.GONE, "Token de download inválido ou expirado."));

        // Validates access again + records the download in the DB
        DownloadPrep prep = downloadService.prepareDownloadById(
                entry.videoId(), entry.userId(), entry.type());

        StreamingResponseBody body = outputStream -> {
            log.debug("Streaming video id={} for userId={} via token", entry.videoId(), entry.userId());
            googleDriveService.streamFile(prep.driveFileId(), outputStream);
        };

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
}
