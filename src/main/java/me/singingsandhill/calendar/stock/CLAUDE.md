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

임계는 모두 `application.yaml` 의 `stock.entry.*` 운영값 (괄호는 Java 기본값):

```
WATCHING      -> Price >= Open x (1 + high-threshold-percent: 1.5%)      -> HIGH_FORMED
HIGH_FORMED   -> Price <= High x (1 - pullback-min-percent:  1.0%)       -> PULLBACK
PULLBACK      -> Price >= PullbackLow x (1 + bounce-threshold-percent: 0.2%) -> ENTRY_READY
                 (+ 진입 검증 2/3 통과 — 체결강도·호가 불균형·풀백 지속 3~15분)
ENTRY_READY   -> Buy order executed                                      -> ENTERED
ENTERED       -> All exits completed                                     -> EXITED

PULLBACK      -> Price < High x (1 - pullback-max-percent: 5.0%)         -> FILTERED_OUT (too deep)
```

## Exit Rules (operating values from `application.yaml`)

| Type | Condition (yaml) | Java default | Action |
|------|----|----|----|
| Stop Loss | `max(PullbackLow × (1 - risk.pullback-stop-buffer-percent: 1.0%), Entry × (1 - risk.max-stop-loss-percent: 2.0%))` — 통상 진입가 대비 약 -1.2% | (same) | Sell 100% |
| TP1 | `entry.tp1-percent: 5.0` (Entry +5%) | +1.5% | Sell `tp1-ratio: 0.5` (진입수량의 50%, 잔여수량 캡) |
| TP2 | Price >= DayHigh | (same) | Sell `tp2-ratio: 0.6` (60% remaining) |
| TP3 | `entry.tp3-percent: 10.0` (**Entry** +10%) | +1.0% | Sell remaining |
| Trailing | `risk.trailing-stop-percent: 3.8` (-3.8% from high, 손익분기 하한) | -0.8% | Sell remaining |
| Time Exit | Time >= 11:20 KST (종목당 3회 재시도, 실패 시 메일 알림) | (same) | Sell 100% |

TP1·TP2·TP3 는 *독립 트리거* (선행 의존 제거). 강한 트리거 우선 발동.
**앵커는 모두 고정값** — 손절은 풀백저가(진입 근거), TP1·TP3 는 진입가, TP2 는 당일고가.
매 틱 갱신되는 당일고가를 TP3 앵커로 쓰면 도달 불가였다 ([ADR 0007](../../../../../../../docs/adr/stock/algorithm/0007-exit-structure-recalibration.md)).
트레일링은 부분익절(TP1·TP2·TP3 중 하나)이 발생하면 활성화되고, 활성화 시점에 스탑가가 즉시 설정된다.
레거시 `risk.stop-loss-percent` 는 손절 계산에 사용되지 않는다(키만 보존).

Time-decay take profit: minimum profit threshold decreases linearly from 0.5% (09:10) to
0.1% (15:15), making TP triggers easier to hit later in the session. `Clock` 빈으로 시간
의존 코드 결정성 테스트 가능.

## 운영 모드 / 동시성 / 관측성

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — 모든 주문 진입부 모드 가드. **기본값 PAPER**,
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
