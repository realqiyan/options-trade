package me.dingtou.options.service;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务实现类
 * 
 * @author system
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    @Override
    public void sendMarkdown(String to,
            String subject,
            String content,
            String smtpHost,
            String smtpPort,
            String smtpUser,
            String smtpPassword) {
        try {
            // 创建自定义的JavaMailSender
            JavaMailSenderImpl customMailSender = new JavaMailSenderImpl();
            customMailSender.setHost(smtpHost);
            customMailSender.setPort(Integer.parseInt(smtpPort));
            customMailSender.setUsername(smtpUser);
            customMailSender.setPassword(smtpPassword);
            customMailSender.setProtocol("smtp");

            // 设置SMTP属性
            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.socketFactory.port", smtpPort);
            customMailSender.setJavaMailProperties(props);

            // 将Markdown转换为HTML
            Node document = markdownParser.parse(content);
            String htmlContent = htmlRenderer.render(document);

            // 添加基本的HTML样式
            htmlContent = wrapWithHtmlStyle(subject, htmlContent);

            // 发送邮件
            MimeMessage message = customMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtpUser);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            customMailSender.send(message);

            log.info("使用自定义SMTP发送Markdown邮件成功，收件人：{}，主题：{}", to, subject);
        } catch (Exception e) {
            log.error("使用自定义SMTP发送Markdown邮件失败，收件人：{}，主题：{}", to, subject, e);
        }
    }

    /**
     * 为HTML内容添加基本样式
     * 
     * @param title       邮件标题
     * @param htmlContent HTML内容
     * @return 带样式的HTML内容
     */
    private String wrapWithHtmlStyle(String title, String htmlContent) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        h1, h2, h3, h4, h5, h6 {\n" +
                "            color: #2c3e50;\n" +
                "            margin-top: 20px;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        p {\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        code {\n" +
                "            background-color: #f4f4f4;\n" +
                "            padding: 2px 4px;\n" +
                "            border-radius: 3px;\n" +
                "            font-family: 'Courier New', Courier, monospace;\n" +
                "        }\n" +
                "        pre {\n" +
                "            background-color: #f4f4f4;\n" +
                "            padding: 10px;\n" +
                "            border-radius: 5px;\n" +
                "            overflow-x: auto;\n" +
                "        }\n" +
                "        blockquote {\n" +
                "            border-left: 4px solid #ddd;\n" +
                "            margin: 0;\n" +
                "            padding-left: 20px;\n" +
                "            color: #666;\n" +
                "        }\n" +
                "        table {\n" +
                "            border-collapse: collapse;\n" +
                "            width: 100%;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        th, td {\n" +
                "            border: 1px solid #ddd;\n" +
                "            padding: 8px;\n" +
                "            text-align: left;\n" +
                "        }\n" +
                "        th {\n" +
                "            background-color: #f2f2f2;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                htmlContent +
                "</body>\n" +
                "</html>";
    }
}