package me.singingsandhill.calendar.trading.infrastructure.persistence;

import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeMode;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import me.singingsandhill.calendar.trading.infrastructure.persistence.adapter.PositionRepositoryAdapter;
import me.singingsandhill.calendar.trading.infrastructure.persistence.adapter.TradeRepositoryAdapter;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.PositionJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.TradeJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.PositionJpaRepository;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.TradeJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 체결·포지션 운영 모드와 진입 출처·최저가 영속화 (ADR trading/observability/0004).
 *
 * 코인 체결 1,512건 중 PAPER- 44건이 LIVE 와 같은 테이블에 섞여 손익·승률 집계에 합산됐다.
 * insert 시점의 봇 모드를 어댑터가 찍는다 — 생성 지점(8곳)마다 설정하면 하나만 빠져도 LIVE 집계에 섞인다.
 */
class TradingPersistenceModeTest {

    private final TradeJpaRepository tradeJpa = mock(TradeJpaRepository.class);
    private final PositionJpaRepository positionJpa = mock(PositionJpaRepository.class);

    private static TradingProperties propsIn(TradingProperties.Bot.Mode mode) {
        TradingProperties props = new TradingProperties();
        props.getBot().setMode(mode);
        return props;
    }

    private static Position position() {
        return Position.open("KRW-ADA", new BigDecimal("290"), BigDecimal.TEN,
            new BigDecimal("285"), new BigDecimal("299"));
    }

    @Test
    void newTradeIsStampedWithCurrentBotMode() {
        when(tradeJpa.save(any(TradeJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TradeRepositoryAdapter adapter = new TradeRepositoryAdapter(tradeJpa, propsIn(TradingProperties.Bot.Mode.PAPER));
        Trade trade = Trade.createBuyOrder("PAPER-1", "KRW-ADA", new BigDecimal("290"),
            new BigDecimal("2900"), "market", null, "Auto buy signal");

        adapter.save(trade);

        ArgumentCaptor<TradeJpaEntity> captor = ArgumentCaptor.forClass(TradeJpaEntity.class);
        verify(tradeJpa).save(captor.capture());
        assertThat(captor.getValue().getMode()).isEqualTo("PAPER");
        assertThat(trade.getMode()).isEqualTo(TradeMode.PAPER);
    }

    @Test
    void persistedModeIsRestoredAndNotOverwrittenByCurrentMode() {
        // LIVE 행을 PAPER 프로세스가 다시 저장해도(스윕 등) LIVE 로 남아야 한다.
        TradeJpaEntity stored = new TradeJpaEntity("uuid-1", null, "KRW-ADA", "BUY", "market",
            new BigDecimal("290"), new BigDecimal("2900"), null, null, null, "SUBMITTED",
            null, null, java.time.LocalDateTime.now(), null, java.time.LocalDateTime.now(), "cid-1");
        stored.setId(5L);
        stored.setMode("LIVE");
        when(tradeJpa.findById(5L)).thenReturn(Optional.of(stored));
        when(tradeJpa.save(any(TradeJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TradeRepositoryAdapter adapter = new TradeRepositoryAdapter(tradeJpa, propsIn(TradingProperties.Bot.Mode.PAPER));

        Trade loaded = adapter.findById(5L).orElseThrow();
        adapter.save(loaded);

        assertThat(loaded.getMode()).isEqualTo(TradeMode.LIVE);
        ArgumentCaptor<TradeJpaEntity> captor = ArgumentCaptor.forClass(TradeJpaEntity.class);
        verify(tradeJpa).save(captor.capture());
        assertThat(captor.getValue().getMode()).isEqualTo("LIVE");
    }

    @Test
    void positionRoundTripCarriesModeEntrySourceAndLowWaterMark() {
        when(positionJpa.save(any(PositionJpaEntity.class))).thenAnswer(inv -> {
            PositionJpaEntity e = inv.getArgument(0);
            e.setId(9L);
            return e;
        });
        PositionRepositoryAdapter adapter = new PositionRepositoryAdapter(positionJpa,
            propsIn(TradingProperties.Bot.Mode.LIVE));
        Position position = position();
        position.setEntrySource(EntrySource.SIGNAL);
        position.updateLowWaterMark(new BigDecimal("287"));

        adapter.save(position);

        ArgumentCaptor<PositionJpaEntity> captor = ArgumentCaptor.forClass(PositionJpaEntity.class);
        verify(positionJpa).save(captor.capture());
        PositionJpaEntity entity = captor.getValue();
        assertThat(entity.getMode()).isEqualTo("LIVE");
        assertThat(entity.getEntrySource()).isEqualTo("SIGNAL");
        assertThat(entity.getLowWaterMark()).isEqualByComparingTo("287");

        when(positionJpa.findById(9L)).thenReturn(Optional.of(entity));
        Position reloaded = adapter.findById(9L).orElseThrow();
        assertThat(reloaded.getMode()).isEqualTo(TradeMode.LIVE);
        assertThat(reloaded.getEntrySource()).isEqualTo(EntrySource.SIGNAL);
        assertThat(reloaded.getLowWaterMark()).isEqualByComparingTo("287");
    }

    @Test
    void legacyPositionKeepsNullEntrySource() {
        // 과거분은 추정 백필하지 않는다 — null 이 "출처 불명" 이라는 정직한 값이다.
        PositionJpaEntity legacy = new PositionJpaEntity("KRW-ADA", "CLOSED", new BigDecimal("290"),
            BigDecimal.TEN, new BigDecimal("2900"), null, null, null, null, null, null, null, null,
            new BigDecimal("291"), false, "STOP_LOSS", java.time.LocalDateTime.now(), null,
            java.time.LocalDateTime.now(), null, null, null);
        legacy.setId(3L);
        when(positionJpa.findById(3L)).thenReturn(Optional.of(legacy));
        PositionRepositoryAdapter adapter = new PositionRepositoryAdapter(positionJpa,
            propsIn(TradingProperties.Bot.Mode.LIVE));

        Position reloaded = adapter.findById(3L).orElseThrow();

        assertThat(reloaded.getEntrySource()).isNull();
        assertThat(reloaded.getLowWaterMark()).isNull();
        assertThat(reloaded.getMode()).isNull();
    }
}
