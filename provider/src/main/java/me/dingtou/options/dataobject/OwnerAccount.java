package me.dingtou.options.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OwnerAccount {
    /**
     * ID
     */
    private Long id;
    /**
     * 所有者
     */
    private String owner;
    /**
     * 账户ID
     */
    private String accountId;
    /**
     * 平台
     */
    private String platform;
    /**
     * 扩展信息
     */
    private String ext;
}
