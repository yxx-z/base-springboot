package com.yxx.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.admin.model.entity.AdminRoleMenu;
import com.yxx.admin.model.response.AdminMenuRes;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:23
 */
public interface AdminRoleMenuService extends IService<AdminRoleMenu> {
    /** 根据当前管理员角色构建已启用、可见的导航菜单树。 */
    List<AdminMenuRes> currentMenuTree(Collection<String> roleCodes);
}
