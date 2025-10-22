package me.dingtou.options.model;

import lombok.Data;

/**
 * 期权链到期日
 *
 * @author qiyan
 *         {
 *         "strikeTime": "2021-06-29",
 *         "strikeTimestamp": 1.624896E9,
 *         "optionExpiryDateDistance": 5
 *         }
 */
@Data
public class OptionsStrikeDate {
    /*
     * "strikeTime": "2021-06-29",
     */
    private String strikeTime;
    /**
     * "strikeTimestamp": 1.624896E9,
     */
    private long strikeTimestamp;
    /**
     * "optionExpiryDateDistance": 5
     */
    private int optionExpiryDateDistance;

    /**
     * 期权标签（例如：月度、周度）
     */
    private String tag;

}
