package pe.prismadev.servmedic.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.prismadev.servmedic.dto.AdminLoginResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
        @Value("${servmedic.jwt.secret}") String secret,
        @Value("${servmedic.jwt.expiration-minutes}") long expirationMinutes
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("La clave JWT debe tener al menos 32 bytes.");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateAdminToken(
        Long userId,
        String email,
        String fullName,
        String role
    ) {
        return generateUserToken(
            userId,
            email,
            fullName,
            role,
            null,
            null,
            null
        );
    }

    public String generateUserToken(
        Long userId,
        String email,
        String fullName,
        String role,
        Long patientProfileId,
        Long specialistProfileId,
        String specialistStatus
    ) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationMinutes * 60);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("fullName", fullName);
        claims.put("role", role);

        if (patientProfileId != null) {
            claims.put("patientProfileId", patientProfileId);
        }

        if (specialistProfileId != null) {
            claims.put("specialistProfileId", specialistProfileId);
        }

        if (specialistStatus != null && !specialistStatus.isBlank()) {
            claims.put("specialistStatus", specialistStatus);
        }

        return Jwts.builder()
            .subject(email)
            .issuer("servmedic-backend")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claims(claims)
            .signWith(secretKey)
            .compact();
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public AdminLoginResponse buildSessionFromToken(String token) {
        Claims claims = validateAndGetClaims(token);

        Object userIdValue = claims.get("userId");
        Long userId = userIdValue instanceof Number number ? number.longValue() : null;

        return new AdminLoginResponse(
            userId,
            claims.get("email", String.class),
            claims.get("fullName", String.class),
            claims.get("role", String.class),
            token,
            claims.getIssuedAt().toInstant().toString()
        );
    }
}