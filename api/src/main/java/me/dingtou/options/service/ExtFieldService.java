package me.dingtou.options.service;

import me.dingtou.options.dto.ExtField;

import java.util.List;

/**
 * 账户扩展字段服务接口
 */
public interface ExtFieldService {

    /**
     * 获取所有账户扩展字段的元数据
     * @return 账户扩展字段元数据列表
     */
    List<ExtField> getAllAccountExtFields();

}
