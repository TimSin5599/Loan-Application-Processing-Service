package ru.creditbank.credit.operations.credit.manage.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StatusUpdateRequest(

        @NotNull(message = "Статус обязателен для заполнения")
        ManagerDecisionStatus status,

        @Size(max = 500, message = "Комментарий менеджера не может превышать 500 символов")
        String managerComment,

        @DecimalMin(value = "0.0", message = "Процентная ставка не может быть отрицательной")
        BigDecimal interestRate
) {
}
