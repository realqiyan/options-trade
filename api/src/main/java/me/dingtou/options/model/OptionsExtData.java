package me.dingtou.options.model;

import lombok.Data;

/**
 * 期权扩展数据
 *
 * {
 * "type": 1,
 * "owner": {
 * "market": 1,
 * "code": "00700"
 * },
 * "strikeTime": "2021-06-29",
 * "strikePrice": 295,
 * "suspend": false,
 * "market": "",
 * "strikeTimestamp": 1624896000
 * }
 *
 * @author qiyan
 */
@Data
public class OptionsExtData {
    /*
     * 1: call, 2: put
     */
    private Integer type;
    /**
     * UnderlyingAsset
     */
    private UnderlyingAsset owner;
    /**
     * 期权到期日 格式：yyyy-MM-dd
     */
    private String strikeTime;
    /**
     * 行权价
     */
    private Double strikePrice;
    /**
     * 是否停牌
     */
    private Boolean suspend;
    /**
     * 市场
     */
    private String market;
    /**
     * 行权时间戳
     */
    private Long strikeTimestamp;
}
