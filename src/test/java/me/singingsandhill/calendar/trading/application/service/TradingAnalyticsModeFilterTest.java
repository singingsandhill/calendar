package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.AnalyticsReport;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeMode;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 신호 품질 분석은 LIVE 포지션만 본다 (ADR trading/observability/0004).
 */
class TradingAnalyticsModeFilterTest {

    private final SignalRepository signalRepository = mock(SignalRepository.class);
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);

    private static Position closed(TradeMode mode) {
        Position p = Position.open("KRW-ADA", new BigDecimal("1000"), BigDecimal.TEN,
            new BigDecimal("985"), new BigDecimal("1030"));
        p.close(new BigDecimal("1010"), BigDecimal.TEN, CloseReason.TIME_EXIT);
        p.setMode(mode);
        return p;
    }

    @Test
    void paperPositionsAreExcludedFromAnalytics() {
        when(signalRepository.findSamplesByMarketAndSignalTimeBetween(any(), any(), any())).thenReturn(List.of());
        when(positionRepository.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any()))
            .thenReturn(List.of(closed(TradeMode.LIVE), closed(TradeMode.PAPER), closed(null)));
        when(tradeRepository.findByMarketAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of());
        TradingAnalyticsService service = new TradingAnalyticsService(signalRepository, positionRepository,
            tradeRepository, new TradingProperties(), mock(PlatformTransactionManager.class));

        LocalDateTime to = LocalDateTime.of(2026, 10, 21, 0, 0);
        AnalyticsReport report = service.analyze(to.minusDays(7), to);

        assertThat(report.coverage().closedPositions()).isEqualTo(2);
    }
}
