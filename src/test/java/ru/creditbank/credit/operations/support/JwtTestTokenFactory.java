package ru.creditbank.credit.operations.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public final class JwtTestTokenFactory {
    private JwtTestTokenFactory() {
    }

    public static String generateToken(String secret, UUID userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .claim("user_id", userId.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        if (email != null) {
            builder.subject(email);
        }
        if (role != null) {
            builder.claim("role", role);
        }

        return builder.signWith(key).compact();
    }
}
