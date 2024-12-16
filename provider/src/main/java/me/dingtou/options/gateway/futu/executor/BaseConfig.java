package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.FTAPI;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 富途基础配置
 *
 * @author qiyan
 */
@Slf4j
public class BaseConfig {

    protected static final String FU_TU_API_IP;
    protected static final int FU_TU_API_PORT;
    protected static final String FU_TU_API_PRIVATE_KEY;
    protected static final String FU_TU_TRADE_PWD_MD5;

    static {
        try {
            // 获取用户主目录
            String userHome = System.getProperty("user.home");
            String fileSeparator = FileSystems.getDefault().getSeparator();
            String configDir = userHome + fileSeparator + ".options-trade" + fileSeparator;
            Path configDirPath = Paths.get(configDir);
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
            }
            Properties properties = new Properties();
            String configFile = configDir + "config.properties";
            if (!Files.exists(Paths.get(configFile))) {
                throw new RuntimeException(configFile + " not exists");
            }
            try (FileInputStream fis = new FileInputStream(configFile)) {
                // 加载properties文件
                properties.load(fis);

                // 读取属性值
                String apiIp = properties.getProperty("futu.api.ip");
                String apiPort = properties.getProperty("futu.api.port");
                String apiUnlockPasswordMd5 = properties.getProperty("futu.api.unlock");

                if (apiIp == null || apiPort == null || apiUnlockPasswordMd5 == null) {
                    throw new RuntimeException(configFile + " futu.api.ip or futu.api.port or futu.api.unlock is null");
                }

                FU_TU_API_IP = apiIp;
                FU_TU_API_PORT = Integer.parseInt(apiPort);
                FU_TU_TRADE_PWD_MD5 = apiUnlockPasswordMd5;
            }


            String rasPrivateKey = configDir + "futu_rsa_private.key";
            Path rasPrivateKeyPath = Paths.get(rasPrivateKey);
            if (!Files.exists(rasPrivateKeyPath)) {
                throw new RuntimeException(rasPrivateKey + " not exists");
            }
            byte[] buf = Files.readAllBytes(rasPrivateKeyPath);
            FU_TU_API_PRIVATE_KEY = new String(buf, StandardCharsets.UTF_8);

            FTAPI.init();
        } catch (Exception e) {
            throw new RuntimeException("load config error (参考README.md初始化配置)", e);
        }
    }

    public static void init() {
        log.warn("load config succeed. ip:{} port:{}", FU_TU_API_IP, FU_TU_API_PORT);
    }

}
