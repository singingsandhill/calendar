# Stock Module

Gap & Pullback (갭 상승 눌림목) trading bot for Korean stock market using Korea Investment Securities (한국투자증권) API.

## Architecture

Follows **Hexagonal Architecture** with four layers:

```
stock/
├── domain/           # Domain models (stock, position, trade, candle, signal)
├── application/      # Trading services, screening, risk management
├── infrastructure/   # KIS API, JPA persistence, schedulers, config
└── presentation/     # Dashboard controller, REST API endpoints
```

## Core Components

| Component | Description |
|-----------|-------------|
| GapPullbackBotService | Main orchestrator - lifecycle, trading loops |
| ScreeningService | Gap stock screening (2-7% gap filter) |
| PullbackDetectionService | State machine for pullback pattern detection |
| StockPositionService | Position management, partial exits |
| StockRiskService | Stop loss, take profit, trailing stop |

## Trading Flow

```
08:30  PreMarket    → Collect prev day data
09:00  Screening    → Find 2-7% gap stocks (max 10)
09:10  Trading      → Every 5 seconds:
                      1. Check risk rules (SL, TP, trailing)
                      2. Update stock states
                      3. Enter if ENTRY_READY & positions < max
11:20  Final Exit   → Close all remaining positions
```

## State Machine

```
WATCHING      → Price >= Open × 1.015         → HIGH_FORMED
HIGH_FORMED   → Price <= High × 0.985         → PULLBACK
PULLBACK      → Price >= PullbackLow × 1.003  → ENTRY_READY
ENTRY_READY   → Buy order executed            → ENTERED
ENTERED       → All exits completed           → EXITED

PULLBACK      → Price < High × 0.970          → FILTERED_OUT (too deep)
```

## Exit Rules

| Exit Type | Condition | Action |
|-----------|-----------|--------|
| Stop Loss | Price ≤ Entry × 0.985 | Sell 100% |
| TP1 | Price ≥ Entry × 1.015 | Sell 50% |
| TP2 | Price ≥ High | Sell 60% of remaining |
| TP3 | Price ≥ High × 1.01 | Sell remaining |
| Trailing | Price ≤ TrailingHigh × 0.992 | Sell remaining |
| Time Exit | Time ≥ 11:20 | Sell 100% |

## Configuration

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
  screening:
    min-gap-percent: 2.0
    max-gap-percent: 7.0
    min-market-cap: 150000000000
    min-trade-strength: 110
  entry:
    high-threshold-percent: 1.5
    pullback-min-percent: 1.5
    pullback-max-percent: 3.0
    bounce-threshold-percent: 0.3
  exit:
    tp1-percent: 1.5
    tp1-ratio: 0.5
    final-exit-time: "11:20"
  risk:
    stop-loss-percent: 1.5
    trailing-stop-percent: 0.8
```

## REST API

| Path | Description |
|------|-------------|
| `/api/stock/bot/*` | Bot control (start/stop/pause/resume) |
| `/api/stock/monitoring` | Watching stocks list |
| `/api/stock/positions/*` | Position management |
| `/api/stock/trades` | Trade history |
| `/api/stock/signals` | Signal history (audit) |

## Dashboard

| Path | Description |
|------|-------------|
| `/stock` | Main dashboard |
| `/stock/history` | Position history |
| `/stock/settings` | Configuration display |

## Database Tables

| Table | Description |
|-------|-------------|
| stock_monitoring | Screened stocks with state |
| stock_positions | Position tracking with TP levels |
| stock_trades | Order records |
| stock_candles | OHLCV data |
| stock_signals | Signal audit trail |
