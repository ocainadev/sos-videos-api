package com.sosvideos.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserStatusRequest {

    @NotNull(message = "Status 'active' é obrigatório")
    private Boolean active;
}
