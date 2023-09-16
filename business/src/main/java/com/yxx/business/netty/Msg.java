package com.yxx.business.netty;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author yxx
 * @classname Msg
 * @since 2023/09/16
 */
@Data
public class Msg implements Serializable {

	@Serial
	private static final long serialVersionUID = 3611169682695799175L;

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 发送者的用户id
	 */
	private String senderId;

	/**
	 * 接受者的用户id
	 */
	private String receiverId;

	/**
	 * 聊天内容
	 */
	private String message;

	/**
	 * 用于消息的签收
	 */
	private String msgId;
}
