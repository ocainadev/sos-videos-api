package com.sosvideos.datenote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DateNoteRequest {
    @NotNull
    private String content;
}
