package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.MenuMapper;
import com.yxx.business.model.entity.Menu;
import com.yxx.business.service.MenuService;
import com.yxx.common.utils.TreeUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yxx
 * @since 2023-05-18 15:05
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<Menu> menuTree() {
        List<Menu> menuList = list();
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
        return TreeUtil.buildTree(menuList, Menu::getParentId, Menu::getId, Menu::setChildren, null);
    }
}
