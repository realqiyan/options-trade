package me.dingtou.options.service.mcp;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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

}
