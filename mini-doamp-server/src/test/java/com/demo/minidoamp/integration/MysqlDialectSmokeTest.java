package com.demo.minidoamp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MySQL 方言冒烟测试。
 * 使用 BaseIntegrationTest 中共享的 Testcontainers MySQL 实例。
 */
public class MysqlDialectSmokeTest extends BaseIntegrationTest {

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

        // 3. MySQL 分页语法验证：验证任务日志接口原生分页
        ResponseEntity<String> pageResp = getWithToken(
                "/api/system/job/log/native?pageNum=1&pageSize=10", token);
        assertEquals(200, getCode(pageResp));
        JsonNode pageData = getData(pageResp);
        assertTrue(pageData.has("records"));
        assertTrue(pageData.get("records").isArray());
    }
}
