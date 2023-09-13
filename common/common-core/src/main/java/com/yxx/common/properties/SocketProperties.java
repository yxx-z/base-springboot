package com.yxx.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * socket配置
 *
 * @author yxx
 * @classname SocketProperties
 * @since 2023-09-13 13:54
 */
@Data
@Component
@ConfigurationProperties(prefix = "netty.websocket")
public class SocketProperties {
    // 端口
    private Integer port;
    // ip
    private String ip;
    // 路径
    private String path;
    // 消息帧最大体积
    private Long maxFrameSize;
}
