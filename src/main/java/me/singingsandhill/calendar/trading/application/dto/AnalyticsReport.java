package me.singingsandhill.calendar.trading.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 신호 품질 분석 리포트 (ADR trading/observability/0001).
 *
 * <p>이 리포트가 답하려는 질문은 감사 문서가 "백테스트 없이는 판단 불가" 로 남겨둔 것들이다:
 * ±40 임계가 맞는가, 거래량 다이버전스 ±20 가중이 과한가, 재보정한 손절 −1.5% / TP +3% 가
 * 실제로 작동하는가.
 *
 * <p><b>읽는 사람이 반드시 알아야 할 통계적 함정 세 가지</b>가 자료구조에 박혀 있다.
 * <ol>
 *   <li><b>{@code n} 이 아니라 {@code effectiveN} 을 본다.</b> 매 분 관측치가 생기므로 +60분
 *       전방수익은 인접 행끼리 59분이 겹친다. n=10,000 이어도 독립 관측은 약 170이다.</li>
 *   <li><b>gross 가 아니라 net 을 본다.</b> 왕복 taker 수수료만 0.5% 다. 평균 +0.3% 구간은
 *       gross 로 양수여도 실제로는 손실이다.</li>
 *   <li><b>커버리지를 먼저 본다.</b> 봇 정지·일시정지 구간과 신호 생성 실패는 시계열에 구멍을
 *       남긴다. 표본이 얇으면 나머지 표는 전부 오독된다.</li>
 * </ol>
 */
public record AnalyticsReport(
        String market,
        Window window,
        Coverage coverage,
        List<ScoreBucketRow> forwardReturnByScore,
        List<ComponentEdgeRow> componentEdge,
        List<ThresholdScenarioRow> thresholdScenarios,
        List<CloseReasonRow> outcomeByCloseReason,
        EntryContext entryContext,
        CostReality cost,
        List<String> caveats,
        long computeMillis
) {

    public boolean isEmpty() {
        return coverage.signalRows() == 0;
    }

    public record Window(LocalDateTime from, LocalDateTime to, int days, List<Integer> horizonsMinutes) {}

    /**
     * 데이터 충분성. 리포트 최상단에 렌더된다 — 이걸 안 보고 아래 표를 읽으면 안 된다.
     *
     * @param signalRows            구간 내 신호 행 수
     * @param expectedMinutes       구간의 분 수 (봇이 쉬지 않고 돌았다면 나왔을 행 수)
     * @param gapCount              연속 2분 이상 비어 있는 구간의 개수
     * @param resolvedForwardCounts 지평(분) → 전방 가격을 실제로 찾은 행 수
     * @param rowsWithVolumeContext volumeMa/currentVolume 이 있는 행 수 — 임계 반사실의 판정 가능 범위
     * @param stochKEqualsStochDRows stoch_k == stoch_d 인 행 수. 설정상 두 값이 같은 식으로 계산되는지를
     *                               주장하지 않고 데이터로 확인한다 — 설정이 바뀌면 이 수치가 알아서 따라간다.
     */
    public record Coverage(
            long signalRows,
            long expectedMinutes,
            BigDecimal coveragePct,
            LocalDateTime firstSignalAt,
            LocalDateTime lastSignalAt,
            long distinctDays,
            int gapCount,
            long longestGapMinutes,
            Map<Integer, Long> resolvedForwardCounts,
            long buySignals,
            long sellSignals,
            long holdSignals,
            long rowsWithVolumeContext,
            long rowsWithAtr,
            long stochKEqualsStochDRows,
            long closedPositions,
            boolean signalSectionsSufficient,
            boolean outcomeSectionsSufficient,
            List<String> thinReasons
    ) {
        public boolean tooThinToConclude() {
            return !signalSectionsSufficient || !outcomeSectionsSufficient;
        }
    }

    /**
     * 한 부분집합의 전방수익 통계.
     *
     * @param effectiveN            {@code resolved / horizonMinutes} — 겹치는 창을 보정한 독립 표본 근사
     * @param netOfRoundTripFeePct  {@code meanPct} 에서 왕복 taker 수수료를 뺀 값
     */
    public record HorizonStat(
            int horizonMinutes,
            long resolved,
            long effectiveN,
            BigDecimal meanPct,
            BigDecimal medianPct,
            BigDecimal winRatePct,
            BigDecimal netOfRoundTripFeePct
    ) {
        /** 독립 표본이 30 미만이면 평균을 신뢰하지 않는다. 페이지는 이 값이 false 인 칸을 흐리게 렌더한다. */
        public boolean reliable() {
            return effectiveN >= 30;
        }
    }

    public record ScoreBucketRow(
            String bucketLabel,
            Integer lowerInclusive,
            Integer upperExclusive,
            long n,
            long episodes,
            Map<Integer, HorizonStat> byHorizon,
            boolean containsCurrentBuyThreshold,
            boolean containsCurrentSellThreshold
    ) {}

    /**
     * @param meanResidualScore 이 부분집합의 {@code totalScore − 해당 구성요소 점수} 평균.
     *                          구성요소별 조건부 평균은 교란돼 있다 — "거래량 다이버전스가 +20 일 때"
     *                          부분집합에는 같이 켜진 다른 요소가 전부 섞여 있다. 이 값이 baseline 보다
     *                          크면, 그 구성요소가 아니라 "원래 점수가 높은 구간" 을 보고 있는 것이다.
     */
    public record ComponentStateStat(
            String stateLabel,
            long n,
            long episodes,
            BigDecimal meanResidualScore,
            Map<Integer, HorizonStat> byHorizon
    ) {}

    public record ComponentEdgeRow(
            String component,
            String displayName,
            int maxAbsWeight,
            ComponentStateStat positive,
            ComponentStateStat zero,
            ComponentStateStat negative,
            ComponentStateStat baseline
    ) {}

    /**
     * 임계 반사실.
     *
     * <p>MA60 하회 시 확인 게이트에는 거래량 스파이크 분기가 있는데, 그 입력(volumeMa/currentVolume)은
     * ADR trading/observability/0002 이전 행에 없다. 그래서 단일 숫자가 아니라 <b>범위</b>로 낸다 —
     * {@code passesFullGateLow} 는 스파이크를 없다고, {@code passesFullGateHigh} 는 있다고 가정한 값이다.
     * 두 값이 같아지고 {@code indeterminate == 0} 이 되면 구간 전체가 새 스키마로 덮인 것이다.
     */
    public record ThresholdScenarioRow(
            int candidateBuyThreshold,
            long passesScoreOnly,
            long passesFullGateLow,
            long passesFullGateHigh,
            long indeterminate,
            long episodesFullGateLow,
            long blockedByRsi,
            long blockedByStochK,
            long blockedByAgreement,
            long blockedByMa60Gate,
            Map<Integer, HorizonStat> byHorizonFullGateLow,
            boolean isCurrentThreshold
    ) {}

    public record CloseReasonRow(
            String closeReason,
            long n,
            BigDecimal meanPnlPct,
            BigDecimal medianPnlPct,
            BigDecimal totalRealizedPnl,
            BigDecimal totalFees,
            BigDecimal meanHoldMinutes,
            long wins,
            BigDecimal winRatePct
    ) {}

    public record EntryBucketRow(
            String bucketLabel,
            long n,
            long wins,
            BigDecimal winRatePct,
            BigDecimal meanPnlPct,
            BigDecimal medianPnlPct,
            BigDecimal totalRealizedPnl,
            BigDecimal meanHoldMinutes
    ) {}

    /**
     * 진입 맥락 → 결과.
     *
     * <p>매칭은 <b>인과 방향</b>을 지킨다: 신호가 매수를 유발하므로 신호는 반드시 체결보다 앞선다.
     * 대칭 최근접 매칭을 하면 T+31초에 열린 포지션이 T+60초 신호(거리 29초)에 붙어, 그 매매가
     * 일어난 <i>뒤에</i> 생성된 신호를 원인으로 기록하게 된다.
     *
     * @param matchedByFollowingSignal 앞선 신호가 없어 뒤 신호로 붙인 건수. 인과 버킷 집계에서는 제외된다.
     * @param unmatched                어느 쪽으로도 못 붙인 건수. 조용히 버리지 않고 센다.
     * @param scoreConfirmedMatches    {@code trades.signal_score == signals.total_score} 로 교차 확인된 건수
     */
    public record EntryContext(
            long closedPositions,
            long signalDrivenPositions,
            long excludedNonSignalPositions,
            long matchedBySignalId,
            long matchedByTimestamp,
            long matchedByFollowingSignal,
            long unmatched,
            long scoreConfirmedMatches,
            List<EntryBucketRow> byEntryScore,
            List<EntryBucketRow> byRsiBand,
            List<EntryBucketRow> byMa60Regime
    ) {}

    /**
     * 비용 현실.
     *
     * @param feesAsPctOfNotional 주 지표. 총 수수료 / 총 진입 명목금액. 항상 정의되며 손익분기 이동폭과
     *                            직접 비교된다.
     * @param feesAsPctOfGross    보조 지표. gross ≤ 0 이면 <b>null</b> — 적자 장부에서 이 비율은 부호가
     *                            뒤집히거나 0 근처에서 발산해 아무 의미가 없다.
     * @param breakEvenMovePct    왕복 taker 수수료(= 2 × takerFeeRate × 100). 이만큼은 움직여야 본전이다.
     */
    public record CostReality(
            long closedPositions,
            BigDecimal totalEntryNotional,
            BigDecimal grossRealizedPnl,
            BigDecimal totalFees,
            BigDecimal netRealizedPnl,
            BigDecimal feesAsPctOfNotional,
            BigDecimal feesAsPctOfGross,
            BigDecimal avgFeePerPosition,
            BigDecimal breakEvenMovePct
    ) {}
}
