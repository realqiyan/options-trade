package me.dingtou.options.config;

import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 配置文件工具类
 *
 * @author qiyan
 */
public class ConfigUtils {

    private static final Properties PROPERTIES = new Properties();
    private static final String CONFIG_DIR;

    static {
        try {
            // 获取用户主目录
            String userHome = System.getProperty("user.home");
            String fileSeparator = FileSystems.getDefault().getSeparator();
            CONFIG_DIR = userHome + fileSeparator + ".options-trade" + fileSeparator;
            Path configDirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
            }
            String configFile = CONFIG_DIR + "config.properties";
            if (!Files.exists(Paths.get(configFile))) {
                throw new RuntimeException(configFile + " not exists");
            }
            try (FileInputStream fis = new FileInputStream(configFile)) {
                // 加载properties文件
                PROPERTIES.load(fis);
            }

        } catch (Exception e) {
            throw new RuntimeException("load config error (参考README.md初始化配置)", e);
        }
    }

    public static String getConfig(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getConfigDir() {
        return CONFIG_DIR;
    }
}
