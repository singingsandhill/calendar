package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
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
import java.math.MathContext;
import java.math.RoundingMode;
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

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final StockRepository stockRepository;
    private final StockSignalRepository signalRepository;
    private final KoreaInvestmentApiClient kisApiClient;
    private final StockProperties stockProperties;
    private final StockBotMetrics metrics;

    public ScreeningService(StockRepository stockRepository,
                            StockSignalRepository signalRepository,
                            KoreaInvestmentApiClient kisApiClient,
                            StockProperties stockProperties,
                            StockBotMetrics metrics) {
        this.stockRepository = stockRepository;
        this.signalRepository = signalRepository;
        this.kisApiClient = kisApiClient;
        this.stockProperties = stockProperties;
        this.metrics = metrics;
    }

    /**
     * 갭 상승 종목 스크리닝 실행
     */
    @Transactional
    public List<Stock> executeScreening(LocalDate tradingDate, List<String> stockCodes) {
        log.info("Starting gap screening for {} stocks (scoring={})",
            stockCodes.size(), stockProperties.getScoring().isEnabled());

        if (stockProperties.getScoring().isEnabled()) {
            return executeScoreBasedScreening(tradingDate, stockCodes);
        } else {
            return executeLegacyScreening(tradingDate, stockCodes);
        }
    }

    // ========== Score-based Screening (Phase 1) ==========

    /**
     * 복합 점수 기반 스크리닝
     * 1단계: Floor 필터 (최소한의 쓰레기 데이터 제거)
     * 2단계: 복합 점수 계산
     * 3단계: 점수 내림차순 정렬 → 상위 N개 선정
     */
    private List<Stock> executeScoreBasedScreening(LocalDate tradingDate, List<String> stockCodes) {
        List<StockCandidate> candidates = new ArrayList<>();
        ScreeningStats stats = new ScreeningStats();

        for (String stockCode : stockCodes) {
            try {
                // PR-5: KisRestClient 의 Semaphore 가 동시성/속도를 제어하므로
                // 종목 단위 Thread.sleep 폴링을 제거.
                StockCandidate candidate = evaluateStock(stockCode, tradingDate, stats);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            } catch (Exception e) {
                log.warn("Error screening stock {}: {}", stockCode, e.getMessage());
                stats.errors++;
            }
        }

        // 점수 내림차순 정렬
        candidates.sort(Comparator.comparing(StockCandidate::compositeScore).reversed());

        // 상위 N개 선정 (minCandidates 보장)
        int maxWatchlist = stockProperties.getScreening().getMaxWatchlistSize();
        int minCandidates = stockProperties.getScoring().getMinCandidates();
        BigDecimal minScoreThreshold = stockProperties.getScoring().getMinScoreThreshold();

        List<StockCandidate> selected = new ArrayList<>();
        for (int i = 0; i < candidates.size() && i < maxWatchlist; i++) {
            StockCandidate c = candidates.get(i);
            if (selected.size() < minCandidates || c.compositeScore().compareTo(minScoreThreshold) >= 0) {
                selected.add(c);
            }
        }

        // 로깅
        logScoreBasedSummary(stockCodes.size(), candidates.size(), selected.size(), stats, candidates);

        // DB 저장
        List<Stock> selectedStocks = new ArrayList<>();
        for (StockCandidate c : selected) {
            stockRepository.save(c.stock());
            StockSignal signal = StockSignal.gapDetected(
                c.stock().getStockCode(),
                c.stock().getGapPercent(),
                c.stock().getMarketCap(),
                c.stock().getTradeValue(),
                c.stock().getTradeStrength()
            );
            signalRepository.save(signal);
            selectedStocks.add(c.stock());
        }

        return selectedStocks;
    }

    /**
     * 단일 종목 평가 (Floor 필터 + 점수 계산)
     */
    private StockCandidate evaluateStock(String stockCode, LocalDate tradingDate, ScreeningStats stats) {
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            stats.apiFailures++;
            return null;
        }

        // Floor 필터 1: 시가 미확정 방어
        if (quote.openPrice() == null || quote.openPrice().compareTo(BigDecimal.ZERO) == 0) {
            stats.dataInsufficient++;
            return null;
        }

        BigDecimal gapPercent = quote.calculateGapPercent();
        BigDecimal floorGap = stockProperties.getScreening().getFloorGapPercent();
        BigDecimal floorMaxGap = stockProperties.getScoring().getFloorMaxGap();
        BigDecimal tradeStrength = quote.calculateTradeStrength();
        BigDecimal floorStrength = stockProperties.getScreening().getFloorTradeStrength();

        // Floor 필터 2: 최소 갭
        if (gapPercent.compareTo(floorGap) < 0 || gapPercent.compareTo(floorMaxGap) > 0) {
            stats.gapFiltered++;
            log.debug("[{}] Floor 갭 탈락: gap={}% (기준: {}~{}%)", stockCode, gapPercent, floorGap, floorMaxGap);
            return null;
        }

        // Floor 필터 3: 최소 체결강도
        // strength=0 은 장 초반 KIS API 미집계 상태. skipZeroStrength=true 면 스킵, false 면 통과시킴.
        if (tradeStrength.compareTo(BigDecimal.ZERO) == 0) {
            if (stockProperties.getScreening().isSkipZeroStrength()) {
                stats.dataInsufficient++;
                log.debug("[{}] 체결강도 미집계(=0): 데이터 부족으로 스킵", stockCode);
                return null;
            }
            log.debug("[{}] 체결강도 미집계(=0): skipZeroStrength=false 로 통과", stockCode);
            tradeStrength = floorStrength; // 후속 점수 계산을 위해 보정
        } else if (tradeStrength.compareTo(floorStrength) < 0) {
            stats.strengthFiltered++;
            log.debug("[{}] Floor 체결강도 탈락: strength={} (기준: {} 이상)", stockCode, tradeStrength, floorStrength);
            return null;
        }

        // Floor 필터 4: 최소 시가총액
        BigDecimal floorMinMarketCap = stockProperties.getScoring().getFloorMinMarketCap();
        if (quote.marketCap() != null && quote.marketCap().compareTo(floorMinMarketCap) < 0) {
            stats.marketCapFiltered++;
            log.debug("[{}] Floor 시총 탈락: {}억", stockCode,
                quote.marketCap().divide(BigDecimal.valueOf(100000000), 0, RoundingMode.HALF_UP));
            return null;
        }

        stats.floorPassed++;

        // 호가 조회 (스프레드 점수용)
        KisOrderbookResponse orderbook = kisApiClient.getOrderbook(stockCode);

        // 복합 점수 계산
        StockProperties.Scoring scoring = stockProperties.getScoring();
        BigDecimal gapScore = normalizeGapScore(gapPercent)
            .multiply(BigDecimal.valueOf(scoring.getGapWeight()));
        BigDecimal strengthScore = normalizeStrengthScore(tradeStrength)
            .multiply(BigDecimal.valueOf(scoring.getStrengthWeight()));
        BigDecimal tradeValueScore = normalizeTradeValueScore(quote.tradeValue())
            .multiply(BigDecimal.valueOf(scoring.getTradeValueWeight()));
        BigDecimal spreadScore = normalizeSpreadScore(orderbook)
            .multiply(BigDecimal.valueOf(scoring.getSpreadWeight()));
        BigDecimal marketCapScore = normalizeMarketCapScore(quote.marketCap())
            .multiply(BigDecimal.valueOf(scoring.getMarketCapWeight()));

        BigDecimal compositeScore = gapScore.add(strengthScore).add(tradeValueScore)
            .add(spreadScore).add(marketCapScore)
            .setScale(1, RoundingMode.HALF_UP);

        // Stock 엔티티 생성
        Stock stock = new Stock(stockCode, stockCode, tradingDate);
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
        stock.setCompositeScore(compositeScore);
        if (orderbook != null) {
            stock.setSpreadPercent(orderbook.calculateSpreadPercent());
        }

        log.info("[{}] Score={} (gap={} str={} vol={} spr={} cap={}) gap={}% str={}",
            stockCode, compositeScore,
            gapScore.setScale(1, RoundingMode.HALF_UP),
            strengthScore.setScale(1, RoundingMode.HALF_UP),
            tradeValueScore.setScale(1, RoundingMode.HALF_UP),
            spreadScore.setScale(1, RoundingMode.HALF_UP),
            marketCapScore.setScale(1, RoundingMode.HALF_UP),
            gapPercent, tradeStrength);

        return new StockCandidate(stock, compositeScore, gapScore, strengthScore,
            tradeValueScore, spreadScore, marketCapScore);
    }

    // ========== Score Normalization Methods ==========

    /**
     * 갭 점수: 종형 분포 (center=4.0%, sigma=3.0)
     * 3~5% 최고점, 0.5% 및 15%로 갈수록 감소
     */
    BigDecimal normalizeGapScore(BigDecimal gapPercent) {
        StockProperties.Scoring s = stockProperties.getScoring();
        BigDecimal diff = gapPercent.subtract(s.getGapCenter());
        BigDecimal exponent = diff.multiply(diff).negate()
            .divide(s.getGapSigma().multiply(s.getGapSigma()).multiply(new BigDecimal("2")), 10, RoundingMode.HALF_UP);
        double score = Math.exp(exponent.doubleValue());
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 체결강도 점수: [strengthMin, strengthMax] 선형 정규화.
     */
    BigDecimal normalizeStrengthScore(BigDecimal strength) {
        StockProperties.Scoring s = stockProperties.getScoring();
        if (strength == null || strength.compareTo(s.getStrengthMin()) <= 0) {
            return BigDecimal.ZERO;
        }
        if (strength.compareTo(s.getStrengthMax()) >= 0) {
            return BigDecimal.ONE;
        }
        return strength.subtract(s.getStrengthMin())
            .divide(s.getStrengthMax().subtract(s.getStrengthMin()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 거래대금 점수: log 스케일 정규화 [tradeValueMin, tradeValueMax].
     */
    BigDecimal normalizeTradeValueScore(BigDecimal tradeValue) {
        if (tradeValue == null || tradeValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        StockProperties.Scoring s = stockProperties.getScoring();
        double logValue = Math.log10(tradeValue.doubleValue());
        double logMin = Math.log10(s.getTradeValueMin().doubleValue());
        double logMax = Math.log10(s.getTradeValueMax().doubleValue());
        double score = (logValue - logMin) / (logMax - logMin);
        return BigDecimal.valueOf(Math.max(0, Math.min(1, score))).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 스프레드 점수: 역비례 - 낮을수록 고점수 [0, spreadMax].
     */
    BigDecimal normalizeSpreadScore(KisOrderbookResponse orderbook) {
        if (orderbook == null) {
            return new BigDecimal("0.5");
        }
        BigDecimal spread = orderbook.calculateSpreadPercent();
        if (spread.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal maxSpread = stockProperties.getScoring().getSpreadMax();
        if (spread.compareTo(maxSpread) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.subtract(spread.divide(maxSpread, 4, RoundingMode.HALF_UP));
    }

    /**
     * 시가총액 점수: log 스케일 정규화 [marketCapMin, marketCapMax].
     */
    BigDecimal normalizeMarketCapScore(BigDecimal marketCap) {
        if (marketCap == null || marketCap.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        StockProperties.Scoring s = stockProperties.getScoring();
        double logValue = Math.log10(marketCap.doubleValue());
        double logMin = Math.log10(s.getMarketCapMin().doubleValue());
        double logMax = Math.log10(s.getMarketCapMax().doubleValue());
        double score = (logValue - logMin) / (logMax - logMin);
        return BigDecimal.valueOf(Math.max(0, Math.min(1, score))).setScale(4, RoundingMode.HALF_UP);
    }

    // ========== Logging ==========

    private void logScoreBasedSummary(int total, int floorPassed, int selected,
                                       ScreeningStats stats, List<StockCandidate> candidates) {
        log.info("=== Screening Summary (Score-based) ===");
        log.info("Total: {}, Floor passed: {}, Selected: {}", total, floorPassed, selected);
        log.info("API failures: {}, Data insufficient: {}, Errors: {}",
            stats.apiFailures, stats.dataInsufficient, stats.errors);
        log.info("Floor filtered - Gap: {}, Strength: {}, MarketCap: {}",
            stats.gapFiltered, stats.strengthFiltered, stats.marketCapFiltered);

        // 침묵 실패 가드: 유니버스가 있는데 0건 선정이면 비정상 — 최다 탈락 버킷을 WARN.
        if (total > 0 && selected == 0) {
            log.warn("0 selected from {} universe — 최다 탈락 버킷: gap={}, dataInsufficient={}, strength={}, marketCap={}, apiFail={}. 유니버스/임계 점검 필요.",
                total, stats.gapFiltered, stats.dataInsufficient, stats.strengthFiltered,
                stats.marketCapFiltered, stats.apiFailures);
        }

        metrics.recordScreeningResult(total, floorPassed, selected,
            stats.dataInsufficient, stats.gapFiltered);

        TradeEvents.event("SCREENING_SUMMARY")
            .with("total", total)
            .with("floorPassed", floorPassed)
            .with("selected", selected)
            .with("dataInsufficient", stats.dataInsufficient)
            .with("gapFiltered", stats.gapFiltered)
            .with("strengthFiltered", stats.strengthFiltered)
            .with("apiFailures", stats.apiFailures)
            .log();

        if (!candidates.isEmpty()) {
            // 갭 분포
            long gap0to1 = candidates.stream().filter(c -> c.stock().getGapPercent().compareTo(BigDecimal.ONE) < 0).count();
            long gap1to3 = candidates.stream().filter(c -> {
                BigDecimal g = c.stock().getGapPercent();
                return g.compareTo(BigDecimal.ONE) >= 0 && g.compareTo(new BigDecimal("3")) < 0;
            }).count();
            long gap3to5 = candidates.stream().filter(c -> {
                BigDecimal g = c.stock().getGapPercent();
                return g.compareTo(new BigDecimal("3")) >= 0 && g.compareTo(new BigDecimal("5")) < 0;
            }).count();
            long gap5to10 = candidates.stream().filter(c -> {
                BigDecimal g = c.stock().getGapPercent();
                return g.compareTo(new BigDecimal("5")) >= 0 && g.compareTo(new BigDecimal("10")) < 0;
            }).count();
            long gap10plus = candidates.stream().filter(c -> c.stock().getGapPercent().compareTo(new BigDecimal("10")) >= 0).count();
            log.info("Gap distribution: 0-1%={}, 1-3%={}, 3-5%={}, 5-10%={}, 10%+={}",
                gap0to1, gap1to3, gap3to5, gap5to10, gap10plus);

            // 점수 분포
            long score80 = candidates.stream().filter(c -> c.compositeScore().compareTo(new BigDecimal("80")) >= 0).count();
            long score60 = candidates.stream().filter(c -> {
                BigDecimal s = c.compositeScore();
                return s.compareTo(new BigDecimal("60")) >= 0 && s.compareTo(new BigDecimal("80")) < 0;
            }).count();
            long score40 = candidates.stream().filter(c -> {
                BigDecimal s = c.compositeScore();
                return s.compareTo(new BigDecimal("40")) >= 0 && s.compareTo(new BigDecimal("60")) < 0;
            }).count();
            long score20 = candidates.stream().filter(c -> c.compositeScore().compareTo(new BigDecimal("40")) < 0).count();
            log.info("Score distribution: 80+={}, 60+={}, 40+={}, <40={}", score80, score60, score40, score20);

            if (selected > 0 && selected <= candidates.size()) {
                BigDecimal cutline = candidates.get(selected - 1).compositeScore();
                log.info("Selection cutline score: {}", cutline);
            }
        }
    }

    // ========== Legacy Screening (backward compat) ==========

    private List<Stock> executeLegacyScreening(LocalDate tradingDate, List<String> stockCodes) {
        List<Stock> qualifiedStocks = new ArrayList<>();
        ScreeningStats stats = new ScreeningStats();

        for (String stockCode : stockCodes) {
            try {
                Stock stock = screenSingleStock(stockCode, tradingDate, stats);
                if (stock != null) {
                    qualifiedStocks.add(stock);
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("Error screening stock {}: {}", stockCode, e.getMessage());
                stats.errors++;
            }
        }

        int maxWatchlist = stockProperties.getScreening().getMaxWatchlistSize();
        List<Stock> selectedStocks = qualifiedStocks.stream()
            .sorted(Comparator.comparing(Stock::getGapPercent).reversed())
            .limit(maxWatchlist)
            .toList();

        logLegacyScreeningSummary(stockCodes.size(), qualifiedStocks.size(), selectedStocks.size(), stats);

        for (Stock stock : selectedStocks) {
            stockRepository.save(stock);
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

    private Stock screenSingleStock(String stockCode, LocalDate tradingDate, ScreeningStats stats) {
        KisQuoteResponse quote = kisApiClient.getQuote(stockCode);
        if (quote == null) {
            stats.apiFailures++;
            return null;
        }

        if (quote.openPrice() == null || quote.openPrice().compareTo(BigDecimal.ZERO) == 0) {
            stats.dataInsufficient++;
            return null;
        }

        BigDecimal gapPercent = quote.calculateGapPercent();
        BigDecimal minGap = stockProperties.getScreening().getMinGapPercent();
        BigDecimal maxGap = stockProperties.getScreening().getMaxGapPercent();

        if (gapPercent.compareTo(minGap) < 0 || gapPercent.compareTo(maxGap) > 0) {
            stats.gapFiltered++;
            return null;
        }

        BigDecimal minMarketCap = stockProperties.getScreening().getMinMarketCap();
        if (quote.marketCap() != null && quote.marketCap().compareTo(minMarketCap) < 0) {
            stats.marketCapFiltered++;
            return null;
        }

        BigDecimal minTradeValue = stockProperties.getScreening().getMinTradeValue();
        if (quote.tradeValue() != null && quote.tradeValue().compareTo(minTradeValue) < 0) {
            stats.tradeValueFiltered++;
            return null;
        }

        BigDecimal tradeStrength = quote.calculateTradeStrength();
        BigDecimal minStrength = stockProperties.getScreening().getMinTradeStrength();
        if (tradeStrength.compareTo(BigDecimal.ZERO) == 0) {
            stats.dataInsufficient++;
            return null;
        }
        if (tradeStrength.compareTo(minStrength) < 0) {
            stats.strengthFiltered++;
            return null;
        }

        KisOrderbookResponse orderbook = kisApiClient.getOrderbook(stockCode);
        if (orderbook != null) {
            BigDecimal spreadPercent = orderbook.calculateSpreadPercent();
            BigDecimal maxSpread = stockProperties.getScreening().getMaxSpreadPercent();
            if (spreadPercent.compareTo(maxSpread) > 0) {
                stats.spreadFiltered++;
                return null;
            }
        }

        Stock stock = new Stock(stockCode, stockCode, tradingDate);
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
        log.info("Stock {} passed screening: gap={}%, strength={}", stockCode, gapPercent, tradeStrength);
        return stock;
    }

    private void logLegacyScreeningSummary(int total, int qualified, int selected, ScreeningStats stats) {
        log.info("=== Screening Summary (Legacy) ===");
        log.info("Total: {}, Passed: {}, Selected: {}", total, qualified, selected);
        log.info("API failures: {}, Data insufficient: {}, Errors: {}",
            stats.apiFailures, stats.dataInsufficient, stats.errors);
        log.info("Filtered - Gap: {}, MarketCap: {}, TradeValue: {}, Strength: {}, Spread: {}",
            stats.gapFiltered, stats.marketCapFiltered, stats.tradeValueFiltered,
            stats.strengthFiltered, stats.spreadFiltered);
    }

    // ========== Inner types ==========

    private record StockCandidate(Stock stock, BigDecimal compositeScore,
        BigDecimal gapScore, BigDecimal strengthScore, BigDecimal tradeValueScore,
        BigDecimal spreadScore, BigDecimal marketCapScore) {}

    private static class ScreeningStats {
        int apiFailures = 0;
        int errors = 0;
        int dataInsufficient = 0;
        int gapFiltered = 0;
        int marketCapFiltered = 0;
        int tradeValueFiltered = 0;
        int strengthFiltered = 0;
        int spreadFiltered = 0;
        int floorPassed = 0;
        int passed = 0;
    }

    // ========== Query Methods ==========

    public List<Stock> getWatchlist(LocalDate tradingDate) {
        return stockRepository.findByTradingDateOrderByGapPercentDesc(tradingDate);
    }

    public List<Stock> getActiveStocks(LocalDate tradingDate) {
        return stockRepository.findActiveStocks(tradingDate);
    }

    public List<Stock> getStocksByState(LocalDate tradingDate, StockState state) {
        return stockRepository.findByTradingDateAndState(tradingDate, state);
    }
}
