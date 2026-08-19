package ru.creditbank.credit.operations.loan.dto;

import java.math.BigDecimal;

public record PaymentHistoryResponse(
        int totalLoans,
        int activeLoans,
        int onTimePayments,
        int latePayments,
        boolean hasActiveOverdue,
        BigDecimal totalOutstandingDebt
) {
}
