package com.demo.minidoamp.doamp.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.core.adapter.DatabaseAdapter;
import com.demo.minidoamp.core.adapter.DatabaseAdapterFactory;
import com.demo.minidoamp.core.mapper.DemoDialectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多库适配演示接口
 */
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
public class DatabaseAdapterController {

    private final DataSource dataSource;
    private final DatabaseIdProvider databaseIdProvider;
    private final DatabaseAdapterFactory adapterFactory;
    private final DemoDialectMapper demoDialectMapper;

    /**
     * 查询当前数据库类型信息
     */
    @GetMapping("/info")
    public R<Map<String, Object>> info() throws SQLException {
        String databaseId = databaseIdProvider.getDatabaseId(dataSource);
        DatabaseAdapter adapter = adapterFactory.getAdapter(databaseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("databaseId", databaseId);
        result.put("adapterClass", adapter.getClass().getSimpleName());
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            result.put("productName", metaData.getDatabaseProductName());
            result.put("productVersion", metaData.getDatabaseProductVersion());
        }
        return R.ok(result);
    }

    /**
     * 演示不同数据库方言生成的 SQL 片段对比
     */
    @GetMapping("/dialect-demo")
    public R<Map<String, Object>> dialectDemo() throws SQLException {
        String databaseId = databaseIdProvider.getDatabaseId(dataSource);
        DatabaseAdapter adapter = adapterFactory.getAdapter(databaseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentDatabase", databaseId);

        // 演示日期格式化差异
        Map<String, String> dateFormatDemo = new LinkedHashMap<>();
        dateFormatDemo.put("input", "dateFormat('create_time', 'yyyy-MM-dd')");
        dateFormatDemo.put("output", adapter.dateFormat("create_time", "yyyy-MM-dd"));
        result.put("dateFormat", dateFormatDemo);

        // 演示分页语法差异
        Map<String, String> paginateDemo = new LinkedHashMap<>();
        paginateDemo.put("input", "paginate('SELECT * FROM t_warn_record', 10, 20)");
        paginateDemo.put("output", adapter.paginate("SELECT * FROM t_warn_record", 10, 20));
        result.put("paginate", paginateDemo);

        // 演示聚合函数差异
        Map<String, String> groupConcatDemo = new LinkedHashMap<>();
        groupConcatDemo.put("input", "groupConcat('index_name')");
        groupConcatDemo.put("output", adapter.groupConcat("index_name"));
        result.put("groupConcat", groupConcatDemo);

        return R.ok(result);
    }

    /**
     * 演示 databaseId 差异化 SQL 执行 — 日期格式化查询
     */
    @GetMapping("/dialect-query/date")
    public R<List<Map<String, Object>>> dialectQueryDate() {
        return R.ok(demoDialectMapper.selectFormattedWarnDate(5));
    }

    /**
     * 演示 databaseId 差异化 SQL 执行 — 分组聚合查询
     */
    @GetMapping("/dialect-query/group")
    public R<List<Map<String, Object>>> dialectQueryGroup() {
        return R.ok(demoDialectMapper.selectGroupedIndexNames());
    }
}
