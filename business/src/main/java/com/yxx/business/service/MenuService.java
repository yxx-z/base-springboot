package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.Menu;

import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:04
 */
public interface MenuService extends IService<Menu> {
    /**
     * 菜单树
     * @return 菜单树
     */
    List<Menu> menuTree();
}
