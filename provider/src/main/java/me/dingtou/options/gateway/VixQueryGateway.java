package me.dingtou.options.gateway;

import me.dingtou.options.model.VixIndicator;

/**
 * VIX查询网关
 *
 * @author qiyan
 */
public interface VixQueryGateway {
    /**
     * 查询VIX
     *
     * @return VixIndicator
     */
     VixIndicator queryCurrentVix();
}
