package com.demo.minidoamp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自动化 MySQL 方言冒烟测试。
 * 使用 Testcontainers 动态拉起彻底隔离的 MySQL，不依赖环境原有 compose 实例。
 */
@Testcontainers
public class MysqlDialectSmokeTest extends BaseIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("mini_doamp")
            .withUsername("root")
            .withPassword("root")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        // 将原测试的内存 H2 替换为容器 MySQL
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        
        // 强制重置初始化源并设置为启动时初始化
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/schema.sql");
        registry.add("spring.sql.init.data-locations", () -> "classpath:sql/data.sql");
        // 屏蔽 Quartz 等额外数据源的影响，强制以 MySQL 执行（databaseIdProvider会自动推断）
    }

    @Test
    public void testMysqlDialectDifferences() throws Exception {
        String token = loginAndGetToken();

        // 1. DATE_FORMAT 差异组验证：验证 "近期预警趋势" 聚合查询
        ResponseEntity<String> trendResp = getWithToken(
                "/api/warn/records/trend?days=7", token);
        assertEquals(200, getCode(trendResp));
        JsonNode trendData = getData(trendResp);
        assertTrue(trendData.isArray());

        // 2. GROUP_CONCAT 差异组验证：验证 "指标类型告警汇总" 字符串拼接聚合
        ResponseEntity<String> summaryResp = getWithToken(
                "/api/warn/indexes/type-summary", token);
        assertEquals(200, getCode(summaryResp));
        JsonNode summaryData = getData(summaryResp);
        assertTrue(summaryData.isArray());

        // 3. MySQL 分页语法拦截验证：验证任务日志接口原生分页
        ResponseEntity<String> pageResp = getWithToken(
                "/api/system/job/log/native?pageNum=1&pageSize=10", token);
        assertEquals(200, getCode(pageResp));
        JsonNode pageData = getData(pageResp);
        assertTrue(pageData.has("records"));
        assertTrue(pageData.get("records").isArray());
    }
}
