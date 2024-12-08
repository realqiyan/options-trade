package me.dingtou.options.model;

import lombok.Data;

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
}
