# Trading Templates

Thymeleaf templates for trading bot dashboard.

## Templates

| Template | Controller | Description |
|----------|------------|-------------|
| dashboard.html | TradingDashboardController | Main trading dashboard |
| trades.html | TradingDashboardController | Trade history view |

## Fragments

| Fragment | Path | Description |
|----------|------|-------------|
| header | fragments/header.html | Navigation and common elements |

## Dashboard (dashboard.html)

### Sections
1. **Bot Control Panel** - Start/Stop/Pause buttons, status indicator
2. **Price Chart** - Candlestick chart with MA overlays
3. **Indicator Panel** - RSI, Stochastic gauges
4. **Account Summary** - Balance, unrealized P&L, asset ratio
5. **Recent Positions** - Open/closed position table
6. **Signal History** - Recent trading signals

### JavaScript Integration
- Chart.js for candlestick and indicator charts
- Polling `/api/trading/ticker` for real-time updates
- Bot control via `/api/trading/bot/*` endpoints

### Model Variables
| Variable | Type | Description |
|----------|------|-------------|
| botStatus | BotStatusResponse | Current bot state |
| profitSummary | ProfitSummaryResponse | P&L overview |
| recentPositions | List<PositionResponse> | Recent positions |

## Trades (trades.html)

### Sections
1. **Trade History Table** - Paginated trade list
2. **Filter Controls** - Date range, type filter
3. **Statistics Summary** - Win rate, avg P&L

### Model Variables
| Variable | Type | Description |
|----------|------|-------------|
| trades | Page<TradeResponse> | Paginated trades |
| dailySummaries | List<DailySummaryResponse> | Daily P&L |

## CSS

Uses shared styles from `/css/style.css` plus trading-specific styles.

## API Integration Pattern

```javascript
// Fetch bot status
async function updateBotStatus() {
    const response = await fetch('/api/trading/bot/status');
    const status = await response.json();
    // Update UI
}

// Start bot
async function startBot() {
    await fetch('/api/trading/bot/start', { method: 'POST' });
    updateBotStatus();
}
```
