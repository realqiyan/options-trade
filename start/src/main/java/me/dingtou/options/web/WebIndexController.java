package me.dingtou.options.web;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 默认首页
 *
 * @author qiyan
 */
@Slf4j
@Controller
public class WebIndexController {


    /**
     * 默认打开账户汇总summary.html
     */
    @GetMapping("/")
    public String index() {
        return "forward:/summary.html";
    }

}
