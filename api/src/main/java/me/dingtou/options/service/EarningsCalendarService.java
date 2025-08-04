package me.dingtou.options.service;

import me.dingtou.options.model.EarningsCalendar;

import java.util.Date;
import java.util.List;

/**
 * 财报日历服务
 */
public interface EarningsCalendarService {
    
    /**
     * 同步未来30天的财报日历
     * 该方法由定时任务自动触发
     */
    void syncEarningsCalendar();
    
    /**
     * 根据股票代码获取近期的财报发布信息
     * 
     * @param symbol 股票代码
     * @return 财报日历列表
     */
    List<EarningsCalendar> getEarningsCalendarBySymbol(String symbol);
    
    /**
     * 根据日期获取财报信息
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    List<EarningsCalendar> getEarningsCalendarByDate(Date date);
}