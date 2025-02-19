package me.dingtou.options.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 实时数据
 */

@Getter
public class PushData {
    /**
     * 数据
     */
    private final Map<String, Object> data = new HashMap<>();
}
