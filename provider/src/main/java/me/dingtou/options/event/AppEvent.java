package me.dingtou.options.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import me.dingtou.options.constant.PushDataType;

/**
 * 应用事件
 */
@Getter
public class AppEvent extends ApplicationEvent {

    /**
     * 推送数据类型
     */
    private final PushDataType dataType;

    public AppEvent(PushDataType dataType, Object data) {
        super(data);
        this.dataType = dataType;
    }
}
