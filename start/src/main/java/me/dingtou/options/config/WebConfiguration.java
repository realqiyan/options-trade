package me.dingtou.options.config;

import me.dingtou.options.config.security.ApiKeyRepository;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.web.filter.LoginFilter;

import org.springaicommunity.mcp.security.server.config.McpApiKeyConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
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

    /**
     * 配置安全过滤器链
     * 
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ApiKeyRepository apiKeyRepository) throws Exception {
        http
                // 禁用CSRF保护
                .csrf(csrf -> csrf.disable())
                // 禁用HTTP基础认证
                .httpBasic(basic -> basic.disable())
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // 禁用登出功能
                .logout(logout -> logout.disable())
                // 配置授权规则：只拦截/mcp路径，其他路径允许所有请求
                .authorizeHttpRequests(auth -> auth
                        // 对/mcp路径进行权限检查（这里可以根据需要添加具体的认证要求）
                        .requestMatchers("/mcp")
                        .authenticated()
                        // 允许所有其他请求，不进行权限检查
                        .anyRequest().permitAll())
                .with(McpApiKeyConfigurer.mcpServerApiKey(), (mcpApiKeyConfigurer) -> {
                    mcpApiKeyConfigurer.apiKeyRepository(apiKeyRepository);
                });

        return http.build();
    }

    @Bean
    public ApiKeyRepository apiKeyRepository(AuthService authService) throws Exception {
        return new ApiKeyRepository(authService, "bcrypt", new BCryptPasswordEncoder());
    }

}
