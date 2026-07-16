package com.yxx.business.auth;

import com.yxx.business.mapper.UserMapper;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.spi.RbacSubjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** business 应用对业务用户主体存在性的领域适配器。 */
@Component
@RequiredArgsConstructor
public class BusinessUserRbacSubjectValidator implements RbacSubjectValidator {

    private final UserMapper userMapper;

    @Override
    public boolean supports(String subjectType) {
        return RbacSubjectType.BUSINESS_USER.code().equals(subjectType);
    }

    @Override
    public boolean exists(Long subjectId) {
        return subjectId != null && userMapper.selectById(subjectId) != null;
    }
}
