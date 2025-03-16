package me.dingtou.options.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.TradeTaskExt;
import me.dingtou.options.constant.TradeTaskStatus;
import me.dingtou.options.constant.TradeTaskType;
import me.dingtou.options.dao.OwnerTradeTaskDAO;
import me.dingtou.options.model.OwnerTradeTask;
import me.dingtou.options.service.TradeTaskService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 交易任务服务实现
 *
 * @author qiyan
 */
@Slf4j
@Service
public class TradeTaskServiceImpl implements TradeTaskService {

    @Autowired
    private OwnerTradeTaskDAO tradeTaskDAO;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTradeTask(String owner, OwnerTradeTask tradeTask) {
        if (tradeTask == null || StringUtils.isEmpty(owner)) {
            return null;
        }

        // 设置所有者
        tradeTask.setOwner(owner);

        // 设置创建时间和更新时间
        Date now = new Date();
        tradeTask.setCreateTime(now);
        tradeTask.setUpdateTime(now);

        // 如果状态为空，设置为待执行
        if (tradeTask.getStatus() == null) {
            tradeTask.setStatus(TradeTaskStatus.PENDING.getCode());
        }

        // 插入数据库
        tradeTaskDAO.insert(tradeTask);
        return tradeTask.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateTradeTask(String owner, List<OwnerTradeTask> tradeTasks) {
        if (tradeTasks == null || tradeTasks.isEmpty() || StringUtils.isEmpty(owner)) {
            return 0;
        }

        int count = 0;
        for (OwnerTradeTask tradeTask : tradeTasks) {
            Long id = createTradeTask(owner, tradeTask);
            if (id != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTradeTask(String owner, OwnerTradeTask tradeTask) {
        if (tradeTask == null || tradeTask.getId() == null || StringUtils.isEmpty(owner)) {
            return false;
        }

        // 查询原任务
        OwnerTradeTask dbTask = tradeTaskDAO.queryTradeTaskById(owner, tradeTask.getId());
        if (dbTask == null) {
            return false;
        }

        // 设置更新时间
        tradeTask.setUpdateTime(new Date());
        tradeTask.setOwner(owner);

        // 更新数据库
        return tradeTaskDAO.updateById(tradeTask) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTradeTask(String owner, Long taskId) {
        if (taskId == null || StringUtils.isEmpty(owner)) {
            return false;
        }

        // 查询原任务
        OwnerTradeTask dbTask = tradeTaskDAO.queryTradeTaskById(owner, taskId);
        if (dbTask == null) {
            return false;
        }

        // 删除数据库记录
        LambdaQueryWrapper<OwnerTradeTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OwnerTradeTask::getOwner, owner).eq(OwnerTradeTask::getId, taskId);
        return tradeTaskDAO.delete(wrapper) > 0;
    }

    @Override
    public List<OwnerTradeTask> queryTradeTask(String owner) {
        if (StringUtils.isEmpty(owner)) {
            return Collections.emptyList();
        }
        return tradeTaskDAO.queryTradeTask(owner);
    }

    @Override
    public List<OwnerTradeTask> queryTradeTaskBySessionId(String owner, String sessionId) {
        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(sessionId)) {
            return Collections.emptyList();
        }
        return tradeTaskDAO.queryTradeTaskBySessionId(owner, sessionId);
    }

    @Override
    public List<OwnerTradeTask> queryTradeTaskByMessageId(String owner, String messageId) {
        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(messageId)) {
            return Collections.emptyList();
        }
        return tradeTaskDAO.queryTradeTaskByMessageId(owner, messageId);
    }

    @Override
    public OwnerTradeTask queryTradeTaskById(String owner, Long id) {
        if (StringUtils.isEmpty(owner) || id == null) {
            return null;
        }
        return tradeTaskDAO.queryTradeTaskById(owner, id);
    }

    @Override
    public List<OwnerTradeTask> queryPendingTradeTask(String owner) {
        if (StringUtils.isEmpty(owner)) {
            return Collections.emptyList();
        }
        return tradeTaskDAO.queryPendingTradeTask(owner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeTradeTask(String owner, Long taskId) {
        if (StringUtils.isEmpty(owner) || taskId == null) {
            return false;
        }

        // 查询原任务
        OwnerTradeTask dbTask = tradeTaskDAO.queryTradeTaskById(owner, taskId);
        if (dbTask == null) {
            return false;
        }

        // 只有待执行的任务才能执行
        if (!TradeTaskStatus.PENDING.getCode().equals(dbTask.getStatus())) {
            return false;
        }

        // 更新任务状态为完成「后续可以更新为EXECUTING」
        LambdaUpdateWrapper<OwnerTradeTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerTradeTask::getId, taskId)
                .eq(OwnerTradeTask::getOwner, owner)
                .set(OwnerTradeTask::getStatus, TradeTaskStatus.COMPLETED.getCode())
                .set(OwnerTradeTask::getUpdateTime, new Date());

         return tradeTaskDAO.update(null, updateWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTradeTask(String owner, Long taskId) {
        if (StringUtils.isEmpty(owner) || taskId == null) {
            return false;
        }

        // 查询原任务
        OwnerTradeTask dbTask = tradeTaskDAO.queryTradeTaskById(owner, taskId);
        if (dbTask == null) {
            return false;
        }

        // 只有待执行或执行中的任务才能取消
        if (!TradeTaskStatus.PENDING.getCode().equals(dbTask.getStatus())
                && !TradeTaskStatus.EXECUTING.getCode().equals(dbTask.getStatus())) {
            return false;
        }

        // 更新任务状态为已取消
        LambdaUpdateWrapper<OwnerTradeTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OwnerTradeTask::getId, taskId)
                .eq(OwnerTradeTask::getOwner, owner)
                .set(OwnerTradeTask::getStatus, TradeTaskStatus.CANCELLED.getCode())
                .set(OwnerTradeTask::getUpdateTime, new Date());

        return tradeTaskDAO.update(null, updateWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OwnerTradeTask> createTradeTaskFromAIMessage(String owner, String sessionId, String messageId,
            String content) {
        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(sessionId)
                || StringUtils.isEmpty(messageId) || StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }

        List<OwnerTradeTask> tradeTasks = new ArrayList<>();

        try {
            // 尝试解析JSON格式的交易行动
            List<OwnerTradeTask> jsonTasks = parseJsonActions(owner, sessionId, messageId, content);
            if (!jsonTasks.isEmpty()) {
                tradeTasks.addAll(jsonTasks);
            }
        } catch (Exception e) {
            log.error("解析AI消息创建交易任务失败", e);
        }

        // 批量创建任务
        if (!tradeTasks.isEmpty()) {
            batchCreateTradeTask(owner, tradeTasks);
        }

        return tradeTasks;
    }

    /**
     * 解析JSON格式的交易行动
     */
    private List<OwnerTradeTask> parseJsonActions(String owner, String sessionId, String messageId, String content) {
        List<OwnerTradeTask> tradeTasks = new ArrayList<>();

        try {
            // 查找JSON格式的交易行动部分
            Pattern pattern = Pattern.compile("```json\\s*\\n(.*?)\\n\\s*```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String jsonContent = matcher.group(1);
                JsonNode rootNode = OBJECT_MAPPER.readTree(jsonContent);

                // 检查是否有actions数组
                if (rootNode.has("actions") && rootNode.get("actions").isArray()) {
                    JsonNode actions = rootNode.get("actions");

                    for (JsonNode action : actions) {
                        OwnerTradeTask task = new OwnerTradeTask();
                        task.setOwner(owner);
                        task.setSessionId(sessionId);
                        task.setMessageId(messageId);
                        task.setStatus(TradeTaskStatus.PENDING.getCode());

                        // 设置任务类型
                        if (action.has("type")) {
                            String type = action.get("type").asText();
                            task.setTaskType(parseTaskType(type));
                        } else {
                            task.setTaskType(TradeTaskType.OTHER.getCode());
                        }

                        // 设置标的信息
                        if (action.has("underlyingCode")) {
                            String symbol = action.get("underlyingCode").asText();
                            // 解析市场和代码
                            parseSymbol(symbol, task);
                        }

                        // 设置策略ID
                        if (action.has("strategyId")) {
                            task.setStrategyId(action.get("strategyId").asText());
                        }

                        // 设置开始时间和结束时间
                        if (action.has("startTime")) {
                            task.setStartTime(parseDate(action.get("startTime").asText()));
                        } else {
                            task.setStartTime(new Date()); // 默认为当前时间
                        }

                        if (action.has("endTime")) {
                            task.setEndTime(parseDate(action.get("endTime").asText()));
                        }

                        // 设置扩展信息
                        setTaskExtInfo(task, action);

                        // 保存原始JSON
                        task.setExtValue(TradeTaskExt.AI_RESPONSE, action.toString());

                        tradeTasks.add(task);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析JSON格式的交易行动失败", e);
        }

        return tradeTasks;
    }

    /**
     * 将JSON中的action类型映射到任务类型
     */
    private Integer parseTaskType(String actionType) {
        if (StringUtils.isEmpty(actionType)) {
            return TradeTaskType.OTHER.getCode();
        }
        switch (actionType.toLowerCase()) {
            case "buy_option":
                return TradeTaskType.BUY_OPTION.getCode();
            case "sell_option":
                return TradeTaskType.SELL_OPTION.getCode();
            case "close_option":
                return TradeTaskType.CLOSE_OPTION.getCode();
            case "roll_option":
                return TradeTaskType.ROLL_OPTION.getCode();
            default:
                return TradeTaskType.OTHER.getCode();
        }
    }

    /**
     * 解析标的代码
     */
    private void parseSymbol(String symbol, OwnerTradeTask task) {
        if (StringUtils.isEmpty(symbol)) {
            return;
        }

        // 假设格式为 "市场.代码"，如 "US.AAPL"
        String[] parts = symbol.split("\\.");
        if (parts.length == 2) {
            // 根据市场名称设置市场代码
            String marketName = parts[0].toUpperCase();
            switch (marketName) {
                case "US":
                    task.setMarket(11); // 美股市场代码
                    break;
                case "HK":
                    task.setMarket(1); // 港股市场代码
                    break;
                default:
                    task.setMarket(-1); // 未知市场
            }
            task.setCode(parts[1]);
        } else {
            // 如果没有市场信息，直接设置代码
            task.setCode(symbol);
        }
    }

    /**
     * 解析日期字符串
     */
    private Date parseDate(String dateStr) {
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }

        try {
            // 尝试解析多种日期格式
            String[] dateFormats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "yyyy/MM/dd",
                    "MM/dd/yyyy",
                    "dd/MM/yyyy"
            };

            for (String format : dateFormats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    return sdf.parse(dateStr);
                } catch (ParseException e) {
                    // 尝试下一种格式
                }
            }
        } catch (Exception e) {
            log.error("解析日期失败: {}", dateStr, e);
        }

        return null;
    }

    /**
     * 设置任务扩展信息
     */
    private void setTaskExtInfo(OwnerTradeTask task, JsonNode action) {
        TradeTaskExt[] values = TradeTaskExt.values();
        for (TradeTaskExt value : values) {
            if (action.has(value.getKey())) {
                task.setExtValue(value, action.get(value.getKey()).asText());
            }
        }
    }
}