package com.yxx.common.mq.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** RabbitMQ 可靠发布配置。 */
@ConfigurationProperties(prefix = "framework.rabbitmq.publisher")
public class RabbitPublisherProperties {

    /** 是否装配公共可靠发布器。 */
    private boolean enabled = true;

    /** 无法路由时是否要求 Broker 将消息退回生产者。 */
    private boolean mandatory = true;

    /** 等待 Broker publisher confirm 的最长时间。 */
    private Duration confirmTimeout = Duration.ofSeconds(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Duration getConfirmTimeout() {
        return confirmTimeout;
    }

    public void setConfirmTimeout(Duration confirmTimeout) {
        if (confirmTimeout == null || confirmTimeout.isZero() || confirmTimeout.isNegative()) {
            throw new IllegalArgumentException("RabbitMQ 发布确认超时时间必须大于 0");
        }
        this.confirmTimeout = confirmTimeout;
    }
}
