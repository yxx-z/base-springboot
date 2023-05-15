package com.yxx.business.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxx.business.mapper.UserMapper;
import com.yxx.business.model.entity.User;
import com.yxx.business.model.request.LoginReq;
import com.yxx.business.model.request.UserRegisterReq;
import com.yxx.business.model.response.LoginRes;
import com.yxx.business.service.UserService;
import com.yxx.common.constant.LoginDevice;
import com.yxx.common.core.model.LoginUser;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.auth.LoginUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

/**
 * @author yxx
 * @since 2022-11-12 13:54
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
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
        // 登录
        LoginUtils.login(loginUser, LoginDevice.PC);
        // 返回token
        return new LoginRes(loginUser.getToken());
    }

    @Override
    public Boolean register(UserRegisterReq req) {
        // 根据注册账号查询用户信息
        User userByLoginCode = getOne(new LambdaQueryWrapper<User>().eq(User::getLoginCode, req.getLoginCode()));
        // 根据注册手机号查询用户信息
        User userByPhone = getOne(new LambdaQueryWrapper<User>().eq(User::getLinkPhone, req.getLinkPhone()));
        // 如果存在该账号信息 表示用户已存在，抛出提示
        ApiAssert.isTrue(ApiCode.USER_EXIST, ObjectUtil.isNull(userByLoginCode) && ObjectUtil.isNull(userByPhone));
        // 加密请求参数中的密码
        String password = DigestUtils.md5DigestAsHex(req.getPassword().getBytes());

        // 初始化用户类
        User user = new User();
        // 拷贝赋值数据
        BeanUtils.copyProperties(req, user);
        // 设置加密后密码
        user.setPassword(password);
        // 插入
        return save(user);
    }
}
