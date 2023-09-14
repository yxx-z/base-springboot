package com.yxx.business.model.response;

import lombok.Data;

/**
 * 阿里userId
 *
 * @author yxx
 * @classname AliUserIdRes
 * @since 2023-09-14 10:31
 */
@Data
public class AliUserIdRes {
    // 根据authCode换取的用户唯一标识userId
    private String userId;
}
