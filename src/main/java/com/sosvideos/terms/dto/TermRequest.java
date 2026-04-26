package com.sosvideos.terms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título muito longo (máx. 200 caracteres)")
    private String title;

    @NotBlank(message = "Conteúdo é obrigatório")
    @Size(max = 100_000, message = "Conteúdo muito longo (máx. 100.000 caracteres)")
    private String content;

    @NotBlank(message = "Versão é obrigatória")
    @Size(max = 20, message = "Versão muito longa (máx. 20 caracteres)")
    private String version;

    private boolean active;
}
