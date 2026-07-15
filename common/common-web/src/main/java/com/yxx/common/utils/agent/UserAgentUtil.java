package com.yxx.common.utils.agent;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.useragent.UserAgent;

/**
 * agen工具类
 *
 * @author yxx
 * @classname UserAgentUtil
 * @since 2023-08-01 18:27
 */
public class UserAgentUtil {
    /**
     * 获得代理
     *
     * @param userAgent 用户代理
     * @return {@link String }
     * @author yxx
     */
    public static String getAgent(String userAgent) {
        if (CharSequenceUtil.isBlank(userAgent)) {
            return "未知";
        }
        UserAgent ua = cn.hutool.http.useragent.UserAgentUtil.parse(userAgent);
        String platform = String.valueOf(ua.getPlatform());
        String os = String.valueOf(ua.getOs());
        String browser = String.valueOf(ua.getBrowser());
        String version = ua.getVersion();
        if (CharSequenceUtil.isBlank(platform) || "Unknown".equals(platform) || "null".equals(platform)) {
            return userAgent;
        } else {
            return platform + "-" + os + "-" + browser + "-" + version;
        }
    }
}
