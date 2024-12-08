package me.dingtou.options.model;

import lombok.Data;

/**
 * 期权组
 *
 * @author qiyan
 */
@Data
public class OptionsTuple {

    /**
     * call
     */
    private Options call;

    /**
     * put
     */
    private Options put;
}
