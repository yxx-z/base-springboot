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
        // 登录元数据采用定向 UPDATE，避免用不完整实体覆盖昵称、邮箱等业务字段。
        return update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getAgent, agent)
                .set(User::getIpHomePlace, ipHomePlace));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, boolean enabled) {
        // 先明确区分“不存在”和“更新失败”，便于接口返回稳定、可诊断的错误语义。
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, findById(userId) != null);
        boolean updated = update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, enabled));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        if (!enabled) {
            // 等事务提交后再注销会话；若数据库回滚，当前可用状态和会话状态应保持一致。
            sessionInvalidationService.invalidateUserAfterCommit(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        // 删除主体前先校验存在性，防止幂等删除掩盖调用方传错 ID。
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, findById(userId) != null);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, removeById(userId));
        sessionInvalidationService.invalidateUserAfterCommit(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReq req) {
        // 所有唯一性查询和持久化统一使用规范化值，避免大小写、空格造成重复账号。
        String loginCode = AccountNormalizer.normalizeLoginCode(req.getLoginCode());
        String email = AccountNormalizer.normalizeEmail(req.getEmail());
        String displayName = AccountNormalizer.normalizeDisplayName(req.getLoginName());
        String phone = AccountNormalizer.normalizeMainlandPhone(req.getLinkPhone());
        OneTimeVerificationCodeService.ReservationResult captchaResult =
                oneTimeVerificationCodeService.reserve(
                        RedisKeyPrefix.EMAIL_REGISTER + email, req.getCaptcha());
        // 区分验证码不存在与验证码错误，便于前端提示重新获取或重新输入。
        ApiAssert.isTrue(ApiCode.CAPTCHA_NOT_EXIST,
                captchaResult != OneTimeVerificationCodeService.ReservationResult.NOT_FOUND);
        ApiAssert.isTrue(ApiCode.CAPTCHA_ERROR,
                captchaResult == OneTimeVerificationCodeService.ReservationResult.RESERVED);

        // 密码账号位于身份表，主体表只承载与认证方式无关的用户资料。
        UserIdentity userByLoginCode = userIdentityService
                .findAnyIdentity(LoginMode.PASSWORD, loginCode)
                .orElse(null);
        // 邮箱是主体级唯一联系方式，需要与登录账号分别检查。
        User userByEmail = getUserByEmail(email);
        // 应用层校验提供友好错误，数据库唯一约束仍负责最终并发一致性。
        ApiAssert.isTrue(ApiCode.USER_EXIST,
                userByLoginCode == null && userByEmail == null);
        // 只保存不可逆密码摘要，明文密码不得进入实体、日志或缓存。
        String password = passwordEncoder.encode(req.getPassword());
        // 先创建统一用户主体，使后续任意认证身份都能绑定到稳定的内部 userId。
        User user = new User();
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(Boolean.TRUE);
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

        // 注册用户立即获得框架约定的基础成员角色，避免存在无授权归属的主体。
        Boolean result = userRoleService.assignMemberRole(user);

        // 三项写入处于同一事务；任一失败均抛出异常并整体回滚。
        if (!(saveResult && identityResult && result)) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }

    }

    @Override
    public void resetPwdEmail(ResetPwdEmailReq req) {
        // 邮箱先规范化，确保查找、频控和令牌载荷使用相同标识。
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
                // realm 是跨用户端/管理端令牌的隔离边界，不能只凭 subjectId 操作。
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
        // 即便内部调用也再次规范化，避免未来新增调用方绕过统一邮箱规则。
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, AccountNormalizer.normalizeEmail(email)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPwd(EditPwdReq req) {
        // 只信任服务端会话中的稳定主体 ID，不接受请求参数指定要修改的用户。
        Long userId = loginSessionService.currentUser()
                .map(com.yxx.security.model.LoginPrincipal::getSubjectId)
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        UserIdentity passwordIdentity = userIdentityService
                .findByUserId(userId, LoginMode.PASSWORD)
                .orElseThrow(() -> new ApiException(ApiCode.PASSWORD_ERROR));

        // matches(明文, 摘要) 会按编码器规则验证；摘要不能与明文直接 equals 比较。
        ApiAssert.isTrue(ApiCode.ORIGINAL_PASSWORD_ERROR,
                passwordEncoder.matches(req.getPassword(), passwordIdentity.getCredential()));

        // 新密码继续使用统一 BCrypt 策略保存。
        String newPassword = passwordEncoder.encode(req.getNewPassword());
        // 只更新密码身份的凭据，不改动用户主体以及支付宝等其他身份。
        boolean updated = userIdentityService.updateCredential(passwordIdentity.getId(), newPassword);
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        // 密码修改提交成功后注销该用户所有旧 Token，阻止已泄露会话继续访问。
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
        // 每日发送上限按服务器自然日统计，计数键在次日边界自动失效。
        long count = redissonCache.increment(countKey, DateUtils.secondsUntilNextDay());
        if (count > mailProperties.getRegisterMax()) {
            // 超限请求不应占用计数和发送窗口，否则会延长合法用户的锁定时间。
            redissonCache.decrement(countKey);
            redissonCache.remove(captchaKey);
            throw new ApiException(ApiCode.REGISTER_MAX);
        }

        // 生成固定六位验证码，避免前导零导致邮件展示位数不一致。
        int random = RandomUtil.randomInt(100000, 999999);
        // 邮件模板由配置提供，业务代码只替换受控占位符，便于项目按品牌定制。
        String resultText = mailProperties.getRegisterContent()
                .replace("{captcha}", String.valueOf(random))
                .replace("{time}", String.valueOf(mailProperties.getRegisterTime()))
                .replace("{domain}", myWebProperties.getDomain())
                .replace("{formName}", mailProperties.getFromName())
                .replace("{form}", mailProperties.getFrom());

        try {
            // 只有邮件成功交给发送器后才用真实验证码替换 sending 占位。
            mailUtils.baseSendMail(email, EmailSubject.REGISTER, resultText, true);
            redissonCache.putString(captchaKey, String.valueOf(random), captchaSeconds);
        } catch (RuntimeException exception) {
            // 发送失败归还额度并释放窗口，让用户可以立即重试而不是等待验证码过期。
            redissonCache.decrement(countKey);
            redissonCache.remove(captchaKey);
            throw exception;
        }
    }

}
