package me.dingtou.options.job.recurring;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetSubInfo;
import me.dingtou.options.gateway.futu.executor.func.query.FuncUnsubAll;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobClient;
import me.dingtou.options.job.JobContext;

/**
 * 检查富途OpenAPI订阅信息
 */
@Slf4j
@Component
public class CheckFutuSubJob implements Job {

    private long startTime;

    public CheckFutuSubJob() {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public UUID id() {
        return UUID.nameUUIDFromBytes(this.getClass().getName().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) {
        
        // 使用简单任务代替JobClient 删除老的任务
        JobClient.deleteRecurringJob(id());

        //checkSubInfo();
    }
    /**
     * 每10分钟执行一次检查订阅信息
     */
    @Scheduled(fixedRate = 600000)
    public void checkSubInfo() {
        try {
            FuncGetSubInfo.SubInfo subInfo = QueryExecutor.query(new FuncGetSubInfo());
            if (null == subInfo) {
                log.warn("FuncGetSubInfo 返回结果为空");
                return;
            }
            log.warn("FuncGetSubInfo remainQuota: {} totalUsedQuota: {}", subInfo.getRemainQuota(),
                    subInfo.getTotalUsedQuota());

            if (subInfo.getRemainQuota() < 200) {
                Boolean result = QueryExecutor.query(new FuncUnsubAll());
                log.warn("checkSubInfo -> unsuball: {}", result);
                if (Boolean.TRUE.equals(result)) {
                    QueryExecutor.getSubSecurity().clear();
                }
            }
            long endTime = System.currentTimeMillis();
            log.info("CheckFutuSubJob持续时间: {}s", (endTime - startTime)/1000);
        } catch (Exception e) {
            log.error("checkSubInfo -> error", e);
        }
    }

    /**
     * 检查订阅信息任务参数
     */
    public static class CheckFutuSubJobArgs implements JobArgs {
    }
}