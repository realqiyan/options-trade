package me.dingtou.options.gateway.http;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.EarningsCalendarGateway;
import me.dingtou.options.model.EarningsCalendar;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 财报日历网关实现类
 */
@Component
@Slf4j
public class EarningsCalendarGatewayImpl implements EarningsCalendarGateway {

    // NASDAQ财报日历API URL模板
    private static final String EARNINGS_CALENDAR_URL = "https://api.nasdaq.com/api/calendar/earnings?date=%s";

    // 日期格式化器
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // 例如：Thu, Sep 4, 2025、Tue, Aug 12, 2025
    private static final SimpleDateFormat AS_OF_DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);

    /**
     * 根据日期获取财报日历数据
     * 
     * @param date 指定日期
     * @return 财报日历列表
     */
    @Override
    public List<EarningsCalendar> getEarningsCalendarByDate(Date date) {
        try {
            // 构造API URL
            String dateStr = DATE_FORMAT.format(date);
            String urlStr = String.format(EARNINGS_CALENDAR_URL, dateStr);

            log.info("开始获取NASDAQ财报日历数据, 日期: {}", dateStr);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .header("accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("accept-language", "zh-CN,zh;q=0.9")
                    .header("dnt", "1")
                    .header("priority", "u=0, i")
                    .header("sec-ch-ua",
                            "\"Not(A:Brand\";v=\"99\", \"Microsoft Edge\";v=\"133\", \"Chromium\";v=\"133\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"macOS\"")
                    .header("sec-fetch-dest", "document")
                    .header("sec-fetch-mode", "navigate")
                    .header("sec-fetch-site", "none")
                    .header("sec-fetch-user", "?1")
                    .header("upgrade-insecure-requests", "1")
                    .header("user-agent",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("查询NASDAQ API失败,statusCode:{}", response.statusCode());
                return Collections.emptyList();
            }

            String body = response.body();

            log.info("成功接收NASDAQ API响应, date:{} body: {}", dateStr, body);

            // 解析JSON响应
            JSONObject jsonResponse = JSON.parseObject(body);
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data != null) {

                // Thu, Sep 4, 2025
                String asOf = data.getString("asOf");
                Date earningsDate = AS_OF_DATE_FORMAT.parse(asOf);

                if (!dateStr.equals(DATE_FORMAT.format(earningsDate))) {
                    log.warn("财报日期与请求日期不一致, 以财报日期为准, earningsDate: {}, requestDate: {}",
                            DATE_FORMAT.format(earningsDate),
                            dateStr);
                }

                List<EarningsCalendar> earningsCalendars = new ArrayList<>();

                JSONArray jsonArray = data.getJSONArray("rows");
                if (null == jsonArray) {
                    return Collections.emptyList();
                }
                // 遍历rows数组
                List<JSONObject> rows = jsonArray.toJavaList(JSONObject.class);

                log.info("开始解析财报日历数据, 数据条数: {}", rows.size());

                for (JSONObject row : rows) {
                    EarningsCalendar calendar = new EarningsCalendar();
                    calendar.setSymbol(row.getString("symbol"));
                    calendar.setName(row.getString("name"));
                    calendar.setMarketCap(parseMarketCap(row.getString("marketCap")));
                    calendar.setFiscalQuarterEnding(row.getString("fiscalQuarterEnding"));
                    calendar.setEpsForecast(parseEps(row.getString("epsForecast")));
                    calendar.setNoOfEsts(parseNoOfEsts(row.getString("noOfEsts")));
                    calendar.setLastYearRptDt(row.getString("lastYearRptDt"));
                    calendar.setLastYearEps(parseEps(row.getString("lastYearEPS")));
                    calendar.setTime(row.getString("time"));
                    calendar.setEarningsDate(earningsDate);

                    earningsCalendars.add(calendar);
                }

                log.info("财报日历数据解析完成, 返回条数: {}", earningsCalendars.size());
                return earningsCalendars;
            } else {
                log.warn("NASDAQ API响应中未包含有效数据");
            }
        } catch (Exception e) {
            log.error("获取NASDAQ财报日历数据时发生异常", e);
        }

        // 出现异常时返回空列表
        return new ArrayList<>();
    }

    /**
     * 解析NASDAQ返回的估计报告次数字符串
     * 
     * @param noOfEstsStr 估计报告次数字符串
     * @return 估计报告次数
     */
    private Integer parseNoOfEsts(String noOfEstsStr) {
        if (StringUtils.isNumeric(noOfEstsStr)) {
            return Integer.parseInt(noOfEstsStr);
        }
        return 0;
    }

    /**
     * 解析NASDAQ返回的市值字符串
     * 
     * @param marketCapStr 市值字符串
     * @return BigDecimal类型的市值
     */
    private BigDecimal parseMarketCap(String marketCapStr) {
        if (marketCapStr == null || marketCapStr.isEmpty()) {
            return null;
        }

        // 移除美元符号和逗号
        String cleanedStr = marketCapStr.replace("$", "").replace(",", "");

        try {
            BigDecimal result = new BigDecimal(cleanedStr);
            log.debug("市值解析成功: {} -> {}", marketCapStr, result);
            return result;
        } catch (NumberFormatException e) {
            log.warn("市值解析失败: {}", marketCapStr, e);
            return null;
        }
    }

    /**
     * 解析NASDAQ返回的EPS字符串
     * 
     * @param epsStr EPS字符串
     * @return BigDecimal类型的EPS
     */
    private BigDecimal parseEps(String epsStr) {
        if (epsStr == null || epsStr.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 移除美元符号
        String cleanedStr = epsStr.replace("$", "")
                .replace("(", "")
                .replace(")", "");

        try {
            if ("N/A".equalsIgnoreCase(cleanedStr)) {
                return BigDecimal.ZERO;
            }
            BigDecimal result = new BigDecimal(cleanedStr);
            log.debug("EPS解析成功: {} -> {}", epsStr, result);
            return result;
        } catch (NumberFormatException e) {
            log.warn("EPS解析失败: {}", epsStr, e);
            return null;
        }
    }

}