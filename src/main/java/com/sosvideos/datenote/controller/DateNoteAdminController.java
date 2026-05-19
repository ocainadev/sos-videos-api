package com.sosvideos.datenote.controller;

import com.sosvideos.datenote.dto.DateNoteRequest;
import com.sosvideos.datenote.dto.DateNoteResponse;
import com.sosvideos.datenote.service.DateNoteService;
import com.sosvideos.drive.service.DriveFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/date-notes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Notas de Data", description = "Gestão das notas diárias vinculadas a pastas do Drive")
@SecurityRequirement(name = "bearerAuth")
public class DateNoteAdminController {

    private final DateNoteService dateNoteService;
    private final DriveFolderService driveFolderService;

    @GetMapping("/{date}")
    @Operation(summary = "Obtém a nota de uma data específica")
    public ResponseEntity<DateNoteResponse> get(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dateNoteService.get(date));
    }

    @PutMapping("/{date}")
    @Operation(summary = "Cria ou atualiza a nota de uma data")
    public ResponseEntity<DateNoteResponse> upsert(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody @Valid DateNoteRequest request) {
        return ResponseEntity.ok(dateNoteService.upsert(date, request));
    }

    @GetMapping("/{date}/drive-files")
    @Operation(summary = "Lista arquivos da pasta do Drive para a data (diagnóstico)")
    public ResponseEntity<Object> driveFiles(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(driveFolderService.listFilesByDate(date));
    }

    @PostMapping("/{date}/reimport")
    @Operation(summary = "Reimporta o arquivo .txt da pasta do Drive para a data")
    public ResponseEntity<Map<String, String>> reimport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String result = driveFolderService.reimportNoteByDate(date);
        return ResponseEntity.ok(Map.of("result", result));
    }
}
