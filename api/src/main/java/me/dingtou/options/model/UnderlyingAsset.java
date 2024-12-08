package me.dingtou.options.model;

import lombok.Data;

/**
 * 标的Underlying Asset
 *
 * {
 * "market": 1,
 * "code": "00700"
 * }
 *
 * @author qiyan
 */
@Data
public class UnderlyingAsset {
    /**
     * 所属用户
     */
    private String owner;
    /**
     * 市场代码
     */
    private Integer market;
    /**
     * 标的代码
     */
    private String code;

}
