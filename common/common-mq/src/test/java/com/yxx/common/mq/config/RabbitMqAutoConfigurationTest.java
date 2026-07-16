package com.yxx.common.mq.config;

import com.yxx.common.mq.core.RabbitMessagePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    RabbitMqSerializationAutoConfiguration.class,
                    RabbitAutoConfiguration.class,
                    RabbitMqPublisherAutoConfiguration.class))
            .withPropertyValues(
                    "spring.rabbitmq.publisher-confirm-type=correlated",
                    "spring.rabbitmq.publisher-returns=true");

    @Test
    void shouldConfigureJsonConverterAndReliablePublisherByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MessageConverter.class);
            assertThat(context.getBean(MessageConverter.class))
                    .isInstanceOf(Jackson2JsonMessageConverter.class);
            assertThat(context).hasSingleBean(RabbitMessagePublisher.class);
            assertThat(context.getBean(RabbitTemplate.class)
                    .isMandatoryFor(new Message(new byte[0]))).isTrue();
        });
    }

    @Test
    void shouldAllowApplicationToDisableCommonPublisher() {
        contextRunner
                .withPropertyValues("framework.rabbitmq.publisher.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RabbitMessagePublisher.class);
                    assertThat(context).hasSingleBean(RabbitTemplate.class);
                    assertThat(context).hasSingleBean(MessageConverter.class);
                });
    }

    @Test
    void shouldRejectMissingCorrelatedPublisherConfirm() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        RabbitMqSerializationAutoConfiguration.class,
                        RabbitAutoConfiguration.class,
                        RabbitMqPublisherAutoConfiguration.class))
                .withPropertyValues("spring.rabbitmq.publisher-returns=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
