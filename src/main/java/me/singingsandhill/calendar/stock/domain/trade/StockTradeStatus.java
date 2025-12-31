package me.singingsandhill.calendar.stock.domain.trade;

/**
 * 거래 상태
 */
public enum StockTradeStatus {
    PENDING("대기"),
    FILLED("체결"),
    PARTIAL("부분체결"),
    CANCELLED("취소");

    private final String displayName;

    StockTradeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isComplete() {
        return this == FILLED || this == CANCELLED;
    }
}
