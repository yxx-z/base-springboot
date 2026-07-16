package com.yxx.admin.service;

import com.yxx.admin.model.request.AdminUserPageReq;
import com.yxx.admin.model.request.CreateAdminUserReq;
import com.yxx.admin.model.request.UpdateAdminUserReq;
import com.yxx.admin.model.response.ManagedAdminUserRes;
import com.yxx.common.core.page.PageResponse;

import java.util.Collection;

/** 管理员账号管理应用服务。 */
public interface AdminUserManagementService {

    PageResponse<ManagedAdminUserRes> page(AdminUserPageReq request);

    ManagedAdminUserRes findById(Long userId);

    Long create(CreateAdminUserReq request);

    void update(Long userId, UpdateAdminUserReq request);

    void changeStatus(Long userId, boolean enabled);

    void replaceRoles(Long userId, Collection<Integer> roleIds);

    void delete(Long userId);
}
