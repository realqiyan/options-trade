package me.dingtou.options.job;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 任务上下文
 * 
 * @param <P> 任务参数类型
 * @author qiyan
 */
@Data
public class JobContext<P extends JobArgs> {

    private P jobArgs;

    private Map<String, String> args;

    public JobContext() {
        this.args = new HashMap<>();
    }

    /**
     * 添加参数
     * 
     * @param key   参数名
     * @param value 参数值
     */
    public void addArg(String key, String value) {
        args.put(key, value);
    }

    /**
     * 获取参数
     * 
     * @param key 参数名
     * @return 参数值
     */
    public String getArg(String key) {
        return args.get(key);
    }

    /**
     * 创建任务上下文
     * 
     * @param <P>     任务参数类型
     * @param jobArgs 任务参数
     * @return 任务上下文
     */
    public static <P extends JobArgs> JobContext<P> of(P jobArgs) {
        JobContext<P> jobContext = new JobContext<>();
        jobContext.setJobArgs(jobArgs);
        return jobContext;
    }

}
