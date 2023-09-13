package com.yxx.framework.socket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * @author yxx
 * @classname WebsocketMessageHandler
 * @since 2023/09/06
 */
@Slf4j
@Sharable
@Component
@RequiredArgsConstructor
public class WebsocketMessageHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketMessageHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
        if (msg instanceof TextWebSocketFrame textWebSocketFrame) {
            // 业务层处理数据
//            xxService.dispose(textWebSocketFrame.text());
            log.info("textWebSocketFrame.text():{}", textWebSocketFrame.text());
            // 响应客户端
            ctx.channel().writeAndFlush(new TextWebSocketFrame("我收到了你的消息：" + System.currentTimeMillis()));
        } else {
            // 不接受文本以外的数据帧类型
            ctx.channel().writeAndFlush(WebSocketCloseStatus.INVALID_MESSAGE_TYPE).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LOGGER.info("链接断开：{}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        LOGGER.info("链接创建：{}", ctx.channel().remoteAddress());
    }
}
