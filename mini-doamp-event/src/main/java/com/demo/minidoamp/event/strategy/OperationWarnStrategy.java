package com.demo.minidoamp.event.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.IndexOperation;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexOperationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OperationWarnStrategy extends AbstractWarnStrategy {

    private final IndexOperationMapper indexOperationMapper;

    @Override
    public IndexType getType() {
        return IndexType.OPERATION;
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        IndexOperation latest = indexOperationMapper.selectOne(
                new LambdaQueryWrapper<IndexOperation>()
                        .eq(IndexOperation::getIndexCode, index.getIndexCode())
                        .orderByDesc(IndexOperation::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        return checkValue(index.getId(), index.getIndexType(),
                latest.getIndexValue(), null, thresholds);
    }
}
