package me.dingtou.options.util;

import me.dingtou.options.model.IndicatorDataFrame;
import me.dingtou.options.model.StockIndicator;

/**
 * 指标数据框架工具类
 *
 * @author qiyan
 */
public class IndicatorDataFrameUtil {

    /**
     * 从StockIndicator创建IndicatorDataFrame
     *
     * @param stockIndicator 股票指标
     * @return IndicatorDataFrame
     */
    public static IndicatorDataFrame createDataFrame(StockIndicator stockIndicator) {
        return IndicatorDataFrame.fromStockIndicator(stockIndicator);
    }

    /**
     * 从StockIndicator创建Markdown表格
     *
     * @param stockIndicator 股票指标
     * @return Markdown表格字符串
     */
    public static String createMarkdownTable(StockIndicator stockIndicator) {
        IndicatorDataFrame dataFrame = IndicatorDataFrame.fromStockIndicator(stockIndicator);
        return dataFrame.toMarkdown();
    }

    /**
     * 从StockIndicator创建普通表格字符串
     *
     * @param stockIndicator 股票指标
     * @return 表格字符串
     */
    public static String createTableString(StockIndicator stockIndicator) {
        IndicatorDataFrame dataFrame = IndicatorDataFrame.fromStockIndicator(stockIndicator);
        return dataFrame.toString();
    }
    
    /**
     * 从StockIndicator创建IndicatorDataFrame，并限制行数
     *
     * @param stockIndicator 股票指标
     * @param maxRows 最大行数
     * @return IndicatorDataFrame
     */
    public static IndicatorDataFrame createDataFrame(StockIndicator stockIndicator, int maxRows) {
        IndicatorDataFrame dataFrame = IndicatorDataFrame.fromStockIndicator(stockIndicator);
        if (dataFrame.getRowCount() > maxRows) {
            dataFrame.setRows(dataFrame.getRows().subList(0, maxRows));
        }
        return dataFrame;
    }
    
    /**
     * 从StockIndicator创建Markdown表格，并限制行数
     *
     * @param stockIndicator 股票指标
     * @param maxRows 最大行数
     * @return Markdown表格字符串
     */
    public static String createMarkdownTable(StockIndicator stockIndicator, int maxRows) {
        IndicatorDataFrame dataFrame = createDataFrame(stockIndicator, maxRows);
        return dataFrame.toMarkdown();
    }
} 