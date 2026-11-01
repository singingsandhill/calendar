package me.singingsandhill.calendar.stock.infrastructure.persistence;

import me.singingsandhill.calendar.stock.domain.candle.CandleInterval;
import me.singingsandhill.calendar.stock.domain.candle.StockCandle;
import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalType;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockProductType;
import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeMode;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeStatus;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import me.singingsandhill.calendar.stock.infrastructure.persistence.adapter.StockCandleRepositoryAdapter;
import me.singingsandhill.calendar.stock.infrastructure.persistence.adapter.StockPositionRepositoryAdapter;
import me.singingsandhill.calendar.stock.infrastructure.persistence.adapter.StockRepositoryAdapter;
import me.singingsandhill.calendar.stock.infrastructure.persistence.adapter.StockSignalRepositoryAdapter;
import me.singingsandhill.calendar.stock.infrastructure.persistence.adapter.StockTradeRepositoryAdapter;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockCandleJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockPositionJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockSignalJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockTradeJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockCandleJpaRepository;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockJpaRepository;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockPositionJpaRepository;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockSignalJpaRepository;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockTradeJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 영속 ↔ 도메인 복원 회귀 테스트 (ADR stock/modes/0004, stock/observability/0003).
 *
 * 복원 경로가 생성자·팩토리를 불러 시각을 조회 시각으로 바꾸고 수치를 버리던 결함 — 운영에서
 * /api/stock/signals·/events 가 요청 시각과 null 을 돌려줬다.
 */
class StockPersistenceRestoreTest {

    /** 조회 시각과 확실히 구분되는 과거 시각. */
    private static final LocalDateTime T_PAST = LocalDateTime.of(2026, 10, 21, 9, 41, 7);

    private final StockTradeJpaRepository tradeJpa = mock(StockTradeJpaRepository.class);
    private final StockPositionJpaRepository positionJpa = mock(StockPositionJpaRepository.class);
    private final StockSignalJpaRepository signalJpa = mock(StockSignalJpaRepository.class);
    private final StockJpaRepository stockJpa = mock(StockJpaRepository.class);
    private final StockCandleJpaRepository candleJpa = mock(StockCandleJpaRepository.class);

    private static StockProperties propsIn(StockProperties.Bot.Mode mode) {
        StockProperties props = new StockProperties();
        props.getBot().setMode(mode);
        return props;
    }

    private StockTradeRepositoryAdapter tradeAdapter() {
        return new StockTradeRepositoryAdapter(tradeJpa, propsIn(StockProperties.Bot.Mode.PAPER));
    }

    @Test
    void tradeUpdatePersistsBrokerOrderId() {
        // 선영속화(PENDING-) 후 응답의 주문번호(SIM-/ODNO)를 붙여 저장해도 updateEntity 가 orderId 를 쓰지 않아
        // DB 의 매수 주문번호가 영원히 PENDING- 였다 — 가상 예수금 원장(SIM- 만 센다)이 매수를 차감하지 못하고,
        // 미확인 주문 스윕의 knownOrderIds 에서도 빠진다.
        StockTradeJpaEntity stored = new StockTradeJpaEntity(
            StockTrade.PENDING_ORDER_ID_PREFIX + "1", "005930", "BUY", "MARKET", 10, new BigDecimal("100000"));
        stored.setId(1L);
        when(tradeJpa.findById(1L)).thenReturn(Optional.of(stored));
        when(tradeJpa.save(any(StockTradeJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StockTrade trade = StockTrade.createBuyOrder(
            StockTrade.PENDING_ORDER_ID_PREFIX + "1", "005930", 10, new BigDecimal("100000"), true);
        trade.setId(1L);
        trade.assignBrokerOrderId("SIM-77");
        trade.markFilled(new BigDecimal("100200"), 10, new BigDecimal("150.30"));

        StockTrade reloaded = tradeAdapter().save(trade);

        assertThat(stored.getOrderId()).isEqualTo("SIM-77");
        assertThat(reloaded.getOrderId()).isEqualTo("SIM-77");
        assertThat(reloaded.isSimulated()).isTrue();
    }

    @Test
    void cancelledTradeIsNotRestoredAsPending() {
        // 체결 수량이 없으면 markFilled 를 건너뛰어 생성자 기본값 PENDING 으로 되살아났다 — 스윕이 이미 포기한
        // 미확인 주문이 다시 미확인으로 보이고, 미확인 매수 재주문 보류(infrastructure/0009)가 그 종목을 계속 막는다.
        StockTradeJpaEntity stored = new StockTradeJpaEntity(
            StockTrade.PENDING_ORDER_ID_PREFIX + "2", "005930", "BUY", "MARKET", 10, new BigDecimal("100000"));
        stored.setId(2L);
        stored.setStatus("CANCELLED");
        stored.setOrderedAt(T_PAST);
        stored.setCreatedAt(T_PAST);
        stored.setMode("LIVE");
        when(tradeJpa.findById(2L)).thenReturn(Optional.of(stored));

        StockTrade restored = tradeAdapter().findById(2L).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(StockTradeStatus.CANCELLED);
        assertThat(restored.isPending()).isFalse();
        assertThat(restored.getOrderedAt()).isEqualTo(T_PAST);
        assertThat(restored.getCreatedAt()).isEqualTo(T_PAST);
        assertThat(restored.getMode()).isEqualTo(StockTradeMode.LIVE);
    }

    @Test
    void filledTradeKeepsExecutionTime() {
        StockTradeJpaEntity stored = new StockTradeJpaEntity("SIM-3", "005930", "SELL", "MARKET", 5,
            new BigDecimal("103000"));
        stored.setId(3L);
        stored.setStatus("FILLED");
        stored.setExecutedQuantity(5);
        stored.setExecutedPrice(new BigDecimal("102700"));
        stored.setFee(new BigDecimal("1104.03"));
        stored.setExitReason("TP2");
        stored.setOrderedAt(T_PAST);
        stored.setExecutedAt(T_PAST.plusSeconds(1));
        stored.setCreatedAt(T_PAST);
        when(tradeJpa.findById(3L)).thenReturn(Optional.of(stored));

        StockTrade restored = tradeAdapter().findById(3L).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(StockTradeStatus.FILLED);
        assertThat(restored.getExecutedAt()).isEqualTo(T_PAST.plusSeconds(1));
        assertThat(restored.getExitReason()).isEqualTo(StockCloseReason.TP2);
        assertThat(restored.getExecutedPrice()).isEqualByComparingTo("102700");
    }

    @Test
    void newTradeIsStampedWithCurrentBotMode() {
        when(tradeJpa.save(any(StockTradeJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        StockTrade trade = StockTrade.createBuyOrder("PENDING-9", "005930", 10, new BigDecimal("100000"), true);

        StockTrade saved = new StockTradeRepositoryAdapter(tradeJpa, propsIn(StockProperties.Bot.Mode.LIVE))
            .save(trade);

        ArgumentCaptor<StockTradeJpaEntity> captor = ArgumentCaptor.forClass(StockTradeJpaEntity.class);
        verify(tradeJpa).save(captor.capture());
        assertThat(captor.getValue().getMode()).isEqualTo("LIVE");
        assertThat(captor.getValue().getOrderedAt()).isEqualTo(trade.getOrderedAt());
        assertThat(saved.getMode()).isEqualTo(StockTradeMode.LIVE);
    }

    @Test
    void positionRestoreKeepsCreatedAtExitTotalsAndMode() {
        // total_exit_amount/quantity 컬럼이 있는데 쓰지도 복원하지도 않아, 5초 틱마다 재로딩된 포지션의
        // 평균 청산가·실현손익률이 마지막 청산분만으로 계산됐다(부분청산 TP2 → 트레일링 잔여 같은 통상 경로).
        StockPositionJpaEntity stored = new StockPositionJpaEntity("005930", LocalDate.of(2026, 10, 21), "PARTIAL");
        stored.setId(4L);
        stored.setEntryPrice(new BigDecimal("100000"));
        stored.setEntryQuantity(100);
        stored.setEntryAmount(new BigDecimal("10000000"));
        stored.setRemainingQuantity(40);
        stored.setRealizedPnl(new BigDecimal("147855"));
        stored.setTotalExitAmount(new BigDecimal("6180000"));
        stored.setTotalExitQuantity(60);
        stored.setTp2Executed(true);
        stored.setCreatedAt(T_PAST);
        stored.setMode("PAPER");
        when(positionJpa.findById(4L)).thenReturn(Optional.of(stored));
        StockPositionRepositoryAdapter adapter =
            new StockPositionRepositoryAdapter(positionJpa, propsIn(StockProperties.Bot.Mode.PAPER));

        StockPosition restored = adapter.findById(4L).orElseThrow();
        restored.executePartialExit(40, new BigDecimal("102000"), StockCloseReason.TRAILING_STOP,
            new BigDecimal("0.00015"), new BigDecimal("0.0020"));

        assertThat(restored.getCreatedAt()).isEqualTo(T_PAST);
        assertThat(restored.getMode()).isEqualTo(StockTradeMode.PAPER);
        // (6,180,000 + 4,080,000) / 100 = 102,600 — 마지막 청산가 102,000 이 아니다
        assertThat(restored.getAverageExitPrice()).isEqualByComparingTo("102600");
    }

    @Test
    void newPositionIsStampedAndPersistsExitTotals() {
        when(positionJpa.save(any(StockPositionJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        StockPosition position = StockPosition.open("005930", LocalDate.of(2026, 10, 21),
            new BigDecimal("100000"), 100, new BigDecimal("98000"), new BigDecimal("103000"));
        position.executePartialExit(60, new BigDecimal("103000"), StockCloseReason.TP2,
            new BigDecimal("0.00015"), new BigDecimal("0.0020"));

        new StockPositionRepositoryAdapter(positionJpa, propsIn(StockProperties.Bot.Mode.PAPER)).save(position);

        ArgumentCaptor<StockPositionJpaEntity> captor = ArgumentCaptor.forClass(StockPositionJpaEntity.class);
        verify(positionJpa).save(captor.capture());
        assertThat(captor.getValue().getMode()).isEqualTo("PAPER");
        assertThat(captor.getValue().getTotalExitAmount()).isEqualByComparingTo("6180000");
        assertThat(captor.getValue().getTotalExitQuantity()).isEqualTo(60);
    }

    @Test
    void signalRestoreKeepsTimeNumbersAndExecutedFlag() {
        StockSignalJpaEntity stored = new StockSignalJpaEntity("005930", T_PAST, "PULLBACK_ENTRY");
        stored.setId(6L);
        stored.setHighPrice(new BigDecimal("101000"));
        stored.setPullbackPercent(new BigDecimal("-1.6"));
        stored.setBouncePercent(new BigDecimal("0.25"));
        stored.setCurrentPrice(new BigDecimal("99600"));
        stored.setGapPercent(new BigDecimal("3.1"));
        stored.setTradeStrength(new BigDecimal("112"));
        stored.setExecuted(true);
        stored.setCreatedAt(T_PAST);
        when(signalJpa.findById(6L)).thenReturn(Optional.of(stored));

        StockSignal restored = new StockSignalRepositoryAdapter(signalJpa).findById(6L).orElseThrow();

        assertThat(restored.getSignalType()).isEqualTo(StockSignalType.PULLBACK_ENTRY);
        assertThat(restored.getSignalTime()).isEqualTo(T_PAST);
        assertThat(restored.getCreatedAt()).isEqualTo(T_PAST);
        assertThat(restored.getHighPrice()).isEqualByComparingTo("101000");
        assertThat(restored.getPullbackPercent()).isEqualByComparingTo("-1.6");
        assertThat(restored.getBouncePercent()).isEqualByComparingTo("0.25");
        assertThat(restored.getCurrentPrice()).isEqualByComparingTo("99600");
        assertThat(restored.getGapPercent()).isEqualByComparingTo("3.1");
        assertThat(restored.getTradeStrength()).isEqualByComparingTo("112");
        assertThat(restored.isExecuted()).isTrue();
    }

    @Test
    void stockRestoreKeepsAuditTimestamps() {
        StockJpaEntity stored = new StockJpaEntity("005930", "삼성전자", LocalDate.of(2026, 10, 21), "WATCHING");
        stored.setId(7L);
        stored.setCreatedAt(T_PAST);
        stored.setUpdatedAt(T_PAST.plusMinutes(5));
        when(stockJpa.findById(7L)).thenReturn(Optional.of(stored));

        Stock restored = new StockRepositoryAdapter(stockJpa).findById(7L).orElseThrow();

        assertThat(restored.getCreatedAt()).isEqualTo(T_PAST);
        assertThat(restored.getUpdatedAt()).isEqualTo(T_PAST.plusMinutes(5));
    }

    @Test
    void stockProductTypeRoundTrips() {
        // ADR stock/algorithm/0011 — 상품 유형은 nullable 컬럼, 과거 행(null)은 모름 그대로
        when(stockJpa.save(any(StockJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        Stock etf = new Stock("069500", "KODEX 200", LocalDate.of(2026, 10, 23));
        etf.setProductType(StockProductType.ETF);

        Stock saved = new StockRepositoryAdapter(stockJpa).save(etf);

        ArgumentCaptor<StockJpaEntity> captor = ArgumentCaptor.forClass(StockJpaEntity.class);
        verify(stockJpa).save(captor.capture());
        assertThat(captor.getValue().getProductType()).isEqualTo("ETF");
        assertThat(saved.getProductType()).isEqualTo(StockProductType.ETF);

        StockJpaEntity legacy = new StockJpaEntity("005930", "삼성전자", LocalDate.of(2026, 10, 21), "WATCHING");
        legacy.setId(8L);
        when(stockJpa.findById(8L)).thenReturn(Optional.of(legacy));
        assertThat(new StockRepositoryAdapter(stockJpa).findById(8L).orElseThrow().getProductType()).isNull();
    }

    @Test
    void candleRestoreKeepsCreatedAt() {
        StockCandleJpaEntity stored = new StockCandleJpaEntity("005930", T_PAST, CandleInterval.MINUTE_1.name(),
            new BigDecimal("100000"), new BigDecimal("100500"), new BigDecimal("99800"), new BigDecimal("100200"),
            1000L, new BigDecimal("100200000"));
        stored.setId(8L);
        stored.setCreatedAt(T_PAST);
        when(candleJpa.findById(8L)).thenReturn(Optional.of(stored));

        StockCandle restored = new StockCandleRepositoryAdapter(candleJpa).findById(8L).orElseThrow();

        assertThat(restored.getCreatedAt()).isEqualTo(T_PAST);
    }
}
