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
 * Endpoint público (sem JWT) que faz streaming do vídeo via token de uso único.
 *
 * <p>O token é gerado pelo endpoint autenticado {@code POST /api/videos/{id}/download/token}
 * e expira em 60 segundos. A segurança está no token, não no JWT.</p>
 *
 * <p>O streaming é feito diretamente via Spring {@link StreamingResponseBody} —
 * o nginx deve ter {@code proxy_buffering off} para evitar bufferização excessiva.</p>
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
            description = "Valida o token, registra o download e faz streaming do arquivo. " +
                    "O token é consumido na primeira chamada e expira em 60 s."
    )
    public ResponseEntity<StreamingResponseBody> downloadByToken(@PathVariable String token) {

        // 1. Valida e consome o token (single-use)
        DownloadTokenEntry entry = downloadTokenService.validateAndConsume(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.GONE, "Token de download inválido ou expirado."));

        // 2. Valida acesso + registra o download no banco
        DownloadPrep prep = downloadService.prepareDownloadById(
                entry.videoId(), entry.userId(), entry.type());

        // 3. Faz streaming do arquivo
        StreamingResponseBody body = outputStream -> {
            log.debug("Streaming video id={} userId={}", entry.videoId(), entry.userId());
            googleDriveService.streamFile(prep.driveFileId(), outputStream);
        };

        String encodedName = URLEncoder.encode(prep.title(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        // application/octet-stream força o browser a baixar o arquivo em vez de
        // tentar reproduzir o vídeo inline — essencial para iOS Safari.
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        if (prep.fileSize() != null && prep.fileSize() > 0) {
            builder = builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(prep.fileSize()));
        }

        return builder.body(body);
    }
}
