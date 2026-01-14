package me.dingtou.options.service.mcp;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.alibaba.fastjson.JSON;

/**
 * BaseMcpService
 */
public abstract class BaseMcpService {

    /**
     * 获取当前用户
     * 
     * @return
     */
    public String getOwner() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (null == context) {
            return null;
        }

        Authentication authentication = context.getAuthentication();
        if (null == authentication) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * 是否是json格式
     * 
     * @param format 数据格式：json、markdown
     * @return 是否是json格式
     */
    public boolean isJson(String format) {
        return null == format || "json".equalsIgnoreCase(format);
    }

    /**
     * 转JSON格式
     * 
     * @param data 原始数据
     * @return JSON String
     */
    public String jsonString(Object data) {
        return JSON.toJSONString(data);
    }

}
