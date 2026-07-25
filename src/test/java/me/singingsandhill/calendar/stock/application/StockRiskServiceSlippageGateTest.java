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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 익절 게이트의 슬리피지 반영 (2026-07-24 리뷰 §4 / P2-1).
 *
 * 모든 주문이 시장가라 체결가가 판단 시점 시세에서 밀린다. 게이트가 수수료·세금만 보면
 * 명목상 익절이 실제로는 순손실이 될 수 있다 — slippageBuffer 는 정의만 되고 어디서도
 * 쓰이지 않았다(사용처 0곳).
 */
class StockRiskServiceSlippageGateTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

    private final StockPositionRepository positionRepository = mock(StockPositionRepository.class);
    private final StockPositionService positionService = mock(StockPositionService.class);
    private final KoreaInvestmentApiClient kisApiClient = mock(KoreaInvestmentApiClient.class);
    private final StockMailService mailService = mock(StockMailService.class);

    private final Clock clock = Clock.fixed(
        LocalDateTime.of(2026, 7, 24, 10, 0).atZone(KST).toInstant(), KST);

    /**
     * 운영값(yaml)과 동일한 TP 임계(TP1 +5%, TP3 +10%) + minProfit 4.6% 고정(시간감쇠 OFF).
     * TP1 도달가에서 순익이 임계 부근이 되도록 잡아 슬리피지 유무가 결과를 가르게 한다.
     */
    private StockRiskService serviceWithSlippage(BigDecimal slippage) {
        StockProperties props = new StockProperties();
        props.getExit().setTp1Percent(new BigDecimal("5.0"));
        props.getExit().setTp3Percent(new BigDecimal("10.0"));
        props.getRisk().setTimeDecayEnabled(false);
        props.getRisk().setMinProfitThreshold(new BigDecimal("0.046"));
        props.getRisk().setSlippageBuffer(slippage);
        return new StockRiskService(positionRepository, positionService, kisApiClient, props, clock, mailService);
    }

    /** 당일고가 106,000 — TP2(전고점 회복)가 먼저 발동하지 않도록. */
    private StockPosition positionAtEntry100000() {
        return StockPosition.open("005930", TODAY, new BigDecimal("100000"), 10,
            new BigDecimal("98800"), new BigDecimal("106000"));
    }

    /** TP1(+5%) 도달가. 수수료·세금 차감 순익 ≈ 4.759%. */
    private static KisQuoteResponse quoteAtTp1() {
        return new KisQuoteResponse("005930", new BigDecimal("105000"), new BigDecimal("98000"),
            new BigDecimal("106000"), new BigDecimal("97000"), new BigDecimal("97500"),
            BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("110"));
    }

    @Test
    void takeProfitHeldBackWhenSlippageEatsTheMargin() {
        when(kisApiClient.getQuote("005930")).thenReturn(quoteAtTp1());
        StockRiskService service = serviceWithSlippage(new BigDecimal("0.002")); // 0.2%

        service.checkPositionRisk(positionAtEntry100000());

        // 수수료 후 4.759% - 슬리피지 0.2% = 4.559% < 임계 4.6% → 보류
        verify(positionService, never()).executePartialExit(any(), anyInt(), any(), eq(StockCloseReason.TP1));
    }

    @Test
    void takeProfitFiresWhenNetOfSlippageClearsThreshold() {
        when(kisApiClient.getQuote("005930")).thenReturn(quoteAtTp1());
        StockRiskService service = serviceWithSlippage(BigDecimal.ZERO); // 슬리피지 미반영

        service.checkPositionRisk(positionAtEntry100000());

        // 수수료 후 4.759% >= 임계 4.6% → 발동
        verify(positionService).executePartialExit(any(), anyInt(), any(), eq(StockCloseReason.TP1));
    }
}
