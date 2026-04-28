package com.sosvideos.datenote.controller;

import com.sosvideos.datenote.dto.DateNoteResponse;
import com.sosvideos.datenote.service.DateNoteService;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.subscription.service.SubscriptionService;
import com.sosvideos.terms.repository.TermAcceptanceRepository;
import com.sosvideos.exception.TermNotAcceptedException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/subscriber/date-notes")
@PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN', 'SUPPORT')")
@RequiredArgsConstructor
public class DateNoteSubscriberController {

    private final DateNoteService dateNoteService;
    private final SubscriptionService subscriptionService;
    private final TermAcceptanceRepository termAcceptanceRepository;

    @GetMapping("/{date}")
    public ResponseEntity<DateNoteResponse> get(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!principal.bypassesSubscriptionCheck()) {
            subscriptionService.requireAccess(principal.getId());
            termAcceptanceRepository.findActiveTermAcceptanceByUserId(principal.getId())
                    .orElseThrow(TermNotAcceptedException::new);
        }
        return ResponseEntity.ok(dateNoteService.get(date));
    }
}
