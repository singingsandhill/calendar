# Trading Application Layer

Business logic services for trading bot operation.

## Core Services

### TradingBotService
Main orchestrator for the trading bot.

| Method | Description |
|--------|-------------|
| `start()` | Start the trading bot |
| `stop()` | Stop the bot |
| `pause()` | Pause execution (keep state) |
| `resume()` | Resume from paused state |
| `executeTradeLoop()` | Main 1-minute execution loop |
| `executeTradeBySignal()` | Execute buy/sell with multi-position support |
| `executeBuy(market, signal)` | Place market buy order with ATR-based sizing |
| `executeSell(market, signal, position)` | Close position with fee-adjusted P&L check |
| `extractExecutedPrice()` | Calculate weighted average price from partial fills |
| `calculateDynamicOrderRatio()` | ATR-based dynamic order ratio (15-35%) |
| `manualBuy(amount)` | Manual buy override |
| `manualSell(volume)` | Manual sell override |
| `emergencyClose()` | Liquidate all positions |

**Trade Loop**:
1. Update candle data
2. Check risk rules (stop-loss/take-profit/trailing stop)
3. Check rebalancing conditions
4. Generate technical signals
5. Execute trades based on signals (max 2 positions, dynamic 15-35% order ratio)

**Recent Changes**:
- Fee-included P&L calculation (`calculateUnrealizedPnlPctWithFee`)
- Weighted average price for partial fills (`extractExecutedPrice`)
- ATR-based dynamic order sizing (high volatility: 15%, low: 35%)
- Minimum profit threshold: 0.6% before selling

### CandleService
Candle data management.

| Method | Description |
|--------|-------------|
| `fetchAndSaveCandles()` | Fetch 1-min candles from API |
| `initializeCandles()` | Load initial 200 candles on startup |
| `cleanupOldCandles()` | Delete candles older than 7 days |
| `getLatestCandles(count)` | Get recent candles for analysis |

### IndicatorService
Technical indicator calculation.

| Method | Description |
|--------|-------------|
| `calculate(candles)` | Calculate all indicators |

**Indicators Calculated**:
- SMA: 5, 20, 60-period moving averages
- RSI: 14-period Relative Strength Index
- RSI Trend: 현재 RSI vs 이전 RSI 비교 (+1/-1/0)
- Stochastic: %K and %D (Slow)
- Volume MA: 20-period average volume

Returns `IndicatorResult` record.

### SignalService
Trading signal generation.

| Method | Description |
|--------|-------------|
| `generateSignal()` | Generate signal from current market data |

**Signal Logic** (Updated):
- Score >= 40 AND RSI < 70 AND StochK < 85 → BUY
- Score <= -40 AND RSI > 30 AND StochK > 15 → SELL
- Otherwise → HOLD

*Note: MA60 requirement removed to allow low-point buying opportunities*

**Score Components**:
- MA Cross: ±25 | MA Trend: ±15 | RSI Divergence: ±20
- RSI Level: ±15 | Stoch Divergence: ±15 | Stoch Level: ±15
- Volume Divergence: ±20 | RSI Trend: ±10

### DivergenceService
Divergence pattern detection.

| Method | Description |
|--------|-------------|
| `detect(candles, indicator)` | Detect divergence for indicator |

**Divergence Types**:
- Bullish: Price Lower Low + Indicator Higher Low
- Bearish: Price Higher High + Indicator Lower High

Detects RSI, Stochastic, and Volume divergences.

### RiskManagementService
Risk rule enforcement with trailing stop.

| Method | Description |
|--------|-------------|
| `checkAndExecuteRiskRules()` | Check stop-loss/take-profit/trailing stop |
| `closePosition(reason)` | Close position with reason |
| `emergencyClose()` | Liquidate all + cancel orders |

**Risk Levels**:
- Stop-loss: -8%
- Take-profit: +15%
- Trailing Stop: +10% 도달 시 활성화, -3% 추적

### RebalanceService
Portfolio rebalancing.

| Method | Description |
|--------|-------------|
| `checkAndExecute()` | Check and execute rebalancing |

**Dynamic Target Ratio**:
- Bullish (price > MA60): 70% coins / 30% KRW
- Bearish (price < MA60): 30% coins / 70% KRW
- Trigger: 10% deviation from target

### ProfitService
P&L statistics and reporting.

| Method | Description |
|--------|-------------|
| `getProfitSummary()` | Aggregate P&L statistics |
| `saveAccountSnapshot()` | Record current account state |
| `generateDailySummary()` | Create daily stats |
| `getDailySummaries(days)` | Historical daily P&L |
| `getAccountSnapshots(hours)` | Account value history |

## DTOs

### IndicatorResult (Record)
```java
BigDecimal currentPrice
BigDecimal ma5, ma20, ma60
BigDecimal rsi
BigDecimal stochK, stochD
BigDecimal volumeMa, currentVolume
int rsiTrend  // +1: 상승, -1: 하락, 0: 중립
```

Helper methods: `isGoldenCross()`, `isDeathCross()`, `isPriceAboveMa60()`, `isRsiUptrend()`, `isRsiDowntrend()`

### DivergenceResult (Record)
```java
DivergenceType rsiDivergence
DivergenceType stochDivergence
DivergenceType volumeDivergence
```

Helper methods: `hasBullishDivergence()`, `hasBearishDivergence()`
