package com.sosvideos.admin.dto;

import com.sosvideos.common.AppConstants;

import com.sosvideos.subscription.enums.SubscriptionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SubscriptionAdjustRequest {

    @NotNull(message = "Status é obrigatório")
    private SubscriptionStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** Motivo do ajuste — registrado em log; limite para evitar log injection. */
    @Size(max = 500, message = "Motivo muito longo (máx. 500 caracteres)")
    private String reason;

    /** Garante que endDate não é anterior a startDate quando ambos são informados. */
    @AssertTrue(message = "A data de fim não pode ser anterior à data de início")
    public boolean isDateRangeValid() {
        if (startDate != null && endDate != null) {
            return !endDate.isBefore(startDate);
        }
        return true;
    }

    /**
     * Se o status for ACTIVE e uma data de início for enviada no request,
     * ela não pode ser futura (ativação antecipada não é permitida).
     * Obs.: a validação completa — incluindo a startDate já gravada no banco —
     * é feita no AdminService, que tem acesso à entidade atual.
     */
    @AssertTrue(message = "Não é possível ativar uma assinatura com data de início no futuro")
    public boolean isActiveStartDateValid() {
        if (status == SubscriptionStatus.ACTIVE && startDate != null) {
            return !startDate.isAfter(AppConstants.today());
        }
        return true;
    }
}
