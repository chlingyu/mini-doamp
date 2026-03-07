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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 运行类预警策略。
 * <p>
 * 差异逻辑：取最新一条数据直接对比阈值。
 * 额外查询最近 7 条记录计算滑动均值，若均值也超限则产生趋势预警。
 */
@Component
@RequiredArgsConstructor
public class RunningWarnStrategy extends AbstractWarnStrategy {

    private static final int TREND_WINDOW = 7;

    private final IndexRunningMapper indexRunningMapper;

    @Override
    public IndexType getType() {
        return IndexType.RUNNING;
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        // 取最新一条
        IndexRunning latest = indexRunningMapper.selectOne(
                new LambdaQueryWrapper<IndexRunning>()
                        .eq(IndexRunning::getIndexCode, index.getIndexCode())
                        .orderByDesc(IndexRunning::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }

        List<WarnRecord> records = new ArrayList<>();

        // 最新值直接对比
        records.addAll(checkValue(index.getId(), index.getIndexType(),
                latest.getIndexValue(), null, thresholds));

        // 近 N 条滑动均值趋势检查
        List<IndexRunning> recentList = indexRunningMapper.selectList(
                new LambdaQueryWrapper<IndexRunning>()
                        .eq(IndexRunning::getIndexCode, index.getIndexCode())
                        .orderByDesc(IndexRunning::getDataDate)
                        .last("LIMIT " + TREND_WINDOW));
        if (recentList.size() >= 2) {
            BigDecimal sum = BigDecimal.ZERO;
            for (IndexRunning r : recentList) {
                sum = sum.add(r.getIndexValue());
            }
            BigDecimal avg = sum.divide(BigDecimal.valueOf(recentList.size()), 4, RoundingMode.HALF_UP);
            String trendKey = String.format("近%d期均值", recentList.size());
            records.addAll(checkValue(index.getId(), index.getIndexType(),
                    avg, trendKey, thresholds));
        }

        return records;
    }
}
