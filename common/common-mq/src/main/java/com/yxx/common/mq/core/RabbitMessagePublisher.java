package com.yxx.common.mq.core;

import com.yxx.common.mq.exception.RabbitMessagePublishException;
import com.yxx.common.mq.properties.RabbitPublisherProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 可靠消息发布器。
 *
 * <p>发布方法同时检查 Broker confirm 和 mandatory return。调用方只有在本方法正常返回后，
 * 才能认为 Broker 已接收且消息已成功路由；消费端处理成功仍需由具体业务的消费确认保证。</p>
 *
 * <p>使用本发布器时必须配置：</p>
 * <pre>
 * spring.rabbitmq.publisher-confirm-type=correlated
 * spring.rabbitmq.publisher-returns=true
 * </pre>
 */
public class RabbitMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;

    public RabbitMessagePublisher(RabbitTemplate rabbitTemplate,
                                  RabbitPublisherProperties properties) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "RabbitTemplate 不能为空");
        this.confirmTimeout = Objects.requireNonNull(
                properties.getConfirmTimeout(), "RabbitMQ 发布确认超时时间不能为空");
    }

    /**
     * 发布 JSON 消息并等待 Broker 确认。
     *
     * @param exchange   目标交换机
     * @param routingKey 路由键
     * @param payload    待发送业务消息
     * @return 自动生成的消息唯一标识
     * @throws RabbitMessagePublishException 消息发送、确认或路由失败
     */
    public String publishAndConfirm(String exchange, String routingKey, Object payload) {
        requireText(exchange, "RabbitMQ 交换机不能为空");
        requireText(routingKey, "RabbitMQ 路由键不能为空");
        Objects.requireNonNull(payload, "RabbitMQ 消息体不能为空");

        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    payload,
                    message -> applyMessageMetadata(message, messageId),
                    correlationData);
        } catch (RuntimeException exception) {
            throw new RabbitMessagePublishException(
                    messageId, "RabbitMQ 消息发送失败", exception);
        }

        CorrelationData.Confirm confirm = awaitConfirm(messageId, correlationData);
        ReturnedMessage returnedMessage = correlationData.getReturned();
        if (returnedMessage != null) {
            throw new RabbitMessagePublishException(messageId,
                    "RabbitMQ 消息无法路由，replyCode=" + returnedMessage.getReplyCode()
                            + "，replyText=" + returnedMessage.getReplyText()
                            + "，exchange=" + returnedMessage.getExchange()
                            + "，routingKey=" + returnedMessage.getRoutingKey());
        }
        if (!confirm.isAck()) {
            throw new RabbitMessagePublishException(messageId,
                    "RabbitMQ Broker 拒绝消息，原因=" + confirm.getReason());
        }
        return messageId;
    }

    private CorrelationData.Confirm awaitConfirm(
            String messageId, CorrelationData correlationData) {
        try {
            return correlationData.getFuture().get(
                    confirmTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RabbitMessagePublishException(
                    messageId, "等待 RabbitMQ 发布确认时线程被中断", exception);
        } catch (TimeoutException exception) {
            throw new RabbitMessagePublishException(
                    messageId, "等待 RabbitMQ 发布确认超时", exception);
        } catch (ExecutionException exception) {
            throw new RabbitMessagePublishException(
                    messageId, "获取 RabbitMQ 发布确认失败", exception.getCause());
        }
    }

    private Message applyMessageMetadata(Message message, String messageId) {
        message.getMessageProperties().setMessageId(messageId);
        message.getMessageProperties().setTimestamp(Date.from(Instant.now()));
        return message;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
