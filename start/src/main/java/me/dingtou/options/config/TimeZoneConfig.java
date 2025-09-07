package me.dingtou.options.config;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * 时区配置类
 * 确保应用程序在所有环境中使用统一的时区
 */
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // 设置JVM默认时区为中国时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }
}