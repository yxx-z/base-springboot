package com.yxx.admin.service;

import com.yxx.admin.model.response.AdminMenuRes;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:23
 */
public interface AdminRoleMenuService {
    /** 根据当前管理员角色构建已启用、可见的导航菜单树。 */
    List<AdminMenuRes> currentMenuTree(Collection<String> roleCodes);

    /** 替换角色菜单关联，并在写入前校验角色和菜单有效性。 */
    void replaceMenus(Integer roleId, Collection<Integer> menuIds);
}
