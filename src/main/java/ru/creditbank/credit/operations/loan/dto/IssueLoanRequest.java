package ru.creditbank.credit.operations.loan.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record IssueLoanRequest(
        UUID creditApplicationId,
        UUID userId,
        BigDecimal totalAmount,
        Integer termMonths,
        BigDecimal interestRate
) {
}
