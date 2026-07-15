package com.yxx.common.utils.ip;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** IP 地址格式校验工具。 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IpUtil {

    /**
     * 校验 IPv4 文本，整个过程只处理字符串，不触发 DNS 查询。
     *
     * @param ip 待校验文本
     * @return 是否为合法 IPv4 地址
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int index = 0; index < part.length(); index++) {
                if (!Character.isDigit(part.charAt(index))) {
                    return false;
                }
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断文本是否为 IPv6 字面量，不对客户端输入执行主机名解析。
     *
     * @param ip 待校验文本
     * @return 是否可能为 IPv6 字面量
     */
    public static boolean isIpv6Literal(String ip) {
        return ip != null && ip.indexOf(':') >= 0 && ip.matches("^[0-9A-Fa-f:.%]+$");
    }
}
