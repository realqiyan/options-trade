package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 财报日历
 */
@Data
public class EarningsCalendar {
    /**
     * ID
     */
    private Long id;
    /**
     * 股票代码
     */
    private String symbol;

    /**
     * 公司名称
     */
    private String name;

    /**
     * 市值
     */
    private BigDecimal marketCap;

    /**
     * 财报季度结束日期
     */
    private String fiscalQuarterEnding;

    /**
     * 预期每股收益
     */
    private BigDecimal epsForecast;

    /**
     * 分析师数量
     */
    private Integer noOfEsts;

    /**
     * 去年财报发布日期
     */
    private String lastYearRptDt;

    /**
     * 去年每股收益
     */
    private BigDecimal lastYearEps;

    /**
     * 财报发布时间
     */
    private String time;

    /**
     * 财报日期
     */
    private Date earningsDate;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}