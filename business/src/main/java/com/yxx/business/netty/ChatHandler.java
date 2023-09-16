package com.yxx.business.netty;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.yxx.business.service.ChatMsgService;
import com.yxx.business.service.impl.ChatMsgServiceImpl;
import com.yxx.common.enums.netty.MsgActionEnum;
import com.yxx.common.utils.jackson.JacksonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 处理消息的handler
 * TextWebSocketFrame: 在netty中，是用于为websocket专门处理文本的对象，frame是消息的载体
 * @author yxx
 * @classname ChatHandler
 * @since 2023/09/16
 */
@Slf4j
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	// 用于记录和管理所有客户端的channel
	public static ChannelGroup users =
			new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
		log.info("read..........");
		// 获取客户端传输过来的消息
		String content = msg.text();

		Channel currentChannel = ctx.channel();

		// 1. 获取客户端发来的消息
		DataContent dataContent = JacksonUtil.readValue(content, DataContent.class);
		Integer action = dataContent.getAction();
		// 2. 判断消息类型，根据不同的类型来处理不同的业务

		if (Objects.equals(action, MsgActionEnum.CONNECT.type)) {
			// 	2.1  当websocket 第一次open的时候，初始化channel，把用的channel和userid关联起来
			String senderId = dataContent.getChatMsg().getSenderId();
			UserChannelRel.put(senderId, currentChannel);

			// 测试
			for (Channel c : users) {
				log.info("c.id().asLongText():{}", c.id().asLongText());
			}
			UserChannelRel.output();
		} else if (Objects.equals(action, MsgActionEnum.CHAT.type)) {
			//  2.2  聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]
			Msg chatMsg = dataContent.getChatMsg();
			String msgText = chatMsg.getMessage();
			log.info("消息内容:{}", msgText);
			String receiverId = chatMsg.getReceiverId();
			String senderId = chatMsg.getSenderId();
			log.info("发送人:{}", senderId);

			// 保存消息到数据库，并且标记为 未签收
			ChatMsgService chatMsgService = SpringUtil.getBean(ChatMsgServiceImpl.class);
			String msgId = chatMsgService.saveMsg(chatMsg);
			chatMsg.setMsgId(msgId);

			DataContent dataContentMsg = new DataContent();
			dataContentMsg.setChatMsg(chatMsg);

			// 发送消息
			// 从全局用户Channel关系中获取接受方的channel
			Channel receiverChannel = UserChannelRel.get(receiverId);
			if (receiverChannel == null) {
				// TODO channel为空代表用户离线，推送消息（JPush，个推，小米推送）
				log.info("用户离线");
			} else {
				// 当receiverChannel不为空的时候，从ChannelGroup去查找对应的channel是否存在
				Channel findChannel = users.find(receiverChannel.id());
				if (findChannel != null) {
					// 用户在线
					receiverChannel.writeAndFlush(
							new TextWebSocketFrame(
									JacksonUtil.toJson(dataContentMsg)));
				} else {
					// 用户离线 TODO 推送消息
					log.info("用户离线,推送消息");
				}
			}

		} else if (Objects.equals(action, MsgActionEnum.SIGNED.type)) {
			//  2.3  签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态[已签收]
			ChatMsgService chatMsgService = SpringUtil.getBean(ChatMsgServiceImpl.class);
			// 扩展字段在signed类型的消息中，代表需要去签收的消息id，逗号间隔
			String msgIdsStr = dataContent.getExtend();
			String[] msgIds = msgIdsStr.split(",");

			List<String> msgIdList = new ArrayList<>();
			for (String mid : msgIds) {
				if (StringUtils.isNotBlank(mid)) {
					msgIdList.add(mid);
				}
			}

			log.info("msgIdList:{}", JacksonUtil.toJson(msgIdList));

			if (CollUtil.isNotEmpty(msgIdList)) {
				// 批量签收
				chatMsgService.updateMsgSigned(msgIdList);
			}

		} else if (Objects.equals(action, MsgActionEnum.KEEPALIVE.type)) {
			//  2.4  心跳类型的消息
			log.info("收到来自channel为[" + currentChannel + "]的心跳包...");
		}
	}

	/**
	 * 当客户端连接服务端之后（打开连接）
	 * 获取客户端的channel，并且放到ChannelGroup中去进行管理
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		log.info("add.....");
		users.add(ctx.channel());
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {

		String channelId = ctx.channel().id().asShortText();
		log.info("客户端被移除，channelId为：" + channelId);

		// 当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
		users.remove(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		// 发生异常之后关闭连接（关闭channel），随后从ChannelGroup中移除
		ctx.channel().close();
		users.remove(ctx.channel());
	}
}
