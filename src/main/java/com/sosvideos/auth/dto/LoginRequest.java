package com.sosvideos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 254, message = "E-mail muito longo")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    // Limita a 128 chars para prevenir DoS via bcrypt (que processa qualquer tamanho antes de truncar)
    @Size(max = 128, message = "Senha muito longa")
    private String password;
}
