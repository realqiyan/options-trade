package me.dingtou.options.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.Market;
import me.dingtou.options.manager.OwnerFlowSummaryManager;
import me.dingtou.options.manager.OwnerManager;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.OwnerFlowSummary;
import me.dingtou.options.service.FlowSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资金流水服务实现类
 *
 * @author qiyan
 */
@Service
@Slf4j
public class FlowSummaryServiceImpl implements FlowSummaryService {

    @Autowired
    private OwnerFlowSummaryManager ownerFlowSummaryManager;

    @Autowired
    private OwnerManager ownerManager;

    @Override
    public int syncFlowSummary(String owner, String clearingMonth) {
        OwnerAccount account = ownerManager.queryOwnerAccount(owner);
        YearMonth yearMonth = YearMonth.parse(clearingMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        int totalDays = yearMonth.lengthOfMonth();
        int totalCount = 0;

        for (int day = 1; day <= totalDays; day++) {
            LocalDate date = yearMonth.atDay(day);
            String clearingDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.warn("sleep error, owner={}, clearingDate={}", owner, clearingDate);
            }
            log.info("sync flow summary, owner={}, clearingDate={}", owner, clearingDate);
            int count = ownerFlowSummaryManager.syncFlowSummary(account, Market.HK, clearingDate);
            totalCount += count;
            count = ownerFlowSummaryManager.syncFlowSummary(account, Market.US, clearingDate);
            totalCount += count;
        }

        return totalCount;
    }

    @Override
    public int syncFlowSummaryByYear(String owner, String year) {
        int totalCount = 0;
        for (int month = 1; month <= 12; month++) {
            String clearingMonth = year + "-" + String.format("%02d", month);
            totalCount += syncFlowSummary(owner, clearingMonth);
        }
        return totalCount;
    }

    @Override
    public List<OwnerFlowSummary> listFlowSummary(String owner, String startDate, String endDate) {
        return ownerFlowSummaryManager.listFlowSummary(owner, startDate, endDate);
    }

    @Override
    public List<OwnerFlowSummary> listFlowSummaryByPlatform(String owner, String platform) {
        return ownerFlowSummaryManager.listFlowSummaryByPlatform(owner, platform);
    }

    @Override
    public List<OwnerFlowSummary> getFlowSummary(String owner, String clearingDate) {
        return ownerFlowSummaryManager.getFlowSummary(owner, clearingDate);
    }

    @Override
    public OwnerFlowSummary getFlowSummaryById(Long id) {
        return ownerFlowSummaryManager.getFlowSummaryById(id);
    }

    @Override
    public OwnerFlowSummary getFlowSummaryByCashflowId(String owner, Long cashflowId) {
        return ownerFlowSummaryManager.getFlowSummaryByCashflowId(owner, cashflowId);
    }
}