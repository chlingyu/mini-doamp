package com.demo.minidoamp.event.mq.producer;

import com.demo.minidoamp.core.entity.MsgRecord;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnRule;
import com.demo.minidoamp.core.enums.MsgStatus;
import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import com.demo.minidoamp.event.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarnMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MsgRecordMapper msgRecordMapper;
    private final SysUserMapper userMapper;

    public void publish(WarnRecord warnRecord, WarnRule rule) {
        if (!StringUtils.hasText(rule.getNotifyType()) || !StringUtils.hasText(rule.getReceiverIds())) {
            return;
        }

        String[] notifyTypes = rule.getNotifyType().split(",");
        String[] receiverIds = rule.getReceiverIds().split(",");

        for (String notifyType : notifyTypes) {
            notifyType = notifyType.trim();
            String routingKey = resolveRoutingKey(notifyType);
            if (routingKey == null) continue;

            for (String receiverIdStr : receiverIds) {
                Long receiverId = Long.parseLong(receiverIdStr.trim());
                SysUser user = userMapper.selectById(receiverId);
                String receiverName = user != null ? user.getRealName() : "";
                String contact = resolveContact(user, notifyType);

                // 1. 先落库 PENDING
                MsgRecord record = new MsgRecord();
                record.setMsgId(UUID.randomUUID().toString());
                record.setWarnRecordId(warnRecord.getId());
                record.setNotifyType(notifyType);
                record.setReceiverId(receiverId);
                record.setReceiverName(receiverName);
                record.setReceiverContact(contact);
                record.setTitle("预警通知");
                record.setContent(buildContent(warnRecord));
                record.setStatus(MsgStatus.PENDING.getCode());
                record.setRetryCount(0);
                record.setCreateTime(LocalDateTime.now());
                record.setUpdateTime(LocalDateTime.now());
                msgRecordMapper.insert(record);

                // 2. 投递到 MQ
                try {
                    rabbitTemplate.convertAndSend(RabbitMqConfig.WARN_EXCHANGE, routingKey, record.getMsgId());
                    log.info("MQ published msgId={} to {}", record.getMsgId(), routingKey);
                } catch (Exception e) {
                    // 投递失败，更新为 FAILED
                    record.setStatus(MsgStatus.FAILED.getCode());
                    record.setFailReason("MQ投递失败: " + e.getMessage());
                    record.setUpdateTime(LocalDateTime.now());
                    msgRecordMapper.updateById(record);
                    log.error("MQ publish failed msgId={}", record.getMsgId(), e);
                }
            }
        }
    }

    /** 重新投递（补偿重试 / 手动重试共用） */
    public void republish(MsgRecord record) {
        String routingKey = resolveRoutingKey(record.getNotifyType());
        if (routingKey == null) return;
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.WARN_EXCHANGE, routingKey, record.getMsgId());
            log.info("MQ republished msgId={} to {}", record.getMsgId(), routingKey);
        } catch (Exception e) {
            record.setStatus(MsgStatus.FAILED.getCode());
            record.setFailReason("MQ重投失败: " + e.getMessage());
            record.setUpdateTime(LocalDateTime.now());
            msgRecordMapper.updateById(record);
            log.error("MQ republish failed msgId={}", record.getMsgId(), e);
        }
    }

    private String resolveRoutingKey(String notifyType) {
        switch (notifyType) {
            case "SMS":    return RabbitMqConfig.RK_SMS;
            case "EMAIL":  return RabbitMqConfig.RK_EMAIL;
            case "WXWORK": return RabbitMqConfig.RK_WXWORK;
            default:       return null;
        }
    }

    private String resolveContact(SysUser user, String notifyType) {
        if (user == null) return "";
        switch (notifyType) {
            case "SMS":    return user.getPhone() != null ? user.getPhone() : "";
            case "EMAIL":  return user.getEmail() != null ? user.getEmail() : "";
            case "WXWORK": return user.getUsername();
            default:       return "";
        }
    }

    private String buildContent(WarnRecord r) {
        return String.format("指标[%s]触发%s级预警，当前值=%s，阈值条件=%s%s",
                r.getIndexType(),
                r.getWarnLevel(),
                r.getCurrentValue(),
                r.getThresholdValue(),
                r.getGroupKey() != null ? "，分组=" + r.getGroupKey() : "");
    }
}
