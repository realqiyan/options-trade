package me.dingtou.options.util;

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
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");
    }

}
