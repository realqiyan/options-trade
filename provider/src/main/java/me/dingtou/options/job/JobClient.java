package me.dingtou.options.job;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.BackgroundJob;

/**
 * 任务统一处理类
 */
public class JobClient {

    /**
     * 提交一次性立即执行任务
     * 
     * @param <P>        任务参数类型
     * @param job        任务
     * @param jobContext 任务上下文
     */
    public static <P extends JobArgs> UUID submit(Job job, JobContext<P> ctx) {
        JobId jobId = BackgroundJob.enqueue(job.id(), () -> job.execute(ctx));
        return jobId.asUUID();
    }

    /**
     * 提交一次性指定时间执行任务
     * 
     * @param <P>        任务参数类型
     * @param job        任务
     * @param jobContext 任务上下文
     * @param instant    任务执行时间
     */
    public static <P extends JobArgs> UUID submit(Job job, JobContext<P> ctx, Instant instant) {
        JobId jobId = BackgroundJob.schedule(instant, () -> job.execute(ctx));
        return jobId.asUUID();
    }

    /**
     * 提交周期任务
     * 
     * @param <P>        任务参数类型
     * @param job        任务
     * @param jobContext 任务上下文
     * @param interval   任务间隔
     */
    public static <P extends JobArgs> UUID submit(Job job, JobContext<P> ctx, Duration interval) {
        String jobId = BackgroundJob.scheduleRecurrently(job.id().toString(), interval, () -> job.execute(ctx));
        return UUID.nameUUIDFromBytes(jobId.getBytes(StandardCharsets.UTF_8));
    }

}
