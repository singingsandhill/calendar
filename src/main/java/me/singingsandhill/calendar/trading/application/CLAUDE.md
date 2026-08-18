# Trading Application Layer

> 결정 근거: [`docs/adr/trading/strategy/`](../../../../../../../../docs/adr/trading/strategy/).

## Signal Logic

- BUY: score >= 40 AND RSI < 70 AND StochK < 85 AND 3+ indicators agree
- SELL: score <= -40 AND RSI > 30 AND StochK > 15 AND 3+ indicators agree
- Otherwise: HOLD
- MA convergence (|MA5-MA20|/MA20 < 0.2%): MA cross score suppressed to 0

**MA60 하락추세 추가 확인 (Issue #9):** 위 BUY 조건을 다 통과해도 **현재가 < MA60** 이면
아래 3개 중 **하나도** 없으면 `HOLD` 로 강제 전환된다 (`SignalService:295-311`).

1. bullish divergence 존재
2. RSI < 30 (강한 과매도)
3. 거래량 > `volumeMa` × 1.5 (거래량 스파이크)

즉 하락추세에서는 "점수만 높은" 매수가 차단되고, 반전 근거가 하나라도 있을 때만 진입한다.

## Score Components (weights, sum = ±128)

MA Cross: ±25 (이벤트) 또는 MA State ±5 (둘 중 하나만) | MA Trend: ±8 | RSI Divergence: ±20 | RSI Level: ±15 | Stoch Divergence: ±15 | Stoch Level: ±15 | Volume Divergence: ±20 | RSI Trend: ±10

총 합산 범위: **±128** = 25+8+20+15+15+15+20+10 ([ADR-0001](../../../../../../../../docs/adr/trading/strategy/0001-multi-indicator-consensus.md), 모멘텀 가중 하향 [ADR-0010](../../../../../../../../docs/adr/trading/strategy/0010-momentum-weight-reduction.md): MA Trend ±15→±8, MA State ±10→±5).
횡보장에서 MA 수렴 시(|MA5-MA20|/MA20 < 0.2%) 크로스 점수는 0 으로 억제됨
([ADR](../../../../../../../../docs/adr/trading/strategy/0002-ma-convergence-suppression.md)).

## Divergence Types

- Bullish: Price Lower Low + Indicator Higher Low
- Bearish: Price Higher High + Indicator Lower High
- Detects on: RSI, Stochastic, Volume

## 지표 계산 세부 (IndicatorService)

- **RSI 추세** 는 인접봉 비교가 아니라 `rsiTrendLookback`(3) 봉 전 RSI 와 비교하며,
  `minRsiTrendDelta`(2.0 포인트) 이상 움직여야 추세로 인정 — 1분봉 잡음 억제
  (P2-6, `IndicatorService:197-227`).
- **`excludeFormingCandle`(기본 OFF)** — 형성 중(index 0)인 봉을 지표·다이버전스 입력에서
  제외할지. OFF 이므로 현재 계산에는 형성봉이 포함된다 (ADR strategy/0009).
- ATR/ATR% 도 여기서 계산 → 동적 주문 비중에 사용 (모듈 `CLAUDE.md` 참고).

## Rebalancing Safety

- Cooldown: 8h between rebalances
- Min order: 5,000 KRW (skip smaller)
- Slippage: 0.5% buffer on market orders
- MA60 data insufficient: `skip-when-data-insufficient`(기본 `true`) 면 스킵. `false` 면
  `default-ratio`(0.50) 로 폴백. `price == MA60` 인 NEUTRAL 구간도 `default-ratio` 사용
  (`RebalanceService:226-244`).
- **회계 정합 (P1-3):** 리밸런스 매수 → 추적 `Position`(SL/TP) 생성; 매도 → OPEN 포지션
  FIFO 청산, 포지션별 수수료차감 PnL ≥ `min-sell-pnl-pct`(0%) 인 것만, 목표량 도달 시 중단.
  청산은 `RiskManagementService.closePosition(…, REBALANCE)` 재사용. 추적 Position ↔ 실잔고
  정합 + 전 코인 SL/TP 보호 — [ADR risk/0002](../../../../../../../../docs/adr/trading/risk/0002-rebalance-position-accounting.md). ADR-0003 의 적자 청산 방지 의도를 포지션별로 정련.
- 강한 신호 매도는 평가손익 ≥ -2% 일 때만 (작은 손실 시 강한 신호로 매도 안 함).
- 트레일링 스탑은 진입가 + 왕복 수수료(0.5%) *아래로* 내려가지 않게 floor.

## 지원 서비스

- **`TradingEventService`** — 모든 거래/리스크/리밸런스 경로가 `record(...)` 로 호출하는
  이벤트 원장. `@Transactional(REQUIRES_NEW)` 로 분리되어 **호출자 트랜잭션이 롤백돼도
  이벤트 기록은 남는다**(사후 추적 가능성이 목적, `TradingEventService:34`).
- **`ProfitService`** — 계좌 스냅샷·일일 요약·today/profit-summary 집계 생성.
  `DailySummaryScheduler` 두 잡의 실제 구현체.
- **`TradingAnalyticsService`** — 신호 품질 분석 7개 섹션. `/trading/analytics` 가 요청 시점에
  호출한다 ([ADR observability/0001](../../../../../../../../docs/adr/trading/observability/0001-signal-quality-analytics-page.md)).
  주의할 점 네 가지:
  1. **전방수익은 캔들이 아니라 `trading_signals.current_price` 로 계산한다** — 신호가 매 분
     기록되므로 그 테이블 자체가 1분 가격 시계열이고, 점수 입력과 가격 원천이 같아 기준가
     불일치가 없다. 목표 시각 ±90초 안의 최근접 행만 쓰고 없으면 결측 처리 —
     인덱스 산술(`i + 15`)은 시계열 구멍 때문에 조용히 틀린다.
  2. **`n` 이 아니라 `effectiveN`(= resolved / 지평분)을 본다.** 매 분 관측치라 +60분 창은
     인접 행끼리 59분이 겹친다. n=10,000 이어도 독립 관측은 약 170이다.
  3. **매수 게이트는 `SignalService.determineSignalType` 의 복제본**이다(`evaluateBuyGate`).
     추출하면 분석을 위해 실주문 경로를 고쳐야 하므로 일부러 복제했고,
     `TradingAnalyticsGateParityTest` 가 드리프트를 빌드 실패로 만든다. **이 테스트가 깨지면
     리팩터링 중이라도 넘어가면 안 된다** — 임계 반사실이 조용히 거짓이 된다는 뜻이다.
  4. 신호 읽기만 엔티티가 아니라 `SignalSample` 투영을 쓴다 — 90일 ≈ 130,000행을 엔티티로
     읽으면 1차 캐시에 남아 요청 내내 힙을 붙잡는다(Jetson Nano 고려).

## Circuit Breaker (P0-2)

`TradingCircuitBreaker` — 연속 손실 `maxConsecutiveLosses`(기본 3) 또는 당일 실현손익
≤ `maxDailyLossPct`(기본 -5%) 시 신규 BUY 차단(리스크 청산은 허용). 청산 결과는
`executeSell`/`closePosition` 에서 집계, 진입 차단은 `executeBuy` 진입부에서 판정.
근거: [ADR](../../../../../../../../docs/adr/trading/risk/0001-circuit-breaker-daily-and-consecutive-loss.md).

재시작 시 `consecutiveLosses`(인메모리)는 0 으로 리셋된다. 보호 전용 자동재개
([ADR modes/0003](../../../../../../../../docs/adr/trading/modes/0003-protection-only-recovery-on-restart.md))의
`executeTradeLoop` recovery 게이트는 신호 기록(3) 직후·매매 단계(4~6) 앞 — 리스크 체크
우선 불변식(`riskCheckRunsBeforeSignalGeneration`)과 청산 틱 신호 기록(ADR observability/0003)을
보존하는 유일한 위치다. 게이트를 이 앞으로 옮기면 안 된다.
