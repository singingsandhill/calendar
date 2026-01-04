# Trading Infrastructure Layer

External integrations: Bithumb API, JPA persistence, schedulers, and configuration.

## Bithumb API Integration

### BithumbApiClient
Unified client combining public and private APIs.

### BithumbPublicApi
Market data (no authentication required).

| Method | Description |
|--------|-------------|
| `getCandles(market, count)` | Fetch OHLCV candles |
| `getOrderbook(market)` | Get order book |
| `getTicker(market)` | Get current price |
| `getRecentTrades(market)` | Recent trade history |

### BithumbPrivateApi
Account operations (requires JWT authentication).

| Method | Description |
|--------|-------------|
| `getBalance()` | Get account balances |
| `getOrders()` | Get pending orders |
| `placeOrder(side, price, volume)` | Place order |
| `cancelOrder(orderId)` | Cancel order |

### BithumbJwtGenerator
JWT token generation for private API authentication.

## Configuration

### TradingProperties
```yaml
trading:
  bithumb:
    accessKey: ${TRADING_API_KEY}
    secretKey: ${TRADING_SECRET_KEY}
  bot:
    enabled: true
    market: KRW-ADA
    maxPositions: 2      # 최대 동시 포지션 수
    orderRatio: 0.25     # 주문 금액 비율 (25%)
  indicators:
    maShort: 5
    maMid: 20
    maLong: 60
    rsiPeriod: 14
    stochK: 14
    stochD: 3
    volumeMa: 20
  thresholds:
    signalBuy: 40        # 매수 신호 점수
    signalSell: -40      # 매도 신호 점수
    rsiOversold: 35      # RSI 과매도
    rsiOverbought: 65    # RSI 과매수
    stochOversold: 25    # Stochastic 과매도
    stochOverbought: 75  # Stochastic 과매수
    buyRsiMax: 70        # 매수 시 RSI 상한
    buyStochKMax: 85     # 매수 시 StochK 상한
    sellRsiMin: 30       # 매도 시 RSI 하한
    sellStochKMin: 15    # 매도 시 StochK 하한
  risk:
    stopLoss: -0.08      # -8%
    takeProfit: 0.15     # +15%
    trailingStop: 0.03   # -3% 추적
    trailingActivation: 0.10  # +10% 도달 시 활성화
  rebalancing:
    enabled: true
    defaultRatio: 0.50
    bullRatio: 0.70
    bearRatio: 0.30
    deviationTrigger: 0.10
    cooldownMinutes: 240      # 4시간 쿨다운
    minOrderAmount: 5000      # 최소 주문 금액 (KRW)
    slippageBuffer: 0.005     # 0.5% 슬리피지 버퍼
    skipWhenDataInsufficient: true  # MA60 부족 시 스킵
```

**Configuration Validation**:
- `@PostConstruct` 검증으로 시작 시 설정값 유효성 체크
- 비정상 값 (bullRatio > 1.0 등) 발견 시 예외 발생
- deviationTrigger ≤ 0 경고 로그 출력

## JPA Entities

| Entity | Table | Description |
|--------|-------|-------------|
| CandleJpaEntity | candles | OHLCV data |
| TradeJpaEntity | trades | Order records |
| PositionJpaEntity | positions | Position tracking |
| SignalJpaEntity | signals | Signal history |
| AccountSnapshotJpaEntity | account_snapshots | Account state history |
| DailySummaryJpaEntity | daily_summaries | Daily statistics |

## Repository Adapters

Each domain repository has a corresponding adapter:
- CandleRepositoryAdapter
- TradeRepositoryAdapter
- PositionRepositoryAdapter
- SignalRepositoryAdapter
- AccountSnapshotRepositoryAdapter
- DailySummaryRepositoryAdapter

## Schedulers

### CandleScheduler
Periodic candle data fetching.

| Schedule | Method | Description |
|----------|--------|-------------|
| Every 1 min | `fetchCandles()` | Fetch latest candles |

### DailySummaryScheduler
Daily summary generation.

| Schedule | Method | Description |
|----------|--------|-------------|
| Daily 00:05 | `generateDailySummary()` | Generate previous day summary |

### TradingSchedulerConfig
Enables `@EnableScheduling` for the module.
