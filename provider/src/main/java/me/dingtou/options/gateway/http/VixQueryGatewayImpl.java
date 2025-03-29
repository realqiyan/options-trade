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
            } else if ("500".equals(indexCode)) {
                sp500 = new StockIndicatorItem(date, indexValue);
            }
        }

        VixIndicator vixIndicator = new VixIndicator();
        vixIndicator.setCurrentVix(vix);
        vixIndicator.setCurrentSp500(sp500);

        return vixIndicator;
    }
}
