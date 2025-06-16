package me.dingtou.options.config;

import me.dingtou.options.service.AuthService;
import me.dingtou.options.web.filter.LoginFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfiguration {

    @Value("${options-trade.default-owner}")
    private String defaultOwner;

    @Value("${options-trade.cookie.maxAgeDays:7}")
    private int cookieMaxAgeDays;

    @Bean
    public LoginFilter loginFilter(AuthService authService) {
        return new LoginFilter(authService, cookieMaxAgeDays);
    }

    @Bean
    public FilterRegistrationBean<LoginFilter> registerMyFilter(LoginFilter loginFilter) {
        FilterRegistrationBean<LoginFilter> bean = new FilterRegistrationBean<>();
        bean.setOrder(1);
        bean.setFilter(loginFilter);
        bean.addUrlPatterns("/*");
        return bean;
    }
}
