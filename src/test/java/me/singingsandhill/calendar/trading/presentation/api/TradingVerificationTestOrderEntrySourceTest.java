package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.auth.BithumbJwtGenerator;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 검증용 test-order 가 만드는 포지션도 진입 출처를 남긴다 (ADR trading/observability/0004) — 운영자가 직접 낸
 * 주문이므로 MANUAL. 생성 지점 하나라도 빠지면 "출처 불명(null)" 이 과거 레거시와 섞인다.
 */
class TradingVerificationTestOrderEntrySourceTest {

    @Test
    void testOrderPositionIsMarkedManual() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        BithumbJwtGenerator jwt = mock(BithumbJwtGenerator.class);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);
        when(jwt.isConfigured()).thenReturn(true);
        when(api.getKrwBalance()).thenReturn(new BithumbAccountResponse("KRW", "100000", "0", null, null, "KRW"));
        when(api.getCurrentPrice()).thenReturn(290.0);
        BithumbOrderResponse.TradeDetail fill = new BithumbOrderResponse.TradeDetail(
            "KRW-ADA", "t1", "290", "18.96551724", "5500", "bid", "2026-10-21T10:00:00");
        when(api.placeMarketBuyOrder(any())).thenReturn(new BithumbOrderResponse(
            "ex-1", "bid", "price", "5500", "done", "KRW-ADA", null,
            "18.96551724", "0", "0", "0", "2.2", "0", "18.96551724", 1, List.of(fill)));
        when(tradeRepo.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(posRepo.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));
        TradingVerificationApiController controller = new TradingVerificationApiController(
            api, jwt, new TradingProperties(), tradeRepo, posRepo);

        controller.testOrder(new BigDecimal("5500"), false);

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(captor.capture());
        assertThat(captor.getValue().getEntrySource()).isEqualTo(EntrySource.MANUAL);
    }
}
