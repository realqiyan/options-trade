package me.dingtou.options.model;

import lombok.Data;

/**
 * 期权基础信息
 *
 * {
 * "security": {
 * "market": 1,
 * "code": "TCH210629C295000"
 * },
 * "id": "80143386",
 * "lotSize": 100,
 * "secType": 8,
 * "name": "腾讯 210629 295.00 购",
 * "listTime": "",
 * "delisting": false
 * }
 *
 * @author qiyan
 */
@Data
public class OptionsBasic {
    /**
     * 证券信息
     */
    private Security security;
    /**
     * ID
     */
    private String id;
    /**
     * 每手数量
     */
    private Integer lotSize;
    /**
     * 类型
     */
    private Integer secType;
    /**
     * 名称
     */
    private String name;
    /**
     * listTime
     */
    private String listTime;
    /**
     * 是否退市
     */
    private Boolean delisting;


}
