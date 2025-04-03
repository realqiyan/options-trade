package me.dingtou.options.model;

import lombok.Data;
import me.dingtou.options.constant.Market;

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
     * 底层资产
     */
    private final Security security;

    /**
     * 期权到期日
     */
    private final String strikeTime;

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

    public OptionsChain(Security security, String strikeTime) {
        this.security = security;
        this.strikeTime = strikeTime;
    }

    /**
     * 计算DTE
     * 
     * @return 距离行权日起间隔天数
     */
    public int dte() {
        if (null == strikeTime) {
            throw new IllegalArgumentException("strikeTime is null");
        }
        ZoneId zoneId = Market.of(security.getMarket()).getZoneId();
        LocalDate localDate = new Date().toInstant().atZone(zoneId).toLocalDate();
        LocalDate strikeDate = LocalDate.parse(strikeTime);
        return (int) ChronoUnit.DAYS.between(localDate, strikeDate);
    }

}
