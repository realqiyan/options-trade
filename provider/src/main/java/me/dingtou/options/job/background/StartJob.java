package me.dingtou.options.job.background;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.job.Job;
import me.dingtou.options.job.JobArgs;
import me.dingtou.options.job.JobContext;

@Slf4j
@Component
public class StartJob implements Job {

    @Override
    public UUID id() {
        return UUID.randomUUID();
    }

    @Override
    public <P extends JobArgs> void execute(JobContext<P> ctx) throws Exception {
        log.warn("StartJob success");
    }

    public static class StartJobArgs implements JobArgs {

    }

}
