package com.yxx.business.service;


/**
 * 邮件服务
 *
 * @author yxx
 * @classname MailService
 * @since 2023/07/05
 */
public interface MailService {

    /**
     * 发送邮件公共方法
     *
     * @param to      接收方 单个
     * @param subject 主题
     * @param text    内容
     * @author yxx
     */
    void baseSendMail(String to, String subject, String text);

    /**
     * 发送邮件公共方法
     *
     * @param to      接收方 数组
     * @param subject 主题
     * @param text    内容
     * @author yxx
     */
    void baseSendMail(String[] to, String subject, String text);

}
