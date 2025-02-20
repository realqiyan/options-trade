package me.dingtou.options.gateway.longport;

import com.longport.OpenApiException;
import com.longport.quote.*;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class SecurityQuoteGatewayImpl extends BaseLongPortGateway implements SecurityQuoteGateway {


    @Override
    public SecurityQuote quote(Security security) {
        List<Security> securityList = new ArrayList<>(1);
        securityList.add(security);
        List<SecurityQuote> quoteList = quote(securityList);
        if (null == quoteList || quoteList.isEmpty()) {
            SecurityQuote securityQuote = new SecurityQuote();
            securityQuote.setSecurity(security);
            securityQuote.setLastDone(BigDecimal.ZERO);
            return securityQuote;
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
            QuoteContext ctx = getQuoteContext(false);
            if (null == ctx) {
                return Collections.emptyList();
            }
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
            getQuoteContext(true);
            throw new RuntimeException(e);
        }

        return quoteList;
    }

    @Override
    public void subscribeQuote(List<Security> security, Function<SecurityQuote, Void> callback) {
        try {
            if (null == security || null == callback) {
                return;
            }
            Set<String> securitySet = security.stream().map(Security::toString).collect(Collectors.toSet());
            QuoteContext ctx = getQuoteContext(false);
            CompletableFuture<Subscription[]> historySubscriptions = ctx.getSubscrptions();
            Subscription[] subscriptions = historySubscriptions.get();
            for (Subscription subscription : subscriptions) {
                securitySet.remove(subscription.getSymbol());
            }
            ctx.subscribe(securitySet.toArray(String[]::new), SubFlags.Quote, true);

            // 处理订阅回调
            ctx.setOnQuote((symbol, event) -> {
                SecurityQuote securityQuote = new SecurityQuote();
                securityQuote.setSecurity(Security.from(symbol));
                securityQuote.setLastDone(event.getLastDone());
                callback.apply(securityQuote);
            });
        } catch (Exception e) {
            getQuoteContext(true);
            log.error("subscribeQuote error. message:{}", e.getMessage(), e);
        }
    }

    private SecurityQuote convertSecurityQuote(com.longport.quote.SecurityQuote securityQuote) {
        SecurityQuote innerSecurityQuote = new SecurityQuote();
        innerSecurityQuote.setSecurity(Security.from(securityQuote.getSymbol()));
        innerSecurityQuote.setLastDone(securityQuote.getLastDone());
        return innerSecurityQuote;
    }
}
