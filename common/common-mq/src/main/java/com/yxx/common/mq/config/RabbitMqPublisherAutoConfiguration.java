package com.yxx.common.mq.config;

import com.yxx.common.mq.core.RabbitMessagePublisher;
import com.yxx.common.mq.properties.RabbitPublisherProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** RabbitMQ 可靠消息发布器自动配置。 */
@AutoConfiguration(after = {RabbitAutoConfiguration.class,
        RabbitMqSerializationAutoConfiguration.class})
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(RabbitPublisherProperties.class)
public class RabbitMqPublisherAutoConfiguration {

    /**
     * 创建可靠消息发布器。应用可以声明同类型 Bean 完整替换默认实现。
     *
     * @param rabbitTemplate Spring AMQP 消息模板
     * @param properties     可靠发布配置
     * @return RabbitMQ 可靠消息发布器
     */
    @Bean
    @ConditionalOnSingleCandidate(RabbitTemplate.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "framework.rabbitmq.publisher", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RabbitMessagePublisher rabbitMessagePublisher(
            RabbitTemplate rabbitTemplate,
            RabbitPublisherProperties properties) {
        validateReliablePublisher(rabbitTemplate, properties);
        return new RabbitMessagePublisher(rabbitTemplate, properties);
    }

    /**
     * 校验 Spring Boot 默认连接工厂已启用 correlated confirm 和 publisher return。
     *
     * <p>如果应用提供自定义 ConnectionFactory，公共模块无法可靠判断其确认能力，交由
     * 应用自行保证；Spring Boot 默认 CachingConnectionFactory 则必须严格校验，避免所有
     * 消息在运行时等待超时或无法识别路由失败。</p>
     */
    private void validateReliablePublisher(
            RabbitTemplate rabbitTemplate,
            RabbitPublisherProperties properties) {
        ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        if (!(connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory)) {
            return;
        }
        if (!cachingConnectionFactory.isPublisherConfirms()
                || cachingConnectionFactory.isSimplePublisherConfirms()) {
            throw new IllegalStateException(
                    "使用 RabbitMessagePublisher 必须配置 "
                            + "spring.rabbitmq.publisher-confirm-type=correlated");
        }
        if (properties.isMandatory() && !cachingConnectionFactory.isPublisherReturns()) {
            throw new IllegalStateException(
                    "启用 RabbitMQ mandatory 发布时必须配置 "
                            + "spring.rabbitmq.publisher-returns=true");
        }
    }
}
