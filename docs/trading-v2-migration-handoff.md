# 코인 트레이딩 P0-2 / Bithumb v2 마이그레이션 — 세션 인계 문서

> **이 문서만 읽고 바로 이어서 작업할 수 있도록 작성됨.** 다음 세션은 이 문서 + 필요 시
> [`trading-bithumb-v2-migration-plan.md`](trading-bithumb-v2-migration-plan.md)(전체 계획·근거)만 읽으면 된다.
> 마지막 갱신: 2026-07-08.

---

## 0. 30초 요약 — 지금 어디까지 왔고 다음은 무엇인가

- **완료·검증됨 (2026-07-06):** P0-1 무인증 API 차단 / P0-2 v1 멱등 재조회(기본 OFF) / §8-A 모드게이트 /
  §8-C 실체결량 / **Phase 1 착수** = `BithumbV2OrderApi`(POST /v2/orders + 정규화) + 버전 라우팅(기본 V1).
- **완료·검증됨 (2026-07-08, 이번 세션):** **§8-B 선영속화 + 틱 스윕 + Position 생성**(§6 설계 그대로,
  TDD + E2E) / v2 취소(DELETE /v2/order) 라우팅 / 정규화 재조회 3회 선형 백오프 / §8-E 중복키 에러→재조회 /
  ADR trading/infrastructure/**0002**(§8-B)·**0003**(v2 마이그레이션) + CLAUDE.md 동기화. 전체 스위트 GREEN(4m1s).
  게이트: 선영속화는 `supportsClientOrderId()`(v2 또는 v1+`clientOrderIdEnabled`)에서만 — **기본 구성(V1+OFF)
  운영 동작 불변.** 스윕 자체는 무조건 실행(잔여 SUBMITTED 수습).
- **완료·검증됨 (2026-07-08 후속, 잔여 마감):** **§8-B 매도 확장** — `executeSell` 선영속화(SUBMITTED SELL,
  positionId 연결) + 스윕이 매도 체결 확인 시 **연결 포지션 청산**(이미 닫혔으면 Trade 만, 이중 청산 금지) +
  서킷브레이커 집계 + 같은 포지션 미해결 매도 시 재매도 차단 / **취소 422 1회 재시도**(Phase 1 (4)) /
  **지정가 cid 부착**(Phase 0b (1) — v2 라우팅은 미사용 경로라 보류) / **기동 직후 스윕 1회**(§8-G) /
  **cid 버전 프리픽스 `t1-`/`t2-`**(§6 추적). ADR 0002·0003 보강. 전체 스위트 GREEN(2m16s).
- **다음 작업(우선순위):**
  1. (사용자 실행) Phase 0a/2/3 소액 라이브 검증 → `order-api-version=v2` 전환 (§8 롤아웃 게이트).
  2. Phase 4 Private WS v2 — **Phase 3(전량 전환) 이후** 착수 (게이트가 LIVE 관찰이라 선행 구현 보류, 계획 §5).
- **커밋 상태:** 2026-07-06 분은 `docs/git_commit.md` **Section T**, 2026-07-08 분은 **Section U**(§8-B 본체)와
  **Section V**(잔여 마감 — 매도 확장·422·지정가 cid·기동 스윕·프리픽스)에 관심사별로 정리됨(미커밋).
  직접 `git commit` 금지(저장소 관례). 워킹트리에 선재 미커밋 변경도 섞여 있으니 파일별 `git diff` 확인 필요.

---

## 1. 빌드·테스트 환경 (WSL — 반드시 준수)

이 저장소는 WSL 안에서 Windows JDK 로 빌드한다. **WSL `./gradlew` 아니라 `cmd.exe` 로 실행:**

```bash
# 전체 테스트
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"
# 특정 테스트
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *BithumbV2OrderApiTest"
# 전체 빌드
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat build"
```

- 전체 스위트 ~2분, 단일 테스트 ~10초. `--console=plain` 권장.
- 앱 검증(bootRun)은 Windows 프로세스로 뜨므로 localhost:8081 확인 시 `cmd.exe /c curl` 사용(WSL curl 아님).
- 테스트 리포트: `build/test-results/test/TEST-*.xml`, `build/reports/tests/test/index.html`.

---

## 2. 방법론 (이 작업의 규칙)

- **TDD 필수** — 라이브 실주문 경로다. RED(테스트 먼저 실패 확인)→GREEN→회귀(전체 스위트). 프로덕션 코드 전
  실패 테스트. (이번 세션 어댑터·라우팅은 신규 클래스라 impl→test 로 갔으나 강한 어서션으로 대체 — 다음부터 red-first 권장.)
- **안전 우선** — 모든 신규 동작은 설정 플래그 기본 OFF/V1. 운영 동작 불변 유지.
- **문서 동기화 규칙(중요)** — 정책/결정 변경 시 `CLAUDE.md` + 새 ADR 필수(저장소 CLAUDE.md 규칙). v2 전환·스윕
  도입은 결정 변경이라 ADR 필요.

---

## 3. 완료 상태 상세 (검증됨 — 재작업 불필요)

| 영역 | 내용 | 위치 |
|---|---|---|
| P0-1 | `/api/trading/**`·`/trading/**` → ROLE_ADMIN | `common/.../SecurityConfig.java`, ADR common/security/0003 |
| §8-A | 모드 게이트를 취소·지정가·미결조회까지 확장(PAPER 실계정 차단) | `BithumbApiClient` cancelOrder/cancelAllPendingOrders/getPendingOrders/placeLimit* |
| P0-2(v1) | null 응답 시 재전송 대신 `client_order_id` 재조회 (플래그 `clientOrderIdEnabled` 기본 OFF) | `BithumbApiClient.placeMarketBuy/SellOrder`, `reconcileByClientOrderId`, `newClientOrderId` |
| P0-2 조회 | `getOrderByClientOrderId` (GET /v1/order?client_order_id=, v1/v2 공통) | `BithumbPrivateApi` |
| §8-C | Position 수량 = 실체결(trades 합산/executed_volume), 유도값 아님 | `TradingBotService.extractExecutedVolume` + executeBuy·manualBuy |
| Phase1 | `order-api-version` enum(기본 V1, fail-fast) | `TradingProperties.Bithumb.OrderApiVersion` |
| Phase1 | v2 어댑터: POST /v2/orders 생성 + GET /v1/order 재조회 정규화(생성 응답엔 trades 없음) | `BithumbV2OrderApi`, `BithumbV2OrderCreateResponse` |
| Phase1 | 파사드 버전 라우팅(v1→privateApi / v2→v2Api / PAPER→simulate) | `BithumbApiClient.isV2()` + placeMarket* |
| §8-B | 선영속화: executeBuy 가 주문 전 Trade(SUBMITTED, cid) 저장 → 체결 시 같은 Trade DONE 갱신(uuid 를 거래소 값으로 교체) + Position 연결. null/UNKNOWN/예외 시 SUBMITTED 유지 | `TradingBotService.executeBuy`, `Trade.createSubmittedBuy`/`assignExchangeUuid` |
| §8-B | 틱 스윕: grace(10초) 경과 SUBMITTED 를 cid 재조회 — 체결→DONE+Position(SL/TP), 취소→CANCEL, 만료(2분) 미발견→FAILED, wait→유지. 실패해도 리스크 체크 안 막음 | `TradingBotService.reconcileSubmittedOrders`/`confirmSubmittedTrade` (executeTradeLoop 시작부) |
| §8-B | 미해결 SUBMITTED 존재 시 신규 매수 차단 + cid 소유권 서비스로 이동(파사드 오버로드) | `hasUnresolvedSubmitted`, `BithumbApiClient.placeMarketBuyOrder(amount, cid)`/`supportsClientOrderId`/`getOrderByClientOrderId` |
| §8-B | 스키마: `TradeStatus.SUBMITTED`, `Trade.clientOrderId`, `trading_trades.client_order_id`(nullable) | `Trade`/`TradeJpaEntity`/`TradeRepositoryAdapter` |
| Phase1 잔여 | v2 취소 라우팅(DELETE /v2/order + GET /v1/order 재조회 정규화), 정규화 재조회 3회 선형 백오프(기본 300ms, 테스트 setter), §8-E 중복 cid 에러→재조회 복구, **취소 422(처리 중) 1회 재시도** | `BithumbV2OrderApi.cancelOrder`/`requestCancel`/`requeryWithBackoff`, `BithumbApiClient.cancelOrder` |
| §8-B 매도 | executeSell 선영속화(SUBMITTED SELL, positionId 연결) → 스윕이 체결 확인 시 포지션 청산(SIGNAL, 수수료)+서킷브레이커 집계, 이미 닫힌 포지션은 Trade 만(이중 청산 금지), 같은 포지션 미해결 매도 시 재매도 차단 | `TradingBotService.executeSell`/`closeReconciledSellPosition`/`hasUnresolvedSubmittedSell`, `Trade.createSubmittedSell` |
| §8-G/§6 | 기동 직후 스윕 1회(`start()`), cid 버전 프리픽스 `t1-`/`t2-`(35자), 지정가 주문 cid 부착(v1 5-인자 오버로드, v2 라우팅 보류) | `TradingBotService.start`, `BithumbApiClient.newClientOrderId`/`placeLimitOrderWithIdempotency`, `BithumbPrivateApi.placeLimitOrder` |
| ADR | trading/infrastructure/0002(§8-B — 매도 확장 갱신 포함)·0003(v2 마이그레이션 — 422·프리픽스 포함) + `trading/CLAUDE.md`·`docs/adr/README.md` 동기화 | `docs/adr/trading/infrastructure/` |

**테스트(모두 GREEN):** `TradingApiSecurityTest`, `BithumbApiClientModeTest`(§8-A 확장),
`BithumbApiClientIdempotencyTest`(재조회+라우팅+cid 오버로드+취소 라우팅), `BithumbV2OrderApiTest`(MockWebServer —
생성·정규화·응답유실·중복키·백오프·취소), `TradingBotServiceExecutedVolumeTest`,
`TradingBotServiceOrderReconciliationTest`(선영속화·스윕·차단), `TradingV2LostResponseSweepTest`(E2E).

---

## 4. 핵심 사실·결정 (재조사 불필요 — 근거는 계획 문서 §2)

**Bithumb v2 API (2026-06-30 릴리스):**
- 마이그레이션 범위 = **주문 생성 `POST /v2/orders` + 취소 `DELETE /v2/order` 뿐.** 조회·계좌·캔들·호가·chance 는 **영구 v1**.
- `order_type`: limit/price(시장가매수)/market(시장가매도)/best(최유리, ioc·fok 필수). `time_in_force`: post_only/ioc/fok.
- `client_order_id`: 1~36자, `[A-Za-z0-9-_]`, 계정 내 유니크. 생성/취소/조회/WS 관통.
- **v2 생성 응답엔 state/executed_volume/trades 가 없음** → 체결정보는 GET /v1/order 재조회로 정규화(이미 구현).
- **거래소 상주 스탑 주문 없음** → P1-5(앱 다운 시 손절 무방비)는 v2 로도 못 품(앱 생존성으로 대체).
- **Private WS v2** `wss://ws-api.bithumb.com/websocket/v2/private`: `myOrder`(wait→trade→done/cancel, client_order_id 포함),
  `myAsset`. Phase 4 에서 관측·경보 전용으로 채택(REST 재조회가 유일 진실원 — WS 승격 금지).
- 인증: v1/v2 동일 JWT(HS256)+query_hash(SHA512). `BithumbJwtGenerator` 재사용.
- 주문 생성/취소 rate limit ~10 TPS.

**결정(수정 금지):**
- 스윕(§8-B)은 **Phase 1(v2)과 함께** 구현(사용자 결정 2026-07-06). 조회가 v1/v2 공통이라 스윕 코드는 재사용.
- `post_only` 는 수수료 레버 아님(maker=taker 0.25%) — 채택 보류.
- best+ioc(원 Phase 5)는 이 봇 규모엔 과잉 — 마이그레이션에서 제외, 별도 제안.
- ExchangePort 정식 추출 보류 — 파사드 내부 분기로 충분.

**불확실(라이브 검증 필요 — Phase 0a/2):** v1 생성의 client_order_id 지원 여부, 동일 키 중복 에러코드,
Bithumb 실 maker/taker 차등 여부, v2 order_id 가 GET /v1/order?uuid= 로 조회되는지.

---

## 5. 파일 지도 (이번 세션이 만지거나 만든 것)

**신규(온전히 이번 세션 소유):**
- `.../trading/infrastructure/api/BithumbV2OrderApi.java` — v2 주문 어댑터
- `.../trading/infrastructure/api/dto/BithumbV2OrderCreateResponse.java` — v2 생성 응답 DTO
- 테스트: `BithumbApiClientIdempotencyTest`, `BithumbV2OrderApiTest`, `TradingBotServiceExecutedVolumeTest`, `TradingApiSecurityTest`
- 문서: `docs/audit/coin-trading-operational-review-2026-07-06.md`, `docs/trading-bithumb-v2-migration-plan.md`,
  `docs/troubleshooting/spring-security-webmvctest.md`, ADR `docs/adr/common/security/0003-*.md`, 이 인계 문서

**수정(선재 변경과 섞였을 수 있음 — `git diff` 확인):**
- `.../common/infrastructure/config/SecurityConfig.java` — P0-1 매처
- `.../trading/infrastructure/config/TradingProperties.java` — `clientOrderIdEnabled`, `orderApiVersion` enum
- `.../trading/infrastructure/api/BithumbApiClient.java` — 모드게이트·재조회·라우팅·생성자(+v2OrderApi)
- `.../trading/infrastructure/api/BithumbPrivateApi.java` — 3-인자 오버로드 + `getOrderByClientOrderId`
- `.../trading/application/service/TradingBotService.java` — `extractExecutedVolume` + executeBuy·manualBuy
- `BithumbApiClientModeTest.java` — §8-A 테스트 + 생성자 갱신
- `CLAUDE.md`, `common/CLAUDE.md`, `docs/adr/README.md`, `docs/troubleshooting/README.md`, `build.gradle`

---

## 6. §8-B 선영속화 + 틱 스윕 + Position 생성 — ✅ 구현 완료 (2026-07-08)

> **이 섹션의 설계는 그대로 구현·검증됐다** (세부: §3 표, ADR trading/infrastructure/0002).
> 설계 대비 확정 사항: 게이트 = `supportsClientOrderId()`(새 플래그 없이 기존 플래그 재사용),
> §6.3-6 의 "신규 매수 금지"는 `hasUnresolvedSubmitted` 저장소 직접 조회(숨은 상태 없음),
> §6.4 스윕은 개별 `getOrderByClientOrderId`(배치 조회 아님). 아래 원문은 설계 기록으로 유지.

**목적:** v2 생성 응답엔 trades 가 없어 "접수는 됐는데 체결정보 재조회 실패(UNKNOWN)" 또는 "응답 유실 후
즉시 재조회도 실패"인 좁은 갭이 남는다. 이때 체결된 매수가 Position(SL/TP) 없이 방치되면 무보호 → 이를
선영속화 + 틱 스윕으로 수습한다. **all-or-nothing**(선영속화만 넣고 스윕 없으면 SUBMITTED 만 쌓임).

### 6.1 스키마 변경
- `TradeStatus`: `SUBMITTED` 추가(주문 전송됨·결과 미확인).
- `Trade`: `clientOrderId`(nullable String) 필드 + 생성자/팩토리 파라미터 + getter. 선영속화용 팩토리
  `Trade.createSubmittedBuy(clientOrderId, market, amount)` 추가 권장.
- `TradeJpaEntity`: `client_order_id` 컬럼(nullable) 추가 + toDomain/fromDomain 매핑. (H2 create-drop 이라 테스트 무영향,
  dev file DB 는 nullable 컬럼 추가라 하위호환.)
- `TradeRepository`: `findByStatus(SUBMITTED)` 이미 존재 — 재사용. 필요 시 `findByMarketAndStatus`.

### 6.2 cid 소유권 이동 (파사드 → 서비스)
- 지금은 파사드가 `newClientOrderId()` 를 내부 생성. 선영속화는 **서비스가 cid 를 알아야** 하므로:
  - `BithumbApiClient` 에 `placeMarketBuyOrder(amount, clientOrderId)` 오버로드 추가(주어진 cid 사용).
    인자 없는 기존 메서드는 내부 생성 유지(하위호환).
  - 서비스(executeBuy)가 cid 생성 → 선영속화 → 오버로드 호출.

### 6.3 executeBuy 흐름(신규)
1. 기존 가드(서킷브레이커·잔고·물타기·노출상한) 통과.
2. 주문액 계산.
3. `cid = newClientOrderId()`; **Trade(SUBMITTED, cid) 선영속화**(짧은 tx).
4. `response = bithumbApiClient.placeMarketBuyOrder(amount, cid)`.
5. **체결 확인(trades 있음)** → Trade DONE(executedPrice/executedVolume/fee) + **Position(SL/TP) 생성**(tx). (기존 로직 재사용)
6. **null 또는 state=UNKNOWN** → Trade 는 SUBMITTED 로 남김 → 틱 스윕이 수습. **이 틱에서 신규 매수 금지**(미정합 상태 진입 차단 — 서킷브레이커 연동 또는 플래그).

### 6.4 틱 스윕 (executeTradeLoop 시작부, 매매 전)
`reconcileSubmittedOrders(market)`:
- `tradeRepository.findByStatus(SUBMITTED)` 중 market 일치·orderedAt 이 grace(예: >10초) 초과분 순회.
- 각 Trade 의 cid 로 `bithumbApiClient.getOrder...`(getOrderByClientOrderId(cid)) 조회:
  - **체결됨(done, trades 존재)** → Trade DONE + BUY 이고 Position 없으면 **Position(SL/TP) 생성**(entry=체결가).
  - **취소됨(cancel)** → Trade CANCEL.
  - **미발견 & orderedAt > 만료(예: >2분)** → Trade FAILED(거래소 미도달로 간주).
  - **아직 wait** → 다음 틱까지 유지.
- 스윕이 미해결 SUBMITTED 를 발견한 틱은 신규 BUY 차단(중복 진입 방지).

### 6.5 테스트(TDD, MockWebServer + Mockito)
- 선영속화: 주문 API 호출 **전에** Trade(SUBMITTED, cid) 저장됨(순서 검증).
- 응답 null → Trade SUBMITTED 로 남고 Position 미생성.
- 스윕: SUBMITTED + getOrderByClientOrderId=done → Trade DONE + Position(SL/TP) 생성.
- 스윕: SUBMITTED + 만료 미발견 → Trade FAILED.
- 스윕 미해결 존재 틱 → executeBuy 신규 매수 차단.
- MockWebServer 로 "생성 응답 유실 → 스윕이 나중에 체결 발견 → Position 생성" 전 경로.

### 6.6 v2 잔여(스윕과 함께 또는 직후)
- v2 취소: `BithumbApiClient.cancelOrder` 를 v2 면 `v2OrderApi.cancelOrder`(DELETE /v2/order, order_id 또는 client_order_id)로 라우팅.
- §8-E 중복키 에러: v2 생성이 "중복 client_order_id" 에러면 실패 아니라 "주문 존재 가능"→즉시 getOrderByClientOrderId 재조회.
- 정규화 재조회 백오프: `BithumbV2OrderApi.normalize` 의 getOrder 를 1회→3회(백오프)로. (테스트 속도 위해 Clock/횟수 주입 고려.)

---

## 7. 테스트 패턴·함정 (이 저장소 특화)

- **MockWebServer 패턴**(`BithumbV2OrderApiTest` 참고): `new MockWebServer()` → `props.getBithumb().setBaseUrl(server.url("/").toString())`
  → `new BithumbV2OrderApi(props, WebClient.builder(), mockJwt, mockPrivateApi)`. `jwt.isConfigured()`→true,
  `generateAuthorizationHeader(any)`→"Bearer x". 응답유실 재현 = `MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)`
  (30초 타임아웃 대기 없이 즉시 연결오류). 요청 검증 = `server.takeRequest()`.
- **`@WebMvcTest` 보안 슬라이스 함정**(`docs/troubleshooting/spring-security-webmvctest.md`): 컨텍스트 로딩 실패 시
  `WebConfig`→`OwnerPathInterceptor`가 `OwnerRepository` 요구 → `@MockitoBean OwnerRepository` 추가. 인증은 `@WithMockUser`
  대신 `.with(user("x").roles("ADMIN"))` post-processor. 미인증=302, 비권한=403 구분.
- **TradingBotService 목 하네스**(`TradingBotServiceExecutedVolumeTest`/`TradingBotServicePersistenceTest` 참고):
  생성자에 필요한 것만 목, 나머지 null. executeBuy 는 circuitBreaker·accountSnapshotRepo·positionRepo·bithumbApiClient·
  indicatorService·riskManagementService·rebalanceService·tradeRepo·txm 필요. `TransactionTemplate` 은 mock
  `PlatformTransactionManager` 로 콜백이 실제 실행됨(getTransaction→null, 콜백 run, commit(null) no-op).
- **BithumbApiClient 생성자**: 이제 `(publicApi, privateApi, v2OrderApi, tradingProperties)` — 4인자. 직접 생성하는 테스트는 v2Api 목 전달.

---

## 8. 롤아웃 게이트 (계획 §5 요약)

OFF(코드만, v1 기본) → PAPER(오케스트레이션 회귀 확인, v2 HTTP 안 탐) → **소액 라이브**(v2 계약 실증 — PAPER 로 검증
불가한 부분) → 전량(정량 게이트: 왕복 각 10건+ · 중복주문 0 · 현재가폴백 0 · 재조회 성공률 ≥95%, UNKNOWN 1건이면 v1 원복).
`order-api-version=v1|v2`, `clientOrderIdEnabled` 플래그 원복만으로 롤백. Phase 0a/2/3 는 실계좌 필요 → 사용자 실행.

---

## 9. 참조

- 전체 계획·근거: [`trading-bithumb-v2-migration-plan.md`](trading-bithumb-v2-migration-plan.md) (§0 진행현황, §5 단계, §8 안전요구)
- 감사 근거(P0/P1/P2): [`audit/coin-trading-operational-review-2026-07-06.md`](audit/coin-trading-operational-review-2026-07-06.md)
- 커밋 정리: `docs/git_commit.md` Section T
- 보안 테스트 함정: [`troubleshooting/spring-security-webmvctest.md`](troubleshooting/spring-security-webmvctest.md)
- 관련 ADR: common/security/0003(P0-1), trading/modes·risk·infrastructure(기존 트레이딩 결정)
