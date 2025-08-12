package me.dingtou.options.job;

import java.time.Duration;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.recurring.EarningsCalendarSyncJob;
import me.dingtou.options.job.recurring.EarningsCalendarSyncJob.EarningsCalendarSyncJobArgs;

@Slf4j
@Component
public class JobInitialize implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 初始化定时任务
        initRecurringJobs();

        // 初始化一次性任务
        initOneTimeJobs();
    }

    private void initRecurringJobs() {

        // 财报日历拉取服务
        {
            JobContext<EarningsCalendarSyncJobArgs> ctx = JobContext.of(new EarningsCalendarSyncJobArgs());
            JobClient.submit(new EarningsCalendarSyncJob(), ctx, Duration.ofHours(24));
        }

        // 检查富途OpenAPI订阅信息
        {
            // 使用简单任务代替JobClient
            // JobContext<CheckFutuSubJobArgs> ctx = JobContext.of(new
            // CheckFutuSubJobArgs());
            // JobClient.submit(new CheckFutuSubJob(), ctx, Duration.ofMinutes(15));
        }

    }

    private void initOneTimeJobs() {
        // 启动服务
        {
            // 移除空任务
            // Instant instant = Instant.now().plus(3, ChronoUnit.SECONDS);
            // JobClient.submit(new StartJob(), JobContext.of(new StartJobArgs()), instant);
        }
    }

}
