package me.dingtou.options;

import me.dingtou.options.gateway.futu.executor.BaseConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("me.dingtou.options.dao")
public class Application {

    public static void main(String[] args) {
        BaseConfig.init();
        SpringApplication.run(Application.class, args);
    }

}
