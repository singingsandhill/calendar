package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.service.StockMailService;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.application.service.StockRiskService;
import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 11:20 최종청산 신뢰성 (2026-07-24 리뷰 §6 / P1-6).
 *
 * 기존: 시세 조회 실패 시 로그조차 없이 스킵, 매도 실패해도 재시도·알림 없음 → 오버나이트 홀드.
 * 기대: 종목별 재시도 후에도 실패하면 알림을 보내 사람이 개입할 수 있어야 한다.
 */
class StockRiskServiceTimeExitTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

    private final StockPositionRepository positionRepository = mock(StockPositionRepository.class);
    private final StockPositionService positionService = mock(StockPositionService.class);
    private final KoreaInvestmentApiClient kisApiClient = mock(KoreaInvestmentApiClient.class);
    private final StockMailService mailService = mock(StockMailService.class);

    private final Clock clock = Clock.fixed(
        LocalDateTime.of(2026, 7, 24, 11, 20).atZone(KST).toInstant(), KST);

    private final StockRiskService service = new StockRiskService(
        positionRepository, positionService, kisApiClient, new StockProperties(), clock, mailService);

    private StockPosition openPosition() {
        return StockPosition.open("005930", TODAY, new BigDecimal("100000"), 10,
            new BigDecimal("98000"), new BigDecimal("101000"));
    }

    private static KisQuoteResponse quote() {
        return new KisQuoteResponse("005930", new BigDecimal("99000"), new BigDecimal("98000"),
            new BigDecimal("101000"), new BigDecimal("97000"), new BigDecimal("97500"),
            BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }

    @Test
    void retriesQuoteLookupBeforeGivingUp() {
        when(positionRepository.findOpenPositions(TODAY)).thenReturn(List.of(openPosition()));
        // 첫 조회 실패 → 재시도에서 성공해야 청산이 이뤄진다
        when(kisApiClient.getQuote("005930")).thenReturn(null).thenReturn(quote());
        when(positionService.closePosition(any(), any(), eq(StockCloseReason.TIME_EXIT))).thenReturn(true);

        service.executeTimeBasedExit(TODAY);

        verify(kisApiClient, times(2)).getQuote("005930");
        verify(positionService).closePosition(any(), any(), eq(StockCloseReason.TIME_EXIT));
    }

    @Test
    void alertsWhenQuoteLookupKeepsFailing() {
        when(positionRepository.findOpenPositions(TODAY)).thenReturn(List.of(openPosition()));
        when(kisApiClient.getQuote(anyString())).thenReturn(null); // 계속 실패

        service.executeTimeBasedExit(TODAY);

        // 사람이 수동 청산할 수 있도록 알림
        verify(mailService).sendTimeExitFailureAlert(eq(TODAY), any());
    }

    @Test
    void retriesAndAlertsWhenSellKeepsBeingRejected() {
        when(positionRepository.findOpenPositions(TODAY)).thenReturn(List.of(openPosition()));
        when(kisApiClient.getQuote("005930")).thenReturn(quote());
        // 시세는 되는데 매도가 계속 미완료 (주문 거부 등)
        when(positionService.closePosition(any(), any(), eq(StockCloseReason.TIME_EXIT))).thenReturn(false);

        service.executeTimeBasedExit(TODAY);

        verify(positionService, times(3)).closePosition(any(), any(), eq(StockCloseReason.TIME_EXIT));
        verify(mailService).sendTimeExitFailureAlert(eq(TODAY), any());
    }

    @Test
    void doesNotAlertWhenAllPositionsClosed() {
        when(positionRepository.findOpenPositions(TODAY)).thenReturn(List.of(openPosition()));
        when(kisApiClient.getQuote("005930")).thenReturn(quote());
        when(positionService.closePosition(any(), any(), eq(StockCloseReason.TIME_EXIT))).thenReturn(true);

        service.executeTimeBasedExit(TODAY);

        verify(mailService, times(0)).sendTimeExitFailureAlert(any(), any());
    }
}
