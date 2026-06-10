package com.sosvideos.datenote.service;

import com.sosvideos.datenote.dto.DateNoteRequest;
import com.sosvideos.datenote.dto.DateNoteResponse;
import com.sosvideos.datenote.entity.DateNote;
import com.sosvideos.datenote.repository.DateNoteRepository;
import com.sosvideos.exception.TermNotAcceptedException;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.subscription.service.SubscriptionService;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DateNoteService {

    private final DateNoteRepository dateNoteRepository;
    private final SubscriptionService subscriptionService;
    private final TermAcceptanceRepository termAcceptanceRepository;

    public void requireSubscriberAccess(UserPrincipal principal) {
        if (!principal.bypassesSubscriptionCheck()) {
            subscriptionService.requireAccess(principal.getId());
            termAcceptanceRepository.findActiveTermAcceptanceByUserId(principal.getId())
                    .orElseThrow(TermNotAcceptedException::new);
        }
    }

    @Transactional(readOnly = true)
    public DateNoteResponse get(LocalDate date) {
        return dateNoteRepository.findByDate(date)
                .map(n -> new DateNoteResponse(n.getDate(), n.getContent()))
                .orElse(new DateNoteResponse(date, ""));
    }

    @Transactional
    public DateNoteResponse upsert(LocalDate date, DateNoteRequest request) {
        DateNote note = dateNoteRepository.findByDate(date)
                .orElseGet(() -> DateNote.builder().date(date).content("").build());
        note.setContent(request.getContent() != null ? request.getContent() : "");
        note = dateNoteRepository.save(note);
        return new DateNoteResponse(note.getDate(), note.getContent());
    }
}
