package me.dingtou.options.gateway.longport;

import com.longport.Config;
import com.longport.ConfigBuilder;
import com.longport.OpenApiException;
import com.longport.quote.QuoteContext;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.ExceptionUtils;

@Slf4j
public class BaseLongPortGateway {

    private static QuoteContext quoteContext;
    private static QuoteContext subscribeQuoteContext;

    protected synchronized static QuoteContext getSubscribeQuoteContext(OwnerAccount ownerAccount) {
        try {
            if (null != subscribeQuoteContext) {
                return subscribeQuoteContext;
            }
            Config config = buildConfig(ownerAccount);
            subscribeQuoteContext = QuoteContext.create(config).get();
        } catch (Throwable e) {
            log.error("getSubscribeQuoteContext init longport_java error. message:{}", e.getMessage());
            ExceptionUtils.throwRuntimeException(e);
        }
        return subscribeQuoteContext;
    }

    private static Config buildConfig(OwnerAccount ownerAccount) throws OpenApiException {
        String appKey = AccountExtUtils.getLongportAppKey(ownerAccount);
        String appSecret = AccountExtUtils.getLongportAppSecret(ownerAccount);
        String accessToken = AccountExtUtils.getLongportAccessToken(ownerAccount);
        Config config = new ConfigBuilder(appKey, appSecret, accessToken).build();
        return config;
    }

    protected synchronized static QuoteContext getQuoteContext(OwnerAccount ownerAccount, boolean refresh) {
        try {
            if (null != quoteContext && !refresh) {
                return quoteContext;
            }
            if (null != quoteContext) {
                quoteContext.close();
                quoteContext = null;
            }
            Config config = buildConfig(ownerAccount);
            quoteContext = QuoteContext.create(config).get();
            if (null == quoteContext) {
                quoteContext = QuoteContext.create(config).get();
            }
        } catch (Throwable e) {
            log.error("getQuoteContext init longport_java error. message:{}", e.getMessage());
            if (null != quoteContext) {
                try {
                    quoteContext.close();
                    quoteContext = null;
                } catch (Exception ex) {
                    log.error("QuoteContext close error. message:{}", ex.getMessage());
                }
            }
            ExceptionUtils.throwRuntimeException(e);
        }
        return quoteContext;
    }

}
