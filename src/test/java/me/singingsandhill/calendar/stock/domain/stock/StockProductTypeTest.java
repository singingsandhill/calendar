package me.singingsandhill.calendar.stock.domain.stock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KIS 주식기본조회(CTPF1002R) {@code scty_grp_id_cd}(증권그룹ID코드) → 상품 유형 (ADR stock/algorithm/0011).
 * 코드표는 KIS 개발자센터 문서 실측(2026-10-23): ST.주권 · KN.코넥스주권 · EF.ETF · FE.해외ETF · EN.ETN …
 * 호가단위 규칙이 확인된 코드만 매핑하고 나머지는 모름(null — 주식 호가표 폴백, 리포트 UNKNOWN).
 */
class StockProductTypeTest {

    @Test
    void mapsVerifiedSecurityGroupCodes() {
        assertThat(StockProductType.fromSecurityGroupCode("ST")).isEqualTo(StockProductType.STOCK);
        assertThat(StockProductType.fromSecurityGroupCode("KN")).isEqualTo(StockProductType.STOCK);
        assertThat(StockProductType.fromSecurityGroupCode("EF")).isEqualTo(StockProductType.ETF);
        assertThat(StockProductType.fromSecurityGroupCode("FE")).isEqualTo(StockProductType.ETF);
        assertThat(StockProductType.fromSecurityGroupCode("EN")).isEqualTo(StockProductType.ETN);
        assertThat(StockProductType.fromSecurityGroupCode(" ef ")).isEqualTo(StockProductType.ETF);
    }

    @Test
    void otherOrMissingCodesAreUnknown() {
        assertThat(StockProductType.fromSecurityGroupCode("EW")).isNull();   // ELW — 호가표 다름, 유니버스 밖
        assertThat(StockProductType.fromSecurityGroupCode("RT")).isNull();   // 리츠 — 호가 규칙 미확인
        assertThat(StockProductType.fromSecurityGroupCode("")).isNull();
        assertThat(StockProductType.fromSecurityGroupCode(null)).isNull();
    }
}
