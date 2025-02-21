package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 期权链
 *
 * @author qiyan
 */
@Data
public class OptionsChain {

    /**
     * 期权到期日
     */
    private String strikeTime;

    /**
     * 期权到期时间戳
     */
    private Long strikeTimestamp;

    /**
     * 股票指标
     */
    private StockIndicator stockIndicator;



    /**
     * 期权列表
     */
    private List<OptionsTuple> optionList;


    /**
     * AI提示词
     */
    private String prompt;
}
