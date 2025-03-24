package me.dingtou.options.job.background;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.service.OptionsTradeService;

@Slf4j
@Component
public class CloseOrderJob implements Job {

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private OptionsTradeService optionsTradeService;

    @Override
    public UUID id() {
        return UUID.randomUUID();
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) throws Exception {
        CloseOrderJobArgs args = (CloseOrderJobArgs) ctx.getJobArgs();
        OwnerOrder ownerOrder = ownerManager.queryOwnerOrder(args.getOwner(), args.getOrderId());
        if (null == ownerOrder) {
            log.warn("订单不存在 orderId:{}", args.getOrderId());
            return;
        }
        if (!OwnerOrder.isTraded(ownerOrder)) {
            throw new IllegalArgumentException("订单状态不正确 orderId:" + args.getOrderId());
        }
        OwnerOrder order = optionsTradeService.close(args.getOwner(), args.getOrderId(), args.getPrice(), args.getCannelTime());
        if (null == order) {
            log.warn("平仓失败 orderId:{}", args.getOrderId());
            return;
        }
        log.warn("平仓成功 orderId:{}", args.getOrderId());
    }

    @Data
    public static class CloseOrderJobArgs implements JobArgs {

        /**
         * 任务ID
         */
        private UUID jobId;

        /**
         * 账号
         */
        private String owner;

        /**
         * 订单ID
         */
        private Long orderId;

        /**
         * 平仓价格
         */
        private BigDecimal price;

        /**
         * 平仓截止时间
         */
        private Date cannelTime;

    }

}
