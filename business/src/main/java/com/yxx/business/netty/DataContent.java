package com.yxx.business.netty;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author yxx
 * @classname DataContent
 * @since 2023/09/16
 */
@Data
public class DataContent implements Serializable {

	@Serial
	private static final long serialVersionUID = 8021381444738260454L;

	/**
	 * 动作类型
	 */
	private Integer action;

	/**
	 * 用户的聊天内容entity
	 */
	private Msg chatMsg;

	/**
	 * 扩展字段
	 */
	private String extend;
}
