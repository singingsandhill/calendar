package me.singingsandhill.calendar.stock.domain.stock;

/**
 * 종목 감시 상태 (State Machine)
 *
 * WATCHING → HIGH_FORMED → PULLBACK → ENTRY_READY → ENTERED → EXITED
 *                                ↘ FILTERED_OUT (조건 미달)
 */
public enum StockState {

    /**
     * 감시 중 - 갭 조건 충족, 고점 형성 대기
     */
    WATCHING("감시중"),

    /**
     * 고점 형성됨 - 시가 대비 +1.5% 이상 상승
     */
    HIGH_FORMED("고점형성"),

    /**
     * 눌림목 진행 중 - 고점 대비 -1.5% ~ -3.0% 조정
     */
    PULLBACK("눌림목"),

    /**
     * 진입 준비 - 반등 +0.3% 확인, 진입 대기
     */
    ENTRY_READY("진입대기"),

    /**
     * 진입 완료 - 포지션 보유 중
     */
    ENTERED("보유중"),

    /**
     * 청산 완료
     */
    EXITED("청산완료"),

    /**
     * 조건 미달로 제외 (고점 대비 -3.0% 초과 하락 등)
     */
    FILTERED_OUT("제외");

    private final String displayName;

    StockState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 활성 감시 상태 여부 (진입 전)
     */
    public boolean isActive() {
        return this == WATCHING || this == HIGH_FORMED || this == PULLBACK || this == ENTRY_READY;
    }

    /**
     * 진입 가능 상태 여부
     */
    public boolean canEnter() {
        return this == ENTRY_READY;
    }

    /**
     * 보유 중 여부
     */
    public boolean isHolding() {
        return this == ENTERED;
    }

    /**
     * 종료 상태 여부
     */
    public boolean isTerminal() {
        return this == EXITED || this == FILTERED_OUT;
    }
}
