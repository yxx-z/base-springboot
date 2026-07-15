package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.RoleMenu;
import com.yxx.business.model.response.MenuRes;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:23
 */
public interface RoleMenuService extends IService<RoleMenu> {
    /**
     * 根据当前用户角色构建已启用、可见的导航菜单树。
     *
     * @param roleCodes 当前用户角色编码
     * @return 用户导航菜单树
     */
    List<MenuRes> currentMenuTree(Collection<String> roleCodes);
}
