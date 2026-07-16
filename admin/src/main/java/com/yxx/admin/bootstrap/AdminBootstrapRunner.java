package com.yxx.admin.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.common.utils.AccountNormalizer;
import com.yxx.rbac.mapper.RbacRoleMapper;
import com.yxx.rbac.mapper.RbacSubjectRoleMapper;
import com.yxx.rbac.model.RbacScope;
import com.yxx.rbac.constant.RbacSecurityCodes;
import com.yxx.rbac.model.RbacSubjectType;
import com.yxx.rbac.model.entity.RbacRole;
import com.yxx.rbac.model.entity.RbacSubjectRole;
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
    private final RbacRoleMapper roleMapper;
    private final RbacSubjectRoleMapper subjectRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyChecker passwordPolicyChecker;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        // 在接触数据库前校验全部必填配置和密码策略，失败时不给库留下任何半成品数据。
        validateProperties();
        // bootstrap 是一次性入口，只允许空管理员表执行，禁止借此覆盖或追加高权限账号。
        if (adminUserMapper.selectCount(null) > 0) {
            throw new IllegalStateException("管理员表已存在数据，禁止再次执行 bootstrap 初始化");
        }

        // 超级角色由 Flyway 初始化，启动器只绑定既有内置角色，不自行创造权限模型。
        RbacRole superAdminRole = roleMapper.selectOne(new LambdaQueryWrapper<RbacRole>()
                .eq(RbacRole::getScope, RbacScope.ADMIN.code())
                .eq(RbacRole::getCode, RbacSecurityCodes.ROLE_ADMIN_SUPER_ADMIN));
        if (superAdminRole == null) {
            throw new IllegalStateException("未找到超级管理员角色，请先执行管理端数据库迁移");
        }

        // 显式填写审计字段，因为最小化 bootstrap 上下文不存在已登录操作人。
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
            // 严格要求影响一行；异常会触发事务回滚，不继续创建孤立关联。
            throw new IllegalStateException("创建初始管理员失败");
        }

        // 管理员和超级角色关联与账号创建处于同一事务，保证初始账号创建后即可管理系统。
        RbacSubjectRole relation = new RbacSubjectRole();
        relation.setSubjectType(RbacSubjectType.ADMIN_USER.code());
        relation.setSubjectId(admin.getId());
        relation.setScope(RbacScope.ADMIN.code());
        relation.setRoleId(superAdminRole.getId());
        relation.setCreateTime(now);
        relation.setUpdateTime(now);
        if (subjectRoleMapper.insert(relation) != 1) {
            throw new IllegalStateException("绑定初始管理员角色失败");
        }

        log.info("初始管理员创建成功，loginCode={}；请立即使用管理端修改临时密码", admin.getLoginCode());
        // 等事务完成资源清理后再关闭最小化容器，避免在提交过程中提前销毁数据源。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // 使用非守护线程触发关闭，确保 JVM 会等待 Spring 完整释放数据源等资源。
                Thread shutdownThread = new Thread(applicationContext::close, "bootstrap-shutdown");
                shutdownThread.setDaemon(false);
                shutdownThread.start();
            }
        });
    }

    private void validateProperties() {
        // 分项报告配置名，便于部署人员直接定位缺失的启动参数。
        requireText(properties.getLoginCode(), "bootstrap.admin.login-code");
        requireText(properties.getLoginName(), "bootstrap.admin.login-name");
        requireText(properties.getEmail(), "bootstrap.admin.email");
        requireText(properties.getPassword(), "bootstrap.admin.password");
        // 初始化流程不经过 Bean Validation，请显式复用同一密码策略检查器。
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
