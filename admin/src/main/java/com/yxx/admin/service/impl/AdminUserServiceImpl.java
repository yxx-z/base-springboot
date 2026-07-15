package com.yxx.admin.service.impl;

import cn.dev33.satoken.temp.SaTempUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.admin.model.request.*;
import com.yxx.admin.mapper.AdminUserMapper;
import com.yxx.admin.model.entity.AdminUser;
import com.yxx.admin.model.response.LoginRes;
import com.yxx.admin.service.AdminUserService;
import com.yxx.admin.security.AdminAuthorizationService;
import com.yxx.common.constant.EmailSubjectConstant;
import com.yxx.security.constant.LoginDeviceType;
import com.yxx.common.constant.RedisConstant;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.properties.IpProperties;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.properties.ResetPwdProperties;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.agent.UserAgentUtil;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.ip.AddressUtil;
import com.yxx.common.utils.ip.ClientIpResolver;
import com.yxx.common.utils.ip.IpUtil;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.context.OneTimeTemporaryTokenService;
import com.yxx.security.context.PasswordLoginProtectionService;
import com.yxx.security.context.SessionInvalidationService;
import com.yxx.security.model.LoginPrincipal;
import com.yxx.security.model.PasswordResetTokenPayload;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {
    private final AdminAuthorizationService authorizationService;

    private final LoginSessionService loginSessionService;

    private final SessionInvalidationService sessionInvalidationService;

    private final OneTimeTemporaryTokenService oneTimeTemporaryTokenService;

    private final PasswordLoginProtectionService loginProtectionService;

    private final RedissonCache redissonCache;

    private final MailUtils mailUtils;

    private final MailProperties mailProperties;

    private final ResetPwdProperties resetPwdProperties;

    private final IpProperties ipProperties;

    private final MyWebProperties myWebProperties;

    private final ClientIpResolver clientIpResolver;

    private final AddressUtil addressUtil;

    /**
     * 管理端与用户端共享统一密码编码策略，避免不同入口产生不兼容的密码格式。
     */
    private final PasswordEncoder passwordEncoder;

    /** 安全通知等非核心链路任务统一使用的有界线程池。 */
    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    @Override
    public LoginRes login(LoginReq request) {
        HttpServletRequest servletRequest = ServletUtils.getRequest();
        String requestIp = clientIpResolver.resolve(servletRequest);
        loginProtectionService.checkAllowed(SecurityRealm.ADMIN, request.getLoginCode(), requestIp);

        // 根据登录账号获取用户信息
        AdminUser user = getOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getLoginCode, request.getLoginCode()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginProtectionService.recordFailure(SecurityRealm.ADMIN, request.getLoginCode(), requestIp);
            throw new com.yxx.common.exceptions.ApiException(ApiCode.AUTHENTICATION_FAILED);
        }
        ApiAssert.isTrue(ApiCode.ACCOUNT_DISABLED, Boolean.TRUE.equals(user.getStatus()));
        loginProtectionService.recordSuccess(SecurityRealm.ADMIN, request.getLoginCode());

        // 获取登录设备信息
        String requestAgent = servletRequest.getHeader("user-agent");
        // 解析登录设备
        String agent = UserAgentUtil.getAgent(requestAgent);
        // 如果校验ip
        if (Boolean.TRUE.equals(ipProperties.getCheck())) {
            // 得到请求时的ip
            // 获取ip归属地
            String ipHomePlace = addressUtil.getIpHomePlace(requestIp, 2);
            // 新建checkUser并将user信息赋值过来，下面异步校验使用。
            // 直接用user会有异步信息还未执行的时候，下面的用户信息已经更新的问题
            AdminUser checkUser = new AdminUser();
            BeanUtils.copyProperties(user, checkUser);

            // 异地登录校验
            CompletableFuture.runAsync(
                    () -> checkRemoteLogin(checkUser, ipHomePlace, requestIp, agent),
                    applicationTaskExecutor);

            user.setAgent(agent);
            user.setIpHomePlace(ipHomePlace);
        }

        // 角色与后端权限分别加载，菜单仅用于前端导航，不再作为接口权限使用。
        AdminAuthorizationService.Snapshot authorization = authorizationService.load(user.getId());
        LoginPrincipal principal = LoginPrincipal.builder()
                .subjectId(user.getId())
                .subjectType(SecurityRealm.ADMIN)
                .account(user.getLoginCode())
                .displayName(user.getLoginName())
                .loginMode(LoginMode.PASSWORD)
                .roles(authorization.roles())
                .permissions(authorization.permissions())
                .loginTime(LocalDateTime.now())
                .build();
        String token = loginSessionService.loginAdmin(principal, LoginDeviceType.PC);

        // 修改用户数据
        updateById(user);

        // 返回token
        return new LoginRes(token);
    }

    @Override
    public Boolean resetPwdEmail(ResetPwdEmailReq req) {
        AdminUser user = getUserByEmail(req.getEmail());
        if (user == null) {
            // 管理端同样隐藏账号存在性，防止外部枚举管理员邮箱。
            return Boolean.TRUE;
        }
        long tokenSeconds = TimeUnit.MINUTES.toSeconds(resetPwdProperties.getResetPwdTime());
        String sendingKey = RedisConstant.ADMIN_RESET_PWD_CONTENT + req.getEmail();
        ApiAssert.isTrue(ApiCode.MAIL_EXIST,
                redissonCache.putStringIfAbsent(sendingKey, "sending", tokenSeconds));

        String countKey = RedisConstant.ADMIN_RESET_PWD_NUM + req.getEmail();
        long count = redissonCache.increment(countKey, DateUtils.theRestOfTheDaySecond());
        if (count > resetPwdProperties.getMaxNumber()) {
            redissonCache.decrement(countKey);
            redissonCache.remove(sendingKey);
            throw new com.yxx.common.exceptions.ApiException(ApiCode.RESET_PWD_MAX);
        }

        try {
            PasswordResetTokenPayload payload = new PasswordResetTokenPayload(
                    SecurityRealm.ADMIN, user.getId(), user.getEmail());
            String token = SaTempUtil.createToken(payload, tokenSeconds);
            String resetPassHref = resetPwdProperties.getBasePath() + "?token=" + token;
            String emailContent = resetPwdProperties.getResetPwdContent().replace("{url}", resetPassHref)
                    .replace("{time}", String.valueOf(resetPwdProperties.getResetPwdTime()))
                    .replace("{domain}", myWebProperties.getDomain())
                    .replace("{formName}", mailProperties.getFromName())
                    .replace("{form}", mailProperties.getFrom());
            mailUtils.baseSendMail(req.getEmail(), EmailSubjectConstant.RESET_PWD, emailContent, true);
            redissonCache.putString(sendingKey, token, tokenSeconds);
            return Boolean.TRUE;
        } catch (RuntimeException exception) {
            redissonCache.decrement(countKey);
            redissonCache.remove(sendingKey);
            throw exception;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean resetPwd(ResetPwdReq req) {
        // 原子取得一次性令牌消费权，避免同一个重置链接被并发使用。
        PasswordResetTokenPayload payload = oneTimeTemporaryTokenService
                .reserve(req.getToken(), PasswordResetTokenPayload.class)
                .filter(value -> SecurityRealm.ADMIN.equals(value.realm()))
                .orElse(null);
        ApiAssert.isTrue(ApiCode.RESET_PWD_TOKEN_ERROR, payload != null);
        AdminUser user = getById(payload.subjectId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                ObjectUtil.isNotNull(user) && payload.email().equals(user.getEmail()));

        // 使用统一 BCrypt 编码器生成新密码。
        String password = passwordEncoder.encode(req.getNewPassword());
        // 根据邮箱修改密码
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getPassword, password));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);

        sessionInvalidationService.invalidateAdminAfterCommit(user.getId());
        return Boolean.TRUE;
    }


    @Override
    public AdminUser getUserByEmail(String email) {
        // 根据邮箱号获取用户信息
        return getOne(new LambdaUpdateWrapper<AdminUser>().eq(AdminUser::getEmail, email));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean editPwd(EditPwdReq req) {
        // 根据登录id 获取该用户详情
        Long userId = loginSessionService.currentAdmin()
                .map(LoginPrincipal::getSubjectId)
                .orElseThrow(() -> new com.yxx.common.exceptions.ApiException(ApiCode.TOKEN_ERROR));
        AdminUser user = getById(userId);

        // 匹对请求参数中的旧密码是否正确
        ApiAssert.isTrue(ApiCode.ORIGINAL_PASSWORD_ERROR, passwordEncoder.matches(req.getPassword(), user.getPassword()));

        // 新密码继续使用统一 BCrypt 策略保存。
        String newPassword = passwordEncoder.encode(req.getNewPassword());
        // 根据用户id修改新密码
        boolean updated = update(new LambdaUpdateWrapper<AdminUser>()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getPassword, newPassword));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        sessionInvalidationService.invalidateAdminAfterCommit(userId);
        return Boolean.TRUE;
    }

    void checkRemoteLogin(AdminUser user, String ipHomePlace, String requestIp, String requestAgent) {
        if (Boolean.TRUE.equals(ipProperties.getCheck())) {
            log.info("异地登录校验~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            if (IpUtil.isValidIPv4(requestIp)) {
                // 判断是否发送过异常登录通知
                boolean exists = redissonCache.exists(RedisConstant.IP_UNUSUAL_LOGIN + user.getId());
                // 如果没有发送过，进行校验
                if (!exists) {
                    // 如果用户ip归属地不为空，且与当前登录ip归属地不同 登录设备名称不为空且与当前设备不同
                    if (CharSequenceUtil.isNotBlank(user.getAgent()) && !user.getAgent().equals(requestAgent) &&
                            CharSequenceUtil.isNotBlank(user.getIpHomePlace()) && !user.getIpHomePlace().equals(ipHomePlace)) {
                        // 获取ip归属地
                        String unusual = addressUtil.getIpHomePlace(requestIp, 3);
                        // 邮件正文
                        String time = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN);
                        String emailContent = mailProperties.getIpUnusualContent().replace("{time}", time)
                                .replace("{ip}", requestIp)
                                .replace("{address}", unusual)
                                .replace("{agent}", requestAgent)
                                .replace("{domain}", myWebProperties.getDomain())
                                .replace("{formName}", mailProperties.getFromName())
                                .replace("{form}", mailProperties.getFrom());
                        // 发送邮件通知
                        mailUtils.baseSendMail(user.getEmail(), EmailSubjectConstant.IP_UNUSUAL, emailContent, true);
                        // 加入redis(一天提醒一次)
                        // 今天剩余时间
                        Long residueTime = DateUtils.theRestOfTheDaySecond();
                        redissonCache.put(RedisConstant.IP_UNUSUAL_LOGIN + user.getId(), Boolean.TRUE, residueTime);
                    }
                }
            } else {
                log.info("非ipv4");
            }
            log.info("异地登录校验结束~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }
    }
}
