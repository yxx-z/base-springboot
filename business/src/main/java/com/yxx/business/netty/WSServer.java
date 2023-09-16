package com.yxx.business.netty;

import cn.hutool.extra.spring.SpringUtil;
import com.yxx.common.properties.SocketProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WSServer {
	private static class SingletionWSServer {
		static final WSServer instance = new WSServer();
	}

	@DependsOn(value = "SocketProperties")
	private static SocketProperties socketProperties(){
		return SpringUtil.getBean(SocketProperties.class);
	}

	public static WSServer getInstance() {
		return SingletionWSServer.instance;
	}

	private EventLoopGroup mainGroup;
	private EventLoopGroup subGroup;
	private ServerBootstrap server;
	private ChannelFuture future;

	public WSServer() {
		mainGroup = new NioEventLoopGroup();
		subGroup = new NioEventLoopGroup();
		server = new ServerBootstrap();
		server.group(mainGroup, subGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new WSServerInitial());
	}

	public void start() {
		this.future = server.bind(socketProperties().getPort());
		log.info("netty websocket server 启动完毕...");
	}
}
