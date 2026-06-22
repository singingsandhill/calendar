package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 체결강도(cttr) 매핑 회귀 테스트.
 *
 * 배경: KIS 주식현재가 시세(FHKST01010100)는 누적 체결량(seln_cntg_smtn/shnu_cntg_smtn)을
 * 주지 않아, 그 두 필드로 계산하던 기존 구현은 체결강도가 항상 0 → 스크리닝/진입 전량 차단.
 * 수정: KIS 가 직접 제공하는 cttr 를 우선 사용.
 */
class KisQuoteResponseTest {

    /** cttr / 누적매도 / 누적매수 만 변화시키는 헬퍼 (가격 필드는 갭 계산용 고정값). */
    private KisQuoteResponse quote(BigDecimal cttr, BigDecimal sellSum, BigDecimal buySum) {
        return new KisQuoteResponse(
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
            BigDecimal.ZERO,           // volumeTurnover
            sellSum,                   // totalSellVolume (seln_cntg_smtn)
            buySum,                    // totalBuyVolume  (shnu_cntg_smtn)
            cttr                       // contractStrength (cttr)
        );
    }

    @Test
    @DisplayName("cttr(체결강도)가 제공되면 그 값을 그대로 사용한다")
    void usesCttrWhenPresent() {
        KisQuoteResponse q = quote(new BigDecimal("123.45"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(q.calculateTradeStrength()).isEqualByComparingTo("123.45");
    }

    @Test
    @DisplayName("cttr 가 없으면 누적 매수/매도 체결량으로 폴백 계산한다 (150/100*100=150)")
    void fallsBackToCumulativeVolumes() {
        KisQuoteResponse q = quote(null, new BigDecimal("100"), new BigDecimal("150"));
        assertThat(q.calculateTradeStrength()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("cttr=0(장초반 미집계) + 폴백 데이터 없음 → 0 (기존 '항상 0' 회귀 가드)")
    void returnsZeroWhenNoData() {
        KisQuoteResponse q = quote(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(q.calculateTradeStrength()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("totalBuyVolume 이 null 이어도 NPE 없이 0 을 반환한다")
    void noNpeWhenBuyVolumeNull() {
        KisQuoteResponse q = quote(null, new BigDecimal("100"), null);
        assertThat(q.calculateTradeStrength()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("cttr 우선순위: cttr 가 양수이면 누적 체결량 계산보다 우선한다")
    void cttrTakesPrecedenceOverCumulative() {
        // 누적 계산은 200 이지만 cttr 105 가 우선
        KisQuoteResponse q = quote(new BigDecimal("105"), new BigDecimal("100"), new BigDecimal("200"));
        assertThat(q.calculateTradeStrength()).isEqualByComparingTo("105.00");
    }

    @Test
    @DisplayName("갭% = (시가-전일종가)/전일종가*100 = (69000-68000)/68000*100")
    void gapPercent() {
        KisQuoteResponse q = quote(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(q.calculateGapPercent()).isEqualByComparingTo("1.4706");
    }
}
