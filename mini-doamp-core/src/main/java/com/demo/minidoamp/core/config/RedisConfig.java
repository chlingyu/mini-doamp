package com.demo.minidoamp.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * 延迟双删专用线程池，单线程即可
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService delayDeleteExecutor() {
        return new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "cache-delay-delete");
            t.setDaemon(true);
            return t;
        });
    }
}
