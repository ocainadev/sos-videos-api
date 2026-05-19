package com.sosvideos.common;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Generic DTO for toggling the {@code active} flag on any entity.
 * Replaces the identical VideoStatusRequest, TutorialStatusRequest and UserStatusRequest.
 */
@Getter
@NoArgsConstructor
public class StatusRequest {

    @NotNull(message = "Status 'active' é obrigatório")
    private Boolean active;
}
