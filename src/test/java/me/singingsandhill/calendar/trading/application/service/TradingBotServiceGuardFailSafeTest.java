package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 진입 가드 fail-safe 회귀 테스트 (ADR trading/risk/0005).
 *
 * 현재가 조회 실패(null)는 물타기 차단·코인 노출상한 가드를 평가할 수 없다는 뜻이다.
 * 이때 가드를 건너뛰고 매수를 진행하면(fail-open) 2026-07-27 로그의
 * PrematureCloseException 경로에서 무가드 매수가 나간다 — 실자금 코드는
 * 평가 불가 시 차단(fail-safe)이어야 한다.
 */
class TradingBotServiceGuardFailSafeTest {

    private static final String MARKET = "KRW-ADA";

    private final BithumbApiClient api = mock(BithumbApiClient.class);
    private final TradeRepository tradeRepo = mock(TradeRepository.class);
    private final PositionRepository posRepo = mock(PositionRepository.class);
    private final TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
    private final AccountSnapshotRepository snapRepo = mock(AccountSnapshotRepository.class);

    private TradingBotService service() {
        return new TradingBotService(mock(CandleService.class), mock(SignalService.class),
                mock(IndicatorService.class), mock(RiskManagementService.class),
                mock(RebalanceService.class), api, tradeRepo, posRepo,
                new TradingProperties(), mock(TradingEventService.class), breaker, snapRepo,
                mock(PlatformTransactionManager.class));
    }

    @Test
    void entryRiskGuardsBlock_blocksWhenPriceUnavailable() {
        when(api.getCurrentPrice()).thenReturn(null);

        // 가드 입력(현재가)을 못 얻으면 통과가 아니라 차단이어야 한다
        assertThat(service().entryRiskGuardsBlock(MARKET)).isTrue();
    }

    @Test
    void executeBuy_skipsWhenPriceUnavailable() {
        when(api.getKrwBalance()).thenReturn(
                new BithumbAccountResponse("KRW", "100000", "0", "0", false, "KRW"));
        when(api.getCurrentPrice()).thenReturn(null);

        service().executeBuy(MARKET, mock(Signal.class));

        // 잔고가 충분해도 가드 평가 불가면 주문을 전송하지 않는다
        verify(api, never()).placeMarketBuyOrder(any(BigDecimal.class));
        verify(api, never()).placeMarketBuyOrder(any(BigDecimal.class), any(String.class));
    }
}
