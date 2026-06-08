package com.rally.utils;

import com.rally.config.AuthJwtProperties;
import com.rally.domain.auth.model.TokenPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUtils {

    private static AuthJwtProperties jwtProperties;

    @Autowired
    public TokenUtils(AuthJwtProperties jwtProperties) {
        TokenUtils.jwtProperties = jwtProperties;
    }

    public static String issue(String userId) {
        SecretKey key = buildKey();
        long expireMs = (long) jwtProperties.getExpireDays() * 24 * 60 * 60 * 1000;
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(key)
                .compact();
    }

    public static Optional<TokenPayload> verify(String token) {
        try {
            SecretKey key = buildKey();
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new TokenPayload(claims.getSubject()));
        } catch (Exception e) {
            log.debug("token 验证失败: {}", token.length() > 8 ? token.substring(0, 8) + "..." : "***");
            return Optional.empty();
        }
    }

    private static SecretKey buildKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
