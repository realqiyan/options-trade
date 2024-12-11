package me.dingtou.options.gateway.longport;

import com.longport.Config;
import com.longport.OpenApiException;
import com.longport.quote.QuoteContext;
import me.dingtou.options.gateway.SecurityGateway;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class SecurityGatewayImpl implements SecurityGateway {

    private final static Config LONGPORT_CONFIG;

    static {
        try {
            LONGPORT_CONFIG = Config.fromEnv();
        } catch (OpenApiException e) {
            throw new RuntimeException(e);
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
        if (null == quoteList) {
            return null;
        }
        return quoteList.iterator().next();
    }

    @Override
    public List<SecurityQuote> quote(List<Security> securityList) {

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
