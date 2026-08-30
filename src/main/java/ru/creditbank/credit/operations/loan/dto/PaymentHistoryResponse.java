package ru.creditbank.credit.operations.loan.dto;

import java.math.BigDecimal;

public record PaymentHistoryResponse(
        int totalLoans,
        int activeLoans,
        boolean hasActiveOverdue,
        BigDecimal totalOutstandingDebt
) {
}
