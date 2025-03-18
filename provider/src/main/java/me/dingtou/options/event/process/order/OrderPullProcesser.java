package me.dingtou.options.event.process.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.event.AppEvent;
import me.dingtou.options.event.EventProcesser;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerOrder;

@Slf4j
@Component
public class OrderPullProcesser implements EventProcesser {
    @Autowired
    private TradeManager tradeManager;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public PushDataType supportType() {
        return PushDataType.ORDER_PUSH;
    }

    @Override
    public void process(AppEvent event) {
        OwnerOrder ownerOrder = (OwnerOrder) event.getSource();
        log.info("OrderPullProcesser process: {}", ownerOrder);

        // 执行订单同步
        tradeManager.syncOrder(ownerManager.queryOwner(ownerOrder.getOwner()));
    }
}
