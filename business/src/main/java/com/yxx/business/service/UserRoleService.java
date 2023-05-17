package com.yxx.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserRole;

import java.util.List;

/**
 * @author yxx
 * @since 2023-05-17 10:00
 */
public interface UserRoleService extends IService<UserRole> {

    /**
     * 根据用户信息 获取该用户角色权限
     *
     * @param user 用户信息
     * @return 用户角色code集合
     */
    List<String> loginUserRoleManage(User user);
}
