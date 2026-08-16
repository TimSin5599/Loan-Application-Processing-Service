package ru.creditbank.credit.operations.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "user_id";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser authenticate(String token) {
        Claims claims = parseClaims(token);
        String userId = claims.get(USER_ID_CLAIM, String.class);
        if (userId == null) {
            throw new JwtException("В JWT токене отсутствует user_id");
        }
        UUID parsedUserId;
        try {
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Некорректный формат user_id в JWT токене", e);
        }

        String email = claims.getSubject();
        String role = claims.get(ROLE_CLAIM, String.class);
        return new AuthenticatedUser(parsedUserId, email, role);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}