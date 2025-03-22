package me.dingtou.options.job.recurring;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetSubInfo;
import me.dingtou.options.gateway.futu.executor.func.query.FuncUnsubAll;
import me.dingtou.options.job.Job;

/**
 * 检查订阅信息
 */
@Slf4j
@Component
public class CheckFutuSubJob implements Job {

    private long startTime;

    public CheckFutuSubJob() {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public String id() {
        return this.getClass().getName();
    }

    @Override
    public void run() {
        try {

            FuncGetSubInfo.SubInfo subInfo = QueryExecutor.query(new FuncGetSubInfo());
            if (null == subInfo) {
                log.warn("FuncGetSubInfo 返回结果为空");
                return;
            }
            log.warn("FuncGetSubInfo remainQuota: {} totalUsedQuota: {}", subInfo.getRemainQuota(),
                    subInfo.getTotalUsedQuota());

            if (subInfo.getRemainQuota() < 50) {
                Boolean result = QueryExecutor.query(new FuncUnsubAll());
                log.warn("checkSubInfo -> unsuball: {}", result);
                if (Boolean.TRUE.equals(result)) {
                    QueryExecutor.getSubSecurity().clear();
                }
            }
            long endTime = System.currentTimeMillis();
            log.info("CheckFutuSubJob持续时间: {}ms", endTime - startTime);
        } catch (Exception e) {
            log.error("checkSubInfo -> error", e);
        }
    }

}