package com.yxx.business.service;

import com.yxx.business.model.entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author yxx
 * @since 2023-05-17 09:58
 */
public interface RoleService {

    /** 根据角色编码查询角色。 */
    Optional<Role> findByCode(String code);

    /** 根据角色编码批量查询角色主键。 */
    List<Integer> findIdsByCodes(Collection<String> codes);

    /** 根据角色主键批量查询角色。 */
    List<Role> findByIds(Collection<Integer> ids);
}
