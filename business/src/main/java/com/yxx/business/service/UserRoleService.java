package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserRole;

import java.util.List;
import java.util.Collection;

/**
 * @author yxx
 * @since 2023-05-17 10:00
 */
public interface UserRoleService extends IService<UserRole> {

    /**
     * 根据用户信息 获取该用户角色权限
     *
     * @param user 用户信息
     * @return 用户角色code集合
     */
    List<String> loginUserRoleManage(User user);

    /**
     * 设置默认角色: 用户
     *
     * @param user 用户信息
     * @return {@link Boolean }
     * @author yxx
     */
    Boolean setDefaultRole(User user);

    /**
     * 替换用户角色并注销该用户全部会话，使旧授权快照立即失效。
     *
     * @param userId  用户主键
     * @param roleIds 新角色主键集合
     */
    void replaceRoles(Long userId, Collection<Integer> roleIds);
}
