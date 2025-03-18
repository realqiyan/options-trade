package me.dingtou.options.service;

import me.dingtou.options.model.OwnerTradeTask;

import java.util.List;

/**
 * 交易任务服务
 *
 * @author qiyan
 */
public interface TradeTaskService {

    /**
     * 创建交易任务
     *
     * @param owner     所有者
     * @param tradeTask 交易任务
     * @return 交易任务ID
     */
    Long createTradeTask(String owner, OwnerTradeTask tradeTask);

    /**
     * 批量创建交易任务
     *
     * @param owner      所有者
     * @param tradeTasks 交易任务列表
     * @return 创建成功的任务数量
     */
    int batchCreateTradeTask(String owner, List<OwnerTradeTask> tradeTasks);

    /**
     * 更新交易任务
     *
     * @param owner     所有者
     * @param tradeTask 交易任务
     * @return 是否成功
     */
    boolean updateTradeTask(String owner, OwnerTradeTask tradeTask);

    /**
     * 删除交易任务
     *
     * @param owner  所有者
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean deleteTradeTask(String owner, Long taskId);

    /**
     * 查询用户的交易任务
     *
     * @param owner 所有者
     * @return 交易任务列表
     */
    List<OwnerTradeTask> queryTradeTask(String owner);

    /**
     * 根据会话ID查询交易任务
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @return 交易任务列表
     */
    List<OwnerTradeTask> queryTradeTaskBySessionId(String owner, String sessionId);


    /**
     * 根据任务ID查询交易任务
     *
     * @param owner 所有者
     * @param id    任务ID
     * @return 交易任务
     */
    OwnerTradeTask queryTradeTaskById(String owner, Long id);

    /**
     * 查询待执行的交易任务
     *
     * @param owner 所有者
     * @return 交易任务列表
     */
    List<OwnerTradeTask> queryPendingTradeTask(String owner);

    /**
     * 执行交易任务
     *
     * @param owner  所有者
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean executeTradeTask(String owner, Long taskId);

    /**
     * 取消交易任务
     *
     * @param owner  所有者
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTradeTask(String owner, Long taskId);

    /**
     * 从AI助手消息中创建交易任务
     *
     * @param owner     所有者
     * @param sessionId 会话ID
     * @param content   消息内容
     * @return 创建的交易任务列表
     */
    List<OwnerTradeTask> createTradeTaskFromAIMessage(String owner, String sessionId, String content);
} 