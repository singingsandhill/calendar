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
        return refresh(tradingDate, true);
    }

    /**
     * 거래량순위를 조회하지 않고 정적 소스(pinned ∪ fallback)만으로 스냅샷을 만든다.
     *
     * 프리마켓(08:30) 전용 — 그 시각엔 당일 거래량이 아직 없어 거래량순위가 구조적으로 0~1건이고,
     * 그 결과가 스냅샷에 남으면 하루치 유니버스를 오염시킨다. 동적 소스는 장중 거래량이 쌓인
     * 스크리닝(09:20) 시점의 {@link #refreshIfDegraded}에만 맡긴다.
     */
    public Snapshot refreshStaticOnly(LocalDate tradingDate) {
        return refresh(tradingDate, false);
    }

    private Snapshot refresh(LocalDate tradingDate, boolean useRankApi) {
        StockProperties.Universe universe = stockProperties.getUniverse();

        Set<String> codes = new LinkedHashSet<>();
        List<String> pinned = universe.getPinned();
        codes.addAll(pinned);

        // KIS 거래량순위 동적 소스 (rank-api-top > 0 일 때). 어떤 실패든 빈 리스트 → 폴백.
        List<String> rankCodes = useRankApi ? fetchRankCodes(universe.getRankApiTop()) : List.of();
        codes.addAll(rankCodes);

        // 폴백: rank 가 요청한 top-N 에 미달하면(비활성/실패/0건/부분응답) 정적 fallback-codes 를
        // 합집합으로 보강한다 (ADR stock/algorithm/0010). 부분 응답은 fallback 을 대체하지 않는다 —
        // 2026-08-03 에 1건 응답이 "비어 있지 않다"는 이유로 70종목 안전망을 통째로 껐다.
        // rank 비활성(rank-api-top=0)이면 Math.max 로 0 < 1 이 되어 기존 폴백 동작을 그대로 유지.
        boolean usedFallback = rankCodes.size() < Math.max(universe.getRankApiTop(), 1);
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

        // 축소 응답 경보: 요청보다 적게 받은 것은 정상 동작이 아니다. 08-03 엔 이 사실이
        // KisRestClient 의 DEBUG 한 줄로만 남아 하루가 지나도록 아무도 알지 못했다.
        if (useRankApi && universe.getRankApiTop() > 0 && rankCodes.size() < universe.getRankApiTop()) {
            log.warn("거래량순위 축소 응답: {}/{} 건 — 정적 fallback {} 종목으로 보강함. KIS 응답/파라미터 점검 필요.",
                rankCodes.size(), universe.getRankApiTop(), universe.getFallbackCodes().size());
            TradeEvents.event("UNIVERSE_DEGRADED")
                .with("tradingDate", tradingDate)
                .with("requested", universe.getRankApiTop())
                .with("returned", rankCodes.size())
                .with("fallback", fallbackCount)
                .warn();
        }

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
     * 스크리닝 시점(09:20) 재조회: 스냅샷의 rank 가 요청한 top-N 에 미달하면 거래량순위를 다시 시도한다.
     *
     * 08:30 pre-market 에는 당일 거래량이 없어 거래량순위가 0~1건이므로 (운영 로그
     * "Volume-rank returned 0 codes" 매일 반복, 2026-08-03 엔 1건), 장 시작 후 20분치 거래량이
     * 쌓인 스크리닝 시점에 재조회해야 동적 유니버스가 실제로 동작한다. rank 가 요청 수를 모두
     * 채웠으면 기존 스냅샷을 유지해 "거래일 1회 스냅샷" 정합성(ADR-0002)을 지킨다.
     *
     * 판정 기준이 {@code rankApi == 0} 이 아니라 <b>미달</b>인 이유는 ADR stock/algorithm/0010 참고 —
     * 1건 응답이 0 이 아니라는 이유로 이 재시도가 통째로 건너뛰어졌던 사고가 있다.
     */
    public Snapshot refreshIfDegraded(LocalDate tradingDate) {
        Snapshot s = latest.get();
        if (s == null || !s.tradingDate.equals(tradingDate) || s.codes.isEmpty()) {
            return refresh(tradingDate);
        }
        int rankApiTop = stockProperties.getUniverse().getRankApiTop();
        if (s.rankApi < rankApiTop) {
            log.info("Universe snapshot is degraded (rank={}/{}) — retrying volume-rank at screening time",
                s.rankApi, rankApiTop);
            return refresh(tradingDate);
        }
        return s;
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
