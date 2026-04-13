package com.demo.minidoamp.event.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.IndexGroup;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 渠道效能类预警策略。
 * <p>
 * 差异逻辑：除了检查最新日期的绝对值，还计算与前一日的环比变化率。
 * 若环比变化率超出阈值范围，也产生预警记录（groupKey 标注 "渠道名[环比+15%]"）。
 */
@Component
public class ChannelWarnStrategy extends AbstractWarnStrategy {

    private final IndexGroupMapper indexGroupMapper;

    public ChannelWarnStrategy(IndexGroupMapper indexGroupMapper) {
        this.indexGroupMapper = indexGroupMapper;
    }

    @Override
    public String getType() {
        return IndexType.CHANNEL.getCode();
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        // 取最新两个日期
        List<IndexGroup> dates = indexGroupMapper.selectList(
                new LambdaQueryWrapper<IndexGroup>()
                        .select(IndexGroup::getDataDate)
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "CHANNEL")
                        .groupBy(IndexGroup::getDataDate)
                        .orderByDesc(IndexGroup::getDataDate)
                        .last("LIMIT 2"));
        if (dates.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDate latestDate = dates.get(0).getDataDate();

        // 查最新日期所有渠道数据
        List<IndexGroup> latestGroups = indexGroupMapper.selectList(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "CHANNEL")
                        .eq(IndexGroup::getDataDate, latestDate));

        List<WarnRecord> records = new ArrayList<>();

        // 绝对值对比
        for (IndexGroup g : latestGroups) {
            records.addAll(checkValue(index.getId(), index.getIndexType(),
                    g.getIndexValue(), g.getGroupKey(), thresholds));
        }

        // 环比变化率对比（独立阈值，避免与绝对值阈值单位混用）
        // 变化率超 ±20% 视为异常波动，产生趋势预警
        if (dates.size() >= 2) {
            LocalDate prevDate = dates.get(1).getDataDate();
            List<IndexGroup> prevGroups = indexGroupMapper.selectList(
                    new LambdaQueryWrapper<IndexGroup>()
                            .eq(IndexGroup::getIndexCode, index.getIndexCode())
                            .eq(IndexGroup::getGroupType, "CHANNEL")
                            .eq(IndexGroup::getDataDate, prevDate));

            // 构建 Map 提升匹配效率 O(n) 替代 O(n²)
            java.util.Map<String, BigDecimal> prevMap = new java.util.HashMap<>();
            for (IndexGroup prev : prevGroups) {
                if (prev.getGroupKey() != null) {
                    prevMap.put(prev.getGroupKey(), prev.getIndexValue());
                }
            }

            BigDecimal rateThreshold = BigDecimal.valueOf(20); // ±20% 固定阈值
            for (IndexGroup curr : latestGroups) {
                if (curr.getGroupKey() == null)
                    continue;
                BigDecimal prevValue = prevMap.get(curr.getGroupKey());
                if (prevValue != null && prevValue.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal changeRate = curr.getIndexValue()
                            .subtract(prevValue)
                            .divide(prevValue.abs(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    // 变化率超出固定阈值时，独立生成趋势预警记录
                    if (changeRate.abs().compareTo(rateThreshold) > 0) {
                        String changeLabel = String.format("%s[环比%+.1f%%]",
                                curr.getGroupKey(), changeRate.doubleValue());
                        WarnRecord r = new WarnRecord();
                        r.setIndexId(index.getId());
                        r.setIndexType(index.getIndexType());
                        r.setWarnLevel(2); // 趋势预警级别
                        r.setCurrentValue(changeRate);
                        r.setThresholdValue("环比变化率超 ±" + rateThreshold + "%");
                        r.setGroupKey(changeLabel);
                        r.setWarnTime(java.time.LocalDateTime.now());
                        r.setCreateTime(java.time.LocalDateTime.now());
                        records.add(r);
                    }
                }
            }
        }

        return records;
    }
}
