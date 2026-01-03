# 코인 트레이딩 봇

Bithumb 거래소 기반 암호화폐 알고리즘 트레이딩 봇. 기술적 지표와 다이버전스 감지를 통한 자동 매매 시스템.

## 1. 아키텍처

### 헥사고날 아키텍처

```
trading/
├── domain/           # 도메인 모델 (candle, trade, position, signal, account)
├── application/      # 비즈니스 로직 (서비스, 지표 계산, 신호 생성)
├── infrastructure/   # 외부 연동 (Bithumb API, JPA, 스케줄러)
└── presentation/     # 대시보드 컨트롤러, REST API
```

### 트레이딩 플로우

```
Bithumb API → 캔들 데이터 → 기술적 지표 → 다이버전스 → 신호 생성 → 매매 실행
                                                              ↓
                              리스크 관리 (손절/익절/추적손절) ← 포지션 추적
                                          ↓
                                    포트폴리오 리밸런싱
```

---

## 2. 핵심 서비스

### TradingBotService (메인 오케스트레이터)

봇 생명주기 관리 및 1분 주기 메인 실행 루프.

| 메서드 | 설명 |
|--------|------|
| `start()` / `stop()` | 봇 시작/중지 |
| `pause()` / `resume()` | 일시정지/재개 |
| `executeTradeLoop()` | 1분 주기 메인 루프 |
| `executeTradeBySignal()` | 신호 기반 매매 실행 (다중 포지션 지원) |
| `executeBuy()` / `executeSell()` | 개별 매수/매도 실행 |
| `calculateDynamicOrderRatio()` | ATR 기반 동적 주문 비율 계산 |
| `extractExecutedPrice()` | 체결가 가중평균 계산 (부분 체결 대응) |
| `emergencyClose()` | 긴급 청산 |

**1분 실행 루프**:
1. 캔들 데이터 업데이트
2. 리스크 체크 (손절/익절/추적손절)
3. 리밸런싱 체크
4. 신호 생성 및 분석
5. 신호에 따른 매매 실행

### IndicatorService (기술적 지표)

| 지표 | 메서드 | 기간 |
|------|--------|------|
| SMA (단순 이동평균) | `calculateMA()` | 5, 20, 60 |
| RSI | `calculateRSI()` | 14 |
| RSI 추세 | `calculateRsiTrend()` | 현재 vs 이전 RSI |
| Stochastic %K | `calculateStochasticK()` | 14 |
| Stochastic %D | `calculateStochasticD()` | 14 / 3 |
| Volume MA | `calculateVolumeMA()` | 20 |
| ATR | `calculateATR()` | 14 |
| ATR % | `calculateATRPercent()` | 변동성 지표 |

### SignalService (신호 생성)

기술적 지표와 다이버전스를 종합하여 매매 신호 생성.

**신호 조건**:
```
매수: score >= 40 AND RSI < 70 AND StochK < 85
매도: score <= -40 AND RSI > 30 AND StochK > 15
홀드: 그 외
```

### DivergenceService (다이버전스 감지)

- **상승 다이버전스**: 가격 저점↓ + 지표 저점↑ (바닥 반전 신호)
- **하락 다이버전스**: 가격 고점↑ + 지표 고점↓ (천장 반전 신호)
- **감지 대상**: RSI, Stochastic, Volume
- **분석 기간**: 20 캔들 (최소 3캔들 간격)

### RiskManagementService (리스크 관리)

매분 리스크 규칙 체크 및 자동 청산.

| 규칙 | 레벨 | 동작 |
|------|------|------|
| 손절 (Stop-Loss) | -8% | 즉시 청산 |
| 익절 (Take-Profit) | +15% | 즉시 청산 |
| 추적 손절 활성화 | +10% 도달 | High Water Mark 추적 시작 |
| 추적 손절 발동 | HWM 대비 -3% | 청산 |

### RebalanceService (리밸런싱)

시장 상태에 따른 동적 비율 조정.

```
시장 상태 → 목표 비율:
- 상승장 (가격 > MA60): 코인 70% / KRW 30%
- 하락장 (가격 < MA60): 코인 30% / KRW 70%
- 중립 (기본값): 코인 50% / KRW 50%

발동 조건: 목표 대비 10% 이상 편차
```

---

## 3. 신호 점수 시스템

총 점수 범위: **-120 ~ +120**

### 점수 구성 요소

| 구성 요소 | 매수 점수 | 매도 점수 | 조건 |
|-----------|-----------|-----------|------|
| MA 크로스 (골든/데드) | +25 | -25 | 실제 크로스 발생 |
| MA 추세 | +15 | -15 | 가격 vs MA60 |
| RSI 다이버전스 | +20 | -20 | 상승/하락 다이버전스 |
| RSI 레벨 | +15 | -15 | 과매도(<35) / 과매수(>65) |
| Stochastic 다이버전스 | +15 | -15 | 상승/하락 다이버전스 |
| Stochastic 레벨 | +15 | -15 | 과매도(<25) / 과매수(>75) |
| Volume 다이버전스 | +20 | -20 | 상승/하락 다이버전스 |
| RSI 추세 | +10 | -10 | RSI 상승/하락 추세 |

### 매매 임계값

```yaml
thresholds:
  signalBuy: 40       # 매수 신호 점수
  signalSell: -40     # 매도 신호 점수
  buyRsiMax: 70       # 매수 시 RSI 상한
  buyStochKMax: 85    # 매수 시 StochK 상한
  sellRsiMin: 30      # 매도 시 RSI 하한
  sellStochKMin: 15   # 매도 시 StochK 하한
```

---

## 4. 리스크 관리 상세

### 손익 계산

**수수료 포함 P&L 계산** (`Position.calculateUnrealizedPnlPctWithFee()`):
```java
// 수수료 포함 미실현 손익
BigDecimal currentValue = currentPrice × entryVolume;
BigDecimal estimatedExitFee = currentValue × feeRate;
BigDecimal totalFees = entryFee + estimatedExitFee;
BigDecimal unrealizedPnl = currentValue - entryAmount - totalFees;
```

### 최소 수익률 임계값

매도 신호 발생 시에도 **최소 0.6% 수익** 확보 후 매도:
- 왕복 수수료: 0.5% (매수 0.25% + 매도 0.25%)
- 마진: 0.1%
- 합계: 0.6%

### 추적 손절 (Trailing Stop)

```
1. 진입가 대비 +10% 도달 → 추적 손절 활성화
2. High Water Mark (최고가) 갱신 시 → 추적 손절가 상향
3. 추적 손절가 = High Water Mark × (1 - 0.03)
4. 현재가 ≤ 추적 손절가 → 청산
```

---

## 5. 설정 옵션 (TradingProperties)

### 봇 설정

```yaml
trading:
  bot:
    enabled: false          # 봇 활성화
    market: "KRW-ADA"       # 거래 쌍
    maxPositions: 2         # 최대 동시 포지션 수
    orderRatio: 0.25        # 기본 주문 비율 (25%)
    orderRatioMin: 0.15     # 최소 비율 (변동성 높음)
    orderRatioMax: 0.35     # 최대 비율 (변동성 낮음)
```

### 동적 주문 비율 (ATR 기반)

```
ATR% ≥ 3%  → 15% (변동성 높음, 보수적)
ATR% ≤ 1%  → 35% (변동성 낮음, 적극적)
1% < ATR% < 3% → 선형 보간
```

### 지표 설정

```yaml
  indicators:
    maShort: 5              # 단기 MA
    maMid: 20               # 중기 MA
    maLong: 60              # 장기 MA
    rsiPeriod: 14           # RSI 기간
    stochK: 14              # Stochastic K
    stochD: 3               # Stochastic D
    volumeMa: 20            # Volume MA
    atrPeriod: 14           # ATR 기간
```

### 리스크 설정

```yaml
  risk:
    stopLoss: -0.08              # 손절 -8%
    takeProfit: 0.15             # 익절 +15%
    trailingStop: 0.03           # 추적 손절 -3%
    trailingActivation: 0.10     # 추적 활성화 +10%
    takerFeeRate: 0.0025         # 수수료율 0.25%
    minProfitThreshold: 0.006    # 최소 수익률 0.6%
```

### 리밸런싱 설정

```yaml
  rebalancing:
    enabled: true
    defaultRatio: 0.50      # 기본 비율
    bullRatio: 0.70         # 상승장 코인 비율
    bearRatio: 0.30         # 하락장 코인 비율
    deviationTrigger: 0.10  # 편차 발동 조건
```

---

## 6. REST API

### 봇 제어 API

| 엔드포인트 | 메서드 | 설명 |
|------------|--------|------|
| `/api/trading/bot/status` | GET | 봇 상태 조회 |
| `/api/trading/bot/start` | POST | 봇 시작 |
| `/api/trading/bot/stop` | POST | 봇 중지 |
| `/api/trading/bot/pause` | POST | 일시정지 |
| `/api/trading/bot/resume` | POST | 재개 |
| `/api/trading/bot/manual/buy` | POST | 수동 매수 (amount=KRW) |
| `/api/trading/bot/manual/sell` | POST | 수동 매도 (volume=코인) |
| `/api/trading/bot/emergency-close` | POST | 긴급 청산 |

### 데이터 조회 API

| 엔드포인트 | 메서드 | 설명 |
|------------|--------|------|
| `/api/trading/candles` | GET | 캔들 데이터 (count=200) |
| `/api/trading/ticker` | GET | 실시간 시세 + 지표 |
| `/api/trading/trades` | GET | 거래 내역 (페이지네이션) |
| `/api/trading/positions` | GET | 포지션 내역 (페이지네이션) |
| `/api/trading/profit/summary` | GET | 손익 요약 |
| `/api/trading/profit/daily` | GET | 일별 손익 (days=30) |

---

## 7. 도메인 모델

### 엔티티

| 서브도메인 | 엔티티 | 설명 |
|------------|--------|------|
| candle/ | Candle | OHLCV 캔들 데이터 |
| trade/ | Trade | 개별 매수/매도 주문 |
| position/ | Position | 진입/청산 포지션 (P&L 추적) |
| signal/ | Signal | 기술적 분석 신호 |
| account/ | AccountSnapshot, DailySummary | 계좌 스냅샷, 일별 요약 |

### Enum

```java
TradeType: BUY, SELL
TradeStatus: WAIT, DONE, CANCEL, FAILED
PositionStatus: OPEN, CLOSED
CloseReason: STOP_LOSS, TAKE_PROFIT, TRAILING_STOP, SIGNAL, MANUAL, REBALANCE
SignalType: BUY, SELL, HOLD
DivergenceType: BULLISH, BEARISH, NONE
```

---

## 8. 최근 변경 사항

### 수수료 포함 P&L 계산
- **변경 전**: `calculateUnrealizedPnlPct()` - 수수료 미포함
- **변경 후**: `calculateUnrealizedPnlPctWithFee()` - 수수료 포함
- **영향**: 매도 판단 시 실제 수익률 반영 → 손실 매도 방지

### 체결가 가중평균 계산
- **변경 전**: 첫 번째 체결가만 사용
- **변경 후**: trades 리스트 전체 가중평균 계산
- **영향**: 부분 체결 시 정확한 평균 진입가 반영

### 다중 포지션 지원
- 최대 2개 포지션 동시 보유 가능
- 포지션별 독립적 리스크 관리

### 동적 주문 비율
- ATR 기반 변동성 측정
- 변동성 높음: 15% (보수적)
- 변동성 낮음: 35% (적극적)

---

## 9. 파일 구조

```
trading/
├── CLAUDE.md                    # 모듈 개요
├── domain/
│   ├── CLAUDE.md               # 도메인 모델 문서
│   ├── candle/                 # Candle 엔티티
│   ├── trade/                  # Trade 엔티티
│   ├── position/               # Position 엔티티
│   ├── signal/                 # Signal 엔티티
│   └── account/                # AccountSnapshot, DailySummary
├── application/
│   ├── CLAUDE.md               # 서비스 문서
│   ├── service/
│   │   ├── TradingBotService.java
│   │   ├── IndicatorService.java
│   │   ├── SignalService.java
│   │   ├── DivergenceService.java
│   │   ├── RiskManagementService.java
│   │   ├── RebalanceService.java
│   │   ├── CandleService.java
│   │   └── ProfitService.java
│   └── dto/
├── infrastructure/
│   ├── CLAUDE.md               # 인프라 문서
│   ├── api/                    # Bithumb API 클라이언트
│   ├── persistence/            # JPA 엔티티, 레포지토리
│   ├── config/                 # 설정 클래스
│   └── scheduler/              # 스케줄러
└── presentation/
    ├── CLAUDE.md               # API 문서
    ├── api/                    # REST 컨트롤러
    └── controller/             # 웹 컨트롤러
```
