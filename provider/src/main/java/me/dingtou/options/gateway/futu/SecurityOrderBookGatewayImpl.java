package me.dingtou.options.gateway.futu;

import me.dingtou.options.gateway.SecurityOrderBookGateway;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.query.FuncGetOrderBook;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;
import org.springframework.stereotype.Component;

@Component
public class SecurityOrderBookGatewayImpl implements SecurityOrderBookGateway {
    @Override
    public SecurityOrderBook getOrderBook(Security security) {
        return QueryExecutor.query(new FuncGetOrderBook(security));
    }
}
