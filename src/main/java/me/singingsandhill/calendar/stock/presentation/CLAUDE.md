# Stock Presentation Layer

REST API and MVC controllers for stock trading dashboard.

## Structure

```
presentation/
├── api/                     # REST controllers
│   ├── StockBotApiController.java
│   ├── StockMonitoringApiController.java
│   ├── StockPositionApiController.java
│   ├── StockTradeApiController.java
│   └── StockSignalApiController.java
└── controller/              # MVC controllers
    └── StockDashboardController.java
```

## REST API Endpoints

### Bot Control API (`/api/stock/bot`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/status` | Get bot status (running, paused, phase, counts) |
| POST | `/start` | Start the trading bot |
| POST | `/stop` | Stop the trading bot |
| POST | `/pause` | Pause execution |
| POST | `/resume` | Resume from paused state |
| POST | `/emergency-close` | Emergency liquidation |

**Response: BotStatusResponse**
```java
boolean running
boolean paused
int watchingCount
int positionCount
String tradingPhase
LocalDateTime startedAt
```

### Monitoring API (`/api/stock/monitoring`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get all watchlist stocks |
| GET | `/active` | Get active stocks (non-terminal) |
| GET | `/state/{state}` | Get stocks by state (WATCHING, HIGH_FORMED, etc.) |

### Position API (`/api/stock/positions`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get all positions |
| GET | `/open` | Get open positions |
| GET | `/closed` | Get closed positions |
| GET | `/{id}` | Get position by ID |

### Trade API (`/api/stock/trades`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get all trades |
| GET | `/position/{positionId}` | Get trades by position |

### Signal API (`/api/stock/signals`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get all signals (audit trail) |
| GET | `/stock/{stockCode}` | Get signals by stock code |

## MVC Endpoints

### StockDashboardController (`/stock`)

| Method | Path | Template | Description |
|--------|------|----------|-------------|
| GET | `/` | `stock/dashboard` | Main dashboard |
| GET | `/history` | `stock/history` | Position history |
| GET | `/settings` | `stock/settings` | Configuration display |

**Dashboard Model Attributes**:
- `botStatus` - Bot running status
- `watchlist` - Active monitoring stocks
- `openPositions` - Current open positions
- `closedPositions` - Closed positions today
- `totalRealizedPnl` - Total P&L
- `winCount` / `loseCount` - Win/loss statistics
- `tradingDate` - Current trading date

## Templates

| Template | Description |
|----------|-------------|
| `stock/dashboard.html` | Main trading dashboard with bot controls |
| `stock/history.html` | Position and trade history |
| `stock/settings.html` | Configuration display |
| `stock/fragments/header.html` | Stock module header fragment |

## Response Patterns

- **Success**: 200 OK with data
- **Created**: 201 Created for new resources
- **Error**: BusinessException with appropriate HTTP status
  - 404: StockNotFoundException, PositionNotFoundException
  - 400: TradingHoursException, InsufficientBalanceException
  - 500: KisApiException
