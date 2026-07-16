package com.yxx.admin.security;

import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.spi.RbacSubjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** admin 应用对管理端账号主体存在性的领域适配器。 */
@Component
@RequiredArgsConstructor
public class AdminUserRbacSubjectValidator implements RbacSubjectValidator {

    private final AdminUserMapper adminUserMapper;

    @Override
    public boolean supports(String subjectType) {
        return RbacSubjectType.ADMIN_USER.code().equals(subjectType);
    }

    @Override
    public boolean exists(Long subjectId) {
        return subjectId != null && adminUserMapper.selectById(subjectId) != null;
    }
}
