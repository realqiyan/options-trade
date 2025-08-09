package me.dingtou.options.util;

import org.apache.commons.text.StringEscapeUtils;

/**
 * 字符串转义工具
 */
public class EscapeUtils {

    /**
     * HTML转义
     * 
     * @param input
     * @return
     */
    public static String escapeHtml(String input) {
        if (input == null || input.isEmpty())
            return input;
        return StringEscapeUtils.escapeHtml4(input);
    }

    /**
     * xml转义
     * 
     * @param input
     * @return
     */
    public static String escapeXml(String input) {
        if (input == null || input.isEmpty())
            return input;
        return StringEscapeUtils.escapeXml11(input);
    }

}
