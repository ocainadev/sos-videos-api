package com.sosvideos.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DriveFolderRequest {

    @NotNull(message = "Data de publicação é obrigatória")
    private LocalDate publishDate;

    /**
     * ID de pasta do Google Drive — apenas caracteres alfanuméricos, hifens e underscores
     * (formato real dos IDs do Drive: 28-44 caracteres).
     * Evita query-injection na API do Drive (concatenação de string em setQ).
     */
    @NotBlank(message = "ID da pasta do Google Drive é obrigatório")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]{10,50}$",
             message = "ID da pasta inválido — deve conter entre 10 e 50 caracteres alfanuméricos")
    private String driveFolderId;
}
