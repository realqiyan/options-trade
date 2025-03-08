package me.dingtou.options.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * 指标数据框架，类似DataFrame的结构
 * 用于将stockIndicator.getIndicatorMap()里的指标按照日期连接起来
 *
 * @author qiyan
 */
@Data
public class IndicatorDataFrame {

    /**
     * 列名列表，第一列是日期，其余列是指标名
     */
    private List<String> columns = new ArrayList<>();

    /**
     * 数据行，每行是一个Map，key是列名，value是对应的值
     */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /**
     * 构造函数
     */
    public IndicatorDataFrame() {
        // 默认第一列是日期
        columns.add("date");
    }

    /**
     * 从StockIndicator构建IndicatorDataFrame
     *
     * @param stockIndicator 股票指标
     * @return IndicatorDataFrame
     */
    public static IndicatorDataFrame fromStockIndicator(StockIndicator stockIndicator) {
        IndicatorDataFrame dataFrame = new IndicatorDataFrame();
        
        // 获取所有日期
        Set<String> allDates = new HashSet<>();
        Map<String, List<StockIndicatorItem>> indicatorMap = stockIndicator.getIndicatorMap();
        
        // 收集所有日期
        for (Map.Entry<String, List<StockIndicatorItem>> entry : indicatorMap.entrySet()) {
            List<StockIndicatorItem> items = entry.getValue();
            for (StockIndicatorItem item : items) {
                allDates.add(item.getDate());
            }
        }
        
        // 按日期排序（降序，最新日期在前）
        List<String> sortedDates = new ArrayList<>(allDates);
        Collections.sort(sortedDates, Collections.reverseOrder());
        
        // 为每个日期创建一行数据
        for (String date : sortedDates) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", date);
            dataFrame.getRows().add(row);
        }
        
        // 添加每个指标的数据
        for (Map.Entry<String, List<StockIndicatorItem>> entry : indicatorMap.entrySet()) {
            String indicatorKey = entry.getKey();
            List<StockIndicatorItem> items = entry.getValue();
            
            // 添加列名
            dataFrame.getColumns().add(indicatorKey);
            
            // 创建日期到值的映射
            Map<String, BigDecimal> dateToValue = new HashMap<>();
            for (StockIndicatorItem item : items) {
                dateToValue.put(item.getDate(), item.getValue());
            }
            
            // 填充每一行的指标值
            for (Map<String, Object> row : dataFrame.getRows()) {
                String rowDate = (String) row.get("date");
                BigDecimal value = dateToValue.get(rowDate);
                row.put(indicatorKey, value); // 如果没有值，会是null
            }
        }
        
        return dataFrame;
    }
    
    /**
     * 转换为字符串表格
     *
     * @return 表格字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // 表头
        for (String column : columns) {
            sb.append(column).append("\t");
        }
        sb.append("\n");
        
        // 数据行
        for (Map<String, Object> row : rows) {
            for (String column : columns) {
                Object value = row.get(column);
                sb.append(value == null ? "" : value).append("\t");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 转换为Markdown表格
     *
     * @return Markdown表格字符串
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        
        // 表头
        sb.append("| ");
        for (String column : columns) {
            sb.append(column).append(" | ");
        }
        sb.append("\n");
        
        // 分隔行
        sb.append("| ");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("--- | ");
        }
        sb.append("\n");
        
        // 数据行
        for (Map<String, Object> row : rows) {
            sb.append("| ");
            for (String column : columns) {
                Object value = row.get(column);
                sb.append(value == null ? "" : value).append(" | ");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取指定行和列的值
     *
     * @param rowIndex 行索引
     * @param column 列名
     * @return 值
     */
    public Object getValue(int rowIndex, String column) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex).get(column);
    }
    
    /**
     * 获取行数
     *
     * @return 行数
     */
    public int getRowCount() {
        return rows.size();
    }
    
    /**
     * 获取列数
     *
     * @return 列数
     */
    public int getColumnCount() {
        return columns.size();
    }
} 