package com.demo.minidoamp.integration;

import com.demo.minidoamp.doamp.config.CacheConstants;
import com.demo.minidoamp.doamp.service.CacheService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 缓存集成测试。
 * 验证简历第3条（Redis缓存）：
 * - CacheService Bean 正常注入
 * - CacheConstants 常量命名规范
 * - isNullValue() 方法正确识别空值标记
 *
 * 注意：H2 + test profile 下无真实 Redis。
 * 缓存刷新接口 + 延迟双删的运行态证据已通过 L4 验证记录在文档中。
 */
@DisplayName("Redis 缓存策略")
class CacheServiceTest extends BaseIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Test
    @DisplayName("CacheService Bean 应正常注入")
    void cacheServiceShouldBeInjected() {
        assertNotNull(cacheService, "CacheService 应被 Spring 管理");
    }

    @Test
    @DisplayName("空值标记常量存在且不为空")
    void nullValueConstantShouldExist() {
        assertNotNull(CacheConstants.NULL_VALUE, "NULL_VALUE 常量不应为空");
        assertFalse(CacheConstants.NULL_VALUE.isEmpty(), "NULL_VALUE 常量不应为空字符串");
    }

    @Test
    @DisplayName("isNullValue() 正确识别空值标记")
    void isNullValueShouldWork() {
        assertTrue(cacheService.isNullValue(CacheConstants.NULL_VALUE), "应识别空值标记");
        assertFalse(cacheService.isNullValue("real_value"), "非空值不应被识别");
        assertFalse(cacheService.isNullValue(null), "null 不应被识别为空值标记");
    }

    @Test
    @DisplayName("缓存 key 前缀常量命名规范")
    void keyPrefixesShouldBeStandardized() {
        // 验证 key 前缀遵循 domain:type: 格式
        assertNotNull(CacheConstants.DICT_KEY_PREFIX, "DICT_KEY_PREFIX 应存在");
        assertNotNull(CacheConstants.INDEX_KEY_PREFIX, "INDEX_KEY_PREFIX 应存在");
        assertTrue(CacheConstants.DICT_KEY_PREFIX.contains(":"), "前缀应包含冒号分隔符");
        assertTrue(CacheConstants.INDEX_KEY_PREFIX.contains(":"), "前缀应包含冒号分隔符");
    }
}
