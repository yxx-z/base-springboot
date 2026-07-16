package com.yxx.business.service.impl;

import com.yxx.business.model.entity.User;
import com.yxx.business.service.UserRoleService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.constant.RbacSecurityCodes;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.service.RbacRoleService;
import com.yxx.rbac.service.RbacSubjectRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author yxx
 * @since 2023-05-17 10:01
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {
    private final RbacRoleService roleService;
    private final RbacSubjectRoleService subjectRoleService;

    @Override
    public Boolean assignMemberRole(User user) {
        // 基础成员角色由迁移脚本初始化；缺失意味着环境不完整，不能创建无角色用户。
        RbacRole role = roleService.findByCode(
                RbacScope.BUSINESS, RbacSecurityCodes.ROLE_BUSINESS_MEMBER).orElse(null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, role != null);
        // 新用户只建立最小默认角色关联，后续扩展角色由独立管理流程替换。
        return subjectRoleService.assignRole(
                RbacSubjectType.BUSINESS_USER.code(), user.getId(), role.getId());
    }
}
