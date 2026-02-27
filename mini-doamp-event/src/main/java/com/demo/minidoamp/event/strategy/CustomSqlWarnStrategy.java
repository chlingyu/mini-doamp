package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.event.util.CustomSqlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomSqlWarnStrategy extends AbstractWarnStrategy {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public IndexType getType() {
        return IndexType.CUSTOM_SQL;
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        CustomSqlValidator.validate(index.getCustomSql());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(index.getCustomSql());
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
