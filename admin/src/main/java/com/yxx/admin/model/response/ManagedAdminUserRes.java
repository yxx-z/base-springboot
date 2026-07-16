package com.yxx.admin.model.response;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端管理员账号响应，明确排除密码摘要。 */
public record ManagedAdminUserRes(
        Long id,
        String loginCode,
        String loginName,
        Boolean status,
        String linkPhone,
        String email,
        String ipHomePlace,
        String agent,
        LocalDateTime createTime,
        List<Integer> roleIds) {
}
