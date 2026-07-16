package com.yxx.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.RoleMapper;
import com.yxx.business.model.entity.Role;
import com.yxx.business.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author yxx
 * @since 2023-05-17 09:59
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public Optional<Role> findByCode(String code) {
        // 角色编码是稳定业务标识，调用方不应依赖环境间可能不同的数据库 ID。
        return Optional.ofNullable(getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, code)));
    }

    @Override
    public List<Integer> findIdsByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            // 空角色集合直接返回，避免生成 SQL IN ()。
            return List.of();
        }
        return list(new LambdaQueryWrapper<Role>().in(Role::getCode, codes)).stream()
                .map(Role::getId)
                .toList();
    }

    @Override
    public List<Role> findByIds(Collection<Integer> ids) {
        // 统一空值语义，调用方可直接比较返回数量完成全集合法性校验。
        return ids == null || ids.isEmpty() ? List.of() : listByIds(ids);
    }
}
