package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시세 응답 파생 계산 테스트.
 *
 * 체결강도는 이 DTO 에서 제거됐다 — 시세 TR(FHKST01010100)은 체결강도 필드를 제공하지
 * 않으며(cttr/seln_cntg_smtn/shnu_cntg_smtn 모두 스펙에 없음), 소스는 inquire-ccnl 의
 * tday_rltv 다 (ADR stock/infrastructure/0007, {@code KisRestClientTradeStrengthTest}).
 */
class KisQuoteResponseTest {

    @Test
    @DisplayName("갭% = (시가-전일종가)/전일종가*100 = (69000-68000)/68000*100")
    void gapPercent() {
        KisQuoteResponse q = new KisQuoteResponse(
            "005930",
            new BigDecimal("70000"),   // currentPrice
            new BigDecimal("69000"),   // openPrice
            new BigDecimal("71000"),   // highPrice
            new BigDecimal("68500"),   // lowPrice
            new BigDecimal("68000"),   // prevClosePrice
            BigDecimal.ZERO,           // priceChange
            BigDecimal.ZERO,           // changeRate
            0L,                        // volume
            BigDecimal.ZERO,           // tradeValue
            BigDecimal.ZERO,           // marketCap
            BigDecimal.ZERO            // volumeTurnover
        );
        assertThat(q.calculateGapPercent()).isEqualByComparingTo("1.4706");
    }
}
