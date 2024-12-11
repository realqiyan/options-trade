package me.dingtou.options.dataobject;

import lombok.Data;

@Data
public class OwnerSecurity {
    /**
     * ID
     */
    private Long id;

    /**
     * 所有者
     */
    private String owner;

    /**
     * 代码
     */
    private String code;

    /**
     * 市场
     */
    private Integer market;

    /**
     * 扩展信息
     */
    private String ext;
}
