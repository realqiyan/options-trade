package me.dingtou.options.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 用户期权策略
 *
 * @author qiyan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OwnerSecurity extends Security {
    /**
     * ID
     */
    private Long id;
    /**
     * 名字
     */
    private String name;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 所有者的标识符，表示策略属于哪个用户或实体
     */
    private String owner;
    /**
     * 状态
     */
    private Integer status;
}
