package me.singingsandhill.calendar.stock.domain.trade;

/**
 * 주문 유형
 */
public enum StockOrderType {
    MARKET("시장가"),
    LIMIT("지정가");

    private final String displayName;

    StockOrderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
