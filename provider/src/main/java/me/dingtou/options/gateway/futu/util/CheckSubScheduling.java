package me.dingtou.options.gateway.futu.util;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetSubInfo;
import me.dingtou.options.gateway.futu.executor.func.query.FuncUnsubAll;
import me.dingtou.options.model.Security;

@Slf4j
@Component
public class CheckSubScheduling {

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    public void checkSubInfo() {
        try {
            List<Security> securities = QueryExecutor.query(new FuncGetSubInfo());
            if (null == securities) {
                log.warn("checkSubInfo -> null");
                return;
            }
            log.warn("checkSubInfo -> size: {}", securities.size());
            for (Security security : securities) {
                log.warn("checkSubInfo -> security: {}", security);
            }

            if (securities.size() > 450) {
                Boolean result = QueryExecutor.query(new FuncUnsubAll());
                log.warn("checkSubInfo -> unsuball: {}", result);
                if (Boolean.TRUE.equals(result)) {
                    QueryExecutor.getSubSecurity().clear();
                }
            }
        } catch (Exception e) {
            log.error("checkSubInfo -> error", e);
        }
    }
}