package me.dingtou.options.model;

import lombok.Data;
import me.dingtou.options.constant.Market;

import java.util.Objects;

/**
 * 证券
 * <p>
 * {
 * "market": 1,
 * "code": "TCH210629C295000"
 * }
 *
 * @author qiyan
 */
@Data
public class Security {
    /**
     * 市场编码
     */
    private Integer market;
    /**
     * 证券代码
     */
    private String code;


    public String toString() {
        String symbol = code + "." + Market.of(market).name();
        return symbol.toUpperCase();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Security security = (Security) o;
        return Objects.equals(market, security.market) && Objects.equals(code, security.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(market, code);
    }


    /**
     * 创建证券
     *
     * @param code   证券代码
     * @param market 市场编码
     * @return 证券
     */
    public static Security of(String code, Integer market) {
        Security security = new Security();
        security.setCode(code);
        security.setMarket(market);
        return security;
    }

    /**
     * 创建证券
     *
     * @param symbol 证券符号
     * @return 证券
     */
    public static Security from(String symbol) {
        if (null == symbol) {
            return null;
        }
        String[] info = symbol.split("\\.");
        if (info.length != 2) {
            return null;
        }
        Security security = new Security();
        security.setCode(info[0]);
        security.setMarket(Market.valueOf(info[1]).getCode());
        return security;
    }
}
