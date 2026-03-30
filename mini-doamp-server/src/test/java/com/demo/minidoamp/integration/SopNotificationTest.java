package com.demo.minidoamp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SOP 工作流全生命周期集成测试。
 * 验证简历第7条（SOP工作流）：
 * 创建workflow → 发布 → 创建template → 创建task → SUBMIT → APPROVE → COMPLETED
 */
@DisplayName("SOP 工作流生命周期")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SopNotificationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("完整生命周期：创建 → SUBMIT → APPROVE → COMPLETED")
    void fullLifecycleShouldWork() throws Exception {
        String token = loginAndGetToken();
        long ts = System.currentTimeMillis();

        // 1. 创建 workflow
        String wfBody = "{\"workflowCode\":\"IT_WF_" + ts + "\"," +
                "\"workflowName\":\"集成测试流程_" + ts + "\"," +
                "\"nodes\":[" +
                "{\"nodeCode\":\"s1\",\"nodeType\":\"START\",\"nodeName\":\"开始\",\"xPos\":100,\"yPos\":200,\"sortOrder\":1}," +
                "{\"nodeCode\":\"p1\",\"nodeType\":\"PROCESS\",\"nodeName\":\"处理\",\"xPos\":300,\"yPos\":200,\"sortOrder\":2,\"assigneeId\":\"1\"}," +
                "{\"nodeCode\":\"a1\",\"nodeType\":\"APPROVE\",\"nodeName\":\"审批\",\"xPos\":500,\"yPos\":200,\"sortOrder\":3,\"assigneeId\":\"1\"}," +
                "{\"nodeCode\":\"e1\",\"nodeType\":\"END\",\"nodeName\":\"结束\",\"xPos\":700,\"yPos\":200,\"sortOrder\":4}" +
                "]," +
                "\"edges\":[" +
                "{\"sourceNodeCode\":\"s1\",\"targetNodeCode\":\"p1\"}," +
                "{\"sourceNodeCode\":\"p1\",\"targetNodeCode\":\"a1\"}," +
                "{\"sourceNodeCode\":\"a1\",\"targetNodeCode\":\"e1\"}" +
                "]}";
        ResponseEntity<String> wfResp = postWithToken("/api/sop/workflows", wfBody, token);
        assertEquals(200, wfResp.getStatusCodeValue());
        assertEquals(200, getCode(wfResp), "创建 workflow 应成功");

        // 2. 查找刚创建的 workflow ID
        ResponseEntity<String> wfListResp = getWithToken(
                "/api/sop/workflows?pageNum=1&pageSize=50", token);
        JsonNode wfRecords = getData(wfListResp).get("records");
        Long wfId = null;
        for (JsonNode wf : wfRecords) {
            if (wf.get("workflowName").asText().contains("集成测试流程_" + ts)) {
                wfId = wf.get("id").asLong();
                break;
            }
        }
        assertNotNull(wfId, "应找到刚创建的 workflow");

        // 3. 发布
        ResponseEntity<String> pubResp = putWithToken(
                "/api/sop/workflows/" + wfId + "/publish", "{}", token);
        assertEquals(200, pubResp.getStatusCodeValue());
        assertEquals(200, getCode(pubResp), "发布 workflow 应成功");

        // 4. 创建 template
        String tplBody = "{\"templateName\":\"IT_TPL_" + ts + "\"," +
                "\"workflowId\":" + wfId + "," +
                "\"triggerType\":\"MANUAL\"," +
                "\"status\":1}";
        ResponseEntity<String> tplResp = postWithToken("/api/sop/task-templates", tplBody, token);
        assertEquals(200, tplResp.getStatusCodeValue());
        assertEquals(200, getCode(tplResp), "创建 template 应成功");

        // 5. 查找 template ID
        ResponseEntity<String> tplListResp = getWithToken(
                "/api/sop/task-templates?pageNum=1&pageSize=50", token);
        JsonNode tplRecords = getData(tplListResp).get("records");
        Long tplId = null;
        for (JsonNode tpl : tplRecords) {
            if (tpl.get("templateName").asText().contains("IT_TPL_" + ts)) {
                tplId = tpl.get("id").asLong();
                break;
            }
        }
        assertNotNull(tplId, "应找到刚创建的 template");

        // 6. 创建 task（SopTaskRequest 需要 taskName + templateId）
        String taskBody = "{\"taskName\":\"IT_TASK_" + ts + "\",\"templateId\":" + tplId + "}";
        ResponseEntity<String> taskResp = postWithToken("/api/sop/tasks", taskBody, token);
        assertEquals(200, taskResp.getStatusCodeValue());
        assertEquals(200, getCode(taskResp), "创建 task 应成功");
        Long taskId = getData(taskResp).asLong();
        assertTrue(taskId > 0, "Task ID 应大于0");

        // 7. 查看 task 详情 — 应为 EXECUTING
        ResponseEntity<String> detailResp = getWithToken("/api/sop/tasks/" + taskId, token);
        assertEquals(200, detailResp.getStatusCodeValue());
        JsonNode taskData = getData(detailResp);
        assertEquals("EXECUTING", taskData.get("status").asText(), "初始状态应为 EXECUTING");

        // 8. 获取 PENDING 的执行记录
        JsonNode execRecords = taskData.get("execRecords");
        assertNotNull(execRecords, "应有执行记录");
        Long execId = null;
        for (JsonNode exec : execRecords) {
            if ("PENDING".equals(exec.get("status").asText())) {
                execId = exec.get("id").asLong();
                break;
            }
        }
        assertNotNull(execId, "应有 PENDING 状态的执行记录");

        // 9. SUBMIT（PROCESS 节点）
        ResponseEntity<String> advResp = postWithToken(
                "/api/sop/task-execs/" + execId + "/advance",
                "{\"action\":\"SUBMIT\",\"remark\":\"集成测试-提交\"}", token);
        assertEquals(200, advResp.getStatusCodeValue());
        assertEquals(200, getCode(advResp), "SUBMIT 应成功");

        // 10. 查看状态 → APPROVING
        detailResp = getWithToken("/api/sop/tasks/" + taskId, token);
        taskData = getData(detailResp);
        assertEquals("APPROVING", taskData.get("status").asText(), "SUBMIT 后应为 APPROVING");

        // 11. 获取审批节点 PENDING 记录
        execRecords = taskData.get("execRecords");
        Long approveExecId = null;
        for (JsonNode exec : execRecords) {
            if ("PENDING".equals(exec.get("status").asText())) {
                approveExecId = exec.get("id").asLong();
                break;
            }
        }
        assertNotNull(approveExecId, "应有审批节点的 PENDING 记录");

        // 12. APPROVE
        ResponseEntity<String> appResp = postWithToken(
                "/api/sop/task-execs/" + approveExecId + "/advance",
                "{\"action\":\"APPROVE\",\"remark\":\"集成测试-审批通过\"}", token);
        assertEquals(200, appResp.getStatusCodeValue());
        assertEquals(200, getCode(appResp), "APPROVE 应成功");

        // 13. 最终状态 → COMPLETED
        detailResp = getWithToken("/api/sop/tasks/" + taskId, token);
        taskData = getData(detailResp);
        assertEquals("COMPLETED", taskData.get("status").asText(), "审批通过后应为 COMPLETED");
    }
}
