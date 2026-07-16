package com.yxx.framework.security;

import cn.dev33.satoken.temp.SaTempUtil;
import com.yxx.common.constant.EmailSubject;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.properties.ResetPwdProperties;
import com.yxx.common.utils.ApiAssert;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.redis.RedissonCache;
import com.yxx.security.model.PasswordResetTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 跨安全域复用的密码重置邮件协调服务。
 *
 * <p>用户端和管理端只负责确认邮箱对应的主体，本服务统一处理发送窗口、每日频次、临时
 * Token、邮件模板和失败补偿。不同安全域必须传入独立 Redis Key 前缀。</p>
 */
@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    private final RedissonCache redissonCache;
    private final MailUtils mailUtils;
    private final MailProperties mailProperties;
    private final ResetPwdProperties resetPwdProperties;
    private final MyWebProperties webProperties;

    /**
     * 发送一次密码重置邮件。
     *
     * @param realm 安全域
     * @param subjectId 主体内部主键
     * @param email 已规范化邮箱
     * @param sendingKeyPrefix 发送窗口 Key 前缀
     * @param countKeyPrefix 每日计数 Key 前缀
     */
    public void send(String realm,
                     Long subjectId,
                     String email,
                     String sendingKeyPrefix,
                     String countKeyPrefix) {
        long tokenSeconds = TimeUnit.MINUTES.toSeconds(resetPwdProperties.getResetPwdTime());
        String sendingKey = sendingKeyPrefix + email;
        ApiAssert.isTrue(ApiCode.MAIL_EXIST,
                redissonCache.putStringIfAbsent(sendingKey, "sending", tokenSeconds));

        String countKey = countKeyPrefix + email;
        long count = redissonCache.increment(countKey, DateUtils.secondsUntilNextDay());
        if (count > resetPwdProperties.getMaxNumber()) {
            redissonCache.decrement(countKey);
            redissonCache.remove(sendingKey);
            throw new ApiException(ApiCode.RESET_PWD_MAX);
        }

        try {
            PasswordResetTokenPayload payload = new PasswordResetTokenPayload(realm, subjectId, email);
            // 临时 Token 仅保存稳定的版本化字符串，避免复杂对象受 Redis 序列化器实现影响。
            String token = SaTempUtil.createToken(payload.encode(), tokenSeconds);
            String resetPassHref = resetPwdProperties.getBasePath() + "?token=" + token;
            String content = resetPwdProperties.getResetPwdContent()
                    .replace("{url}", resetPassHref)
                    .replace("{time}", String.valueOf(resetPwdProperties.getResetPwdTime()))
                    .replace("{domain}", webProperties.getDomain())
                    .replace("{formName}", mailProperties.getFromName())
                    .replace("{form}", mailProperties.getFrom());
            mailUtils.baseSendMail(email, EmailSubject.RESET_PASSWORD, content, true);
            redissonCache.putString(sendingKey, token, tokenSeconds);
        } catch (RuntimeException exception) {
            // 邮件发送或 Token 创建失败时归还次数并释放发送窗口，允许用户再次尝试。
            redissonCache.decrement(countKey);
            redissonCache.remove(sendingKey);
            throw exception;
        }
    }
}
