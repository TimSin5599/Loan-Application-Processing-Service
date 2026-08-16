package ru.creditbank.credit.operations.config;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String role) {
}
