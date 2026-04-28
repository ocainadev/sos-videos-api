package com.sosvideos.tutorial.service;

import com.sosvideos.exception.ResourceNotFoundException;
import com.sosvideos.exception.SubscriptionRequiredException;
import com.sosvideos.exception.TermNotAcceptedException;
import com.sosvideos.subscription.service.SubscriptionService;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import com.sosvideos.tutorial.dto.TutorialRequest;
import com.sosvideos.tutorial.dto.TutorialResponse;
import com.sosvideos.tutorial.dto.TutorialStatusRequest;
import com.sosvideos.tutorial.entity.Tutorial;
import com.sosvideos.tutorial.repository.TutorialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorialService {

    private final TutorialRepository tutorialRepository;
    private final SubscriptionService subscriptionService;
    private final TermAcceptanceRepository termAcceptanceRepository;

    // ── Subscriber ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TutorialResponse> listActiveForSubscriber(Long userId) {
        requireFullAccess(userId);
        return tutorialRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TutorialResponse findByIdForSubscriber(Long id, Long userId) {
        requireFullAccess(userId);
        return tutorialRepository.findByIdAndActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tutorial", id));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Transactional
    public TutorialResponse create(TutorialRequest request) {
        Tutorial tutorial = Tutorial.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .contentType(request.getContentType())
                .url(request.getUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .active(request.isActive())
                .sortOrder(request.getSortOrder())
                .build();
        return toResponse(tutorialRepository.save(tutorial));
    }

    @Transactional
    public TutorialResponse update(Long id, TutorialRequest request) {
        Tutorial tutorial = tutorialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutorial", id));
        tutorial.setTitle(request.getTitle());
        tutorial.setDescription(request.getDescription());
        tutorial.setContentType(request.getContentType());
        tutorial.setUrl(request.getUrl());
        tutorial.setThumbnailUrl(request.getThumbnailUrl());
        tutorial.setActive(request.isActive());
        tutorial.setSortOrder(request.getSortOrder());
        return toResponse(tutorialRepository.save(tutorial));
    }

    @Transactional
    public void delete(Long id) {
        Tutorial tutorial = tutorialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutorial", id));
        tutorialRepository.delete(tutorial);
        log.info("Tutorial id={} deleted", id);
    }

    @Transactional
    public void toggleStatus(Long id, TutorialStatusRequest request) {
        Tutorial tutorial = tutorialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutorial", id));
        tutorial.setActive(request.getActive());
        tutorialRepository.save(tutorial);
        log.info("Tutorial id={} active set to {}", id, request.getActive());
    }

    @Transactional(readOnly = true)
    public List<TutorialResponse> listAllForAdmin() {
        return tutorialRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "sortOrder", "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireFullAccess(Long userId) {
        subscriptionService.requireAccess(userId);
        termAcceptanceRepository.findActiveTermAcceptanceByUserId(userId)
                .orElseThrow(TermNotAcceptedException::new);
    }

    private TutorialResponse toResponse(Tutorial t) {
        return new TutorialResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getContentType().name(),
                t.getUrl(),
                t.getThumbnailUrl(),
                t.getActive(),
                t.getSortOrder(),
                t.getCreatedAt()
        );
    }
}
