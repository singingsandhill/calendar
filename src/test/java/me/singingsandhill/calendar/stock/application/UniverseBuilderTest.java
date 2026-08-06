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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UniverseBuilder 동작 회귀 테스트.
 *
 * 기대 (ADR stock/algorithm/0010 — 부분 응답 임계):
 *  - rank-api-top &gt; 0 이고 거래량순위가 <b>요청한 top-N 을 모두 반환</b>하면 동적 유니버스로 사용 (pinned ∪ rank).
 *  - 거래량순위가 top-N 에 <b>미달</b>하면(0건 포함) 정적 fallback-codes 를 합집합으로 보강.
 *  - 동일 코드는 중복 제거.
 *  - 캐싱: 같은 거래일에 두 번 호출하면 같은 Snapshot.
 */
class UniverseBuilderTest {

    private final KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);

    /** 요청 수를 모두 채운 rank 응답 생성 (완전 응답 = 폴백 미사용 조건). */
    private static List<String> rankCodes(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> String.format("%06d", 100000 + i))
            .toList();
    }

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
    void usesVolumeRankWhenResponseIsComplete() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of("111111", "222222"));
        UniverseBuilder b = builder(List.of("005930"), List.of("999999"), 2);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));

        // pinned ∪ rank; rank 가 요청 수(2)를 모두 채웠으므로 정적 fallback(999999) 은 섞이지 않는다.
        assertThat(snap.codes()).containsExactly("005930", "111111", "222222");
        assertThat(snap.rankApi()).isEqualTo(2);
        assertThat(snap.fallback()).isZero();
    }

    /**
     * 2026-08-03 사고 재현: 거래량순위가 30건 요청에 1건만 반환.
     *
     * 과거 계약(`rankCodes.isEmpty()`)에서는 1건이 "비어 있지 않다"는 이유로 70종목 정적 안전망이
     * 통째로 스킵돼 하루치 유니버스가 1종목이 됐다. 부분 응답은 폴백을 <b>대체</b>하지 않고
     * <b>합집합</b>이어야 한다.
     */
    @Test
    void unionsStaticPoolWhenRankResponseIsPartial() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of("252670"));
        UniverseBuilder b = builder(List.of(), List.of("000660", "035420"), 30);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 8, 3));

        assertThat(snap.codes()).containsExactly("252670", "000660", "035420");
        assertThat(snap.rankApi()).isEqualTo(1);
        assertThat(snap.fallback()).isEqualTo(2);
    }

    @Test
    void deduplicatesRankAgainstPinned() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of("005930", "111111"));
        UniverseBuilder b = builder(List.of("005930"), List.of(), 30);

        UniverseBuilder.Snapshot snap = b.refresh(LocalDate.of(2026, 5, 1));

        assertThat(snap.codes()).containsExactly("005930", "111111");
        assertThat(snap.rankApi()).isEqualTo(2);
    }

    /** 프리마켓(08:30)은 당일 거래량이 없어 거래량순위가 무의미 — 정적 소스만으로 스냅샷. */
    @Test
    void refreshStaticOnlySkipsRankApiEntirely() {
        UniverseBuilder b = builder(List.of("005930"), List.of("000660", "035420"), 30);

        UniverseBuilder.Snapshot snap = b.refreshStaticOnly(LocalDate.of(2026, 5, 1));

        assertThat(snap.codes()).containsExactly("005930", "000660", "035420");
        assertThat(snap.rankApi()).isZero();
        assertThat(snap.fallback()).isEqualTo(2);
        verify(api, never()).getTopVolumeCodes(anyInt());
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

    // ===== 스크리닝 시점 rank 재시도 (refreshIfDegraded) =====

    @Test
    void retriesRankAtScreeningWhenSnapshotIsFallbackOnly() {
        // 08:30 pre-market: 장 시작 전이라 거래량순위 0건 → 폴백 스냅샷
        when(api.getTopVolumeCodes(anyInt()))
            .thenReturn(List.of())
            .thenReturn(List.of("111111", "222222"));
        UniverseBuilder b = builder(List.of("005930"), List.of("000660"), 2);
        LocalDate day = LocalDate.of(2026, 5, 1);
        b.refresh(day);

        // 09:20 스크리닝: rank=0 이었으므로 재조회 → 완전 응답이라 동적 유니버스로 대체
        UniverseBuilder.Snapshot snap = b.refreshIfDegraded(day);

        assertThat(snap.codes()).containsExactly("005930", "111111", "222222");
        assertThat(snap.rankApi()).isEqualTo(2);
        assertThat(snap.fallback()).isZero();
    }

    /**
     * 2026-08-03 사고 재현 2: 08:30 스냅샷의 rank 가 1건(≠0)이면 과거 계약에서는
     * `rankApi == 0` 이 아니라는 이유로 09:20 재시도(ADR-0006)마저 발동하지 않았다.
     */
    @Test
    void retriesRankAtScreeningWhenSnapshotRankIsPartial() {
        when(api.getTopVolumeCodes(anyInt()))
            .thenReturn(List.of("252670"))
            .thenReturn(rankCodes(30));
        UniverseBuilder b = builder(List.of(), List.of("000660"), 30);
        LocalDate day = LocalDate.of(2026, 8, 3);
        b.refresh(day);

        UniverseBuilder.Snapshot snap = b.refreshIfDegraded(day);

        verify(api, times(2)).getTopVolumeCodes(anyInt());
        assertThat(snap.rankApi()).isEqualTo(30);
        assertThat(snap.fallback()).isZero();
        assertThat(snap.codes()).hasSize(30).doesNotContain("000660");
    }

    @Test
    void keepsFallbackWhenRetryAlsoReturnsEmpty() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of());
        UniverseBuilder b = builder(List.of(), List.of("000660"), 30);
        LocalDate day = LocalDate.of(2026, 5, 1);
        b.refresh(day);

        UniverseBuilder.Snapshot snap = b.refreshIfDegraded(day);

        // 재시도도 0건이면 기존과 동일하게 정적 폴백 (무회귀)
        assertThat(snap.codes()).containsExactly("000660");
        assertThat(snap.rankApi()).isZero();
        assertThat(snap.fallback()).isEqualTo(1);
    }

    @Test
    void doesNotRetryWhenRankResponseIsComplete() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(rankCodes(30));
        UniverseBuilder b = builder(List.of(), List.of("000660"), 30);
        LocalDate day = LocalDate.of(2026, 5, 1);
        UniverseBuilder.Snapshot first = b.refresh(day);

        UniverseBuilder.Snapshot snap = b.refreshIfDegraded(day);

        // rank 가 요청 수를 모두 채웠으면 스냅샷 유지 (거래일 1회 스냅샷 정합성)
        assertThat(snap).isSameAs(first);
        verify(api, times(1)).getTopVolumeCodes(anyInt());
    }

    @Test
    void doesNotRetryWhenRankDisabled() {
        UniverseBuilder b = builder(List.of("005930"), List.of("000660"), 0);
        LocalDate day = LocalDate.of(2026, 5, 1);
        UniverseBuilder.Snapshot first = b.refresh(day);

        UniverseBuilder.Snapshot snap = b.refreshIfDegraded(day);

        assertThat(snap).isSameAs(first);
        verify(api, never()).getTopVolumeCodes(anyInt());
    }

    @Test
    void buildsFreshWhenNoSnapshotForDate() {
        when(api.getTopVolumeCodes(anyInt())).thenReturn(List.of("111111"));
        UniverseBuilder b = builder(List.of(), List.of("000660"), 1);

        UniverseBuilder.Snapshot snap = b.refreshIfDegraded(LocalDate.of(2026, 5, 2));

        assertThat(snap.tradingDate()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(snap.codes()).containsExactly("111111");
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
