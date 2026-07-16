package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.rbac.mapper.RbacRoleMapper;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.entity.RbacRole;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 统一角色查询服务，所有查询都显式携带权限域。 */
@Service
public class RbacRoleService extends ServiceImpl<RbacRoleMapper, RbacRole> {

    /** 根据权限域和稳定编码查询角色。 */
    public Optional<RbacRole> findByCode(RbacScope scope, String code) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .eq(RbacRole::getCode, code)));
    }

    /** 查询权限域内给定角色编码对应的主键。 */
    public List<Integer> findIdsByCodes(RbacScope scope, Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<RbacRole>()
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
        return list(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .in(RbacRole::getId, ids));
    }

    /** 查询指定权限域的全部有效角色，供管理端配置授权。 */
    public List<RbacRole> listByScope(RbacScope scope) {
        return list(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, scope.code())
                .orderByAsc(RbacRole::getId));
    }
}
