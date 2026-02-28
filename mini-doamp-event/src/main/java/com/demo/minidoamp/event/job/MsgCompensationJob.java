package com.demo.minidoamp.event.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.JobExecLog;
import com.demo.minidoamp.core.entity.MsgRecord;
import com.demo.minidoamp.core.enums.MsgStatus;
import com.demo.minidoamp.core.mapper.JobExecLogMapper;
import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import com.demo.minidoamp.event.mq.producer.WarnMessageProducer;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MsgCompensationJob {

    private static final int MAX_RETRY = 3;

    private final MsgRecordMapper msgRecordMapper;
    private final WarnMessageProducer messageProducer;
    private final JobExecLogMapper jobExecLogMapper;

    @XxlJob("msgCompensationHandler")
    public void execute() {
        long start = System.currentTimeMillis();
        List<MsgRecord> failedRecords = msgRecordMapper.selectList(
                new LambdaQueryWrapper<MsgRecord>()
                        .eq(MsgRecord::getStatus, MsgStatus.FAILED.getCode()));

        int retried = 0, alarmed = 0;
        for (MsgRecord record : failedRecords) {
            if (record.getRetryCount() >= MAX_RETRY) {
                record.setStatus(MsgStatus.ALARM.getCode());
                record.setUpdateTime(LocalDateTime.now());
                msgRecordMapper.updateById(record);
                alarmed++;
                continue;
            }

            record.setRetryCount(record.getRetryCount() + 1);
            record.setStatus(MsgStatus.RETRYING.getCode());
            record.setUpdateTime(LocalDateTime.now());
            msgRecordMapper.updateById(record);

            try {
                messageProducer.republish(record);
                retried++;
            } catch (Exception e) {
                record.setStatus(MsgStatus.FAILED.getCode());
                record.setFailReason("补偿重试投递失败: " + e.getMessage());
                record.setUpdateTime(LocalDateTime.now());
                msgRecordMapper.updateById(record);
                log.error("MsgCompensation republish failed msgId={}", record.getMsgId(), e);
            }
        }

        JobExecLog execLog = new JobExecLog();
        execLog.setJobName("msg_compensation");
        execLog.setStatus(1);
        execLog.setMessage("total=" + failedRecords.size() + " retried=" + retried + " alarmed=" + alarmed);
        execLog.setCostMs(System.currentTimeMillis() - start);
        execLog.setCreateTime(LocalDateTime.now());
        jobExecLogMapper.insert(execLog);
    }
}
