package com.financedomain.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Standard BASE64 secret key (minimum 256 bits for HMAC-SHA256)
    private final String secretKey = "NDBFRTYzNTI2NjU1NkE1ODZOMjcyMzU3NTM4N1g4MkY0MTNGOTQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==";

    private String validToken;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secretKey);

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        validToken = Jwts.builder()
                .subject("771234567")
                .claims(Map.of("id", "1", "role", "CLIENT"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Devrait valider un token JWT valide")
    void shouldValidateValidToken() {
        assertDoesNotThrow(() -> jwtUtil.validateToken(validToken));
    }

    @Test
    @DisplayName("Devrait extraire les claims d'un token JWT (id, role, subject)")
    void shouldExtractClaimsFromToken() {
        Claims claims = jwtUtil.getClaims(validToken);

        assertNotNull(claims);
        assertEquals("771234567", claims.getSubject());
        assertEquals("1", claims.get("id", String.class));
        assertEquals("CLIENT", claims.get("role", String.class));
    }

    @Test
    @DisplayName("Devrait suivre l'activité du token et détecter l'inactivité après 10 minutes")
    void shouldTrackTokenActivityAndDetectInactivity() {
        long now = System.currentTimeMillis();

        // Initialement (token non vu), n'est pas inactif
        assertFalse(jwtUtil.isTokenInactive(validToken, now));

        // Mettre à jour l'activité
        jwtUtil.updateTokenActivity(validToken, now);
        assertFalse(jwtUtil.isTokenInactive(validToken, now + 5000));

        // Inactif si le délai dépasse 10 min (600 000 ms)
        assertTrue(jwtUtil.isTokenInactive(validToken, now + 600001));
    }

    @Test
    @DisplayName("Devrait nettoyer les tokens expirés lors de l'exécution de la tâche programmée")
    void shouldCleanExpiredTokens() {
        long now = System.currentTimeMillis();
        jwtUtil.updateTokenActivity(validToken, now - 600001); // Expiré il y a >10 min

        jwtUtil.cleanExpiredTokens();

        // Token nettoyé, donc à nouveau considéré comme non présent/inactif = false
        assertFalse(jwtUtil.isTokenInactive(validToken, System.currentTimeMillis()));
    }
}
