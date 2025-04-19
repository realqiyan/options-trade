package me.dingtou.options.gateway.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.VixQueryGateway;
import me.dingtou.options.model.StockIndicatorItem;
import me.dingtou.options.model.VixIndicator;
import me.dingtou.options.util.NumberUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VixQueryGatewayImpl implements VixQueryGateway {

    /**
     * VIX缓存
     */
    private static final Cache<String, VixIndicator> VIX_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final String URL = "https://www.spglobal.com/spdji/zh/util/redesign/get-index-comparison-data.dot?compareArray=92026376&compareArray=340&periodFlag=monthToDateFlag&language_id=142";


    @Override
    public VixIndicator queryCurrentVix() {

        VixIndicator vixIndicator = VIX_CACHE.getIfPresent("vixIndicator");
        if (null != vixIndicator) {
            return vixIndicator;
        }


        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("dnt", "1")
                .header("priority", "u=0, i")
                .header("sec-ch-ua", "\"Not(A:Brand\";v=\"99\", \"Microsoft Edge\";v=\"133\", \"Chromium\";v=\"133\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "document")
                .header("sec-fetch-mode", "navigate")
                .header("sec-fetch-site", "none")
                .header("sec-fetch-user", "?1")
                .header("upgrade-insecure-requests", "1")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0")
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                VixIndicator indicator = convert(response.body());
                VIX_CACHE.put("vixIndicator", indicator);
                return indicator;
            }
            log.error("查询VIX失败,statusCode:{}", response.statusCode());
        } catch (Exception e) {
            log.error("查询VIX失败,message:{}", e.getMessage());
        }

        return null;
    }

    /**
     * 解析json
     *
     * @param json json
     * @return VixIndicator
     */
    private VixIndicator convert(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        JsonNode indexPerformance = root.path("performanceComparisonHolder").path("indexPerformanceForComparison");

        StockIndicatorItem vix = null;
        StockIndicatorItem sp500 = null;
        BigDecimal vixDailyChange = null;
        BigDecimal vixDailyReturn = null;
        BigDecimal vixMonthToDateReturn = null;
        BigDecimal vixYearToDateReturn = null;
        BigDecimal vixOneYearVolatility = null;
        BigDecimal sp500DailyChange = null;
        BigDecimal sp500DailyReturn = null;
        BigDecimal correlationWithSp500 = null;
        List<StockIndicatorItem> vixHistory = new ArrayList<>();
        List<StockIndicatorItem> sp500History = new ArrayList<>();

        // 解析历史数据
        JsonNode levelComparison = root.path("levelComparisonHolder").path("indexLevelForComparison");
        JsonNode vixHistoryData = levelComparison.path("92026376");
        JsonNode sp500HistoryData = levelComparison.path("340");

        for (JsonNode item : vixHistoryData) {
            long effectiveDate = item.path("effectiveDate").asLong();
            BigDecimal indexValue = NumberUtils.scale(new BigDecimal(item.path("indexValue").asText()));
            
            String date = Instant.ofEpochMilli(effectiveDate)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            vixHistory.add(new StockIndicatorItem(date, indexValue));
        }
        vixHistory.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        for (JsonNode item : sp500HistoryData) {
            long effectiveDate = item.path("effectiveDate").asLong();
            BigDecimal indexValue = NumberUtils.scale(new BigDecimal(item.path("indexValue").asText()));
            
            String date = Instant.ofEpochMilli(effectiveDate)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            sp500History.add(new StockIndicatorItem(date, indexValue));
        }
        sp500History.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        // 解析相关系数
        JsonNode correlationMatrix = root.path("indexCorrelationHolder").path("indexCorrelationMatrix");
        for (JsonNode item : correlationMatrix) {
            if (item.path("indexId_1").asInt() == 340 && item.path("indexId_2").asInt() == 92026376) {
                correlationWithSp500 = NumberUtils.scale(new BigDecimal(item.path("correlationValue").asText()));
            }
        }

        // 解析指标数据
        for (JsonNode item : indexPerformance) {
            String indexCode = item.path("indexCode").asText();
            long effectiveDate = item.path("effectiveDate").asLong();
            BigDecimal indexValue = new BigDecimal(item.path("indexValue").asText());
            // 保留三位小数并截断，避免进位。
            indexValue = NumberUtils.scale(indexValue);

            String date = Instant.ofEpochMilli(effectiveDate)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);

            if ("VIX".equals(indexCode)) {
                vix = new StockIndicatorItem(date, indexValue);
                vixDailyChange = NumberUtils.scale(new BigDecimal(item.path("dailyChange").asText()));
                vixDailyReturn = NumberUtils.scale(new BigDecimal(item.path("dailyReturn").asText()));
                vixMonthToDateReturn = NumberUtils.scale(new BigDecimal(item.path("monthToDateReturn").asText()));
                vixYearToDateReturn = NumberUtils.scale(new BigDecimal(item.path("yearToDateReturn").asText()));
                vixOneYearVolatility = NumberUtils.scale(new BigDecimal(item.path("oneYearSD").asText()));
            } else if ("500".equals(indexCode)) {
                sp500 = new StockIndicatorItem(date, indexValue);
                sp500DailyChange = NumberUtils.scale(new BigDecimal(item.path("dailyChange").asText()));
                sp500DailyReturn = NumberUtils.scale(new BigDecimal(item.path("dailyReturn").asText()));
            }
        }

        VixIndicator vixIndicator = new VixIndicator();
        vixIndicator.setCurrentVix(vix);
        vixIndicator.setCurrentSp500(sp500);
        vixIndicator.setVixDailyChange(vixDailyChange);
        vixIndicator.setVixDailyReturn(vixDailyReturn);
        vixIndicator.setVixMonthToDateReturn(vixMonthToDateReturn);
        vixIndicator.setVixYearToDateReturn(vixYearToDateReturn);
        vixIndicator.setVixOneYearVolatility(vixOneYearVolatility);
        vixIndicator.setSp500DailyChange(sp500DailyChange);
        vixIndicator.setSp500DailyReturn(sp500DailyReturn);
        vixIndicator.setCorrelationWithSp500(correlationWithSp500);

        // 历史数据由远到近排序 按日期ASC排序
        vixIndicator.setSp500History(sp500History);
        vixIndicator.setVixHistory(vixHistory);

        return vixIndicator;
    }
}
