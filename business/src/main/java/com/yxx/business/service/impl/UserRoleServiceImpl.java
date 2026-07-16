package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.UserRoleMapper;
import com.yxx.business.mapper.UserMapper;
import com.yxx.business.model.entity.Role;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserRole;
import com.yxx.business.service.RoleService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.security.UserSecurityCodes;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.security.context.SessionInvalidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author yxx
 * @since 2023-05-17 10:01
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private final RoleService roleService;

    private final UserMapper userMapper;

    private final SessionInvalidationService sessionInvalidationService;

    @Override
    public Boolean assignMemberRole(User user) {
        // 基础成员角色由迁移脚本初始化；缺失意味着环境不完整，不能创建无角色用户。
        Role role = roleService.findByCode(UserSecurityCodes.ROLE_MEMBER).orElse(null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, role != null);
        // 新用户只建立最小默认角色关联，后续扩展角色由独立管理流程替换。
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        return save(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        // 先验证主体及全部角色，再删除旧关联，避免错误输入导致权限被意外清空。
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, userMapper.selectById(userId) != null);
        // 去重既减少无意义写入，也避免触发 user_id + role_id 唯一约束。
        List<Integer> distinctRoleIds = roleIds == null
                ? List.of()
                : roleIds.stream().distinct().toList();
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                roleService.findByIds(distinctRoleIds).size() == distinctRoleIds.size());
        // 关联表采用物理删除，确保同一角色可以在撤销后再次分配。
        remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (!distinctRoleIds.isEmpty()) {
            // 将完整目标集合一次批量写入；空集合明确表示撤销全部角色。
            List<UserRole> relations = distinctRoleIds.stream().map(roleId -> {
                UserRole relation = new UserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                return relation;
            }).toList();
            ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, saveBatch(relations));
        }
        // 当前项目采用登录时权限快照，角色变更后必须注销旧会话。
        sessionInvalidationService.invalidateUserAfterCommit(userId);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Integer roleId) {
        // distinct 防御历史脏数据，避免同一用户被重复执行会话失效操作。
        return list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))
                .stream()
                .map(UserRole::getUserId)
                .distinct()
                .toList();
    }

    @Override
    public List<Integer> listRoleIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
    }
}
