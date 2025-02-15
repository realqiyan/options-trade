package me.dingtou.options.gateway.longport;

import com.longport.Config;
import com.longport.ConfigBuilder;
import com.longport.quote.QuoteContext;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.config.ConfigUtils;

@Slf4j
public class BaseLongPortGateway {

    private static Config LONGPORT_CONFIG = null;

    static {
        try {
            // Init config without ENV
            // https://longportapp.github.io/openapi-sdk/java/com/longport/ConfigBuilder.html
            // 读取属性值
            String appKey = ConfigUtils.getConfig("longport.app.key");
            String appSecret = ConfigUtils.getConfig("longport.app.secret");
            String accessToken = ConfigUtils.getConfig("longport.access.token");
            if (null != appKey && null != appSecret && null != accessToken) {
                LONGPORT_CONFIG = new ConfigBuilder(appKey, appSecret, accessToken).build();
            } else {
                LONGPORT_CONFIG = Config.fromEnv();
            }
        } catch (Throwable e) {
            log.error("init longport_java error. message:{}", e.getMessage());
        }
    }

    private QuoteContext quoteContext;

    protected QuoteContext getQuoteContext(boolean refresh) {
        try {
            if (refresh) {
                quoteContext = QuoteContext.create(LONGPORT_CONFIG).get();
            }
            if (null == quoteContext) {
                quoteContext = QuoteContext.create(LONGPORT_CONFIG).get();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return quoteContext;
    }

}
