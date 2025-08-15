package me.dingtou.options.web;

import me.dingtou.options.dto.ExtField;
import me.dingtou.options.service.ExtFieldService;
import me.dingtou.options.web.model.WebResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 账户扩展字段控制器
 */
@RestController
@RequestMapping("/field/ext")
public class ExtFieldController {

    @Resource
    private ExtFieldService extFieldService;

    /**
     * 获取所有账户扩展字段的元数据
     *
     * @return 账户扩展字段元数据列表
     */
    @GetMapping("/account")
    public WebResult<List<ExtField>> getAllAccountExtFields() {
        return WebResult.success(extFieldService.getAllAccountExtFields());
    }
}
