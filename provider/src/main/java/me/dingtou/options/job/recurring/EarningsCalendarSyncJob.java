package me.dingtou.options.job.recurring;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.service.EarningsCalendarService;

/**
 * 同步财报日历
 */
@Slf4j
@Component
public class EarningsCalendarSyncJob implements Job {

    @Autowired
    private EarningsCalendarService earningsCalendarService;

    private boolean first = true;

    public EarningsCalendarSyncJob() {
    }

    @Override
    public UUID id() {
        return UUID.nameUUIDFromBytes(this.getClass().getName().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) {
        syncEarningsCalendar();
    }

    public void syncEarningsCalendar() {
        try {
            // 应用启动时不执行
            if (first) {
                first = false;
                return;
            }
            earningsCalendarService.syncEarningsCalendar();
        } catch (Exception e) {
            log.error("syncEarningsCalendar -> error", e);
        }
    }

    /**
     * 检查订阅信息任务参数
     */
    public static class EarningsCalendarSyncJobArgs implements JobArgs {
    }
}