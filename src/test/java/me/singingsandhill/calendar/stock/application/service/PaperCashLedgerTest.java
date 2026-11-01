package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeRepository;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAPER 가상 예수금 원장 (ADR stock/modes/0004).
 *
 * 사고 이력 (2026-08-03 ~ 10-20, 51거래일): PAPER 사이징이 실계좌 예수금(0원)을 읽어 수량 0 →
 * openPosition 이 기록 없이 null 을 돌려 PAPER 실측이 0체결이었다. 원장은 거래일마다
 * paper-cash 에서 시작해 그날의 시뮬레이션 체결(SIM-) 금액·비용을 DB 에서 매번 다시 계산한다 —
 * 인메모리 잔고가 없으므로 재시작해도 같은 값이 나온다.
 */
class PaperCashLedgerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TRADING_DATE = LocalDate.of(2026, 10, 21);
    private static final Clock CLOCK = Clock.fixed(
        TRADING_DATE.atTime(10, 0).atZone(KST).toInstant(), KST);

    private final StockTradeRepository tradeRepository = mock(StockTradeRepository.class);

    private PaperCashLedger ledger() {
        StockProperties props = new StockProperties();
        props.getBot().setPaperCash(new BigDecimal("10000000"));
        return new PaperCashLedger(tradeRepository, props, CLOCK);
    }

    private static StockTrade filledBuy(String orderId, int qty, String price, String fee) {
        StockTrade t = StockTrade.createBuyOrder(orderId, "005930", qty, new BigDecimal(price), true);
        t.markFilled(new BigDecimal(price), qty, new BigDecimal(fee));
        return t;
    }

    private static StockTrade filledSell(String orderId, int qty, String price, String fee) {
        StockTrade t = StockTrade.createSellOrder(orderId, "005930", qty, new BigDecimal(price), true,
            StockCloseReason.TP2);
        t.markFilled(new BigDecimal(price), qty, new BigDecimal(fee));
        return t;
    }

    private void tradesToday(StockTrade... trades) {
        when(tradeRepository.findByOrderedAtBetween(any(), any())).thenReturn(List.of(trades));
    }

    @Test
    void startsFromConfiguredPaperCashWhenNothingTradedToday() {
        tradesToday();

        assertThat(ledger().available(TRADING_DATE)).isEqualByComparingTo("10000000");
    }

    @Test
    void entryDeductsEntryAmountPlusBuyFee() {
        // 10주 × 100,200 = 1,002,000 + 매수 수수료 150.30
        tradesToday(filledBuy("SIM-1", 10, "100200", "150.30"));

        assertThat(ledger().available(TRADING_DATE)).isEqualByComparingTo("8997849.70");
    }

    @Test
    void partialExitAddsProceedsNetOfSellFeeAndTax() {
        // 매도 5주 × 102,700 = 513,500, 매도 비용(수수료+세금) 1,104.025
        tradesToday(
            filledBuy("SIM-1", 10, "100200", "150.30"),
            filledSell("SIM-2", 5, "102700", "1104.025"));

        assertThat(ledger().available(TRADING_DATE)).isEqualByComparingTo("9510245.675");
    }

    @Test
    void fullExitLeavesOnlyRealizedResultAndCosts() {
        tradesToday(
            filledBuy("SIM-1", 10, "100200", "150.30"),
            filledSell("SIM-2", 5, "102700", "1104.025"),
            filledSell("SIM-3", 5, "99800", "1072.85"));

        // 10,000,000 − 1,002,150.30 + (513,500 − 1,104.025) + (499,000 − 1,072.85)
        assertThat(ledger().available(TRADING_DATE)).isEqualByComparingTo("10008172.825");
    }

    @Test
    void ignoresBrokerOrdersAndUnfilledTrades() {
        StockTrade pendingSim = StockTrade.createBuyOrder(
            StockTrade.PENDING_ORDER_ID_PREFIX + "1", "005930", 10, new BigDecimal("100000"), true);
        tradesToday(
            filledBuy("0000123456", 10, "100000", "150"),   // LIVE 브로커 주문번호 — 가상 원장 아님
            pendingSim,                                      // 미체결 선영속화 흔적
            filledBuy("SIM-9", 1, "50000", "7.5"));

        assertThat(ledger().available(TRADING_DATE)).isEqualByComparingTo("9949992.5");
    }

    @Test
    void recomputesSameBalanceAfterRestart() {
        tradesToday(
            filledBuy("SIM-1", 10, "100200", "150.30"),
            filledSell("SIM-2", 5, "102700", "1104.025"));

        BigDecimal beforeRestart = ledger().available(TRADING_DATE);
        BigDecimal afterRestart = ledger().available(TRADING_DATE);   // 새 인스턴스 = 재기동

        assertThat(afterRestart).isEqualByComparingTo(beforeRestart);
    }

    @Test
    void readsOnlyTheTradingDate() {
        tradesToday();

        ledger().available(TRADING_DATE);

        verify(tradeRepository).findByOrderedAtBetween(
            LocalDateTime.of(2026, 10, 21, 0, 0), LocalDateTime.of(2026, 10, 22, 0, 0));
    }

    @Test
    void todayUsesClockDate() {
        tradesToday();

        ledger().availableToday();

        verify(tradeRepository).findByOrderedAtBetween(
            LocalDateTime.of(2026, 10, 21, 0, 0), LocalDateTime.of(2026, 10, 22, 0, 0));
    }
}
