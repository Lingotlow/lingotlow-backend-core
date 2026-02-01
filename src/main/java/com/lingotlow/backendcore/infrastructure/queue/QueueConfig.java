package com.lingotlow.backendcore.infrastructure.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class QueueConfig {

    @Value("${QUEUE_URL:redis://localhost:6379}")
    private String queueUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Parse QUEUE_URL if provided, otherwise use individual properties
        if (queueUrl != null && !queueUrl.equals("redis://localhost:6379")) {
            return createFromUrl();
        } else {
            return createFromProperties();
        }
    }

    private RedisConnectionFactory createFromUrl() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        JedisConnectionFactory factory = new JedisConnectionFactory(poolConfig);

        // Parse URL format: redis://[password@]host:port/database
        if (queueUrl.contains("@")) {
            String[] parts = queueUrl.replace("redis://", "").split("@");
            redisPassword = parts[0];
            String[] hostPort = parts[1].split("/");
            String[] hostPortParts = hostPort[0].split(":");
            redisHost = hostPortParts[0];
            redisPort = Integer.parseInt(hostPortParts[1]);
            if (hostPort.length > 1) {
                redisDatabase = Integer.parseInt(hostPort[1]);
            }
        } else {
            String[] parts = queueUrl.replace("redis://", "").split("/");
            String[] hostPort = parts[0].split(":");
            redisHost = hostPort[0];
            redisPort = Integer.parseInt(hostPort[1]);
            if (parts.length > 1) {
                redisDatabase = Integer.parseInt(parts[1]);
            }
        }

        factory.setHostName(redisHost);
        factory.setPort(redisPort);
        factory.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            factory.setPassword(redisPassword);
        }

        factory.afterPropertiesSet();
        return factory;
    }

    private RedisConnectionFactory createFromProperties() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        JedisConnectionFactory factory = new JedisConnectionFactory(poolConfig);
        factory.setHostName(redisHost);
        factory.setPort(redisPort);
        factory.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            factory.setPassword(redisPassword);
        }

        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
