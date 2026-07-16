package com.yxx.admin.service;

import com.yxx.admin.model.response.RbacMenuRes;
import com.yxx.admin.model.response.RbacPermissionRes;
import com.yxx.admin.model.response.RbacRoleRes;
import com.yxx.rbac.model.RbacScope;

import java.util.List;

/** 管理端统一 RBAC 配置查询服务。 */
public interface RbacConfigurationQueryService {

    List<RbacRoleRes> listRoles(RbacScope scope);

    List<RbacPermissionRes> listPermissions(RbacScope scope);

    List<RbacMenuRes> listMenus(RbacScope scope);
}
