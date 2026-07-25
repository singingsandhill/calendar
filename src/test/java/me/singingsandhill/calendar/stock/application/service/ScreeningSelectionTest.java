package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 후보 선정 규칙 회귀 테스트 (2026-07-24 리뷰 §5 / P2-2).
 *
 * 기존 문제:
 *  - `selected.size() < minCandidates || score >= threshold` 라서 점수 미달 종목도 상위 3개는
 *    강제 선정됐다 — 엣지 없는 날에도 매일 진입을 시도하는 구조.
 *  - 가중치가 유동성 팩터(거래대금 20 + 스프레드 15 + 시총 10 = 45)만으로 임계 40 을 넘겨,
 *    갭·체결강도 신호가 사실상 없어도 통과할 수 있었다.
 */
class ScreeningSelectionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

    private ScreeningService serviceWith(BigDecimal signalMinScore) {
        StockProperties props = new StockProperties();
        props.getScoring().setSignalMinScore(signalMinScore);
        return new ScreeningService(null, null, null, props, null);
    }

    /** gapScore + strengthScore 가 signal, 나머지는 유동성 팩터. */
    private ScreeningService.StockCandidate candidate(String code, double total,
                                                       double gapScore, double strengthScore) {
        Stock stock = new Stock(code, code, TODAY);
        return new ScreeningService.StockCandidate(stock, BigDecimal.valueOf(total),
            BigDecimal.valueOf(gapScore), BigDecimal.valueOf(strengthScore),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    void doesNotForceSelectionWhenAllCandidatesAreBelowThreshold() {
        List<ScreeningService.StockCandidate> sorted = List.of(
            candidate("A", 30, 20, 10),
            candidate("B", 25, 18, 7),
            candidate("C", 20, 15, 5));

        assertThat(serviceWith(BigDecimal.ZERO).selectCandidates(sorted)).isEmpty();
    }

    @Test
    void selectsOnlyCandidatesClearingScoreThreshold() {
        List<ScreeningService.StockCandidate> sorted = List.of(
            candidate("A", 55, 30, 10),
            candidate("B", 45, 25, 8),
            candidate("C", 20, 15, 5));

        assertThat(serviceWith(BigDecimal.ZERO).selectCandidates(sorted))
            .extracting(c -> c.stock().getStockCode())
            .containsExactly("A", "B");
    }

    @Test
    void rejectsLiquidityOnlyCandidateWithoutSignal() {
        // 총점 50 이지만 신호(갭+체결강도) 합이 8 뿐 — 거래대금·시총만 큰 무신호 종목
        List<ScreeningService.StockCandidate> sorted = List.of(
            candidate("LIQUID", 50, 5, 3),
            candidate("SIGNAL", 48, 28, 9));

        assertThat(serviceWith(new BigDecimal("25")).selectCandidates(sorted))
            .extracting(c -> c.stock().getStockCode())
            .containsExactly("SIGNAL");
    }

    @Test
    void respectsMaxWatchlistSize() {
        StockProperties props = new StockProperties();
        props.getScreening().setMaxWatchlistSize(2);
        props.getScoring().setSignalMinScore(BigDecimal.ZERO);
        ScreeningService service = new ScreeningService(null, null, null, props, null);

        List<ScreeningService.StockCandidate> sorted = List.of(
            candidate("A", 60, 30, 10),
            candidate("B", 55, 28, 9),
            candidate("C", 50, 26, 8));

        assertThat(service.selectCandidates(sorted)).hasSize(2);
    }
}
