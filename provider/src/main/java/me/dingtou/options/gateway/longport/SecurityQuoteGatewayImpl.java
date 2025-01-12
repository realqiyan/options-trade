package me.dingtou.options.gateway.longport;

import com.longport.Config;
import com.longport.ConfigBuilder;
import com.longport.OpenApiException;
import com.longport.quote.QuoteContext;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.config.ConfigUtils;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SecurityQuoteGatewayImpl implements SecurityQuoteGateway {

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

    private QuoteContext getCtx(boolean refresh) {
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

    @Override
    public SecurityQuote quote(Security security) {
        List<Security> securityList = new ArrayList<>(1);
        securityList.add(security);
        List<SecurityQuote> quoteList = quote(securityList);
        if (null == quoteList || quoteList.isEmpty()) {
            SecurityQuote securityQuote = new SecurityQuote();
            securityQuote.setSecurity(security);
            return securityQuote;
        }
        return quoteList.iterator().next();
    }

    @Override
    public List<SecurityQuote> quote(List<Security> securityList) {
        if (null == LONGPORT_CONFIG) {
            return Collections.emptyList();
        }
        if (null == securityList || securityList.isEmpty()) {
            return Collections.emptyList();
        }
        List<SecurityQuote> quoteList = new ArrayList<>(securityList.size());
        try {
            QuoteContext ctx = getCtx(false);
            String[] symbols = new String[securityList.size()];
            for (int i = 0; i < securityList.size(); i++) {
                Security security = securityList.get(i);
                symbols[i] = security.toString();
            }
            CompletableFuture<com.longport.quote.SecurityQuote[]> ctxQuote = ctx.getQuote(symbols);
            com.longport.quote.SecurityQuote[] securityQuotes = ctxQuote.get(10, TimeUnit.SECONDS);

            for (com.longport.quote.SecurityQuote securityQuote : securityQuotes) {
                if (null == securityQuote) {
                    continue;
                }
                quoteList.add(convertSecurityQuote(securityQuote));

            }
        } catch (Exception e) {
            getCtx(true);
            throw new RuntimeException(e);
        }

        return quoteList;
    }

    private SecurityQuote convertSecurityQuote(com.longport.quote.SecurityQuote securityQuote) {
        SecurityQuote innerSecurityQuote = new SecurityQuote();
        innerSecurityQuote.setSecurity(Security.from(securityQuote.getSymbol()));
        innerSecurityQuote.setLastDone(securityQuote.getLastDone());
        return innerSecurityQuote;
    }
}
