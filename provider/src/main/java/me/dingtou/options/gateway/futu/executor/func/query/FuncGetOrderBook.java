package me.dingtou.options.gateway.futu.executor.func.query;

import com.futu.openapi.pb.QotCommon;
import com.futu.openapi.pb.QotGetOrderBook;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.gateway.futu.executor.QueryExecutor;
import me.dingtou.options.gateway.futu.executor.func.QueryFunctionCall;
import me.dingtou.options.model.Security;
import me.dingtou.options.model.SecurityOrderBook;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取摆盘信息
 *
 * @author qiyan
 */
@Slf4j
public class FuncGetOrderBook implements QueryFunctionCall<SecurityOrderBook> {

    private final Security security;

    public FuncGetOrderBook(Security security) {
        this.security = security;
    }

    @Override
    public List<Security> getSubSecurityList() {
        List<Security> target = new ArrayList<>();
        target.add(security);
        return target;
    }

    @Override
    public List<Integer> getSubTypeList() {
        List<Integer> subTypeList = new ArrayList<>();
        subTypeList.add(QotCommon.SubType.SubType_Basic_VALUE);
        subTypeList.add(QotCommon.SubType.SubType_OrderBook_VALUE);
        return subTypeList;
    }

    @Override
    public int call(QueryExecutor client) {

        QotCommon.Security sec = QotCommon.Security.newBuilder()
                .setMarket(security.getMarket())
                .setCode(security.getCode())
                .build();

        QotGetOrderBook.C2S c2s = QotGetOrderBook.C2S.newBuilder()
                .setSecurity(sec)
                .setNum(10)
                .build();
        QotGetOrderBook.Request req = QotGetOrderBook.Request.newBuilder().setC2S(c2s).build();
        int seqNo = client.getOrderBook(req);
        log.debug("Send QotGetOrderBook: {}", seqNo);
        if (seqNo == 0) {
            throw new RuntimeException("QotGetOrderBook error");
        }
        return seqNo;
    }

    @Override
    public SecurityOrderBook result(GeneratedMessageV3 response) {

        QotGetOrderBook.Response resp = (QotGetOrderBook.Response) response;
        if (null == resp || 0 != resp.getRetType()) {
            return null;
        }

        SecurityOrderBook orderBook = new SecurityOrderBook();
        QotGetOrderBook.S2C s2C = resp.getS2C();
        orderBook.setCode(s2C.getSecurity().getCode());
        orderBook.setMarket(s2C.getSecurity().getMarket());
        List<QotCommon.OrderBook> orderBookAskListList = s2C.getOrderBookAskListList();
        List<QotCommon.OrderBook> orderBookBidListList = s2C.getOrderBookBidListList();
        List<BigDecimal> askList = orderBookAskListList.stream().map(item -> BigDecimal.valueOf(item.getPrice()))
                .collect(Collectors.toList());
        List<BigDecimal> bidList = orderBookBidListList.stream().map(item -> BigDecimal.valueOf(item.getPrice()))
                .collect(Collectors.toList());
        orderBook.setAskList(askList);
        orderBook.setBidList(bidList);
        return orderBook;
    }

}
