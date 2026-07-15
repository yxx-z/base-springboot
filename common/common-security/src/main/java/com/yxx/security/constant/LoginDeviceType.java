package com.yxx.security.constant;

/**
 * Sa-Token 登录设备类型。
 *
 * <p>设备类型属于认证会话语义，因此归属安全模块，而不是无差别放入 common-core。</p>
 */
public final class LoginDeviceType {

    /** PC 或普通 Web 浏览器。 */
    public static final String PC = "pc";

    /** 支付宝小程序。 */
    public static final String ALIPAY_APPLET = "alipay-applet";

    private LoginDeviceType() {
    }
}
