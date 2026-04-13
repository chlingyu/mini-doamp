package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.event.util.CustomSqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSqlWarnStrategy extends AbstractWarnStrategy {

    private static final int QUERY_TIMEOUT_SECONDS = 10;
    private static final int MAX_ROWS = 1000;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getType() {
        return IndexType.CUSTOM_SQL.getCode();
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        CustomSqlValidator.validate(index.getCustomSql());

        // 使用 StatementCallback 在每个 Statement 上独立设置超时和行数上限，
        // 避免修改共享 JdbcTemplate 全局属性导致并发串扰
        List<Map<String, Object>> rows = jdbcTemplate.execute(
                (Statement stmt) -> {
                    stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    stmt.setMaxRows(MAX_ROWS);
                    ResultSet rs = stmt.executeQuery(index.getCustomSql());
                    List<Map<String, Object>> result = new ArrayList<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(meta.getColumnLabel(i).toLowerCase(), rs.getObject(i));
                        }
                        result.add(row);
                    }
                    return result;
                });

        log.info("CustomSql executed, rows={}, indexId={}", rows.size(), index.getId());

        List<WarnRecord> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object valObj = row.get("value");
            if (valObj == null) continue;
            BigDecimal value = new BigDecimal(valObj.toString());
            Object groupKeyObj = row.get("group_key");
            String groupKey = groupKeyObj != null ? groupKeyObj.toString() : null;
            records.addAll(checkValue(index.getId(), index.getIndexType(),
                    value, groupKey, thresholds));
        }
        return records;
    }
}

