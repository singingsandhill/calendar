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
| RiskManagementService | Stop-loss, take-profit, trailing stop enforcement |
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
    maxPositions: 2      # 최대 동시 포지션 수
    orderRatio: 0.25     # 주문 금액 비율 (25%)
  indicators:
    maShort: 5
    maMid: 20
    maLong: 60
    rsiPeriod: 14
  thresholds:
    signalBuy: 40        # 매수 신호 점수
    signalSell: -40      # 매도 신호 점수
    rsiOversold: 35      # RSI 과매도
    rsiOverbought: 65    # RSI 과매수
    stochOversold: 25    # Stochastic 과매도
    stochOverbought: 75  # Stochastic 과매수
    buyRsiMax: 70        # 매수 시 RSI 상한
    buyStochKMax: 85     # 매수 시 StochK 상한
    sellRsiMin: 30       # 매도 시 RSI 하한
    sellStochKMin: 15    # 매도 시 StochK 하한
  risk:
    stopLoss: -0.08           # -8%
    takeProfit: 0.15          # +15%
    trailingStop: 0.03        # -3% 추적
    trailingActivation: 0.10  # +10% 도달 시 활성화
    takerFeeRate: 0.0025      # 수수료율 0.25%
    minProfitThreshold: 0.006 # 최소 수익률 0.6%
  rebalancing:
    enabled: true
    defaultRatio: 0.50
    bullRatio: 0.70           # 강세장 코인 비중
    bearRatio: 0.30           # 약세장 코인 비중
    deviationTrigger: 0.10    # 10% 편차 시 트리거
    cooldownMinutes: 240      # 4시간 쿨다운
    minOrderAmount: 5000      # 최소 주문 금액 (KRW)
    slippageBuffer: 0.005     # 0.5% 슬리피지 버퍼
    skipWhenDataInsufficient: true  # MA60 부족 시 스킵
```

## Recent Changes

### Rebalancing Logic Improvements
- **쿨다운 타임**: 4시간 간격으로 빈번한 리밸런싱 방지
- **최소 주문 금액**: 5,000원 미만 주문 스킵으로 API 오류 방지
- **슬리피지 버퍼**: 0.5% 버퍼 적용으로 시장가 주문 시 체결가 차이 보정
- **예외 처리 강화**: API 실패 시 Trade 상태를 FAILED로 업데이트
- **MA60 부족 처리**: 데이터 부족 시 리밸런싱 스킵 옵션 추가
- **상세 로깅**: 리밸런싱 전후 상태 명시적 로깅
- **설정 검증**: 시작 시 `@PostConstruct`로 설정값 유효성 검증

### Fee-included P&L Calculation
- `calculateUnrealizedPnlPctWithFee()` 사용으로 수수료 포함 수익률 계산
- 매도 판단 시 실제 수익률 반영 → 손실 매도 방지

### Weighted Average Price
- `extractExecutedPrice()`: 부분 체결 시 가중평균 체결가 계산
- 변경 전: 첫 번째 체결가만 사용
- 변경 후: trades 리스트 전체 가중평균

### ATR-based Dynamic Order Ratio
- 변동성 높음 (ATR ≥ 3%): 15% 주문
- 변동성 낮음 (ATR ≤ 1%): 35% 주문
- 중간: 선형 보간

### Signal Logic Update
- MA60 조건 제거 (저점 매수 기회 확대)
- 매수: score ≥ 40 AND RSI < 70 AND StochK < 85
- 매도: score ≤ -40 AND RSI > 30 AND StochK > 15

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
