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
        ScreeningStats stats = new ScreeningStats();

        for (String stockCode : stockCodes) {
            try {
                Stock stock = screenSingleStock(stockCode, tradingDate, stats);
                if (stock != null) {
                    qualifiedStocks.add(stock);
                }

                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("Error screening stock {}: {}", stockCode, e.getMessage());
                stats.errors++;
            }
        }

        // 갭 비율 순으로 정렬하고 상위 N개만 선정
        int maxWatchlist = stockProperties.getScreening().getMaxWatchlistSize();
        List<Stock> selectedStocks = qualifiedStocks.stream()
            .sorted(Comparator.comparing(Stock::getGapPercent).reversed())
            .limit(maxWatchlist)
            .toList();

        // 스크리닝 결과 요약 로그
        logScreeningSummary(stockCodes.size(), qualifiedStocks.size(), selectedStocks.size(), stats);

        // DB에 저장
        for (Stock stock : selectedStocks) {
            stockRepository.save(stock);

            // 갭 감지 시그널 저장
            StockSignal signal = StockSignal.gapDetected(
                stock.getStockCode(),
                stock.getGapPercent(),
                stock.getMarketCap(),
                stock.getTradeValue(),
                stock.getTradeStrength()
            );
            signalRepository.save(signal);
        }

        return selectedStocks;
    }

    /**
     * 단일 종목 스크리닝 (디버그 로깅 포함)
     */
    private Stock screenSingleStock(String stockCode, LocalDate tradingDate, ScreeningStats stats) {
        // 현재가 시세 조회
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            log.debug("[{}] API 실패: 시세 조회 불가", stockCode);
            stats.apiFailures++;
            return null;
        }

        // 갭 비율 계산
        BigDecimal gapPercent = quote.calculateGapPercent();
        BigDecimal minGap = stockProperties.getScreening().getMinGapPercent();
        BigDecimal maxGap = stockProperties.getScreening().getMaxGapPercent();

        // 1차 필터: 갭 비율
        if (gapPercent.compareTo(minGap) < 0 || gapPercent.compareTo(maxGap) > 0) {
            log.debug("[{}] 갭 필터 탈락: gap={}% (기준: {}~{}%)",
                stockCode, gapPercent, minGap, maxGap);
            stats.gapFiltered++;
            return null;
        }

        // 2차 필터: 시가총액
        BigDecimal minMarketCap = stockProperties.getScreening().getMinMarketCap();
        if (quote.marketCap() != null && quote.marketCap().compareTo(minMarketCap) < 0) {
            log.debug("[{}] 시총 필터 탈락: marketCap={}억 (기준: {}억 이상)",
                stockCode, quote.marketCap().divide(BigDecimal.valueOf(100000000)), minMarketCap.divide(BigDecimal.valueOf(100000000)));
            stats.marketCapFiltered++;
            return null;
        }

        // 3차 필터: 거래대금
        BigDecimal minTradeValue = stockProperties.getScreening().getMinTradeValue();
        if (quote.tradeValue() != null && quote.tradeValue().compareTo(minTradeValue) < 0) {
            log.debug("[{}] 거래대금 필터 탈락: tradeValue={}억 (기준: {}억 이상)",
                stockCode, quote.tradeValue().divide(BigDecimal.valueOf(100000000)), minTradeValue.divide(BigDecimal.valueOf(100000000)));
            stats.tradeValueFiltered++;
            return null;
        }

        // 4차 필터: 체결강도
        BigDecimal tradeStrength = quote.calculateTradeStrength();
        BigDecimal minStrength = stockProperties.getScreening().getMinTradeStrength();
        if (tradeStrength.compareTo(minStrength) < 0) {
            log.debug("[{}] 체결강도 필터 탈락: strength={} (기준: {} 이상)",
                stockCode, tradeStrength, minStrength);
            stats.strengthFiltered++;
            return null;
        }

        // 5차 필터: 스프레드
        KisOrderbookResponse orderbook = kisApiClient.getOrderbook(stockCode);
        if (orderbook != null) {
            BigDecimal spreadPercent = orderbook.calculateSpreadPercent();
            BigDecimal maxSpread = stockProperties.getScreening().getMaxSpreadPercent();
            if (spreadPercent.compareTo(maxSpread) > 0) {
                log.debug("[{}] 스프레드 필터 탈락: spread={}% (기준: {}% 이하)",
                    stockCode, spreadPercent, maxSpread);
                stats.spreadFiltered++;
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

        stats.passed++;
        log.info("Stock {} passed screening: gap={}%, strength={}",
            stockCode, gapPercent, tradeStrength);

        return stock;
    }

    /**
     * 스크리닝 통계 로깅
     */
    private void logScreeningSummary(int total, int qualified, int selected, ScreeningStats stats) {
        log.info("=== Screening Summary ===");
        log.info("Total: {}, Passed: {}, Selected: {}", total, qualified, selected);
        log.info("API failures: {}, Errors: {}", stats.apiFailures, stats.errors);
        log.info("Filtered - Gap: {}, MarketCap: {}, TradeValue: {}, Strength: {}, Spread: {}",
            stats.gapFiltered, stats.marketCapFiltered, stats.tradeValueFiltered,
            stats.strengthFiltered, stats.spreadFiltered);
    }

    /**
     * 스크리닝 통계 클래스
     */
    private static class ScreeningStats {
        int apiFailures = 0;
        int errors = 0;
        int gapFiltered = 0;
        int marketCapFiltered = 0;
        int tradeValueFiltered = 0;
        int strengthFiltered = 0;
        int spreadFiltered = 0;
        int passed = 0;
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
