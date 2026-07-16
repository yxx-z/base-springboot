package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.rbac.mapper.RbacRoleMapper;
import com.yxx.rbac.constant.RbacSecurityCodes;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacRole;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 统一角色查询服务，所有查询都显式携带权限域。 */
@Service
@RequiredArgsConstructor
public class RbacRoleService {

    private final RbacRoleMapper roleMapper;

    /** 根据权限域和稳定编码查询角色。 */
    public Optional<RbacRole> findByCode(RbacScope scope, String code) {
        return Optional.ofNullable(roleMapper.selectOne(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .eq(RbacRole::getCode, code)));
    }

    /** 查询权限域内给定角色编码对应的主键。 */
    public List<Integer> findIdsByCodes(RbacScope scope, Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .in(RbacRole::getCode, codes)).stream()
                .map(RbacRole::getId)
                .toList();
    }

    /** 按权限域批量加载角色，跨域主键不会出现在结果中。 */
    public List<RbacRole> findByIds(RbacScope scope, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .in(RbacRole::getId, ids));
    }

    /** 查询指定权限域的全部有效角色，供管理端配置授权。 */
    public List<RbacRole> listByScope(RbacScope scope) {
        return roleMapper.selectList(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .orderByAsc(RbacRole::getId));
    }

    /** 按主键查询未删除角色；调用方仍需根据业务场景校验权限域。 */
    public Optional<RbacRole> findById(Integer roleId) {
        return Optional.ofNullable(roleId == null ? null : roleMapper.selectById(roleId));
    }

    /**
     * 判断角色是否为系统唯一认可的内置超级管理员角色。
     *
     * <p>授权、菜单和最后管理员保护必须复用同一规则，不能一处按编码、一处按布尔字段。</p>
     */
    public boolean isCanonicalSuperRole(RbacRole role) {
        return role != null
                && RbacScope.ADMIN.code().equals(role.getScope())
                && RbacSecurityCodes.ROLE_ADMIN_SUPER_ADMIN.equals(role.getCode())
                && Boolean.TRUE.equals(role.getBuiltIn())
                && Boolean.TRUE.equals(role.getSuperRole());
    }
}
