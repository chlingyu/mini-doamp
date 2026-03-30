package com.demo.minidoamp.integration;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 测试基础设施配置：用内存 Mock 替代 Redis 和 RabbitMQ。
 */
@TestConfiguration
public class TestInfraConfig {

    private final ConcurrentHashMap<String, String> redisStore = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate mock = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

        when(mock.opsForValue()).thenReturn(valueOps);

        // SET with expiry
        Mockito.doAnswer(inv -> {
            redisStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // SETNX
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenAnswer(inv -> redisStore.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);

        // GET
        when(valueOps.get(anyString())).thenAnswer(inv -> redisStore.get(inv.getArgument(0)));

        // hasKey
        when(mock.hasKey(anyString())).thenAnswer(inv -> redisStore.containsKey(inv.getArgument(0)));

        // DELETE single key
        when(mock.delete(anyString())).thenAnswer(inv -> redisStore.remove(inv.getArgument(0)) != null);

        // DELETE collection of keys
        when(mock.delete(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<String> keys = inv.getArgument(0);
            long count = 0;
            for (String k : keys) {
                if (redisStore.remove(k) != null) count++;
            }
            return count;
        });

        // SCAN — return empty cursor (no keys to scan in test mock)
        @SuppressWarnings("unchecked")
        Cursor<String> emptyCursor = Mockito.mock(Cursor.class);
        when(emptyCursor.hasNext()).thenReturn(false);
        Mockito.doNothing().when(emptyCursor).close();
        when(mock.scan(any(ScanOptions.class))).thenReturn(emptyCursor);

        return mock;
    }

    @Bean
    @Primary
    public ConnectionFactory rabbitConnectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }
}
