package com.yxx.business.auth;

import com.yxx.business.model.entity.User;
import com.yxx.business.service.UserService;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.agent.UserAgentUtil;
import com.yxx.common.utils.ip.ClientIpResolver;
import com.yxx.framework.security.LoginRiskNotificationService;
import com.yxx.security.constant.SecurityRealm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 用户登录风险信息处理服务。
 *
 * <p>认证策略只验证身份，本服务独立处理设备、IP 归属地和异地登录提醒，避免安全通知逻辑
 * 重新耦合进密码或第三方登录策略。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginRiskService {

    private final UserService userService;
    private final ClientIpResolver clientIpResolver;
    private final LoginRiskNotificationService loginRiskNotificationService;

    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    /**
     * 记录登录设备和地址，并异步检查异常登录。
     *
     * @param user 已完成身份认证的系统用户
     */
    public void handleSuccessfulLogin(User user) {
        // 在请求线程内提取必要信息，避免异步线程访问已回收的 ServletRequest。
        String rawAgent = ServletUtils.getRequest().getHeader("user-agent");
        String agent = UserAgentUtil.getAgent(rawAgent);
        String requestIp = clientIpResolver.resolve(ServletUtils.getRequest());
        // 使用应用统一的有界执行器，避免每次登录创建线程导致资源失控。
        CompletableFuture.runAsync(
                        () -> updateMetadataAndCheckRisk(user, requestIp, agent),
                        applicationTaskExecutor)
                .exceptionally(exception -> {
                    // 登录元数据和安全提醒属于辅助链路，失败不能使已经通过的认证失效。
                    log.error("处理用户登录风险信息失败，userId={}", user.getId(), exception);
                    return null;
                });
    }

    private void updateMetadataAndCheckRisk(User user, String requestIp, String agent) {
        // 先比较历史与当前登录特征并发送提醒，再持久化本次特征供下次比较。
        String ipRegion = loginRiskNotificationService.process(
                SecurityRealm.USER, user.getId(), user.getEmail(),
                user.getAgent(), user.getIpHomePlace(), requestIp, agent);
        boolean updated = userService.updateLoginMetadata(user.getId(), agent, ipRegion);
        if (!updated) {
            log.warn("用户登录元数据未更新，userId={}", user.getId());
        }
    }

}
