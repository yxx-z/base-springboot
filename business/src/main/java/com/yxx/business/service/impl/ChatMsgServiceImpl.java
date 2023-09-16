package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.ChatMsgMapper;
import com.yxx.business.model.entity.ChatMsg;
import com.yxx.business.netty.Msg;
import com.yxx.business.service.ChatMsgService;
import com.yxx.common.enums.netty.MsgSignFlagEnum;
import com.yxx.common.utils.auth.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天消息service实现类
 *
 * @author yxx
 * @classname ChatMsgServiceImpl
 * @since 2023-09-16 14:39
 */
@Slf4j
@Service
public class ChatMsgServiceImpl extends ServiceImpl<ChatMsgMapper, ChatMsg> implements ChatMsgService {

    /**
     * 保存聊天记录
     *
     * @param msg 聊天记录
     * @return {@link String }
     * @author yxx
     */
    @Override
    public String saveMsg(Msg msg) {
        ChatMsg msgDB = new ChatMsg();
        msgDB.setAcceptUserId(msg.getReceiverId());
        msgDB.setSendUserId(msg.getSenderId());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(msg.getMessage());

        save(msgDB);
        return String.valueOf(msgDB.getId());
    }

    /**
     * 批量已读消息
     *
     * @param msgIdList 消息id集合
     * @author yxx
     */
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        this.baseMapper.batchUpdateMsgSigned(msgIdList);
    }

    /**
     * 获取未读消息
     *
     * @return {@link List }<{@link ChatMsg }>
     * @author yxx
     */
    @Override
    public List<ChatMsg> getUnReadMsgList() {
        return list(new LambdaQueryWrapper<ChatMsg>().eq(ChatMsg::getSignFlag, Boolean.FALSE)
                .eq(ChatMsg::getAcceptUserId, LoginUtils.getUserId()));
    }
}
