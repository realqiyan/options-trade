package me.dingtou.options.web;

import me.dingtou.options.model.OwnerFlowSummary;
import me.dingtou.options.service.FlowSummaryService;
import me.dingtou.options.web.util.SessionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资金流水控制器
 *
 * @author qiyan
 */
@RestController
@RequestMapping("/api/flow")
public class FlowSummaryController {

    @Autowired
    private FlowSummaryService ownerFlowSummaryService;

    /**
     * 同步资金流水
     *
     * @param owner         所有者
     * @param platform      平台
     * @param clearingMonth 清算月份(yyyy-MM)
     * @return 同步结果
     */
    @PostMapping("/sync")
    public Map<String, Object> syncFlowSummary(@RequestParam String clearingMonth) {
        Map<String, Object> result = new HashMap<>();
        try {
            String owner = SessionUtils.getCurrentOwner();
            int count = ownerFlowSummaryService.syncFlowSummary(owner, clearingMonth);
            result.put("code", 0);
            result.put("message", "同步成功");
            result.put("data", count);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", "同步失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 查询资金流水列表
     *
     * @param owner 所有者
     * @return 资金流水列表
     */
    @GetMapping("/list")
    public Map<String, Object> listFlowSummary() {
        Map<String, Object> result = new HashMap<>();
        try {
            String owner = SessionUtils.getCurrentOwner();
            List<OwnerFlowSummary> flowSummaries = ownerFlowSummaryService.listFlowSummary(owner);
            result.put("code", 0);
            result.put("message", "查询成功");
            result.put("data", flowSummaries);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询资金流水详情
     *
     * @param id 流水ID
     * @return 资金流水详情
     */
    @GetMapping("/detail")
    public Map<String, Object> getFlowSummaryDetail(@RequestParam Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            OwnerFlowSummary flowSummary = ownerFlowSummaryService.getFlowSummaryById(id);
            result.put("code", 0);
            result.put("message", "查询成功");
            result.put("data", flowSummary);
        } catch (Exception e) {
            result.put("code", 1);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
}