package me.singingsandhill.calendar.trading.domain.signal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 분석용 신호 관측치 — {@link Signal} 의 읽기 전용 축소판.
 *
 * <p>전체 {@code Signal} 엔티티를 로드하지 않는 이유는 규모다. {@code trading_signals} 는 매 루프
 * 틱마다 1행이 쌓여 하루 ~1,440행, 90일이면 약 130,000행이다. 이만큼을
 * {@code @Transactional(readOnly = true)} 안에서 엔티티로 읽으면 전부 영속성 컨텍스트 1차 캐시에
 * 남아 요청이 끝날 때까지 힙을 붙잡는다. 이 앱은 Jetson Nano 에서도 돌아간다.
 *
 * <p>그래서 여기에는 분석이 실제로 쓰는 필드만 담는다. {@code ma5}/{@code ma20} 은 어떤 표에도
 * 들어가지 않아 뺐고, {@code executed} 는 한 번도 true 가 된 적 없는 죽은 컬럼이라 뺐다.
 */
public record SignalSample(
        Long id,
        LocalDateTime signalTime,
        SignalType signalType,
        int totalScore,
        // 8개 구성점수. 과거 행은 null 일 수 있어 Integer 로 받는다.
        Integer maCrossScore,
        Integer maTrendScore,
        Integer rsiDivergenceScore,
        Integer rsiLevelScore,
        Integer stochDivergenceScore,
        Integer stochLevelScore,
        Integer volumeDivergenceScore,
        Integer rsiTrendScore,
        BigDecimal ma60,
        BigDecimal rsi,
        BigDecimal stochK,
        BigDecimal stochD,
        DivergenceType rsiDivergence,
        DivergenceType stochDivergence,
        DivergenceType volumeDivergence,
        BigDecimal currentPrice,
        // ADR trading/observability/0002 이전 행에서는 null — 임계 반사실 분석의 판정불가 사유.
        BigDecimal volumeMa,
        BigDecimal currentVolume,
        BigDecimal atrPercent
) {

    /** 구성요소별 조건부 엣지 표에서 8개 점수를 하나의 루프로 돌기 위한 디스패치. */
    public Integer score(SignalComponent component) {
        return switch (component) {
            case MA_CROSS -> maCrossScore;
            case MA_TREND -> maTrendScore;
            case RSI_DIVERGENCE -> rsiDivergenceScore;
            case RSI_LEVEL -> rsiLevelScore;
            case STOCH_DIVERGENCE -> stochDivergenceScore;
            case STOCH_LEVEL -> stochLevelScore;
            case VOLUME_DIVERGENCE -> volumeDivergenceScore;
            case RSI_TREND -> rsiTrendScore;
        };
    }

    public boolean isPriceBelowMa60() {
        return currentPrice != null && ma60 != null && currentPrice.compareTo(ma60) < 0;
    }

    public boolean hasBullishDivergence() {
        return rsiDivergence == DivergenceType.BULLISH
                || stochDivergence == DivergenceType.BULLISH
                || volumeDivergence == DivergenceType.BULLISH;
    }

    /**
     * MA60 하회 시 매수 확인 게이트의 거래량 스파이크 분기를 판정할 수 있는가.
     * false 면 그 신호는 임계 반사실 분석에서 "판정불가" 로 분류된다.
     */
    public boolean hasVolumeContext() {
        return volumeMa != null && currentVolume != null;
    }

    /** {@code SignalService.determineSignalType} 의 거래량 스파이크 조건(volumeMa × 1.5)과 같은 식. */
    public boolean hasVolumeSpike(double multiplier) {
        if (!hasVolumeContext()) {
            return false;
        }
        return currentVolume.compareTo(volumeMa.multiply(BigDecimal.valueOf(multiplier))) > 0;
    }
}
