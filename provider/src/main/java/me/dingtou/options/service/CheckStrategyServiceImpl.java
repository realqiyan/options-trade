package me.dingtou.options.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.service.copilot.CopilotService;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.Message;
import me.dingtou.options.model.Owner;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerStrategy;
import me.dingtou.options.model.StrategyExt;

@Slf4j
@Service
public class CheckStrategyServiceImpl implements CheckStrategyService {

    /**
     * 提醒内容
     */
    private static final String REMIND = """

            ---
            * 不要预测，触发点位即执行。
            * 不要侥幸，不赌运气。
            * 要看长线，避免因小失大。
            ---

            """;

    @Autowired
    private OwnerManager ownerManager;

    // 自动注入agent模式的agentCopilotServiceV2
    @Autowired
    @Qualifier("agentCopilotServiceV2")
    private CopilotService copilotService;

    // 自动注入邮件服务
    @Autowired
    private EmailService emailService;

    @Override
    public void checkALlOwnerStrategy() {
        log.info("开始检查所有账户的策略");

        try {
            // 第一步获取所有账户信息
            List<Owner> owners = ownerManager.queryAllOwner();
            if (owners == null || owners.isEmpty()) {
                log.warn("没有找到任何账户信息");
                return;
            }

            log.info("找到 {} 个账户，开始检查策略", owners.size());

            // 第二步获取账户所有策略并使用AI服务检查
            owners.forEach(owner -> {
                check(owner);
            });

        } catch (Exception e) {
            log.error("检查所有策略时发生错误", e);
        }

    }

    @Override
    public void checkOwnerStrategy(String owner) {
        Owner ownerInfo = ownerManager.queryOwner(owner);
        if (ownerInfo == null) {
            log.warn("账户 {} 不存在", owner);
            return;
        }
        check(ownerInfo);
    }

    /**
     * 检查账户的策略
     * 
     * @param owner 账户信息
     */
    private void check(Owner owner) {
        String ownerId = owner.getOwner();
        log.info("开始检查账户 {} 的策略", ownerId);

        try {
            List<OwnerStrategy> strategies = owner.getStrategyList();
            if (strategies == null || strategies.isEmpty()) {
                log.info("账户 {} 没有任何策略", ownerId);
                return;
            }

            log.info("账户 {} 有 {} 个策略", ownerId, strategies.size());

            // 第三步检查策略（使用AI服务检查）
            strategies.forEach(strategy -> {
                try {
                    checkStrategyWithAI(owner, strategy);
                } catch (Exception e) {
                    log.error("检查账户 {} 的策略 {} 时发生错误", ownerId, strategy.getStrategyId(), e);
                }
            });

        } catch (Exception e) {
            log.error("处理账户 {} 时发生错误", ownerId, e);
        }
    }

    /**
     * 使用AI服务检查单个策略
     * 
     * @param owner    账户ID
     * @param strategy 策略
     */
    private void checkStrategyWithAI(Owner owner, OwnerStrategy strategy) {
        if (!isEmailNotifyEnabled(owner)) {
            log.info("账户 {} 的策略 {} 没有开启邮件通知，跳过检查", owner.getOwner(), strategy.getStrategyId());
            return;
        }

        boolean needEvaluate = Boolean.valueOf(strategy.getExtValue(StrategyExt.NEED_EVALUATE, "false"));
        // 检查是否需要评估策略
        if (!needEvaluate) {
            log.info("账户 {} 的策略 {} 没有开启评估，跳过检查", owner.getOwner(), strategy.getStrategyId());
            return;
        }

        if ("default".equals(strategy.getStrategyCode())) {
            log.info("账户 {} 的策略 {} 是默认策略，跳过检查", owner.getOwner(), strategy.getStrategyId());
            return;
        }
        log.info("使用AI检查账户 {} 的策略 {}", owner.getOwner(), strategy.getStrategyId());
        try {
            String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String title = strategy.getStrategyName() + "-策略检查-" + datetime;

            // 构建策略检查提示词
            String prompt = buildStrategyCheckPrompt(strategy);

            // 创建消息对象
            Message message = new Message("user", prompt);

            // 定义回调函数
            Function<Message, Void> callback = response -> {
                return null;
            };

            // 定义最终回调函数
            Function<Message, Void> finalCallback = response -> {
                log.info("AI检查策略 {} 的响应: {}", strategy.getStrategyId(), response.getContent());
                sendEmailNotification(owner, title, response);
                return null;
            };

            // 定义失败回调函数
            Function<Message, Void> failCallback = error -> {
                log.error("AI检查策略 {} 失败: {}", strategy.getStrategyId(), error.getContent());
                sendEmailNotification(owner, title, error);
                return null;
            };

            // 生成会话ID
            String sessionId = owner.getOwner() + "-" + System.currentTimeMillis();

            // 调用AI服务
            copilotService.start(
                    owner.getOwner(),
                    sessionId,
                    title,
                    message,
                    callback,
                    failCallback,
                    finalCallback);

        } catch (Exception e) {
            log.error("使用AI检查策略 {} 时发生异常", strategy.getStrategyId(), e);
        }

    }

    private boolean isEmailNotifyEnabled(Owner owner) {
        OwnerAccount account = owner.getAccount();
        String smtpHost = AccountExtUtils.getSmtpHost(account);
        if (smtpHost == null) {
            log.warn("账户 {} 没有配置SMTP主机", owner.getOwner());
            return false;
        }
        String smtpPort = AccountExtUtils.getSmtpPort(account);
        if (smtpPort == null) {
            log.warn("账户 {} 没有配置SMTP端口", owner.getOwner());
            return false;
        }
        String smtpUser = AccountExtUtils.getSmtpUsername(account);
        if (smtpUser == null) {
            log.warn("账户 {} 没有配置SMTP用户", owner.getOwner());
            return false;
        }
        String smtpPassword = AccountExtUtils.getSmtpPassword(account);
        if (smtpPassword == null) {
            log.warn("账户 {} 没有配置SMTP密码", owner.getOwner());
            return false;
        }
        String emailTo = AccountExtUtils.getEmailNotifyReceiver(account);
        if (emailTo == null) {
            log.warn("账户 {} 没有配置邮件接收人", owner.getOwner());
            return false;
        }
        return true;
    }

    private void sendEmailNotification(Owner owner, String title, Message message) {
        OwnerAccount account = owner.getAccount();
        String smtpHost = AccountExtUtils.getSmtpHost(account);
        String smtpPort = AccountExtUtils.getSmtpPort(account);
        String smtpUser = AccountExtUtils.getSmtpUsername(account);
        String smtpPassword = AccountExtUtils.getSmtpPassword(account);
        String emailTo = AccountExtUtils.getEmailNotifyReceiver(account);
        String content = message.getContent();

        // 使用自定义SMTP配置发送Markdown格式邮件

        content = REMIND + content;
        emailService.sendMarkdown(emailTo, title, content, smtpHost, smtpPort, smtpUser, smtpPassword);

        log.info("已发送邮件通知，收件人：{}，主题：{}", emailTo, title);
    }

    /**
     * 构建策略检查提示词
     * 
     * @param strategy 策略
     * @return 提示词
     */
    private String buildStrategyCheckPrompt(OwnerStrategy strategy) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请帮我对策略：").append(strategy.getStrategyName()).append(" 进行综合分析。\n")
                .append("策略ID：").append(strategy.getStrategyId())
                .append("，期权策略Code：").append(strategy.getStrategyCode())
                .append("，请按照期权策略规则、期权策略详情和订单，以及其他你评估需要的信息，告诉我策略持仓是否需要调整。");
        return prompt.toString();
    }

}
