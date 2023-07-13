package com.yxx.business.service.impl;

import com.yxx.business.service.MailService;
import com.yxx.common.enums.ApiCode;
import com.yxx.common.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

/**
 * 邮件服务impl
 *
 * @author yxx
 * @classname MailServiceImpl
 * @since 2023/07/05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String mailFrom;

    @Value("${mail.from-name}")
    private String mailFromName;

    @Override
    public void baseSendMail(String to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailFrom, mailFromName);
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

    @Override
    public void baseSendMail(String[] to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailFrom, mailFromName);
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
