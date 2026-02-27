package com.demo.minidoamp.doamp.service;

import com.demo.minidoamp.doamp.config.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService delayDeleteExecutor;

    // ========== 读操作 ==========

    /**
     * 获取缓存值，如果是空值标记则返回 null 并标记 isNullValue
     * @return 缓存值，null 表示缓存不存在，__NULL__ 表示空值缓存
     */
    public String get(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            log.debug("缓存 MISS key={}", key);
        } else if (CacheConstants.NULL_VALUE.equals(value)) {
            log.info("空值缓存命中 key={}", key);
        } else {
            log.debug("缓存 HIT key={}", key);
        }
        return value;
    }

    /**
     * 判断是否为空值标记
     */
    public boolean isNullValue(String value) {
        return CacheConstants.NULL_VALUE.equals(value);
    }

    // ========== 写操作 ==========

    /**
     * 缓存正常值，使用随机 TTL 防雪崩
     */
    public void set(String key, String value) {
        long ttl = randomTtl();
        redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MINUTES);
        log.info("缓存回填 key={} ttl={}min", key, ttl);
    }

    /**
     * 缓存空值，防穿透，短 TTL
     */
    public void setNull(String key) {
        redisTemplate.opsForValue().set(key, CacheConstants.NULL_VALUE,
                CacheConstants.NULL_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("空值缓存写入 key={} ttl={}min", key, CacheConstants.NULL_TTL_MINUTES);
    }

    // ========== 删除操作 ==========

    /**
     * 直接删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 延迟双删：立即删一次 → 延迟 500ms → 再删一次
     * 调用方应在事务提交后调用此方法
     */
    public void doubleDelete(String key) {
        // 第一次删除
        redisTemplate.delete(key);
        log.info("延迟双删-第一次删除 key={}", key);

        // 延迟 500ms 第二次删除
        delayDeleteExecutor.schedule(() -> {
            try {
                redisTemplate.delete(key);
                log.info("延迟双删-第二次删除完成 key={}", key);
            } catch (Exception e) {
                log.warn("延迟双删-第二次删除失败 key={}, error={}", key, e.getMessage());
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    // ========== 批量操作（SCAN） ==========

    /**
     * 按前缀扫描并删除，使用 SCAN 避免阻塞 Redis
     * @return 删除的 key 数量
     */
    public int deleteByPrefix(String prefix) {
        List<String> keys = scanKeys(prefix + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("批量删除缓存 prefix={} count={}", prefix, keys.size());
        }
        return keys.size();
    }

    /**
     * 使用 SCAN 扫描匹配的 key
     */
    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    // ========== 工具方法 ==========

    /**
     * 随机 TTL：baseTTL ± randomOffset（防雪崩）
     */
    private long randomTtl() {
        long offset = ThreadLocalRandom.current().nextLong(
                -CacheConstants.RANDOM_OFFSET_MINUTES,
                CacheConstants.RANDOM_OFFSET_MINUTES + 1);
        return CacheConstants.BASE_TTL_MINUTES + offset;
    }
}
