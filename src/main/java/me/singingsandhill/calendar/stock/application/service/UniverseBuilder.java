package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
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
 *   2) PR-3 다음 단계: KIS 등락률/거래량 순위 API 결과 (현재는 비어있음, 자리만 마련)
 *   3) yml 의 fallback 종목 (KIS 순위 API 도입 전 임시 풀)
 *
 * executePreMarketLoop 에서 refresh() 가 호출되어 그날의 유니버스를 캐시한다.
 * ScreeningService 는 currentUniverse() 로 동기 조회한다.
 */
@Service
public class UniverseBuilder {

    private static final Logger log = LoggerFactory.getLogger(UniverseBuilder.class);

    private final StockProperties stockProperties;
    private final AtomicReference<Snapshot> latest = new AtomicReference<>();

    public UniverseBuilder(StockProperties stockProperties) {
        this.stockProperties = stockProperties;
    }

    public Snapshot refresh(LocalDate tradingDate) {
        StockProperties.Universe universe = stockProperties.getUniverse();

        Set<String> codes = new LinkedHashSet<>();
        List<String> pinned = universe.getPinned();
        codes.addAll(pinned);

        // 자리: KIS 순위 API. 추후 KisRestClient.getTopGainers / getTopVolume 추가 시 주입.
        // 현재는 fallback-codes 로 대체.
        codes.addAll(universe.getFallbackCodes());

        List<String> result = new ArrayList<>(codes);
        Snapshot snapshot = new Snapshot(tradingDate, result, pinned.size(), universe.getFallbackCodes().size(), 0);
        latest.set(snapshot);

        log.info("Universe refreshed for {}: {} codes (pinned={}, fallback={}, rank=0)",
            tradingDate, result.size(), snapshot.pinned, snapshot.fallback);
        TradeEvents.event("UNIVERSE_BUILT")
            .with("tradingDate", tradingDate)
            .with("count", result.size())
            .with("pinned", snapshot.pinned)
            .with("fallback", snapshot.fallback)
            .log();

        return snapshot;
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
