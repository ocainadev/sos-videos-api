package com.sosvideos.drive.scheduler;

import com.sosvideos.drive.service.DriveFolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriveFolderCleanupScheduler {

    private final DriveFolderService driveFolderService;

    @Scheduled(cron = "0 0 3 * * *")
    public void expireOldFolders() {
        log.info("DriveFolderCleanupScheduler: starting scheduled expiry run");
        int removed = driveFolderService.deleteExpired();
        log.info("DriveFolderCleanupScheduler: finished — {} folder(s) purged", removed);
    }
}
