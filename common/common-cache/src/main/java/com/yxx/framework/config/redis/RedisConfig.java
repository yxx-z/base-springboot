package com.yxx.framework.config.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置。
 *
 * <p>类型反序列化仅允许项目自身类型和必要的 JDK 容器、时间类型，禁止使用无约束的
 * {@code LaissezFaireSubTypeValidator}，降低缓存被越权写入后的反序列化风险。</p>
 */
@EnableCaching
@Configuration
public class RedisConfig {

    /**
     * 创建业务 RedisTemplate。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @param redisValueSerializer   受限 JSON 序列化器
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            RedisSerializer<Object> redisValueSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(redisValueSerializer);
        redisTemplate.setHashKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setHashValueSerializer(redisValueSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 创建带白名单约束的 Redis JSON 序列化器。
     *
     * @param applicationObjectMapper Spring Boot 管理的 ObjectMapper
     * @return Redis 值序列化器
     */
    @Bean
    public RedisSerializer<Object> redisValueSerializer(ObjectMapper applicationObjectMapper) {
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.yxx.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build();

        ObjectMapper redisObjectMapper = applicationObjectMapper.copy();
        redisObjectMapper.activateDefaultTyping(
                validator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }
}
