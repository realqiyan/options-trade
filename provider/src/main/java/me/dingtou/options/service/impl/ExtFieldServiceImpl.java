package me.dingtou.options.service.impl;

import me.dingtou.options.constant.AccountExt;
import me.dingtou.options.dto.ExtField;
import me.dingtou.options.service.ExtFieldService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户扩展字段服务实现类
 */
@Service
public class ExtFieldServiceImpl implements ExtFieldService {

    /**
     * 获取所有账户扩展字段的元数据
     * @return 账户扩展字段元数据列表
     */
    @Override
    public List<ExtField> getAllAccountExtFields() {
        return Arrays.stream(AccountExt.getAllFields())
                .map(ExtField::from)
                .sorted((f1, f2) -> Integer.compare(f1.getSort(), f2.getSort()))
                .collect(Collectors.toList());
    }
}
