package me.dingtou.options.task;

import me.dingtou.options.service.EarningsCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 财报日历同步任务
 */
@Component
public class EarningsCalendarSyncTask {
    
    @Autowired
    private EarningsCalendarService earningsCalendarService;
    
    /**
     * 每天凌晨2点同步未来30天的财报日历
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncEarningsCalendar() {
        try {
            earningsCalendarService.syncEarningsCalendar();
        } catch (Exception e) {
            // 记录错误日志
            e.printStackTrace();
        }
    }
}