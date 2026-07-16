package com.yxx.business.service;

import com.yxx.business.model.entity.User;

/**
 * @author yxx
 * @since 2023-05-17 10:00
 */
public interface UserRoleService {

    /**
     * 设置默认角色: 用户
     *
     * @param user 用户信息
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean assignMemberRole(User user);

}
