package me.dingtou.options.gateway;

import me.dingtou.options.model.EarningsCalendar;

import java.util.Date;
import java.util.List;

/**
 * 财报日历网关接口
 */
public interface EarningsCalendarGateway {
    
    /**
     * 根据日期获取财报日历数据
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    List<EarningsCalendar> getEarningsCalendarByDate(Date date);
}