package me.dingtou.options.service;

/**
 * 邮件服务接口
 * 
 * @author system
 */
public interface EmailService {

    /**
     * 使用自定义SMTP配置发送Markdown格式邮件
     *
     * @param to           收件人
     * @param subject      邮件主题
     * @param content      Markdown格式的内容
     * @param smtpHost     SMTP服务器地址
     * @param smtpPort     SMTP服务器端口
     * @param smtpUser     SMTP用户名
     * @param smtpPassword SMTP密码
     */
    void sendMarkdown(String to,
            String subject,
            String content,
            String smtpHost,
            String smtpPort,
            String smtpUser,
            String smtpPassword);
}