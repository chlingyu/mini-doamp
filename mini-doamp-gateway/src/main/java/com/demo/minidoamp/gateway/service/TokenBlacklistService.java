package com.demo.minidoamp.gateway.service;

import com.demo.minidoamp.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    public void blacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = jwtUtil.parseToken(token);
            String jti = claims.getId();
            long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (StringUtils.hasText(jti) && ttlMillis > 0) {
                stringRedisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", ttlMillis, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("blacklist token ignored, token invalid");
        }
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            String jti = jwtUtil.parseToken(token).getId();
            return StringUtils.hasText(jti)
                    && Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
        } catch (Exception e) {
            return false;
        }
    }
}