package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminMenuMapper;
import com.yxx.admin.model.entity.AdminMenu;
import com.yxx.admin.service.AdminMenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collection;

/**
 * @author yxx
 * @since 2023-05-18 15:05
 */
@Service
public class AdminMenuServiceImpl extends ServiceImpl<AdminMenuMapper, AdminMenu> implements AdminMenuService {

    @Override
    public List<AdminMenu> findEnabledMenus() {
        return list(new LambdaQueryWrapper<AdminMenu>().eq(AdminMenu::getStatus, Boolean.TRUE));
    }

    @Override
    public List<AdminMenu> findEnabledByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<AdminMenu>()
                .in(AdminMenu::getId, ids)
                .eq(AdminMenu::getStatus, Boolean.TRUE));
    }
}
