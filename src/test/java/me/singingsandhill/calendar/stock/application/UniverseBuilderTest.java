package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.service.UniverseBuilder;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UniverseBuilder 동작 회귀 테스트.
 *
 * 기대:
 *  - rank-api-top &gt; 0 이면 KIS 거래량순위 결과를 동적 유니버스로 사용 (pinned ∪ rank).
 *  - 거래량순위가 비었거나(0건) API 가 실패하면 정적 fallback-codes 로 폴백 (무회귀).
 *  - 동일 코드는 중복 제거.
 *  - 캐싱: 같은 거래일에 두 번 호출하면 같은 Snapshot.
 */
class UniverseBuilderTest {

    private final KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);

    /** rank 비활성(rank-api-top=0) 빌더 — 정적 fallback 경로 테스트용. */
    private UniverseBuilder builder(List<String> pinned, List<String> fallback) {
        return builder(pinned, fallback, 0);
    }

    private UniverseBuilder builder(List<String> pinned, List<String> fallback, int rankTop) {
        StockProperties props = new StockProperties();
        props.getUniverse().setPinned(pinned);
        props.getUniverse().setFallbackCodes(fallback);
        props.getUniverse().setRankApiTop(rankTop);
        return new UniverseBuilder(props, api);
    }

    // ===== 정적 fallback 경로 (rank 비활성) =====

    @Test
    void buildsUnionOfPinnedAndFallbackWhenRankDisabled() {
        UniverseBuilder b = builder(List.of("005930"), List.of("000660", "035420"));
        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));
        assertThat(snap.codes()).containsExactly("005930", "000660", "035420");
        assertThat(snap.pinned()).isEqualTo(1);
        assertThat(snap.fallback()).isEqualTo(2);
        assertThat(snap.rankApi()).isZero();
    }

    @Test
    void deduplicatesAcrossSources() {
        UniverseBuilder b = builder(List.of("005930"), List.of("005930", "000660"));
        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));
        assertThat(snap.codes()).containsExactly("005930", "000660");
    }

    // ===== 동적 거래량순위 경로 (rank 활성) =====

    @Test
    void usesVolumeRankWhenEnabled() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of("111111", "222222"));
        UniverseBuilder b = builder(List.of("005930"), List.of("999999"), 30);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));

        // pinned ∪ rank; rank 성공 시 정적 fallback(999999) 은 섞이지 않는다.
        assertThat(snap.codes()).containsExactly("005930", "111111", "222222");
        assertThat(snap.rankApi()).isEqualTo(2);
        assertThat(snap.fallback()).isZero();
    }

    @Test
    void deduplicatesRankAgainstPinned() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of("005930", "111111"));
        UniverseBuilder b = builder(List.of("005930"), List.of(), 30);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));

        assertThat(snap.codes()).containsExactly("005930", "111111");
        assertThat(snap.rankApi()).isEqualTo(2);
    }

    @Test
    void fallsBackToStaticPoolWhenRankEmpty() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of());
        UniverseBuilder b = builder(List.of("005930"), List.of("000660", "035420"), 30);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));

        assertThat(snap.codes()).containsExactly("005930", "000660", "035420");
        assertThat(snap.rankApi()).isZero();
        assertThat(snap.fallback()).isEqualTo(2);
    }

    @Test
    void fallsBackWhenRankApiThrows() {
        when(api.getTopVolumeCodes(anyInt())).thenThrow(new RuntimeException("KIS 5xx"));
        UniverseBuilder b = builder(List.of(), List.of("000660"), 30);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));

        // 예외가 전파되지 않고 정적 폴백 사용.
        assertThat(snap.codes()).containsExactly("000660");
        assertThat(snap.rankApi()).isZero();
        assertThat(snap.fallback()).isEqualTo(1);
    }

    // ===== 스냅샷 캐싱 =====

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
