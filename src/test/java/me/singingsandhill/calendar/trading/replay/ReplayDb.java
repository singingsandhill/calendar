package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * 운영 H2 DB 사본 읽기 (읽기 전용 — ACCESS_MODE_DATA=r, 없으면 만들지 않음 IFEXISTS). 운영 DB 에는 접속하지 않는다.
 *
 * <p>DB 의 시각 컬럼은 JVM 기본 시간대의 LocalDateTime 이다(운영 호스트 TZ 는 "추정 UTC, 단정 금지" — 서버 이관 런북). 1분봉
 * {@code candle_date_time} 만 빗썸 KST 값이므로, 같은 행의 {@code created_at - candle_date_time} 중앙값을 시간 단위로 반올림해
 * DB 시각 - KST 차이를 추정하고 모든 시각을 KST 로 바꾼다. replay.db-zone 을 주면 그 값을 쓴다.
 *
 * <p>PAPER 판별은 백필 SQL 과 같은 규칙 — 연결된 체결의 uuid 가 'PAPER-' 로 시작하면 제외. mode 컬럼이 있는 사본(배포 후)이면
 * LIVE·NULL 만 남긴다. 2026-07-12 이전 신호 매수는 체결-포지션 연결이 없어 그 시기의 PAPER 포지션은 가려내지 못한다.
 */
final class ReplayDb implements AutoCloseable {

    private static final int KST_OFFSET_SECONDS = 9 * 3600;

    private final Connection connection;
    private final Duration dbMinusKst;
    private final boolean hasModeColumn;

    ReplayDb(Connection connection, Duration dbMinusKst) throws SQLException {
        this.connection = connection;
        this.dbMinusKst = dbMinusKst;
        this.hasModeColumn = hasColumn(connection, "TRADING_POSITIONS", "MODE");
    }

    static ReplayDb open(Path file, String user, String password, ZoneId dbZone) throws SQLException {
        String base = file.toAbsolutePath().toString().replaceFirst("\\.mv\\.db$", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:file:" + base + ";ACCESS_MODE_DATA=r;IFEXISTS=TRUE;MODE=MySQL", user, password);
        Duration offset = dbZone != null
                ? Duration.ofSeconds(dbZone.getRules().getOffset(Instant.now()).getTotalSeconds() - KST_OFFSET_SECONDS)
                : detectOffset(connection);
        return new ReplayDb(connection, offset);
    }

    /** DB 시각 - KST. 1분봉 created_at(JVM 로컬) - candle_date_time(KST) 중앙값을 시간 단위로 반올림. */
    static Duration detectOffset(Connection connection) throws SQLException {
        List<Long> diffs = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT candle_date_time, created_at FROM trading_candles ORDER BY id DESC LIMIT 5000");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                diffs.add(MINUTES.between(rs.getObject(1, LocalDateTime.class), rs.getObject(2, LocalDateTime.class)));
            }
        }
        if (diffs.isEmpty()) {
            throw new IllegalStateException("trading_candles 가 비어 DB 시각대를 추정할 수 없다 — -Dreplay.db-zone=UTC 처럼 지정");
        }
        diffs.sort(Long::compare);
        long median = diffs.get(diffs.size() / 2);
        return Duration.ofHours(Math.round(median / 60.0));
    }

    Duration dbMinusKst() {
        return dbMinusKst;
    }

    LocalDateTime toKst(LocalDateTime dbLocal) {
        return dbLocal == null ? null : dbLocal.minus(dbMinusKst);
    }

    LocalDateTime toDb(LocalDateTime kst) {
        return kst.plus(dbMinusKst);
    }

    /** opened_at ≥ fromKst 인 청산 포지션(LIVE 추정), 진입 시각 오름차순. */
    List<ReplayPosition> closedPositions(String market, LocalDateTime fromKst) throws SQLException {
        List<ReplayPosition> positions = new ArrayList<>();
        String sql = "SELECT p.id, p.opened_at, p.closed_at, p.entry_price, p.entry_volume, p.entry_fee, p.exit_price,"
                + " p.close_reason, p.realized_pnl FROM trading_positions p WHERE " + liveClosedFilter()
                + " ORDER BY p.opened_at";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, market);
            ps.setObject(2, toDb(fromKst));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getBigDecimal(4) == null || rs.getBigDecimal(5) == null || rs.getBigDecimal(7) == null) {
                        continue;
                    }
                    positions.add(new ReplayPosition(rs.getLong(1), toKst(rs.getObject(2, LocalDateTime.class)),
                            toKst(rs.getObject(3, LocalDateTime.class)), rs.getBigDecimal(4), rs.getBigDecimal(5),
                            orZero(rs.getBigDecimal(6)), rs.getBigDecimal(7), rs.getString(8), orZero(rs.getBigDecimal(9))));
                }
            }
        }
        return positions;
    }

    /** 같은 범위에서 PAPER 로 판별돼 빠진 포지션 수 (보고서 기재용). */
    int countExcluded(String market, LocalDateTime fromKst) throws SQLException {
        String sql = "SELECT COUNT(*) FROM trading_positions p WHERE p.market = ? AND p.status = 'CLOSED'"
                + " AND p.closed_at IS NOT NULL AND p.opened_at >= ? AND NOT (" + liveFilterOnly() + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, market);
            ps.setObject(2, toDb(fromKst));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    List<ReplaySignal> signals(String market, LocalDateTime fromKst, LocalDateTime toKst) throws SQLException {
        List<ReplaySignal> signals = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT signal_time, signal_type, total_score, rsi_divergence, stoch_divergence, current_price"
                        + " FROM trading_signals WHERE market = ? AND signal_time >= ? AND signal_time < ? ORDER BY signal_time")) {
            ps.setString(1, market);
            ps.setObject(2, toDb(fromKst));
            ps.setObject(3, toDb(toKst));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean bearish = "BEARISH".equals(rs.getString(4)) || "BEARISH".equals(rs.getString(5));
                    signals.add(new ReplaySignal(toKst(rs.getObject(1, LocalDateTime.class)), rs.getString(2),
                            rs.getInt(3), bearish, rs.getBigDecimal(6)));
                }
            }
        }
        return signals;
    }

    private String liveClosedFilter() {
        return "p.market = ? AND p.status = 'CLOSED' AND p.closed_at IS NOT NULL AND p.opened_at >= ? AND " + liveFilterOnly();
    }

    private String liveFilterOnly() {
        String paper = "NOT EXISTS (SELECT 1 FROM trading_trades t WHERE t.position_id = p.id AND t.uuid LIKE 'PAPER-%')";
        return hasModeColumn ? "(" + paper + " AND (p.mode IS NULL OR p.mode = 'LIVE'))" : paper;
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
