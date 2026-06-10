package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-8: 정체 포지션 시간 청산. maxHoldMinutes 초과 + 손익분기 이상이면 청산(적자는 강제 안 함).
 */
class RiskManagementServiceTimeExitTest {

    private RiskManagementService service(long maxHoldMinutes) {
        TradingProperties props = new TradingProperties();
        props.getBot().setMaxHoldMinutes(maxHoldMinutes);
        return new RiskManagementService(null, null, null, props, null, null, null);
    }

    private Position posOpenedAt(LocalDateTime openedAt) {
        BigDecimal p = new BigDecimal("1000");
        return new Position(null, "KRW-ADA", PositionStatus.OPEN, p, BigDecimal.TEN, p.multiply(BigDecimal.TEN),
                null, null, null, null, null, null, null, null, p, false, null,
                openedAt, null, openedAt, BigDecimal.ZERO, null, null);
    }

    @Test
    void staleAndProfitable_exits() {
        Position pos = posOpenedAt(LocalDateTime.now().minusHours(10));
        assertThat(service(360).shouldTimeExit(pos, new BigDecimal("1.0"))).isTrue();
    }

    @Test
    void freshAndProfitable_doesNotExit() {
        Position pos = posOpenedAt(LocalDateTime.now());
        assertThat(service(360).shouldTimeExit(pos, new BigDecimal("1.0"))).isFalse();
    }

    @Test
    void staleButAtLoss_doesNotForceExit() {
        Position pos = posOpenedAt(LocalDateTime.now().minusHours(10));
        assertThat(service(360).shouldTimeExit(pos, new BigDecimal("-1.0"))).isFalse();
    }

    @Test
    void disabled_neverExits() {
        Position pos = posOpenedAt(LocalDateTime.now().minusHours(10));
        assertThat(service(0).shouldTimeExit(pos, new BigDecimal("1.0"))).isFalse();
    }
}
