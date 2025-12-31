package me.singingsandhill.calendar.stock.domain.position;

/**
 * 포지션 상태
 */
public enum StockPositionStatus {

    /**
     * 오픈 - 전량 보유 중
     */
    OPEN("오픈"),

    /**
     * 부분 청산 - 일부 수량 청산됨
     */
    PARTIAL("부분청산"),

    /**
     * 종료 - 전량 청산 완료
     */
    CLOSED("종료");

    private final String displayName;

    StockPositionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOpen() {
        return this == OPEN || this == PARTIAL;
    }
}
