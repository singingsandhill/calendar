# Trading Presentation Layer

Dashboard controller and REST API endpoints.

## MVC Controller

### TradingDashboardController
Web dashboard for trading bot.

| Path | Template | Description |
|------|----------|-------------|
| GET `/trading` | trading/dashboard | Main dashboard |
| GET `/trading/trades` | trading/trades | Trade history |
| GET `/trading/settings` | trading/settings | Bot settings |

## REST API Controllers

### BotControlApiController
Base path: `/api/trading/bot`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/status` | Get bot running/paused state |
| POST | `/start` | Start the trading bot |
| POST | `/stop` | Stop the bot |
| POST | `/pause` | Pause bot execution |
| POST | `/resume` | Resume paused bot |
| POST | `/manual/buy` | Manual buy order (amount in KRW) |
| POST | `/manual/sell` | Manual sell order (volume in coins) |
| POST | `/emergency-close` | Emergency liquidation |

### ChartApiController
Base path: `/api/trading`

| Method | Path | Query Params | Description |
|--------|------|--------------|-------------|
| GET | `/candles` | count=200 | Candle data for charts |
| GET | `/ticker` | - | Real-time price + indicators |

### TradeApiController
Base path: `/api/trading`

| Method | Path | Query Params | Description |
|--------|------|--------------|-------------|
| GET | `/trades` | page=0, size=20 | Trade history (paginated) |
| GET | `/positions` | page=0, size=20 | Position history (paginated) |
| GET | `/profit/summary` | - | Aggregate P&L snapshot |
| GET | `/profit/daily` | days=30 | Daily P&L history |

## Response DTOs

### BotStatusResponse
```java
boolean running
boolean paused
String market
LocalDateTime startedAt
```

### CandleResponse
```java
LocalDateTime dateTime
BigDecimal open, high, low, close
BigDecimal volume
```

### TickerResponse
```java
String market
BigDecimal price
BigDecimal change24h
IndicatorResult indicators
```

### TradeResponse
```java
Long id
String market
TradeType type
TradeStatus status
BigDecimal price, volume
BigDecimal executedPrice, executedVolume
BigDecimal fee
LocalDateTime createdAt
```

### PositionResponse
```java
Long id
String market
PositionStatus status
BigDecimal entryPrice, entryVolume
BigDecimal exitPrice
BigDecimal realizedPnl, realizedPnlPercent
CloseReason closeReason
LocalDateTime openedAt, closedAt
```

### ProfitSummaryResponse
```java
BigDecimal totalValue
BigDecimal unrealizedPnl
BigDecimal realizedPnl
BigDecimal totalPnl
int tradeCount
int winCount
BigDecimal winRate
BigDecimal avgPnlPercent
```

### DailySummaryResponse
```java
LocalDate date
int buyCount, sellCount
BigDecimal realizedPnl
int winCount, loseCount
BigDecimal winRate
BigDecimal startBalance, endBalance
BigDecimal balanceChange
```
