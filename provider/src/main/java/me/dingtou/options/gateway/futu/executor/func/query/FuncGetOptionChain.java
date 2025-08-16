package me.dingtou.options.gateway.futu.executor.func.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.alibaba.fastjson2.JSON;
import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOptionChain;
import com.futu.openapi.pb.QotGetOptionChain.C2S.Builder;
import com.futu.openapi.pb.QotGetOptionChain.OptionItem;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.Options;
import me.dingtou.options.model.OptionsTuple;

/**
 * 获取期权链
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetOptionChain implements QueryFunctionCall<List<Options>> {

    private static final long TIME_WINDOW_MS = 30000; // 30秒时间窗口
    private static final int MAX_REQUESTS_PER_WINDOW = 9; // 每个时间窗口最多9次请求
    private static final Queue<Long> requestTimestamps = new LinkedList<>();

    private final int market;
    private final String code;
    private final String strikeTime;
    private final boolean isAll;

    public FuncGetOptionChain(int market, String code, String strikeTime) {
        this(market, code, strikeTime, false);
    }

    public FuncGetOptionChain(int market, String code, String strikeTime, boolean isAll) {
        this.market = market;
        this.code = code;
        this.strikeTime = strikeTime;
        this.isAll = isAll;
    }

    @Override
    public int call(QueryExecutor client) {
        // 实现滑动窗口频率限制，30秒内最多10次请求
        synchronized (FuncGetOptionChain.class) {
            long currentTime = System.currentTimeMillis();

            // 清理时间窗口外的旧请求记录
            while (!requestTimestamps.isEmpty() &&
                    currentTime - requestTimestamps.peek() > TIME_WINDOW_MS) {
                requestTimestamps.poll();
            }

            // 记录当前窗口内的请求数量
            int currentRequests = requestTimestamps.size();
            log.info("GetOptionChain requests in window: {}, max allowed: {}", currentRequests,
                    MAX_REQUESTS_PER_WINDOW);

            // 检查是否超过请求限制
            if (currentRequests >= MAX_REQUESTS_PER_WINDOW) {
                // 计算需要等待的时间
                long oldestRequestTime = requestTimestamps.peek();
                long sleepTime = TIME_WINDOW_MS - (currentTime - oldestRequestTime) + 1; // 加1毫秒确保窗口移动

                if (sleepTime > 0) {
                    log.info(
                            "GetOptionChain rate limit enforced for market: {}, code: {}, strikeTime: {}. Current requests: {}, waiting for {} ms",
                            market, code, strikeTime, currentRequests, sleepTime);
                    try {
                        Thread.sleep(sleepTime);
                        // 等待后需要更新当前时间并重新清理过期记录
                        currentTime = System.currentTimeMillis();
                        while (!requestTimestamps.isEmpty() &&
                                currentTime - requestTimestamps.peek() > TIME_WINDOW_MS) {
                            requestTimestamps.poll();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting for rate limit for market: {}, code: {}, strikeTime: {}",
                                market, code, strikeTime, e);
                        throw new RuntimeException("Interrupted while waiting for rate limit", e);
                    }
                }
            }

            // 记录当前请求时间
            requestTimestamps.offer(currentTime);
            log.info("GetOptionChain new window count: {}", requestTimestamps.size());
        }

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(market)
                .setCode(code)
                .build();

        QotGetOptionChain.DataFilter.Builder builder = QotGetOptionChain.DataFilter.newBuilder();
        // builder.setDeltaMax(0.800).setDeltaMin(-0.800);
        QotGetOptionChain.DataFilter dataFilter = builder.build();

        Builder chainBuilder = QotGetOptionChain.C2S.newBuilder();
        if (!isAll) {
            chainBuilder.setDataFilter(dataFilter)
                    .setCondition(QotGetOptionChain.OptionCondType.OptionCondType_Outside_VALUE);
        }
        QotGetOptionChain.C2S c2s = chainBuilder
                .setOwner(sec)
                .setBeginTime(strikeTime)
                .setEndTime(strikeTime)
                .build();

        QotGetOptionChain.Request req = QotGetOptionChain.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOptionChain(req);
        log.debug("Send QotGetOptionChain: {}", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOptionChain error");
        }
        return seqNo;
    }

    @Override
    public List<Options> result(GeneratedMessageV3 response) {

        QotGetOptionChain.Response resp = (QotGetOptionChain.Response) response;
        if (null == resp || 0 != resp.getRetType()) {
            throw new RuntimeException(resp != null ? resp.getRetMsg() : "QotGetOptionChain error");
        }
        QotGetOptionChain.OptionChain optionChain = resp.getS2C().getOptionChainList().iterator().next();

        return convert(optionChain);
    }

    private List<Options> convert(QotGetOptionChain.OptionChain resp) {
        if (resp == null || null == resp.getOptionList()) {
            return Collections.emptyList();
        }
        List<Options> optionsList = new ArrayList<>();

        for (QotGetOptionChain.OptionItem optionItem : resp.getOptionList()) {
            OptionsTuple optionsTuple = convert(optionItem);

            Options call = optionsTuple.getCall();
            if (null != call && null != call.getBasic() && !"0".equals(call.getBasic().getId())) {
                optionsList.add(call);
            }

            Options put = optionsTuple.getPut();
            if (null != put && null != put.getBasic() && !"0".equals(put.getBasic().getId())) {
                optionsList.add(put);
            }
        }
        return optionsList;
    }

    /**
     * 对象转换
     * 
     * @param optionItem API数据结构
     * @return OptionsTuple
     */
    private OptionsTuple convert(OptionItem optionItem) {
        String jsonString = JSON.toJSONString(optionItem);
        OptionsTuple convertValue = JSON.parseObject(jsonString, OptionsTuple.class);
        return convertValue;
    }

}
