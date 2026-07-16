package com.yxx.common.mq.core;

import com.yxx.common.mq.exception.RabbitMessagePublishException;
import com.yxx.common.mq.properties.RabbitPublisherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitMessagePublisherTest {

    @Test
    void shouldReturnMessageIdAfterBrokerAck() {
        RabbitMessagePublisher publisher = new RabbitMessagePublisher(
                new StubRabbitTemplate(ConfirmBehavior.ACK),
                properties(Duration.ofSeconds(1)));

        String messageId = assertDoesNotThrow(() -> publisher.publishAndConfirm(
                "framework.events", "user.created", new TestMessage("1001")));
        assertEquals(36, messageId.length());
    }

    @Test
    void shouldRejectBrokerNack() {
        RabbitMessagePublisher publisher = new RabbitMessagePublisher(
                new StubRabbitTemplate(ConfirmBehavior.NACK),
                properties(Duration.ofSeconds(1)));

        RabbitMessagePublishException exception = assertThrows(
                RabbitMessagePublishException.class,
                () -> publisher.publishAndConfirm(
                        "framework.events", "user.created", new TestMessage("1001")));
        assertEquals("RabbitMQ Broker 拒绝消息，原因=exchange unavailable",
                exception.getMessage());
    }

    @Test
    void shouldFailWhenConfirmTimesOut() {
        RabbitMessagePublisher publisher = new RabbitMessagePublisher(
                new StubRabbitTemplate(ConfirmBehavior.NONE),
                properties(Duration.ofMillis(10)));

        RabbitMessagePublishException exception = assertThrows(
                RabbitMessagePublishException.class,
                () -> publisher.publishAndConfirm(
                        "framework.events", "user.created", new TestMessage("1001")));
        assertEquals("等待 RabbitMQ 发布确认超时", exception.getMessage());
    }

    private RabbitPublisherProperties properties(Duration confirmTimeout) {
        RabbitPublisherProperties properties = new RabbitPublisherProperties();
        properties.setConfirmTimeout(confirmTimeout);
        return properties;
    }

    private record TestMessage(String userId) {
    }

    /**
     * 只替换网络发送步骤，保留 CorrelationData 的真实 Future 行为，避免单元测试依赖
     * Mockito inline agent 或本机 RabbitMQ。
     */
    private static final class StubRabbitTemplate extends RabbitTemplate {

        private final ConfirmBehavior confirmBehavior;

        private StubRabbitTemplate(ConfirmBehavior confirmBehavior) {
            this.confirmBehavior = confirmBehavior;
        }

        @Override
        public void convertAndSend(String exchange, String routingKey, Object message,
                                   MessagePostProcessor messagePostProcessor,
                                   CorrelationData correlationData) {
            if (confirmBehavior == ConfirmBehavior.ACK) {
                correlationData.getFuture().complete(
                        new CorrelationData.Confirm(true, null));
            } else if (confirmBehavior == ConfirmBehavior.NACK) {
                correlationData.getFuture().complete(
                        new CorrelationData.Confirm(false, "exchange unavailable"));
            }
        }
    }

    private enum ConfirmBehavior {
        ACK,
        NACK,
        NONE
    }
}
