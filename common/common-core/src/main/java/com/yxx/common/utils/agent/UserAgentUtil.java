package com.yxx.common.utils.agent;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.useragent.UserAgent;
import com.yxx.common.utils.jackson.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * agen工具类
 *
 * @author yxx
 * @classname UserAgentUtil
 * @since 2023-08-01 18:27
 */
@Slf4j
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
        String json = JacksonUtil.toJson(ua);
        log.info("ua json = " + json);
        String platform = ua.getPlatform().toString();//Windows
        log.info("系统:{} ", platform);
        String os = ua.getOs().toString();//Windows 7
        log.info("系统版本:{} ", os);
        String browser = ua.getBrowser().toString();//Chrome
        log.info("浏览器:{} ", browser);
        String version = ua.getVersion();//14.0.835.163
        log.info("版本:{} ", version);
        String engine = ua.getEngine().toString();//Webkit
        log.info("浏览器引擎:{} ", engine);
        String engineVersion = ua.getEngineVersion();//535.1
        log.info("浏览器引擎版本:{} ", engineVersion);
        boolean mobile = ua.isMobile();
        log.info("是否是移动端:{} ", mobile);
        if (CharSequenceUtil.isBlank(platform) || "Unknown".equals(platform) || "null".equals(platform)) {
            return userAgent;
        } else {
            return platform + "-" + os + "-" + browser + "-" + version;
        }
    }
}
