package me.dingtou.options.dto;

import lombok.Data;
import me.dingtou.options.constant.AccountExt;

/**
 * 账户扩展字段元数据
 * 用于前端动态渲染表单
 */
@Data
public class ExtField {
    /**
     * 字段键名
     */
    private String key;

    /**
     * 字段描述
     */
    private String desc;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 排序
     */
    private int sort;

    /**
     * 字段值
     */
    private String value;

    /**
     * 从AccountExt枚举创建AccountExtField对象
     * @param accountExt AccountExt枚举
     * @return AccountExtField对象
     */
    public static ExtField from(AccountExt accountExt) {
        ExtField field = new ExtField();
        field.setKey(accountExt.getKey());
        field.setDesc(accountExt.getDesc());
        field.setType(accountExt.getType());
        field.setSort(accountExt.getSort());
        return field;
    }
}
