package com.yxx.admin.service;

import com.yxx.admin.model.entity.AdminRole;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author yxx
 * @since 2023-05-17 09:58
 */
public interface AdminRoleService {

    Optional<AdminRole> findByCode(String code);

    List<Integer> findIdsByCodes(Collection<String> codes);

    List<AdminRole> findByIds(Collection<Integer> ids);
}
