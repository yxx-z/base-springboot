package com.yxx.business.service;

import com.yxx.business.model.entity.Menu;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:04
 */
public interface MenuService {

    /** 查询所有已启用菜单，包含隐藏节点，以便正确解析可见子菜单的祖先链。 */
    List<Menu> findEnabledMenus();

    /** 查询指定主键中已启用的菜单，用于角色菜单授权校验。 */
    List<Menu> findEnabledByIds(Collection<Integer> ids);
}
