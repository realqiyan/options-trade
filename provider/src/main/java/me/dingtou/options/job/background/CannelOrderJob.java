package me.dingtou.options.job.background;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerOrder;
import me.dingtou.options.service.OptionsTradeService;

@Slf4j
@Component
public class CannelOrderJob implements Job {

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
        // 指定时间取消平仓单
        CannelOrderJobArgs args = (CannelOrderJobArgs) ctx.getJobArgs();
        OwnerOrder ownerOrder = ownerManager.queryOwnerOrder(args.getOwner(), args.getOrderId());
        if (null == ownerOrder) {
            log.warn("订单不存在 orderId:{}", args.getOrderId());
            return;
        }
        if (OwnerOrder.isTraded(ownerOrder) || OwnerOrder.isClose(ownerOrder)) {
            log.warn("订单已平仓或已交易 orderId:{}", args.getOrderId());
            return;
        }
        optionsTradeService.modify(args.getOwner(), args.getOrderId(), OrderAction.CANCEL);
    }

    @Data
    public static class CannelOrderJobArgs implements JobArgs {

        /**
         * 账号
         */
        private String owner;

        /**
         * 订单ID
         */
        private Long orderId;

        /**
         * 订单取消时间
         */
        private Date cannelTime;

    }
}
