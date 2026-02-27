package com.demo.minidoamp.event.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // ========== Exchange ==========
    public static final String WARN_EXCHANGE = "warn.exchange";
    public static final String WARN_DLX_EXCHANGE = "warn.dlx.exchange";

    // ========== Queue ==========
    public static final String SMS_QUEUE = "sms.queue";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String WXWORK_QUEUE = "wxwork.queue";
    public static final String WARN_DLQ = "warn.dlq";

    // ========== Routing Key ==========
    public static final String RK_SMS = "warn.sms";
    public static final String RK_EMAIL = "warn.email";
    public static final String RK_WXWORK = "warn.wxwork";

    @Bean
    public TopicExchange warnExchange() {
        return new TopicExchange(WARN_EXCHANGE);
    }

    @Bean
    public DirectExchange warnDlxExchange() {
        return new DirectExchange(WARN_DLX_EXCHANGE);
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .deadLetterExchange(WARN_DLX_EXCHANGE)
                .deadLetterRoutingKey("dlq")
                .build();
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .deadLetterExchange(WARN_DLX_EXCHANGE)
                .deadLetterRoutingKey("dlq")
                .build();
    }

    @Bean
    public Queue wxworkQueue() {
        return QueueBuilder.durable(WXWORK_QUEUE)
                .deadLetterExchange(WARN_DLX_EXCHANGE)
                .deadLetterRoutingKey("dlq")
                .build();
    }

    @Bean
    public Queue warnDlq() {
        return QueueBuilder.durable(WARN_DLQ).build();
    }

    // ========== Binding: 主队列 → Topic Exchange ==========
    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange warnExchange) {
        return BindingBuilder.bind(smsQueue).to(warnExchange).with(RK_SMS);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange warnExchange) {
        return BindingBuilder.bind(emailQueue).to(warnExchange).with(RK_EMAIL);
    }

    @Bean
    public Binding wxworkBinding(Queue wxworkQueue, TopicExchange warnExchange) {
        return BindingBuilder.bind(wxworkQueue).to(warnExchange).with(RK_WXWORK);
    }

    // ========== Binding: DLQ → DLX Exchange ==========
    @Bean
    public Binding dlqBinding(Queue warnDlq, DirectExchange warnDlxExchange) {
        return BindingBuilder.bind(warnDlq).to(warnDlxExchange).with("dlq");
    }
}
