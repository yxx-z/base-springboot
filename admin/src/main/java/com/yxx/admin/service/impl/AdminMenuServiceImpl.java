package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminMenuMapper;
import com.yxx.admin.model.entity.AdminMenu;
import com.yxx.admin.service.AdminMenuService;
import com.yxx.common.utils.TreeUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:05
 */
@Service
public class AdminMenuServiceImpl extends ServiceImpl<AdminMenuMapper, AdminMenu> implements AdminMenuService {

    @Override
    public List<AdminMenu> menuTree() {
        List<AdminMenu> adminMenuList = list();
        /*
         * 构建树
         *
         * @param listData 需要构建的结果集              这里是menuList
         * @param parentKeyFunction 父节点             这里是parentId
         * @param keyFunction 主键                     这里是id
         * @param setChildrenFunction 子集             这里是children
         * @param rootParentValue 父节点的值            这里null
         * @return java.util.List<T>
         */
        return TreeUtil.buildTree(adminMenuList, AdminMenu::getParentId, AdminMenu::getId, AdminMenu::setChildren, null);
    }
}
