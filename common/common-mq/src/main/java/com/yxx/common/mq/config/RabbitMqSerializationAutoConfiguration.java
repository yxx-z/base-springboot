package com.yxx.common.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxx.common.mq.properties.RabbitPublisherProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ 消息序列化自动配置。
 *
 * <p>该配置先于 Spring Boot 的 Rabbit 自动配置加载，使 {@link MessageConverter} 和
 * {@link RabbitTemplateCustomizer} 能够参与 RabbitTemplate 及监听容器的标准装配过程。
 * 交换机、队列和绑定属于业务拓扑，不在公共模块中声明。</p>
 */
@AutoConfiguration(before = RabbitAutoConfiguration.class)
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(RabbitPublisherProperties.class)
public class RabbitMqSerializationAutoConfiguration {

    /**
     * 使用应用统一 ObjectMapper 进行 JSON 消息转换，保持日期、枚举和自定义模块行为一致。
     *
     * @param objectMapper 应用统一 Jackson ObjectMapper
     * @return AMQP JSON 消息转换器
     */
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter rabbitJsonMessageConverter(ObjectMapper objectMapper) {
        // 只信任当前项目命名空间中的类型标识，避免消费不可信消息时按任意类名反序列化。
        // 需要接入外部契约的应用可以声明自己的 MessageConverter 完整替换该默认 Bean。
        return new Jackson2JsonMessageConverter(objectMapper, "com.yxx.*");
    }

    /**
     * 配置无法路由消息必须返回生产者，避免消息在交换机端静默丢失。
     *
     * @param properties 可靠发布配置
     * @return RabbitTemplate 定制器
     */
    @Bean
    public RabbitTemplateCustomizer rabbitMandatoryTemplateCustomizer(
            RabbitPublisherProperties properties) {
        return rabbitTemplate -> rabbitTemplate.setMandatory(properties.isMandatory());
    }
}
