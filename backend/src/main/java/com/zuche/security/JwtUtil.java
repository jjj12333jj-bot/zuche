package com.zuche.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析（HS256）
 *
 * <p>payload 只放 userId / username / roleCode / storeId，
 * 不放信用分等易变数据——信用分会变，必须每次查库。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:24}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度不足 32 字节，HS256 要求至少 256 位");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(Long userId, String username, String roleCode, Long storeId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours * 3600_000L);
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCode", roleCode)
                .issuedAt(now)
                .expiration(expire);
        if (storeId != null) {
            builder.claim("storeId", storeId);
        }
        return builder.signWith(key).compact();
    }

    /**
     * 解析 token，失败（过期/篡改/格式错）统一抛 JwtException，由拦截器转 401
     */
    public LoginUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(claims.getSubject()));
        user.setUsername(claims.get("username", String.class));
        user.setRoleCode(claims.get("roleCode", String.class));
        Number storeId = claims.get("storeId", Number.class);
        user.setStoreId(storeId == null ? null : storeId.longValue());
        return user;
    }

    public long getExpireHours() {
        return expireHours;
    }
}
