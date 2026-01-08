# Stock Application Layer

Business logic services for Gap & Pullback stock trading bot.

## Core Services

### GapPullbackBotService (Main Orchestrator)

Bot lifecycle and trading loop management.

| Method | Description |
|--------|-------------|
| `start()` | Start the trading bot |
| `stop()` | Stop the bot |
| `pause()` | Pause execution (keep state) |
| `resume()` | Resume from paused state |
| `getStatus()` | Get BotStatus (running, paused, watchingCount, positionCount, phase) |
| `executePreMarketLoop()` | Pre-market data collection (08:30~09:00) |
| `executeScreeningLoop()` | Gap stock screening (09:00~09:10) |
| `executeTradingLoop()` | Main trading loop (09:10~11:20) |
| `executeFinalExitCheck()` | Final liquidation (11:20) |
| `emergencyCloseAll()` | Emergency position liquidation |

**Trading Phases**:
- `PRE_MARKET_WAIT` - Before 08:30
- `PRE_MARKET` - 08:30~09:00
- `SCREENING` - 09:00~09:10
- `TRADING` - 09:10~11:20
- `FINAL_EXIT` - 11:20~11:30
- `MARKET_CLOSED` - After 11:30

### ScreeningService

Gap stock screening with multi-step filters.

| Method | Description |
|--------|-------------|
| `executeScreening(tradingDate, stockCodes)` | Execute gap screening |
| `getWatchlist(tradingDate)` | Get screened stocks by gap percent |
| `getActiveStocks(tradingDate)` | Get stocks in active states |
| `getStocksByState(tradingDate, state)` | Get stocks by specific state |

**Screening Filters**:
1. Gap ratio: 2-7%
2. Market cap: >= minMarketCap
3. Trade value: >= minTradeValue
4. Trade strength: >= minStrength (110)
5. Spread: <= maxSpread

### PullbackDetectionService

State machine implementation for pullback pattern detection.

| Method | Description |
|--------|-------------|
| `updateAllStockStates(tradingDate)` | Update all active stocks |
| `updateStockState(stock)` | Update single stock state |
| `getEntryReadyStocks(tradingDate)` | Get ENTRY_READY stocks |
| `filterOutStock(stock)` | Mark stock as FILTERED_OUT |

**State Transitions**:
- `WATCHING` → `HIGH_FORMED`: price >= open × 1.015
- `HIGH_FORMED` → `PULLBACK`: drop within 1.5-3.0% from high
- `HIGH_FORMED` → `FILTERED_OUT`: drop > 3.0% from high
- `PULLBACK` → `ENTRY_READY`: bounce >= 0.3% from pullback low

**Entry Validation**:
- Trade strength >= 105
- Order imbalance (bid/ask) > 1.2
- Pullback duration: 3-15 minutes

### StockPositionService

Position management with partial exits.

| Method | Description |
|--------|-------------|
| `openPosition(stock)` | Open new position |
| `closePosition(position, price, reason)` | Close position |
| `partialClose(position, percent, reason)` | Partial close |
| `countOpenPositions(tradingDate)` | Count open positions |
| `getOpenPositions(tradingDate)` | Get all open positions |

### StockRiskService

Risk management with multi-level take-profits.

| Method | Description |
|--------|-------------|
| `checkAndExecuteRiskRules(tradingDate)` | Check all open positions |
| `checkPositionRisk(position)` | Check single position risk |
| `executeTimeBasedExit(tradingDate)` | Time-based exit at 11:20 |
| `emergencyCloseAll(tradingDate)` | Emergency liquidation |

**Exit Rules**:
| Type | Condition | Action |
|------|-----------|--------|
| Stop Loss | price <= entry × 0.985 | Sell 100% |
| TP1 | price >= entry × 1.015 | Sell 50% |
| TP2 | price >= high | Sell 60% remaining |
| TP3 | price >= high × 1.01 | Sell remaining |
| Trailing | price <= trailingHigh × 0.992 | Sell remaining |
| Time Exit | time >= 11:20 | Sell 100% |

## Transaction Pattern

- Class-level: `@Transactional(readOnly = true)`
- Write methods override with `@Transactional`

## Exceptions

All extend `BusinessException` with `code` and `HttpStatus`.

| Exception | HTTP Status | When Thrown |
|-----------|-------------|-------------|
| `StockNotFoundException` | 404 | Stock lookup fails |
| `PositionNotFoundException` | 404 | Position lookup fails |
| `InsufficientBalanceException` | 400 | Not enough balance for order |
| `TradingHoursException` | 400 | Outside trading hours |
| `KisApiException` | 500 | KIS API call fails |
