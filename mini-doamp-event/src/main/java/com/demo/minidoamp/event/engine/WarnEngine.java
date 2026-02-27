package com.demo.minidoamp.event.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnRule;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.WarnIndexMapper;
import com.demo.minidoamp.core.mapper.WarnRecordMapper;
import com.demo.minidoamp.core.mapper.WarnRuleMapper;
import com.demo.minidoamp.core.mapper.WarnThresholdMapper;
import com.demo.minidoamp.event.mq.producer.WarnMessageProducer;
import com.demo.minidoamp.event.strategy.WarnStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarnEngine {

    private final List<WarnStrategy> strategies;
    private final WarnIndexMapper indexMapper;
    private final WarnThresholdMapper thresholdMapper;
    private final WarnRuleMapper ruleMapper;
    private final WarnRecordMapper recordMapper;
    private final WarnMessageProducer messageProducer;

    private Map<IndexType, WarnStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(WarnStrategy::getType, s -> s));
        log.info("WarnEngine initialized with {} strategies: {}", strategyMap.size(), strategyMap.keySet());
    }

    public List<WarnRecord> check(Long ruleId) {
        WarnRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }

        WarnIndex index = indexMapper.selectById(rule.getIndexId());
        if (index == null) {
            throw new BusinessException(ErrorCode.INDEX_NOT_FOUND);
        }

        List<WarnThreshold> thresholds = thresholdMapper.selectList(
                new LambdaQueryWrapper<WarnThreshold>()
                        .eq(WarnThreshold::getIndexId, index.getId()));

        IndexType type = IndexType.valueOf(index.getIndexType());
        WarnStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        List<WarnRecord> records = strategy.check(index, thresholds);
        records.forEach(r -> {
            r.setRuleId(ruleId);
            recordMapper.insert(r);
        });

        // 触发消息推送
        if (!records.isEmpty() && StringUtils.hasText(rule.getNotifyType())) {
            records.forEach(r -> messageProducer.publish(r, rule));
        }

        log.info("WarnEngine.check ruleId={} indexType={} triggered={}", ruleId, type, records.size());
        return records;
    }
}
