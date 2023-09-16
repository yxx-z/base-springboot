package com.yxx.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yxx.business.model.entity.ChatMsg;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author yxx
 * @since 2023-09-16 14:38
 */
public interface ChatMsgMapper extends BaseMapper<ChatMsg> {
    void batchUpdateMsgSigned(@Param("list") List<String> msgIdList);
}
