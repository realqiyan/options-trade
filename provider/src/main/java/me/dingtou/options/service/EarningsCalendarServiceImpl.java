package me.dingtou.options.service;

import me.dingtou.options.manager.EarningsCalendarManager;
import me.dingtou.options.model.EarningsCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 财报日历服务实现
 */
@Service
public class EarningsCalendarServiceImpl implements EarningsCalendarService {
    
    @Autowired
    private EarningsCalendarManager earningsCalendarManager;
    
    /**
     * 同步未来30天的财报日历
     * 该方法由定时任务自动触发
     */
    @Override
    public void syncEarningsCalendar() {
        // 计算未来30天的日期
        Calendar calendar = Calendar.getInstance();
        
        for (int i = 0; i < 30; i++) {
            Date currentDate = calendar.getTime();
            // 同步指定日期的财报日历
            earningsCalendarManager.syncEarningsCalendarForDate(currentDate);
            // 日期加1天
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    
    /**
     * 根据股票代码获取近期的财报发布信息
     * 
     * @param symbol 股票代码
     * @return 财报日历列表
     */
    @Override
    public List<EarningsCalendar> getEarningsCalendarBySymbol(String symbol) {
        return earningsCalendarManager.getEarningsCalendarBySymbol(symbol);
    }
    
    /**
     * 根据日期获取财报信息
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    @Override
    public List<EarningsCalendar> getEarningsCalendarByDate(Date date) {
        return earningsCalendarManager.getEarningsCalendarByDate(date);
    }
}