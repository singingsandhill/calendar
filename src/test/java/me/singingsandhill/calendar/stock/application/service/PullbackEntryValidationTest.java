package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 진입 검증 조건 3(눌림목 지속시간)의 null 처리 회귀 테스트.
 *
 * 사고 이력 (수익성 검사 2026-07-25): pullbackStartAt 미기록이면 조건 3이 자동 PASS 라
 * soft 2/3 검증의 실질 문턱이 1개로 내려갔다. 데이터 부족은 조건 1(체결강도)·2(호가
 * 불균형)와 동일하게 FAIL 이어야 한다.
 */
class PullbackEntryValidationTest {

    private final StockRepository stockRepository = mock(StockRepository.class);
    private final StockSignalRepository signalRepository = mock(StockSignalRepository.class);
    private final KoreaInvestmentApiClient kisApiClient = mock(KoreaInvestmentApiClient.class);
    private final EntryAttemptRepository entryAttemptRepository = mock(EntryAttemptRepository.class);

    private final PullbackDetectionService service = new PullbackDetectionService(
        stockRepository, signalRepository, kisApiClient, new StockProperties(), entryAttemptRepository);

    /** PULLBACK 상태 종목 — pullbackStartAt 은 파라미터로 제어 (미기록 행은 null). */
    private Stock pullbackStock(LocalDateTime pullbackStartAt) {
        Stock stock = new Stock("005930", "삼성전자", LocalDate.of(2026, 7, 24));
        stock.setOpenPrice(new BigDecimal("98000"));
        stock.setCurrentPrice(new BigDecimal("99800"));
        stock.updateState(StockState.PULLBACK);
        stock.restorePersistedState(new BigDecimal("101000"), LocalDateTime.now().minusMinutes(10),
            new BigDecimal("99800"), pullbackStartAt, null);
        return stock;
    }

    /** 반등 +0.2% 충족(99800→100000), 체결강도 PASS, 호가 불균형 FAIL(orderbook null). */
    private void stubBounceTick() {
        when(kisApiClient.getQuote("005930")).thenReturn(quoteAt(new BigDecimal("100000")));
        when(kisApiClient.getTradeStrength("005930")).thenReturn(new BigDecimal("120"));
        when(kisApiClient.getOrderbook("005930")).thenReturn(null);
    }

    @Test
    void missingPullbackStartAt_isFailNotFreePass() {
        stubBounceTick();
        Stock stock = pullbackStock(null);

        service.updateStockState(stock);

        // 통과는 체결강도 1개뿐 (시간 조건은 데이터 없음 = FAIL) → 2/3 미달, 진입 불가
        assertThat(stock.getState()).isEqualTo(StockState.PULLBACK);
    }

    @Test
    void validPullbackDuration_stillEnters() {
        stubBounceTick();
        Stock stock = pullbackStock(LocalDateTime.now().minusMinutes(10)); // 3~15분 범위 내

        service.updateStockState(stock);

        // 체결강도 + 시간 = 2/3 충족 → 정상 경로는 그대로 진입
        assertThat(stock.getState()).isEqualTo(StockState.ENTRY_READY);
    }

    private static KisQuoteResponse quoteAt(BigDecimal price) {
        return new KisQuoteResponse("005930", price, new BigDecimal("98000"),
            new BigDecimal("101000"), new BigDecimal("97000"), new BigDecimal("97500"),
            BigDecimal.ZERO, BigDecimal.ZERO, 1_000_000L, new BigDecimal("1000000000"),
            new BigDecimal("300000000000"), BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("120"));
    }
}
