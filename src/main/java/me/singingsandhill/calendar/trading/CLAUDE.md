# Trading Module

Cryptocurrency algorithmic trading bot for Bithumb exchange. Uses technical indicators (MA, RSI, Stochastic) with divergence detection for signal generation. Includes automated risk management and portfolio rebalancing.

## Architecture

Follows **Hexagonal Architecture** with four layers:

```
trading/
├── domain/           # Domain models by subdomain (candle, trade, position, signal, account)
├── application/      # Trading services, indicator calculation, signal generation
├── infrastructure/   # Bithumb API, JPA persistence, schedulers, config
└── presentation/     # Dashboard controller, REST API endpoints
```

## Core Components

| Component | Description |
|-----------|-------------|
| TradingBotService | Main orchestrator - lifecycle, trade execution |
| IndicatorService | Technical indicator calculation (MA, RSI, Stochastic) |
| SignalService | Trading signal generation with scoring |
| DivergenceService | Bullish/bearish divergence detection |
| RiskManagementService | Stop-loss, take-profit enforcement |
| RebalanceService | Portfolio rebalancing by market condition |

## Trading Flow

```
Bithumb API → Candles → Indicators → Divergences → Signals → Trade Execution
                                                         ↓
                                    Risk Management ← Position Tracking
```

## Configuration

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
  thresholds:
    signalBuy: 50
    signalSell: -50
  risk:
    stopLoss: -0.10      # -10%
    takeProfit: 0.20     # +20%
  rebalancing:
    enabled: true
    defaultRatio: 0.50
```

## REST API

| Path | Description |
|------|-------------|
| `/api/trading/bot/*` | Bot control (start/stop/pause) |
| `/api/trading/candles` | Candle data for charts |
| `/api/trading/ticker` | Real-time price and indicators |
| `/api/trading/trades` | Trade history |
| `/api/trading/positions` | Position history |
| `/api/trading/profit/*` | P&L statistics |

## Package Documentation

| Package | Path | Description |
|---------|------|-------------|
| domain | `trading/domain/CLAUDE.md` | Candle, Trade, Position, Signal entities |
| application | `trading/application/CLAUDE.md` | Trading services, DTOs |
| infrastructure | `trading/infrastructure/CLAUDE.md` | Bithumb API, JPA, schedulers |
| presentation | `trading/presentation/CLAUDE.md` | Controllers, API endpoints |
