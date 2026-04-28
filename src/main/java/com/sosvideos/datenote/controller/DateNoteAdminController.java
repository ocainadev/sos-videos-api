package com.sosvideos.datenote.controller;

import com.sosvideos.datenote.dto.DateNoteRequest;
import com.sosvideos.datenote.dto.DateNoteResponse;
import com.sosvideos.datenote.service.DateNoteService;
import com.sosvideos.drive.service.DriveFolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/date-notes")
@RequiredArgsConstructor
public class DateNoteAdminController {

    private final DateNoteService dateNoteService;
    private final DriveFolderService driveFolderService;

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

    /** Diagnóstico: lista arquivos da pasta do Drive desta data */
    @GetMapping("/{date}/drive-files")
    public ResponseEntity<Object> driveFiles(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(driveFolderService.listFilesByDate(date));
    }

    /** Reimporta o links.txt da pasta do Drive desta data e retorna o que aconteceu */
    @PostMapping("/{date}/reimport")
    public ResponseEntity<Map<String, String>> reimport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String result = driveFolderService.reimportNoteByDate(date);
        return ResponseEntity.ok(Map.of("result", result));
    }
}
