package com.yxx.admin.model.response;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端业务用户视图，不包含任何认证凭据。 */
public record ManagedBusinessUserRes(
        Long id,
        String displayName,
        String avatar,
        Boolean status,
        String phone,
        String email,
        String ipHomePlace,
        String agent,
        LocalDateTime createTime,
        List<Integer> roleIds) {

    public ManagedBusinessUserRes {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
