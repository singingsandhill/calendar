package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisOrderbookResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 갭 상승 종목 스크리닝 서비스
 */
@Service
@Transactional(readOnly = true)
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);

    private final StockRepository stockRepository;
    private final StockSignalRepository signalRepository;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;

    public ScreeningService(StockRepository stockRepository,
                            StockSignalRepository signalRepository,
                            KoreaInvestmentApiClient kisApiClient,
                            StockProperties stockProperties) {
        this.stockRepository = stockRepository;
        this.signalRepository = signalRepository;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
    }

    /**
     * 갭 상승 종목 스크리닝 실행
     * 1차: 갭 비율 필터 (2-7%)
     * 2차: 시가총액, 거래대금, 체결강도, 스프레드 필터
     */
    @Transactional
    public List<Stock> executeScreening(LocalDate tradingDate, List<String> stockCodes) {
        log.info("Starting gap screening for {} stocks", stockCodes.size());

        List<Stock> qualifiedStocks = new ArrayList<>();

        for (String stockCode : stockCodes) {
            try {
                Stock stock = screenSingleStock(stockCode, tradingDate);
                if (stock != null) {
                    qualifiedStocks.add(stock);
                }

                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("Error screening stock {}: {}", stockCode, e.getMessage());
            }
        }

        // 갭 비율 순으로 정렬하고 상위 N개만 선정
        int maxWatchlist = stockProperties.getScreening().getMaxWatchlistSize();
        List<Stock> selectedStocks = qualifiedStocks.stream()
            .sorted(Comparator.comparing(Stock::getGapPercent).reversed())
            .limit(maxWatchlist)
            .toList();

        log.info("Screening complete. Selected {} stocks out of {} qualified",
            selectedStocks.size(), qualifiedStocks.size());

        // DB에 저장 (에러 처리 포함)
        int savedStockCount = 0;
        int savedSignalCount = 0;
        int failedCount = 0;

        for (Stock stock : selectedStocks) {
            try {
                // 주식 정보와 시그널을 함께 저장 (트랜잭션 내에서 원자성 보장)
                Stock savedStock = stockRepository.save(stock);
                savedStockCount++;

                // 갭 감지 시그널 저장
                StockSignal signal = StockSignal.gapDetected(
                    stock.getStockCode(),
                    stock.getGapPercent(),
                    stock.getMarketCap(),
                    stock.getTradeValue(),
                    stock.getTradeStrength()
                );
                signalRepository.save(signal);
                savedSignalCount++;

                log.debug("Saved stock {} with gap signal", stock.getStockCode());
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to save stock {} and its signal: {}. Error: {}",
                    stock.getStockCode(),
                    e.getClass().getSimpleName(),
                    e.getMessage());
            }
        }

        if (failedCount > 0) {
            log.warn("Screening save results: {} stocks saved, {} signals saved, {} failed",
                savedStockCount, savedSignalCount, failedCount);
        } else {
            log.info("Successfully saved all {} stocks and signals", savedStockCount);
        }

        return selectedStocks;
    }

    /**
     * 단일 종목 스크리닝
     */
    private Stock screenSingleStock(String stockCode, LocalDate tradingDate) {
        // 현재가 시세 조회
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            return null;
        }

        // 갭 비율 계산
        BigDecimal gapPercent = quote.calculateGapPercent();
        BigDecimal minGap = stockProperties.getScreening().getMinGapPercent();
        BigDecimal maxGap = stockProperties.getScreening().getMaxGapPercent();

        // 1차 필터: 갭 비율
        if (gapPercent.compareTo(minGap) < 0 || gapPercent.compareTo(maxGap) > 0) {
            return null;
        }

        // 2차 필터: 시가총액
        BigDecimal minMarketCap = stockProperties.getScreening().getMinMarketCap();
        if (quote.marketCap() != null && quote.marketCap().compareTo(minMarketCap) < 0) {
            return null;
        }

        // 3차 필터: 거래대금
        BigDecimal minTradeValue = stockProperties.getScreening().getMinTradeValue();
        if (quote.tradeValue() != null && quote.tradeValue().compareTo(minTradeValue) < 0) {
            return null;
        }

        // 4차 필터: 체결강도
        BigDecimal tradeStrength = quote.calculateTradeStrength();
        BigDecimal minStrength = stockProperties.getScreening().getMinTradeStrength();
        if (tradeStrength.compareTo(minStrength) < 0) {
            return null;
        }

        // 5차 필터: 스프레드
        KisOrderbookResponse orderbook = kisApiClient.getOrderbook(stockCode);
        if (orderbook != null) {
            BigDecimal spreadPercent = orderbook.calculateSpreadPercent();
            BigDecimal maxSpread = stockProperties.getScreening().getMaxSpreadPercent();
            if (spreadPercent.compareTo(maxSpread) > 0) {
                return null;
            }
        }

        // 필터 통과 - Stock 엔티티 생성
        Stock stock = new Stock(stockCode, stockCode, tradingDate); // 종목명은 추후 업데이트
        stock.setPrevClosePrice(quote.prevClosePrice());
        stock.setOpenPrice(quote.openPrice());
        stock.setCurrentPrice(quote.currentPrice());
        stock.setHighPrice(quote.highPrice());
        stock.setLowPrice(quote.lowPrice());
        stock.setVolume(quote.volume());
        stock.setTradeValue(quote.tradeValue());
        stock.setGapPercent(gapPercent);
        stock.setMarketCap(quote.marketCap());
        stock.setTradeStrength(tradeStrength);
        if (orderbook != null) {
            stock.setSpreadPercent(orderbook.calculateSpreadPercent());
        }

        log.info("Stock {} passed screening: gap={}%, strength={}",
            stockCode, gapPercent, tradeStrength);

        return stock;
    }

    /**
     * 기존 관심종목 목록 조회
     */
    public List<Stock> getWatchlist(LocalDate tradingDate) {
        return stockRepository.findByTradingDateOrderByGapPercentDesc(tradingDate);
    }

    /**
     * 활성 상태 종목 조회 (WATCHING, HIGH_FORMED, PULLBACK, ENTRY_READY)
     */
    public List<Stock> getActiveStocks(LocalDate tradingDate) {
        return stockRepository.findActiveStocks(tradingDate);
    }

    /**
     * 특정 상태의 종목 조회
     */
    public List<Stock> getStocksByState(LocalDate tradingDate, StockState state) {
        return stockRepository.findByTradingDateAndState(tradingDate, state);
    }
}
