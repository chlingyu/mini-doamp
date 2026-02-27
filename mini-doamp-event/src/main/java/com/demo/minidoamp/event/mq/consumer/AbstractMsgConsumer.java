package com.demo.minidoamp.event.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.MsgRecord;
import com.demo.minidoamp.core.enums.MsgStatus;
import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMsgConsumer {

    private static final String IDEMPOTENT_KEY_PREFIX = "msg:idempotent:";
    private static final long IDEMPOTENT_TTL_HOURS = 24;

    protected final MsgRecordMapper msgRecordMapper;
    protected final StringRedisTemplate redisTemplate;

    protected abstract String getChannel();

    protected void handleMessage(String msgId, Channel channel, long deliveryTag) throws IOException {
        // 1. SETNX 原子幂等：返回 false 说明已消费过
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + msgId;
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (absent == null || !absent) {
            log.info("[{}] 消息已消费，跳过 msgId={}", getChannel(), msgId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        // 2. 查消息记录
        MsgRecord record = msgRecordMapper.selectOne(
                new LambdaQueryWrapper<MsgRecord>().eq(MsgRecord::getMsgId, msgId));
        if (record == null) {
            log.warn("[{}] 消息记录不存在 msgId={}", getChannel(), msgId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        // 3. Mock 发送
        try {
            mockSend(record);
            record.setStatus(MsgStatus.SENT.getCode());
            record.setSendTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            msgRecordMapper.updateById(record);
            log.info("[{}] 发送成功 msgId={} receiver={}", getChannel(), msgId, record.getReceiverName());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            record.setStatus(MsgStatus.FAILED.getCode());
            record.setFailReason(e.getMessage());
            record.setUpdateTime(LocalDateTime.now());
            msgRecordMapper.updateById(record);
            // 从幂等 key 删除，允许重试时再次消费
            redisTemplate.delete(idempotentKey);
            log.error("[{}] 发送失败 msgId={}", getChannel(), msgId, e);
            // NACK + requeue=false → 进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    protected void mockSend(MsgRecord record) {
        log.info("[{}] 模拟发送: 接收人={}, 联系方式={}, 标题={}, 内容={}",
                getChannel(), record.getReceiverName(), record.getReceiverContact(),
                record.getTitle(), record.getContent());
    }
}
