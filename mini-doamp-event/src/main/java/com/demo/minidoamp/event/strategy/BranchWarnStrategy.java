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
import java.util.Collections;
import java.util.List;

/**
 * 营业部类预警策略。
 * <p>
 * 差异逻辑：不逐个营业部检查，而是计算所有营业部的算术均值，
 * 用整体均值对比阈值。预警记录的 groupKey 标注营业部数量。
 * 适用于需要从全局视角监控整体水位的场景。
 */
@Component
public class BranchWarnStrategy extends AbstractWarnStrategy {

    private final IndexGroupMapper indexGroupMapper;

    public BranchWarnStrategy(IndexGroupMapper indexGroupMapper) {
        this.indexGroupMapper = indexGroupMapper;
    }

    @Override
    public IndexType getType() {
        return IndexType.BRANCH;
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        // 取最新日期
        IndexGroup latest = indexGroupMapper.selectOne(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "BRANCH")
                        .orderByDesc(IndexGroup::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        LocalDate latestDate = latest.getDataDate();

        // 查该日期所有营业部数据
        List<IndexGroup> groups = indexGroupMapper.selectList(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "BRANCH")
                        .eq(IndexGroup::getDataDate, latestDate));

        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算所有营业部的均值
        BigDecimal sum = BigDecimal.ZERO;
        for (IndexGroup g : groups) {
            sum = sum.add(g.getIndexValue());
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(groups.size()), 4, RoundingMode.HALF_UP);

        // 用均值对比阈值，groupKey 标注营业部数量
        String groupKey = String.format("全部营业部均值(共%d家)", groups.size());
        return checkValue(index.getId(), index.getIndexType(), avg, groupKey, thresholds);
    }
}
