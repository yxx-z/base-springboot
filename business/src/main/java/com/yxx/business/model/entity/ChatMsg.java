package com.yxx.business.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 聊天记录
 *
 * @author yxx
 * @classname ChatMsg
 * @since 2023-09-16 14:14
 */
@Data
public class ChatMsg extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送人
     */
    private String sendUserId;

    /**
     * 接收人
     */
    private String acceptUserId;

    /**
     * 消息内容
     */
    private String msg;

    /**
     * 消息是否签收状态:1-签收;0-未签收
     */
    private Integer signFlag;
}
