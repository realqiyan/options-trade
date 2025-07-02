package me.dingtou.options.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类
 */
public class DateUtils {

    /**
     * 获取当前时间
     * 
     * @return 当前时间 yyyy-MM-dd HH:mm:ss
     */
    public static String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

}
