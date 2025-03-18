package me.dingtou.options.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 统一事件监听器
 */
@Slf4j
@Component
public class EventListener implements ApplicationListener<AppEvent> {

    @Autowired
    private List<EventProcesser> eventProcessers;

    @Override
    public void onApplicationEvent(AppEvent event) {
        log.debug("Event received: {}", event.getDataType());
        eventProcessers.parallelStream()
                .filter(eventProcesser -> eventProcesser.supportType() == event.getDataType())
                .findFirst()
                .ifPresent(eventProcesser -> eventProcesser.process(event));
    }
}
