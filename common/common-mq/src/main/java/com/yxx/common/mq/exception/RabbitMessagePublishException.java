package com.yxx.common.mq.exception;

/** RabbitMQ 消息未获得可靠投递确认时抛出的异常。 */
public class RabbitMessagePublishException extends RuntimeException {

    private final String messageId;

    public RabbitMessagePublishException(String messageId, String message) {
        super(message);
        this.messageId = messageId;
    }

    public RabbitMessagePublishException(String messageId, String message, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
    }

    /**
     * 获取本次发送使用的消息唯一标识，便于日志、补偿任务和问题排查关联。
     *
     * @return 消息唯一标识
     */
    public String getMessageId() {
        return messageId;
    }
}
