package com.yxx.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.mapper.AdminRoleMapper;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.service.AdminRoleService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author yxx
 * @since 2023-05-17 09:59
 */
@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {

    @Override
    public Optional<AdminRole> findByCode(String code) {
        // 角色编码是跨环境稳定标识，内置角色保护逻辑统一通过编码识别。
        return Optional.ofNullable(getOne(
                new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getCode, code)));
    }

    @Override
    public List<Integer> findIdsByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            // 避免对空集合生成 IN 条件。
            return List.of();
        }
        return list(new LambdaQueryWrapper<AdminRole>().in(AdminRole::getCode, codes)).stream()
                .map(AdminRole::getId)
                .toList();
    }

    @Override
    public List<AdminRole> findByIds(Collection<Integer> ids) {
        // 统一返回空集合而非 null，方便调用方通过数量校验全部 ID 是否存在。
        return ids == null || ids.isEmpty() ? List.of() : listByIds(ids);
    }
}
