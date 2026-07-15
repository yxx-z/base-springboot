package com.yxx.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.entity.AdminUserRole;

import java.util.List;
import java.util.Collection;

/**
 * @author yxx
 * @since 2023-05-17 10:00
 */
public interface AdminUserRoleService extends IService<AdminUserRole> {

    /**
     * 根据用户信息 获取该用户角色权限
     *
     * @param user 用户信息
     * @return 用户角色code集合
     */
    List<String> loginUserRoleManage(AdminUser user);

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
}
