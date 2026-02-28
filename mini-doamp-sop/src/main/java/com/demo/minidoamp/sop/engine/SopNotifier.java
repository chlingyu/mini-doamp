package com.demo.minidoamp.sop.engine;

import com.demo.minidoamp.core.entity.MsgRecord;
import com.demo.minidoamp.core.entity.SopTask;
import com.demo.minidoamp.core.enums.MsgStatus;
import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SopNotifier {

    private static final String SOP_EXCHANGE = "warn.exchange";
    private static final String SOP_RK = "warn.email";

    private final RabbitTemplate rabbitTemplate;
    private final MsgRecordMapper msgRecordMapper;

    public void send(SopTask task, String fromStatus, String toStatus, Long operatorId) {
        MsgRecord record = new MsgRecord();
        record.setMsgId(UUID.randomUUID().toString());
        record.setNotifyType("EMAIL");
        record.setReceiverId(operatorId != null ? operatorId : 0L);
        record.setTitle("SOP任务状态变更");
        record.setContent(String.format("任务[%s] %s→%s", task.getTaskName(), fromStatus, toStatus));
        record.setStatus(MsgStatus.PENDING.getCode());
        record.setRetryCount(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        msgRecordMapper.insert(record);

        try {
            rabbitTemplate.convertAndSend(SOP_EXCHANGE, SOP_RK, record.getMsgId());
            log.info("SOP通知已发送: taskId=, msgId={}, {}→{}", task.getId(), record.getMsgId(), fromStatus, toStatus);
        } catch (Exception e) {
            record.setStatus(MsgStatus.FAILED.getCode());
            record.setFailReason("MQ投递失败: " + e.getMessage());
            record.setUpdateTime(LocalDateTime.now());
            msgRecordMapper.updateById(record);
            log.warn("SOP通知发送失败: taskId={}, msgId={}", task.getId(), record.getMsgId(), e);
        }
    }
}
