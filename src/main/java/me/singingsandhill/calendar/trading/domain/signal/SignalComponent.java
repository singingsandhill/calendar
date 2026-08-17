package me.singingsandhill.calendar.trading.domain.signal;

/**
 * 합성 점수를 이루는 8개 구성요소.
 *
 * <p>가중치는 {@code SignalService} 의 각 {@code calculate*Score} 메서드 본문에 있고 설정으로
 * 빠져 있지 않다. 여기 적힌 {@code maxAbsWeight} 는 그 값의 사본이며, 분석 페이지가
 * "이 구성요소가 총점에서 차지할 수 있는 몫" 을 보여주기 위한 표시용이다 — 점수 계산에는 쓰이지
 * 않으므로 드리프트가 나도 매매에는 영향이 없다 (ADR trading/strategy/0001, 0010).
 */
public enum SignalComponent {

    MA_CROSS("MA 크로스/상태", 25),
    MA_TREND("MA 추세 (가격 vs MA60)", 8),
    RSI_DIVERGENCE("RSI 다이버전스", 20),
    RSI_LEVEL("RSI 레벨", 15),
    STOCH_DIVERGENCE("스토캐스틱 다이버전스", 15),
    STOCH_LEVEL("스토캐스틱 레벨", 15),
    VOLUME_DIVERGENCE("거래량 다이버전스", 20),
    RSI_TREND("RSI 추세", 10);

    private final String displayName;
    private final int maxAbsWeight;

    SignalComponent(String displayName, int maxAbsWeight) {
        this.displayName = displayName;
        this.maxAbsWeight = maxAbsWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxAbsWeight() {
        return maxAbsWeight;
    }
}
