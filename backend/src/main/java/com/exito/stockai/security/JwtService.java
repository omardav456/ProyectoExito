package com.exito.stockai.security;

import com.exito.stockai.model.security.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:28800000}") long expirationMillis) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public record TokenPayload(Long id, String email, String nombre, String rol) {
    }

    public String generarToken(Usuario u) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("nombre", u.getNombre())
                .claim("rol", u.getAuthority())
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + expirationMillis))
                .signWith(key)
                .compact();
    }

    public Optional<TokenPayload> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number uid = claims.get("uid", Number.class);
            String nombre = claims.get("nombre", String.class);
            String rol = claims.get("rol", String.class);
            return Optional.of(new TokenPayload(uid.longValue(), claims.getSubject(), nombre, rol));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}