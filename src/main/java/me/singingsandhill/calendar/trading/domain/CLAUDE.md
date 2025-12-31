# Trading Domain Layer

Pure domain models organized by subdomain. No framework dependencies.

## Subdomains

```
domain/
├── candle/      # OHLCV candlestick data
├── trade/       # Individual trade orders
├── position/    # Open/closed trading positions
├── signal/      # Technical analysis signals
└── account/     # Account snapshots, daily summaries
```

## Entities

### Candle (candle/)
OHLCV candlestick data from exchange.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| market | String | Trading pair (e.g., "KRW-ADA") |
| candleDateTime | LocalDateTime | Candle timestamp |
| openingPrice | BigDecimal | Open price |
| highPrice | BigDecimal | High price |
| lowPrice | BigDecimal | Low price |
| tradePrice | BigDecimal | Close price |
| candleAccTradeVolume | BigDecimal | Volume |
| candleAccTradePrice | BigDecimal | Trade value |

### Trade (trade/)
Individual buy/sell order.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| market | String | Trading pair |
| tradeType | TradeType | BUY, SELL |
| status | TradeStatus | WAIT, DONE, CANCEL |
| orderPrice | BigDecimal | Order price |
| orderVolume | BigDecimal | Order volume |
| executedPrice | BigDecimal | Actual executed price |
| executedVolume | BigDecimal | Actual executed volume |
| fee | BigDecimal | Trading fee |
| signalScore | Integer | Signal score at order time |
| signalInfo | String | Signal details |

### Position (position/)
Trading position with P&L tracking.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| market | String | Trading pair |
| status | PositionStatus | OPEN, CLOSED |
| entryPrice | BigDecimal | Average entry price |
| entryVolume | BigDecimal | Position size |
| entryAmount | BigDecimal | Total entry cost |
| exitPrice | BigDecimal | Exit price (if closed) |
| realizedPnl | BigDecimal | Realized P&L amount |
| realizedPnlPercent | BigDecimal | Realized P&L % |
| closeReason | CloseReason | Why position was closed |
| stopLossPrice | BigDecimal | Stop-loss level |
| takeProfitPrice | BigDecimal | Take-profit level |

### Signal (signal/)
Trading signal from technical analysis.

| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| market | String | Trading pair |
| signalType | SignalType | BUY, SELL, HOLD |
| totalScore | Integer | Composite signal score |
| currentPrice | BigDecimal | Price at signal time |
| ma5/ma20/ma60 | BigDecimal | Moving averages |
| rsi | BigDecimal | RSI value |
| stochK/stochD | BigDecimal | Stochastic values |
| rsiDivergence | DivergenceType | RSI divergence |
| stochDivergence | DivergenceType | Stochastic divergence |
| volumeDivergence | DivergenceType | Volume divergence |

**Score Components**:
- MA Cross: ±30 (golden/death cross)
- MA Trend: ±10 (price vs MA60)
- RSI Divergence: ±25
- RSI Level: ±10 (oversold/overbought)
- Stochastic Divergence: ±20
- Stochastic Level: ±10
- Volume Divergence: ±15

### Account Models (account/)

**AccountSnapshot**: Point-in-time account state
- KRW balance, coin balance, average price, current price
- Calculated: total value, asset ratio, unrealized P&L

**DailySummary**: Daily trading statistics
- Trade counts, realized P&L, win/lose counts, win rate

## Enums

| Enum | Values |
|------|--------|
| TradeType | BUY, SELL |
| TradeStatus | WAIT, DONE, CANCEL |
| PositionStatus | OPEN, CLOSED |
| CloseReason | STOP_LOSS, TAKE_PROFIT, SIGNAL, MANUAL, REBALANCE |
| SignalType | BUY, SELL, HOLD |
| DivergenceType | BULLISH, BEARISH, NONE |

## Repository Interfaces

Each subdomain has its own repository interface (port):
- CandleRepository
- TradeRepository
- PositionRepository
- SignalRepository
- AccountSnapshotRepository
- DailySummaryRepository
