package com.yxx.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.rbac.mapper.RbacMenuMapper;
import com.yxx.rbac.mapper.RbacPermissionMapper;
import com.yxx.rbac.mapper.RbacRoleMapper;
import com.yxx.rbac.mapper.RbacRoleMenuMapper;
import com.yxx.rbac.mapper.RbacRolePermissionMapper;
import com.yxx.rbac.mapper.RbacSubjectRoleMapper;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.model.RbacSubjectRef;
import com.yxx.rbac.model.entity.RbacMenu;
import com.yxx.rbac.model.entity.RbacPermission;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.model.entity.RbacRoleMenu;
import com.yxx.rbac.model.entity.RbacRolePermission;
import com.yxx.rbac.model.entity.RbacSubjectRole;
import com.yxx.security.context.SessionInvalidationReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RBAC 配置命令服务。
 *
 * <p>所有角色、权限和菜单写操作集中在此处，查询服务不再继承 MyBatis-Plus 通用 CRUD。
 * 这样调用方无法绕过内置角色保护、权限域校验、关联清理和会话失效规则。</p>
 */
@Service
@RequiredArgsConstructor
public class RbacManagementService {

    private final RbacRoleMapper roleMapper;
    private final RbacPermissionMapper permissionMapper;
    private final RbacMenuMapper menuMapper;
    private final RbacSubjectRoleMapper subjectRoleMapper;
    private final RbacRolePermissionMapper rolePermissionMapper;
    private final RbacRoleMenuMapper roleMenuMapper;
    private final RbacRoleService roleService;
    private final RbacSubjectRoleService subjectRoleService;

    @Transactional(rollbackFor = Exception.class)
    public Integer createRole(RbacScope scope, String code, String name, String remark) {
        String normalizedCode = normalizeCode(code);
        validateSecurityCode(scope, normalizedCode);
        RbacRole role = new RbacRole();
        role.setScope(scope.code());
        role.setCode(normalizedCode);
        role.setName(name.trim());
        role.setRemark(trimToNull(remark));
        // 内置和超级角色只能由受版本控制的 Flyway 迁移创建，普通管理接口永远不能提升该标记。
        role.setBuiltIn(Boolean.FALSE);
        role.setSuperRole(Boolean.FALSE);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, roleMapper.insert(role) == 1);
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Integer roleId, String name, String remark) {
        RbacRole role = requiredRole(roleId);
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE, !Boolean.TRUE.equals(role.getBuiltIn()));
        int updated = roleMapper.update(new LambdaUpdateWrapper<RbacRole>()
                .eq(RbacRole::getId, roleId)
                .set(RbacRole::getName, name.trim())
                .set(RbacRole::getRemark, trimToNull(remark)));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated == 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Integer roleId) {
        RbacRole role = requiredRole(roleId);
        ApiAssert.isTrue(ApiCode.BUILT_IN_ROLE_IMMUTABLE, !Boolean.TRUE.equals(role.getBuiltIn()));
        List<RbacSubjectRef> affectedSubjects = subjectRoleService.listSubjectsByRoleId(roleId);
        subjectRoleMapper.delete(new LambdaQueryWrapper<RbacSubjectRole>()
                .eq(RbacSubjectRole::getRoleId, roleId));
        rolePermissionMapper.delete(new LambdaQueryWrapper<RbacRolePermission>()
                .eq(RbacRolePermission::getRoleId, roleId));
        roleMenuMapper.delete(new LambdaQueryWrapper<RbacRoleMenu>()
                .eq(RbacRoleMenu::getRoleId, roleId));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, roleMapper.deleteById(roleId) == 1);
        affectedSubjects.forEach(subject -> subjectRoleService.invalidateAfterCommit(
                subject, SessionInvalidationReason.ROLE_DELETED));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer createPermission(RbacScope scope, String code, String name,
                                    String resourceType, String description, boolean enabled) {
        String normalizedCode = normalizeCode(code);
        validateSecurityCode(scope, normalizedCode);
        RbacPermission permission = new RbacPermission();
        permission.setScope(scope.code());
        permission.setCode(normalizedCode);
        permission.setName(name.trim());
        permission.setResourceType(resourceType.trim().toLowerCase(Locale.ROOT));
        permission.setDescription(trimToNull(description));
        permission.setStatus(enabled);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, permissionMapper.insert(permission) == 1);
        return permission.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(Integer permissionId, String name, String resourceType,
                                 String description, boolean enabled) {
        requiredPermission(permissionId);
        List<RbacSubjectRef> affectedSubjects = subjectsByPermission(permissionId);
        int updated = permissionMapper.update(new LambdaUpdateWrapper<RbacPermission>()
                .eq(RbacPermission::getId, permissionId)
                .set(RbacPermission::getName, name.trim())
                .set(RbacPermission::getResourceType,
                        resourceType.trim().toLowerCase(Locale.ROOT))
                .set(RbacPermission::getDescription, trimToNull(description))
                .set(RbacPermission::getStatus, enabled));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated == 1);
        affectedSubjects.forEach(subject -> subjectRoleService.invalidateAfterCommit(
                subject, SessionInvalidationReason.PERMISSION_CHANGED));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Integer permissionId) {
        requiredPermission(permissionId);
        List<RbacSubjectRef> affectedSubjects = subjectsByPermission(permissionId);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RbacRolePermission>()
                .eq(RbacRolePermission::getPermissionId, permissionId));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, permissionMapper.deleteById(permissionId) == 1);
        affectedSubjects.forEach(subject -> subjectRoleService.invalidateAfterCommit(
                subject, SessionInvalidationReason.PERMISSION_CHANGED));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer createMenu(RbacMenu menu) {
        RbacScope scope = RbacScope.fromCode(menu.getScope());
        validateParent(scope, null, menu.getParentId());
        menu.setMenuCode(menu.getMenuCode().trim());
        normalizeMenuFields(menu);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, menuMapper.insert(menu) == 1);
        return menu.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Integer menuId, RbacMenu changes) {
        RbacMenu current = requiredMenu(menuId);
        RbacScope scope = RbacScope.fromCode(current.getScope());
        validateParent(scope, menuId, changes.getParentId());
        normalizeMenuFields(changes);
        int updated = menuMapper.update(new LambdaUpdateWrapper<RbacMenu>()
                .eq(RbacMenu::getId, menuId)
                .set(RbacMenu::getParentId, changes.getParentId())
                .set(RbacMenu::getMenuName, changes.getMenuName())
                .set(RbacMenu::getPath, changes.getPath())
                .set(RbacMenu::getComponent, changes.getComponent())
                .set(RbacMenu::getIcon, changes.getIcon())
                .set(RbacMenu::getSort, changes.getSort())
                .set(RbacMenu::getVisible, changes.getVisible())
                .set(RbacMenu::getStatus, changes.getStatus()));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated == 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Integer menuId) {
        requiredMenu(menuId);
        long childCount = menuMapper.selectCount(new LambdaQueryWrapper<RbacMenu>()
                .eq(RbacMenu::getParentId, menuId));
        ApiAssert.isTrue(ApiCode.DATA_CONFLICT, childCount == 0);
        roleMenuMapper.delete(new LambdaQueryWrapper<RbacRoleMenu>()
                .eq(RbacRoleMenu::getMenuId, menuId));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, menuMapper.deleteById(menuId) == 1);
    }

    private RbacRole requiredRole(Integer roleId) {
        RbacRole role = roleService.findById(roleId).orElse(null);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, role != null);
        return role;
    }

    private RbacPermission requiredPermission(Integer permissionId) {
        RbacPermission permission = permissionId == null
                ? null : permissionMapper.selectById(permissionId);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, permission != null);
        return permission;
    }

    private RbacMenu requiredMenu(Integer menuId) {
        RbacMenu menu = menuId == null ? null : menuMapper.selectById(menuId);
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, menu != null);
        return menu;
    }

    private List<RbacSubjectRef> subjectsByPermission(Integer permissionId) {
        List<Integer> roleIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RbacRolePermission>()
                                .eq(RbacRolePermission::getPermissionId, permissionId))
                .stream().map(RbacRolePermission::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return subjectRoleMapper.selectList(new LambdaQueryWrapper<RbacSubjectRole>()
                        .in(RbacSubjectRole::getRoleId, roleIds))
                .stream()
                .map(relation -> new RbacSubjectRef(
                        relation.getSubjectType(), relation.getSubjectId()))
                .distinct()
                .toList();
    }

    private void validateSecurityCode(RbacScope scope, String code) {
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID,
                code != null && code.startsWith(scope.code() + ":")
                        && code.matches("^[a-z][a-z0-9-]*(?::[a-z0-9-]+)+$"));
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void normalizeMenuFields(RbacMenu menu) {
        menu.setMenuName(menu.getMenuName().trim());
        menu.setPath(trimToNull(menu.getPath()));
        menu.setComponent(trimToNull(menu.getComponent()));
        menu.setIcon(trimToNull(menu.getIcon()));
    }

    private void validateParent(RbacScope scope, Integer currentMenuId, Integer parentId) {
        if (parentId == null) {
            return;
        }
        ApiAssert.isTrue(ApiCode.PARAM_IS_INVALID, !parentId.equals(currentMenuId));
        RbacMenu parent = menuMapper.selectById(parentId);
        ApiAssert.isTrue(ApiCode.RBAC_SCOPE_MISMATCH,
                parent != null && scope.code().equals(parent.getScope()));

        // 从候选父节点向上回溯；遇到当前菜单即会形成循环，遇到重复节点说明存量数据已损坏。
        Set<Integer> visited = new HashSet<>();
        RbacMenu cursor = parent;
        while (cursor != null) {
            ApiAssert.isTrue(ApiCode.DATA_CONFLICT, visited.add(cursor.getId()));
            ApiAssert.isTrue(ApiCode.DATA_CONFLICT, !cursor.getId().equals(currentMenuId));
            cursor = cursor.getParentId() == null ? null : menuMapper.selectById(cursor.getParentId());
        }
    }
}
