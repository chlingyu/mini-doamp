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
 * 银行类预警策略。
 * <p>
 * 差异逻辑：按银行分组查询最新日期数据，对每家银行独立对比阈值，
 * 额外记录超限银行数量和占比信息到 groupKey 中。
 */
@Component
public class BankWarnStrategy extends AbstractWarnStrategy {

    private final IndexGroupMapper indexGroupMapper;

    public BankWarnStrategy(IndexGroupMapper indexGroupMapper) {
        this.indexGroupMapper = indexGroupMapper;
    }

    @Override
    public IndexType getType() {
        return IndexType.BANK;
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        // 取该指标+银行分组下的最新日期
        IndexGroup latest = indexGroupMapper.selectOne(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "BANK")
                        .orderByDesc(IndexGroup::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        LocalDate latestDate = latest.getDataDate();

        // 查该日期所有银行数据
        List<IndexGroup> groups = indexGroupMapper.selectList(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "BANK")
                        .eq(IndexGroup::getDataDate, latestDate));

        List<WarnRecord> records = new ArrayList<>();
        int totalBanks = groups.size();
        int breachCount = 0;

        // 逐家银行对比阈值
        for (IndexGroup g : groups) {
            List<WarnRecord> bankRecords = checkValue(index.getId(), index.getIndexType(),
                    g.getIndexValue(), g.getGroupKey(), thresholds);
            if (!bankRecords.isEmpty()) {
                breachCount++;
            }
            records.addAll(bankRecords);
        }

        // 在每条记录的 groupKey 中追加超限占比（如 "工商银行[2/5=40%]"）
        if (!records.isEmpty() && totalBanks > 0) {
            String suffix = String.format("[%d/%d=%d%%]",
                    breachCount, totalBanks, breachCount * 100 / totalBanks);
            for (WarnRecord r : records) {
                r.setGroupKey(r.getGroupKey() + suffix);
            }
        }
        return records;
    }
}
