package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 스크리닝 탈락 사유 버킷 분리 회귀 테스트.
 *
 * 과거에는 {@code dataInsufficient} 하나에 "시가 미확정"과 "체결강도 미집계"가, {@code gapFiltered}
 * 하나에 "하한 미달"과 "상한 초과"가 합산됐다. 그래서 2026-07-20~23 의 4일 연속 0건 선정
 * (매일 {@code gapFiltered + dataInsufficient == 70}) 에서 로그만으로 근본 원인을 특정할 수 없었다.
 *
 * 원인이 다르면 조치도 다르다 — 체결강도 0 은 데이터 소스 문제(ADR infrastructure/0007),
 * 갭 하한 미달은 유니버스 문제, 갭 상한 초과는 임계 문제다.
 */
class ScreeningStatsBucketTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    /** 시가/전일종가로 갭을 만드는 시세 응답 (시총 1000억, 거래대금 100억 — 다른 Floor 는 통과). */
    private static KisQuoteResponse quote(BigDecimal openPrice, BigDecimal prevClose) {
        return new KisQuoteResponse("005930",
            openPrice, openPrice, openPrice, openPrice, prevClose,
            BigDecimal.ZERO, BigDecimal.ZERO, 1_000_000L,
            new BigDecimal("10000000000"), new BigDecimal("100000000000"), new BigDecimal("1.0"));
    }

    private static ScreeningService serviceWith(KoreaInvestmentApiClient api, StockBotMetrics metrics) {
        return new ScreeningService(
            mock(StockRepository.class), mock(StockSignalRepository.class),
            api, new StockProperties(), metrics);
    }

    @Test
    void zeroTradeStrengthIsCountedSeparatelyFromMissingOpenPrice() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        // 갭 4% 로 Floor 2 통과 → 체결강도 조회 → 미집계(null)
        when(api.getQuote(anyString())).thenReturn(quote(new BigDecimal("10400"), new BigDecimal("10000")));
        when(api.getTradeStrength(anyString())).thenReturn(null);
        StockBotMetrics metrics = mock(StockBotMetrics.class);

        serviceWith(api, metrics).executeScreening(TODAY, List.of("005930"));

        // total=1, floorPassed=0, selected=0, openPriceMissing=0, zeroStrength=1, gapBelow=0, gapAbove=0
        verify(metrics).recordScreeningResult(1, 0, 0, 0, 1, 0, 0);
    }

    @Test
    void missingOpenPriceIsCountedSeparatelyFromZeroTradeStrength() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        when(api.getQuote(anyString())).thenReturn(quote(BigDecimal.ZERO, new BigDecimal("10000")));
        StockBotMetrics metrics = mock(StockBotMetrics.class);

        serviceWith(api, metrics).executeScreening(TODAY, List.of("005930"));

        verify(metrics).recordScreeningResult(1, 0, 0, 1, 0, 0, 0);
        // 시가가 없으면 갭을 계산할 수 없으므로 체결강도까지 가지 않는다 (콜 예산)
        verify(api, org.mockito.Mockito.never()).getTradeStrength(anyString());
    }

    @Test
    void gapBelowFloorAndAboveCeilingAreCountedSeparately() {
        StockProperties props = new StockProperties();
        BigDecimal prevClose = new BigDecimal("10000");

        KoreaInvestmentApiClient below = mock(KoreaInvestmentApiClient.class);
        // 갭 0% < floor-gap-percent(0.5)
        when(below.getQuote(anyString())).thenReturn(quote(prevClose, prevClose));
        StockBotMetrics belowMetrics = mock(StockBotMetrics.class);
        serviceWith(below, belowMetrics).executeScreening(TODAY, List.of("005930"));
        verify(belowMetrics).recordScreeningResult(1, 0, 0, 0, 0, 1, 0);

        KoreaInvestmentApiClient above = mock(KoreaInvestmentApiClient.class);
        // 갭 20% > scoring.floor-max-gap (Java 기본 15)
        when(above.getQuote(anyString())).thenReturn(quote(new BigDecimal("12000"), prevClose));
        StockBotMetrics aboveMetrics = mock(StockBotMetrics.class);
        serviceWith(above, aboveMetrics).executeScreening(TODAY, List.of("005930"));
        verify(aboveMetrics).recordScreeningResult(1, 0, 0, 0, 0, 0, 1);

        // 위 두 케이스가 실효 임계를 쓰는지 확인 (yaml 의 legacy max-gap-percent 가 아니라)
        org.assertj.core.api.Assertions.assertThat(props.getScoring().getFloorMaxGap())
            .isEqualByComparingTo(new BigDecimal("15"));
    }

    @Test
    void snapshotKeepsBackwardCompatibleTotals() {
        StockBotMetrics metrics = new StockBotMetrics();
        metrics.recordScreeningResult(70, 0, 0, 3, 60, 5, 2);

        StockBotMetrics.ScreeningSnapshot snap = metrics.getLastScreeningResult();
        org.assertj.core.api.Assertions.assertThat(snap.dataInsufficient()).isEqualTo(63);
        org.assertj.core.api.Assertions.assertThat(snap.gapFiltered()).isEqualTo(7);
    }
}
