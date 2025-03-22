package me.dingtou.options.job;

import java.time.Duration;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.recurring.CheckFutuSubJob;

@Slf4j
@Component
public class JobInitialize implements InitializingBean {

    @Autowired
    private JobScheduler jobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 检查富途OpenAPI订阅信息
        CheckFutuSubJob checkFutuSubJob = new CheckFutuSubJob();
        jobScheduler.scheduleRecurrently(checkFutuSubJob.id(), Duration.ofSeconds(60), () -> {
            checkFutuSubJob.run();
        });
    }
}
