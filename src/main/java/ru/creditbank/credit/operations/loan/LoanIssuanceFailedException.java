package ru.creditbank.credit.operations.loan;

import java.util.UUID;

public class LoanIssuanceFailedException extends RuntimeException {
    public LoanIssuanceFailedException(UUID creditApplicationId, Throwable cause) {
        super("Не удалось выдать кредит по заявке " + creditApplicationId, cause);
    }
}
