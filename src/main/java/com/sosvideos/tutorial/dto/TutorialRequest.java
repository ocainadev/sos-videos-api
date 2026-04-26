package com.sosvideos.tutorial.dto;

import com.sosvideos.tutorial.enums.TutorialContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TutorialRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título muito longo (máx. 200 caracteres)")
    private String title;

    @Size(max = 1000, message = "Descrição muito longa (máx. 1000 caracteres)")
    private String description;

    @NotNull(message = "Tipo de conteúdo é obrigatório")
    private TutorialContentType contentType;

    /**
     * URL do tutorial — aceita apenas https:// ou http:// para evitar
     * javascript: URIs e outros esquemas maliciosos.
     */
    @Pattern(regexp = "^https?://[^\\s]{3,2000}$",
             message = "URL inválida — deve começar com http:// ou https://")
    private String url;

    private boolean active = true;

    private int sortOrder = 0;
}
