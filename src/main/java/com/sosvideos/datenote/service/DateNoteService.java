package com.sosvideos.datenote.service;

import com.sosvideos.datenote.dto.DateNoteRequest;
import com.sosvideos.datenote.dto.DateNoteResponse;
import com.sosvideos.datenote.entity.DateNote;
import com.sosvideos.datenote.repository.DateNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DateNoteService {

    private final DateNoteRepository dateNoteRepository;

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
