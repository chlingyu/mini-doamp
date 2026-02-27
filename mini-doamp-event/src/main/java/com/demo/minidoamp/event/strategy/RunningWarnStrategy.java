package com.demo.minidoamp.event.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.IndexRunning;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexRunningMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RunningWarnStrategy extends AbstractWarnStrategy {

    private final IndexRunningMapper indexRunningMapper;

    @Override
    public IndexType getType() {
        return IndexType.RUNNING;
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        IndexRunning latest = indexRunningMapper.selectOne(
                new LambdaQueryWrapper<IndexRunning>()
                        .eq(IndexRunning::getIndexCode, index.getIndexCode())
                        .orderByDesc(IndexRunning::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        return checkValue(index.getId(), index.getIndexType(),
                latest.getIndexValue(), null, thresholds);
    }
}
