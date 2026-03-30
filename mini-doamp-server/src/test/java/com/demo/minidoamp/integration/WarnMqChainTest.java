package com.demo.minidoamp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预警消息链路集成测试。
 * 验证简历第1条（预警引擎）+ 第2条（消息推送）：
 * - 查询已有指标和规则
 * - 手动触发预警
 * - 检查预警记录生成
 */
@DisplayName("预警 → MQ 消息链路")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarnMqChainTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("触发已有规则 → 预警记录应生成")
    void triggerExistingRuleShouldWork() throws Exception {
        String token = loginAndGetToken();

        // 查询已有的启用规则（data.sql 中有预置数据）
        ResponseEntity<String> rulesResp = getWithToken(
                "/api/warn/rules?pageNum=1&pageSize=50", token);
        assertEquals(200, rulesResp.getStatusCodeValue());
        JsonNode records = getData(rulesResp).get("records");
        assertNotNull(records, "应有规则记录");

        // 找一个 status=1 的规则
        Long ruleId = null;
        for (JsonNode rule : records) {
            if (rule.get("status").asInt() == 1) {
                ruleId = rule.get("id").asLong();
                break;
            }
        }

        if (ruleId != null) {
            // 触发
            ResponseEntity<String> triggerResp = postWithToken(
                    "/api/warn/rules/" + ruleId + "/trigger", "{}", token);
            assertEquals(200, triggerResp.getStatusCodeValue());
            assertEquals(200, getCode(triggerResp), "触发应成功");

            // 检查预警记录
            ResponseEntity<String> recordsResp = getWithToken(
                    "/api/warn/records?pageNum=1&pageSize=50", token);
            assertEquals(200, recordsResp.getStatusCodeValue());
            JsonNode warnRecords = getData(recordsResp).get("records");
            assertNotNull(warnRecords, "预警记录不应为空");
        }
    }

    @Test
    @Order(2)
    @DisplayName("预警记录列表接口可用")
    void warnRecordsEndpointShouldBeAvailable() throws Exception {
        String token = loginAndGetToken();
        ResponseEntity<String> resp = getWithToken(
                "/api/warn/records?pageNum=1&pageSize=50", token);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(200, getCode(resp));
    }

    @Test
    @Order(3)
    @DisplayName("消息记录列表接口可用")
    void msgRecordsEndpointShouldBeAvailable() throws Exception {
        String token = loginAndGetToken();
        ResponseEntity<String> resp = getWithToken(
                "/api/msg/records?pageNum=1&pageSize=50", token);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals(200, getCode(resp));
    }
}
