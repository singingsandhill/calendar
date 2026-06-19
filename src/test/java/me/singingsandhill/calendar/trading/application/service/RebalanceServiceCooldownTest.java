package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-11: markRebalanceCooldown() 호출 시 리밸런스 쿨다운이 활성화되어야 한다(엔진 핑퐁 방지).
 */
class RebalanceServiceCooldownTest {

    private RebalanceService service() {
        return new RebalanceService(null, null, new TradingProperties(), null, null, null, null, null);
    }

    private boolean cooldownElapsed(RebalanceService svc) throws Exception {
        Method m = RebalanceService.class.getDeclaredMethod("isCooldownElapsed");
        m.setAccessible(true);
        return (boolean) m.invoke(svc);
    }

    @Test
    void markRebalanceCooldown_activatesCooldown() throws Exception {
        RebalanceService svc = service();
        assertThat(cooldownElapsed(svc)).isTrue(); // 초기: 쿨다운 경과(없음)
        svc.markRebalanceCooldown();
        assertThat(cooldownElapsed(svc)).isFalse(); // 마킹 후: 쿨다운 활성
    }
}
