# 코인 트레이딩 봇 실전 운영 관점 리뷰 (2026-07-06)

> 대상: `trading` 모듈 — Bithumb KRW-ADA 자동매매. Spring Boot 4 / Java 21, 헥사고날 구조.
> 방법: 8개 관점 멀티에이전트 정적 분석 → 발견 60건 → 발견별 적대적 검증(2회 재실행 합산 **73건 검증, 반박 0건**) → 핵심 P0는 코드 1차 확인.
> 성격: **분석·권고 문서.** 실제 구현은 단계적 PR(§7 우선순위)로 진행. 코드는 변경하지 않음.
> 관련: 선행 감사 [`coin-trading-profit-audit-2026-05-30.md`](coin-trading-profit-audit-2026-05-30.md)(수익성/전략), 결정 기록 [`docs/adr/trading/`](../adr/trading/).

핵심 파일: `TradingBotService.java`(869행), `RiskManagementService.java`, `RebalanceService.java`,
`TradingCircuitBreaker.java`, `BithumbPrivateApi.java`, `BithumbApiClient.java`,
`TradingProperties.java`, `common/.../SecurityConfig.java`, `application.yaml:124-186`.

> **범위 구분:** 선행 감사(2026-05-30)는 *수익성·전략*을 다뤘다. 본 리뷰는 *운영 안정성·보안·주문 안정성·테스트 가능성*에 초점을 둔다. ADR로 이미 수용된 트레이드오프(기본 LIVE, BACKTEST 미구현, 서킷 스트릭 인메모리, 리밸런스 maxPositions 우회 등)는 "버그"가 아니라 "재검토 대상"으로 표기한다.

---

## 1. 분석 대상 파일 요약

**시장 데이터 수집**
- `infrastructure/api/BithumbPublicApi.java` — 분봉/체결/호가 조회(무인증 공개 API, 타임아웃 30초).
- `infrastructure/api/BithumbApiClient.java` — Public+Private 통합 파사드. `getCurrentPrice()`는 **최우선 호가 (ask+bid)/2**(81행). 시장가 주문에 **모드 게이트**(`isLive()` 아니면 `simulateBuy/Sell` 인메모리 체결) 적용.
- `application/service/CandleService.java` — 1분봉 200개 수집·중복제거·배치저장(`@Transactional`, 44·53행), 7일 초과 정리.
- `domain/candle/Candle.java`(+Repository) — OHLCV 엔티티/포트.

**신호 생성**
- `application/service/IndicatorService.java` — MA(5/20/60), Wilder RSI(14), Slow Stochastic, 거래량 MA, RSI 추세, ATR%.
- `application/service/DivergenceService.java` — RSI/Stoch/거래량 다이버전스(피벗강도 3, 최소거리 5, 룩백 20).
- `application/service/SignalService.java` — 8개 컴포넌트 점수 합산(±128), `determineSignalType()`로 BUY/SELL/HOLD 판정, 모든 신호 DB 저장.
- `domain/signal/*`, `application/dto/{IndicatorResult,DivergenceResult}.java` — 신호/지표 스냅샷.

**주문 판단·실행 (오케스트레이터)**
- `application/service/TradingBotService.java` (869행) — 봇 수명주기(`AtomicBoolean running/paused`), `executeTradeLoop()` 틱 메인, `executeTradeBySignal/executeBuy/executeSell`, `manualBuy/manualSell/emergencyClose`, 체결가 재시도, ATR 동적 주문비율.
- `infrastructure/api/BithumbPrivateApi.java` — JWT 인증 주문/조회, 429 재시도(3회, 지수백오프).
- `infrastructure/api/auth/BithumbJwtGenerator.java` — Bithumb v1 JWT 생성, `isConfigured()`.

**리스크 관리**
- `application/service/RiskManagementService.java` — 포지션별 손절/트레일링/익절/시간청산, `closePosition()`, 긴급청산.
- `application/service/TradingCircuitBreaker.java` — 연속손실·일일손실 시 신규 BUY 차단.
- `application/service/RebalanceService.java` — MA60 국면 기반 목표비중 리밸런싱, FIFO 이익 포지션 청산.
- `application/service/ProfitService.java` — 손익 집계, 계좌 스냅샷, 일일 요약.
- `domain/position/Position.java` — 포지션 애그리거트(트레일링 상태기계, 청산 재시도 추적).

**스케줄링·설정·관측성**
- `infrastructure/scheduler/{CandleScheduler,DailySummaryScheduler,TradingSchedulerConfig}.java` — 매분 :05 트레이딩 루프, 5분 캔들동기화, 자정 정리, 5분 스냅샷, 00:01 요약.
- `infrastructure/config/{TradingProperties,WebClientConfig}.java` — 설정 바인딩, WebClient 타임아웃.
- `application/service/TradingEventService.java` — 거래 이벤트 DB 기록(`REQUIRES_NEW`).

**프레젠테이션**
- `presentation/api/{BotControlApiController,RebalanceApiController,TradingVerificationApiController,TradeApiController,ChartApiController,TradingEventApiController}.java` — 봇 제어·수동주문·검증(실주문)·조회 REST.

**테스트** (16개) — 지표(RSI/Stoch/다이버전스/형성봉), 서킷브레이커, 리밸런스(회계/쿨다운), 시간청산, 신호 가중, 엔트리 가드, 영속화, 봇 모드 게이트. **주문 실행·리스크 청산 트리거 통합 테스트는 없음.**

---

## 2. 현재 트레이딩 플로우

**틱 1회 (매분 :05초, `CandleScheduler` → `TradingBotService.executeTradeLoop()`):**

1. **게이트** — `bot.enabled=false`(기본)면 스케줄러 즉시 return. 봇은 `POST /api/trading/bot/start`로 `running=true` 해야 동작. `paused`면 스킵.
2. **캔들 수집** — `CandleService.fetchAndSaveCandles()`가 200개 조회 후 **이미 존재하는 타임스탬프는 스킵(기존값 유지)**하고 신규만 저장(71·77행).
3. **리스크 체크 (최우선)** — `RiskManagementService.checkAndExecuteRiskRules()`: OPEN 포지션별 현재가 갱신 → 수수료 반영 PnL로 ①손절(-1.5%) ②HWM 갱신 ③트레일링 활성화(+1.5%) ④트레일링 갱신(손익분기 floor) ⑤트레일링 트리거 ⑥익절(+3%) ⑦시간청산(360분+손익분기). 청산 시 틱 종료.
4. **신호 생성** — `SignalService.generateSignal()`: 지표+다이버전스로 8컴포넌트 점수 합산 → `determineSignalType()`. 모든 신호 DB 저장.
5. **우선순위 분기** — `|score|≥60`(하드코딩) & HOLD 아니면 리밸런싱 건너뛰고 신호 직행 / 아니면 `RebalanceService.checkAndExecute()`(쿨다운 480분, 편차 10%p) 우선 / 실행 안 되면 신호 매매.
6. **매매 실행** — 쿨다운(30분) 체크 → BUY: `openPositions<2`이면 `executeBuy()`(서킷브레이커→물타기차단→노출상한→ATR 비율→시장가매수→체결가 재시도→`Trade+Position` 원자저장) / SELL: OPEN 포지션 순회(최소보유 30분, 수익률 게이트).
7. **주문 실행** — `BithumbApiClient.placeMarketBuyOrder()`: **LIVE면 실주문, 아니면 인메모리 시뮬레이션**. 체결가는 `trades` 가중평균 → 실패 시 `getOrder()` 3회 재조회 → **현재가 폴백**.
8. **기록** — `TradeEvent`(DB), `lastTradeTime` 갱신, 매도 시 서킷브레이커 결과 반영.

**보조 스케줄러:** 캔들 동기화(5분), 캔들 정리(자정, **enabled 가드 없음**), 계좌 스냅샷(5분 — 서킷브레이커 `dayStartEquity` 기준), 일일 요약(00:01).

---

## 3. 핵심 문제 요약

### 🔴 P0 — 실거래 손실·중복주문·보안 노출

#### P0-1. 실주문·봇제어 REST API 전체가 무인증으로 노출
- **문제:** 봇 시작/중지/수동매수/수동매도/긴급청산/리밸런스강제/**실계좌 테스트주문**이 전부 인증 없이 외부에서 호출 가능.
- **위치:** `common/.../SecurityConfig.java:24`(`/api/**` permitAll), `:72`(CSRF 비활성), `:20`(CORS 허용) + `BotControlApiController.java:39~118`, `RebalanceApiController.java:55`, `TradingVerificationApiController.java:197`.
- **근거:** `SecurityConfig`에 `.requestMatchers("/api/**").permitAll()` + `.ignoringRequestMatchers("/api/**")`(CSRF) + `.cors(withDefaults())`. 봇 제어 컨트롤러가 모두 `/api/trading/**` 아래. `TradingVerificationApiController.java:191`엔 주석 *"테스트 주문 실행 (실제 돈 사용!)"*, `:257`에서 `placeMarketBuyOrder(amount)` 실행. 서버에 HTTP 접근 가능한 누구나 `curl`로 실계좌 주문 가능. CORS는 브라우저 교차출처만 통제하며 직접 HTTP 호출은 막지 못함.
- **영향:** 계좌 탈취 없이도 외부에서 강제 매수/매도/전량청산 → 직접 자금 손실. `apps-in-toss` 미니앱용 `/api/**` 공개(ADR common/security/0002)가 **의도치 않게 트레이딩 봇 제어까지 포함** — 어떤 ADR에도 승인 기록 없음.
- **개선 방향:** `/api/trading/**`를 별도 매처로 분리해 `ROLE_ADMIN` 요구 + CSRF 활성 + CORS 대상 제외. 검증용 `test-order`는 운영 프로파일에서 비활성.
- **수정 난이도:** 하.

#### P0-2. 주문 API 타임아웃/예외를 null로 삼켜 "미체결" 오인 → 중복 주문 가능
- **문제:** 거래소에 접수된 주문이 타임아웃/네트워크 오류로 `null` 반환되면 호출부가 "실패=미체결"로 판단, 다음 틱 재매수하거나 정합화 없이 진행.
- **위치:** `BithumbPrivateApi.java:212~243`(`executeWithRetry`), `:204`(30초 타임아웃) → 호출부 `TradingBotService.java:385~388`, `RiskManagementService.java:208~213`.
- **근거:** 429면 **동일 POST /v1/orders 바디를 그대로 재전송**(213~231행), 그 외 예외(타임아웃 포함)는 `catch(Exception)`→`return null`(236~238행). Bithumb v1의 클라이언트 주문식별자(`identifier`)를 파라미터에 넣지 않음(158~185행). 검증 확인: `getOrder`는 체결가 추출용으로만 쓰이고 타임아웃 후 접수 여부 정합화 코드는 주요 매매 경로에 없음.
- **영향:** ①중복 시장가 주문 ②접수됐으나 null 처리 → 실잔고엔 반영, `Position` 미기록 → 미추적 코인/재매도. 저빈도지만 발생 시 손실 직결.
- **개선 방향:** 주문마다 멱등키(`identifier`) → 실패 시 `getOrders(identifier)`로 접수 여부 조회 후 조건부 재시도. 타임아웃과 429 구분(무조건 재전송 금지). 다음 틱 진입 전 미결 주문 정합화.
- **수정 난이도:** 상.

#### P0-3. `enabled=false`(킬스위치)가 수동 주문 API를 막지 못함
- **문제:** 봇이 꺼져 있어도(`running=false`/`enabled=false`) 수동 매수·매도·긴급청산 API가 실주문.
- **위치:** `TradingBotService.java:629`(`manualBuy`), `:672`(`manualSell`), `:706`(`emergencyClose`).
- **근거:** 세 메서드에 `running`/`enabled` 검사 없음. `manualBuy`는 서킷브레이커·물타기차단·노출상한도 없이 바로 `placeMarketBuyOrder`(635행). 유일한 안전장치는 모드 게이트뿐인데 **기본값 LIVE**(`application.yaml:133`).
- **영향:** P0-1과 결합 시 "정지된 봇"조차 외부 HTTP로 강제 실거래. 킬스위치 안전 보장 미성립.
- **개선 방향:** 수동 주문 계열에 전역 실거래 가드(`trading-armed` 플래그) + 인증. 킬스위치를 API 계층에서 강제.
- **수정 난이도:** 하.

### 🟠 P1 — 운영 안정성

#### P1-1. 체결 기록 실패 시 상태 불일치 (돈은 나갔는데 포지션 없음 / 판 코인 재매도)
- **위치:** 매수 `TradingBotService.java:399~404`·`:444~448`, 매도 `:568~574`.
- **근거:** 매수는 `entryPrice==null`이면 저장 없이 return(주석도 "체결됐을 수 있음" 인정), 저장 예외는 catch. 매도는 체결가 미확보 시 `Position`을 OPEN으로 둔 채 return → 다음 틱에 이미 판 코인 재매도. 청산 실패는 `Position.shouldRetryClose()` 5분/30분 백오프로 재시도되나 매수측 미기록은 복구 경로 없음.
- **영향:** 미추적 포지션(손절 무방비), 이중 매도, 회계 붕괴. **개선:** "주문 접수됨" 사실을 먼저 영속화(멱등키), 체결가는 상태 불명으로 기록 후 정합화. **난이도:** 중.

#### P1-2. 캔들 동결 — 미완성 봉이 확정봉으로 영구 고정될 수 있음
- **위치:** `CandleService.java:67`(중복 시 기존값 유지), `:77`(존재하면 스킵).
- **근거:** 매분 :05 fetch 시점 최신 봉이 형성 중이면 그대로 저장되고 갱신 안 됨. **추정:** 실제 동결 여부는 "Bithumb index 0이 형성봉인가"에 달려 있고 이는 ADR strategy/0009가 "미확정"으로 명시. 동결 로직(기존값 유지) 자체는 코드로 확정.
- **영향:** 모든 지표가 오염된 시계열로 계산 → 잘못된 신호. **개선:** 최신 1~2개 봉 upsert 갱신 또는 직전 확정봉까지만 저장. **난이도:** 중.

#### P1-3. 앱 재시작 시 봇 정지 복귀 → 열린 포지션 손절 보호 공백
- **위치:** `TradingBotService.java:45~48`·`:167~170`.
- **근거:** `running`이 인메모리 → 재시작 후 false. 수동 start 전까지 리스크 루프 미작동. **영향:** 배포·크래시 재시작 사이 포지션 무방비, 급락 시 손절 미작동. **개선:** `running` 영속화 후 부팅 복원, 또는 OPEN 포지션 있으면 리스크 루프 항상 실행. **난이도:** 중.

#### P1-4. 수동/검증 주문이 리스크 가드를 우회
- **위치:** `TradingBotService.java:629~667`, `TradingVerificationApiController.java:197~257`.
- **근거:** `manualBuy`는 잔고 확인만 하고 `executeBuy`의 가드(326~365행)를 재사용 안 함. "수동 매수는 서킷브레이커 미차단"은 ADR risk/0001의 의도이나, **노출상한·물타기차단까지 함께 우회**되는 점 + 무인증 노출(P0-1) 결합은 승인 범위 초과. **개선:** 수동 경로도 공용 가드 통과(관리자 override는 명시적으로). **난이도:** 중.

#### P1-5. 리스크 청산 트리거 로직 테스트 0건 + Clock 미주입
- **위치:** `RiskManagementService.java:106~190`; 시간 의존 `TradingBotService.java:259·723·806`.
- **근거:** `checkPositionRisk`/`closePosition` 통합 테스트 없음. 시간 의존이 `LocalDateTime.now()` 직접 호출(stock 모듈은 `Clock` 빈 주입). **영향:** 자본 보호 핵심 로직이 회귀 안전망 없이 변경됨. **개선:** `Clock` 주입 → 손절/익절/트레일링 경계값 테스트. **난이도:** 중.

#### P1-6. 리밸런스 쿨다운 check-then-act 비원자 → 이중 리밸런스
- **위치:** `RebalanceService.java:142~151·210~217·302`.
- **근거:** 쿨다운 읽기→주문→쓰기 사이 락 없음. 스케줄러 틱과 무인증 `POST /execute` 동시 통과 시 이중 주문. **개선:** 종목별 `ReentrantLock`/CAS로 진입 직렬화. **난이도:** 하.

#### P1-7. 서킷브레이커 연속손실 카운터 인메모리 유실 (일일손실 가드는 DB 기반으로 유지됨)
- **위치:** `TradingCircuitBreaker.java:24`(volatile int)·`:33~40`.
- **근거:** `consecutiveLosses`가 인메모리 필드. **검증 정정:** 일일손실(-5%) 가드는 `realizedPnlToday()`(`TradingBotService.java:722`, DB)·`dayStartEquity()`(736, 스냅샷) 기반으로 **재시작 후에도 유지**. 유실되는 것은 연속손실 스트릭뿐. ADR risk/0001이 이 한계 명시. **개선:** 스트릭도 당일 CLOSED 포지션으로 부팅 시 재계산. **난이도:** 중.

#### P1-8. 운영 모드 기본값 LIVE + 설정 누락/오타 시 실주문 폴백
- **위치:** `TradingProperties.java:76·109·133`.
- **근거:** `mode: ${TRADING_BOT_MODE:LIVE}`, 자바 기본값도 LIVE. "기본 LIVE"는 ADR modes/0001의 수용된 결정이나, 오타·바인딩 실패 시 안전측(PAPER)이 아닌 위험측(LIVE)으로 폴백. **개선:** 안전측 기본 전환 또는 LIVE는 이중 플래그(mode=LIVE AND armed) 동시 충족 시에만. **난이도:** 하(ADR 재검토 수반).

#### P1-9. 주문 응답 검증 부족 + `TradingProperties` 기본값이 yaml과 대폭 불일치
- **위치:** `TradingBotService.java:407·483~488`; `TradingProperties.java:193~196`.
- **근거:** 체결 수량을 응답 `executed_volume`이 아니라 `주문금액÷체결가`로 계산(407행). `state`/`executed_volume`은 `TradingVerificationApiController`에서만 읽고 실매매 경로에선 미검증. `risk` 자바 기본값이 yaml과 3~5배 차이(stopLoss -0.03 vs -0.015, TP 0.15 vs 0.03 등) → yaml 키 하나 누락 시 과거 "죽은 파라미터"로 회귀. **개선:** 응답 `executed_volume`/`state=done` 확인 후 기록; 자바 기본값을 유효값과 일치 또는 필수 키 미설정 시 부팅 실패. **난이도:** 하~중.

### 🟡 P2 — 전략 신뢰도·유지보수성

- **P2-1. 지정가 주문 경로가 모드 게이트 우회** — `BithumbApiClient.java:121~130`: `placeLimitBuy/SellOrder`가 `isLive()` 검사 없이 직접 호출. 현재 미배선이나 배선 시 PAPER에서도 실주문. 난이도: 하.
- **P2-2. 약한 SELL 신호가 수익률 게이트에 종속** — `TradingBotService.java:279~293`. **정정:** 강한 SELL(≤-60/약세 다이버전스)은 -2%까지 매도 가능하므로 "구조적 청산 불가"는 과장. 약한 하락 신호만 -1.5%~+0.1% 구간에서 손절 전 청산 불가. ADR strategy/0003·0004 트레이드오프. 난이도: 중.
- **P2-3. 급락장 감지 서킷 부재** — `TradingCircuitBreaker.java:47~71`: 손실 "확정 후"에만 후행 차단. 선제 중단 장치 없음. 난이도: 중.
- **P2-4. 청산 실패 백오프 동안 손절 체크 자체가 정지** — `RiskManagementService.java:74~83`: 5분/30분 백오프 동안 해당 포지션 손절 판정 미실행. 난이도: 하.
- **P2-5. 운영 알림 채널 부재** — `TradingEventService.java:34~41`: CRITICAL도 DB+로그만(stock은 메일 있음). 난이도: 중.
- **P2-6. `@Transactional` 안 HTTP 잔존(캔들/스냅샷)** — `CandleService.java:44·53~55`, `ProfitService.java:124~135`: API 지연 동안 트랜잭션·커넥션 점유. 난이도: 하.
- **P2-7. 서킷브레이커 `consecutiveLosses++` 비원자** — `TradingCircuitBreaker.java:33~40`: `volatile int`에 read-modify-write. `AtomicInteger`로. 난이도: 하.
- **P2-8. 캔들 동기화 vs 트레이딩 루프 동시 insert race** — `CandleService.java:71~92`: 유니크 제약 위반 예외가 틱 전체(리스크 체크 포함)를 중단시킬 수 있음. upsert/직렬화. 난이도: 하.
- **P2-9. 판단 지점별 기준가 혼용** — 신호는 DB 캔들가(동결 스냅샷), 주문·리스크는 호가 중간값(`BithumbApiClient.java:81`). 기준가 정책 일원화. 난이도: 중.
- **P2-10. 거래소 포트 부재 / 단일 심볼 고정** — application 서비스 5개가 `BithumbApiClient`·DTO 직접 결합, `market` 단일값 가정. `ExchangePort` 추출. 난이도: 상.
- **P2-11. `TradingBotService` 부분 god class + 중복 코드 3중화** — `extractExecutedPrice/extractFee`가 3개 서비스에 복제(동작 상이). 공용 컴포넌트로. 난이도: 중.
- **기타:** 하드코딩 매직넘버(`strongSignalThreshold=60`, `-60`, `minOrderAmount=5000`) 설정 미분리 / 사전 rate-limit 스로틀 부재 / 스토캐스틱 `%D`가 `%K`와 동일 계산(`IndicatorService.java:57~61`) / dev·prod 프로파일 미분리(운영에서도 SQL 로깅·h2-console 노출 가능) / 거래량 다이버전스에 오실레이터 프레임 재사용 + RSI와 동일 ±20 가중.

### 🟢 P3 — 개선하면 좋은 것

- 주석·문서 수치 드리프트(`RiskManagementService.java:125·171` 등). 난이도: 하.
- 스케줄러 계층 테스트 0건(enabled 가드·잡 중복 미검증). 난이도: 하.
- 거래 이벤트 전용 로그 격리 부재(stock은 `stock-events.log` 분리). 난이도: 중.
- 테스트가 리플렉션·null 대량주입 의존(`SignalServiceWeightTest.java:28~38`). 난이도: 하.
- 리밸런스 매수 체결가 폴백이 재시도 없이 사전조회가 사용(`RebalanceService.java:112~120`) → SL/TP 기준선 어긋난 포지션. 난이도: 하.

---

## 4. 매매 전략 관점 개선안

> 수익률 장담 금지. 검증 가능성·리스크 축소 관점.

- **수수료/슬리피지:** 신호 매도 게이트·슬리피지 버퍼(0.5%)엔 반영됨. **리스크 출구(손절/익절/트레일링)는 슬리피지 미반영**이라 신호 출구와 비대칭(ADR strategy/0004 알려진 한계) → 출구 판정에도 일관 적용 검토.
- **손절/익절/트레일링:** -1.5%/+3%/(+1.5% 활성, -0.8% 추적) 1:2 R:R 재보정됨(ADR strategy/0005). **실제 백테스트 검증 기록 없음** → PAPER 백테스트로 트립 빈도·EV 확인 후 확정.
- **포지션 사이징:** ATR 15~35% 동적 비율은 합리적. ATR% 경계(3%/1%) 하드코딩 → 설정화 + 페어별 재튜닝.
- **변동성/거래량 필터:** MA 수렴 억제·거래량 스파이크 확인 존재. 급락장 선제 필터(P2-3) 없음 → 추가.
- **중복 진입 방지:** 물타기 차단·노출상한·최대 2포지션·쿨다운 30분 다층 방어. 단 `Signal.executed` 미사용 + 인메모리 쿨다운(P1-3 재시작 리셋)이라 재시작 직후 중복 위험.
- **과최적화 방지:** 파라미터가 KRW-ADA 튜닝값(ADR 명시). out-of-sample 검증이 코드로 강제되지 않음이 약점.
- **백테스트-실거래 차이:** `Mode.BACKTEST`는 enum뿐, 리플레이 엔진 없음(ADR modes/0001 후속). PAPER는 현재가±슬리피지 인메모리 체결이라 호가 깊이·부분체결 미반영 → 체결 품질 차이.

---

## 5. 시스템 안정성 개선안

- **주문 idempotency:** 멱등키(`identifier`) + 실패 시 조회 기반 정합화 (P0-2 핵심).
- **주문 상태 머신:** `Trade`에 SUBMITTED→FILLED/PARTIAL/UNKNOWN/FAILED + `state=done`/`executed_volume` 검증 (P1-9).
- **dry-run 모드:** PAPER/BACKTEST 게이트 존재(양호). 지정가 게이트 누락 보완(P2-1), BACKTEST 리플레이 엔진 구현.
- **실거래 보호:** `/api/trading/**` 인증(P0-1) + 이중 플래그(mode=LIVE AND armed) + 킬스위치 API 계층 강제(P0-3).
- **rate limit:** 사전 스로틀(토큰버킷), 조회 API에도 429 대응.
- **재시도 정책:** 타임아웃/429 구분, 주문은 멱등키 없이 재전송 금지.
- **알림/모니터링:** CRITICAL 이벤트 푸시(P2-5), 거래 전용 로그 격리(P3).
- **kill switch:** 전역 `trading-armed`로 모든 주문 경로(수동 포함) 차단.
- **스케줄러 중복 방지:** 종목별 락 + 캔들 insert upsert (P1-6, P2-8).
- **장애 복구:** `running`·연속손실 스트릭 영속화, 부팅 시 OPEN 포지션 리스크 루프 자동 가동 (P1-3, P1-7).

---

## 6. 테스트 추가 계획 (우선순위 순 10개)

각 항목: 테스트명 / 목적 / 입력 조건 / 기대 결과 / 관련 파일.

1. **`/api/trading/**` 인증 회귀** / 봇 제어·주문 엔드포인트 무인증 차단 / 미인증 `POST /manual/buy`·`/emergency-close`·`/verify/test-order` / 401·403 거부 / `SecurityConfig`, `BotControlApiController`, `TradingVerificationApiController`.
2. **주문 타임아웃 멱등성** / 타임아웃/null 후 중복 주문 없음 / `placeMarketBuyOrder` 타임아웃 → 다음 틱 재진입 / 멱등키로 재전송 차단 또는 조회 후 조건부 처리 / `BithumbPrivateApi`, `TradingBotService.executeBuy`.
3. **손절 트리거** / PnL ≤ -1.5%(수수료 반영)에서 `STOP_LOSS` 청산 / 진입가·현재가 -1.5% 하회 / `closePosition(STOP_LOSS)` / `RiskManagementService.checkPositionRisk`.
4. **익절/트레일링 트리거** / +3% 익절, +1.5% 활성 후 -0.8% 추적·손익분기 floor / 가격 시퀀스(상승→되돌림) / TAKE_PROFIT/TRAILING_STOP, floor 미하회 / 동일 파일 + `Position`.
5. **잔고 부족 매수 스킵** / KRW<5,000이면 주문 안 함 / `getKrwBalance` 4,000 / 주문 미호출 / `TradingBotService.executeBuy:344`.
6. **매도 체결가 미확보 시 재매도 방지** / 체결가 null이면 재매도 안 함 / 매도 응답 trades 빔 + `getOrder`/현재가 실패 / 상태 불명 처리, 중복 매도 없음 / `TradingBotService.executeSell:568`.
7. **재시작 후 리스크 보호** / OPEN 포지션 있으면 정지 상태여도 손절 작동 / 재시작 시뮬(running=false) + 포지션 -2% / 리스크 루프 청산 / `TradingBotService.executeTradeLoop`.
8. **수동 매수 리스크 가드 통과** / `manualBuy`가 노출상한·물타기차단 우회 안 함 / 손실 포지션 보유 중 `manualBuy` / 가드 적용(또는 명시적 override) / `TradingBotService.manualBuy`.
9. **리밸런스 쿨다운 동시성** / 쿨다운 내 동시 execute가 이중 주문 안 냄 / 병렬 `checkAndExecute` + `POST /execute` / 1회만 실행 / `RebalanceService`.
10. **캔들 동결/신선도** / 형성봉이 확정봉으로 갱신되거나 걸러짐 / 같은 타임스탬프 형성봉→확정봉 순차 fetch / 확정값 반영 또는 스킵 / `CandleService.fetchAndSaveCandles`.

---

## 7. 리팩터링 제안 (안전한 순서)

1. **관측 가능성(동작 변경 없음):** 거래 전용 로그 격리, 주문 요청-응답 상관관계 로그(uuid/identifier), CRITICAL 알림 훅. → 장애 추적성. 파일: `TradingEventService`, `logback-spring.xml`.
2. **테스트 추가:** §6의 1·3·4·5부터, `Clock` 주입. → 회귀 안전망. 파일: 신규 테스트 + 시간 의존부.
3. **주문 안정성:** 멱등키·주문 상태 머신·응답 검증, 타임아웃/429 분리. → P0-2·P1-1·P1-9. 파일: `BithumbPrivateApi`, `TradingBotService`, `Trade`.
4. **리스크 보안:** `/api/trading/**` 인증 분리, 전역 armed 킬스위치, 수동 경로 가드 통일. → P0-1·P0-3·P1-4. 파일: `SecurityConfig`, 컨트롤러, `TradingBotService`.
5. **전략 검증 구조:** `Mode.BACKTEST` 리플레이 엔진, 파라미터 설정화, 재시작 상태 복원. → 검증가능성·복구성. 파일: `BithumbApiClient`/신규 리플레이, `TradingProperties`, 서킷브레이커.
6. **구조 리팩터링:** `ExchangePort` 추출, `TradingBotService` 책임 분해, 체결가/수수료 파서 통합, 멀티심볼 대비. → 확장성. (P2-10·P2-11)

---

## 8. 최종 권고

### 판정: **실거래 금지 (현 상태) → P0 해소 + PAPER 검증 후 "소액 제한적 실거래 가능"**

**근거:**
- **현재 실거래 투입 불가.** 미인증 실주문 API(P0-1)와 실계좌 `test-order` 노출은 서버 접근 순간 직접 자금 탈취 벡터 — 이것만으로 실거래 금지 사유.
- 주문 타임아웃→중복주문(P0-2)과 체결 기록 실패 시 상태 불일치(P1-1)는 저빈도지만 발생 시 손실·회계 붕괴로 직결.
- 자본 보호 핵심(손절/익절/트레일링 트리거)에 **테스트 전무**(P1-5), 재보정 R:R 파라미터가 **백테스트로 검증된 기록 없음**. 전략 +EV가 코드·문서 어디에도 입증되지 않음.
- 재시작 시 봇 정지 복귀로 인한 손절 보호 공백(P1-3)은 무인 운영 전제를 무너뜨림.

**단계적 투입 조건:**
1. **P0 전량 해소**(인증 분리·킬스위치 강제·주문 멱등성) + §6의 1·2·6·7 테스트 통과.
2. **PAPER 모드 실시간 무주문 검증**(수 주) → 신호 빈도·서킷 트립·체결가 폴백·캔들 동결(P1-2) 실측.
3. 이후 **최소 금액 제한적 실거래**로 이행하되 P1-1·P1-5·P1-7·P1-8 병행 해소.

**참고:** LIVE 기본·BACKTEST 미구현·서킷 스트릭 인메모리·리밸런스 maxPositions 우회·물타기 중 피라미딩 허용 등은 **ADR로 문서화된 수용된 트레이드오프**로 "재검토 대상"으로 분류. 반면 **무인증 API 노출(P0-1)은 어떤 ADR에도 승인된 바 없는 신규 결함**으로 최우선 조치 대상.

---

## 부록. 방법론 및 검증 상태

- 8개 관점(신호 로직, 주문 실행, 리스크 관리, 예외/장애, 설정/보안, 테스트/백테스트, 동시성/스케줄러, 유지보수성) 병렬 정적 분석으로 발견 60건 도출.
- 발견별 적대적 검증(evidence 렌즈 = 인용 코드 실재·해석 확인, mitigation 렌즈 = 기존 방어장치 탐색)을 P0/P1에 2중 적용. **2회 실행 합산 73건 검증 완료, 반박 0건.** 정정은 라인번호 정밀화·범위 축소·과장 완화 수준.
- 나머지 검증은 세션 한도로 미완(반박이 아니라 미실행). 해당 발견 중 P0/P1은 코드 1차 확인 완료.
- 본 문서는 특정 코인 매수/매도를 추천하지 않으며 수익률을 보장하지 않는다. 모든 판정은 파일·라인 근거에 기반하며 불확실 항목은 "추정"으로 표기했다.
