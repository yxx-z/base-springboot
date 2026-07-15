package com.yxx.admin.service;

import com.yxx.admin.model.entity.AdminUser;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-17 10:00
 */
public interface AdminUserRoleService {

    /**
     * 设置默认角色: 用户
     *
     * @param user 用户信息
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean setDefaultRole(AdminUser user);

    /**
     * 替换管理员角色并注销其全部会话，使旧权限快照立即失效。
     *
     * @param userId  管理员主键
     * @param roleIds 新角色主键集合
     */
    void replaceRoles(Long userId, Collection<Integer> roleIds);

    /** 查询拥有指定角色的管理员主键，用于权限变更后的会话失效。 */
    List<Long> listUserIdsByRoleId(Integer roleId);

    /** 查询指定管理员当前拥有的角色主键。 */
    List<Integer> listRoleIdsByUserId(Long userId);
}
