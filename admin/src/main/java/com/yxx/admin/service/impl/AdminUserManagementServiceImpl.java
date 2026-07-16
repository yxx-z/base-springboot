package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.request.AdminUserPageReq;
import com.yxx.admin.model.request.CreateAdminUserReq;
import com.yxx.admin.model.request.UpdateAdminUserReq;
import com.yxx.admin.model.response.ManagedAdminUserRes;
import com.yxx.admin.service.AdminUserManagementService;
import com.yxx.admin.service.AdminUserRoleService;
import com.yxx.admin.service.AdminUserService;
import com.yxx.common.core.page.PageResponse;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.utils.AccountNormalizer;
import com.yxx.common.utils.ApiAssert;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.service.RbacSubjectRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 管理员账号管理应用服务实现。 */
@Service
@RequiredArgsConstructor
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private final AdminUserMapper adminUserMapper;
    private final AdminUserService adminUserService;
    private final AdminUserRoleService adminUserRoleService;
    private final RbacSubjectRoleService subjectRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResponse<ManagedAdminUserRes> page(AdminUserPageReq request) {
        LambdaQueryWrapper<AdminUser> query = new LambdaQueryWrapper<>();
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        if (keyword != null && !keyword.isEmpty()) {
            query.and(wrapper -> wrapper.like(AdminUser::getLoginCode, keyword)
                    .or().like(AdminUser::getLoginName, keyword)
                    .or().like(AdminUser::getLinkPhone, keyword)
                    .or().like(AdminUser::getEmail, keyword));
        }
        if (request.getStatus() != null) {
            query.eq(AdminUser::getStatus, request.getStatus());
        }
        query.orderByDesc(AdminUser::getId);
        Page<AdminUser> page = adminUserMapper.selectPage(
                new Page<>(request.getPage(), request.getPageSize()), query);
        Map<Long, List<Integer>> roleIds = subjectRoleService.mapRoleIdsBySubjects(
                RbacSubjectType.ADMIN_USER.code(),
                page.getRecords().stream().map(AdminUser::getId).toList());
        List<ManagedAdminUserRes> records = page.getRecords().stream()
                .map(user -> toResponse(user, roleIds.getOrDefault(user.getId(), List.of())))
                .toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(),
                page.getTotal(), page.getPages());
    }

    @Override
    public ManagedAdminUserRes findById(Long userId) {
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ApiCode.USER_NOT_EXIST);
        }
        return toResponse(user, subjectRoleService.listRoleIdsBySubject(
                RbacSubjectType.ADMIN_USER.code(), userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateAdminUserReq request) {
        AdminUser user = new AdminUser();
        user.setLoginCode(AccountNormalizer.normalizeLoginCode(request.loginCode()));
        user.setLoginName(AccountNormalizer.normalizeDisplayName(request.loginName()));
        user.setEmail(AccountNormalizer.normalizeEmail(request.email()));
        user.setLinkPhone(AccountNormalizer.normalizeMainlandPhone(request.linkPhone()));
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(Boolean.TRUE);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, adminUserMapper.insert(user) == 1);
        adminUserRoleService.replaceRoles(user.getId(), request.roleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, UpdateAdminUserReq request) {
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, adminUserMapper.selectById(userId) != null);
        int updated = adminUserMapper.update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, userId)
                .set(AdminUser::getLoginName,
                        AccountNormalizer.normalizeDisplayName(request.loginName()))
                .set(AdminUser::getEmail, AccountNormalizer.normalizeEmail(request.email()))
                .set(AdminUser::getLinkPhone,
                        AccountNormalizer.normalizeMainlandPhone(request.linkPhone())));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated == 1);
    }

    @Override
    public void changeStatus(Long userId, boolean enabled) {
        adminUserService.changeStatus(userId, enabled);
    }

    @Override
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        adminUserRoleService.replaceRoles(userId, roleIds);
    }

    @Override
    public void delete(Long userId) {
        adminUserService.delete(userId);
    }

    private ManagedAdminUserRes toResponse(AdminUser user, List<Integer> roleIds) {
        return new ManagedAdminUserRes(
                user.getId(), user.getLoginCode(), user.getLoginName(), user.getStatus(),
                user.getLinkPhone(), user.getEmail(), user.getIpHomePlace(), user.getAgent(),
                user.getCreateTime(), roleIds);
    }
}
