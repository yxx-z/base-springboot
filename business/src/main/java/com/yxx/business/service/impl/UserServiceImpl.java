package com.yxx.business.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.UserMapper;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.model.request.*;
import com.yxx.business.service.UserIdentityService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.service.UserService;
import com.yxx.common.constant.EmailSubject;
import com.yxx.common.constant.RedisKeyPrefix;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.utils.AccountNormalizer;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.redis.RedissonCache;
import com.yxx.framework.security.PasswordResetMailService;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.context.OneTimeTemporaryTokenService;
import com.yxx.security.context.OneTimeVerificationCodeService;
import com.yxx.security.context.SessionInvalidationService;
import com.yxx.security.model.PasswordResetTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserRoleService userRoleService;

    private final UserIdentityService userIdentityService;

    private final LoginSessionService loginSessionService;

    private final SessionInvalidationService sessionInvalidationService;

    private final OneTimeTemporaryTokenService oneTimeTemporaryTokenService;

    private final OneTimeVerificationCodeService oneTimeVerificationCodeService;

    private final RedissonCache redissonCache;

    private final MailUtils mailUtils;

    private final MailProperties mailProperties;

    private final MyWebProperties myWebProperties;

    private final PasswordResetMailService passwordResetMailService;

    /**
     * 统一密码编码器，确保注册、登录、修改和重置密码采用同一套 BCrypt 策略。
     */
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findById(Long userId) {
        return getById(userId);
    }

    @Override
    public boolean create(User user) {
        return save(user);
    }

    @Override
    public boolean updateLoginMetadata(Long userId, String agent, String ipHomePlace) {
        return update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getAgent, agent)
                .set(User::getIpHomePlace, ipHomePlace));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, boolean enabled) {
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, findById(userId) != null);
        boolean updated = update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, enabled));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        if (!enabled) {
            sessionInvalidationService.invalidateUserAfterCommit(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, findById(userId) != null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, removeById(userId));
        sessionInvalidationService.invalidateUserAfterCommit(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReq req) {
        String loginCode = AccountNormalizer.normalizeLoginCode(req.getLoginCode());
        String email = AccountNormalizer.normalizeEmail(req.getEmail());
        String displayName = AccountNormalizer.normalizeDisplayName(req.getLoginName());
        String phone = AccountNormalizer.normalizeMainlandPhone(req.getLinkPhone());
        OneTimeVerificationCodeService.ReservationResult captchaResult =
                oneTimeVerificationCodeService.reserve(
                        RedisKeyPrefix.EMAIL_REGISTER + email, req.getCaptcha());
        ApiAssert.isTrue(ApiCode.CAPTCHA_NOT_EXIST,
                captchaResult != OneTimeVerificationCodeService.ReservationResult.NOT_FOUND);
        ApiAssert.isTrue(ApiCode.CAPTCHA_ERROR,
                captchaResult == OneTimeVerificationCodeService.ReservationResult.RESERVED);

        // 根据注册账号查询用户信息
        UserIdentity userByLoginCode = userIdentityService
                .findAnyIdentity(LoginMode.PASSWORD, loginCode)
                .orElse(null);
        // 根据注册邮箱号查询用户信息
        User userByEmail = getUserByEmail(email);
        // 如果存在该账号信息 表示用户已存在，抛出提示
        ApiAssert.isTrue(ApiCode.USER_EXIST,
                userByLoginCode == null && userByEmail == null);
        // 对密码进行哈希
        String password = passwordEncoder.encode(req.getPassword());


        // 初始化用户类
        User user = new User();
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(Boolean.TRUE);
        // 插入
        boolean saveResult = create(user);

        // 密码只保存在登录身份表，用户主体不再与某一种认证方式绑定。
        UserIdentity passwordIdentity = new UserIdentity();
        passwordIdentity.setUserId(user.getId());
        passwordIdentity.setIdentityType(LoginMode.PASSWORD);
        passwordIdentity.setIdentifier(loginCode);
        passwordIdentity.setCredential(password);
        passwordIdentity.setVerified(Boolean.TRUE);
        passwordIdentity.setStatus(Boolean.TRUE);
        boolean identityResult = userIdentityService.create(passwordIdentity);

        // 设置默认角色
        Boolean result = userRoleService.assignMemberRole(user);

        // 校验操作结果
        if (!(saveResult && identityResult && result)) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }

    }

    @Override
    public void resetPwdEmail(ResetPwdEmailReq req) {
        String email = AccountNormalizer.normalizeEmail(req.getEmail());
        User user = getUserByEmail(email);
        if (user == null) {
            // 找回密码接口始终返回成功，避免被用于枚举平台注册邮箱。
            return;
        }
        passwordResetMailService.send(
                SecurityRealm.USER, user.getId(), email,
                RedisKeyPrefix.USER_RESET_PASSWORD_TOKEN,
                RedisKeyPrefix.USER_RESET_PASSWORD_COUNT);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetPwd(ResetPwdReq req) {
        // 原子取得一次性令牌消费权，避免同一个重置链接被并发使用。
        PasswordResetTokenPayload payload = oneTimeTemporaryTokenService
                .reserve(req.getToken(), PasswordResetTokenPayload::decode)
                .filter(value -> SecurityRealm.USER.equals(value.realm()))
                .orElse(null);
        ApiAssert.isTrue(ApiCode.RESET_PWD_TOKEN_ERROR, payload != null);
        // 通过令牌绑定的内部 ID 查询，避免邮箱变化或跨安全域令牌造成误操作。
        User user = findById(payload.subjectId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                user != null && payload.email().equals(user.getEmail()));

        // 使用统一 BCrypt 编码器生成新密码，避免重置密码后无法通过登录校验。
        String password = passwordEncoder.encode(req.getNewPassword());
        // 根据邮箱修改密码
        UserIdentity passwordIdentity = userIdentityService
                .findByUserId(user.getId(), LoginMode.PASSWORD)
                .orElse(null);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, passwordIdentity != null);
        boolean updated = userIdentityService.updateCredential(passwordIdentity.getId(), password);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);

        // 凭据变更只有在事务成功提交后才注销旧会话。
        sessionInvalidationService.invalidateUserAfterCommit(user.getId());
    }

    @Override
    public User getUserByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, AccountNormalizer.normalizeEmail(email)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPwd(EditPwdReq req) {
        // 根据登录id 获取该用户详情
        Long userId = loginSessionService.currentUser()
                .map(com.yxx.security.model.LoginPrincipal::getSubjectId)
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        UserIdentity passwordIdentity = userIdentityService
                .findByUserId(userId, LoginMode.PASSWORD)
                .orElseThrow(() -> new ApiException(ApiCode.PASSWORD_ERROR));

        // 匹对请求参数中的旧密码是否正确
        ApiAssert.isTrue(ApiCode.ORIGINAL_PASSWORD_ERROR,
                passwordEncoder.matches(req.getPassword(), passwordIdentity.getCredential()));

        // 新密码继续使用统一 BCrypt 策略保存。
        String newPassword = passwordEncoder.encode(req.getNewPassword());
        // 根据用户id修改新密码
        boolean updated = userIdentityService.updateCredential(passwordIdentity.getId(), newPassword);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        sessionInvalidationService.invalidateUserAfterCommit(userId);
    }

    @Override
    public void sendRegisterCaptcha(RegisterCaptchaReq req) {
        String email = AccountNormalizer.normalizeEmail(req.getEmail());
        // 判断该邮箱是否注册过
        User userByEmail = getUserByEmail(email);
        // 如果注册过，抛出提示
        ApiAssert.isTrue(ApiCode.EMAIL_EXIST, userByEmail == null);
        // 原子占用发送窗口；并发请求中只有一个请求可以真正发送邮件。
        String captchaKey = RedisKeyPrefix.EMAIL_REGISTER + email;
        long captchaSeconds = TimeUnit.MINUTES.toSeconds(mailProperties.getRegisterTime());
        ApiAssert.isTrue(ApiCode.MAIL_EXIST,
                redissonCache.putStringIfAbsent(captchaKey, "sending", captchaSeconds));

        String countKey = RedisKeyPrefix.EMAIL_REGISTER_NUM + email;
        long count = redissonCache.increment(countKey, DateUtils.secondsUntilNextDay());
        if (count > mailProperties.getRegisterMax()) {
            redissonCache.decrement(countKey);
            redissonCache.remove(captchaKey);
            throw new ApiException(ApiCode.REGISTER_MAX);
        }

        // 获得六位随机数
        int random = RandomUtil.randomInt(100000, 999999);
        // 拼接邮件内容
        String resultText = mailProperties.getRegisterContent()
                .replace("{captcha}", String.valueOf(random))
                .replace("{time}", String.valueOf(mailProperties.getRegisterTime()))
                .replace("{domain}", myWebProperties.getDomain())
                .replace("{formName}", mailProperties.getFromName())
                .replace("{form}", mailProperties.getFrom());

        try {
            mailUtils.baseSendMail(email, EmailSubject.REGISTER, resultText, true);
            redissonCache.putString(captchaKey, String.valueOf(random), captchaSeconds);
        } catch (RuntimeException exception) {
            redissonCache.decrement(countKey);
            redissonCache.remove(captchaKey);
            throw exception;
        }
    }

}
