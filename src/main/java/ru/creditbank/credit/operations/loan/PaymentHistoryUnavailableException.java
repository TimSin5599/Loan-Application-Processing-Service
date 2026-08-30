package ru.creditbank.credit.operations.loan;

import java.util.UUID;

public class PaymentHistoryUnavailableException extends RuntimeException {
    public PaymentHistoryUnavailableException(UUID userId, Throwable cause) {
        super("Не удалось получить кредитную историю пользователя " + userId, cause);
    }
}
