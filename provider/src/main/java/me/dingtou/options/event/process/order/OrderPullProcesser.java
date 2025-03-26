package me.dingtou.options.event.process.order;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

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
        SyncOrderJob syncOrderJob = new SyncOrderJob();

        // 一分钟内只创建一个同步任务
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        String now = sdf.format(new Date());
        UUID uuid = UUID.nameUUIDFromBytes(now.getBytes());

        JobClient.submit(uuid,
                syncOrderJob,
                JobContext.of(args),
                Instant.now().plus(30, ChronoUnit.SECONDS));

    }
}
