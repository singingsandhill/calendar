package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbCandleResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CandleService {

    private static final Logger log = LoggerFactory.getLogger(CandleService.class);
    private static final DateTimeFormatter KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CandleRepository candleRepository;
    private final BithumbApiClient bithumbApiClient;
    private final TradingProperties tradingProperties;

    public CandleService(CandleRepository candleRepository,
                         BithumbApiClient bithumbApiClient,
                         TradingProperties tradingProperties) {
        this.candleRepository = candleRepository;
        this.bithumbApiClient = bithumbApiClient;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 최신 캔들 데이터 수집 및 저장
     */
    @Transactional
    public int fetchAndSaveCandles() {
        String market = tradingProperties.getBot().getMarket();
        return fetchAndSaveCandles(market, 1, 200);
    }

    /**
     * 캔들 데이터 수집 및 저장 (배치 처리로 N+1 쿼리 해결)
     */
    @Transactional
    public int fetchAndSaveCandles(String market, int unit, int count) {
        List<BithumbCandleResponse> candles = bithumbApiClient.getMinuteCandles(unit, market, count);

        if (candles == null || candles.isEmpty()) {
            log.warn("No candle data received from API");
            return 0;
        }

        // 1. API 응답을 시간 -> 응답 맵으로 변환
        Map<LocalDateTime, BithumbCandleResponse> responseMap = candles.stream()
                .collect(Collectors.toMap(
                        r -> parseDateTime(r.candleDateTimeKst()),
                        r -> r,
                        (existing, replacement) -> existing // 중복 시 기존 값 유지
                ));

        // 2. 단일 쿼리로 이미 존재하는 시간 조회
        Set<LocalDateTime> existingDateTimes = candleRepository.findExistingDateTimesByMarketAndDateTimeIn(
                market, responseMap.keySet());

        // 3. 신규 캔들만 필터링하여 리스트 생성
        List<Candle> newCandles = new ArrayList<>();
        for (Map.Entry<LocalDateTime, BithumbCandleResponse> entry : responseMap.entrySet()) {
            if (!existingDateTimes.contains(entry.getKey())) {
                try {
                    newCandles.add(mapToCandle(entry.getValue()));
                } catch (Exception e) {
                    log.error("Failed to map candle: {}", entry.getValue(), e);
                }
            }
        }

        // 4. 배치 저장
        if (!newCandles.isEmpty()) {
            candleRepository.saveAll(newCandles);
            log.info("Saved {} new candles for {} (batch)", newCandles.size(), market);
        }

        return newCandles.size();
    }

    /**
     * 초기 데이터 로드 (최대 200개)
     */
    @Transactional
    public int initializeCandles() {
        String market = tradingProperties.getBot().getMarket();
        long existingCount = candleRepository.countByMarket(market);

        if (existingCount >= 100) {
            log.info("Sufficient candle data exists: {} candles", existingCount);
            return 0;
        }

        log.info("Initializing candle data for {}...", market);
        return fetchAndSaveCandles(market, 1, 200);
    }

    /**
     * 오래된 캔들 데이터 정리 (7일 이상)
     */
    @Transactional
    public int cleanupOldCandles() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        int deleted = candleRepository.deleteByDateTimeBefore(cutoffDate);

        if (deleted > 0) {
            log.info("Deleted {} old candles", deleted);
        }

        return deleted;
    }

    /**
     * 최신 캔들 조회
     */
    public List<Candle> getLatestCandles(int count) {
        String market = tradingProperties.getBot().getMarket();
        return candleRepository.findByMarketOrderByDateTimeDesc(market, count);
    }

    /**
     * 특정 기간 캔들 조회
     */
    public List<Candle> getCandlesByDateRange(LocalDateTime from, LocalDateTime to) {
        String market = tradingProperties.getBot().getMarket();
        return candleRepository.findByMarketAndDateTimeRange(market, from, to);
    }

    /**
     * API 응답을 도메인 객체로 변환
     */
    private Candle mapToCandle(BithumbCandleResponse response) {
        return new Candle(
                null,
                response.market(),
                parseDateTime(response.candleDateTimeKst()),
                BigDecimal.valueOf(response.openingPrice()),
                BigDecimal.valueOf(response.highPrice()),
                BigDecimal.valueOf(response.lowPrice()),
                BigDecimal.valueOf(response.tradePrice()),
                BigDecimal.valueOf(response.candleAccTradeVolume()),
                BigDecimal.valueOf(response.candleAccTradePrice()),
                LocalDateTime.now()
        );
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, KST_FORMATTER);
    }
}
