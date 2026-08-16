package ru.creditbank.credit.operations.exception;

import java.util.UUID;

public class CreditNotFoundException extends RuntimeException {

    public CreditNotFoundException(UUID id) {
        super("Кредитная заявка не найдена: " + id);
    }
}
