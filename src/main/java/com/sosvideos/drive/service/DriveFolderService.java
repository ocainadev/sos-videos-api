package com.sosvideos.drive.service;

import com.google.api.services.drive.model.File;
import com.sosvideos.common.AppConstants;
import com.sosvideos.drive.dto.DriveFolderRequest;
import com.sosvideos.drive.dto.DriveFolderResponse;
import com.sosvideos.drive.entity.DriveFolder;
import com.sosvideos.drive.enums.SyncStatus;
import com.sosvideos.drive.repository.DriveFolderRepository;
import com.sosvideos.exception.BusinessException;
import com.sosvideos.exception.ResourceNotFoundException;
import com.sosvideos.datenote.repository.DateNoteRepository;
import com.sosvideos.download.repository.DownloadRepository;
import com.sosvideos.video.entity.Video;
import com.sosvideos.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveFolderService {

    private final DriveFolderRepository driveFolderRepository;
    private final VideoRepository videoRepository;
    private final DownloadRepository downloadRepository;
    private final GoogleDriveService googleDriveService;
    private final DateNoteRepository dateNoteRepository;

    @Transactional
    public DriveFolderResponse register(DriveFolderRequest request) {
        // minAllowed = today - 7 (e.g. today=27 → 20). Dates 19 and before are rejected.
        LocalDate minAllowed = AppConstants.today().minusDays(AppConstants.FOLDER_EXPIRY_DAYS);
        if (request.getPublishDate().isBefore(minAllowed)) {
            throw new BusinessException(
                    "Não é possível cadastrar pasta para uma data anterior a " + minAllowed
                    + ". O limite é de " + AppConstants.FOLDER_EXPIRY_DAYS + " dias no passado.");
        }

        if (driveFolderRepository.existsByPublishDate(request.getPublishDate())) {
            throw new BusinessException("Já existe uma pasta cadastrada para a data: " + request.getPublishDate());
        }

        DriveFolder folder = DriveFolder.builder()
                .publishDate(request.getPublishDate())
                .driveFolderId(request.getDriveFolderId())
                .syncStatus(SyncStatus.PENDING)
                .build();
        return toResponse(driveFolderRepository.save(folder), null);
    }

    @Transactional
    public DriveFolderResponse sync(Long folderId) {
        DriveFolder folder = driveFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("DriveFolder", folderId));

        log.info("Starting sync for folder id={} date={}", folderId, folder.getPublishDate());

        List<File> driveFiles;
        try {
            driveFiles = googleDriveService.listVideoFiles(folder.getDriveFolderId());
        } catch (Exception e) {
            folder.setSyncStatus(SyncStatus.ERROR);
            driveFolderRepository.save(folder);
            log.error("Sync failed for folder id={}: {}", folderId, e.getMessage(), e);
            throw new BusinessException("Falha na sincronização com o Google Drive: " + e.getMessage());
        }

        log.info("Found {} files in Drive folder {}", driveFiles.size(), folder.getDriveFolderId());

        if (driveFiles.isEmpty()) {
            folder.setSyncStatus(SyncStatus.SYNCED);
            folder.setLastSyncedAt(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
            driveFolderRepository.save(folder);
            throw new BusinessException("A pasta está vazia no Google Drive — nenhum vídeo foi encontrado.");
        }

        AtomicInteger count = new AtomicInteger(0);
        for (File driveFile : driveFiles) {
            if (isTxtFile(driveFile)) {
                upsertDateNote(driveFile, folder);
            } else {
                upsertVideo(driveFile, folder);
                count.incrementAndGet();
            }
        }

        folder.setSyncStatus(SyncStatus.SYNCED);
        folder.setLastSyncedAt(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        log.info("Sync completed for folder id={}: {} files", folderId, count.get());

        return toResponse(driveFolderRepository.save(folder), count.get());
    }

    public List<DriveFolderResponse> listAll() {
        return driveFolderRepository.findAll().stream()
                .map(f -> toResponse(f, null))
                .toList();
    }

    public List<java.util.Map<String, String>> listFilesByDate(LocalDate date) {
        DriveFolder folder = driveFolderRepository.findByPublishDate(date)
                .orElseThrow(() -> new BusinessException("Nenhuma pasta cadastrada para " + date));
        try {
            return googleDriveService.listVideoFiles(folder.getDriveFolderId()).stream()
                    .map(f -> java.util.Map.of(
                            "id",       f.getId() != null ? f.getId() : "",
                            "name",     f.getName() != null ? f.getName() : "",
                            "mimeType", f.getMimeType() != null ? f.getMimeType() : "",
                            "isTxt",    String.valueOf(isTxtFile(f))
                    ))
                    .toList();
        } catch (Exception e) {
            throw new BusinessException("Erro ao listar arquivos: " + e.getMessage());
        }
    }

    @Transactional
    public String reimportNoteByDate(LocalDate date) {
        DriveFolder folder = driveFolderRepository.findByPublishDate(date)
                .orElseThrow(() -> new BusinessException("Nenhuma pasta cadastrada para " + date));

        List<File> driveFiles;
        try {
            driveFiles = googleDriveService.listVideoFiles(folder.getDriveFolderId());
        } catch (Exception e) {
            throw new BusinessException("Erro ao listar arquivos: " + e.getMessage());
        }

        StringBuilder result = new StringBuilder();
        result.append("Total de arquivos na pasta: ").append(driveFiles.size()).append("\n");
        driveFiles.forEach(f -> result.append("  - ").append(f.getName()).append(" | ").append(f.getMimeType()).append("\n"));

        List<File> txtFiles = driveFiles.stream().filter(this::isTxtFile).toList();
        if (txtFiles.isEmpty()) {
            result.append("\nNenhum arquivo .txt detectado.");
            return result.toString();
        }

        for (File f : txtFiles) {
            String mime = f.getMimeType();
            result.append("\nProcessando: ").append(f.getName()).append(" (").append(mime).append(")\n");
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (GOOGLE_DOC_MIME.equalsIgnoreCase(mime)) {
                    result.append("→ Google Doc detectado, usando export\n");
                    googleDriveService.exportAsPlainText(f.getId(), baos);
                } else {
                    result.append("→ Arquivo texto puro, usando streamFile\n");
                    googleDriveService.streamFile(f.getId(), baos);
                }
                String content = baos.toString(StandardCharsets.UTF_8).trim();
                result.append("→ Conteúdo: ").append(content.length()).append(" chars\n");
                result.append("→ Prévia:\n").append(content.length() > 300 ? content.substring(0, 300) + "..." : content).append("\n");

                dateNoteRepository.findByDate(date).ifPresentOrElse(
                        note -> { note.setContent(content); dateNoteRepository.save(note); },
                        () -> dateNoteRepository.save(
                                com.sosvideos.datenote.entity.DateNote.builder()
                                        .date(date).content(content).build())
                );
                result.append("✓ Salvo para ").append(date).append("\n");
            } catch (Exception e) {
                result.append("✗ ERRO: ").append(e.getMessage()).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * Deletes all folders whose publishDate is strictly older than
     * (today - FOLDER_EXPIRY_DAYS), together with their videos and download records.
     *
     * @return number of folders removed
     */
    @Transactional
    public int deleteExpired() {
        // cutoff = today - 7 (e.g. today=27 → 20). Query: publishDate < 20 → day 19 and before deleted.
        LocalDate cutoff = AppConstants.today().minusDays(AppConstants.FOLDER_EXPIRY_DAYS);
        List<DriveFolder> expired = driveFolderRepository.findExpired(cutoff);

        if (expired.isEmpty()) {
            log.info("deleteExpired: no expired folders found (cutoff={})", cutoff);
            return 0;
        }

        int removed = 0;
        for (DriveFolder folder : expired) {
            List<Video> videos = videoRepository.findByPublishDateOrderByIdAsc(folder.getPublishDate());
            videos.forEach(v -> downloadRepository.deleteByVideoId(v.getId()));
            videoRepository.deleteAll(videos);
            dateNoteRepository.findByDate(folder.getPublishDate()).ifPresent(dateNoteRepository::delete);
            driveFolderRepository.delete(folder);
            log.info("Expired folder removed: id={} date={} ({} videos)", folder.getId(), folder.getPublishDate(), videos.size());
            removed++;
        }

        log.info("deleteExpired: {} folder(s) purged (cutoff={})", removed, cutoff);
        return removed;
    }

    @Transactional
    public void delete(Long folderId) {
        DriveFolder folder = driveFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("DriveFolder", folderId));

        List<Video> videos = videoRepository.findByPublishDateOrderByIdAsc(folder.getPublishDate());
        videos.forEach(v -> downloadRepository.deleteByVideoId(v.getId()));
        videoRepository.deleteAll(videos);
        dateNoteRepository.findByDate(folder.getPublishDate()).ifPresent(dateNoteRepository::delete);
        driveFolderRepository.delete(folder);
        log.info("DriveFolder id={} deleted along with {} videos (hard delete)", folderId, videos.size());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isTxtFile(File driveFile) {
        String mime = driveFile.getMimeType();
        String name = driveFile.getName();
        return "text/plain".equalsIgnoreCase(mime)
                || (name != null && name.toLowerCase().endsWith(".txt"));
    }

    private static final String GOOGLE_DOC_MIME = "application/vnd.google-apps.document";

    private void upsertDateNote(File driveFile, DriveFolder folder) {
        String mime = driveFile.getMimeType();
        log.info("Processing txt file: name={} mimeType={} date={}", driveFile.getName(), mime, folder.getPublishDate());
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Google Drive auto-converte .txt para Google Doc — nesse caso usa export
            if (GOOGLE_DOC_MIME.equalsIgnoreCase(mime)) {
                log.info("File was auto-converted to Google Doc — exporting as plain text");
                googleDriveService.exportAsPlainText(driveFile.getId(), baos);
            } else {
                googleDriveService.streamFile(driveFile.getId(), baos);
            }

            String content = baos.toString(StandardCharsets.UTF_8).trim();
            log.info("DateNote content read ({} chars) for date={}", content.length(), folder.getPublishDate());

            dateNoteRepository.findByDate(folder.getPublishDate()).ifPresentOrElse(
                    note -> {
                        note.setContent(content);
                        dateNoteRepository.save(note);
                        log.info("DateNote updated from txt file: date={} file={}", folder.getPublishDate(), driveFile.getName());
                    },
                    () -> {
                        dateNoteRepository.save(
                                com.sosvideos.datenote.entity.DateNote.builder()
                                        .date(folder.getPublishDate())
                                        .content(content)
                                        .build());
                        log.info("DateNote created from txt file: date={} file={}", folder.getPublishDate(), driveFile.getName());
                    }
            );
        } catch (Exception e) {
            log.error("Failed to read txt file '{}' (mime={}) from Drive: {}", driveFile.getName(), mime, e.getMessage(), e);
        }
    }

    private void upsertVideo(File driveFile, DriveFolder folder) {
        videoRepository.findByDriveFileId(driveFile.getId()).ifPresentOrElse(
                existing -> {
                    existing.setTitle(driveFile.getName());
                    existing.setMimeType(driveFile.getMimeType());
                    existing.setFileSize(driveFile.getSize());
                    existing.setActive(true);
                    videoRepository.save(existing);
                    log.debug("Video updated: driveFileId={}", driveFile.getId());
                },
                () -> {
                    Video video = Video.builder()
                            .title(driveFile.getName())
                            .driveFileId(driveFile.getId())
                            .publishDate(folder.getPublishDate())
                            .mimeType(driveFile.getMimeType())
                            .fileSize(driveFile.getSize())
                            .active(true)
                            .build();
                    videoRepository.save(video);
                    log.debug("Video created: title={} driveFileId={}", video.getTitle(), video.getDriveFileId());
                }
        );
    }

    private DriveFolderResponse toResponse(DriveFolder f, Integer videoCount) {
        return new DriveFolderResponse(
                f.getId(),
                f.getPublishDate(),
                f.getDriveFolderId(),
                f.getSyncStatus().name(),
                f.getLastSyncedAt(),
                f.getCreatedAt(),
                videoCount
        );
    }
}
