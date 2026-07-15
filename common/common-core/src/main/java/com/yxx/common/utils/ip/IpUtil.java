package com.yxx.common.utils.ip;

import com.yxx.common.constant.SeparatorConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * IP工具类
 *
 * @author yxx
 * @since 2022/07/19
 */
@Slf4j
public class IpUtil {

    private static final Integer MAX_LENGTH = 15;
    private static final String UNKNOWN = "unknown";
    private static final String IPV6_LOCAL = "0:0:0:0:0:0:0:1";

    private IpUtil() {
        throw new AssertionError();
    }

    /**
     * 获取请求用户的IP地址
     *
     * @return IP地址
     */
    public static String getRequestIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
        return getRequestIp(request);
    }

    /**
     * 获取请求用户的IP地址
     *
     * @param request HttpServletRequest
     * @return IP地址
     */
    public static String getRequestIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (IPV6_LOCAL.equals(ip)) {
            ip = getLocalhostIp();
        }

        if (ip != null && ip.length() > MAX_LENGTH) {
            if (ip.indexOf(SeparatorConstant.COMMA) > 0) {
                ip = ip.substring(0, ip.indexOf(SeparatorConstant.COMMA));
            }
        }
        return ip;
    }

    public static String getLocalhostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }
        return null;
    }
}
