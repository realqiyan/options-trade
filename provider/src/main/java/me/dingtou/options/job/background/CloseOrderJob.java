package me.dingtou.options.job.background;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;

@Slf4j
@Component
public class CloseOrderJob implements Job{

    @Override
    public UUID id() {
        return UUID.randomUUID();
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) throws Exception {
        // TODO 交易完成后 创建平仓单（可以联动交易回调 ）
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }


    @Data
    public static class CloseOrderJobArgs implements JobArgs {

        /**
         * 订单ID
         */
        private Long orderId;

        /**
         * 盈利比例
         */
        private BigDecimal profitRatio;

    }

}
