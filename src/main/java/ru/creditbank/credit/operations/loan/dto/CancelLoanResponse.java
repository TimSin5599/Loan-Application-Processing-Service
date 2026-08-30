package ru.creditbank.credit.operations.loan.dto;

import java.util.UUID;

public record CancelLoanResponse(UUID loanId, String status) {
}
