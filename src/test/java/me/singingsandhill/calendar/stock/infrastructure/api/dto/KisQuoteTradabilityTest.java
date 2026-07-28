package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거래 가능 상태 판정 (2026-07-24 리뷰 §6 / P2-3).
 *
 * VI·거래정지·정리매매·투자경고 종목을 걸러낼 방어 코드가 전혀 없었다. KIS 주식현재가 시세
 * 응답의 상태 필드를 읽어 스크리닝·진입에서 배제한다.
 *
 * **필드 부재는 거래 가능으로 본다** — 응답 스펙이 다르거나 필드가 비어도 기존 동작을
 * 깨지 않기 위함(무회귀). 대신 호출측이 계측 로그로 필드 존재 여부를 관측한다.
 */
class KisQuoteTradabilityTest {

    private KisQuoteResponse quoteWith(String statusCode, String tempStop,
                                        String marketWarn, String liquidation) {
        return new KisQuoteResponse("005930", new BigDecimal("70000"), new BigDecimal("69000"),
            new BigDecimal("71000"), new BigDecimal("68000"), new BigDecimal("68500"),
            BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            statusCode, tempStop, marketWarn, liquidation);
    }

    @Test
    void normalStockIsTradable() {
        assertThat(quoteWith("00", "N", "00", "N").isTradable()).isTrue();
    }

    @Test
    void missingStatusFieldsAreTreatedAsTradable() {
        // 필드가 없거나 비어도 기존 동작 유지 (무회귀)
        assertThat(quoteWith(null, null, null, null).isTradable()).isTrue();
        assertThat(quoteWith("", "", "", "").isTradable()).isTrue();
    }

    @Test
    void temporarilyHaltedStockIsNotTradable() {
        // VI 발동 시 단일가 전환 구간 포함
        assertThat(quoteWith("00", "Y", "00", "N").isTradable()).isFalse();
    }

    @Test
    void suspendedStockIsNotTradable() {
        // 58: 거래정지
        assertThat(quoteWith("58", "N", "00", "N").isTradable()).isFalse();
    }

    @Test
    void investmentAlertAndRiskAreNotTradable() {
        // 02: 투자경고, 03: 투자위험 — 신용/증거금 제한과 급변동 위험
        assertThat(quoteWith("00", "N", "02", "N").isTradable()).isFalse();
        assertThat(quoteWith("00", "N", "03", "N").isTradable()).isFalse();
        // 01: 투자주의는 허용 (빈도가 높고 매매 제약이 없음)
        assertThat(quoteWith("00", "N", "01", "N").isTradable()).isTrue();
    }

    @Test
    void liquidationTradingIsNotTradable() {
        // 정리매매 — 상장폐지 예정
        assertThat(quoteWith("00", "N", "00", "Y").isTradable()).isFalse();
    }

    @Test
    void managedAndOverheatedStocksAreNotTradable() {
        assertThat(quoteWith("51", "N", "00", "N").isTradable()).isFalse(); // 관리종목
        assertThat(quoteWith("59", "N", "00", "N").isTradable()).isFalse(); // 단기과열
    }
}
