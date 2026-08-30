package ru.creditbank.credit.operations.loan.dto;

import java.util.UUID;

public record IssueLoanResponse(UUID loanId, String status) {
}
