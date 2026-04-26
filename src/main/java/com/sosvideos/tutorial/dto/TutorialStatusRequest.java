package com.sosvideos.tutorial.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TutorialStatusRequest {

    @NotNull(message = "Status 'active' é obrigatório")
    private Boolean active;
}
