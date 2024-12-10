package me.dingtou.options.gateway.longport;

import com.longport.Config;
import com.longport.OpenApiException;
import com.longport.quote.QuoteContext;
import com.longport.quote.SubFlags;
import me.dingtou.options.gateway.SecurityGateway;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class SecurityGatewayImpl implements SecurityGateway {
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
        try (Config config = Config.fromEnv(); QuoteContext ctx = QuoteContext.create(config).get()) {

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
