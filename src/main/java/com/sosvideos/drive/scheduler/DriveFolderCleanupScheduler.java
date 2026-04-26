package com.sosvideos.drive.scheduler;

import com.sosvideos.download.repository.DownloadRepository;
import com.sosvideos.drive.entity.DriveFolder;
import com.sosvideos.drive.repository.DriveFolderRepository;
import com.sosvideos.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriveFolderCleanupScheduler {

    private static final int EXPIRY_DAYS = 8;

    private final DriveFolderRepository driveFolderRepository;
    private final VideoRepository videoRepository;
    private final DownloadRepository downloadRepository;

    // Runs every day at 03:00
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireOldFolders() {
        LocalDate cutoff = LocalDate.now().minusDays(EXPIRY_DAYS);
        List<DriveFolder> old = driveFolderRepository.findExpired(cutoff);

        if (old.isEmpty()) return;

        for (DriveFolder folder : old) {
            List<com.sosvideos.video.entity.Video> videos =
                    videoRepository.findByPublishDateOrderByIdAsc(folder.getPublishDate());
            videos.forEach(v -> downloadRepository.deleteByVideoId(v.getId()));
            videoRepository.deleteAll(videos);
            driveFolderRepository.delete(folder);
            log.info("Auto-expired folder id={} date={} ({} videos removed)",
                    folder.getId(), folder.getPublishDate(), videos.size());
        }
    }
}
