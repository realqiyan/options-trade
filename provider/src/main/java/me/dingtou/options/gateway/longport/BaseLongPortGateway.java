package me.dingtou.options.gateway.longport;

import com.longport.Config;
import com.longport.ConfigBuilder;
import com.longport.quote.QuoteContext;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.util.AccountExtUtils;
import me.dingtou.options.util.ExceptionUtils;

@Slf4j
public class BaseLongPortGateway {

    private Config config;
    private QuoteContext quoteContext;

    protected QuoteContext getQuoteContext(OwnerAccount ownerAccount, boolean refresh) {
        try {
            if (null == config) {
                config = new ConfigBuilder(AccountExtUtils.getLongportAppKey(ownerAccount),
                        AccountExtUtils.getLongportAppSecret(ownerAccount),
                        AccountExtUtils.getLongportAccessToken(ownerAccount)).build();
            }
            if (refresh) {
                quoteContext = QuoteContext.create(config).get();
            }
            if (null == quoteContext) {
                quoteContext = QuoteContext.create(config).get();
            }
        } catch (Throwable e) {
            log.error("init longport_java error. message:{}", e.getMessage());
            ExceptionUtils.throwRuntimeException(e);
        }
        return quoteContext;
    }

}
