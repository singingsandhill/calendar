package me.singingsandhill.calendar.stock.domain.signal;

/**
 * 주식 신호 유형
 */
public enum StockSignalType {

    /**
     * 갭 상승 감지
     */
    GAP_DETECTED("갭감지"),

    /**
     * 고점 형성
     */
    HIGH_FORMED("고점형성"),

    /**
     * 눌림목 진입 신호
     */
    PULLBACK_ENTRY("눌림목진입"),

    /**
     * 1차 익절 신호
     */
    TP1_EXIT("1차익절"),

    /**
     * 2차 익절 신호
     */
    TP2_EXIT("2차익절"),

    /**
     * 3차 익절 신호
     */
    TP3_EXIT("3차익절"),

    /**
     * 손절 신호
     */
    STOP_LOSS_EXIT("손절"),

    /**
     * 트레일링 청산 신호
     */
    TRAILING_EXIT("트레일링청산"),

    /**
     * 시간 기반 청산 신호
     */
    TIME_EXIT("시간청산"),

    /**
     * 필터 아웃 (조건 미달)
     */
    FILTERED_OUT("필터아웃");

    private final String displayName;

    StockSignalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEntry() {
        return this == GAP_DETECTED || this == HIGH_FORMED || this == PULLBACK_ENTRY;
    }

    public boolean isExit() {
        return this == TP1_EXIT || this == TP2_EXIT || this == TP3_EXIT
            || this == STOP_LOSS_EXIT || this == TRAILING_EXIT || this == TIME_EXIT;
    }
}
