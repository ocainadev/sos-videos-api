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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoint público (sem JWT) que redireciona o browser diretamente para o Google Drive.
 *
 * <h3>Por que redirect e não streaming?</h3>
 * <p>No modelo anterior (proxy), cada download trafegava pelo caminho:<br>
 * {@code Mobile → VPS nginx → Spring Boot → Google Drive → Spring Boot → VPS nginx → Mobile}<br>
 * A VPS virava o gargalo: toda a banda de upload era consumida para repassar os bytes.</p>
 *
 * <p>Com redirect HTTP 302, o browser vai direto para os servidores do Google:<br>
 * {@code Mobile → VPS (só o token, ~1 KB) → Google Drive CDN → Mobile}<br>
 * A VPS não trafega nenhum byte de vídeo — a velocidade é a do CDN do Google (50–200 Mbps).</p>
 *
 * <h3>Segurança</h3>
 * <ul>
 *   <li>O token de download é de uso único, expira em 60 s e é validado aqui antes do redirect.</li>
 *   <li>O access token do Google (DRIVE_READONLY, ~1 h) fica na URL do redirect — transmitido
 *       apenas via HTTPS, nunca armazenado.</li>
 *   <li>O download é registrado no banco ANTES do redirect, garantindo auditoria mesmo que o
 *       cliente não complete o download.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
@Tag(name = "Downloads", description = "Endpoint público de download por token (redirect para Drive)")
public class PublicDownloadController {

    private final DownloadTokenService downloadTokenService;
    private final DownloadService downloadService;
    private final GoogleDriveService googleDriveService;

    @GetMapping("/{token}")
    @Operation(
            summary = "Download de vídeo via token de uso único",
            description = "Valida o token, registra o download e redireciona para URL autenticada do Google Drive. " +
                    "O browser baixa o arquivo diretamente do CDN do Google, sem passar pela VPS."
    )
    public ResponseEntity<Void> downloadByToken(@PathVariable String token) {

        // 1. Valida e consome o token (single-use)
        DownloadTokenEntry entry = downloadTokenService.validateAndConsume(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.GONE, "Token de download inválido ou expirado."));

        // 2. Valida acesso novamente + registra o download no banco
        DownloadPrep prep = downloadService.prepareDownloadById(
                entry.videoId(), entry.userId(), entry.type());

        // 3. Gera URL autenticada temporária e redireciona o browser
        //    O cliente baixa direto do Google Drive — a VPS não trafega bytes de vídeo
        try {
            String driveUrl = googleDriveService.buildTempDownloadUrl(prep.driveFileId());
            log.debug("Redirecting download: videoId={} userId={}", entry.videoId(), entry.userId());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, driveUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to build Drive download URL for fileId={}: {}", prep.driveFileId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao gerar URL de download. Tente novamente.");
        }
    }
}
