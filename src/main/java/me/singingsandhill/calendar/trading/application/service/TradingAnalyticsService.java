package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.AnalyticsReport;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.SignalComponent;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.signal.SignalSample;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeType;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 신호 품질 분석 (ADR trading/observability/0001).
 *
 * <p>집계는 자바에서 스트리밍으로 한다 — 이 모듈에는 SQL 집계가 한 건도 없고
 * ({@code ProfitService} 도 전부 자바 집계) 그 관례를 깨지 않는다. 다만 신호 <b>읽기</b>만은
 * 엔티티가 아니라 {@link SignalSample} 투영을 쓴다 (사유는 그 클래스 Javadoc).
 *
 * <p>전방수익은 {@code trading_candles} 가 아니라 신호 자신의 {@code current_price} 로 계산한다.
 * 이유는 두 가지다. (1) 캔들은 정리되지만 신호는 영구 보관이라 지평이 길다. (2) 점수 입력과
 * 같은 가격 원천이라 서로 다른 소스를 섞을 때 생기는 기준가 불일치가 없다.
 */
@Service
@Transactional(readOnly = true)
public class TradingAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(TradingAnalyticsService.class);

    /** 전방수익 지평(분). */
    static final List<Integer> HORIZONS = List.of(15, 60);
    /** t+h 정각 행이 없을 때 허용하는 근접 오차. 이보다 멀면 그 행은 결측으로 버린다. */
    static final int FORWARD_TOLERANCE_SECONDS = 90;
    /** 포지션 진입 시각과 원인 신호 사이 허용 간격. 주문 왕복(재시도 sleep 최대 3초)을 감안한 값. */
    static final long ENTRY_MATCH_WINDOW_SECONDS = 90;
    static final int MAX_WINDOW_DAYS = 180;
    static final int DEFAULT_WINDOW_DAYS = 30;

    // 표본 충분성 문턱
    static final long MIN_DISTINCT_DAYS = 14;
    static final long MIN_SIGNAL_ROWS = 10_000;
    static final BigDecimal MIN_COVERAGE_PCT = new BigDecimal("60");
    static final long MIN_CLOSED_POSITIONS = 20;
    /** 독립 표본이 이보다 적으면 평균을 신뢰 표시하지 않는다. */
    static final long MIN_RELIABLE_EFFECTIVE_N = 30;

    /** MA60 하회 확인 게이트의 거래량 스파이크 배수 — SignalService:306-308 과 같은 값. */
    private static final double VOLUME_SPIKE_MULTIPLIER = 1.5;
    /** 같은 게이트의 강한 과매도 임계 — SignalService:305 의 하드코딩 30. */
    private static final BigDecimal STRONG_OVERSOLD_RSI = BigDecimal.valueOf(30);

    private static final int[] BUCKET_EDGES = {-60, -40, -20, 0, 20, 40, 60};
    private static final List<Integer> THRESHOLD_CANDIDATES = List.of(30, 40, 50, 60, 70);

    private final SignalRepository signalRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final TradingProperties tradingProperties;

    public TradingAnalyticsService(SignalRepository signalRepository,
                                   PositionRepository positionRepository,
                                   TradeRepository tradeRepository,
                                   TradingProperties tradingProperties) {
        this.signalRepository = signalRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.tradingProperties = tradingProperties;
    }

    /** 요청 구간을 1..{@link #MAX_WINDOW_DAYS} 로 제한한다. 상한은 대구간 조회의 메모리·시간 방어. */
    public static int clampDays(int days) {
        return Math.max(1, Math.min(days, MAX_WINDOW_DAYS));
    }

    /** 컨트롤러 진입점. days 는 {@link #clampDays} 로 제한된다. */
    public AnalyticsReport analyze(int days) {
        LocalDateTime to = LocalDateTime.now();
        return analyze(to.minusDays(clampDays(days)), to);
    }

    public AnalyticsReport analyze(LocalDateTime from, LocalDateTime to) {
        String market = tradingProperties.getBot().getMarket();
        long startedAt = System.nanoTime();

        List<SignalSample> samples = signalRepository.findSamplesByMarketAndSignalTimeBetween(market, from, to);
        List<Position> closed = positionRepository.findByMarketAndStatusAndClosedAtBetween(
                market, PositionStatus.CLOSED, from, to);
        List<Trade> trades = tradeRepository.findByMarketAndCreatedAtBetween(market, from, to);

        AnalyticsReport report = buildReport(market, from, to, samples, closed, trades);
        log.debug("Analytics for {} [{} ~ {}]: {} signals, {} closed positions",
                market, from, to, samples.size(), closed.size());
        return withComputeMillis(report, (System.nanoTime() - startedAt) / 1_000_000);
    }

    // ------------------------------------------------------------------
    // 순수 집계 — 저장소 접근 없음. 테스트는 여기로 들어온다
    // (stock 의 DailyPerformanceReportService.buildReport 선례).
    // ------------------------------------------------------------------

    AnalyticsReport buildReport(String market,
                                LocalDateTime from,
                                LocalDateTime to,
                                List<SignalSample> samplesAsc,
                                List<Position> closedPositions,
                                List<Trade> trades) {

        List<SignalSample> samples = samplesAsc.stream()
                .filter(s -> s.signalTime() != null)
                .sorted(Comparator.comparing(SignalSample::signalTime))
                .toList();

        Map<Integer, List<ForwardPoint>> forward = new LinkedHashMap<>();
        for (int horizon : HORIZONS) {
            forward.put(horizon, resolveForward(samples, horizon));
        }

        AnalyticsReport.Coverage coverage = buildCoverage(from, to, samples, forward, closedPositions);
        BigDecimal roundTripFeePct = roundTripFeePct();

        return new AnalyticsReport(
                market,
                new AnalyticsReport.Window(from, to, (int) Math.max(1, Duration.between(from, to).toDays()), HORIZONS),
                coverage,
                buildScoreBuckets(samples, forward, roundTripFeePct),
                buildComponentEdge(samples, forward, roundTripFeePct),
                buildThresholdScenarios(samples, forward, roundTripFeePct),
                buildOutcomeByCloseReason(closedPositions),
                buildEntryContext(samples, closedPositions, trades),
                buildCostReality(closedPositions),
                buildCaveats(coverage),
                0L
        );
    }

    // ---------------- 전방수익 ----------------

    /** 전방 조회 결과 한 건. {@code resolved=false} 면 그 행은 이 지평의 통계에서 빠진다. */
    record ForwardPoint(BigDecimal returnPct, boolean resolved) {}

    /**
     * 각 관측치에 대해 {@code t + horizon} 시점 가격을 찾아 수익률(%)을 만든다.
     *
     * <p>인덱스 산술({@code i + horizon})을 쓰지 않는다 — 봇 정지·일시정지·신호 생성 실패로
     * 시계열에 구멍이 있어서, 인덱스로 세면 조용히 엉뚱한 시각의 가격을 집는다. 목표 시각의
     * 앞뒤 후보 중 더 가까운 쪽을 고르고, 그것도 허용오차를 넘으면 결측으로 버린다.
     */
    static List<ForwardPoint> resolveForward(List<SignalSample> asc, int horizonMinutes) {
        List<ForwardPoint> out = new ArrayList<>(asc.size());
        int j = 0;
        for (SignalSample base : asc) {
            LocalDateTime target = base.signalTime().plusMinutes(horizonMinutes);
            while (j < asc.size() && asc.get(j).signalTime().isBefore(target)) {
                j++;
            }
            SignalSample best = null;
            long bestGap = Long.MAX_VALUE;
            for (int k = j - 1; k <= j; k++) {
                if (k < 0 || k >= asc.size()) {
                    continue;
                }
                SignalSample candidate = asc.get(k);
                if (!candidate.signalTime().isAfter(base.signalTime())) {
                    continue;   // 자기 자신 또는 과거는 전방이 아니다
                }
                long gap = Math.abs(Duration.between(target, candidate.signalTime()).toSeconds());
                if (gap < bestGap) {
                    bestGap = gap;
                    best = candidate;
                }
            }
            if (best == null || bestGap > FORWARD_TOLERANCE_SECONDS
                    || base.currentPrice() == null || best.currentPrice() == null
                    || base.currentPrice().signum() == 0) {
                out.add(new ForwardPoint(null, false));
                continue;
            }
            BigDecimal ret = best.currentPrice().subtract(base.currentPrice())
                    .divide(base.currentPrice(), 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            out.add(new ForwardPoint(ret, true));
        }
        return out;
    }

    /**
     * 부분집합의 전방수익 통계. {@code effectiveN} 은 겹치는 창을 보정한 독립 표본 근사다 —
     * 매 분 관측치가 생기므로 +60분 수익률은 인접 행끼리 59분이 겹친다.
     */
    static AnalyticsReport.HorizonStat stat(int horizonMinutes, List<BigDecimal> returns, BigDecimal roundTripFeePct) {
        if (returns.isEmpty()) {
            return new AnalyticsReport.HorizonStat(horizonMinutes, 0, 0, null, null, null, null);
        }
        List<BigDecimal> sorted = returns.stream().sorted().toList();
        BigDecimal sum = BigDecimal.ZERO;
        long wins = 0;
        for (BigDecimal r : sorted) {
            sum = sum.add(r);
            if (r.signum() > 0) {
                wins++;
            }
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(sorted.size()), 4, RoundingMode.HALF_UP);
        BigDecimal winRate = BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sorted.size()), 2, RoundingMode.HALF_UP);
        return new AnalyticsReport.HorizonStat(
                horizonMinutes,
                sorted.size(),
                sorted.size() / horizonMinutes,
                mean,
                median(sorted),
                winRate,
                mean.subtract(roundTripFeePct)
        );
    }

    static BigDecimal median(List<BigDecimal> ascending) {
        int n = ascending.size();
        if (n == 0) {
            return null;
        }
        if (n % 2 == 1) {
            return ascending.get(n / 2);
        }
        return ascending.get(n / 2 - 1).add(ascending.get(n / 2))
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    /**
     * 조건을 만족하는 <b>연속 구간</b>의 개수. 부분집합의 n 이 커도 그것이 몇 개의 서로 다른
     * 국면에서 나왔는지는 별개다 — total_score 는 천천히 움직여서 5,000행이 실제로는
     * 수십 개 에피소드일 수 있다.
     */
    static long countEpisodes(List<SignalSample> asc, Predicate<SignalSample> member) {
        long episodes = 0;
        boolean inRun = false;
        for (SignalSample s : asc) {
            if (member.test(s)) {
                if (!inRun) {
                    episodes++;
                    inRun = true;
                }
            } else {
                inRun = false;
            }
        }
        return episodes;
    }

    private Map<Integer, AnalyticsReport.HorizonStat> statsFor(List<SignalSample> samples,
                                                              Map<Integer, List<ForwardPoint>> forward,
                                                              Predicate<SignalSample> member,
                                                              BigDecimal roundTripFeePct) {
        Map<Integer, AnalyticsReport.HorizonStat> byHorizon = new LinkedHashMap<>();
        for (int horizon : HORIZONS) {
            List<ForwardPoint> points = forward.get(horizon);
            List<BigDecimal> returns = new ArrayList<>();
            for (int i = 0; i < samples.size(); i++) {
                ForwardPoint p = points.get(i);
                if (p.resolved() && member.test(samples.get(i))) {
                    returns.add(p.returnPct());
                }
            }
            byHorizon.put(horizon, stat(horizon, returns, roundTripFeePct));
        }
        return byHorizon;
    }

    // ---------------- 섹션 1: 커버리지 ----------------

    private AnalyticsReport.Coverage buildCoverage(LocalDateTime from,
                                                   LocalDateTime to,
                                                   List<SignalSample> samples,
                                                   Map<Integer, List<ForwardPoint>> forward,
                                                   List<Position> closedPositions) {
        long expectedMinutes = Math.max(1, Duration.between(from, to).toMinutes());
        long rows = samples.size();
        BigDecimal coveragePct = BigDecimal.valueOf(rows)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(expectedMinutes), 2, RoundingMode.HALF_UP);

        int gapCount = 0;
        long longestGap = 0;
        for (int i = 1; i < samples.size(); i++) {
            long gapMinutes = Duration.between(samples.get(i - 1).signalTime(), samples.get(i).signalTime()).toMinutes();
            if (gapMinutes >= 2) {
                gapCount++;
                longestGap = Math.max(longestGap, gapMinutes);
            }
        }

        Map<Integer, Long> resolved = new LinkedHashMap<>();
        forward.forEach((horizon, points) -> resolved.put(horizon, points.stream().filter(ForwardPoint::resolved).count()));

        long distinctDays = samples.stream().map(s -> s.signalTime().toLocalDate()).distinct().count();
        long buy = samples.stream().filter(s -> s.signalType() == SignalType.BUY).count();
        long sell = samples.stream().filter(s -> s.signalType() == SignalType.SELL).count();
        long hold = rows - buy - sell;
        long withVolume = samples.stream().filter(SignalSample::hasVolumeContext).count();
        long withAtr = samples.stream().filter(s -> s.atrPercent() != null).count();
        long stochIdentical = samples.stream()
                .filter(s -> s.stochK() != null && s.stochD() != null && s.stochK().compareTo(s.stochD()) == 0)
                .count();

        List<String> thin = new ArrayList<>();
        if (distinctDays < MIN_DISTINCT_DAYS) {
            thin.add("거래일 %d일 < 권장 %d일".formatted(distinctDays, MIN_DISTINCT_DAYS));
        }
        if (rows < MIN_SIGNAL_ROWS) {
            thin.add("신호 %d행 < 권장 %d행".formatted(rows, MIN_SIGNAL_ROWS));
        }
        if (coveragePct.compareTo(MIN_COVERAGE_PCT) < 0) {
            thin.add("커버리지 %s%% < 권장 %s%%".formatted(coveragePct.toPlainString(), MIN_COVERAGE_PCT.toPlainString()));
        }
        boolean signalSufficient = thin.isEmpty();
        boolean outcomeSufficient = closedPositions.size() >= MIN_CLOSED_POSITIONS;
        if (!outcomeSufficient) {
            thin.add("청산 포지션 %d건 < 권장 %d건".formatted(closedPositions.size(), MIN_CLOSED_POSITIONS));
        }

        return new AnalyticsReport.Coverage(
                rows, expectedMinutes, coveragePct,
                samples.isEmpty() ? null : samples.get(0).signalTime(),
                samples.isEmpty() ? null : samples.get(samples.size() - 1).signalTime(),
                distinctDays, gapCount, longestGap, resolved,
                buy, sell, hold, withVolume, withAtr, stochIdentical,
                closedPositions.size(), signalSufficient, outcomeSufficient, List.copyOf(thin));
    }

    // ---------------- 섹션 2: 점수 구간별 전방수익 ----------------

    private List<AnalyticsReport.ScoreBucketRow> buildScoreBuckets(List<SignalSample> samples,
                                                                   Map<Integer, List<ForwardPoint>> forward,
                                                                   BigDecimal roundTripFeePct) {
        int buyThreshold = tradingProperties.getThresholds().getSignalBuy();
        int sellThreshold = tradingProperties.getThresholds().getSignalSell();

        List<AnalyticsReport.ScoreBucketRow> rows = new ArrayList<>();
        for (int i = 0; i <= BUCKET_EDGES.length; i++) {
            Integer lower = i == 0 ? null : BUCKET_EDGES[i - 1];
            Integer upper = i == BUCKET_EDGES.length ? null : BUCKET_EDGES[i];
            Predicate<SignalSample> member = s ->
                    (lower == null || s.totalScore() >= lower) && (upper == null || s.totalScore() < upper);

            long n = samples.stream().filter(member).count();
            rows.add(new AnalyticsReport.ScoreBucketRow(
                    bucketLabel(lower, upper), lower, upper,
                    n, countEpisodes(samples, member),
                    statsFor(samples, forward, member, roundTripFeePct),
                    inBucket(buyThreshold, lower, upper),
                    inBucket(sellThreshold, lower, upper)));
        }
        return rows;
    }

    private static boolean inBucket(int value, Integer lower, Integer upper) {
        return (lower == null || value >= lower) && (upper == null || value < upper);
    }

    private static String bucketLabel(Integer lower, Integer upper) {
        if (lower == null) {
            return "< " + upper;
        }
        if (upper == null) {
            return "≥ " + lower;
        }
        return lower + " ~ " + upper;
    }

    // ---------------- 섹션 3: 구성요소별 조건부 엣지 ----------------

    private List<AnalyticsReport.ComponentEdgeRow> buildComponentEdge(List<SignalSample> samples,
                                                                      Map<Integer, List<ForwardPoint>> forward,
                                                                      BigDecimal roundTripFeePct) {
        List<AnalyticsReport.ComponentEdgeRow> rows = new ArrayList<>();
        for (SignalComponent component : SignalComponent.values()) {
            rows.add(new AnalyticsReport.ComponentEdgeRow(
                    component.name(),
                    component.getDisplayName(),
                    component.getMaxAbsWeight(),
                    componentState(samples, forward, component, "양수", v -> v != null && v > 0, roundTripFeePct),
                    componentState(samples, forward, component, "0", v -> v == null || v == 0, roundTripFeePct),
                    componentState(samples, forward, component, "음수", v -> v != null && v < 0, roundTripFeePct),
                    componentState(samples, forward, component, "전체", v -> true, roundTripFeePct)));
        }
        return rows;
    }

    private AnalyticsReport.ComponentStateStat componentState(List<SignalSample> samples,
                                                              Map<Integer, List<ForwardPoint>> forward,
                                                              SignalComponent component,
                                                              String label,
                                                              Predicate<Integer> scoreTest,
                                                              BigDecimal roundTripFeePct) {
        Predicate<SignalSample> member = s -> scoreTest.test(s.score(component));
        List<SignalSample> subset = samples.stream().filter(member).toList();

        // totalScore 에서 이 구성요소의 몫을 뺀 잔차. baseline 보다 높으면 그 부분집합은
        // "이 구성요소가 켜진 구간" 이 아니라 "원래 점수가 높은 구간" 이다.
        BigDecimal residual = null;
        if (!subset.isEmpty()) {
            long sum = 0;
            for (SignalSample s : subset) {
                Integer own = s.score(component);
                sum += s.totalScore() - (own != null ? own : 0);
            }
            residual = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(subset.size()), 2, RoundingMode.HALF_UP);
        }

        return new AnalyticsReport.ComponentStateStat(
                label, subset.size(), countEpisodes(samples, member), residual,
                statsFor(samples, forward, member, roundTripFeePct));
    }

    // ---------------- 섹션 4: 임계 반사실 ----------------

    /** 후보 임계에서 매수 게이트를 통과했겠는가. 거래량 맥락이 없으면 판정불가 범위로 나온다. */
    record GateOutcome(boolean passesLow, boolean passesHigh, boolean indeterminate, BlockReason firstBlock) {}

    enum BlockReason { NONE, SCORE, RSI, STOCH_K, AGREEMENT, MA60_CONFIRMATION }

    /**
     * {@code SignalService.determineSignalType} 의 매수 분기를 재현한다.
     *
     * <p>공용 헬퍼로 추출하지 않고 <b>일부러 복제</b>했다 — 추출하면 분석 기능을 위해 실주문
     * 신호 생성 경로를 수정해야 하고, 그것이 이 작업 전체가 피하려는 위험이다. 대신
     * {@code TradingAnalyticsGateParityTest} 가 두 구현의 일치를 빌드 실패로 강제한다.
     */
    GateOutcome evaluateBuyGate(SignalSample s, int candidateThreshold) {
        TradingProperties.Thresholds t = tradingProperties.getThresholds();

        int agreeing = countAgreeing(s);
        if (agreeing < t.getMinAgreeingIndicators()) {
            return new GateOutcome(false, false, false, BlockReason.AGREEMENT);
        }
        if (s.totalScore() < candidateThreshold) {
            return new GateOutcome(false, false, false, BlockReason.SCORE);
        }
        if (s.rsi() == null || s.rsi().compareTo(BigDecimal.valueOf(t.getBuyRsiMax())) >= 0) {
            return new GateOutcome(false, false, false, BlockReason.RSI);
        }
        if (s.stochK() == null || s.stochK().compareTo(BigDecimal.valueOf(t.getBuyStochKMax())) >= 0) {
            return new GateOutcome(false, false, false, BlockReason.STOCH_K);
        }
        if (s.ma60() != null && s.isPriceBelowMa60()) {
            boolean bullish = s.hasBullishDivergence();
            boolean strongOversold = s.rsi().compareTo(STRONG_OVERSOLD_RSI) < 0;
            if (bullish || strongOversold) {
                return new GateOutcome(true, true, false, BlockReason.NONE);
            }
            if (!s.hasVolumeContext()) {
                // 거래량 스파이크 분기를 판정할 수 없다 — 없다고 보면 차단, 있다고 보면 통과.
                return new GateOutcome(false, true, true, BlockReason.MA60_CONFIRMATION);
            }
            boolean spike = s.hasVolumeSpike(VOLUME_SPIKE_MULTIPLIER);
            return spike
                    ? new GateOutcome(true, true, false, BlockReason.NONE)
                    : new GateOutcome(false, false, false, BlockReason.MA60_CONFIRMATION);
        }
        return new GateOutcome(true, true, false, BlockReason.NONE);
    }

    /** {@code SignalService.countAgreeingIndicators} 와 같은 식 — 총점 방향과 부호가 같은 구성요소 수. */
    static int countAgreeing(SignalSample s) {
        int direction = s.totalScore() >= 0 ? 1 : -1;
        int count = 0;
        for (SignalComponent c : SignalComponent.values()) {
            Integer score = s.score(c);
            if (score != null && score * direction > 0) {
                count++;
            }
        }
        return count;
    }

    private List<AnalyticsReport.ThresholdScenarioRow> buildThresholdScenarios(List<SignalSample> samples,
                                                                               Map<Integer, List<ForwardPoint>> forward,
                                                                               BigDecimal roundTripFeePct) {
        int current = tradingProperties.getThresholds().getSignalBuy();
        List<AnalyticsReport.ThresholdScenarioRow> rows = new ArrayList<>();

        for (int candidate : THRESHOLD_CANDIDATES) {
            long scoreOnly = 0;
            long low = 0;
            long high = 0;
            long indeterminate = 0;
            Map<BlockReason, Long> blocks = new HashMap<>();

            for (SignalSample s : samples) {
                if (s.totalScore() >= candidate) {
                    scoreOnly++;
                }
                GateOutcome outcome = evaluateBuyGate(s, candidate);
                if (outcome.passesLow()) {
                    low++;
                }
                if (outcome.passesHigh()) {
                    high++;
                }
                if (outcome.indeterminate()) {
                    indeterminate++;
                }
                if (outcome.firstBlock() != BlockReason.NONE && s.totalScore() >= candidate) {
                    blocks.merge(outcome.firstBlock(), 1L, Long::sum);
                }
            }

            final int c = candidate;
            Predicate<SignalSample> passes = s -> evaluateBuyGate(s, c).passesLow();
            rows.add(new AnalyticsReport.ThresholdScenarioRow(
                    candidate, scoreOnly, low, high, indeterminate,
                    countEpisodes(samples, passes),
                    blocks.getOrDefault(BlockReason.RSI, 0L),
                    blocks.getOrDefault(BlockReason.STOCH_K, 0L),
                    blocks.getOrDefault(BlockReason.AGREEMENT, 0L),
                    blocks.getOrDefault(BlockReason.MA60_CONFIRMATION, 0L),
                    statsFor(samples, forward, passes, roundTripFeePct),
                    candidate == current));
        }
        return rows;
    }

    // ---------------- 섹션 5: 청산사유별 실현 성과 ----------------

    private List<AnalyticsReport.CloseReasonRow> buildOutcomeByCloseReason(List<Position> closed) {
        Map<String, List<Position>> grouped = new LinkedHashMap<>();
        for (Position p : closed) {
            String key = p.getCloseReason() != null ? p.getCloseReason().name() : "UNKNOWN";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        List<AnalyticsReport.CloseReasonRow> rows = new ArrayList<>();
        grouped.forEach((reason, group) -> {
            List<BigDecimal> pnlPcts = group.stream()
                    .map(Position::getRealizedPnlPct)
                    .filter(v -> v != null)
                    .sorted()
                    .toList();
            BigDecimal totalPnl = sum(group.stream().map(Position::getRealizedPnl).toList());
            BigDecimal totalFees = sum(group.stream().map(Position::getTotalFees).toList());
            long wins = group.stream()
                    .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().signum() > 0)
                    .count();
            rows.add(new AnalyticsReport.CloseReasonRow(
                    reason, group.size(),
                    mean(pnlPcts), median(pnlPcts),
                    totalPnl, totalFees,
                    meanHoldMinutes(group), wins,
                    percentOf(wins, group.size())));
        });
        rows.sort(Comparator.comparingLong(AnalyticsReport.CloseReasonRow::n).reversed());
        return rows;
    }

    // ---------------- 섹션 6: 진입 맥락 → 결과 ----------------

    private AnalyticsReport.EntryContext buildEntryContext(List<SignalSample> samples,
                                                           List<Position> closed,
                                                           List<Trade> trades) {
        // 신호 기반 진입만 본다. 리밸런싱 매수는 비중을 맞추려고 사는 것이라 섞으면
        // 신호의 예측력이 희석된다.
        Map<Long, Trade> buyTradeByPosition = new HashMap<>();
        for (Trade t : trades) {
            if (t.getTradeType() == TradeType.BUY && t.getPositionId() != null) {
                buyTradeByPosition.putIfAbsent(t.getPositionId(), t);
            }
        }
        Map<Long, SignalSample> sampleById = new HashMap<>();
        for (SignalSample s : samples) {
            if (s.id() != null) {
                sampleById.put(s.id(), s);
            }
        }

        long bySignalId = 0;
        long byTimestamp = 0;
        long byFollowing = 0;
        long unmatched = 0;
        long scoreConfirmed = 0;
        long excluded = 0;
        List<Matched> matched = new ArrayList<>();

        for (Position p : closed) {
            Trade buy = buyTradeByPosition.get(p.getId());
            if (buy != null && !isSignalDriven(buy)) {
                excluded++;
                continue;
            }

            SignalSample origin = null;
            if (buy != null && buy.getSignalId() != null) {
                origin = sampleById.get(buy.getSignalId());
                if (origin != null) {
                    bySignalId++;
                }
            }
            if (origin == null) {
                origin = precedingSignal(samples, p.getOpenedAt());
                if (origin != null) {
                    byTimestamp++;
                }
            }
            if (origin == null) {
                origin = followingSignal(samples, p.getOpenedAt());
                if (origin != null) {
                    byFollowing++;
                    continue;   // 인과 방향이 뒤집힌 매칭은 버킷 집계에서 뺀다
                }
            }
            if (origin == null) {
                unmatched++;
                continue;
            }
            if (buy != null && buy.getSignalScore() != null && buy.getSignalScore() == origin.totalScore()) {
                scoreConfirmed++;
            }
            matched.add(new Matched(p, origin));
        }

        long signalDriven = matched.size() + byFollowing + unmatched;
        return new AnalyticsReport.EntryContext(
                closed.size(), signalDriven, excluded,
                bySignalId, byTimestamp, byFollowing, unmatched, scoreConfirmed,
                bucketBy(matched, m -> scoreBandLabel(m.signal().totalScore())),
                bucketBy(matched, m -> rsiBandLabel(m.signal().rsi())),
                bucketBy(matched, m -> m.signal().isPriceBelowMa60() ? "MA60 하회" : "MA60 상회"));
    }

    private record Matched(Position position, SignalSample signal) {}

    /**
     * 원인 신호는 반드시 체결보다 <b>앞선다</b>. 대칭 최근접 매칭을 하면 T+31초에 열린 포지션이
     * T+60초 신호(거리 29초)에 붙어, 그 매매 뒤에 생성된 신호를 원인으로 기록하게 된다.
     */
    static SignalSample precedingSignal(List<SignalSample> asc, LocalDateTime openedAt) {
        if (openedAt == null) {
            return null;
        }
        SignalSample best = null;
        for (SignalSample s : asc) {
            if (s.signalTime().isAfter(openedAt)) {
                break;
            }
            if (Duration.between(s.signalTime(), openedAt).toSeconds() <= ENTRY_MATCH_WINDOW_SECONDS) {
                best = s;
            }
        }
        return best;
    }

    static SignalSample followingSignal(List<SignalSample> asc, LocalDateTime openedAt) {
        if (openedAt == null) {
            return null;
        }
        for (SignalSample s : asc) {
            if (s.signalTime().isAfter(openedAt)) {
                return Duration.between(openedAt, s.signalTime()).toSeconds() <= ENTRY_MATCH_WINDOW_SECONDS ? s : null;
            }
        }
        return null;
    }

    private static boolean isSignalDriven(Trade buy) {
        if (buy.getSignalId() != null) {
            return true;
        }
        return "Auto buy signal".equals(buy.getSignalReason());
    }

    private List<AnalyticsReport.EntryBucketRow> bucketBy(List<Matched> matched,
                                                          java.util.function.Function<Matched, String> classifier) {
        Map<String, List<Matched>> grouped = new LinkedHashMap<>();
        for (Matched m : matched) {
            grouped.computeIfAbsent(classifier.apply(m), k -> new ArrayList<>()).add(m);
        }
        List<AnalyticsReport.EntryBucketRow> rows = new ArrayList<>();
        grouped.forEach((label, group) -> {
            List<Position> positions = group.stream().map(Matched::position).toList();
            List<BigDecimal> pnlPcts = positions.stream()
                    .map(Position::getRealizedPnlPct)
                    .filter(v -> v != null)
                    .sorted()
                    .toList();
            long wins = positions.stream()
                    .filter(p -> p.getRealizedPnl() != null && p.getRealizedPnl().signum() > 0)
                    .count();
            rows.add(new AnalyticsReport.EntryBucketRow(
                    label, positions.size(), wins, percentOf(wins, positions.size()),
                    mean(pnlPcts), median(pnlPcts),
                    sum(positions.stream().map(Position::getRealizedPnl).toList()),
                    meanHoldMinutes(positions)));
        });
        rows.sort(Comparator.comparing(AnalyticsReport.EntryBucketRow::bucketLabel));
        return rows;
    }

    private static String scoreBandLabel(int score) {
        if (score >= 60) return "≥ 60";
        if (score >= 40) return "40 ~ 60";
        if (score >= 20) return "20 ~ 40";
        return "< 20";
    }

    private static String rsiBandLabel(BigDecimal rsi) {
        if (rsi == null) return "미측정";
        if (rsi.compareTo(BigDecimal.valueOf(30)) < 0) return "< 30";
        if (rsi.compareTo(BigDecimal.valueOf(50)) < 0) return "30 ~ 50";
        if (rsi.compareTo(BigDecimal.valueOf(70)) < 0) return "50 ~ 70";
        return "≥ 70";
    }

    // ---------------- 섹션 7: 비용 현실 ----------------

    private AnalyticsReport.CostReality buildCostReality(List<Position> closed) {
        BigDecimal notional = sum(closed.stream().map(Position::getEntryAmount).toList());
        BigDecimal fees = sum(closed.stream().map(Position::getTotalFees).toList());
        BigDecimal net = sum(closed.stream().map(Position::getRealizedPnl).toList());
        BigDecimal gross = net.add(fees);

        BigDecimal feesOfNotional = notional.signum() > 0
                ? fees.multiply(BigDecimal.valueOf(100)).divide(notional, 4, RoundingMode.HALF_UP)
                : null;
        // gross ≤ 0 이면 이 비율은 부호가 뒤집히거나 0 근처에서 발산해 의미가 없다. 계산하지 않는다.
        BigDecimal feesOfGross = gross.signum() > 0
                ? fees.multiply(BigDecimal.valueOf(100)).divide(gross, 4, RoundingMode.HALF_UP)
                : null;
        BigDecimal avgFee = closed.isEmpty()
                ? null
                : fees.divide(BigDecimal.valueOf(closed.size()), 2, RoundingMode.HALF_UP);

        return new AnalyticsReport.CostReality(
                closed.size(), notional, gross, fees, net,
                feesOfNotional, feesOfGross, avgFee, roundTripFeePct());
    }

    /** 왕복 taker 수수료(%). 진입·청산 두 번 물기 때문에 2배다. */
    private BigDecimal roundTripFeePct() {
        return BigDecimal.valueOf(tradingProperties.getRisk().getTakerFeeRate())
                .multiply(BigDecimal.valueOf(200))
                .setScale(4, RoundingMode.HALF_UP);
    }

    // ---------------- 고지 ----------------

    private List<String> buildCaveats(AnalyticsReport.Coverage coverage) {
        List<String> caveats = new ArrayList<>();
        caveats.add("전방수익은 수수료·슬리피지 차감 전(gross) 값이다. net 컬럼과 함께 읽어야 한다 — "
                + "왕복 taker 수수료만 " + roundTripFeePct().toPlainString() + "% 다.");
        caveats.add("매 분 관측치가 생기므로 전방수익 창이 겹친다. n 이 아니라 독립창(effectiveN)을 보고 판단한다.");
        if (!tradingProperties.getIndicators().isExcludeFormingCandle()) {
            caveats.add("exclude-forming-candle 이 꺼져 있어 저장된 지표는 형성 중인 봉을 포함한다 — 경미한 리페인트.");
        }
        if (coverage.rowsWithVolumeContext() < coverage.signalRows()) {
            caveats.add("거래량 맥락이 없는 행 %d개는 MA60 하회 확인 게이트를 판정할 수 없어 임계 반사실이 범위로 나온다."
                    .formatted(coverage.signalRows() - coverage.rowsWithVolumeContext()));
        }
        if (coverage.signalRows() > 0 && coverage.stochKEqualsStochDRows() == coverage.signalRows()) {
            caveats.add("stoch_k 와 stoch_d 가 모든 행에서 동일하다 — 현재 설정(stoch-d = stoch-slow)에서 "
                    + "두 값이 같은 식으로 계산되므로 stoch_d 는 정보량이 없다.");
        }
        if (coverage.gapCount() > 0) {
            caveats.add("시계열에 %d개 구간(최대 %d분)의 공백이 있다 — 봇 정지·일시정지 또는 신호 생성 실패."
                    .formatted(coverage.gapCount(), coverage.longestGapMinutes()));
        }
        return caveats;
    }

    // ---------------- 공통 계산 ----------------

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    private static BigDecimal mean(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        return sum(values).divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentOf(long part, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal meanHoldMinutes(List<Position> positions) {
        List<BigDecimal> holds = positions.stream()
                .filter(p -> p.getOpenedAt() != null && p.getClosedAt() != null)
                .map(p -> BigDecimal.valueOf(Duration.between(p.getOpenedAt(), p.getClosedAt()).toMinutes()))
                .toList();
        return mean(holds);
    }

    private static AnalyticsReport withComputeMillis(AnalyticsReport r, long millis) {
        return new AnalyticsReport(r.market(), r.window(), r.coverage(), r.forwardReturnByScore(),
                r.componentEdge(), r.thresholdScenarios(), r.outcomeByCloseReason(), r.entryContext(),
                r.cost(), r.caveats(), millis);
    }

    /** 오늘 기준 기본 구간. 컨트롤러가 파라미터 없이 부를 때 쓴다. */
    public static LocalDate defaultFrom() {
        return LocalDate.now().minusDays(DEFAULT_WINDOW_DAYS);
    }
}
