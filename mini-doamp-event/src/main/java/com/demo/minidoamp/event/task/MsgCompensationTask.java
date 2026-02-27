package com.demo.minidoamp.event.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.MsgRecord;
import com.demo.minidoamp.core.enums.MsgStatus;
import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import com.demo.minidoamp.event.mq.producer.WarnMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MsgCompensationTask {

    private static final int MAX_RETRY = 3;

    private final MsgRecordMapper msgRecordMapper;
    private final WarnMessageProducer messageProducer;

    @Scheduled(fixedDelay = 60000)
    public void compensate() {
        List<MsgRecord> failedRecords = msgRecordMapper.selectList(
                new LambdaQueryWrapper<MsgRecord>()
                        .eq(MsgRecord::getStatus, MsgStatus.FAILED.getCode()));

        if (failedRecords.isEmpty()) return;
        log.info("MsgCompensation found {} records to retry", failedRecords.size());

        for (MsgRecord record : failedRecords) {
            // 先判断：retryCount 已达上限则置 ALARM，不再重投
            if (record.getRetryCount() >= MAX_RETRY) {
                record.setStatus(MsgStatus.ALARM.getCode());
                record.setUpdateTime(LocalDateTime.now());
                msgRecordMapper.updateById(record);
                log.warn("MsgCompensation msgId={} exceeded max retry, set ALARM", record.getMsgId());
                continue;
            }

            // 累加 retryCount 并重投
            record.setRetryCount(record.getRetryCount() + 1);
            record.setStatus(MsgStatus.RETRYING.getCode());
            record.setUpdateTime(LocalDateTime.now());
            msgRecordMapper.updateById(record);

            try {
                messageProducer.republish(record);
                log.info("MsgCompensation republished msgId={} retryCount={}", record.getMsgId(), record.getRetryCount());
            } catch (Exception e) {
                record.setStatus(MsgStatus.FAILED.getCode());
                record.setFailReason("补偿重试投递失败: " + e.getMessage());
                record.setUpdateTime(LocalDateTime.now());
                msgRecordMapper.updateById(record);
                log.error("MsgCompensation republish failed msgId={}", record.getMsgId(), e);
            }
        }
    }
}
