package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxx.admin.mapper.ManagedBusinessUserMapper;
import com.yxx.admin.model.entity.ManagedBusinessUser;
import com.yxx.admin.model.request.BusinessUserPageReq;
import com.yxx.admin.model.response.ManagedBusinessUserRes;
import com.yxx.admin.service.BusinessUserAccessManagementService;
import com.yxx.common.core.page.PageResponse;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.service.RbacSubjectRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 管理端业务用户授权管理实现。 */
@Service
@RequiredArgsConstructor
public class BusinessUserAccessManagementServiceImpl
        implements BusinessUserAccessManagementService {

    private final ManagedBusinessUserMapper businessUserMapper;
    private final RbacSubjectRoleService subjectRoleService;

    @Override
    public PageResponse<ManagedBusinessUserRes> page(BusinessUserPageReq request) {
        LambdaQueryWrapper<ManagedBusinessUser> query = new LambdaQueryWrapper<>();
        String keyword = request.getKeyword() == null ? null : request.getKeyword().trim();
        if (keyword != null && !keyword.isEmpty()) {
            // 关键词条件使用括号包裹，避免后续状态条件只作用于最后一个 OR 分支。
            query.and(wrapper -> wrapper
                    .like(ManagedBusinessUser::getDisplayName, keyword)
                    .or().like(ManagedBusinessUser::getPhone, keyword)
                    .or().like(ManagedBusinessUser::getEmail, keyword));
        }
        if (request.getStatus() != null) {
            query.eq(ManagedBusinessUser::getStatus, request.getStatus());
        }
        query.orderByDesc(ManagedBusinessUser::getId);

        Page<ManagedBusinessUser> page = businessUserMapper.selectPage(
                new Page<>(request.getPage(), request.getPageSize()), query);
        Map<Long, List<Integer>> roleIdsByUserId = subjectRoleService.mapRoleIdsBySubjects(
                RbacSubjectType.BUSINESS_USER.code(),
                page.getRecords().stream().map(ManagedBusinessUser::getId).toList());
        List<ManagedBusinessUserRes> records = page.getRecords().stream()
                .map(user -> toResponse(user,
                        roleIdsByUserId.getOrDefault(user.getId(), List.of())))
                .toList();
        return new PageResponse<>(records, page.getCurrent(), page.getSize(),
                page.getTotal(), page.getPages());
    }

    @Override
    public ManagedBusinessUserRes findById(Long userId) {
        ManagedBusinessUser user = businessUserMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ApiCode.USER_NOT_EXIST);
        }
        return toResponse(user, subjectRoleService.listRoleIdsBySubject(
                RbacSubjectType.BUSINESS_USER.code(), user.getId()));
    }

    @Override
    public void replaceRoles(Long userId, Collection<Integer> roleIds) {
        // 公共服务会再次校验主体存在性以及所有角色是否属于 business 权限域。
        subjectRoleService.replaceRoles(
                RbacSubjectType.BUSINESS_USER.code(), userId, roleIds);
    }

    private ManagedBusinessUserRes toResponse(
            ManagedBusinessUser user, List<Integer> roleIds) {
        return new ManagedBusinessUserRes(
                user.getId(), user.getDisplayName(), user.getAvatar(), user.getStatus(),
                user.getPhone(), user.getEmail(), user.getIpHomePlace(), user.getAgent(),
                user.getCreateTime(), roleIds);
    }
}
