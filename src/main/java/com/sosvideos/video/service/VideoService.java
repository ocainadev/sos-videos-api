package com.sosvideos.video.service;

import com.sosvideos.common.AppConstants;
import com.sosvideos.download.repository.DownloadRepository;
import com.sosvideos.exception.ResourceNotFoundException;
import com.sosvideos.exception.TermNotAcceptedException;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.subscription.entity.Subscription;
import com.sosvideos.subscription.service.SubscriptionService;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import com.sosvideos.video.dto.VideoResponse;
import com.sosvideos.video.dto.VideoStatusRequest;
import com.sosvideos.video.dto.VideosByDateResponse;
import com.sosvideos.video.entity.Video;
import com.sosvideos.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final SubscriptionService subscriptionService;
    private final TermAcceptanceRepository termAcceptanceRepository;
    private final DownloadRepository downloadRepository;

    @Transactional(readOnly = true)
    public List<VideosByDateResponse> getVisibleVideos(UserPrincipal principal) {
        LocalDate today = LocalDate.now();
        LocalDate visibleFrom;

        if (principal.bypassesSubscriptionCheck()) {
            visibleFrom = today.minusDays(AppConstants.VIDEO_VISIBILITY_DAYS - 1);
        } else {
            Subscription sub = subscriptionService.requireAccess(principal.getId());
            requireTermAccepted(principal.getId());
            visibleFrom = visibleFrom(sub, today);
        }

        List<Video> videos = videoRepository.findVisible(visibleFrom, today);

        if (videos.isEmpty()) {
            return List.of();
        }

        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        Set<Long> downloadedIds = downloadRepository.findDownloadedVideoIds(principal.getId(), videoIds);

        return videos.stream()
                .collect(Collectors.groupingBy(Video::getPublishDate,
                        java.util.LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.<LocalDate, List<Video>>comparingByKey().reversed())
                .map(entry -> new VideosByDateResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(v -> toResponse(v, downloadedIds.contains(v.getId())))
                                .toList()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Video requireAccessToVideo(Long videoId, UserPrincipal principal) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vídeo", videoId));

        if (!video.getActive()) {
            throw new ResourceNotFoundException("Vídeo", videoId);
        }

        if (!principal.bypassesSubscriptionCheck()) {
            Subscription sub = subscriptionService.requireAccess(principal.getId());
            requireTermAccepted(principal.getId());
            LocalDate visibleFrom = visibleFrom(sub, LocalDate.now());
            if (video.getPublishDate().isBefore(visibleFrom)) {
                throw new com.sosvideos.exception.BusinessException("Vídeo fora do período de acesso.");
            }
        }

        return video;
    }

    @Transactional(readOnly = true)
    public List<Video> requireAccessToVideosOnDate(LocalDate date, UserPrincipal principal) {
        if (principal.bypassesSubscriptionCheck()) {
            return videoRepository.findVisibleByDate(date, date);
        }
        Subscription sub = subscriptionService.requireAccess(principal.getId());
        requireTermAccepted(principal.getId());
        return videoRepository.findVisibleByDate(date, visibleFrom(sub, LocalDate.now()));
    }

    @Transactional
    public void toggleStatus(Long videoId, VideoStatusRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vídeo", videoId));
        video.setActive(request.getActive());
        videoRepository.save(video);
        log.info("Video id={} active set to {}", videoId, request.getActive());
    }

    @Transactional
    public void delete(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vídeo", videoId));
        downloadRepository.deleteByVideoId(videoId);
        videoRepository.delete(video);
        log.info("Video id={} permanently deleted by admin", videoId);
    }

    private static final int ADMIN_LIST_MAX = 1_000;

    @Transactional(readOnly = true)
    public List<VideoResponse> listAllForAdmin() {
        org.springframework.data.domain.Page<Video> page = videoRepository.findAll(
                org.springframework.data.domain.PageRequest.of(
                        0, ADMIN_LIST_MAX,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "publishDate", "id")));
        if (page.getTotalElements() > ADMIN_LIST_MAX) {
            log.warn("listAllForAdmin: total videos ({}) exceeds cap ({}) — only first {} returned",
                    page.getTotalElements(), ADMIN_LIST_MAX, ADMIN_LIST_MAX);
        }
        return page.stream().map(v -> toResponse(v, false)).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the earliest date the subscriber can see.
     *
     * <p>Rule: {@code max(today - VISIBILITY_WINDOW, subscriptionStart - 1)}<br>
     * The effective start is always one day before the subscription start date,
     * so every subscriber can see the day prior to their registration.</p>
     */
    private LocalDate visibleFrom(Subscription sub, LocalDate today) {
        LocalDate windowStart = today.minusDays(AppConstants.VIDEO_VISIBILITY_DAYS - 1);
        LocalDate subscriptionStart = sub.getStartDate() != null ? sub.getStartDate() : today;
        LocalDate effectiveStart = subscriptionStart.minusDays(1);
        return effectiveStart.isAfter(windowStart) ? effectiveStart : windowStart;
    }

    private void requireTermAccepted(Long userId) {
        termAcceptanceRepository.findActiveTermAcceptanceByUserId(userId)
                .orElseThrow(TermNotAcceptedException::new);
    }

    public VideoResponse toResponse(Video v, boolean downloaded) {
        return new VideoResponse(
                v.getId(),
                v.getTitle(),
                v.getPublishDate(),
                v.getMimeType(),
                v.getFileSize(),
                downloaded
        );
    }
}
