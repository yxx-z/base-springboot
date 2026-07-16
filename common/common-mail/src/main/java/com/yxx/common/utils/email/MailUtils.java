package com.yxx.common.utils.email;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailUtils {

    /**
     * 该 Bean 由 Spring Boot Mail 在配置 {@code spring.mail.host} 后按条件自动创建。
     * 公共子模块自身没有最终应用配置，IDE 无法静态推断条件自动配置，因此仅抑制该误报；
     * 不在此处手工创建 JavaMailSender，避免覆盖 Spring Boot 的连接、认证和超时配置。
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final JavaMailSender mailSender;

    private final MailProperties mailProperties;


    /**
     * 发送邮件 单个接收人
     *
     * @param to      接收人邮箱
     * @param subject 邮件主题
     * @param text    邮件正文
     * @param html    true:发送html格式邮件；false:发送普通邮件
     * @author yxx
     */
    public void baseSendMail(String to, String subject, String text, boolean html) {
        // 每次发送创建独立 MimeMessage，JavaMailSender 负责底层连接与传输生命周期。
        MimeMessage message = mailSender.createMimeMessage();
        try {
            // UTF-8 保证中文发件人名称、主题和正文跨邮件客户端正确显示。
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            if (html) {
                // HTML 标志必须显式传入，避免模板正文被邮件客户端当作纯文本展示。
                helper.setText(text, true);
            } else {
                helper.setText(text);
            }
            mailSender.send(message);
            log.info("邮件已经发送");
        } catch (MessagingException | UnsupportedEncodingException e) {
            // 屏蔽邮件实现异常类型，向业务层提供稳定的统一错误码。
            log.error("发送邮件时发生异常！", e);
            throw new ApiException(ApiCode.MAIL_ERROR);
        }
    }

    /**
     * 发送邮件，多个接收人
     *
     * @param to      接收人邮箱
     * @param subject 邮件主题
     * @param text    邮件正文
     * @param html    true:发送html格式邮件；false:发送普通邮件
     * @author yxx
     */
    public void baseSendMail(String[] to, String subject, String text, boolean html) {
        // 多收件人与单收件人保持完全相同的编码、发件人和异常策略。
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            if (html) {
                helper.setText(text, true);
            } else {
                helper.setText(text);
            }
            mailSender.send(message);
            log.info("邮件已经发送");
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("发送邮件时发生异常！", e);
            throw new ApiException(ApiCode.MAIL_ERROR);
        }
    }

}
