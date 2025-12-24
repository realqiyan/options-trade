package me.dingtou.options.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OwnerFlowSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Date createTime;

    private Date updateTime;

    private String owner;

    private String platform;

    private Long cashflowId;

    private Date clearingDate;

    private Date settlementDate;

    private String currency;

    private String cashflowType;

    private String cashflowDirection;

    private BigDecimal cashflowAmount;

    private String cashflowRemark;

}
