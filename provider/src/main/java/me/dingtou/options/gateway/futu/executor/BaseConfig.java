package me.dingtou.options.gateway.futu.executor;

import com.futu.openapi.FTAPI;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * 富途基础配置
 *
 * @author qiyan
 */
public class BaseConfig {

    protected static final String FU_TU_API_IP = System.getProperty("fuTuApiIp", "10.0.12.160");
    protected static final String FU_TU_API_PORT_CFG = System.getProperty("fuTuApiPort", "18888");
    protected static final int FU_TU_API_PORT;
    protected static final String FU_TU_API_PRIVATE_KEY;
    protected static final String PWD_MD5;

    static {
        try {
            FU_TU_API_PORT = Integer.parseInt(FU_TU_API_PORT_CFG);

            {
                URI uri = Objects.requireNonNull(SingleQueryExecutor.class.getResource("/key/private.key")).toURI();
                byte[] buf = Files.readAllBytes(Paths.get(uri));
                FU_TU_API_PRIVATE_KEY = new String(buf, StandardCharsets.UTF_8);
            }

            {
                URI uri = Objects.requireNonNull(SingleQueryExecutor.class.getResource("/key/trade.key")).toURI();
                byte[] buf = Files.readAllBytes(Paths.get(uri));
                PWD_MD5 = new String(buf, StandardCharsets.UTF_8);
            }

            FTAPI.init();
        } catch (Exception e) {
            throw new RuntimeException("init SingleQueryExecutor error", e);
        }
    }

}
