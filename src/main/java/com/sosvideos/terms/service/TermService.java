package com.sosvideos.terms.service;

import com.sosvideos.exception.BusinessException;
import com.sosvideos.exception.ResourceNotFoundException;
import com.sosvideos.terms.dto.TermRequest;
import com.sosvideos.terms.dto.TermResponse;
import com.sosvideos.terms.dto.TermStatusResponse;
import com.sosvideos.terms.entity.Term;
import com.sosvideos.terms.entity.TermAcceptance;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import com.sosvideos.terms.repository.TermRepository;
import com.sosvideos.user.entity.User;
import com.sosvideos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final TermAcceptanceRepository termAcceptanceRepository;
    private final UserRepository userRepository;

    // ── Subscriber ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TermResponse getActiveTerm() {
        return termRepository.findFirstByActiveTrue()
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Nenhum termo ativo encontrado."));
    }

    @Transactional(readOnly = true)
    public TermStatusResponse getTermStatus(Long userId) {
        Optional<Term> activeTerm = termRepository.findFirstByActiveTrue();
        if (activeTerm.isEmpty()) {
            return new TermStatusResponse(false, false, null, null, null);
        }
        Term term = activeTerm.get();
        boolean accepted = termAcceptanceRepository.existsByUserIdAndTermId(userId, term.getId());
        return new TermStatusResponse(true, accepted, term.getId(), term.getVersion(), term.getTitle());
    }

    @Transactional
    public void acceptActiveTerm(Long userId, String ipAddress, String userAgent) {
        Term term = termRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new BusinessException("Nenhum termo ativo para aceitar."));

        if (termAcceptanceRepository.existsByUserIdAndTermId(userId, term.getId())) {
            log.debug("Term already accepted by userId={} termId={}", userId, term.getId());
            return;
        }

        User userRef = userRepository.getReferenceById(userId);
        TermAcceptance acceptance = TermAcceptance.builder()
                .user(userRef)
                .term(term)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        termAcceptanceRepository.save(acceptance);
        log.info("Term accepted: userId={} termId={} version={}", userId, term.getId(), term.getVersion());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Transactional
    public TermResponse create(TermRequest request) {
        if (request.isActive()) {
            termRepository.deactivateAll();
        }
        Term term = Term.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .version(request.getVersion())
                .active(request.isActive())
                .build();
        return toResponse(termRepository.save(term));
    }

    @Transactional
    public TermResponse update(Long id, TermRequest request) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Termo", id));

        if (request.isActive() && !term.getActive()) {
            // Activating this term: deactivate all others first
            termRepository.deactivateAll();
        }

        term.setTitle(request.getTitle());
        term.setContent(request.getContent());
        term.setVersion(request.getVersion());
        term.setActive(request.isActive());
        return toResponse(termRepository.save(term));
    }

    @Transactional(readOnly = true)
    public List<TermResponse> listAll() {
        return termRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TermResponse toResponse(Term t) {
        return new TermResponse(
                t.getId(),
                t.getTitle(),
                t.getContent(),
                t.getVersion(),
                t.getActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
