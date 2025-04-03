package me.dingtou.options.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 期权链
 *
 * @author qiyan
 */
@Data
public class OptionsChain {
    /**
     * 纽约时区
     */
    private static final ZoneId NEW_YORK_ZONE_ID = ZoneId.of("America/New_York");

    /**
     * 期权到期日
     */
    private String strikeTime;

    /**
     * 期权列表
     */
    private List<Options> optionsList;

    /**
     * VIX指标
     */
    private VixIndicator vixIndicator;

    /**
     * 股票指标
     */
    private StockIndicator stockIndicator;

    /**
     * 交易级别
     * 0: 不推荐
     * 1: 正常
     */
    private Integer tradeLevel;

    /**
     * AI提示词
     */
    private String prompt;

    /**
     * 计算DTE
     * 
     * @return 距离行权日起间隔天数
     */
    public int dte() {
        if (null == strikeTime) {
            throw new IllegalArgumentException("strikeTime is null");
        }
        // 使用America/New_York时区
        LocalDate localDate = new Date().toInstant().atZone(NEW_YORK_ZONE_ID).toLocalDate();
        LocalDate strikeDate = LocalDate.parse(strikeTime);
        return (int) ChronoUnit.DAYS.between(localDate, strikeDate);
    }

}
