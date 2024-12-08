package me.dingtou.options.model;

import lombok.Data;

/**
 * 期权
 *
 * @author qiyan
 */
@Data
public class Options {

    /**
     * 期权基础信息
     */
    private OptionsBasic basic;

    /**
     * 期权扩展信息
     */
    private OptionsExtData optionExData;
}
