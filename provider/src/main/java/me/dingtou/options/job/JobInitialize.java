package me.dingtou.options.job;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.background.StartJob;
import me.dingtou.options.job.background.StartJob.StartJobArgs;
import me.dingtou.options.job.recurring.CheckFutuSubJob;
import me.dingtou.options.job.recurring.CheckFutuSubJob.CheckFutuSubJobArgs;

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
        // 检查富途OpenAPI订阅信息
        JobContext<CheckFutuSubJobArgs> ctx = JobContext.of(new CheckFutuSubJobArgs(System.currentTimeMillis()));
        ctx.addArg("interval", "15");
        JobClient.submit(new CheckFutuSubJob(), ctx, Duration.ofMinutes(15));

    }

    private void initOneTimeJobs() {
        JobClient.submit(new StartJob(), JobContext.of(new StartJobArgs()), Instant.now().plus(3, ChronoUnit.SECONDS));
    }

}
