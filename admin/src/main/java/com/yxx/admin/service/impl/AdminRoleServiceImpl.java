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
        return Optional.ofNullable(getOne(
                new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getCode, code)));
    }

    @Override
    public List<Integer> findIdsByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<AdminRole>().in(AdminRole::getCode, codes)).stream()
                .map(AdminRole::getId)
                .toList();
    }

    @Override
    public List<AdminRole> findByIds(Collection<Integer> ids) {
        return ids == null || ids.isEmpty() ? List.of() : listByIds(ids);
    }
}
