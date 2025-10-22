package me.dingtou.options.job.recurring;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.service.CheckStrategyService;

/**
 * 检查持仓策略
 */
@Slf4j
@Component
public class CheckOwnerStrategyJob implements Job {

    @Autowired
    private CheckStrategyService checkStrategyService;

    public CheckOwnerStrategyJob() {
    }

    @Override
    public UUID id() {
        return UUID.nameUUIDFromBytes(this.getClass().getName().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) {
        // 任务过于频繁 直接使用Spring的@Scheduled
        // checkOwnerStrategy();
    }

    /**
     * 周一到周五 每天22:30和09:30执行一次检查持仓策略
     */
    @Scheduled(cron = "0 30 22,09 ? * MON-FRI")
    public void checkOwnerStrategy() {
        try {
            // 检查持仓策略
            checkStrategyService.checkALlOwnerStrategy();
            log.info("checkOwnerStrategy -> success");
        } catch (Exception e) {
            log.error("checkOwnerStrategy -> error", e);
        }
    }

    /**
     * 检查持仓策略任务参数
     */
    public static class CheckOwnerStrategyJobArgs implements JobArgs {
    }
}