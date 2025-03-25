package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 期权行情数据
 * <p>
 * {
 * "contractMultiplier": 100,
 * "contractNominalValue": 0,
 * "contractSize": 100,
 * "contractSizeFloat": 100,
 * "delta": 0.211882744,
 * "expiryDateDistance": 5,
 * "gamma": 0.061131966,
 * "impliedVolatility": 45.424,
 * "indexOptionType": 0,
 * "netOpenInterest": 0,
 * "openInterest": 7343,
 * "optionAreaType": 1,
 * "ownerLotMultiplier": 0,
 * "premium": 5.364,
 * "rho": 0.002602093,
 * "strikePrice": 90,
 * "theta": -0.131148082,
 * "vega": 0.030221842
 * }
 */
@Data
public class OptionsRealtimeData {

    //证券
    private Security security;
    //希腊值 Delta
    private BigDecimal delta;
    //希腊值 Gamma
    private BigDecimal gamma;
    //希腊值 Theta
    private BigDecimal theta;
    //希腊值 Vega
    private BigDecimal vega;
    //希腊值 Rho
    private BigDecimal rho;
    //隐含波动率（该字段为百分比字段，默认不展示 %，如 20 实际对应 20%）
    private BigDecimal impliedVolatility;
    //溢价（该字段为百分比字段，默认不展示 %，如 20 实际对应 20%）
    private BigDecimal premium;
    //当前价格
    private BigDecimal curPrice;
    //未平仓数量
    private Integer openInterest;
    //成交量
    private Long volume;
    //时间价值
    private BigDecimal timeValue;
    //内在价值
    private BigDecimal intrinsicValue;

}
