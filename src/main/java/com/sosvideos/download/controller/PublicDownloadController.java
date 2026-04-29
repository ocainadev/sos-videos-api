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

/**
 * Endpoint público (sem JWT) que redireciona o browser para download direto
 * no Google Drive CDN — sem trafegar o arquivo pela VPS.
 *
 * <h2>Fluxo</h2>
 * <ol>
 *   <li>Frontend abre nova aba e navega para {@code GET /api/download/{token}}</li>
 *   <li>Backend valida o token (uso único, 60 s), registra o download</li>
 *   <li>Backend responde 302 → {@code drive.google.com/uc?export=download&confirm=t&id={fileId}}</li>
 *   <li>Browser segue o redirect e baixa diretamente do CDN do Google</li>
 * </ol>
 *
 * <h2>Requisito</h2>
 * Os arquivos de vídeo no Google Drive precisam ter permissão
 * <b>"Qualquer pessoa com o link pode visualizar"</b> (ou download).
 * Sem isso o Google retorna 403 ao browser. O ID do arquivo não é adivinhável
 * (string opaca de ~33 chars), então a segurança está no token + no ID.
 *
 * <h2>Por que não streaming pela VPS?</h2>
 * Streaming proxy (VPS como middleman) limita a velocidade ao uplink da VPS (~10–50 Mbps).
 * O redirect envia o browser direto ao CDN do Google (>100 Mbps), igual ao
 * comportamento do site HTML anterior.
 */
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
        //
        //    drive.usercontent.google.com/download é o endpoint atual do Google para
        //    downloads de arquivo — substitui o antigo drive.google.com/uc que exibia
        //    páginas de aviso de vírus para arquivos > 25 MB. Retorna o arquivo com
        //    Content-Disposition: attachment, fazendo o browser baixar sem navegar
        //    para fora da página da aplicação (nenhuma aba do Drive é aberta).
        //
        //    O arquivo NÃO passa pela VPS — vai direto Google CDN → browser do usuário.
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
