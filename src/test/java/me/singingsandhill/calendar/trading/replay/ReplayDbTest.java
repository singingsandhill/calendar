package me.singingsandhill.calendar.trading.replay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB 사본 읽기 — UTC 호스트(DB 시각 = KST - 9h)를 1분봉 created_at 으로 추정하고, PAPER- 체결이 연결된 포지션을 뺀다.
 * 운영 스키마의 필요한 컬럼만 만든 인메모리 H2.
 */
class ReplayDbTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:replay-" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE trading_positions (id BIGINT PRIMARY KEY, market VARCHAR(20), status VARCHAR(10),"
                    + " entry_price DECIMAL(20,8), entry_volume DECIMAL(20,8), entry_fee DECIMAL(20,8), exit_price DECIMAL(20,8),"
                    + " close_reason VARCHAR(50), realized_pnl DECIMAL(20,8), opened_at TIMESTAMP, closed_at TIMESTAMP)");
            st.execute("CREATE TABLE trading_trades (id BIGINT PRIMARY KEY, uuid VARCHAR(50), position_id BIGINT)");
            st.execute("CREATE TABLE trading_candles (id BIGINT PRIMARY KEY, candle_date_time TIMESTAMP, created_at TIMESTAMP)");
            st.execute("CREATE TABLE trading_signals (id BIGINT PRIMARY KEY, market VARCHAR(20), signal_time TIMESTAMP,"
                    + " signal_type VARCHAR(10), total_score INT, rsi_divergence VARCHAR(10), stoch_divergence VARCHAR(10),"
                    + " current_price DECIMAL(20,8))");
            // UTC JVM: created_at = KST 봉 시각 - 9h + 저장 지연 1~3분
            st.execute("INSERT INTO trading_candles VALUES (1, '2026-08-01 10:00:00', '2026-08-01 01:01:00'),"
                    + " (2, '2026-08-01 10:01:00', '2026-08-01 01:03:00'), (3, '2026-08-01 10:02:00', '2026-08-01 01:04:00')");
            st.execute("INSERT INTO trading_positions VALUES"
                    + " (1, 'KRW-ADA', 'CLOSED', 290, 10, 1.16, 286, 'STOP_LOSS', -42, '2026-07-01 01:00:00', '2026-07-01 02:00:00'),"
                    + " (2, 'KRW-ADA', 'CLOSED', 290, 10, 0, 300, 'TAKE_PROFIT', 100, '2026-07-02 01:00:00', '2026-07-02 02:00:00'),"
                    + " (3, 'KRW-ADA', 'CLOSED', 290, 10, 0, 300, 'TAKE_PROFIT', 100, '2026-06-30 14:00:00', '2026-06-30 15:00:00'),"
                    + " (4, 'KRW-ADA', 'OPEN', 290, 10, 0, NULL, NULL, NULL, '2026-07-03 01:00:00', NULL)");
            st.execute("INSERT INTO trading_trades VALUES (1, 'live-uuid', 1), (2, 'PAPER-abc', 2)");
            st.execute("INSERT INTO trading_signals VALUES"
                    + " (1, 'KRW-ADA', '2026-07-01 01:05:10', 'SELL', -70, 'BEARISH', 'NONE', 289),"
                    + " (2, 'KRW-ADA', '2026-07-01 01:06:10', 'HOLD', 5, 'NONE', 'NONE', 290)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void detectsAUtcHostFromCandleInsertTimes() throws SQLException {
        assertThat(ReplayDb.detectOffset(connection)).isEqualTo(Duration.ofHours(-9));
    }

    @Test
    void readsLiveClosedPositionsInKst() throws SQLException {
        ReplayDb db = new ReplayDb(connection, Duration.ofHours(-9));

        // 07-01 00:00 KST = 06-30 15:00 UTC — #3(06-30 14:00 UTC)은 범위 밖, #2 는 PAPER-, #4 는 미청산
        List<ReplayPosition> positions = db.closedPositions("KRW-ADA", LocalDateTime.of(2026, 7, 1, 0, 0));

        assertThat(positions).extracting(ReplayPosition::id).containsExactly(1L);
        assertThat(positions.get(0).openedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
        assertThat(positions.get(0).recordedEntryFee()).isEqualByComparingTo("1.16");
        assertThat(db.countExcluded("KRW-ADA", LocalDateTime.of(2026, 7, 1, 0, 0))).isEqualTo(1);
    }

    @Test
    void readsSignalsInKstWithTheBearishDivergenceFlag() throws SQLException {
        ReplayDb db = new ReplayDb(connection, Duration.ofHours(-9));

        List<ReplaySignal> signals = db.signals("KRW-ADA", LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 11, 0));

        assertThat(signals).hasSize(2);
        assertThat(signals.get(0).time()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 5, 10));
        assertThat(signals.get(0).bearishDivergence()).isTrue();
        assertThat(signals.get(1).bearishDivergence()).isFalse();
    }

    @Test
    void aCopyWithTheModeColumnAlsoDropsNonLivePositions() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE trading_positions ADD COLUMN mode VARCHAR(10)");
            st.execute("UPDATE trading_positions SET mode = 'PAPER' WHERE id = 1");
        }
        ReplayDb db = new ReplayDb(connection, Duration.ofHours(-9));

        assertThat(db.closedPositions("KRW-ADA", LocalDateTime.of(2026, 7, 1, 0, 0))).isEmpty();
        assertThat(db.countExcluded("KRW-ADA", LocalDateTime.of(2026, 7, 1, 0, 0))).isEqualTo(2);
    }
}
