package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.FTAPI;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.config.ConfigUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 富途基础配置
 *
 * @author qiyan
 */
@Component
@Slf4j
public class BaseConfig implements InitializingBean {

    protected static final String FU_TU_API_IP;
    protected static final int FU_TU_API_PORT;
    protected static final String FU_TU_API_PRIVATE_KEY;
    protected static final String FU_TU_TRADE_PWD_MD5;

    static {
        try {

            // 读取属性值
            String apiIp = ConfigUtils.getConfig("futu.api.ip");
            String apiPort = ConfigUtils.getConfig("futu.api.port");
            String apiUnlockPasswordMd5 = ConfigUtils.getConfig("futu.api.unlock");

            if (apiIp == null || apiPort == null) {
                throw new RuntimeException("futu.api.ip or futu.api.port is null");
            }

            FU_TU_API_IP = apiIp;
            FU_TU_API_PORT = Integer.parseInt(apiPort);
            FU_TU_TRADE_PWD_MD5 = apiUnlockPasswordMd5;

            String rasPrivateKey = ConfigUtils.getConfigDir() + "futu_rsa_private.key";
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

    public void init() {
        log.warn("load config succeed. ip:{} port:{}", FU_TU_API_IP, FU_TU_API_PORT);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

}
