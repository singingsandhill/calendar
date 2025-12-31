package me.singingsandhill.calendar.stock.domain.position;

/**
 * 포지션 청산 사유
 */
public enum StockCloseReason {

    /**
     * 1차 익절 (+1.5%)
     */
    TP1("1차익절"),

    /**
     * 2차 익절 (당일 고점)
     */
    TP2("2차익절"),

    /**
     * 3차 익절 (고점 +1%)
     */
    TP3("3차익절"),

    /**
     * 손절 (-1.5%)
     */
    STOP_LOSS("손절"),

    /**
     * 트레일링 스탑 (고점 대비 -0.8%)
     */
    TRAILING_STOP("트레일링"),

    /**
     * 시간 청산 (11:20)
     */
    TIME_EXIT("시간청산"),

    /**
     * 수동 청산
     */
    MANUAL("수동"),

    /**
     * 긴급 청산
     */
    EMERGENCY("긴급청산");

    private final String displayName;

    StockCloseReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTakeProfit() {
        return this == TP1 || this == TP2 || this == TP3;
    }

    public boolean isStopLoss() {
        return this == STOP_LOSS || this == TRAILING_STOP;
    }
}
