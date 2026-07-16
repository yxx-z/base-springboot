package com.yxx.framework.security;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.yxx.common.constant.EmailSubject;
import com.yxx.common.constant.RedisKeyPrefix;
import com.yxx.common.properties.IpProperties;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.ip.AddressUtil;
import com.yxx.common.utils.ip.IpUtil;
import com.yxx.common.utils.redis.RedissonCache;
import com.yxx.security.constant.SecurityRealm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户端和管理端共用的异地登录风险提醒服务。
 *
 * <p>该服务只处理 IP 归属地解析、风险判断、提醒去重和邮件发送，不依赖具体用户实体；
 * 业务模块仍负责持久化各自的最近登录元数据。</p>
 */
@Service
@RequiredArgsConstructor
public class LoginRiskNotificationService {

    private final IpProperties ipProperties;
    private final RedissonCache redissonCache;
    private final MailUtils mailUtils;
    private final MailProperties mailProperties;
    private final MyWebProperties webProperties;
    private final AddressUtil addressUtil;

    /**
     * 解析当前登录地区并在满足风险条件时发送提醒。
     *
     * @return 当前 IP 归属地；关闭 IP 检查时返回 null
     */
    public String process(String realm,
                          Long subjectId,
                          String email,
                          String previousAgent,
                          String previousRegion,
                          String requestIp,
                          String currentAgent) {
        if (!Boolean.TRUE.equals(ipProperties.getCheck())) {
            // 关闭归属地能力时不做数据库查询和邮件判断，并以 null 表示无需更新该字段。
            return null;
        }
        // 省级归属地用于稳定比较，市级信息只在实际通知时按需解析。
        String currentRegion = addressUtil.getIpHomePlace(requestIp, 2);
        if (!IpUtil.isValidIPv4(requestIp) || CharSequenceUtil.isBlank(email)) {
            // IP 或邮箱不可用时仍返回已解析结果，但跳过无法可靠完成的风险通知。
            return currentRegion;
        }

        String notificationKey = notificationKey(realm, subjectId);
        if (redissonCache.exists(notificationKey)) {
            // 同一主体每天最多提醒一次，避免移动网络变化造成邮件轰炸。
            return currentRegion;
        }
        // 只有设备和地区同时变化才认为风险足够高，降低单一信号带来的误报。
        boolean deviceChanged = CharSequenceUtil.isNotBlank(previousAgent)
                && !previousAgent.equals(currentAgent);
        boolean regionChanged = CharSequenceUtil.isNotBlank(previousRegion)
                && !previousRegion.equals(currentRegion);
        if (!deviceChanged || !regionChanged) {
            return currentRegion;
        }

        String unusualAddress = addressUtil.getIpHomePlace(requestIp, 3);
        String time = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN);
        // 模板内容由配置维护，公共服务只替换约定占位符。
        String content = mailProperties.getIpUnusualContent()
                .replace("{time}", time)
                .replace("{ip}", requestIp)
                .replace("{address}", unusualAddress)
                .replace("{agent}", currentAgent)
                .replace("{domain}", webProperties.getDomain())
                .replace("{formName}", mailProperties.getFromName())
                .replace("{form}", mailProperties.getFrom());
        mailUtils.baseSendMail(email, EmailSubject.UNUSUAL_LOGIN, content, true);
        // 邮件成功后才写去重标记，发送失败时保留下一次重试机会。
        redissonCache.put(notificationKey, Boolean.TRUE, DateUtils.secondsUntilNextDay());
        return currentRegion;
    }

    private String notificationKey(String realm, Long subjectId) {
        // 用户端和管理端使用独立 Key 空间，避免相同数据库 ID 相互抑制通知。
        if (SecurityRealm.USER.equals(realm)) {
            return RedisKeyPrefix.USER_LOGIN_RISK + subjectId;
        }
        if (SecurityRealm.ADMIN.equals(realm)) {
            return RedisKeyPrefix.ADMIN_LOGIN_RISK + subjectId;
        }
        throw new IllegalArgumentException("不支持的登录安全域：" + realm);
    }
}
