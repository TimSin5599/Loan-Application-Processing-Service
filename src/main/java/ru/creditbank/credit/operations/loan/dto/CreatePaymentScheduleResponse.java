package ru.creditbank.credit.operations.loan.dto;

import java.util.UUID;

public record CreatePaymentScheduleResponse(UUID loanId, int installmentsCreated) {
}
