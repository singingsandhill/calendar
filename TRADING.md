# 트레이딩 봇 (코인 + 주식) — 통합 진입점

이 문서는 두 봇(코인/주식) 의 *현재 코드 사실* 만 짧게 요약한다. 알고리즘 설명이나
결정 근거는 다른 문서에 있다.

| 알고 싶은 것 | 가야 할 곳 |
|---|---|
| 코인 봇 8지표 컨센서스 / 리스크 / 리밸런싱 상세 | [`docs/trading/bot.md`](docs/trading/bot.md), [`src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md`](src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md) |
| 주식 봇 갭&풀백 / TP 비순차화 / 시간 감소 임계 | [`docs/stock/bot.md`](docs/stock/bot.md), [`src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md`](src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md) |
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
신호 품질 분석: <http://localhost:8081/trading/analytics> (`?days=7|14|30|60|90`, 온디맨드 계산 —
스케줄러·JSON 엔드포인트 없음. [ADR observability/0001](docs/adr/trading/observability/0001-signal-quality-analytics-page.md)).

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
| 08:30 | 프리마켓 — `UniverseBuilder.refresh()` (pinned ∪ KIS 거래량순위 top-30, 실패 시 fallback-codes) |
| 09:20 | 스크리닝 — rank=0 이면 거래량순위 재시도 → floor filter + 5요인 score + 신호 게이트 → 메일 |
| 09:20~11:20 | 5초 polling 트레이딩 루프 (시작부에 미확인 주문 스윕) |
| 11:20 | 모든 포지션 강제 청산 (종목당 3회 재시도, 실패 시 메일) |
| 11:40 | 일일 실적 요약 리포트 ([ADR](docs/adr/stock/observability/0002-daily-performance-report.md)) |

휴일: `stock.trading.holidays` (yyyy-MM-dd 리스트).

### 청산 규칙 (yaml 운영값 — [ADR-0007](docs/adr/stock/algorithm/0007-exit-structure-recalibration.md))

| Type | Condition | Action |
|---|---|---|
| Stop Loss | `max(PullbackLow × (1 - 1.0%), Entry × (1 - 2.0%))` — 통상 진입가 대비 약 -1.2% | Sell 100% |
| TP1 | Entry +5% (`entry.tp1-percent: 5.0`) | Sell 진입수량 50% (잔여 캡) |
| TP2 | DayHigh 도달 | Sell 60% remaining |
| TP3 | **Entry** +10% (`entry.tp3-percent: 10.0`) | Sell remaining |
| Trailing | TrailingHigh 대비 -3.8% (`risk.trailing-stop-percent: 3.8`), 손익분기 하한 | Sell remaining |
| Time Exit | 11:20 KST | Sell 100% |

TP1·TP2·TP3 는 *독립 트리거* — 선행 의존 제거 ([ADR](docs/adr/stock/algorithm/0004-tp-independent-triggers.md)).
앵커는 모두 고정값(손절=풀백저가, TP1·TP3=진입가, TP2=당일고가) — 갱신되는 당일고가를 TP3 앵커로
쓰면 도달 불가였다. 트레일링은 부분익절(TP1·TP2·TP3 중 하나) 발생 시 활성화 + 즉시 스탑가 설정.
익절 게이트는 수수료·세금(매도 0.20%, 2026 시행) **+ 슬리피지 0.2%** 를 차감한 순익 기준.
시간 감소 임계: 09:10 의 0.5% → 15:15 의 0.1% 로 선형 감소(실거래창이 11:20 이라 유효 하한 ~0.36%).

### 운영 모드 / 동시성

- `Bot.Mode {LIVE, PAPER, BACKTEST}` — **기본 PAPER**, LIVE 는 `STOCK_BOT_MODE=LIVE` 로만 opt-in
  ([ADR modes/0001](docs/adr/stock/modes/0001-paper-backtest-mode-and-clock-bean.md),
  [modes/0002](docs/adr/stock/modes/0002-paper-default-mode.md)).
  모드 분기는 주문 4개 메서드뿐 — **시세·호가·잔고 조회는 모드 무관 실 API** 라 PAPER 에서도
  스크리닝·상태머신·리스크 루프·손익 기록이 전부 동작한다(주문만 가상).
  `BACKTEST` 는 현재 PAPER 와 동일(히스토리 fixture 미구현).
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
