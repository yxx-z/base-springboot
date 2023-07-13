package com.yxx.common.utils.email;

import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import com.yxx.common.properties.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailUtils {

    private final JavaMailSender mailSender;

    private final MailProperties mailProperties;


    /**
     * 发送邮件 单个接收人
     *
     * @param to      接收人邮箱
     * @param subject 邮件主题
     * @param text    邮件正文
     * @author yxx
     */
    public void baseSendMail(String to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
            log.info("邮件已经发送");
        } catch (MessagingException | UnsupportedEncodingException e) {
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
     * @author yxx
     */
    public void baseSendMail(String[] to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
            log.info("邮件已经发送");
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("发送邮件时发生异常！", e);
            throw new ApiException(ApiCode.MAIL_ERROR);
        }
    }

}