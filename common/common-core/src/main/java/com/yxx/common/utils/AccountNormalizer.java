package com.yxx.common.utils;

import java.util.Locale;

/**
 * 账号相关输入的统一规范化工具。
 *
 * <p>规范化必须发生在生成缓存 Key、唯一性查询和数据持久化之前，否则同一个邮箱或账号
 * 可能因为大小写或首尾空白产生不同的业务行为。第三方平台身份标识不应调用本工具，
 * 应保持外部平台返回的原始语义。</p>
 */
public final class AccountNormalizer {

    private AccountNormalizer() {
    }

    /**
     * 规范化密码登录账号。当前框架约定账号大小写不敏感。
     */
    public static String normalizeLoginCode(String loginCode) {
        return normalizeLowercase(loginCode);
    }

    /**
     * 规范化邮箱。邮箱域名和本框架账号匹配均按大小写不敏感处理。
     */
    public static String normalizeEmail(String email) {
        return normalizeLowercase(email);
    }

    /**
     * 规范化中国大陆手机号，仅移除用户误输入的首尾空白。
     */
    public static String normalizeMainlandPhone(String phone) {
        return trimToNull(phone);
    }

    /**
     * 规范化显示名称。显示名称保留大小写和内部空白，仅移除首尾空白。
     */
    public static String normalizeDisplayName(String displayName) {
        return trimToNull(displayName);
    }

    private static String trimToNull(String value) {
        // null 表示调用方没有提供值，保持 null 可以让后续校验或数据库条件正确区分“未提供”。
        if (value == null) {
            return null;
        }
        // 先清理首尾空白；清理后为空时统一转为 null，避免空串作为另一种“无值”进入系统。
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeLowercase(String value) {
        // 大小写转换必须基于 ROOT Locale，避免土耳其语等系统区域设置改变账号字符语义。
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
