package com.financedomain.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private static final long INACTIVITY_LIMIT_MS = 600000; // 10 minutes
    private final Map<String, Long> tokenLastActivity = new ConcurrentHashMap<>();

    public void validateToken(final String token) {
        Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token);
    }

    public Claims getClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenInactive(final String token, long now) {
        Long lastActivity = tokenLastActivity.get(token);
        if (lastActivity == null) {
            // First time seeing the token, it is active
            return false;
        }
        return (now - lastActivity) > INACTIVITY_LIMIT_MS;
    }

    public void updateTokenActivity(final String token, long now) {
        tokenLastActivity.put(token, now);
    }

    @Scheduled(fixedRate = 60000) // every minute
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenLastActivity.entrySet().removeIf(entry -> (now - entry.getValue()) > INACTIVITY_LIMIT_MS);
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
