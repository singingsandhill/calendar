package me.singingsandhill.calendar.stock.domain.trade;

import me.singingsandhill.calendar.stock.domain.stock.StockProductType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KRX 호가가격단위 (ADR stock/modes/0004).
 *
 * 주식(유가증권·코스닥) 표는 2023-01-25 단일화 기준, ETF·ETN 은 2023-12-18 개정으로
 * 2,000원 미만 1원 / 이상 5원. PAPER 체결가를 실제로 주문 가능한 가격으로 맞추는 데 쓴다 —
 * 호가단위에 없는 가격(예: 6,743.46원)으로 체결을 기록하면 장부가 픽션이 된다.
 */
class KrxTickSizeTest {

    private static BigDecimal tick(String price, StockProductType type) {
        return KrxTickSize.tickSize(new BigDecimal(price), type);
    }

    @Test
    void stockTickFollowsUnifiedKrxBands() {
        assertThat(tick("1999", StockProductType.STOCK)).isEqualByComparingTo("1");
        assertThat(tick("2000", StockProductType.STOCK)).isEqualByComparingTo("5");
        assertThat(tick("4995", StockProductType.STOCK)).isEqualByComparingTo("5");
        assertThat(tick("5000", StockProductType.STOCK)).isEqualByComparingTo("10");
        assertThat(tick("19990", StockProductType.STOCK)).isEqualByComparingTo("10");
        assertThat(tick("20000", StockProductType.STOCK)).isEqualByComparingTo("50");
        assertThat(tick("49950", StockProductType.STOCK)).isEqualByComparingTo("50");
        assertThat(tick("50000", StockProductType.STOCK)).isEqualByComparingTo("100");
        assertThat(tick("199900", StockProductType.STOCK)).isEqualByComparingTo("100");
        assertThat(tick("200000", StockProductType.STOCK)).isEqualByComparingTo("500");
        assertThat(tick("499500", StockProductType.STOCK)).isEqualByComparingTo("500");
        assertThat(tick("500000", StockProductType.STOCK)).isEqualByComparingTo("1000");
    }

    @Test
    void unknownProductTypeUsesStockTable() {
        assertThat(tick("25000", null)).isEqualByComparingTo("50");
    }

    @Test
    void etfAndEtnUseOneWonBelow2000AndFiveWonAbove() {
        assertThat(tick("1999", StockProductType.ETF)).isEqualByComparingTo("1");
        assertThat(tick("2000", StockProductType.ETF)).isEqualByComparingTo("5");
        assertThat(tick("50000", StockProductType.ETF)).isEqualByComparingTo("5");
        assertThat(tick("1500", StockProductType.ETN)).isEqualByComparingTo("1");
        assertThat(tick("30000", StockProductType.ETN)).isEqualByComparingTo("5");
    }

    @Test
    void roundUpMovesToNextValidPrice() {
        assertThat(KrxTickSize.roundUp(new BigDecimal("6743.46"), StockProductType.STOCK))
            .isEqualByComparingTo("6750");
        assertThat(KrxTickSize.roundUp(new BigDecimal("100200.000"), StockProductType.STOCK))
            .isEqualByComparingTo("100200");
        // 밴드 경계를 넘는 올림 — 아래 밴드 단위로 올려도 경계값은 위 밴드에서도 유효하다
        assertThat(KrxTickSize.roundUp(new BigDecimal("1999.4"), StockProductType.STOCK))
            .isEqualByComparingTo("2000");
        assertThat(KrxTickSize.roundUp(new BigDecimal("30061"), StockProductType.ETF))
            .isEqualByComparingTo("30065");
    }

    @Test
    void roundDownMovesToPreviousValidPrice() {
        assertThat(KrxTickSize.roundDown(new BigDecimal("102794"), StockProductType.STOCK))
            .isEqualByComparingTo("102700");
        assertThat(KrxTickSize.roundDown(new BigDecimal("2003"), StockProductType.STOCK))
            .isEqualByComparingTo("2000");
        assertThat(KrxTickSize.roundDown(new BigDecimal("1999.9"), StockProductType.STOCK))
            .isEqualByComparingTo("1999");
        assertThat(KrxTickSize.roundDown(new BigDecimal("30064"), StockProductType.ETN))
            .isEqualByComparingTo("30060");
    }
}
