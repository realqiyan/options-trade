package me.dingtou.options.gateway.longport;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.longport.quote.QuoteContext;
import com.longport.quote.SubFlags;
import com.longport.quote.Subscription;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.SecurityQuoteGateway;
import me.dingtou.options.model.OwnerAccount;
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

@Component
@Slf4j
public class SecurityQuoteGatewayImpl extends BaseLongPortGateway implements SecurityQuoteGateway {

    /**
     * 价格实时缓存
     */
    private static final Cache<Security, SecurityQuote> SECURITY_QUOTE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .build();

    @Override
    public SecurityQuote quote(OwnerAccount ownerAccount, Security security) {
        List<Security> securityList = new ArrayList<>(1);
        securityList.add(security);
        List<SecurityQuote> quoteList = quote(ownerAccount, securityList);
        if (null == quoteList || quoteList.isEmpty()) {
            SecurityQuote securityQuote = new SecurityQuote();
            securityQuote.setSecurity(security);
            securityQuote.setLastDone(BigDecimal.ZERO);
            return securityQuote;
        }
        return quoteList.iterator().next();
    }

    @Override
    public List<SecurityQuote> quote(OwnerAccount ownerAccount, List<Security> securityList) {
        if (null == securityList || securityList.isEmpty()) {
            return Collections.emptyList();
        }
        List<SecurityQuote> quoteList = new ArrayList<>(securityList.size());
        try {
            QuoteContext ctx = getQuoteContext(ownerAccount, false);
            if (null == ctx) {
                return Collections.emptyList();
            }
            List<String> symbols = new ArrayList<>();
            for (Security security : securityList) {
                SecurityQuote securityQuote = SECURITY_QUOTE_CACHE.getIfPresent(security);
                if (null != securityQuote) {
                    quoteList.add(securityQuote);
                } else {
                    symbols.add(security.toString());
                }
            }
            if (symbols.isEmpty()) {
                return quoteList;
            }
            CompletableFuture<com.longport.quote.SecurityQuote[]> ctxQuote = ctx
                    .getQuote(symbols.toArray(new String[0]));
            com.longport.quote.SecurityQuote[] securityQuotes = ctxQuote.get(10, TimeUnit.SECONDS);

            for (com.longport.quote.SecurityQuote securityQuote : securityQuotes) {
                if (null == securityQuote) {
                    continue;
                }
                SecurityQuote quote = convertSecurityQuote(securityQuote);
                quoteList.add(quote);
                SECURITY_QUOTE_CACHE.put(quote.getSecurity(), quote);
            }
        } catch (Exception e) {
            log.error("quote error. message:{}", e.getMessage(), e);
            try {
                getQuoteContext(ownerAccount, true);
            } catch (Exception exception) {
                log.error("subscribeQuote error. message:{}", exception.getMessage());
            }
        }

        return quoteList;
    }

    @Override
    public void subscribeQuote(OwnerAccount ownerAccount, List<Security> security,
            Function<SecurityQuote, Void> callback) {
        try {
            if (null == security || null == callback) {
                return;
            }
            Set<String> securitySet = security.stream().map(Security::toString).collect(Collectors.toSet());
            QuoteContext ctx = getQuoteContext(ownerAccount, false);
            CompletableFuture<Subscription[]> historySubscriptions = ctx.getSubscrptions();
            Subscription[] subscriptions = historySubscriptions.get();
            for (Subscription subscription : subscriptions) {
                securitySet.remove(subscription.getSymbol());
            }
            ctx.subscribe(securitySet.toArray(String[]::new), SubFlags.Quote, true);

            // 处理订阅回调
            ctx.setOnQuote((symbol, event) -> {
                SecurityQuote securityQuote = new SecurityQuote();
                Security currSecurity = Security.from(symbol);
                securityQuote.setSecurity(currSecurity);
                securityQuote.setLastDone(event.getLastDone());
                SECURITY_QUOTE_CACHE.put(currSecurity, securityQuote);
                callback.apply(securityQuote);
            });
        } catch (Throwable e) {
            log.error("subscribeQuote error. message:{}", e.getMessage(), e);
            try {
                getQuoteContext(ownerAccount, true);
            } catch (Throwable exception) {
                log.error("getQuoteContext error. message:{}", exception.getMessage());
            }
        }
    }

    private SecurityQuote convertSecurityQuote(com.longport.quote.SecurityQuote securityQuote) {
        SecurityQuote innerSecurityQuote = new SecurityQuote();
        innerSecurityQuote.setSecurity(Security.from(securityQuote.getSymbol()));
        innerSecurityQuote.setLastDone(securityQuote.getLastDone());
        return innerSecurityQuote;
    }
}
