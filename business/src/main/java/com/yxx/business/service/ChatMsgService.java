package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.ChatMsg;
import com.yxx.business.netty.Msg;

import java.util.List;

/**
 * @author yxx
 * @since 2023-09-16 14:38
 */
public interface ChatMsgService extends IService<ChatMsg> {

    /**
     * 保存聊天记录
     *
     * @param chatMsg 聊天记录
     * @return {@link String }
     * @author yxx
     */
    String saveMsg(Msg chatMsg);

    /**
     * 批量签收消息
     *
     * @param msgIdList 消息id集合
     * @author yxx
     */
    void updateMsgSigned(List<String> msgIdList);

    /**
     * 获取未读消息列表
     * @return {@link List }<{@link ChatMsg }>
     * @author yxx
     */
    List<ChatMsg> getUnReadMsgList();

}
