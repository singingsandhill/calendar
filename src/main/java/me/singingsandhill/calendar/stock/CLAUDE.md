# Stock Module

Gap & Pullback trading bot for Korean stocks via Korea Investment Securities API.

> 결정 근거 (UniverseBuilder, TP 비순차화, 동시성 3-레이어, PAPER/BACKTEST 모드 등)
> 는 [`docs/adr/stock/`](../../../../../../../docs/adr/stock/) 참고.

## Trading Flow

```
08:30  PreMarket    -> UniverseBuilder.refresh() (pinned ∪ KIS 거래량순위 top-N, 실패 시 fallback-codes)
09:20  Screening    -> Floor filter + composite score ranking (top N) + email alert
09:20~ Trading loop -> Every 5s (polling-interval-seconds): risk check -> state update -> enter if ready
11:20  Final Exit   -> Force close all remaining positions
```

휴일 차단: `stock.trading.holidays` (yyyy-MM-dd 리스트). 주말은 cron `MON-FRI` 으로 차단.

## State Machine

```
WATCHING      -> Price >= Open x 1.015         -> HIGH_FORMED
HIGH_FORMED   -> Price <= High x 0.985         -> PULLBACK
PULLBACK      -> Price >= PullbackLow x 1.003  -> ENTRY_READY
ENTRY_READY   -> Buy order executed             -> ENTERED
ENTERED       -> All exits completed            -> EXITED

PULLBACK      -> Price < High x 0.970          -> FILTERED_OUT (too deep)
```

## Exit Rules (operating values from `application.yaml`)

| Type | Condition (yaml) | Java default | Action |
|------|----|----|----|
| Stop Loss | `risk.stop-loss-percent: 5.0` (-5%) | -1.5% | Sell 100% |
| TP1 | `entry.tp1-percent: 5.0` (+5%) | +1.5% | Sell `tp1-ratio: 0.5` (50%) |
| TP2 | Price >= DayHigh | (same) | Sell `tp2-ratio: 0.6` (60% remaining) |
| TP3 | `entry.tp3-percent: 10.0` (+10% above day high) | +1.0% | Sell remaining |
| Trailing | `risk.trailing-stop-percent: 3.8` (-3.8% from high) | -0.8% | Sell remaining |
| Time Exit | Time >= 11:20 KST | (same) | Sell 100% |

TP1·TP2·TP3 는 *독립 트리거* (선행 의존 제거). 강한 트리거 우선 발동.

Time-decay take profit: minimum profit threshold decreases linearly from 0.5% (09:10) to
0.1% (15:15), making TP triggers easier to hit later in the session. `Clock` 빈으로 시간
의존 코드 결정성 테스트 가능.

## 운영 모드 / 동시성 / 관측성

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — 모든 주문 진입부 모드 가드.
- **`Semaphore(8, fair)` (KisRestClient)** + **`StockCodeLocks` (per-symbol ReentrantLock)** + **`ThreadPoolTaskScheduler(pool=4)`** 동시성 3-레이어.
- **`TradeEvents` 로거** + **KST 자정 회전** + **`BotStatus.{lastTradingTickAt, lastScreeningResult, apiCallsLast5min}`** 메트릭.
- **`StockBotConfigValidator`** — 기동 시(`ApplicationReadyEvent`) 유효 설정 1줄 요약 + 위험/부정합(LIVE 모드, 빈 유니버스, floor>entry, 메일 미설정) WARN. 진단 전용, 동작 불변.
- **스크리닝 침묵 실패 가드** — 유니버스가 있는데 `Selected: 0` 이면 최다 탈락 버킷을 WARN.
