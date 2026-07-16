package com.yxx.admin.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.admin.mapper.AdminRoleMapper;
import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.admin.mapper.AdminUserRoleMapper;
import com.yxx.admin.model.entity.AdminRole;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.entity.AdminUserRole;
import com.yxx.admin.security.AdminSecurityCodes;
import com.yxx.common.utils.AccountNormalizer;
import com.yxx.security.validation.PasswordPolicyChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 一次性初始管理员创建器。
 *
 * <p>仅当管理员表完全为空时允许执行。初始化账号直接绑定超级管理员角色，密码使用 BCrypt
 * 保存。任何参数缺失或数据库已有管理员都会终止执行，禁止覆盖现有账号。</p>
 */
@Slf4j
@Component
@Profile("bootstrap")
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyChecker passwordPolicyChecker;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        validateProperties();
        if (adminUserMapper.selectCount(null) > 0) {
            throw new IllegalStateException("管理员表已存在数据，禁止再次执行 bootstrap 初始化");
        }

        AdminRole superAdminRole = adminRoleMapper.selectOne(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getCode, AdminSecurityCodes.ROLE_SUPER_ADMIN));
        if (superAdminRole == null) {
            throw new IllegalStateException("未找到超级管理员角色，请先执行管理端数据库迁移");
        }

        LocalDateTime now = LocalDateTime.now();
        AdminUser admin = new AdminUser();
        admin.setLoginCode(AccountNormalizer.normalizeLoginCode(properties.getLoginCode()));
        admin.setLoginName(AccountNormalizer.normalizeDisplayName(properties.getLoginName()));
        admin.setEmail(AccountNormalizer.normalizeEmail(properties.getEmail()));
        admin.setPassword(passwordEncoder.encode(properties.getPassword()));
        admin.setStatus(Boolean.TRUE);
        admin.setIsDelete(Boolean.FALSE);
        admin.setCreateUid(0L);
        admin.setUpdateUid(0L);
        admin.setCreateTime(now);
        admin.setUpdateTime(now);
        if (adminUserMapper.insert(admin) != 1) {
            throw new IllegalStateException("创建初始管理员失败");
        }

        AdminUserRole relation = new AdminUserRole();
        relation.setUserId(admin.getId());
        relation.setRoleId(superAdminRole.getId());
        relation.setCreateTime(now);
        relation.setUpdateTime(now);
        if (adminUserRoleMapper.insert(relation) != 1) {
            throw new IllegalStateException("绑定初始管理员角色失败");
        }

        log.info("初始管理员创建成功，loginCode={}；请立即使用管理端修改临时密码", admin.getLoginCode());
        // 等事务完成资源清理后再关闭最小化容器，避免在提交过程中提前销毁数据源。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                Thread shutdownThread = new Thread(applicationContext::close, "bootstrap-shutdown");
                shutdownThread.setDaemon(false);
                shutdownThread.start();
            }
        });
    }

    private void validateProperties() {
        requireText(properties.getLoginCode(), "bootstrap.admin.login-code");
        requireText(properties.getLoginName(), "bootstrap.admin.login-name");
        requireText(properties.getEmail(), "bootstrap.admin.email");
        requireText(properties.getPassword(), "bootstrap.admin.password");
        if (!passwordPolicyChecker.isValid(properties.getPassword(), true)) {
            throw new IllegalArgumentException("初始管理员临时密码不符合系统密码策略");
        }
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少初始化参数：" + propertyName);
        }
    }
}
