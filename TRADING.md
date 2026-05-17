# 트레이딩 봇 (코인 + 주식) — 통합 진입점

이 문서는 두 봇(코인/주식) 의 *현재 코드 사실* 만 짧게 요약한다. 알고리즘 설명이나
결정 근거는 다른 문서에 있다.

| 알고 싶은 것 | 가야 할 곳 |
|---|---|
| 코인 봇 8지표 컨센서스 / 리스크 / 리밸런싱 상세 | [`docs/trading-bot.md`](docs/trading-bot.md), [`src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md`](src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md) |
| 주식 봇 갭&풀백 / TP 비순차화 / 시간 감소 임계 | [`docs/stock-bot.md`](docs/stock-bot.md), [`src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md`](src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md) |
| 결정 _왜_ (MA 수렴 억제, 적자 매매 가드, UniverseBuilder, KIS Semaphore 등) | [`docs/adr/trading/`](docs/adr/trading/), [`docs/adr/stock/`](docs/adr/stock/) |
| 빌드·실행 / 환경변수 / 포트 | [`CLAUDE.md`](CLAUDE.md) |

---

## 코인 봇 (Bithumb)

### 점수 시스템 (`SignalService`)

총 점수 범위 **±135** = 다음 8개 구성요소 합산:

| 구성요소 | 매수 점수 | 매도 점수 |
|---|---|---|
| MA Cross 이벤트 또는 State (둘 중 하나만) | +25 (cross) / +10 (state) | -25 / -10 |
| MA Trend (가격 vs MA60) | +15 | -15 |
| RSI Divergence | +20 | -20 |
| RSI Level (<35 / >65) | +15 | -15 |
| Stoch Divergence | +15 | -15 |
| Stoch Level (<25 / >75) | +15 | -15 |
| Volume Divergence | +20 | -20 |
| RSI Trend | +10 | -10 |

매수 임계: score ≥ +40 AND RSI < 70 AND StochK < 85 AND 동의 지표 ≥ 3.
매도 임계: score ≤ -40 AND RSI > 30 AND StochK > 15 AND 동의 지표 ≥ 3.

MA 수렴 시 (|MA5−MA20|/MA20 < 0.2%) MA 크로스 점수는 0 으로 억제 ([ADR](docs/adr/trading/strategy/0002-ma-convergence-suppression.md)).

### 리스크 (`application.yaml` 운영값)

| 항목 | 값 | 비고 |
|---|---|---|
| stop-loss | -3% | yaml `trading.risk.stop-loss: -0.03` |
| take-profit | +15% | yaml `trading.risk.take-profit: 0.15` |
| trailing-activation | +10% 도달 시 활성화 | |
| trailing-stop | High Water Mark 대비 -3% | |
| taker fee | 0.25% | |
| min profit threshold | 0.6% (왕복 수수료 + 마진) | |
| 적자 매매 가드 | 평가손익 ≥ -2% 일 때만 강신호 매도, 리밸런싱 매도는 평균 P/L ≥ 0%, 트레일링 ≥ 손익분기점 | [ADR](docs/adr/trading/strategy/0003-loss-prevention-guards.md) |

### 리밸런싱

- 상승장 (price > MA60): 코인 70% / KRW 30%
- 하락장 (price < MA60): 코인 30% / KRW 70%
- 발동: 목표 대비 10% 이상 편차
- 쿨다운: 8시간

### REST API

| Endpoint | 동작 |
|---|---|
| `GET /api/trading/bot/status` | 상태 조회 |
| `POST /api/trading/bot/{start,stop,pause,resume}` | 제어 |
| `POST /api/trading/bot/manual/{buy,sell}` | 수동 주문 |
| `POST /api/trading/bot/emergency-close` | 긴급 청산 |
| `GET /api/trading/{candles,ticker,trades,positions}` | 데이터 조회 |
| `GET /api/trading/profit/{summary,daily}` | 손익 |

대시보드: <http://localhost:8081/trading>.

### 환경변수

```
BITHUMB_ACCESS_KEY=...
BITHUMB_SECRET_KEY=...
TRADING_BOT_ENABLED=false   # true 로 봇 자동 시작 활성화
```

---

## 주식 봇 (한국투자증권 KIS)

### 전략: Gap & Pullback

평일 09:20 갭 스크리닝 → 9:20~11:20 사이 갭 종목의 *눌림목 후 반등* 진입 → TP1/TP2/TP3
독립 트리거로 부분 익절.

### 스케줄 (KST, 평일만)

| 시각 | 잡 |
|---|---|
| 08:30 | 프리마켓 — `UniverseBuilder.build()` (pinned ∪ fallback-codes) |
| 09:20 | 스크리닝 — floor filter + 5요인 가중 score → top N + 메일 발송 |
| 09:20~11:20 | 5초 polling 트레이딩 루프 |
| 11:20 | 모든 포지션 강제 청산 |

휴일: `stock.trading.holidays` (yyyy-MM-dd 리스트).

### 청산 규칙 (yaml 운영값)

| Type | Condition | Action |
|---|---|---|
| Stop Loss | -5% (`risk.stop-loss-percent: 5.0`) | Sell 100% |
| TP1 | +5% (`entry.tp1-percent: 5.0`) | Sell 50% |
| TP2 | DayHigh 도달 | Sell 60% remaining |
| TP3 | DayHigh + 10% (`entry.tp3-percent: 10.0`) | Sell remaining |
| Trailing | TrailingHigh 대비 -3.8% (`risk.trailing-stop-percent: 3.8`) | Sell remaining |
| Time Exit | 11:20 KST | Sell 100% |

TP1·TP2·TP3 는 *독립 트리거* — 선행 의존 제거 ([ADR](docs/adr/stock/algorithm/0004-tp-independent-triggers.md)).
시간 감소 임계: 09:10 의 0.5% → 15:15 의 0.1% 로 선형 감소.

### 운영 모드 / 동시성

- `Bot.Mode {LIVE, PAPER, BACKTEST}` ([ADR](docs/adr/stock/modes/0001-paper-backtest-mode-and-clock-bean.md))
- `KisRestClient` `Semaphore(8, fair)` + `StockCodeLocks` (per-symbol) +
  `ThreadPoolTaskScheduler(pool=4)` 동시성 3-레이어 ([ADR-0001](docs/adr/stock/infrastructure/0001-kis-rate-limit-semaphore.md), [-0002](docs/adr/stock/infrastructure/0002-per-symbol-reentrant-lock.md), [-0003](docs/adr/stock/infrastructure/0003-thread-pool-task-scheduler.md))

### REST API

| Endpoint | 동작 |
|---|---|
| `GET /api/stock/bot/status` | 상태 (`lastTradingTickAt`, `lastScreeningResult`, `apiCallsLast5min` 포함) |
| `POST /api/stock/bot/{start,stop,pause,resume,emergency-close}` | 제어 |

대시보드: <http://localhost:8081/stock>.

### 환경변수

```
KIS_APP_KEY=...
KIS_APP_SECRET=...
KIS_ACCOUNT_NUMBER=...
STOCK_BOT_ENABLED=false
```
