package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.rbac.mapper.RbacSubjectRoleMapper;
import com.yxx.rbac.model.RbacSubjectRef;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.model.entity.RbacSubjectRole;
import com.yxx.rbac.spi.RbacSubjectValidator;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.SessionInvalidationService;
import com.yxx.security.context.SessionInvalidationReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一主体角色服务。
 *
 * <p>该服务同时校验主体存在性、主体与角色权限域一致性，并在提交后注销旧会话。调用方
 * 不能绕过此服务直接写关联表，否则会破坏 RBAC 的核心安全边界。</p>
 */
@Service
public class RbacSubjectRoleService {

    private final RbacSubjectRoleMapper subjectRoleMapper;
    private final RbacRoleService roleService;
    private final Map<RbacSubjectType, RbacSubjectValidator> subjectValidators;
    private final SessionInvalidationService sessionInvalidationService;

    public RbacSubjectRoleService(RbacSubjectRoleMapper subjectRoleMapper,
                                  RbacRoleService roleService,
                                  List<RbacSubjectValidator> validators,
                                  SessionInvalidationService sessionInvalidationService) {
        this.subjectRoleMapper = subjectRoleMapper;
        this.roleService = roleService;
        this.subjectValidators = indexValidators(validators);
        this.sessionInvalidationService = sessionInvalidationService;
    }

    /** 给主体追加一个同权限域角色，主要用于新用户分配默认角色。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRole(String subjectTypeCode, Long subjectId, Integer roleId) {
        RbacSubjectType subjectType = validateSubject(subjectTypeCode, subjectId);
        ApiAssert.isTrue(ApiCode.RBAC_SCOPE_MISMATCH,
                roleService.findByIds(subjectType.scope(), List.of(roleId)).size() == 1);

        RbacSubjectRole relation = new RbacSubjectRole();
        relation.setSubjectType(subjectType.code());
        relation.setSubjectId(subjectId);
        relation.setScope(subjectType.scope().code());
        relation.setRoleId(roleId);
        boolean assigned = subjectRoleMapper.insert(relation) == 1;
        if (assigned) {
            // 新注册用户尚无会话时该操作没有副作用；若未来复用于存量主体，仍能保证快照失效。
            invalidateAfterCommit(subjectType, subjectId,
                    SessionInvalidationReason.SUBJECT_ROLE_CHANGED);
        }
        return assigned;
    }

    /**
     * 以最终集合语义替换主体角色。
     *
     * <p>全部输入验证在删除旧关联之前完成；空集合明确表示撤销全部角色。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(String subjectTypeCode, Long subjectId, Collection<Integer> roleIds) {
        RbacSubjectType subjectType = validateSubject(subjectTypeCode, subjectId);
        List<Integer> distinctRoleIds = roleIds == null
                ? List.of()
                : roleIds.stream().distinct().toList();
        ApiAssert.isTrue(ApiCode.RBAC_SCOPE_MISMATCH,
                roleService.findByIds(subjectType.scope(), distinctRoleIds).size()
                        == distinctRoleIds.size());

        subjectRoleMapper.delete(subjectWrapper(subjectType, subjectId));
        if (!distinctRoleIds.isEmpty()) {
            List<RbacSubjectRole> relations = distinctRoleIds.stream().map(roleId -> {
                RbacSubjectRole relation = new RbacSubjectRole();
                relation.setSubjectType(subjectType.code());
                relation.setSubjectId(subjectId);
                relation.setScope(subjectType.scope().code());
                relation.setRoleId(roleId);
                return relation;
            }).toList();
            int inserted = relations.stream().mapToInt(subjectRoleMapper::insert).sum();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, inserted == relations.size());
        }
        invalidateAfterCommit(subjectType, subjectId,
                SessionInvalidationReason.SUBJECT_ROLE_CHANGED);
    }

    /** 查询主体当前拥有的角色主键。 */
    public List<Integer> listRoleIdsBySubject(String subjectTypeCode, Long subjectId) {
        RbacSubjectType subjectType = RbacSubjectType.fromCode(subjectTypeCode);
        return subjectRoleMapper.selectList(subjectWrapper(subjectType, subjectId)).stream()
                .map(RbacSubjectRole::getRoleId)
                .distinct()
                .toList();
    }

    /**
     * 批量查询多个同类型主体的角色，供管理端分页接口避免逐用户查询造成 N+1。
     */
    public Map<Long, List<Integer>> mapRoleIdsBySubjects(
            String subjectTypeCode, Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        RbacSubjectType subjectType = RbacSubjectType.fromCode(subjectTypeCode);
        Map<Long, List<Integer>> result = new LinkedHashMap<>();
        subjectRoleMapper.selectList(new LambdaQueryWrapper<RbacSubjectRole>()
                .eq(RbacSubjectRole::getSubjectType, subjectType.code())
                .eq(RbacSubjectRole::getScope, subjectType.scope().code())
                .in(RbacSubjectRole::getSubjectId, subjectIds)).forEach(relation ->
                result.computeIfAbsent(relation.getSubjectId(), ignored ->
                        new java.util.ArrayList<>()).add(relation.getRoleId()));
        return result;
    }

    /** 查询持有指定角色的全部主体，用于角色授权变化后的会话失效。 */
    public List<RbacSubjectRef> listSubjectsByRoleId(Integer roleId) {
        return subjectRoleMapper.selectList(new LambdaQueryWrapper<RbacSubjectRole>()
                .eq(RbacSubjectRole::getRoleId, roleId)).stream()
                .map(relation -> new RbacSubjectRef(
                        relation.getSubjectType(), relation.getSubjectId()))
                .distinct()
                .toList();
    }

    /** 判断主体是否持有指定角色。 */
    public boolean hasRole(String subjectTypeCode, Long subjectId, Integer roleId) {
        RbacSubjectType subjectType = RbacSubjectType.fromCode(subjectTypeCode);
        return subjectRoleMapper.selectCount(subjectWrapper(subjectType, subjectId)
                .eq(RbacSubjectRole::getRoleId, roleId)) > 0;
    }

    /** 在事务提交后注销指定主体会话。 */
    public void invalidateAfterCommit(RbacSubjectRef subject) {
        invalidateAfterCommit(subject, SessionInvalidationReason.SUBJECT_ROLE_CHANGED);
    }

    /**
     * 在事务提交后按指定原因注销主体会话。
     *
     * @param subject 受影响主体
     * @param reason  授权变化原因
     */
    public void invalidateAfterCommit(
            RbacSubjectRef subject, SessionInvalidationReason reason) {
        invalidateAfterCommit(
                RbacSubjectType.fromCode(subject.subjectType()), subject.subjectId(), reason);
    }

    private RbacSubjectType validateSubject(String subjectTypeCode, Long subjectId) {
        RbacSubjectType subjectType = RbacSubjectType.fromCode(subjectTypeCode);
        RbacSubjectValidator validator = subjectValidators.get(subjectType);
        if (validator == null) {
            throw new IllegalStateException(
                    "应用未注册 RBAC 主体校验器，subjectType=" + subjectType.code());
        }
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, subjectId != null && validator.exists(subjectId));
        return subjectType;
    }

    private Map<RbacSubjectType, RbacSubjectValidator> indexValidators(
            List<RbacSubjectValidator> validators) {
        Map<RbacSubjectType, RbacSubjectValidator> result = new EnumMap<>(RbacSubjectType.class);
        for (RbacSubjectType subjectType : RbacSubjectType.values()) {
            for (RbacSubjectValidator validator : validators) {
                if (!validator.supports(subjectType.code())) {
                    continue;
                }
                RbacSubjectValidator duplicate = result.put(subjectType, validator);
                if (duplicate != null) {
                    throw new IllegalStateException(
                            "存在重复 RBAC 主体校验器，subjectType=" + subjectType.code());
                }
            }
        }
        return Map.copyOf(result);
    }

    private LambdaQueryWrapper<RbacSubjectRole> subjectWrapper(
            RbacSubjectType subjectType, Long subjectId) {
        return new LambdaQueryWrapper<RbacSubjectRole>()
                .eq(RbacSubjectRole::getSubjectType, subjectType.code())
                .eq(RbacSubjectRole::getScope, subjectType.scope().code())
                .eq(RbacSubjectRole::getSubjectId, subjectId);
    }

    private void invalidateAfterCommit(
            RbacSubjectType subjectType, Long subjectId, SessionInvalidationReason reason) {
        if (SecurityRealm.ADMIN.equals(subjectType.code())) {
            sessionInvalidationService.invalidateAdminAfterCommit(subjectId, reason);
            return;
        }
        sessionInvalidationService.invalidateUserAfterCommit(subjectId, reason);
    }
}
