package me.singingsandhill.calendar.stock.domain.stock;

import java.util.Locale;

/**
 * 종목 상품 유형. KRX 호가가격단위가 유형별로 다르다 — ETF·ETN 은 2,000원 미만 1원 /
 * 이상 5원, 주식은 가격대별 7단계 (ADR stock/modes/0004).
 */
public enum StockProductType {
    STOCK,
    ETF,
    ETN;

    /**
     * KIS 주식기본조회(CTPF1002R) {@code scty_grp_id_cd}(증권그룹ID코드) → 유형 (ADR stock/algorithm/0011, 코드표 = KIS
     * 개발자센터 문서). 호가단위 규칙이 확인된 코드만 매핑한다 — ST 주권·KN 코넥스주권 → STOCK, EF ETF·FE 해외ETF → ETF,
     * EN → ETN. 그 밖의 코드(ELW·리츠·투자회사 등)와 부재는 null(모름 — 호가는 주식 표 폴백, 리포트는 UNKNOWN).
     */
    public static StockProductType fromSecurityGroupCode(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "ST", "KN" -> STOCK;
            case "EF", "FE" -> ETF;
            case "EN" -> ETN;
            default -> null;
        };
    }
}
