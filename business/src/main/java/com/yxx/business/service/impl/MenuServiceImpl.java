package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.MenuMapper;
import com.yxx.business.model.entity.Menu;
import com.yxx.business.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collection;

/**
 * @author yxx
 * @since 2023-05-18 15:05
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<Menu> findEnabledMenus() {
        return list(new LambdaQueryWrapper<Menu>().eq(Menu::getStatus, Boolean.TRUE));
    }

    @Override
    public List<Menu> findEnabledByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<Menu>()
                .in(Menu::getId, ids)
                .eq(Menu::getStatus, Boolean.TRUE));
    }
}
