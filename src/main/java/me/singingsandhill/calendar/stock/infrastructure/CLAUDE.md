# Stock Infrastructure Layer

External integrations and technical implementations for stock trading.

## Structure

```
infrastructure/
├── api/                     # Korea Investment Securities API
│   ├── KoreaInvestmentApiClient.java  # Unified API client
│   ├── KisAuthService.java            # Token management
│   ├── KisRestClient.java             # REST client
│   └── dto/                           # API response DTOs
├── config/
│   └── StockProperties.java           # Configuration properties
├── persistence/
│   ├── entity/              # JPA entities
│   ├── repository/          # JPA repositories
│   └── adapter/             # Repository adapters
└── scheduler/
    ├── StockSchedulerConfig.java      # @EnableScheduling
    └── StockTradingScheduler.java     # Trading loop scheduler
```

## KIS API Integration

### KoreaInvestmentApiClient (Unified Client)

**Authentication**
| Method | Description |
|--------|-------------|
| `isConfigured()` | Check if API keys are configured |
| `isAuthenticated()` | Check if access token is valid |
| `revokeToken()` | Revoke access token on shutdown |

**Market Data**
| Method | Description |
|--------|-------------|
| `getQuote(stockCode)` | Get current price quote |
| `getOrderbook(stockCode)` | Get order book (bid/ask) |
| `getDailyPrices(stockCode, days)` | Get historical daily prices |
| `getTradeStrength(stockCode)` | Get trade strength indicator |

**Account Operations**
| Method | Description |
|--------|-------------|
| `getBalance()` | Get account balance |
| `getBuyingPower()` | Get available buying power |
| `getOrders()` | Get pending orders |

**Order Operations**
| Method | Description |
|--------|-------------|
| `placeOrder(stockCode, type, quantity, price)` | Place order |
| `cancelOrder(orderId)` | Cancel pending order |

### KisAuthService
Token lifecycle management with automatic refresh.

### KisRestClient
Low-level REST API wrapper with WebClient.

### API DTOs

| DTO | Description |
|-----|-------------|
| `KisQuoteResponse` | Current price, gap calculation |
| `KisOrderbookResponse` | Bid/ask prices, spread calculation |
| `KisDailyPriceResponse` | Historical OHLCV |
| `KisBalanceResponse` | Account positions |
| `KisBuyingPowerResponse` | Available balance |
| `KisOrderResponse` | Order result |
| `KisOrderDetailResponse` | Order details |
| `KisTokenResponse` | Auth token |
| `KisHashkeyResponse` | Request hash key |
| `KisApiResponse` | Generic API response wrapper |

## Configuration

### StockProperties

```yaml
stock:
  kis:
    base-url: https://openapi.koreainvestment.com:9443
    app-key: ${KIS_APP_KEY}
    app-secret: ${KIS_APP_SECRET}
    account-number: ${KIS_ACCOUNT_NUMBER}
  bot:
    enabled: false
    max-positions: 5
    max-position-size: 5000000
  trading:
    pre-market-start: "08:30"
    market-open: "09:00"
    screening-end: "09:10"
    trading-end: "11:30"
  screening:
    min-gap-percent: 2.0
    max-gap-percent: 7.0
    min-market-cap: 150000000000
    min-trade-value: 100000000
    min-trade-strength: 110
    max-spread-percent: 0.5
    max-watchlist-size: 10
  entry:
    high-threshold-percent: 1.5
    pullback-min-percent: 1.5
    pullback-max-percent: 3.0
    bounce-threshold-percent: 0.3
    min-pullback-minutes: 3
    max-pullback-minutes: 15
  exit:
    tp1-percent: 1.5
    tp1-ratio: 0.5
    tp2-ratio: 0.6
    final-exit-time: "11:20"
  risk:
    stop-loss-percent: 1.5
    trailing-stop-percent: 0.8
```

## JPA Persistence

### Entities
| Entity | Table | Description |
|--------|-------|-------------|
| StockJpaEntity | stock_monitoring | Screened stocks |
| StockPositionJpaEntity | stock_positions | Position tracking |
| StockTradeJpaEntity | stock_trades | Order records |
| StockCandleJpaEntity | stock_candles | OHLCV data |
| StockSignalJpaEntity | stock_signals | Signal audit |

### Repository Adapters
Each adapter implements domain repository interface and converts between domain and JPA entities.

| Adapter | Domain Interface |
|---------|------------------|
| StockRepositoryAdapter | StockRepository |
| StockPositionRepositoryAdapter | StockPositionRepository |
| StockTradeRepositoryAdapter | StockTradeRepository |
| StockCandleRepositoryAdapter | StockCandleRepository |
| StockSignalRepositoryAdapter | StockSignalRepository |

## Schedulers

### StockTradingScheduler

Trading loop execution at fixed intervals.

| Schedule | Method | Description |
|----------|--------|-------------|
| 08:30~09:00 | `executePreMarketLoop()` | Pre-market data collection |
| 09:00~09:10 | `executeScreeningLoop()` | Gap screening |
| 09:10~11:20 (5초) | `executeTradingLoop()` | Main trading loop |
| 11:20 | `executeFinalExitCheck()` | Final liquidation |

### StockSchedulerConfig
Enables `@EnableScheduling` for the stock module.
