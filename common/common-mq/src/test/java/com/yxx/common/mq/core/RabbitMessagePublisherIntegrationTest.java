package com.yxx.common.mq.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxx.common.mq.exception.RabbitMessagePublishException;
import com.yxx.common.mq.properties.RabbitPublisherProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 RabbitMQ 验证 JSON 消息、publisher confirm 和 mandatory return。 */
@Testcontainers(disabledWithoutDocker = true)
class RabbitMessagePublisherIntegrationTest {

    private static final String EXCHANGE = "framework.integration.events";
    private static final String QUEUE = "framework.integration.user-created";
    private static final String ROUTING_KEY = "user.created";

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.1-alpine"));

    private static CachingConnectionFactory connectionFactory;
    private static RabbitTemplate rabbitTemplate;
    private static RabbitMessagePublisher publisher;

    @BeforeAll
    static void setUpRabbitMq() {
        connectionFactory = new CachingConnectionFactory(
                RABBITMQ.getHost(), RABBITMQ.getAmqpPort());
        connectionFactory.setUsername(RABBITMQ.getAdminUsername());
        connectionFactory.setPassword(RABBITMQ.getAdminPassword());
        connectionFactory.setPublisherConfirmType(
                CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            // 退回详情由 CorrelationData 断言，测试回调只避免框架输出“无回调”告警。
        });
        rabbitTemplate.setReceiveTimeout(Duration.ofSeconds(3).toMillis());
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(
                new ObjectMapper().findAndRegisterModules(), "com.yxx.*"));

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        DirectExchange exchange = new DirectExchange(EXCHANGE, false, true);
        Queue queue = QueueBuilder.nonDurable(QUEUE).autoDelete().build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);

        RabbitPublisherProperties properties = new RabbitPublisherProperties();
        properties.setConfirmTimeout(Duration.ofSeconds(5));
        publisher = new RabbitMessagePublisher(rabbitTemplate, properties);
    }

    @AfterAll
    static void closeConnectionFactory() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldPublishAndReceiveJsonMessage() {
        String messageId = publisher.publishAndConfirm(
                EXCHANGE, ROUTING_KEY, new UserCreatedMessage("1001", "framework-user"));

        UserCreatedMessage received = rabbitTemplate.receiveAndConvert(
                QUEUE, new ParameterizedTypeReference<>() {
                });
        assertNotNull(messageId);
        assertEquals(new UserCreatedMessage("1001", "framework-user"), received);
    }

    @Test
    void shouldRejectUnroutableMessage() {
        RabbitMessagePublishException exception = assertThrows(
                RabbitMessagePublishException.class,
                () -> publisher.publishAndConfirm(
                        EXCHANGE, "missing.route", new UserCreatedMessage("1002", "nobody")));
        assertNotNull(exception.getMessageId());
        assertTrue(exception.getMessage().contains("RabbitMQ 消息无法路由"));
    }

    private record UserCreatedMessage(String userId, String account) {
    }
}
