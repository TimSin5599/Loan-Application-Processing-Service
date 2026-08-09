package ru.creditbank.credit.operations.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String message,
        List<String> errors
) {
    public ErrorResponse(int status, String message, List<String> errors) {
        this(OffsetDateTime.now(), status, message, errors);
    }
}