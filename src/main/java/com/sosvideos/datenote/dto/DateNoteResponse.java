package com.sosvideos.datenote.dto;

import java.time.LocalDate;

public record DateNoteResponse(LocalDate date, String content) {}
