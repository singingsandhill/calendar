package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 그날의 스크리닝 유니버스를 만든다.
 *
 * 우선순위:
 *   1) yml 의 pinned 종목 (항상 포함)
 *   2) KIS 거래량순위 API 상위 N (stock.universe.rank-api-top &gt; 0 일 때)
 *   3) yml 의 fallback 종목 — rank 가 비었을 때만 쓰는 정적 안전망
 *
 * 동적 소스(2)는 거래일 1회(pre-market) 호출되어 스냅샷으로 캐시된다(ADR-0002 의 "거래일 1회
 * 스냅샷" 정합성 유지). executePreMarketLoop 에서 refresh() 가 호출되며 ScreeningService 는
 * currentUniverse() 로 동기 조회한다.
 */
@Service
public class UniverseBuilder {

    private static final Logger log = LoggerFactory.getLogger(UniverseBuilder.class);

    private final StockProperties stockProperties;
    private final KoreaInvestmentApiClient apiClient;
    private final AtomicReference<Snapshot> latest = new AtomicReference<>();

    public UniverseBuilder(StockProperties stockProperties, KoreaInvestmentApiClient apiClient) {
        this.stockProperties = stockProperties;
        this.apiClient = apiClient;
    }

    public Snapshot refresh(LocalDate tradingDate) {
        StockProperties.Universe universe = stockProperties.getUniverse();

        Set<String> codes = new LinkedHashSet<>();
        List<String> pinned = universe.getPinned();
        codes.addAll(pinned);

        // KIS 거래량순위 동적 소스 (rank-api-top > 0 일 때). 어떤 실패든 빈 리스트 → 폴백.
        List<String> rankCodes = fetchRankCodes(universe.getRankApiTop());
        codes.addAll(rankCodes);

        // 폴백: rank 가 비었을 때만(비활성/실패/0건) 정적 fallback-codes 사용.
        boolean usedFallback = rankCodes.isEmpty();
        if (usedFallback) {
            codes.addAll(universe.getFallbackCodes());
        }

        List<String> result = new ArrayList<>(codes);
        int fallbackCount = usedFallback ? universe.getFallbackCodes().size() : 0;
        Snapshot snapshot = new Snapshot(tradingDate, result, pinned.size(), fallbackCount, rankCodes.size());
        latest.set(snapshot);

        log.info("Universe refreshed for {}: {} codes (pinned={}, fallback={}, rank={})",
            tradingDate, result.size(), snapshot.pinned, snapshot.fallback, snapshot.rankApi);
        TradeEvents.event("UNIVERSE_BUILT")
            .with("tradingDate", tradingDate)
            .with("count", result.size())
            .with("pinned", snapshot.pinned)
            .with("fallback", snapshot.fallback)
            .with("rank", snapshot.rankApi)
            .log();

        return snapshot;
    }

    /**
     * 거래량순위 상위 N 종목코드. rank-api-top &le; 0 이거나 API 실패 시 빈 리스트.
     */
    private List<String> fetchRankCodes(int rankApiTop) {
        if (rankApiTop <= 0) {
            return List.of();
        }
        try {
            List<String> codes = apiClient.getTopVolumeCodes(rankApiTop);
            return codes != null ? codes : List.of();
        } catch (Exception e) {
            log.warn("Rank API(거래량순위) 실패 → fallback-codes 로 폴백: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 캐시된 유니버스 (없으면 즉시 refresh).
     */
    public Snapshot currentUniverse(LocalDate tradingDate) {
        Snapshot s = latest.get();
        if (s == null || !s.tradingDate.equals(tradingDate) || s.codes.isEmpty()) {
            return refresh(tradingDate);
        }
        return s;
    }

    public record Snapshot(LocalDate tradingDate, List<String> codes,
                            int pinned, int fallback, int rankApi) {}
}
