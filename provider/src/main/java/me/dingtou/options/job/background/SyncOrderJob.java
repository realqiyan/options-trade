package me.dingtou.options.job.background;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Data;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.manager.TradeManager;
import me.dingtou.options.model.Owner;

@Component
public class SyncOrderJob implements Job {

    @Autowired
    private OwnerManager ownerManager;
    @Autowired
    private TradeManager tradeManager;

    @Override
    public UUID id() {
        return UUID.nameUUIDFromBytes(this.getClass().getName().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) throws Exception {
        SyncOrderJobArgs args = (SyncOrderJobArgs) ctx.getJobArgs();
        Owner owner = ownerManager.queryOwner(args.getOwner());
        if (owner == null) {
            throw new Exception("Owner not found");
        }
        tradeManager.syncOrder(owner);
    }

    @Data
    public static class SyncOrderJobArgs implements JobArgs {
        /**
         * 用户名
         */
        private String owner;
    }

}
