package com.yxx.business.service.impl;

import cn.dev33.satoken.temp.SaTempUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.UserMapper;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.request.*;
import com.yxx.business.model.entity.UserIdentity;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.service.UserIdentityService;
import com.yxx.business.service.UserService;
import com.yxx.common.constant.EmailSubjectConstant;
import com.yxx.common.constant.RedisConstant;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.properties.ResetPwdProperties;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.constant.SecurityRealm;
import com.yxx.security.context.LoginSessionService;
import com.yxx.security.context.OneTimeTemporaryTokenService;
import com.yxx.security.context.SessionInvalidationService;
import com.yxx.security.model.PasswordResetTokenPayload;

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

    private final RedissonCache redissonCache;

    private final MailUtils mailUtils;

    private final MailProperties mailProperties;

    private final ResetPwdProperties resetPwdProperties;

    private final MyWebProperties myWebProperties;

    /**
     * 统一密码编码器，确保注册、登录、修改和重置密码采用同一套 BCrypt 策略。
     */
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean register(UserRegisterReq req) {
        // 判断该邮箱是否存在验证码
        Boolean emailIsSend = redissonCache.isExists(RedisConstant.EMAIL_REGISTER + req.getEmail());
        // 如果不存在，抛出提示
        ApiAssert.isTrue(ApiCode.CAPTCHA_NOT_EXIST, emailIsSend);

        // 获取验证码
        String captcha = redissonCache.getString(RedisConstant.EMAIL_REGISTER + req.getEmail());
        //对比用户传入的验证码是否正确
        ApiAssert.isTrue(ApiCode.CAPTCHA_ERROR, req.getCaptcha().equals(captcha));

        // 根据注册账号查询用户信息
        UserIdentity userByLoginCode = userIdentityService
                .findIdentity(LoginMode.PASSWORD, req.getLoginCode())
                .orElse(null);
        // 根据注册邮箱号查询用户信息
        User userByEmail = getUserByEmail(req.getEmail());
        // 如果存在该账号信息 表示用户已存在，抛出提示
        ApiAssert.isTrue(ApiCode.USER_EXIST,
                ObjectUtil.isNull(userByLoginCode) && ObjectUtil.isNull(userByEmail));
        // 对密码进行哈希
        String password = passwordEncoder.encode(req.getPassword());


        // 初始化用户类
        User user = new User();
        user.setDisplayName(req.getLoginName());
        user.setPhone(req.getLinkPhone());
        user.setEmail(req.getEmail());
        user.setStatus(Boolean.TRUE);
        // 插入
        boolean saveResult = save(user);

        // 密码只保存在登录身份表，用户主体不再与某一种认证方式绑定。
        UserIdentity passwordIdentity = new UserIdentity();
        passwordIdentity.setUserId(user.getId());
        passwordIdentity.setIdentityType(LoginMode.PASSWORD);
        passwordIdentity.setIdentifier(req.getLoginCode());
        passwordIdentity.setCredential(password);
        passwordIdentity.setVerified(Boolean.TRUE);
        passwordIdentity.setStatus(Boolean.TRUE);
        boolean identityResult = userIdentityService.save(passwordIdentity);

        // 设置默认角色
        Boolean result = userRoleService.setDefaultRole(user);

        // 删除该邮箱注册验证码
        redissonCache.remove(RedisConstant.EMAIL_REGISTER + req.getEmail());

        // 校验操作结果
        if (!(saveResult && identityResult && result)) {
            throw new ApiException(ApiCode.SYSTEM_ERROR);
        }

        return Boolean.TRUE;
    }

    @Override
    public Boolean resetPwdEmail(ResetPwdEmailReq req) {
        // 先确认邮箱对应用户，再通过 Redis 原子占位避免并发重复发送。
        User user = getUserByEmail(req.getEmail());
        if (user == null) {
            // 找回密码接口始终返回成功，避免被用于枚举平台注册邮箱。
            return Boolean.TRUE;
        }
        long tokenSeconds = TimeUnit.MINUTES.toSeconds(resetPwdProperties.getResetPwdTime());
        String sendingKey = RedisConstant.USER_RESET_PWD_CONTENT + req.getEmail();
        ApiAssert.isTrue(ApiCode.MAIL_EXIST,
                redissonCache.putStringIfAbsent(sendingKey, "sending", tokenSeconds));

        String countKey = RedisConstant.USER_RESET_PWD_NUM + req.getEmail();
        long count = redissonCache.increment(countKey, DateUtils.theRestOfTheDaySecond());
        if (count > resetPwdProperties.getMaxNumber()) {
            redissonCache.decrement(countKey);
            redissonCache.remove(sendingKey);
            throw new ApiException(ApiCode.RESET_PWD_MAX);
        }

        try {
            PasswordResetTokenPayload payload = new PasswordResetTokenPayload(
                    SecurityRealm.USER, user.getId(), user.getEmail());
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
            // 邮件发送失败时归还频次并释放占位，允许用户重试。
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
                .filter(value -> SecurityRealm.USER.equals(value.realm()))
                .orElse(null);
        ApiAssert.isTrue(ApiCode.RESET_PWD_TOKEN_ERROR, payload != null);
        // 通过令牌绑定的内部 ID 查询，避免邮箱变化或跨安全域令牌造成误操作。
        User user = getById(payload.subjectId());
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST,
                ObjectUtil.isNotNull(user) && payload.email().equals(user.getEmail()));

        // 使用统一 BCrypt 编码器生成新密码，避免重置密码后无法通过登录校验。
        String password = passwordEncoder.encode(req.getNewPassword());
        // 根据邮箱修改密码
        UserIdentity passwordIdentity = userIdentityService
                .findByUserId(user.getId(), LoginMode.PASSWORD)
                .orElse(null);
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, passwordIdentity != null);
        boolean updated = userIdentityService.update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getId, passwordIdentity.getId())
                .set(UserIdentity::getCredential, password));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);

        // 凭据变更只有在事务成功提交后才注销旧会话。
        sessionInvalidationService.invalidateUserAfterCommit(user.getId());
        return Boolean.TRUE;
    }

    @Override
    public User getUserByEmail(String email) {
        // 根据邮箱号获取用户信息
        return getOne(new LambdaUpdateWrapper<User>().eq(User::getEmail, email));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean editPwd(EditPwdReq req) {
        // 根据登录id 获取该用户详情
        Long userId = loginSessionService.currentUser()
                .map(com.yxx.security.model.LoginPrincipal::getSubjectId)
                .orElseThrow(() -> new ApiException(ApiCode.TOKEN_ERROR));
        User user = getById(userId);
        UserIdentity passwordIdentity = userIdentityService
                .findByUserId(userId, LoginMode.PASSWORD)
                .orElseThrow(() -> new ApiException(ApiCode.PASSWORD_ERROR));

        // 匹对请求参数中的旧密码是否正确
        ApiAssert.isTrue(ApiCode.ORIGINAL_PASSWORD_ERROR,
                passwordEncoder.matches(req.getPassword(), passwordIdentity.getCredential()));

        // 新密码继续使用统一 BCrypt 策略保存。
        String newPassword = passwordEncoder.encode(req.getNewPassword());
        // 根据用户id修改新密码
        boolean updated = userIdentityService.update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getId, passwordIdentity.getId())
                .set(UserIdentity::getCredential, newPassword));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);
        sessionInvalidationService.invalidateUserAfterCommit(userId);
        return Boolean.TRUE;
    }

    @Override
    public Boolean sendRegisterCaptcha(RegisterCaptchaReq req) {
        // 判断该邮箱是否注册过
        User userByEmail = getUserByEmail(req.getEmail());
        // 如果注册过，抛出提示
        ApiAssert.isTrue(ApiCode.EMAIL_EXIST, ObjectUtil.isNull(userByEmail));
        // 原子占用发送窗口；并发请求中只有一个请求可以真正发送邮件。
        String captchaKey = RedisConstant.EMAIL_REGISTER + req.getEmail();
        long captchaSeconds = TimeUnit.MINUTES.toSeconds(mailProperties.getRegisterTime());
        ApiAssert.isTrue(ApiCode.MAIL_EXIST,
                redissonCache.putStringIfAbsent(captchaKey, "sending", captchaSeconds));

        String countKey = RedisConstant.EMAIL_REGISTER_NUM + req.getEmail();
        long count = redissonCache.increment(countKey, DateUtils.theRestOfTheDaySecond());
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
            mailUtils.baseSendMail(req.getEmail(), EmailSubjectConstant.REGISTER_SUBJECT, resultText, true);
            redissonCache.putString(captchaKey, String.valueOf(random), captchaSeconds);
            return Boolean.TRUE;
        } catch (RuntimeException exception) {
            redissonCache.decrement(countKey);
            redissonCache.remove(captchaKey);
            throw exception;
        }
    }

}
