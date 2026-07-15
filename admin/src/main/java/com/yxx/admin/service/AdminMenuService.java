package com.yxx.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.admin.model.entity.AdminMenu;

import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:04
 */
public interface AdminMenuService extends IService<AdminMenu> {
    /**
     * 菜单树
     * @return 菜单树
     */
    List<AdminMenu> menuTree();
}
