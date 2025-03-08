package me.dingtou.options.util;

import me.dingtou.options.constant.IndicatorKey;
import me.dingtou.options.model.IndicatorDataFrame;
import me.dingtou.options.model.StockIndicator;
import me.dingtou.options.model.StockIndicatorItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 指标数据框架示例
 *
 * @author qiyan
 */
public class IndicatorDataFrameTest {

    /**
     * 示例方法
     */
    public static void main(String[] args) {
        // 创建示例数据
        StockIndicator stockIndicator = createSampleStockIndicator();
        
        // 使用工具类创建DataFrame
        IndicatorDataFrame dataFrame = IndicatorDataFrameUtil.createDataFrame(stockIndicator);
        
        // 打印DataFrame
        System.out.println("DataFrame 表格输出:");
        System.out.println(dataFrame.toString());
        
        // 打印Markdown表格
        System.out.println("\nMarkdown 表格输出:");
        System.out.println(IndicatorDataFrameUtil.createMarkdownTable(stockIndicator));
        
        // 限制行数
        System.out.println("\n限制行数的 Markdown 表格输出 (最多3行):");
        System.out.println(IndicatorDataFrameUtil.createMarkdownTable(stockIndicator, 3));
    }
    
    /**
     * 创建示例StockIndicator数据
     *
     * @return StockIndicator
     */
    private static StockIndicator createSampleStockIndicator() {
        StockIndicator stockIndicator = new StockIndicator();
        
        // 添加RSI指标
        List<StockIndicatorItem> rsiItems = new ArrayList<>();
        rsiItems.add(new StockIndicatorItem("2023-05-01", new BigDecimal("65.23")));
        rsiItems.add(new StockIndicatorItem("2023-05-02", new BigDecimal("67.45")));
        rsiItems.add(new StockIndicatorItem("2023-05-03", new BigDecimal("70.12")));
        rsiItems.add(new StockIndicatorItem("2023-05-04", new BigDecimal("68.78")));
        rsiItems.add(new StockIndicatorItem("2023-05-05", new BigDecimal("64.32")));
        stockIndicator.addIndicator(IndicatorKey.RSI.getKey(), rsiItems);
        
        // 添加MACD指标
        List<StockIndicatorItem> macdItems = new ArrayList<>();
        macdItems.add(new StockIndicatorItem("2023-05-01", new BigDecimal("1.23")));
        macdItems.add(new StockIndicatorItem("2023-05-02", new BigDecimal("1.45")));
        macdItems.add(new StockIndicatorItem("2023-05-03", new BigDecimal("1.67")));
        macdItems.add(new StockIndicatorItem("2023-05-04", new BigDecimal("1.89")));
        macdItems.add(new StockIndicatorItem("2023-05-05", new BigDecimal("1.56")));
        stockIndicator.addIndicator(IndicatorKey.MACD.getKey(), macdItems);
        
        // 添加EMA5指标
        List<StockIndicatorItem> ema5Items = new ArrayList<>();
        ema5Items.add(new StockIndicatorItem("2023-05-01", new BigDecimal("150.23")));
        ema5Items.add(new StockIndicatorItem("2023-05-02", new BigDecimal("152.45")));
        ema5Items.add(new StockIndicatorItem("2023-05-03", new BigDecimal("155.67")));
        ema5Items.add(new StockIndicatorItem("2023-05-04", new BigDecimal("153.89")));
        // 注意：这里故意少一个日期，演示left join的效果
        stockIndicator.addIndicator(IndicatorKey.EMA5.getKey(), ema5Items);
        
        // 添加EMA50指标
        List<StockIndicatorItem> ema50Items = new ArrayList<>();
        ema50Items.add(new StockIndicatorItem("2023-05-01", new BigDecimal("145.23")));
        ema50Items.add(new StockIndicatorItem("2023-05-02", new BigDecimal("146.45")));
        ema50Items.add(new StockIndicatorItem("2023-05-03", new BigDecimal("147.67")));
        // 注意：这里故意少两个日期，演示left join的效果
        stockIndicator.addIndicator(IndicatorKey.EMA50.getKey(), ema50Items);
        
        return stockIndicator;
    }
} 