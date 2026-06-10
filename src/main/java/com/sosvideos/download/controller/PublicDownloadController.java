package com.sosvideos.download.controller;

import com.sosvideos.download.dto.DownloadPrep;
import com.sosvideos.download.dto.DownloadTokenEntry;
import com.sosvideos.download.service.DownloadService;
import com.sosvideos.download.service.DownloadTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Endpoint público de download por token")
public class PublicDownloadController {

    private final DownloadTokenService downloadTokenService;
    private final DownloadService downloadService;

    @GetMapping("/{token}")
    @Operation(
            summary = "Download de vídeo via token de uso único",
            description = "Valida o token, registra o download e redireciona para o Google Drive CDN. " +
                    "O token é consumido na primeira chamada e expira em 60 s."
    )
    public ResponseEntity<Void> downloadByToken(@PathVariable String token) {

        // 1. Valida e consome o token (single-use)
        DownloadTokenEntry entry = downloadTokenService.validateAndConsume(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.GONE, "Token de download inválido ou expirado."));

        // 2. Valida acesso + registra o download no banco
        DownloadPrep prep = downloadService.prepareDownloadById(
                entry.videoId(), entry.userId(), entry.type());

        // 3. Redireciona browser direto ao CDN do Google Drive.
        String driveUrl = "https://drive.usercontent.google.com/download?id="
                + prep.driveFileId() + "&export=download&authuser=0";

        log.info("Download token OK — redirect videoId={} userId={} → Drive fileId={}",
                entry.videoId(), entry.userId(), prep.driveFileId());

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, driveUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
