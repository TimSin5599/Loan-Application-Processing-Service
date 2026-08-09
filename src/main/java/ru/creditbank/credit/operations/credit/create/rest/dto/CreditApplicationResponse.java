package ru.creditbank.credit.operations.credit.create.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreditApplicationResponse(
        UUID id,
        String status,
        OffsetDateTime createdAt
) {
}