package me.singingsandhill.calendar.trading.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 출구 규칙 설정 기본값 (ADR trading/strategy/0011) — 틱 하한은 최적화가 아니라 "노이즈 미만 임계값 금지" 하한이라 기본 ON,
 * 결론이 안 난 전략 키(ATR 손절·시간청산 모드·손실 시간청산)는 기본 = 현재 동작. 값은 리플레이(작업 6-A)로 정한다.
 */
class TradingExitPropertiesTest {

    @Test
    void tickFloorIsOnByDefault() {
        TradingProperties.Risk risk = new TradingProperties().getRisk();

        assertThat(risk.getMinTrailingTicks()).isEqualTo(5);
        assertThat(risk.getMinStopTicks()).isEqualTo(5);
    }

    @Test
    void unsettledExitKeysDefaultToCurrentBehavior() {
        TradingProperties props = new TradingProperties();

        assertThat(props.getRisk().getStopMode()).isEqualTo(TradingProperties.Risk.StopMode.PERCENT);
        assertThat(props.getRisk().getAtrPeriod()).isEqualTo(14);
        assertThat(props.getRisk().getAtrMultiplier()).isEqualTo(2.0);
        assertThat(props.getRisk().getAtrBarMinutes()).isEqualTo(15);
        assertThat(props.getBot().getTimeExitMode()).isEqualTo(TradingProperties.Bot.TimeExitMode.BREAKEVEN_CLOSE);
        assertThat(props.getBot().getTimeExitLossThreshold()).isNull();
    }
}
