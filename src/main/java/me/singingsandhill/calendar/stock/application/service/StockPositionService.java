package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.concurrency.StockCodeLocks;
import me.singingsandhill.calendar.stock.application.exception.InsufficientBalanceException;
import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.position.StockPositionStatus;
import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalType;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeRepository;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisOrderDetailResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisOrderResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 주식 포지션 관리 서비스
 */
@Service
@Transactional(readOnly = true)
public class StockPositionService {

    private static final Logger log = LoggerFactory.getLogger(StockPositionService.class);

    /** 미확인 주문을 브로커 원장에서 찾는 최대 시도 횟수 (트레이딩 루프 틱 기준). */
    public static final int MAX_RECONCILE_ATTEMPTS = 12;

    /** 미확인 주문별 스윕 시도 횟수 (거래일 내 인메모리 — 재시작 시 초기화되어도 안전). */
    private final Map<Long, Integer> reconcileAttempts = new ConcurrentHashMap<>();

    private final StockPositionRepository positionRepository;
    private final StockTradeRepository tradeRepository;
    private final StockRepository stockRepository;
    private final StockSignalRepository signalRepository;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;
    private final StockCodeLocks stockCodeLocks;

    public StockPositionService(StockPositionRepository positionRepository,
                                 StockTradeRepository tradeRepository,
                                 StockRepository stockRepository,
                                 StockSignalRepository signalRepository,
                                 KoreaInvestmentApiClient kisApiClient,
                                 StockProperties stockProperties,
                                 StockCodeLocks stockCodeLocks) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.stockRepository = stockRepository;
        this.signalRepository = signalRepository;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
        this.stockCodeLocks = stockCodeLocks;
    }

    /**
     * 새 포지션 오픈 (시장가 매수)
     */
    @Transactional
    public StockPosition openPosition(Stock stock) {
        return stockCodeLocks.withLock(stock.getStockCode(), () -> doOpenPosition(stock));
    }

    private StockPosition doOpenPosition(Stock stock) {
        String stockCode = stock.getStockCode();
        LocalDate tradingDate = stock.getTradingDate();

        log.info("Opening position for {}", stockCode);

        // 현재가 조회
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            log.error("Failed to get quote for {}", stockCode);
            return null;
        }

        // 진입 직전 거래 가능 상태 재확인 — 스크리닝(09:20) 이후 VI·거래정지가 걸릴 수 있다
        // (2026-07-24 리뷰 §6 / P2-3).
        if (!quote.isTradable()) {
            log.warn("진입 취소 {} — 거래 불가 상태: {}", stockCode, quote.tradabilityReason());
            TradeEvents.event("ENTRY_BLOCKED_NOT_TRADABLE")
                .with("stockCode", stockCode)
                .with("reason", quote.tradabilityReason())
                .log();
            return null;
        }

        BigDecimal currentPrice = quote.currentPrice();

        // 포지션 사이즈 계산
        int quantity = calculatePositionSize(stockCode, currentPrice);
        if (quantity <= 0) {
            log.warn("Calculated position size is 0 for {}", stockCode);
            return null;
        }

        // 주문 선영속화: 응답이 유실돼도 "이 시각에 이 종목을 이만큼 주문했다"는 흔적을 남겨
        // 다음 틱 스윕이 고아 체결을 수습할 수 있게 한다 (2026-07-24 리뷰 §3-④ / P1-1).
        StockTrade trade = tradeRepository.save(StockTrade.createBuyOrder(
            StockTrade.PENDING_ORDER_ID_PREFIX + System.nanoTime(),
            stockCode, quantity, currentPrice, true));

        // 시장가 매수 주문
        KisOrderResponse orderResponse = kisApiClient.buyMarket(stockCode, quantity);
        if (orderResponse == null || !orderResponse.isSuccess()) {
            // 접수 여부 불명 — PENDING 을 실패로 덮지 않는다(스윕이 재조회로 판정).
            log.error("Buy order unconfirmed for {} ({} shares) — 스윕 대기", stockCode, quantity);
            TradeEvents.event("ORDER_UNCONFIRMED")
                .with("stockCode", stockCode)
                .with("quantity", quantity)
                .with("tradeId", trade.getId())
                .log();
            return null;
        }

        trade.assignBrokerOrderId(orderResponse.getOrderId());

        // 실체결가·수량 backfill (조회 실패 시 요청가 폴백 + WARN)
        Fill fill = resolveBuyFill(stockCode, orderResponse.getOrderId(), currentPrice, quantity);
        trade.markFilled(fill.price(), fill.quantity(), buyFee(fill));
        trade = tradeRepository.save(trade);

        // 손절가: 풀백저가 앵커 + 진입가 대비 최대 손실 캡 (ADR stock/algorithm/0007)
        StockProperties.Risk risk = stockProperties.getRisk();
        BigDecimal stopLossPrice = StockPosition.resolveStopLossPrice(
            fill.price(), stock.getPullbackLow(),
            risk.getPullbackStopBufferPercent(), risk.getMaxStopLossPercent());

        // 포지션 생성 (실체결가 기준)
        StockPosition position = StockPosition.open(
            stockCode,
            tradingDate,
            fill.price(),
            fill.quantity(),
            stopLossPrice,
            stock.getHighAfterOpen()
        );
        position.setStockId(stock.getId());
        position = positionRepository.save(position);

        // 거래에 포지션 ID 연결
        trade.setPositionId(position.getId());
        tradeRepository.save(trade);

        // Stock 상태 업데이트
        stock.markEntered(fill.price());
        stockRepository.save(stock);

        log.info("Position opened for {}: {} shares @ {} (요청가 {}), SL={}",
            stockCode, fill.quantity(), fill.price(), currentPrice, stopLossPrice);

        return position;
    }

    /**
     * 미확인 주문 스윕 — 주문 응답이 유실된 매수의 실제 접수/체결 여부를 브로커 원장으로 판정한다.
     *
     * 체결이 확인되면 거래 기록을 정합화하고, 포지션이 없으면 생성해 **무보호 포지션**
     * (손절·익절 루프가 모르는 실보유)을 제거한다. {@link #MAX_RECONCILE_ATTEMPTS} 틱 동안
     * 원장에서 발견되지 않으면 미접수로 간주해 CANCELLED 처리한다.
     * 트레이딩 루프 시작부에서 호출된다 (2026-07-24 리뷰 §3-④ / P1-1).
     */
    @Transactional
    public void reconcileUnconfirmedOrders(LocalDate tradingDate) {
        List<StockTrade> todayTrades = tradeRepository.findTodayTrades();
        List<StockTrade> unconfirmed = todayTrades.stream()
            .filter(t -> t.isBuy() && t.isPending() && t.isUnconfirmedOrder())
            .toList();
        if (unconfirmed.isEmpty()) {
            reconcileAttempts.clear();
            return;
        }

        Set<String> knownOrderIds = todayTrades.stream()
            .filter(t -> !t.isUnconfirmedOrder())
            .map(StockTrade::getOrderId)
            .collect(Collectors.toSet());

        KisOrderDetailResponse history;
        try {
            history = kisApiClient.getTodayOrders();
        } catch (Exception e) {
            log.warn("미확인 주문 스윕: 당일주문조회 실패 — 다음 틱 재시도 ({})", e.getMessage());
            return;
        }
        List<KisOrderDetailResponse.OrderDetail> orders =
            (history != null && history.orders() != null) ? history.orders() : List.of();

        for (StockTrade pending : unconfirmed) {
            orders.stream()
                .filter(o -> o.isFilled())
                .filter(o -> pending.getStockCode().equals(o.stockCode()))
                .filter(o -> !knownOrderIds.contains(o.orderId()))
                .filter(o -> Objects.equals(pending.getQuantity(), o.orderQuantity()))
                .findFirst()
                .ifPresentOrElse(
                    o -> recoverOrphanFill(pending, o, tradingDate, knownOrderIds),
                    () -> giveUpIfExhausted(pending));
        }
    }

    private void recoverOrphanFill(StockTrade pending, KisOrderDetailResponse.OrderDetail order,
                                    LocalDate tradingDate, Set<String> knownOrderIds) {
        String stockCode = pending.getStockCode();
        BigDecimal fillPrice = (order.filledPrice() != null && order.filledPrice().signum() > 0)
            ? order.filledPrice() : pending.getOrderPrice();
        int fillQuantity = order.filledQuantity() != null ? order.filledQuantity() : pending.getQuantity();

        log.warn("고아 체결 발견 {} — 주문번호 {} ({}주 @ {}), 거래 기록 정합화",
            stockCode, order.orderId(), fillQuantity, fillPrice);

        pending.assignBrokerOrderId(order.orderId());
        pending.markFilled(fillPrice, fillQuantity, buyFee(new Fill(fillPrice, fillQuantity, true)));
        knownOrderIds.add(order.orderId());
        reconcileAttempts.remove(pending.getId());

        Optional<StockPosition> existing = positionRepository
            .findByStockCodeAndTradingDateAndStatusNot(stockCode, tradingDate, StockPositionStatus.CLOSED);
        if (existing.isPresent()) {
            pending.setPositionId(existing.get().getId());
            tradeRepository.save(pending);
            return;
        }

        Stock stock = stockRepository.findByStockCodeAndTradingDate(stockCode, tradingDate).orElse(null);
        StockProperties.Risk risk = stockProperties.getRisk();
        BigDecimal stopLossPrice = StockPosition.resolveStopLossPrice(
            fillPrice, stock != null ? stock.getPullbackLow() : null,
            risk.getPullbackStopBufferPercent(), risk.getMaxStopLossPercent());

        StockPosition recovered = StockPosition.open(stockCode, tradingDate, fillPrice, fillQuantity,
            stopLossPrice, stock != null ? stock.getHighAfterOpen() : null);
        if (stock != null) {
            recovered.setStockId(stock.getId());
        }
        recovered = positionRepository.save(recovered);
        pending.setPositionId(recovered.getId());
        tradeRepository.save(pending);

        if (stock != null) {
            stock.markEntered(fillPrice);
            stockRepository.save(stock);
        }

        TradeEvents.event("ORPHAN_FILL_RECOVERED")
            .with("stockCode", stockCode)
            .with("orderId", order.orderId())
            .with("price", fillPrice)
            .with("quantity", fillQuantity)
            .log();
    }

    private void giveUpIfExhausted(StockTrade pending) {
        int attempts = reconcileAttempts.merge(pending.getId(), 1, Integer::sum);
        if (attempts < MAX_RECONCILE_ATTEMPTS) {
            return;
        }
        log.warn("미확인 주문 {} ({}주): {}회 조회에도 원장에 없음 — 미접수로 간주하고 취소 처리",
            pending.getStockCode(), pending.getQuantity(), attempts);
        pending.markCancelled();
        tradeRepository.save(pending);
        reconcileAttempts.remove(pending.getId());
        TradeEvents.event("ORDER_NOT_ACCEPTED")
            .with("stockCode", pending.getStockCode())
            .with("quantity", pending.getQuantity())
            .log();
    }

    /** 체결 결과 (실체결가·수량). */
    private record Fill(BigDecimal price, int quantity, boolean confirmed) {}

    /**
     * 당일주문체결조회(TTTC8001R)로 실체결가·수량을 확인한다.
     * 조회 실패/미발견이면 요청가로 폴백하되 WARN — 장부가 픽션임을 로그로 드러낸다.
     */
    private Fill resolveBuyFill(String stockCode, String orderId,
                                 BigDecimal requestedPrice, int requestedQuantity) {
        try {
            KisOrderDetailResponse history = kisApiClient.getTodayOrders();
            if (history != null && history.orders() != null) {
                for (KisOrderDetailResponse.OrderDetail o : history.orders()) {
                    if (orderId != null && orderId.equals(o.orderId()) && o.isFilled()
                            && o.filledPrice() != null && o.filledPrice().signum() > 0) {
                        return new Fill(o.filledPrice(), o.filledQuantity(), true);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("체결 조회 실패 {} ({}): {}", stockCode, orderId, e.getMessage());
        }
        log.warn("체결 확인 불가 {} ({}) — 요청가 {} 로 기록 (실체결가와 다를 수 있음)",
            stockCode, orderId, requestedPrice);
        return new Fill(requestedPrice, requestedQuantity, false);
    }

    private BigDecimal buyFee(Fill fill) {
        return fill.price().multiply(BigDecimal.valueOf(fill.quantity()))
            .multiply(stockProperties.getRisk().getCommissionRate())
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 포지션 사이즈 계산
     */
    private int calculatePositionSize(String stockCode, BigDecimal price) {
        // 가용 현금 조회
        BigDecimal availableCash = kisApiClient.getAvailableCash();
        if (availableCash == null || availableCash.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        // 최대 포지션 크기
        BigDecimal maxPositionSize = stockProperties.getBot().getMaxPositionSize();

        // 포지션 비율 (계좌의 10%)
        BigDecimal positionRatio = stockProperties.getRisk().getPositionSizeRatio();
        BigDecimal targetAmount = availableCash.multiply(positionRatio);

        // 최대 금액 제한
        BigDecimal orderAmount = targetAmount.min(maxPositionSize);

        // 수량 계산
        int quantity = orderAmount.divide(price, 0, RoundingMode.DOWN).intValue();

        // 매수 가능 수량 확인
        int buyableQty = kisApiClient.getBuyableQuantity(stockCode, price);
        quantity = Math.min(quantity, buyableQty);

        return quantity;
    }

    /**
     * 부분 청산 실행
     */
    @Transactional
    public void executePartialExit(StockPosition position, int quantity,
                                    BigDecimal price, StockCloseReason reason) {
        stockCodeLocks.withLock(position.getStockCode(),
            () -> doExecutePartialExit(position, quantity, price, reason));
    }

    private void doExecutePartialExit(StockPosition position, int quantity,
                                        BigDecimal price, StockCloseReason reason) {
        String stockCode = position.getStockCode();

        // 주문은 롤백 불가능하므로 도메인 수량 검증을 주문 발행 *전*에 수행한다.
        Integer remaining = position.getRemainingQuantity();
        if (quantity <= 0 || remaining == null || quantity > remaining) {
            log.error("Invalid exit quantity for {}: requested={}, remaining={} ({}) — order not placed",
                stockCode, quantity, remaining, reason);
            return;
        }

        log.info("Executing partial exit for {}: {} shares @ {} ({})",
            stockCode, quantity, price, reason);

        // 시장가 매도 주문
        KisOrderResponse orderResponse = kisApiClient.sellMarket(stockCode, quantity);
        if (orderResponse == null || !orderResponse.isSuccess()) {
            log.error("Failed to place sell order for {}", stockCode);
            return;
        }

        // 거래 기록 저장
        StockTrade trade = StockTrade.createSellOrder(
            orderResponse.getOrderId(),
            stockCode,
            quantity,
            price,
            true,
            reason
        );
        trade.setPositionId(position.getId());
        trade.markFilled(price, quantity, BigDecimal.ZERO);
        tradeRepository.save(trade);

        // 포지션 업데이트 (수수료 포함)
        BigDecimal commissionRate = stockProperties.getRisk().getCommissionRate();
        BigDecimal sellTaxRate = stockProperties.getRisk().getSellTaxRate();
        position.executePartialExit(quantity, price, reason, commissionRate, sellTaxRate);
        positionRepository.save(position);

        // 시그널 저장
        StockSignalType signalType = switch (reason) {
            case TP1 -> StockSignalType.TP1_EXIT;
            case TP2 -> StockSignalType.TP2_EXIT;
            case TP3 -> StockSignalType.TP3_EXIT;
            case STOP_LOSS -> StockSignalType.STOP_LOSS_EXIT;
            case TRAILING_STOP -> StockSignalType.TRAILING_EXIT;
            case TIME_EXIT -> StockSignalType.TIME_EXIT;
            case MANUAL -> StockSignalType.MANUAL_EXIT;
            case EMERGENCY -> StockSignalType.EMERGENCY_EXIT;
        };

        StockSignal signal = StockSignal.exitSignal(stockCode, signalType, price);
        signal.markExecuted();
        signalRepository.save(signal);

        log.info("Partial exit completed for {}: remaining {} shares",
            stockCode, position.getRemainingQuantity());

        TradeEvents.event(position.getRemainingQuantity() == 0 ? "POSITION_CLOSED" : "PARTIAL_EXIT")
            .with("stockCode", stockCode)
            .with("positionId", position.getId())
            .with("reason", reason)
            .with("price", price)
            .with("quantity", quantity)
            .with("remaining", position.getRemainingQuantity())
            .with("realizedPnl", position.getRealizedPnl())
            .log();
    }

    /**
     * 잔여 전량 청산.
     *
     * @return 실제로 전량 청산됐으면 true. 매도 주문 거부 등으로 잔여가 남으면 false —
     *         호출측(최종청산 등)이 재시도·알림을 판단할 수 있어야 하므로 void 가 아니다
     *         (2026-07-24 리뷰 §6 / P1-6).
     */
    @Transactional
    public boolean closePosition(StockPosition position, BigDecimal price, StockCloseReason reason) {
        if (!position.hasRemainingQuantity()) {
            return true;
        }
        executePartialExit(position, position.getRemainingQuantity(), price, reason);

        if (position.hasRemainingQuantity()) {
            return false;
        }

        // Stock 상태 업데이트
        if (position.getStockId() != null) {
            stockRepository.findById(position.getStockId())
                .ifPresent(stock -> {
                    stock.markExited();
                    stockRepository.save(stock);
                });
        }
        return true;
    }

    /**
     * 오픈 포지션 목록 조회
     */
    public List<StockPosition> getOpenPositions(LocalDate tradingDate) {
        return positionRepository.findOpenPositions(tradingDate);
    }

    /**
     * 오픈 포지션 수 조회
     */
    public int countOpenPositions(LocalDate tradingDate) {
        return positionRepository.countOpenPositions(tradingDate);
    }

    /**
     * 청산된 포지션 목록 조회
     */
    public List<StockPosition> getClosedPositions(LocalDate tradingDate) {
        return positionRepository.findClosedPositions(tradingDate);
    }

    /**
     * 모든 포지션 조회
     */
    public List<StockPosition> getAllPositions(LocalDate tradingDate) {
        return positionRepository.findByTradingDate(tradingDate);
    }

    /**
     * 기간 내 포지션 조회 (history용)
     */
    public List<StockPosition> getPositionsInRange(LocalDate from, LocalDate to) {
        return positionRepository.findByTradingDateBetween(from, to);
    }
}
