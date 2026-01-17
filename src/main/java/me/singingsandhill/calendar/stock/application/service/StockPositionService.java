package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.exception.InsufficientBalanceException;
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

/**
 * 주식 포지션 관리 서비스
 */
@Service
@Transactional(readOnly = true)
public class StockPositionService {

    private static final Logger log = LoggerFactory.getLogger(StockPositionService.class);

    private final StockPositionRepository positionRepository;
    private final StockTradeRepository tradeRepository;
    private final StockRepository stockRepository;
    private final StockSignalRepository signalRepository;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;

    public StockPositionService(StockPositionRepository positionRepository,
                                 StockTradeRepository tradeRepository,
                                 StockRepository stockRepository,
                                 StockSignalRepository signalRepository,
                                 KoreaInvestmentApiClient kisApiClient,
                                 StockProperties stockProperties) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.stockRepository = stockRepository;
        this.signalRepository = signalRepository;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
    }

    /**
     * 새 포지션 오픈 (시장가 매수)
     */
    @Transactional
    public StockPosition openPosition(Stock stock) {
        String stockCode = stock.getStockCode();
        LocalDate tradingDate = stock.getTradingDate();

        log.info("Opening position for {}", stockCode);

        // 현재가 조회
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            log.error("Failed to open position for {}: Cannot retrieve quote from API. Stock may not exist or API is unavailable.", stockCode);
            return null;
        }

        BigDecimal currentPrice = quote.currentPrice();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Failed to open position for {}: Invalid current price ({})", stockCode, currentPrice);
            return null;
        }

        // 포지션 사이즈 계산
        int quantity = calculatePositionSize(stockCode, currentPrice);
        if (quantity <= 0) {
            log.warn("Failed to open position for {}: Calculated position size is 0. This could be due to insufficient funds or position limits.", stockCode);
            return null;
        }

        // 시장가 매수 주문
        KisOrderResponse orderResponse = kisApiClient.buyMarket(stockCode, quantity);
        if (orderResponse == null) {
            log.error("Failed to open position for {}: Buy order API returned null. Network or authentication issue likely.", stockCode);
            return null;
        }
        if (!orderResponse.isSuccess()) {
            log.error("Failed to open position for {}: Buy order rejected by broker. Code: {}, Message: {}",
                stockCode, orderResponse.messageCode(), orderResponse.message());
            return null;
        }

        // 거래 기록 저장
        StockTrade trade = StockTrade.createBuyOrder(
            orderResponse.getOrderId(),
            stockCode,
            quantity,
            currentPrice,
            true
        );
        trade.markFilled(currentPrice, quantity, BigDecimal.ZERO);
        trade = tradeRepository.save(trade);

        // 손절가 계산 (-1.5%)
        BigDecimal stopLossPercent = stockProperties.getRisk().getStopLossPercent();
        BigDecimal stopLossPrice = currentPrice.multiply(
            BigDecimal.ONE.subtract(stopLossPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
        );

        // 포지션 생성
        StockPosition position = StockPosition.open(
            stockCode,
            tradingDate,
            currentPrice,
            quantity,
            stopLossPrice,
            stock.getHighAfterOpen()
        );
        position.setStockId(stock.getId());
        position = positionRepository.save(position);

        // 거래에 포지션 ID 연결
        trade.setPositionId(position.getId());
        tradeRepository.save(trade);

        // Stock 상태 업데이트
        stock.markEntered(currentPrice);
        stockRepository.save(stock);

        log.info("Position opened for {}: {} shares @ {}, SL={}",
            stockCode, quantity, currentPrice, stopLossPrice);

        return position;
    }

    /**
     * 포지션 사이즈 계산
     */
    private int calculatePositionSize(String stockCode, BigDecimal price) {
        // 가용 현금 조회
        BigDecimal availableCash = kisApiClient.getAvailableCash();
        if (availableCash == null) {
            log.error("Cannot calculate position size for {}: API failure while retrieving available cash", stockCode);
            return 0;
        }
        if (availableCash.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Cannot calculate position size for {}: Insufficient balance (available: {})", stockCode, availableCash);
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
        String stockCode = position.getStockCode();
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

        // 포지션 업데이트
        position.executePartialExit(quantity, price, reason);
        positionRepository.save(position);

        // 시그널 저장
        StockSignalType signalType = switch (reason) {
            case TP1 -> StockSignalType.TP1_EXIT;
            case TP2 -> StockSignalType.TP2_EXIT;
            case TP3 -> StockSignalType.TP3_EXIT;
            case STOP_LOSS -> StockSignalType.STOP_LOSS_EXIT;
            case TRAILING_STOP -> StockSignalType.TRAILING_EXIT;
            case TIME_EXIT -> StockSignalType.TIME_EXIT;
            default -> null;
        };

        if (signalType != null) {
            StockSignal signal = StockSignal.exitSignal(stockCode, signalType, price);
            signal.markExecuted();
            signalRepository.save(signal);
        }

        log.info("Partial exit completed for {}: remaining {} shares",
            stockCode, position.getRemainingQuantity());
    }

    /**
     * 잔여 전량 청산
     */
    @Transactional
    public void closePosition(StockPosition position, BigDecimal price, StockCloseReason reason) {
        if (!position.hasRemainingQuantity()) {
            return;
        }
        executePartialExit(position, position.getRemainingQuantity(), price, reason);

        // Stock 상태 업데이트
        if (position.getStockId() != null) {
            stockRepository.findById(position.getStockId())
                .ifPresent(stock -> {
                    stock.markExited();
                    stockRepository.save(stock);
                });
        }
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
}
