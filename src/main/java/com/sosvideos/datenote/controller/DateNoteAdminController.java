package com.sosvideos.datenote.controller;

import com.sosvideos.datenote.dto.DateNoteRequest;
import com.sosvideos.datenote.dto.DateNoteResponse;
import com.sosvideos.datenote.service.DateNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/date-notes")
@RequiredArgsConstructor
public class DateNoteAdminController {

    private final DateNoteService dateNoteService;

    @GetMapping("/{date}")
    public ResponseEntity<DateNoteResponse> get(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dateNoteService.get(date));
    }

    @PutMapping("/{date}")
    public ResponseEntity<DateNoteResponse> upsert(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody @Valid DateNoteRequest request) {
        return ResponseEntity.ok(dateNoteService.upsert(date, request));
    }
}
