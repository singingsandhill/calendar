package me.singingsandhill.calendar.stock.infrastructure.api.dto;

import me.singingsandhill.calendar.stock.domain.stock.StockProductType;

/**
 * 주식기본조회(CTPF1002R, search-stock-info) 결과 — 종목명(약칭 우선)과 상품 유형(scty_grp_id_cd).
 * 응답은 받았지만 필드가 없으면 각 값이 null (ADR stock/infrastructure/0008, stock/algorithm/0011).
 */
public record KisStockInfo(String name, StockProductType productType) {
}
