package me.dingtou.options.model;

import lombok.Data;

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
     * 期权标的行情
     */
    private SecurityQuote securityQuote;

    /**
     * 期权列表
     */
    private List<OptionsTuple> optionList;
}
