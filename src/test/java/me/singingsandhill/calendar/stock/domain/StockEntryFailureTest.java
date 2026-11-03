package me.singingsandhill.calendar.stock.domain;

import me.singingsandhill.calendar.stock.domain.stock.EntryOutcome;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진입 실패 상태·사유 (ADR stock/infrastructure/0009).
 *
 * 사고 이력 (2026-08-03 ~ 10-20): 진입에 실패한 종목이 ENTRY_READY 에 남아 11:20 까지 5초마다
 * 시세·잔고·매수가능 API 3회를 반복했고(종목당 최대 ~1,200틱), 실패 사유는 로그 이벤트뿐이라
 * DB 로 원인을 볼 수 없었다.
 */
class StockEntryFailureTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 21, 9, 40, 0);

    private Stock entryReady() {
        Stock stock = new Stock("005930", "삼성전자", LocalDate.of(2026, 10, 21));
        stock.setOpenPrice(new BigDecimal("98000"));
        stock.setCurrentPrice(new BigDecimal("100000"));
        stock.recordHighFormed(new BigDecimal("101000"));
        stock.recordPullbackStart(new BigDecimal("99800"));
        stock.markEntryReady();
        return stock;
    }

    @Test
    void failureIsCountedWithReasonAndTime() {
        Stock stock = entryReady();

        boolean failed = stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, T0, 3);

        assertThat(failed).isFalse();
        assertThat(stock.getEntryFailCount()).isEqualTo(1);
        assertThat(stock.getLastEntryFailReason()).isEqualTo(EntryOutcome.SIZE_ZERO);
        assertThat(stock.getLastEntryFailAt()).isEqualTo(T0);
        assertThat(stock.getState()).isEqualTo(StockState.ENTRY_READY);
    }

    @Test
    void reachingMaxAttemptsMovesToEntryFailed() {
        Stock stock = entryReady();

        stock.recordEntryFailure(EntryOutcome.QUOTE_UNAVAILABLE, T0, 3);
        stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, T0.plusSeconds(30), 3);
        boolean failed = stock.recordEntryFailure(EntryOutcome.NOT_TRADABLE, T0.plusSeconds(60), 3);

        assertThat(failed).isTrue();
        assertThat(stock.getEntryFailCount()).isEqualTo(3);
        assertThat(stock.getState()).isEqualTo(StockState.ENTRY_FAILED);
        assertThat(stock.getLastEntryFailReason()).isEqualTo(EntryOutcome.NOT_TRADABLE);
    }

    @Test
    void unconfirmedOrderIsRecordedButNotCountedAndKeepsState() {
        // 접수 여부 불명 — 판정은 스윕(reconcileUnconfirmedOrders) 몫이다.
        Stock stock = entryReady();

        stock.recordEntryFailure(EntryOutcome.ORDER_UNCONFIRMED, T0, 1);

        assertThat(stock.getEntryFailCount()).isZero();
        assertThat(stock.getState()).isEqualTo(StockState.ENTRY_READY);
        assertThat(stock.getLastEntryFailReason()).isEqualTo(EntryOutcome.ORDER_UNCONFIRMED);
        assertThat(stock.getLastEntryFailAt()).isEqualTo(T0);
    }

    @Test
    void retryIsDueOnlyAfterInterval() {
        Stock stock = entryReady();
        assertThat(stock.isEntryRetryDue(T0, 30)).isTrue();   // 실패 이력 없음

        stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, T0, 3);

        assertThat(stock.isEntryRetryDue(T0.plusSeconds(5), 30)).isFalse();
        assertThat(stock.isEntryRetryDue(T0.plusSeconds(29), 30)).isFalse();
        assertThat(stock.isEntryRetryDue(T0.plusSeconds(30), 30)).isTrue();
    }

    @Test
    void entryFailedIsTerminalAndNotActive() {
        assertThat(StockState.ENTRY_FAILED.isActive()).isFalse();
        assertThat(StockState.ENTRY_FAILED.canEnter()).isFalse();
        assertThat(StockState.ENTRY_FAILED.isTerminal()).isTrue();
    }
}
