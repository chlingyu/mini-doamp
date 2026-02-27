package com.demo.minidoamp.event.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.IndexGroup;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractGroupWarnStrategy extends AbstractWarnStrategy {

    protected final IndexGroupMapper indexGroupMapper;

    protected abstract String getGroupType();

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        // 先取该指标+分组类型下的最新日期
        IndexGroup latest = indexGroupMapper.selectOne(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, getGroupType())
                        .orderByDesc(IndexGroup::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        LocalDate latestDate = latest.getDataDate();

        List<IndexGroup> groups = indexGroupMapper.selectList(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, getGroupType())
                        .eq(IndexGroup::getDataDate, latestDate));

        List<WarnRecord> records = new ArrayList<>();
        for (IndexGroup g : groups) {
            records.addAll(checkValue(index.getId(), index.getIndexType(),
                    g.getIndexValue(), g.getGroupKey(), thresholds));
        }
        return records;
    }
}
