# Stock Domain Layer

Pure domain models for Gap & Pullback stock trading. No framework dependencies.

## Subdomains

### stock/ - Monitored Gap Stocks

**Stock Entity**
| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| stockCode | String | Stock code (e.g., "005930") |
| stockName | String | Stock name |
| tradingDate | LocalDate | Trading date |
| state | StockState | State machine status |
| prevClosePrice | BigDecimal | Previous close price |
| openPrice | BigDecimal | Open price |
| currentPrice | BigDecimal | Current price |
| highPrice | BigDecimal | High price |
| lowPrice | BigDecimal | Low price |
| volume | Long | Volume |
| tradeValue | BigDecimal | Trade value |
| gapPercent | BigDecimal | Gap percentage |
| marketCap | BigDecimal | Market cap |
| tradeStrength | BigDecimal | Trade strength |
| highAfterOpen | BigDecimal | High after market open |
| pullbackLow | BigDecimal | Pullback low price |

**StockState Enum (State Machine)**
```
WATCHING → HIGH_FORMED → PULLBACK → ENTRY_READY → ENTERED → EXITED
                                 ↘ FILTERED_OUT (조건 미달)
```
| State | Korean | Description |
|-------|--------|-------------|
| WATCHING | 감시중 | Gap screening passed, waiting for high formation |
| HIGH_FORMED | 고점형성 | Price >= open × 1.015 |
| PULLBACK | 눌림목 | Drop 1.5-3.0% from high |
| ENTRY_READY | 진입대기 | Bounce +0.3% confirmed |
| ENTERED | 보유중 | Position opened |
| EXITED | 청산완료 | Position closed |
| FILTERED_OUT | 제외 | Drop > 3.0% from high |

### position/ - Stock Positions

**StockPosition Entity**
| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| stockId | Long | FK to Stock |
| stockCode | String | Stock code |
| tradingDate | LocalDate | Trading date |
| status | StockPositionStatus | Position status |
| entryPrice | BigDecimal | Entry price |
| entryQuantity | Integer | Entry quantity |
| remainingQuantity | Integer | Remaining quantity |
| stopLossPrice | BigDecimal | Stop loss price |
| trailingHigh | BigDecimal | Trailing stop high |
| trailingStopPrice | BigDecimal | Trailing stop price |
| tp1Executed | boolean | TP1 executed flag |
| tp2Executed | boolean | TP2 executed flag |
| tp3Executed | boolean | TP3 executed flag |
| dayHighPrice | BigDecimal | Day high for TP2/TP3 |
| realizedPnl | BigDecimal | Realized P&L |
| closeReason | StockCloseReason | Close reason |

**StockPositionStatus Enum**
| Status | Korean | Description |
|--------|--------|-------------|
| OPEN | 오픈 | Full position held |
| PARTIAL | 부분청산 | Partial exit executed |
| CLOSED | 종료 | Fully closed |

**StockCloseReason Enum**
| Reason | Korean | Condition |
|--------|--------|-----------|
| TP1 | 1차익절 | +1.5% |
| TP2 | 2차익절 | Day high |
| TP3 | 3차익절 | High +1% |
| STOP_LOSS | 손절 | -1.5% |
| TRAILING_STOP | 트레일링 | High -0.8% |
| TIME_EXIT | 시간청산 | 11:20 KST |
| MANUAL | 수동 | Manual close |
| EMERGENCY | 긴급청산 | Emergency |

### trade/ - Stock Trades

**StockTrade Entity**
| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| positionId | Long | FK to Position |
| stockCode | String | Stock code |
| tradingDate | LocalDate | Trading date |
| tradeType | StockTradeType | BUY / SELL |
| orderType | StockOrderType | MARKET / LIMIT |
| status | StockTradeStatus | Order status |
| orderPrice | BigDecimal | Order price |
| orderQuantity | Integer | Order quantity |
| filledPrice | BigDecimal | Filled price |
| filledQuantity | Integer | Filled quantity |
| orderId | String | Exchange order ID |

**StockTradeType Enum**: `BUY`, `SELL`
**StockOrderType Enum**: `MARKET`, `LIMIT`
**StockTradeStatus Enum**: `PENDING`, `FILLED`, `PARTIAL`, `CANCELLED`

### signal/ - Signal Audit Trail

**StockSignal Entity**
| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| stockCode | String | Stock code |
| signalType | String | Signal type (GAP_DETECTED, HIGH_FORMED, PULLBACK_ENTRY, etc.) |
| gapPercent | BigDecimal | Gap percentage (for GAP_DETECTED) |
| highPrice | BigDecimal | High price (for HIGH_FORMED) |
| entryPrice | BigDecimal | Entry price (for PULLBACK_ENTRY) |
| createdAt | LocalDateTime | Creation timestamp |

### candle/ - Stock OHLCV Data

**StockCandle Entity**
| Field | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| stockCode | String | Stock code |
| interval | CandleInterval | Time interval |
| candleDatetime | LocalDateTime | Candle timestamp |
| openPrice | BigDecimal | Open |
| highPrice | BigDecimal | High |
| lowPrice | BigDecimal | Low |
| closePrice | BigDecimal | Close |
| volume | Long | Volume |

**CandleInterval Enum**: `MINUTE`, `DAILY`, `WEEKLY`

## Repository Interfaces (Ports)

### StockRepository
```java
Optional<Stock> findById(Long id)
Optional<Stock> findByStockCodeAndTradingDate(String code, LocalDate date)
List<Stock> findByTradingDateOrderByGapPercentDesc(LocalDate date)
List<Stock> findActiveStocks(LocalDate date)
List<Stock> findByTradingDateAndState(LocalDate date, StockState state)
Stock save(Stock stock)
```

### StockPositionRepository
```java
Optional<StockPosition> findById(Long id)
List<StockPosition> findOpenPositions(LocalDate date)
int countOpenPositions(LocalDate date)
StockPosition save(StockPosition position)
```

### StockTradeRepository
```java
Optional<StockTrade> findById(Long id)
List<StockTrade> findByPositionId(Long positionId)
StockTrade save(StockTrade trade)
```

### StockSignalRepository
```java
StockSignal save(StockSignal signal)
List<StockSignal> findByStockCode(String code)
```

### StockCandleRepository
```java
List<StockCandle> findByStockCodeAndInterval(String code, CandleInterval interval)
StockCandle save(StockCandle candle)
```
