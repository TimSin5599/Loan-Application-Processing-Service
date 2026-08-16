package ru.creditbank.credit.operations.credit.manage.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreditApplicationDetails(
        UUID id,
        UserInfo userInfo,
        LoanDetails loanDetails,
        String status,
        OffsetDateTime createdAt
) {

    public record UserInfo(String userId, String fullName, String email) {
    }

    public record LoanDetails(BigDecimal requestedAmount, Integer termMonths, BigDecimal interestRate) {
    }
}
