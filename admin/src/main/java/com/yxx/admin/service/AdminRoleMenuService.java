package com.yxx.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.admin.model.entity.AdminRoleMenu;

import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:23
 */
public interface AdminRoleMenuService extends IService<AdminRoleMenu> {
    /**
     * 根据角色code集合 获取菜单code集合
     *
     * @param roleCodeList 角色code集合
     * @return 菜单code集合
     */
    List<String> loginUserMenu(List<String> roleCodeList);
}
