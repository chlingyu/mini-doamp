package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.CompareType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWarnStrategy implements WarnStrategy {

    protected List<WarnRecord> checkValue(Long indexId, String indexType,
                                          BigDecimal value, String groupKey,
                                          List<WarnThreshold> thresholds) {
        List<WarnRecord> records = new ArrayList<>();
        for (WarnThreshold t : thresholds) {
            if (isTriggered(value, t)) {
                WarnRecord r = new WarnRecord();
                r.setIndexId(indexId);
                r.setIndexType(indexType);
                r.setWarnLevel(t.getLevel());
                r.setCurrentValue(value);
                r.setThresholdValue(describeThreshold(t));
                r.setGroupKey(groupKey);
                r.setWarnTime(LocalDateTime.now());
                r.setCreateTime(LocalDateTime.now());
                records.add(r);
            }
        }
        return records;
    }

    private boolean isTriggered(BigDecimal value, WarnThreshold t) {
        CompareType ct = CompareType.valueOf(t.getCompareType());
        switch (ct) {
            case GT:      return value.compareTo(t.getUpperLimit()) > 0;
            case LT:      return value.compareTo(t.getLowerLimit()) < 0;
            case GTE:     return value.compareTo(t.getUpperLimit()) >= 0;
            case LTE:     return value.compareTo(t.getLowerLimit()) <= 0;
            case EQ:      return value.compareTo(t.getUpperLimit()) == 0;
            case BETWEEN: return value.compareTo(t.getLowerLimit()) >= 0
                              && value.compareTo(t.getUpperLimit()) <= 0;
            default:      return false;
        }
    }

    private String describeThreshold(WarnThreshold t) {
        CompareType ct = CompareType.valueOf(t.getCompareType());
        switch (ct) {
            case GT:      return "> " + t.getUpperLimit();
            case LT:      return "< " + t.getLowerLimit();
            case GTE:     return ">= " + t.getUpperLimit();
            case LTE:     return "<= " + t.getLowerLimit();
            case EQ:      return "= " + t.getUpperLimit();
            case BETWEEN: return "[" + t.getLowerLimit() + ", " + t.getUpperLimit() + "]";
            default:      return "";
        }
    }
}
