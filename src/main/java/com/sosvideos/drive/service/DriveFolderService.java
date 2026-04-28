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
        LocalDate minAllowed = LocalDate.now().minusDays(AppConstants.FOLDER_EXPIRY_DAYS);
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
            upsertVideo(driveFile, folder);
            count.incrementAndGet();
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

    /**
     * Deletes all folders whose publishDate is strictly older than
     * (today - FOLDER_EXPIRY_DAYS), together with their videos and download records.
     *
     * @return number of folders removed
     */
    @Transactional
    public int deleteExpired() {
        // cutoff = today - 7 (e.g. today=27 → 20). Query: publishDate < 20 → day 19 and before deleted.
        LocalDate cutoff = LocalDate.now().minusDays(AppConstants.FOLDER_EXPIRY_DAYS);
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
