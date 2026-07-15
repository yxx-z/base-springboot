package com.yxx.common.utils.ip;

import com.yxx.common.properties.IpProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 IP 解析器。
 *
 * <p>只有请求的直接来源位于 {@code ip.trusted-proxies} 白名单时，才读取代理转发头。
 * 这样可以防止客户端自行构造 {@code X-Forwarded-For} 伪造审计与风控 IP。</p>
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";
    private static final String IPV4_LOCALHOST = "127.0.0.1";
    private static final String IPV6_LOCALHOST = "0:0:0:0:0:0:0:1";
    private static final String IPV6_LOCALHOST_SHORT = "::1";

    private final IpProperties ipProperties;

    /**
     * 解析真实客户端 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP；无法识别时返回直接连接地址
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!ipProperties.getTrustedProxies().contains(remoteAddress)) {
            return remoteAddress;
        }

        String forwarded = resolveForwardedChain(request.getHeader("X-Forwarded-For"));
        if (isUsable(forwarded)) {
            return forwarded;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        return isUsable(realIp) ? realIp : remoteAddress;
    }

    private String resolveForwardedChain(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        List<String> chain = new ArrayList<>();
        for (String item : header.split(",")) {
            String address = normalize(item);
            if (isUsable(address)) {
                chain.add(address);
            }
        }
        // 从最靠近应用的代理向外回溯，跳过明确受信任的代理，首个非可信节点才是客户端。
        for (int index = chain.size() - 1; index >= 0; index--) {
            String address = chain.get(index);
            if (!ipProperties.getTrustedProxies().contains(address)) {
                return address;
            }
        }
        return chain.isEmpty() ? null : chain.get(0);
    }

    private boolean isUsable(String address) {
        return address != null
                && !address.isBlank()
                && !UNKNOWN.equalsIgnoreCase(address)
                && (IpUtil.isValidIPv4(address) || IpUtil.isIpv6Literal(address));
    }

    private String normalize(String address) {
        if (address == null) {
            return null;
        }
        String value = address.trim();
        if (IPV6_LOCALHOST.equals(value) || IPV6_LOCALHOST_SHORT.equals(value)) {
            return IPV4_LOCALHOST;
        }
        return value;
    }
}
