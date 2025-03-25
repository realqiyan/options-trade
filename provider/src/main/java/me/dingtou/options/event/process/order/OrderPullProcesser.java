package me.dingtou.options.event.process.order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.PushDataType;
import me.dingtou.options.event.AppEvent;
import me.dingtou.options.event.EventProcesser;
import me.dingtou.options.job.JobClient;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.job.background.SyncOrderJob;
import me.dingtou.options.job.background.SyncOrderJob.SyncOrderJobArgs;
import me.dingtou.options.model.OwnerOrder;

@Slf4j
@Component
public class OrderPullProcesser implements EventProcesser {

    @Override
    public PushDataType supportType() {
        return PushDataType.ORDER_PUSH;
    }

    @Override
    public void process(AppEvent event) {
        OwnerOrder ownerOrder = (OwnerOrder) event.getSource();
        log.info("OrderPullProcesser process: {}", ownerOrder);

        // 执行订单同步
        SyncOrderJobArgs args = new SyncOrderJobArgs();
        args.setOwner(ownerOrder.getOwner());
        JobClient.submit(new SyncOrderJob(),
                JobContext.of(args),
                Instant.now().plus(30, ChronoUnit.SECONDS));

    }
}
