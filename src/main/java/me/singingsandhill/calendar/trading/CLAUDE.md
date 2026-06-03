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

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — 기본 **LIVE**(기존 운영 동작 유지). LIVE 가
  아니면 `BithumbApiClient` 의 시장가 주문이 실주문 대신 현재가 기반 인메모리 체결
  (`simulateOrder`)을 반환. 파라미터/로직 변경 검증은 `TRADING_BOT_MODE=PAPER` 권장.
- **서킷브레이커 (`TradingCircuitBreaker`)** — 연속 손실 `maxConsecutiveLosses`(기본 3)
  또는 당일 실현손익 ≤ `maxDailyLossPct`(기본 -5%) 시 신규 BUY 차단(리스크 청산은 허용).
  `circuitBreakerEnabled` 로 on/off.
- **스케줄러 풀** — `spring.task.scheduling.pool.size=4` 로 트레이딩 루프와 캔들 동기화/
  요약 잡 병렬화(느린 루프가 다른 잡을 굶기지 않도록).
- **트랜잭션 경계 (P0-3)** — `TradingBotService` 는 `@Transactional` 을 제거하고 주문
  HTTP/sleep 을 트랜잭션 밖에서 수행, `Trade`+`Position` 영속화만 `TransactionTemplate`
  로 원자적 저장. P0-3b 로 `RiskManagementService`/`RebalanceService` 청산·매수 경로에도
  동일 패턴 적용 → **모든 주문 경로에서 HTTP-in-tx 제거** [ADR infrastructure/0001].
- **포지션 리스크 가드 (P2)** — `maxHoldMinutes`(360) 정체 포지션 손익분기 이상이면
  `TIME_EXIT` 청산 / `blockAveragingDown`(true) 손실 포지션 보유 중 추가 매수 차단 /
  `maxCoinExposurePct`(0.8) 코인 비중 상한 초과 시 신규 매수 스킵 [ADR risk/0003].
