package com.yxx.business.controller;

import com.yxx.business.model.entity.ChatMsg;
import com.yxx.business.service.ChatMsgService;
import com.yxx.common.annotation.response.ResponseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天接口
 *
 * @author yxx
 */
@Slf4j
@Validated
@ResponseResult
@RestController
@RequestMapping("/chatMsg")
@RequiredArgsConstructor
public class ChatMsgController {

    private final ChatMsgService chatMsgService;

    /**
     * 用户手机端获取未签收的消息列表
     *
     * @return {@link List }<{@link ChatMsg }>
     * @author yxx
     */
    @PostMapping("/getUnReadMsgList")
    public List<ChatMsg> getUnReadMsgList() {
        return chatMsgService.getUnReadMsgList();
    }
}
