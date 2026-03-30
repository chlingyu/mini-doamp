package com.demo.minidoamp.integration;

import com.demo.minidoamp.core.adapter.DatabaseAdapter;
import com.demo.minidoamp.core.adapter.DatabaseAdapterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库适配层集成测试。
 * 验证简历第5条（多库适配）：
 * - DatabaseAdapterFactory 在 H2 环境下通过 databaseId="h2" 返回 H2Adapter
 * - 方言方法返回 H2 语法（FORMATDATETIME / LIMIT / LISTAGG）
 * - /api/db/info 接口返回正确 databaseId
 */
@DisplayName("数据库 Adapter 多库适配")
class DatabaseAdapterTest extends BaseIntegrationTest {

    @Autowired
    private DatabaseAdapterFactory adapterFactory;

    @Test
    @DisplayName("H2 模式下 → Adapter 类型应为 H2Adapter")
    void adapterShouldBeH2() {
        DatabaseAdapter adapter = adapterFactory.getAdapter("h2");
        assertNotNull(adapter, "adapter 不应为空");
        assertTrue(adapter.getClass().getSimpleName().contains("H2"),
                "H2 模式下应返回 H2Adapter，实际: " + adapter.getClass().getSimpleName());
    }

    @Test
    @DisplayName("H2 方言：dateFormat 返回 FORMATDATETIME")
    void dateFormatShouldUseH2Syntax() {
        DatabaseAdapter adapter = adapterFactory.getAdapter("h2");
        String result = adapter.dateFormat("create_time", "%Y-%m");
        assertNotNull(result);
        assertTrue(result.toUpperCase().contains("FORMATDATETIME"),
                "H2 dateFormat 应使用 FORMATDATETIME，实际: " + result);
    }

    @Test
    @DisplayName("H2 方言：paginate 返回 LIMIT/OFFSET")
    void paginateShouldUseLimitOffset() {
        DatabaseAdapter adapter = adapterFactory.getAdapter("h2");
        String result = adapter.paginate("SELECT * FROM t", 10, 20);
        assertNotNull(result);
        assertTrue(result.toUpperCase().contains("LIMIT"),
                "H2 paginate 应包含 LIMIT，实际: " + result);
    }

    @Test
    @DisplayName("H2 方言：groupConcat 返回 LISTAGG")
    void groupConcatShouldUseLISTAGG() {
        DatabaseAdapter adapter = adapterFactory.getAdapter("h2");
        String result = adapter.groupConcat("name");
        assertNotNull(result);
        assertTrue(result.toUpperCase().contains("LISTAGG"),
                "H2 groupConcat 应使用 LISTAGG，实际: " + result);
    }

    @Test
    @DisplayName("/api/db/info → databaseId=h2")
    void dbInfoEndpointShouldReturnH2() throws Exception {
        String token = loginAndGetToken();
        ResponseEntity<String> resp = getWithToken("/api/db/info", token);
        assertEquals(200, resp.getStatusCodeValue());
        JsonNode data = getData(resp);
        assertEquals("h2", data.get("databaseId").asText(), "databaseId 应为 h2");
    }
}
