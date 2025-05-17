package me.dingtou.options.gateway.longport;

import com.longport.quote.AdjustType;
import com.longport.quote.Period;
import com.longport.quote.QuoteContext;
import com.longport.quote.TradeSessions;

import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.CandlestickAdjustType;
import me.dingtou.options.constant.CandlestickPeriod;
import me.dingtou.options.gateway.CandlestickGateway;
import me.dingtou.options.model.Candlestick;
import me.dingtou.options.model.OwnerAccount;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityCandlestick;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CandlestickGatewayImpl extends BaseLongPortGateway implements CandlestickGateway {
    @Override
    public SecurityCandlestick getCandlesticks(OwnerAccount ownerAccount, Security security, CandlestickPeriod period,
            Integer count, CandlestickAdjustType adjustType) {
        SecurityCandlestick result = new SecurityCandlestick();
        result.setSecurity(security);
        result.setCandlesticks(new ArrayList<>());
        try {
            QuoteContext ctx = getQuoteContext(ownerAccount, false);
            if (null == ctx) {
                return result;
            }
            Period queryPeriod = switch (period) {
                case WEEK -> Period.Week;
                case MONTH -> Period.Month;
                case YEAR -> Period.Year;
                default -> Period.Day;
            };
            AdjustType queryAdjustType = switch (adjustType) {
                case FORWARD_ADJUST -> AdjustType.ForwardAdjust;
                case NO_ADJUST -> AdjustType.NoAdjust;
            };
            CompletableFuture<com.longport.quote.Candlestick[]> completableFuture = ctx
                    .getCandlesticks(security.toString(), queryPeriod, count, queryAdjustType, TradeSessions.All);
            com.longport.quote.Candlestick[] candlesticks = completableFuture.get(10, TimeUnit.SECONDS);
            for (com.longport.quote.Candlestick candlestick : candlesticks) {
                if (null == candlestick) {
                    continue;
                }
                result.getCandlesticks().add(convertCandlestick(candlestick));
            }
        } catch (Exception e) {
            log.error("获取K线数据失败,security:{},message:{}", security.toString(), e.getMessage());
            try {
                getQuoteContext(ownerAccount, true);
            } catch (Exception exception) {
                log.error("获取K线数据再次失败,security:{},message:{}", security.toString(), exception.getMessage());
            }
            return result;
        }

        return result;
    }

    private Candlestick convertCandlestick(com.longport.quote.Candlestick candlestick) {
        Candlestick innerCandlestick = new Candlestick();
        innerCandlestick.setClose(candlestick.getClose());
        innerCandlestick.setOpen(candlestick.getOpen());
        innerCandlestick.setHigh(candlestick.getHigh());
        innerCandlestick.setLow(candlestick.getLow());
        innerCandlestick.setVolume(candlestick.getVolume());
        innerCandlestick.setTurnover(candlestick.getTurnover());
        innerCandlestick.setTimestamp(candlestick.getTimestamp().toEpochSecond());
        return innerCandlestick;
    }
}
