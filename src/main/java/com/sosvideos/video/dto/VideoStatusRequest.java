package com.sosvideos.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VideoStatusRequest {

    @NotNull(message = "Status 'active' é obrigatório")
    private Boolean active;
}
