package com.yxx.business.netty;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户id和channel的关联关系处理
 *
 * @author yxx
 * @classname UserChannelRel
 * @since 2023/09/16
 */
@Slf4j
public class UserChannelRel {

	private static final ConcurrentHashMap<String, Channel> manager = new ConcurrentHashMap<>();

	public static void put(String senderId, Channel channel) {
		manager.put(senderId, channel);
	}

	public static Channel get(String senderId) {
		return manager.get(senderId);
	}

	public static void output() {
		Set<Map.Entry<String, Channel>> entries = manager.entrySet();
		for (Map.Entry<String, Channel> entry : entries) {
			log.info("UserId:{}, ChannelId:{}",entry.getKey(), entry.getValue().id().asLongText());
		}
	}
}
