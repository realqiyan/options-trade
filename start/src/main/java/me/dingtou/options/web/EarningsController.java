package me.dingtou.options.web;

import me.dingtou.options.model.EarningsCalendar;
import me.dingtou.options.service.EarningsCalendarService;
import me.dingtou.options.web.model.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 财报日历控制器
 */
@RestController
@RequestMapping("/api/earnings-calendar")
public class EarningsController {

    @Autowired
    private EarningsCalendarService earningsCalendarService;

    /**
     * 根据股票代码获取近期的财报发布信息
     * 
     * @param symbol 股票代码
     * @return 财报日历列表
     */
    @GetMapping("/symbol")
    public WebResult<List<EarningsCalendar>> getEarningsCalendarBySymbol(@RequestParam("symbol") String symbol) {
        List<EarningsCalendar> earningsCalendars = earningsCalendarService.getEarningsCalendarBySymbol(symbol);
        return WebResult.success(earningsCalendars);
    }

    /**
     * 根据日期获取财报信息
     * 
     * @param date 指定日期(格式: yyyy-MM-dd)
     * @return 财报日历列表
     */
    @GetMapping("/date")
    public WebResult<List<EarningsCalendar>> getEarningsCalendarByDate(@RequestParam("date") Date date) {
        List<EarningsCalendar> earningsCalendars = earningsCalendarService.getEarningsCalendarByDate(date);
        return WebResult.success(earningsCalendars);
    }

    /**
     * 同步未来30天的财报日历
     * 
     * @return 同步结果
     */
    @GetMapping("/sync")
    public WebResult<Boolean> syncEarningsCalendar() {
        earningsCalendarService.syncEarningsCalendar();
        return WebResult.success(true);
    }
}