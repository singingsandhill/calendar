package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * P2-10: 물타기 차단 (손실 포지션 보유 중 추가 매수 차단).
 * P2-12: 코인 노출 상한 (코인/총자본 ≥ maxCoinExposurePct 시 신규 매수 스킵).
 */
class TradingBotServiceEntryGuardTest {

    private TradingBotService service(TradingProperties props) {
        return new TradingBotService(null, null, null, null, null, null, null, null,
                props, null, null, null, mock(PlatformTransactionManager.class));
    }

    private Position posEntry(String entryPrice) {
        BigDecimal p = new BigDecimal(entryPrice);
        return Position.open("KRW-ADA", p, BigDecimal.TEN, p.multiply(new BigDecimal("0.985")), p);
    }

    // ---- P2-10 ----
    @Test
    void blocksAveragingDown_whenAnyOpenPositionAtLoss() {
        TradingBotService svc = service(new TradingProperties());
        // 진입 1000, 현재가 950 → 손실 → 추가 매수 차단
        assertThat(svc.blocksAveragingDown(List.of(posEntry("1000")), new BigDecimal("950"))).isTrue();
    }

    @Test
    void allowsEntry_whenOpenPositionsInProfit() {
        TradingBotService svc = service(new TradingProperties());
        assertThat(svc.blocksAveragingDown(List.of(posEntry("1000")), new BigDecimal("1050"))).isFalse();
    }

    @Test
    void allowsEntry_whenNoOpenPositions() {
        TradingBotService svc = service(new TradingProperties());
        assertThat(svc.blocksAveragingDown(List.of(), new BigDecimal("950"))).isFalse();
    }

    @Test
    void averagingDown_notBlocked_whenDisabled() {
        TradingProperties props = new TradingProperties();
        props.getBot().setBlockAveragingDown(false);
        assertThat(service(props).blocksAveragingDown(List.of(posEntry("1000")), new BigDecimal("950"))).isFalse();
    }

    // ---- P2-12 ----
    @Test
    void exceedsExposureCap_whenCoinRatioAboveCap() {
        TradingProperties props = new TradingProperties();
        props.getBot().setMaxCoinExposurePct(0.8);
        // 코인 850k / 총자본 1,000k = 0.85 ≥ 0.8 → 초과
        assertThat(service(props).exceedsExposureCap(new BigDecimal("850000"), new BigDecimal("1000000"))).isTrue();
    }

    @Test
    void withinExposureCap_whenCoinRatioBelowCap() {
        TradingProperties props = new TradingProperties();
        props.getBot().setMaxCoinExposurePct(0.8);
        assertThat(service(props).exceedsExposureCap(new BigDecimal("700000"), new BigDecimal("1000000"))).isFalse();
    }

    @Test
    void exposureCap_disabledWhenZero() {
        TradingProperties props = new TradingProperties();
        props.getBot().setMaxCoinExposurePct(0);
        assertThat(service(props).exceedsExposureCap(new BigDecimal("999999"), new BigDecimal("1000000"))).isFalse();
    }
}
