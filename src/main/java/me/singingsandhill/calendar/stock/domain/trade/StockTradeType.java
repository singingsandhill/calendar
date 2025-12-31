package me.singingsandhill.calendar.stock.domain.trade;

/**
 * 거래 유형
 */
public enum StockTradeType {
    BUY("매수"),
    SELL("매도");

    private final String displayName;

    StockTradeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
