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
import com.yxx.business.model.response.LoginRes;
import com.yxx.business.service.RoleMenuService;
import com.yxx.business.service.UserRoleService;
import com.yxx.business.service.UserService;
import com.yxx.common.constant.EmailSubjectConstant;
import com.yxx.common.constant.LoginDevice;
import com.yxx.common.constant.RedisConstant;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.auth.LoginUtils;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserRoleService userRoleService;

    private final RoleMenuService roleMenuService;

    private final RedissonCache redissonCache;

    private final MailUtils mailUtils;

    private final MailProperties mailProperties;

    @Value("${reset-password.base-path}")
    private String basePath;

    @Value("${reset-password.max-number}")
    private Integer maxNumber;

    @Override
    public LoginRes login(LoginReq request) {
        // 根据登录账号获取用户信息
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getLoginCode, request.getLoginCode()));
        // 如果用户信息不存在，抛出异常
        ApiAssert.isTrue(ApiCode.USER_NOT_EXIST, ObjectUtil.isNotNull(user));
        // 加密请求参数中的密码
        String password = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        // 如果请求参数中的密码加密后和数据库中不一致，抛出异常
        ApiAssert.isTrue(ApiCode.PASSWORD_ERROR, user.getPassword().equals(password));

        // 初始化登录信息
        LoginUser loginUser = new LoginUser();
        // 拷贝赋值数据
        BeanUtils.copyProperties(user, loginUser);
        // 设置登录时间
        loginUser.setLoginTime(LocalDateTime.now());
        // 获取该用户角色信息
        List<String> roleCodeList = userRoleService.loginUserRoleManage(user);
        // 赋值角色集合
        loginUser.setRolePermission(roleCodeList);
        // 根据角色code获取该用户菜单集合
        List<String> menuCodeList = roleMenuService.loginUserMenu(roleCodeList);
        // 赋值菜单集合
        loginUser.setMenuPermission(menuCodeList);

        // 登录
        LoginUtils.login(loginUser, LoginDevice.PC);
        // 返回token
        return new LoginRes(loginUser.getToken());
    }

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
        User userByLoginCode = getOne(
                new LambdaQueryWrapper<User>().eq(User::getLoginCode, req.getLoginCode()));
        // 根据注册邮箱号查询用户信息
        User userByEmail = getUserByEmail(req.getEmail());
        // 如果存在该账号信息 表示用户已存在，抛出提示
        ApiAssert.isTrue(ApiCode.USER_EXIST,
                ObjectUtil.isNull(userByLoginCode) && ObjectUtil.isNull(userByEmail));
        // 加密请求参数中的密码
        String password = DigestUtils.md5DigestAsHex(req.getPassword().getBytes());

        // 初始化用户类
        User user = new User();
        // 拷贝赋值数据
        BeanUtils.copyProperties(req, user);
        // 设置加密后密码
        user.setPassword(password);
        // 插入
        boolean saveResult = save(user);

        // 设置默认角色
        Boolean result = userRoleService.setDefaultRole(user);

        // 删除该邮箱注册验证码
        redissonCache.remove(RedisConstant.EMAIL_REGISTER + req.getEmail());

        // 校验操作结果
        if (!(saveResult && result)) {
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
        ApiAssert.isTrue(ApiCode.EMAIL_NOT_EXIST, ObjectUtil.isNotNull(user));

        // 创建临时token 临时时间15分钟
        String token = SaTempUtil.createToken(req.getEmail(), 900);
        // 找回密码路径 拼接token
        String resetPassHref = basePath + "?token=" + token;
        // 邮件内容
        String emailContent = "请勿回复本邮件.点击下面的链接,重设密码<br/><br/><a href=" + resetPassHref + " target='_BLANK'>点击我重新设置密码</a>" +
                "<br/><br/>tips:本邮件超过15分钟,链接将会失效，需要重新申请'找回密码'";
        // 发送html格式邮件
        mailUtils.baseSendMail(req.getEmail(), EmailSubjectConstant.RESET_PWD, emailContent, true);

        // 将临时token 存入redis中
        redissonCache.putString(RedisConstant.RESET_PWD_CONTENT + req.getEmail(), token, 900, TimeUnit.SECONDS);

        // 从redis中获取该邮箱号今日找回密码次数
        Integer number = redissonCache.get(RedisConstant.RESET_PWD_NUM + req.getEmail());
        // 如果找回次数不为空，并且大于等于设置的最大次数，抛出异常
        ApiAssert.isFalse(ApiCode.RESET_PWD_MAX, number != null && number >= maxNumber);
        // 今天剩余时间
        Long time = DateUtils.theRestOfTheDaySecond();
        // 添加找回密码次数到redis中 找回密码次数+1
        redissonCache.put(RedisConstant.RESET_PWD_NUM + req.getEmail(), Optional.ofNullable(number).map(x -> ++x).orElse(1), time);

        return Boolean.TRUE;
    }

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

        // 删除临时token
        SaTempUtil.deleteToken(req.getToken());

        // 加密密码
        String password = DigestUtils.md5DigestAsHex(req.getNewPassword().getBytes());
        // 根据邮箱修改密码
        return update(new LambdaUpdateWrapper<User>().eq(User::getEmail, email).set(User::getPassword, password));
    }


    @Override
    public User getUserByEmail(String email){
        // 根据邮箱号获取用户信息
        return getOne(new LambdaUpdateWrapper<User>().eq(User::getEmail, email));
    }

    @Override
    public Boolean editPwd(EditPwdReq req) {
        // 根据登录id 获取该用户详情
        User user = getById(LoginUtils.getUserId());

        // 加密请求参数中的旧密码
        String password = DigestUtils.md5DigestAsHex(req.getPassword().getBytes());
        // 匹对请求参数中的旧密码是否正确
        ApiAssert.isFalse(ApiCode.ORIGINAL_PASSWORD_ERROR, user.getPassword().equals(password));

        // 加密新密码
        String newPassword = DigestUtils.md5DigestAsHex(req.getNewPassword().getBytes());
        // 根据用户id修改新密码
        return update(new LambdaUpdateWrapper<User>().eq(User::getId, user.getId()).set(User::getPassword, newPassword));
    }

    @Override
    public Boolean sendRegisterCaptcha(RegisterCaptchaReq req) {
        // 判断该邮箱是否已经发送过验证码
        // 如果已经发送过，抛出提示
        ApiAssert.isFalse(ApiCode.MAIL_EXIST, redissonCache.isExists(RedisConstant.EMAIL_REGISTER + req.getEmail()));

        // 获得六位随机数
        int random = RandomUtil.randomInt(100000, 999999);
        // 拼接邮件内容
        String resultText = String.format(EmailSubjectConstant.REGISTER_CONTENT, random, mailProperties.getRegisterTime());

        // 发送邮件
        mailUtils.baseSendMail(req.getEmail(), EmailSubjectConstant.REGISTER_SUBJECT, resultText, false);

        // 验证码存入redis
        redissonCache.putString(RedisConstant.EMAIL_REGISTER + req.getEmail(), String.valueOf(random),
                mailProperties.getRegisterTime(), TimeUnit.MINUTES);

        // 防止恶意发送邮件
        // 从redis中获取该邮箱号今日注册次数
        Integer number = redissonCache.get(RedisConstant.EMAIL_REGISTER_NUM + req.getEmail());
        // 如果注册次数不为空，并且大于等于设置的最大次数，抛出异常
        ApiAssert.isFalse(ApiCode.REGISTER_MAX, number != null && number >= mailProperties.getRegisterMax());
        // 今天剩余时间
        Long time = DateUtils.theRestOfTheDaySecond();
        // 添加注册次数到redis中 注册次数+1
        redissonCache.put(RedisConstant.EMAIL_REGISTER_NUM + req.getEmail(), Optional.ofNullable(number).map(x -> ++x).orElse(1), time);

        return Boolean.TRUE;
    }
}
