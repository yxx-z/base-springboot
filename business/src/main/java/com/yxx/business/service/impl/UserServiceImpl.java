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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.yxx.security.constant.LoginMode;
import com.yxx.security.context.LoginSessionService;

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
        // 校验邮件是否已经发送过
        ApiAssert.isFalse(ApiCode.MAIL_EXIST, redissonCache.exists(RedisConstant.RESET_PWD_CONTENT + req.getEmail()));

        // 根据邮箱 获取用户
        User user = getUserByEmail(req.getEmail());
        // 如果用户不存在，抛出提示
        ApiAssert.isTrue(ApiCode.EMAIL_NOT_REGISTER, ObjectUtil.isNotNull(user));

        // 从redis中获取该邮箱号今日找回密码次数
        Integer number = redissonCache.get(RedisConstant.RESET_PWD_NUM + req.getEmail());
        // 如果找回次数不为空，并且大于等于设置的最大次数，抛出异常
        ApiAssert.isFalse(ApiCode.RESET_PWD_MAX, number != null && number >= resetPwdProperties.getMaxNumber());

        // 创建临时token 临时时间15分钟
        String token = SaTempUtil.createToken(req.getEmail(), resetPwdProperties.getResetPwdTime());
        // 找回密码路径 拼接token
        String resetPassHref = resetPwdProperties.getBasePath() + "?token=" + token;
        // 邮件内容
        String emailContent = resetPwdProperties.getResetPwdContent().replace("{url}", resetPassHref)
                .replace("{time}", String.valueOf(resetPwdProperties.getResetPwdTime()))
                .replace("{domain}", myWebProperties.getDomain())
                .replace("{formName}", mailProperties.getFromName())
                .replace("{form}", mailProperties.getFrom());
        // 发送html格式邮件
        mailUtils.baseSendMail(req.getEmail(), EmailSubjectConstant.RESET_PWD, emailContent, true);

        // 将临时token 存入redis中
        redissonCache.putString(RedisConstant.RESET_PWD_CONTENT + req.getEmail(), token, 900, TimeUnit.SECONDS);

        // 防止恶意刷邮件
        // 今天剩余时间
        Long time = DateUtils.theRestOfTheDaySecond();
        // 添加找回密码次数到redis中 找回密码次数+1
        redissonCache.put(RedisConstant.RESET_PWD_NUM + req.getEmail(), Optional.ofNullable(number).map(x -> ++x).orElse(1), time);

        return Boolean.TRUE;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean resetPwd(ResetPwdReq req) {
        // 获取临时token的存活时间 -1 代表永久，-2 代表token无效
        long timeout = SaTempUtil.getTimeout(req.getToken());
        // 如果token无效，抛出提示
        ApiAssert.isFalse(ApiCode.RESET_PWD_TOKEN_ERROR, timeout == -2);
        // 获取token对应的邮箱
        String email = SaTempUtil.parseToken(req.getToken(), String.class);
        // 根据邮箱获取用户
        User user = getUserByEmail(email);
        // 如果用户为空，抛出提示
        ApiAssert.isTrue(ApiCode.DATE_ERROR, ObjectUtil.isNotNull(user));

        // 使用统一 BCrypt 编码器生成新密码，避免重置密码后无法通过登录校验。
        String password = passwordEncoder.encode(req.getNewPassword());
        // 根据邮箱修改密码
        UserIdentity passwordIdentity = userIdentityService
                .findByUserId(user.getId(), LoginMode.PASSWORD)
                .orElse(null);
        ApiAssert.isTrue(ApiCode.DATE_ERROR, passwordIdentity != null);
        boolean updated = userIdentityService.update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getId, passwordIdentity.getId())
                .set(UserIdentity::getCredential, password));
        ApiAssert.isTrue(ApiCode.SYSTEM_ERROR, updated);

        // 数据库更新成功后再销毁一次性令牌，防止更新失败时用户无法重新提交。
        SaTempUtil.deleteToken(req.getToken());
        return Boolean.TRUE;
    }

    @Override
    public User getUserByEmail(String email) {
        // 根据邮箱号获取用户信息
        return getOne(new LambdaUpdateWrapper<User>().eq(User::getEmail, email));
    }

    @Override
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
        return userIdentityService.update(new LambdaUpdateWrapper<UserIdentity>()
                .eq(UserIdentity::getId, passwordIdentity.getId())
                .set(UserIdentity::getCredential, newPassword));
    }

    @Override
    public Boolean sendRegisterCaptcha(RegisterCaptchaReq req) {
        // 判断该邮箱是否注册过
        User userByEmail = getUserByEmail(req.getEmail());
        // 如果注册过，抛出提示
        ApiAssert.isTrue(ApiCode.EMAIL_EXIST, ObjectUtil.isNull(userByEmail));
        // 判断该邮箱是否已经发送过验证码
        Boolean emailIsSend = redissonCache.isExists(RedisConstant.EMAIL_REGISTER + req.getEmail());
        // 如果已经发送过，抛出提示
        ApiAssert.isFalse(ApiCode.MAIL_EXIST, emailIsSend);

        // 防止恶意发送邮件
        // 从redis中获取该邮箱号今日注册次数
        Integer number = redissonCache.get(RedisConstant.EMAIL_REGISTER_NUM + req.getEmail());
        // 如果注册次数不为空，并且大于等于设置的最大次数，抛出异常
        ApiAssert.isFalse(ApiCode.REGISTER_MAX, number != null && number >= mailProperties.getRegisterMax());

        // 获得六位随机数
        int random = RandomUtil.randomInt(100000, 999999);
        // 拼接邮件内容
        String resultText = mailProperties.getRegisterContent()
                .replace("{captcha}", String.valueOf(random))
                .replace("{time}", String.valueOf(mailProperties.getRegisterTime()))
                .replace("{domain}", myWebProperties.getDomain())
                .replace("{formName}", mailProperties.getFromName())
                .replace("{form}", mailProperties.getFrom());

        // 发送邮件
        mailUtils.baseSendMail(req.getEmail(), EmailSubjectConstant.REGISTER_SUBJECT, resultText, true);

        // 存入redis
        redissonCache.putString(RedisConstant.EMAIL_REGISTER + req.getEmail(), String.valueOf(random),
                mailProperties.getRegisterTime(), TimeUnit.MINUTES);

        // 防止恶意发送邮件
        // 今天剩余时间
        Long time = DateUtils.theRestOfTheDaySecond();
        // 添加注册次数到redis中 注册次数+1
        redissonCache.put(RedisConstant.EMAIL_REGISTER_NUM + req.getEmail(), Optional.ofNullable(number).map(x -> ++x).orElse(1), time);

        return Boolean.TRUE;
    }

}
