package com.sosvideos.common;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StatusRequest {

    @NotNull(message = "Status 'active' é obrigatório")
    private Boolean active;
}
