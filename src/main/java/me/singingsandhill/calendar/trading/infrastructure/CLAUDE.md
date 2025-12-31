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
  indicators:
    maShort: 5
    maMid: 20
    maLong: 60
    rsiPeriod: 14
    stochK: 14
    stochD: 3
    volumeMa: 20
  thresholds:
    signalBuy: 50
    signalSell: -50
    rsiOversold: 30
    rsiOverbought: 70
    stochOversold: 20
    stochOverbought: 80
  risk:
    stopLoss: -0.10
    takeProfit: 0.20
    trailingStop: 0.05
  rebalancing:
    enabled: true
    defaultRatio: 0.50
    bullRatio: 0.70
    bearRatio: 0.30
    deviationTrigger: 0.10
```

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
