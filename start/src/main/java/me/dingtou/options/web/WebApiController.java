package me.dingtou.options.web;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import me.dingtou.options.constant.OrderAction;
import me.dingtou.options.constant.OrderStatus;
import me.dingtou.options.constant.TradeSide;
import me.dingtou.options.job.JobClient;
import me.dingtou.options.job.JobContext;
import me.dingtou.options.job.background.CloseOrderJob;
import me.dingtou.options.job.background.CloseOrderJob.CloseOrderJobArgs;
import me.dingtou.options.model.*;
import me.dingtou.options.service.AuthService;
import me.dingtou.options.service.OptionsQueryService;
import me.dingtou.options.service.OptionsTradeService;
import me.dingtou.options.service.SummaryService;
import me.dingtou.options.web.model.CloseOrderJobReq;
import me.dingtou.options.web.model.WebResult;
import me.dingtou.options.web.util.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API 控制器
 *
 * @author qiyan
 */
@Slf4j
@RestController
public class WebApiController {

    @Autowired
    private AuthService authService;

    @Autowired
    private OptionsQueryService optionsQueryService;

    @Autowired
    private OptionsTradeService optionsTradeService;

    @Autowired
    private SummaryService summaryService;

    /**
     * 查询当前用户的证券和策略
     *
     * @return 用户的证券和策略
     */
    @RequestMapping(value = "/owner/get", method = RequestMethod.GET)
    public WebResult<Owner> queryOwner() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return WebResult.success(optionsQueryService.queryOwnerWithOrder(owner));
    }

    /**
     * 查询当前用户的证券和策略汇总
     *
     * @return 汇总信息
     */
    @RequestMapping(value = "/owner/summary", method = RequestMethod.GET)
    public WebResult<OwnerSummary> queryOwnerSummary() throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return WebResult.success(summaryService.queryOwnerSummary(owner));
    }

    /**
     * 查询证券的期权链到期日
     *
     * @param security 证券
     * @return 期权链到期日
     */
    @RequestMapping(value = "/options/strike/list", method = RequestMethod.GET)
    public WebResult<List<OptionsStrikeDate>> listOptionsExpDate(Security security) {
        log.info("list strike. security:{}", security);
        if (null == security || StringUtils.isEmpty(security.getCode())) {
            return WebResult.success(Collections.emptyList());
        }
        List<OptionsStrikeDate> optionsStrikeDates = optionsQueryService.queryOptionsExpDate(security);
        if (null == optionsStrikeDates || optionsStrikeDates.isEmpty()) {
            return WebResult.success(Collections.emptyList());
        }
        return WebResult.success(optionsStrikeDates);
    }

    /**
     * 查询期权链
     *
     * @param market                   市场
     * @param code                     证券代码
     * @param strikeTime               期权链到期日
     * @param strikeTimestamp          到期时间戳
     * @param optionExpiryDateDistance 到期天数
     * @return 期权链
     */
    @RequestMapping(value = "/options/chain/get", method = RequestMethod.GET)
    public WebResult<OptionsChain> listOptionsChain(@RequestParam(value = "market", required = true) Integer market,
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "strikeTime", required = true) String strikeTime,
            @RequestParam(value = "strategyId", required = false) String strategyId) throws Exception {
        log.info("get options chain. market:{}, code:{}, strikeTime:{}, strategyId:{}",
                market,
                code,
                strikeTime,
                strategyId);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);

        // 当传入策略ID，则查询对应的策略进行期权链处理。
        String currentOwner = SessionUtils.getCurrentOwner();
        Owner owner = optionsQueryService.queryOwner(currentOwner);
        OwnerStrategy strategy = null;
        if (null != owner && null != strategyId) {
            Optional<? extends OwnerStrategy> ownerStrategy = owner.getStrategyList()
                    .stream()
                    .filter(item -> item.getStrategyId().equals(strategyId))
                    .findFirst();
            if (ownerStrategy.isPresent()) {
                strategy = ownerStrategy.get();
            }
        }

        try {
            OptionsChain optionsChain = optionsQueryService.queryOptionsChain(currentOwner,
                    security,
                    strikeTime,
                    strategy);
            return WebResult.success(optionsChain);
        } catch (Exception e) {
            log.error("get options chain error. market:{}, code:{}, strikeTime:{}, message:{}", market, code,
                    strikeTime, e.getMessage(), e);
            return WebResult.failure(e.getMessage());
        }
    }

    @RequestMapping(value = "/options/strategy/get", method = RequestMethod.GET)
    public WebResult<StrategySummary> queryStrategySummary(
            @RequestParam(value = "strategyId", required = true) String strategyId) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        return WebResult.success(summaryService.queryStrategySummary(owner, strategyId));
    }

    @RequestMapping(value = "/options/orderbook/get", method = RequestMethod.GET)
    public WebResult<SecurityOrderBook> listOrderBook(@RequestParam(value = "market", required = true) Integer market,
            @RequestParam(value = "code", required = true) String code) throws Exception {
        log.info("get orderbook. market:{}, code:{}", market, code);
        Security security = new Security();
        security.setMarket(market);
        security.setCode(code);
        return WebResult.success(optionsQueryService.queryOrderBook(security));
    }

    @RequestMapping(value = "/trade/submit", method = RequestMethod.POST)
    public WebResult<OwnerOrder> submit(@RequestParam(value = "side", required = true) Integer side,
            @RequestParam(value = "strategyId", required = true) String strategyId,
            @RequestParam(value = "quantity", required = true) Integer quantity,
            @RequestParam(value = "price", required = true) String price,
            @RequestParam(value = "options", required = true) String options,
            @RequestParam(value = "closeOrderJob", required = false) String closeOrderJob,
            @RequestParam(value = "password", required = true) String password) throws Exception {

        String owner = SessionUtils.getCurrentOwner();
        log.info("trade submit. owner:{}, side:{}, quantity:{}, price:{}, options:{}, closeOrderJob:{}",
                owner, side, quantity, price, options, closeOrderJob);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        Options optionsObj = JSON.parseObject(options, Options.class);
        BigDecimal sellPrice = new BigDecimal(price);

        OwnerOrder order = optionsTradeService.submit(strategyId, TradeSide.of(side), quantity, sellPrice, optionsObj);
        if (null == order || null == order.getId()) {
            return WebResult.failure("下单失败");
        }

        // 解析平仓单请求
        CloseOrderJobReq closeOrderJobReq = null;
        if (StringUtils.isNotEmpty(closeOrderJob)) {
            closeOrderJobReq = JSON.parseObject(closeOrderJob, CloseOrderJobReq.class);
            closeOrderJobReq.setOrderId(order.getId());
            log.info("close order job req:{}", closeOrderJobReq);
            if (closeOrderJobReq.getEnabled()) {
                CloseOrderJobArgs args = new CloseOrderJobArgs();
                String jobId = String.format("close-%s", order.getId());
                args.setJobId(UUID.nameUUIDFromBytes(jobId.getBytes(StandardCharsets.UTF_8)));
                args.setOwner(owner);
                args.setOrderId(order.getId());
                args.setPrice(closeOrderJobReq.getPrice());
                args.setCannelTime(closeOrderJobReq.getCannelTime());
                // 默认600秒后执行
                Instant executeTime = Instant.now().plus(Duration.ofSeconds(600));
                JobClient.submit(args.getJobId(), new CloseOrderJob(), JobContext.of(args), executeTime);
            }
        }

        return WebResult.success(order);
    }

    @RequestMapping(value = "/trade/close", method = RequestMethod.POST)
    public WebResult<OwnerOrder> close(@RequestParam(value = "price", required = true) String price,
            @RequestParam(value = "orderId", required = true) Long orderId,
            @RequestParam(value = "password", required = true) String password,
            @RequestParam(value = "cannelTime", required = false) Date cannelTime) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade close. owner:{}, price:{}, orderId:{}", owner, price, orderId);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        return WebResult.success(optionsTradeService.close(owner, orderId, new BigDecimal(price), cannelTime));
    }

    @RequestMapping(value = "/trade/modify", method = RequestMethod.POST)
    public WebResult<OwnerOrder> modify(@RequestParam(value = "action", required = true) String action,
            @RequestParam(value = "orderId", required = true) Long orderId,
            @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade modify. owner:{}, action:{}, orderId:{}", owner, action, orderId);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        OrderAction orderAction = OrderAction.of(action);
        return WebResult.success(optionsTradeService.modify(owner, orderId, orderAction));
    }

    @RequestMapping(value = "/trade/sync", method = RequestMethod.GET)
    public WebResult<Boolean> sync(@RequestParam(value = "password", required = false) String password)
            throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade sync. owner:{}, password:{}", owner, password);
        if (StringUtils.isBlank(owner)) {
            return WebResult.failure("未登录");
        }
        return WebResult.success(optionsTradeService.sync(owner));
    }

    @RequestMapping(value = "/trade/order/draft", method = RequestMethod.GET)
    public WebResult<List<OwnerOrder>> queryDraftOrder(
            @RequestParam(value = "password", required = false) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("query order draft. owner:{}, password:{}", owner, password);
        if (StringUtils.isBlank(owner)) {
            return WebResult.failure("未登录");
        }
        return WebResult.success(optionsQueryService.queryDraftOrder(owner));
    }

    @RequestMapping(value = "/trade/update", method = RequestMethod.POST)
    public WebResult<Integer> updateOrderStrategy(
            @RequestParam(value = "strategyId", required = true) String strategyId,
            @RequestParam(value = "orderIds", required = false) List<Long> orderIds,
            @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade update. owner:{}, strategyId:{}, orderIds:{}", owner, strategyId, orderIds);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        if (null == orderIds || orderIds.isEmpty()) {
            return WebResult.failure("订单为空");
        }
        return WebResult.success(optionsTradeService.updateOrderStrategy(owner, orderIds, strategyId));
    }

    @RequestMapping(value = "/trade/updateStatus", method = RequestMethod.POST)
    public WebResult<Boolean> updateOrderStatus(
            @RequestParam(value = "orderId", required = true) Long orderId,
            @RequestParam(value = "status", required = true) Integer status,
            @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade updateStatus. owner:{}, orderId:{}, status:{}", owner, orderId, status);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        return WebResult.success(optionsTradeService.updateOrderStatus(owner, orderId, OrderStatus.of(status)));
    }
    
    @RequestMapping(value = "/trade/updateIncome", method = RequestMethod.POST)
    public WebResult<Boolean> updateOrderIncome(
            @RequestParam(value = "orderId", required = true) Long orderId,
            @RequestParam(value = "manualIncome", required = true) String manualIncome,
            @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade updateIncome. owner:{}, orderId:{}, manualIncome:{}", owner, orderId, manualIncome);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        try {
            BigDecimal incomeValue = new BigDecimal(manualIncome);
            return WebResult.success(optionsTradeService.updateOrderIncome(owner, orderId, incomeValue));
        } catch (NumberFormatException e) {
            return WebResult.failure("收益值格式错误");
        }
    }

    @RequestMapping(value = "/trade/updateStrategy", method = RequestMethod.POST)
    public WebResult<Boolean> updateOrderStrategy(
            @RequestParam(value = "orderId", required = true) Long orderId,
            @RequestParam(value = "strategyId", required = true) String strategyId,
            @RequestParam(value = "password", required = true) String password) throws Exception {
        String owner = SessionUtils.getCurrentOwner();
        log.info("trade updateStrategy. owner:{}, orderId:{}, strategyId:{}", owner, orderId, strategyId);
        if (!authService.auth(owner, password)) {
            return WebResult.failure("验证码错误");
        }
        return WebResult.success(
                optionsTradeService.updateOrderStrategy(owner, Collections.singletonList(orderId), strategyId) > 0);
    }

}
