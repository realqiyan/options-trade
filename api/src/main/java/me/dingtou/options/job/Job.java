package me.dingtou.options.job;

import java.util.UUID;

/**
 * 任务接口
 * 
 * @author qiyan
 */
public interface Job {
    /**
     * 唯一任务ID
     * 
     * @return
     */
    UUID id();

    /**
     * 任务执行
     * 
     * @param ctx 任务上下文
     * @throws Exception 异常
     */
    <P extends JobArgs> void execute(JobContext<P> ctx) throws Exception;

}
