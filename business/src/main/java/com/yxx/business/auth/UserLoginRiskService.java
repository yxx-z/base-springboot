package com.yxx.business.auth;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.yxx.business.model.entity.User;
import com.yxx.business.service.UserService;
import com.yxx.common.constant.EmailSubjectConstant;
import com.yxx.common.constant.RedisConstant;
import com.yxx.common.properties.IpProperties;
import com.yxx.common.properties.MailProperties;
import com.yxx.common.properties.MyWebProperties;
import com.yxx.common.utils.DateUtils;
import com.yxx.common.utils.ServletUtils;
import com.yxx.common.utils.agent.UserAgentUtil;
import com.yxx.common.utils.email.MailUtils;
import com.yxx.common.utils.ip.AddressUtil;
import com.yxx.common.utils.ip.IpUtil;
import com.yxx.common.utils.redis.RedissonCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 用户登录风险信息处理服务。
 *
 * <p>认证策略只验证身份，本服务独立处理设备、IP 归属地和异地登录提醒，避免安全通知逻辑
 * 重新耦合进密码或第三方登录策略。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginRiskService {

    private final UserService userService;
    private final IpProperties ipProperties;
    private final RedissonCache redissonCache;
    private final MailUtils mailUtils;
    private final MailProperties mailProperties;
    private final MyWebProperties webProperties;

    @Qualifier("applicationTaskExecutor")
    private final Executor applicationTaskExecutor;

    /**
     * 记录登录设备和地址，并异步检查异常登录。
     *
     * @param user 已完成身份认证的系统用户
     */
    public void handleSuccessfulLogin(User user) {
        String rawAgent = ServletUtils.getRequest().getHeader("user-agent");
        String agent = UserAgentUtil.getAgent(rawAgent);
        if (!Boolean.TRUE.equals(ipProperties.getCheck())) {
            user.setAgent(agent);
            userService.updateById(user);
            return;
        }

        String requestIp = IpUtil.getRequestIp();
        String ipRegion = AddressUtil.getIpHomePlace(requestIp, 2);
        User previousSnapshot = new User();
        BeanUtils.copyProperties(user, previousSnapshot);

        CompletableFuture.runAsync(
                () -> checkRemoteLogin(previousSnapshot, ipRegion, requestIp, agent),
                applicationTaskExecutor);
        user.setAgent(agent);
        user.setIpHomePlace(ipRegion);
        userService.updateById(user);
    }

    private void checkRemoteLogin(User user, String currentRegion, String requestIp, String currentAgent) {
        if (!AddressUtil.isValidIPv4(requestIp)) {
            return;
        }
        if (redissonCache.exists(RedisConstant.IP_UNUSUAL_LOGIN + user.getId())) {
            return;
        }
        boolean deviceChanged = CharSequenceUtil.isNotBlank(user.getAgent())
                && !user.getAgent().equals(currentAgent);
        boolean regionChanged = CharSequenceUtil.isNotBlank(user.getIpHomePlace())
                && !user.getIpHomePlace().equals(currentRegion);
        if (!deviceChanged || !regionChanged || CharSequenceUtil.isBlank(user.getEmail())) {
            return;
        }

        String unusualAddress = AddressUtil.getIpHomePlace(requestIp, 3);
        String time = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN);
        String content = mailProperties.getIpUnusualContent()
                .replace("{time}", time)
                .replace("{ip}", requestIp)
                .replace("{address}", unusualAddress)
                .replace("{agent}", currentAgent)
                .replace("{domain}", webProperties.getDomain())
                .replace("{formName}", mailProperties.getFromName())
                .replace("{form}", mailProperties.getFrom());
        mailUtils.baseSendMail(user.getEmail(), EmailSubjectConstant.IP_UNUSUAL, content, true);
        redissonCache.put(
                RedisConstant.IP_UNUSUAL_LOGIN + user.getId(), Boolean.TRUE,
                DateUtils.theRestOfTheDaySecond());
    }
}
