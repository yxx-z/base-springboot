package com.yxx.common.utils.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yxx.common.constant.Constant;
import com.yxx.common.core.model.LoginUser;

import java.util.List;

/**
 * 登录鉴权工具
 *
 * @author yxx
 * @since 2022/4/13 14:22
 */
public class LoginUtils {

    /**
     * 登录系统
     *
     * @param loginUser 登录用户信息
     */
    public static void login(LoginUser loginUser) {
        StpUtil.login(loginUser.getLoginCode());
        setLoginUser(loginUser);
    }

    /**
     * 登录系统 并指定登录设备类型
     *
     * @param device    登录设备类型
     * @param loginUser 登录用户信息
     */
    public static void login(LoginUser loginUser, String device) {
        StpUtil.login(loginUser.getLoginCode(), device);
        setLoginUser(loginUser);
        loginUser.setToken(StpUtil.getTokenValue());
    }

    /**
     * 设置用户数据
     */
    public static void setLoginUser(LoginUser loginUser) {
        StpUtil.getTokenSession().set(Constant.LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取用户
     **/
    public static LoginUser getLoginUser() {
        return (LoginUser) StpUtil.getTokenSession().get(Constant.LOGIN_USER_KEY);
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNull(loginUser)) {
            return null;
        }
        return loginUser.getId();
    }

    /**
     * 获取用户账户
     **/
    public static String getLoginCode() {
        return getLoginUser().getLoginCode();
    }

    /**
     * 获取用户角色
     *
     * @return 用户角色
     */
    public static List<String> getRoleCode() {
        return getLoginUser().getRolePermission();
    }

}
