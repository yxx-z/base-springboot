package com.yxx.admin.security;

import com.yxx.admin.mapper.ManagedBusinessUserMapper;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.spi.RbacSubjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** admin 应用对业务用户主体存在性的领域适配器。 */
@Component
@RequiredArgsConstructor
public class ManagedBusinessUserRbacSubjectValidator implements RbacSubjectValidator {

    private final ManagedBusinessUserMapper businessUserMapper;

    @Override
    public boolean supports(String subjectType) {
        return RbacSubjectType.BUSINESS_USER.code().equals(subjectType);
    }

    @Override
    public boolean exists(Long subjectId) {
        return subjectId != null && businessUserMapper.selectById(subjectId) != null;
    }
}
