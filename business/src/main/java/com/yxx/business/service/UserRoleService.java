package com.yxx.business.service;

import com.yxx.business.model.entity.User;

import java.util.Collection;
import java.util.List;

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

    /**
     * 替换用户角色并注销该用户全部会话，使旧授权快照立即失效。
     *
     * @param userId  用户主键
     * @param roleIds 新角色主键集合
     */
    void replaceRoles(Long userId, Collection<Integer> roleIds);

    /** 查询拥有指定角色的用户主键，用于权限变更后的会话失效。 */
    List<Long> listUserIdsByRoleId(Integer roleId);

    /** 查询指定用户当前拥有的角色主键。 */
    List<Integer> listRoleIdsByUserId(Long userId);
}
