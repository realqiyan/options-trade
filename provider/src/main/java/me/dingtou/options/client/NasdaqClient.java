package me.dingtou.options.client;

import me.dingtou.options.model.EarningsCalendar;

import java.util.Date;
import java.util.List;

/**
 * NASDAQ API客户端
 */
public interface NasdaqClient {
    
    /**
     * 根据日期获取财报日历数据
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    List<EarningsCalendar> getEarningsCalendarByDate(Date date);
}