package com.yxx.admin.service;

import com.yxx.admin.model.entity.AdminMenu;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:04
 */
public interface AdminMenuService {

    /** 查询所有已启用菜单，包含隐藏节点。 */
    List<AdminMenu> findEnabledMenus();

    /** 查询指定主键中已启用的菜单。 */
    List<AdminMenu> findEnabledByIds(Collection<Integer> ids);
}
