package com.demo.minidoamp.event.mq.consumer;

import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import com.demo.minidoamp.event.config.RabbitMqConfig;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EmailConsumer extends AbstractMsgConsumer {

    public EmailConsumer(MsgRecordMapper msgRecordMapper, StringRedisTemplate redisTemplate) {
        super(msgRecordMapper, redisTemplate);
    }

    @Override
    protected String getChannel() {
        return "EMAIL";
    }

    @RabbitListener(queues = RabbitMqConfig.EMAIL_QUEUE)
    public void consume(String msgId, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        handleMessage(msgId, channel, deliveryTag);
    }
}
