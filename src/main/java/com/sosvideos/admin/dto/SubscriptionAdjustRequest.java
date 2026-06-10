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

    @Size(max = 500, message = "Motivo muito longo (máx. 500 caracteres)")
    private String reason;

    @AssertTrue(message = "A data de fim não pode ser anterior à data de início")
    public boolean isDateRangeValid() {
        if (startDate != null && endDate != null) {
            return !endDate.isBefore(startDate);
        }
        return true;
    }

    @AssertTrue(message = "Não é possível ativar uma assinatura com data de início no futuro")
    public boolean isActiveStartDateValid() {
        if (status == SubscriptionStatus.ACTIVE && startDate != null) {
            return !startDate.isAfter(AppConstants.today());
        }
        return true;
    }
}
