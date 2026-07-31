# Trading Module

Crypto algorithmic trading bot for Bithumb. Uses MA/RSI/Stochastic with divergence detection.

> 결정 근거: [`docs/adr/trading/strategy/`](../../../../../../../docs/adr/trading/strategy/) —
> 8지표 컨센서스(±128, P2-7 모멘텀 가중 하향 후; 원 ±135), MA 수렴 억제, 적자 매매 방지 가드.

## Trading Flow

```
Bithumb API -> Candles -> Indicators -> Divergences -> Signals -> Trade Execution
                                                           |
                                      Risk Management <- Position Tracking
```

## Risk Levels

- Stop-loss: -1.5% (P1-1: TP +3% 와 1:2 R:R)
- Take-profit: +3% (P1-2)
- Trailing stop: activates at +1.5%, trails -0.8%, floor = 진입가+왕복수수료 (P1-2)
- Fee rate: 0.25% (taker = maker; 0.04% 쿠폰 보유 시 `taker-fee-rate: 0.0004`)
- Min profit threshold: 0.1% (순수 net 마진 — 왕복 수수료/슬리피지는 PnL 계산에서 이미 차감, P1-8 이중계상 제거)

## Anti-Whipsaw (휩소 방지)

- Signal cooldown: 30 minutes between trades (P2-9: 10→30, 1분봉 과회전 억제)
- Min holding: 30 minutes before sell allowed (P2-9: 15→30)
- MA convergence suppression: |MA5-MA20|/MA20 < 0.2% → MA cross score = 0
- Min agreeing indicators: 3 (out of 8 score components)

## Rebalancing

- Bullish (price > MA60): 70% coins / 30% KRW
- Bearish (price < MA60): 30% coins / 70% KRW
- Trigger: 10% deviation from target
- Cooldown: 8 hours between rebalances
- Min sell PnL: 0% (`min-sell-pnl-pct: 0.0`)
- **회계 정합 (P1-3):** 리밸런스 매수는 추적 `Position`(SL/TP 포함) 생성, 매도는 OPEN
  포지션을 FIFO 청산하되 포지션별 수수료차감 PnL ≥ 0% 인 것만 (적자 청산 방지).
  → 모든 코인이 리스크 루프 보호를 받고 추적 Position ↔ 실잔고 정합. [ADR risk/0002].
- Safety details: see `application/CLAUDE.md`

## ATR-based Dynamic Order Ratio

- High volatility (ATR >= 3%): 15% order
- Low volatility (ATR <= 1%): 35% order
- Mid: linear interpolation

## 운영 모드 & 안전장치 (P0)

> 근거: [`docs/adr/trading/modes/0001`](../../../../../../../docs/adr/trading/modes/0001-paper-mode-default-and-order-gate.md),
> [`docs/adr/trading/risk/0001`](../../../../../../../docs/adr/trading/risk/0001-circuit-breaker-daily-and-consecutive-loss.md).
> 전체 진단·로드맵: [`docs/audit/coin-trading-profit-audit-2026-05-30.md`](../../../../../../../docs/audit/coin-trading-profit-audit-2026-05-30.md).

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — **기본 PAPER**, 실주문은 서버 환경변수
  `TRADING_BOT_MODE=LIVE` 로만 opt-in ([ADR modes/0002](../../../../../../../docs/adr/trading/modes/0002-paper-default-mode.md)).
  LIVE 가 아니면 `BithumbApiClient` 의 시장가 주문이 실주문 대신 현재가 기반 인메모리
  체결(`simulateBuy`/`simulateSell`)을 반환. `TradingProperties` 의 Risk/Bot/Rebalancing
  Java 기본값은 yaml 운영값과 정합 (P1-9 — 키 누락 시 구식 고위험 파라미터 회귀 방지).
- **서킷브레이커 (`TradingCircuitBreaker`)** — 연속 손실 `maxConsecutiveLosses`(기본 3)
  또는 당일 실현손익 ≤ `maxDailyLossPct`(기본 -5%) 시 신규 BUY 차단(리스크 청산은 허용).
  `circuitBreakerEnabled` 로 on/off.
- **스케줄러 풀** — `spring.task.scheduling.pool.size=4` 로 트레이딩 루프와 캔들 동기화/
  요약 잡 병렬화(느린 루프가 다른 잡을 굶기지 않도록).
  ⚠️ `trading.bot.enabled=false` 는 `executeTradeLoop`·`syncCandles`·계좌 스냅샷·일일 요약만
  막는다. **`CandleScheduler.cleanupOldCandles()`(자정 캔들 정리)는 가드가 없어 항상 실행**된다.
- **트랜잭션 경계 (P0-3)** — `TradingBotService` 는 `@Transactional` 을 제거하고 주문
  HTTP/sleep 을 트랜잭션 밖에서 수행, `Trade`+`Position` 영속화만 `TransactionTemplate`
  로 원자적 저장. P0-3b 로 `RiskManagementService`/`RebalanceService` 청산·매수 경로에도
  동일 패턴 적용 → **모든 주문 경로에서 HTTP-in-tx 제거** [ADR infrastructure/0001].
- **주문 선영속화 + 틱 스윕 (§8-B)** — cid 부착 구성(`supportsClientOrderId()` = v2 또는
  v1+`clientOrderIdEnabled`)이면 `executeBuy`/`executeSell`(신호 경로)이 주문 전송 **전에**
  `Trade(SUBMITTED, client_order_id)` 선영속화(매도는 positionId 연결). 응답 null/`UNKNOWN`/
  예외 시 SUBMITTED 유지 → 스윕(`reconcileSubmittedOrders`, 루프 시작부 + `start()` 기동
  직후 1회 §8-G)이 grace(10초) 경과분을 cid 재조회로 정합화: 매수 체결 → DONE +
  **Position(SL/TP) 생성**, 매도 체결 → DONE + **연결 포지션 청산**(이미 닫혔으면 Trade 만),
  취소 → CANCEL, 만료(2분) 미발견 → FAILED. 미해결 SUBMITTED 존재 시 신규 매수 차단,
  같은 포지션 미해결 매도 존재 시 재매도 차단. 기본 구성(V1+OFF)은 선영속화 꺼짐 — 운영
  동작 불변 [ADR infrastructure/0002].
- **Bithumb v2 주문 API (Phase 1)** — `trading.bithumb.order-api-version` enum(`V1`|`V2`,
  기본 V1, fail-fast). v2 는 생성 `POST /v2/orders`·취소 `DELETE /v2/order` 만이고 조회는
  영구 v1 — `BithumbV2OrderApi` 가 `GET /v1/order` 재조회(최대 3회 선형 백오프, 기본
  300ms)로 v1 형태(`BithumbOrderResponse`, trades 포함)로 정규화해 application 계층은
  버전 무관. 생성 에러/응답유실은 재전송 금지, cid 재조회로 복구(중복 cid 에러 포함 §8-E).
  취소 422(처리 중)는 1회 재시도. 재조회 전부 실패 시 `state=UNKNOWN` 부분 응답 →
  §8-B 스윕이 수습. cid 는 `t1-`/`t2-` 버전 프리픽스로 추적. 지정가는 미사용 경로라 v1
  유지 + cid 부착만 [ADR infrastructure/0003].
- **포지션 리스크 가드 (P2)** — `maxHoldMinutes`(360) 정체 포지션 손익분기 이상이면
  `TIME_EXIT` 청산 / `blockAveragingDown`(true) 손실 포지션 보유 중 추가 매수 차단 /
  `maxCoinExposurePct`(0.8) 코인 비중 상한 초과 시 신규 매수 스킵 [ADR risk/0003].
  현재가 조회 실패 시 가드 평가 불가 → 신규 매수(자동·수동 공통) 차단 — fail-open 아님
  ([ADR risk/0005](../../../../../../../docs/adr/trading/risk/0005-fail-safe-entry-guard-on-price-unavailable.md)).
- **수동매매 정합 + 엔진 핑퐁 방지** ([ADR risk/0004](../../../../../../../docs/adr/trading/risk/0004-manual-trade-position-consistency-and-engine-coordination.md)) —
  `manualBuy` 는 `Trade` 만 저장하고 Position 을 만들지 않아 그 코인이 SL/TP 무보호 +
  리스크 루프에 비가시였고, `manualSell` 은 OPEN Position 을 닫지 않아 추적 Position ↔
  실잔고가 드리프트(이미 판 코인을 유령 포지션으로 재매도 시도)했다 → 양쪽 모두 정합 처리.
  신호 매매가 `lastRebalanceTime` 을 갱신하지 않아 직후 틱에 리밸런스가 발화하던
  **엔진 핑퐁**(레그마다 taker 수수료)도 함께 차단. 위 P2-9 쿨다운 상향도 같은 ADR.
- **지표 계산 세부** — `excludeFormingCandle`(**기본 OFF**, `TradingProperties:142`)은 형성 중
  (index 0) 봉을 모든 지표·다이버전스 입력에서 제외할지 결정 → 켜면 룩어헤드/리페인트가
  사라지지만 신호 타이밍이 한 봉 늦는다 ([ADR strategy/0009](../../../../../../../docs/adr/trading/strategy/0009-exclude-forming-candle.md)).
  그 외 지표 정련: Wilder RSI ([0006](../../../../../../../docs/adr/trading/strategy/0006-wilder-rsi.md)),
  다이버전스 피벗 강화 ([0007](../../../../../../../docs/adr/trading/strategy/0007-divergence-pivot-strengthening.md)),
  Slow Stoch·RSI 추세 잡음 감소 ([0008](../../../../../../../docs/adr/trading/strategy/0008-indicator-noise-reduction.md)).

## Presentation (`/api/trading/**` · `/trading*`)

전부 `ROLE_ADMIN` (ADR common/security/0003) 이지만 표면이 넓다 — 봇을 정지시켜도 막히지
않는 주문 경로가 있으므로 주의.

| 컨트롤러 | 경로 | 비고 |
|---|---|---|
| `BotControlApiController` | `/api/trading/bot` — status·start·stop·pause·resume·**manual/buy**·**manual/sell**·emergency-close | 수동매매는 ADR risk/0004 정합 경로 |
| `TradingVerificationApiController` | `/api/trading/verify` — config·price·balance·**`POST /test-order`**·trades/recent·positions/recent·full | ⚠️ 아래 경고 |
| `ChartApiController` | `/api/trading` — candles·ticker·chart/trades | |
| `TradeApiController` | `/api/trading` — trades·profit/summary·today·profit/daily·positions | |
| `RebalanceApiController` | `/api/trading/rebalance` — status·execute | |
| `TradingEventApiController` | `/api/trading/events` | |
| `TradingDashboardController` | `/trading`, `/trades`, `/settings`, `/portfolio`, `/verify` (Thymeleaf) | |

⚠️ **`POST /api/trading/verify/test-order`** — API 검증용으로 시장가 매수를 1회 전송한다
(기본 5,500원, 최소 5,000원, `immediatelySell=true` 면 즉시 반대매매로 원상복구).
주문 자체는 `placeMarketBuyOrder` 를 타므로 **모드 가드는 적용**되지만(PAPER 면 가상체결),
**`bot.enabled` 와 서킷브레이커는 우회**한다 — 즉 봇을 정지·비활성화한 상태에서도 LIVE
모드면 실주문이 나간다. 차단 조건은 API 키 설정 여부와 잔고뿐
(`TradingVerificationApiController:197-266`).
