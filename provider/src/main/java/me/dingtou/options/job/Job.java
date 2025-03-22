package me.dingtou.options.job;


/**
 * 任务接口
 */
public interface Job {
    /**
     * 任务ID
     * 
     * @return
     */
    String id();

    /**
     * 执行任务
     */
    void run() throws Exception;

}
