package com.concurrency.locking.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.mockito.Mockito;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Fallback and test configuration for distributed infrastructure components.
 * Allows local compilation and tests without requiring external Redis or ZooKeeper clusters.
 */
@Configuration
public class MockInfrastructureConfig {

    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            // Default single-server configuration for local dev
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379")
                    .setConnectTimeout(1000)
                    .setTimeout(1000);
            return Redisson.create(config);
        } catch (Exception e) {
            // Provide Mockito mock if local Redis instance is not reachable
            return Mockito.mock(RedissonClient.class);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public CuratorFramework curatorFramework() {
        try {
            return CuratorFrameworkFactory.builder()
                    .connectString("127.0.0.1:2181")
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .connectionTimeoutMs(1000)
                    .sessionTimeoutMs(1000)
                    .build();
        } catch (Exception e) {
            return Mockito.mock(CuratorFramework.class);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate() {
        try {
            RedisConnectionFactory factory = new LettuceConnectionFactory("127.0.0.1", 6379);
            ((LettuceConnectionFactory) factory).afterPropertiesSet();
            return new StringRedisTemplate(factory);
        } catch (Exception e) {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }
}
