package me.dingtou.options.gateway.futu.util;

import java.util.LinkedList;
import java.util.Queue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RateLimiter {

    private final long timeWindowMs;
    private final int maxRequestsPerWindow;
    private final Queue<Long> requestTimestamps;
    private final String name;

    public RateLimiter(String name, long timeWindowMs, int maxRequestsPerWindow) {
        this.name = name;
        this.timeWindowMs = timeWindowMs;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.requestTimestamps = new LinkedList<>();
    }

    public void acquire() {
        synchronized (this) {
            long currentTime = System.currentTimeMillis();

            while (!requestTimestamps.isEmpty() &&
                    currentTime - requestTimestamps.peek() > timeWindowMs) {
                requestTimestamps.poll();
            }

            int currentRequests = requestTimestamps.size();
            log.info("{} requests in window: {}, max allowed: {}", name, currentRequests,
                    maxRequestsPerWindow);

            if (currentRequests >= maxRequestsPerWindow) {
                long oldestRequestTime = requestTimestamps.peek();
                long sleepTime = timeWindowMs - (currentTime - oldestRequestTime) + 1;

                if (sleepTime > 0) {
                    log.info(
                            "{} rate limit enforced. Current requests: {}, waiting for {} ms",
                            name, currentRequests, sleepTime);
                    try {
                        Thread.sleep(sleepTime);
                        currentTime = System.currentTimeMillis();
                        while (!requestTimestamps.isEmpty() &&
                                currentTime - requestTimestamps.peek() > timeWindowMs) {
                            requestTimestamps.poll();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("{} Interrupted while waiting for rate limit", name, e);
                        throw new RuntimeException("Interrupted while waiting for rate limit", e);
                    }
                }
            }

            requestTimestamps.offer(currentTime);
            log.info("{} new window count: {}", name, requestTimestamps.size());
        }
    }
}
