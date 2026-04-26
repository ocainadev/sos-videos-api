package com.sosvideos.download.service;

import com.sosvideos.download.dto.BatchDownloadListResponse;
import com.sosvideos.download.dto.BatchDownloadListResponse.VideoDownloadItem;
import com.sosvideos.download.dto.DownloadPrep;
import com.sosvideos.download.entity.Download;
import com.sosvideos.download.enums.DownloadType;
import com.sosvideos.download.repository.DownloadRepository;
import com.sosvideos.user.entity.User;
import com.sosvideos.user.repository.UserRepository;
import com.sosvideos.video.entity.Video;
import com.sosvideos.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadRepository downloadRepository;
    private final UserRepository userRepository;
    private final VideoService videoService;

    /**
     * Validates access, records the download, and returns the info needed to stream.
     * Transaction is committed BEFORE streaming begins, so the record is always saved.
     */
    @Transactional
    public DownloadPrep prepareDownload(Long videoId, Long userId, DownloadType type) {
        Video video = videoService.requireAccessToVideo(videoId, userId);

        User userRef = userRepository.getReferenceById(userId);
        Download download = Download.builder()
                .user(userRef)
                .video(video)
                .downloadType(type)
                .build();
        downloadRepository.save(download);

        log.info("Download recorded: userId={} videoId={} type={}", userId, videoId, type);

        return new DownloadPrep(
                video.getDriveFileId(),
                video.getTitle(),
                video.getMimeType() != null ? video.getMimeType() : "application/octet-stream",
                video.getFileSize()
        );
    }

    /**
     * Returns the ordered list of videos for a date so the frontend can
     * chain individual download calls sequentially.
     */
    @Transactional(readOnly = true)
    public BatchDownloadListResponse getBatchList(LocalDate date, Long userId) {
        List<Video> videos = videoService.requireAccessToVideosOnDate(date, userId);

        List<VideoDownloadItem> items = videos.stream()
                .map(v -> new VideoDownloadItem(
                        v.getId(),
                        v.getTitle(),
                        v.getMimeType() != null ? v.getMimeType() : "application/octet-stream",
                        v.getFileSize()
                ))
                .toList();

        log.debug("Batch list for date={}: {} videos available for userId={}", date, items.size(), userId);
        return new BatchDownloadListResponse(date, items.size(), items);
    }
}
