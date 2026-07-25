# Stock Module

Gap & Pullback trading bot for Korean stocks via Korea Investment Securities API.

> 결정 근거 (UniverseBuilder, TP 비순차화, 동시성 3-레이어, PAPER/BACKTEST 모드 등)
> 는 [`docs/adr/stock/`](../../../../../../../docs/adr/stock/) 참고.

## Trading Flow

```
08:30  PreMarket    -> UniverseBuilder.refresh() (pinned ∪ KIS 거래량순위 top-N, 실패 시 fallback-codes)
09:20  Screening    -> rank=0 스냅샷이면 거래량순위 재시도(refreshIfDegraded, ADR-0006 — 08:30 엔 당일 거래량 없음)
                       -> Floor filter + composite score ranking (top N) + email alert (첨부는 09:20 시점 스냅샷)
09:20~ Trading loop -> Every 5s (polling-interval-seconds): risk check -> state update -> enter if ready
11:20  Final Exit   -> Force close all remaining positions (`exit.final-exit-time` 은 청산 cron 과 동일하게 11:20 유지)
```

휴일 차단: `stock.trading.holidays` (yyyy-MM-dd 리스트). 주말은 cron `MON-FRI` 으로 차단.

## State Machine

임계는 모두 `application.yaml` 의 `stock.entry.*` 운영값 (Java 기본값도 동일하게 정합 —
yaml 키 누락 시 구식 값 회귀 방지):

```
WATCHING      -> Price >= Open x (1 + high-threshold-percent: 1.5%)      -> HIGH_FORMED
HIGH_FORMED   -> Price <= High x (1 - pullback-min-percent:  1.5%)       -> PULLBACK
PULLBACK      -> Price >= PullbackLow x (1 + bounce-threshold-percent: 0.2%) -> ENTRY_READY
                 (+ 진입 검증 2/3 통과 — 체결강도·호가 불균형·풀백 지속 3~15분,
                    풀백 시작시각 미기록은 FAIL — 데이터 부족 ≠ 통과)
ENTRY_READY   -> Buy order executed                                      -> ENTERED
ENTERED       -> All exits completed                                     -> EXITED

PULLBACK      -> Price < High x (1 - pullback-max-percent: 5.0%)         -> FILTERED_OUT (too deep)
```

풀백 하한 1.5% 근거: 1.0~1.3% 얕은 풀백은 TP2 기대 이득이 비용 게이트(실효 0.43% +
시간감쇠 최소수익)를 못 넘어 구조적으로 익절 불가 ([ADR 0009](../../../../../../../docs/adr/stock/algorithm/0009-entry-floor-and-trailing-cost-alignment.md)).

## Exit Rules (operating values from `application.yaml`)

| Type | Condition (yaml = Java default) | Action |
|------|----|----|
| Stop Loss | `max(PullbackLow × (1 - risk.pullback-stop-buffer-percent: 1.0%), Entry × (1 - risk.max-stop-loss-percent: 2.0%))` — 통상 진입가 대비 약 -1.2% | Sell 100% |
| TP1 | `entry.tp1-percent: 5.0` (Entry +5%) | Sell `tp1-ratio: 0.5` (진입수량의 50%, 잔여수량 캡) |
| TP2 | Price >= DayHigh | Sell `tp2-ratio: 0.6` (잔여의 60%, 설정값 사용) |
| TP3 | `entry.tp3-percent: 10.0` (**Entry** +10%) | Sell remaining |
| Trailing | `risk.trailing-stop-percent: 2.0` (-2.0% from high, 손익분기 하한 — 3.8% 는 본전 스탑으로 퇴화해 축소, [ADR 0009](../../../../../../../docs/adr/stock/algorithm/0009-entry-floor-and-trailing-cost-alignment.md)) | Sell remaining |
| Time Exit | Time >= 11:20 KST (종목당 3회 재시도, 실패 시 메일 알림) | Sell 100% |

매도 원장(`StockTrade.fee`)에도 매도 비용(수수료+거래세)이 기록된다 — 포지션 손익과 동일 비용 모델.

TP1·TP2·TP3 는 *독립 트리거* (선행 의존 제거). 강한 트리거 우선 발동.
**앵커는 모두 고정값** — 손절은 풀백저가(진입 근거), TP1·TP3 는 진입가, TP2 는 당일고가.
매 틱 갱신되는 당일고가를 TP3 앵커로 쓰면 도달 불가였다 ([ADR 0007](../../../../../../../docs/adr/stock/algorithm/0007-exit-structure-recalibration.md)).
트레일링은 부분익절(TP1·TP2·TP3 중 하나)이 발생하면 활성화되고, 활성화 시점에 스탑가가 즉시 설정된다.
레거시 `risk.stop-loss-percent` 는 손절 계산에 사용되지 않는다(키만 보존).

Time-decay take profit: minimum profit threshold decreases linearly from 0.5% (09:10) to
0.1% (15:15), making TP triggers easier to hit later in the session. `Clock` 빈으로 시간
의존 코드 결정성 테스트 가능.

**TP 순이익 게이트 (TP1·TP2·TP3 공통):** 위 표의 가격 조건이 충족돼도 그것만으로 익절되지
않는다. `tryFireTp` 가 `수수료차감 손익률 − 슬리피지 ≥ 시간감쇠 최소수익` 을 확인하고
미달이면 발동을 **건너뛴다** — **TP2(당일고가 도달)도 예외가 아니다**
(`StockRiskService:128-170`). 즉 가격 트리거는 필요조건일 뿐이고, 실제 발동 여부는 비용
차감 후 순이익이 결정한다.

**비용 모델** (`StockProperties.Risk`) — 위 "실효 0.43%" 의 구성:
수수료 `commission-rate` 0.015% × 2(왕복) + 매도 거래세 `sell-tax-rate` 0.20%
= 왕복 수수료 0.23%, 여기에 시장가 슬리피지 `slippage-buffer` 0.2% 를 더한
`getEffectiveExitCostRate()` = **0.43%**. 이 값이 트레일링 손익분기 하한이자 위 순이익
게이트의 비용 기준이다. (`StockCostModel` 이라는 클래스는 없다 — 테스트명만 그렇고 로직은
`StockProperties.Risk` 에 있다.)

## 운영 모드 / 동시성 / 관측성

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — 모든 주문 진입부 모드 가드 (분기점은 주문 4개 메서드뿐 —
  시세·호가·잔고는 모드 무관 실 API, 따라서 PAPER 도 스크리닝·상태머신·리스크·손익 기록이 전부 동작).
  `BACKTEST` 는 현재 PAPER 와 동일(히스토리 fixture 미구현). **기본값 PAPER**,
  LIVE 는 `STOCK_BOT_MODE=LIVE` 로만 opt-in (ADR stock/modes/0002). 주문 POST 는 비멱등이라
  무재시도(`executePostNoRetry`, ADR stock/infrastructure/0005), 봇 제어 API 는
  `POST /api/stock/bot/**` ADMIN 전용 (ADR common/security/0005).
- **`Semaphore(8, fair)` (KisRestClient)** + **`StockCodeLocks` (per-symbol ReentrantLock)** + **`ThreadPoolTaskScheduler(pool=4)`** 동시성 3-레이어.
- **주문 신뢰성** ([ADR stock/infrastructure/0006](../../../../../../../docs/adr/stock/infrastructure/0006-order-pre-persistence-and-fill-backfill.md)) —
  매수는 주문 *전* `PENDING` 거래 선영속화 → 응답 성공 시 ODNO 부착 + 당일주문체결조회로
  **실체결가·수수료 backfill**(포지션 진입가도 실체결 기준). 응답 유실분은 트레이딩 루프
  시작부 `reconcileUnconfirmedOrders()` 가 원장과 대조해 고아 체결이면 포지션을 생성(무보호 제거),
  12틱 내 미발견이면 CANCELLED.
- **재시작 복구** ([ADR stock/modes/0003](../../../../../../../docs/adr/stock/modes/0003-protection-only-recovery-on-restart.md)) —
  기동 시 오픈 포지션이 있으면 `recoveryMode` 로 자동 재개: 리스크 루프·시간청산은 동작하되
  **신규 진입은 차단**. 관리자 `start()` 시 해제. `BotStatus.recoveryMode` 로 노출.
- **`TradeEvents` 로거** + **KST 자정 회전** + **`BotStatus.{recoveryMode, lastTradingTickAt, lastScreeningResult, apiCallsLast5min}`** 메트릭.
- **`StockBotConfigValidator`** — 기동 시(`ApplicationReadyEvent`) 유효 설정 1줄 요약 + 위험/부정합(LIVE 모드, 빈 유니버스, floor>entry, 메일 미설정) WARN. 진단 전용, 동작 불변.
- **스크리닝 침묵 실패 가드** — 유니버스가 있는데 `Selected: 0` 이면 최다 탈락 버킷을 WARN.
- **거래정지·VI 가드** ([ADR algorithm/0008](../../../../../../../docs/adr/stock/algorithm/0008-screening-selection-and-cost-model.md)) —
  시세 응답의 `iscd_stat_cls_code`/`temp_stop_yn`/`mrkt_warn_cls_code`/`sltr_yn` 로
  `KisQuoteResponse.isTradable()` 판정. 스크리닝 Floor 0번 + **진입 직전 재확인**.
  필드 부재는 거래 가능으로 간주(무회귀)하되 전 종목 부재 시 WARN.
- **일일 실적 리포트** ([ADR observability/0002](../../../../../../../docs/adr/stock/observability/0002-daily-performance-report.md)) —
  11:40 cron 으로 승률·실현손익·청산 사유·진입 거절 사유를 집계해 메일 + `DAILY_REPORT` 이벤트.
  PAPER 실측(LIVE 전환 전제조건)의 데이터원.
- **영속성/외부호출 효율** ([ADR stock/infrastructure/0004](../../../../../../../docs/adr/stock/infrastructure/0004-n-plus-one-and-batch-candle.md)) —
  N+1 조회 제거 + `CandleService` 배치 처리 + KIS 조회 재시도.

## Presentation (`/api/stock/**` · `/stock*`)

`POST /api/stock/bot/**` 만 ADMIN, 나머지 조회는 공개 대시보드용 permitAll
(ADR common/security/0005).

| 컨트롤러 | 경로 |
|---|---|
| `StockBotApiController` | `/api/stock/bot` — `GET status`(공개) + POST start·stop·pause·resume·emergency-close(ADMIN) |
| `StockPositionApiController` | `/api/stock/positions` — open·closed·pnl/summary |
| `StockMonitoringApiController` | `/api/stock/monitoring` — active·state/{state} |
| `StockEventApiController` | `/api/stock/events` — 진입/청산 + 풀백·고점형성 시그널을 시각순으로 병합한 이벤트 스트림 |
| `StockSignalApiController` | `/api/stock/signals/{stockCode}` |
| `StockTradeApiController` | `/api/stock/trades/{stockCode}` |
| `StockDashboardController` | `/stock`, `/stock/history`, `/stock/settings` (Thymeleaf) |

> `domain/candle` (`StockCandle`, `CandleInterval`, `StockCandleRepository`) 는 현재
> application·presentation 에서 **호출처가 없는 비활성 스캐폴딩**이다. 캔들 기반 로직을
> 새로 붙일 때 여기서 출발하되, 지금 동작에는 관여하지 않는다.
