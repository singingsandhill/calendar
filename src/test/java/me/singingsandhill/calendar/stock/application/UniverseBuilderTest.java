package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.service.UniverseBuilder;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UniverseBuilder 동작 회귀 테스트.
 *
 * 기대:
 *  - pinned + fallback 합집합이 코드 리스트로 반환된다.
 *  - 동일 코드는 중복 제거.
 *  - 캐싱: 같은 거래일에 두 번 호출하면 같은 Snapshot.
 */
class UniverseBuilderTest {

    private UniverseBuilder builder(List<String> pinned, List<String> fallback) {
        StockProperties props = new StockProperties();
        props.getUniverse().setPinned(pinned);
        props.getUniverse().setFallbackCodes(fallback);
        return new UniverseBuilder(props);
    }

    @Test
    void buildsUnionOfPinnedAndFallback() {
        UniverseBuilder b = builder(List.of("005930"), List.of("000660", "035420"));
        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));
        assertThat(snap.codes()).containsExactly("005930", "000660", "035420");
        assertThat(snap.pinned()).isEqualTo(1);
        assertThat(snap.fallback()).isEqualTo(2);
    }

    @Test
    void deduplicatesAcrossSources() {
        UniverseBuilder b = builder(List.of("005930"), List.of("005930", "000660"));
        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));
        assertThat(snap.codes()).containsExactly("005930", "000660");
    }

    @Test
    void cachesSnapshotForSameTradingDate() {
        UniverseBuilder b = builder(List.of("005930"), List.of("000660"));
        LocalDate day = LocalDate.of(2026, 5, 1);
        UniverseBuilder.Snapshot a = b.refresh(day);
        UniverseBuilder.Snapshot c = b.currentUniverse(day);
        assertThat(c).isSameAs(a);
    }

    @Test
    void rebuildsWhenTradingDateChanges() {
        UniverseBuilder b = builder(List.of("005930"), List.of("000660"));
        UniverseBuilder.Snapshot a = b.refresh(LocalDate.of(2026, 5, 1));
        UniverseBuilder.Snapshot c = b.currentUniverse(LocalDate.of(2026, 5, 2));
        assertThat(c).isNotSameAs(a);
        assertThat(c.tradingDate()).isEqualTo(LocalDate.of(2026, 5, 2));
    }
}
