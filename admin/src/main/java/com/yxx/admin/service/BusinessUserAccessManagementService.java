package com.yxx.admin.service;

import com.yxx.admin.model.request.BusinessUserPageReq;
import com.yxx.admin.model.response.ManagedBusinessUserRes;
import com.yxx.common.core.page.PageResponse;

import java.util.Collection;

/** 管理端对业务用户及其授权关系的应用服务。 */
public interface BusinessUserAccessManagementService {

    /** 分页查询业务用户，并返回每个用户当前的业务角色。 */
    PageResponse<ManagedBusinessUserRes> page(BusinessUserPageReq request);

    /** 查询单个业务用户及其业务角色。 */
    ManagedBusinessUserRes findById(Long userId);

    /** 替换业务用户角色；只能分配 business 权限域角色。 */
    void replaceRoles(Long userId, Collection<Integer> roleIds);
}
