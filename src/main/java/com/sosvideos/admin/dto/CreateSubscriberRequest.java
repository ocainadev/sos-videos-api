package com.sosvideos.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateSubscriberRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome muito longo (máx. 150 caracteres)")
    private String name;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 254, message = "E-mail muito longo")
    private String email;

    @Pattern(regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})?$",
             message = "CPF inválido — informe 11 dígitos ou no formato 000.000.000-00")
    private String cpf;

    @Pattern(regexp = "^(SUBSCRIBER|SUPPORT)?$",
             message = "Role inválida — valores aceitos: SUBSCRIBER, SUPPORT")
    private String role;
}
