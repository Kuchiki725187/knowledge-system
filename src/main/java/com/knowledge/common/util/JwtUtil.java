package com.knowledge.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-ttl-hours:2}")
    private long accessTtlHours;

    @Value("${jwt.refresh-ttl-days:7}")
    private long refreshTtlDays;

    public String createAccessToken(Long userId) {
        return createToken(userId, "access", Duration.ofHours(accessTtlHours));
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, "refresh", Duration.ofDays(refreshTtlDays));
    }

    private String createToken(Long userId, String type, Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(Date.from(now.toInstant().plus(ttl)))
                .signWith(key())
                .compact();
    }

    /**
     * 解析并校验签名,过期或被篡改时抛异常
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
