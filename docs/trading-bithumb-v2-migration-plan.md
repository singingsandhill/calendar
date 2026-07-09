# Bithumb v2 API 마이그레이션 & 주문 신뢰성 개선 계획 (2026-07-06)

> 대상: `trading` 모듈 — Bithumb 코인 자동매매.
> 성격: **계획·설계 문서.** 구현은 아래 단계별 PR + 검증 게이트로 진행. 이 문서 자체는 코드를 바꾸지 않는다.
> 방법: v2 API 전수 조사(1차 코드/문서 검증) → v2↔감사 연결 → 단계 설계 → 적대적 검토(치명적 전제 오류 교정 반영).
> 배경: [운영 리뷰 2026-07-06](audit/coin-trading-operational-review-2026-07-06.md) **P0-2**(주문 타임아웃→중복주문) 해결을 위해 시작. 조사 결과 완전한 멱등성은 v2 주문 생성이 전제이며, v2 릴리스(2026-06-30 "주문 기능 고도화 + WebSocket Private v2")가 봇 신뢰성을 근본 개선할 기능을 제공함이 확인됨.

---

## 0. 진행 현황 (2026-07-08)

| 항목 | 상태 |
|---|---|
| MockWebServer(okhttp3) 테스트 의존성 | ✅ 추가 |
| §8-A 모드 게이트 커버리지 수리 (PAPER 실계정 취소/지정가 차단) | ✅ TDD 완료 |
| 파사드 멱등 재조회 (`clientOrderIdEnabled` 기본 OFF) — null 응답 시 `client_order_id` 재조회 | ✅ TDD 완료 |
| §8-C executed_volume 실측화 (executeBuy·manualBuy) | ✅ TDD 완료 |
| Phase 1 착수 — `order-api-version` enum(fail-fast, 기본 V1) | ✅ |
| Phase 1 착수 — `BithumbV2OrderApi`(POST /v2/orders 생성 + GET /v1/order 재조회 정규화) | ✅ MockWebServer 테스트(응답유실→재조회 결정적 검증 포함) |
| Phase 1 착수 — 파사드 버전 라우팅(v1/v2, PAPER 안전) | ✅ TDD |
| Phase 1 잔여 — v2 취소(DELETE /v2/order) 라우팅 | ✅ TDD (2026-07-08, 재조회 정규화 동일 패턴) |
| Phase 1 잔여 — 중복키 에러→재조회(§8-E), 정규화 재조회 백오프(3회 선형) | ✅ TDD (2026-07-08) |
| **선영속화 + 틱 스윕 + 스윕의 Position 생성(§8-B)** — Phase 1 로 이관됨 | ✅ TDD (2026-07-08) + E2E(응답유실→스윕→Position). 게이트 = `supportsClientOrderId()`(v2 또는 v1+플래그) — 기본 구성 운영 불변. ADR trading/infrastructure/0002 |
| Phase 1 ADR (trading/infrastructure) | ✅ 0002(§8-B)·0003(v2 마이그레이션) 작성 (2026-07-08) |
| §8-B 매도 확장 — executeSell 선영속화 + 스윕의 포지션 청산 + 재매도 가드 | ✅ TDD (2026-07-08) |
| Phase 1 (4) — 취소 422(처리 중) 1회 재시도 | ✅ TDD (2026-07-08) |
| Phase 0b (1) 잔여 — 지정가 주문 cid 부착(v1, v2 라우팅은 미사용 경로라 보류) | ✅ TDD (2026-07-08) |
| §8-G — 기동 직후 스윕 1회 / §6 — cid 버전 프리픽스(t1-/t2-) | ✅ (2026-07-08) |
| Phase 4 WS | ⬜ Phase 3(전량 전환) 이후 — 게이트가 LIVE 관찰이라 선행 구현 보류 |
| Phase 0a·2·3 라이브 검증·전환 | ⬜ 사용자 실행 |

**시퀀싱 결정 (2026-07-06):** 선영속화+스윕+Position 생성(§8-B)은 **Phase 1(v2 어댑터)과 함께** 구현한다.
근거: 스윕의 조회는 v1/v2 동일(`GET /v1/order?client_order_id=`)이라 코드가 재사용되고, **v2 생성 응답엔
`trades`가 없어 이 스윕이 필수**가 되므로 그곳에서 설계하는 것이 자연스럽다. v1 잔여 갭은 파사드 재조회 +
executed_volume 실측화로 이미 상당 부분 방어됨. → 아래 §5 Phase 0b 의 (2)(3) 항목은 Phase 1 로 이동.

---

## 1. 목표

1. **P0-2 완결** — 주문 타임아웃/응답유실 시 "미체결 오인 → 중복 주문"을 멱등키(`client_order_id`) + 재조회 정합화로 차단.
2. **체결 확인 정확화** — 현재의 "응답 trades 가중평균 → getOrder 3회 → **현재가 폴백**" 사슬을 실측 체결가·수량·수수료로 대체(P1-1, P1-9).
3. **점진·안전** — 라이브 실주문 경로이므로 빅뱅 금지. 설정 플래그로 v1↔v2 전환, 각 단계 롤백 가능, PAPER/소액 라이브 게이트.

**비목표(명시적 제외):** 거래소 상주 손절(§3 참고, v2에도 없음), 다중 거래소 추상화, 전략(신호·수수료 EV) 변경.

---

## 2. v2 API 사실 (조사·1차 검증 완료)

출처: [apidocs.bithumb.com](https://apidocs.bithumb.com) — `주문-요청.md`, `내-주문-및-체결-myorder.md`, `내-자산-myasset.md`, `다건-주문-요청.md`, changelog(2026-06-30), llms.txt 전수 인덱스. 확인 못 한 항목은 "불확실" 표기.

### 2.1 엔드포인트 지형 — **마이그레이션 범위는 "주문 생성·취소" 뿐**

| 분류 | 경로 | 비고 |
|---|---|---|
| 주문 생성 | `POST /v2/orders` | `order_type`, `time_in_force`, `client_order_id` 지원 |
| 다건 주문 | `POST /v2/orders/batch` | 최대 20건, **비원자적** |
| 주문 취소 | `DELETE /v2/order` | `order_id` 또는 `client_order_id` |
| 다건 취소 | `POST /v2/orders/cancel` | 최대 30건 |
| **주문 조회** | `GET /v1/order`, `GET /v1/orders` | **v2 없음 — 영구 v1**. `client_order_id`/`client_order_ids[]`(최대 100) 조회 지원 |
| 계좌·시세·캔들·호가·chance | `GET /v1/*` | **v2 없음 — 영구 v1**. 변경 금지 |

→ 즉 클라이언트 코드에서 바뀌는 것은 **주문 생성 1개 + 취소 1개**뿐. 나머지(`BithumbPublicApi` 캔들/호가, `/v1/accounts`, `/v1/orders/chance`, 조회)는 그대로 둔다.

### 2.2 신규 기능

- **`order_type`**: `limit`(지정가) / `price`(시장가 매수) / `market`(시장가 매도) / **`best`**(최유리 = 상대 1호가 즉시 체결, 원화마켓 한정, `ioc`/`fok` 필수·`post_only` 불가).
- **`time_in_force`**: `ioc`(즉시 가능분만·잔량 취소) / `fok`(전량 아니면 전체 취소) / `post_only`(maker 전용, 즉시 체결가면 자동 취소). `limit`만 3종 자유 조합, 시장가(`price`/`market`)는 tif 불가. tif로 취소되면 `cancel_type=tif_cancel`.
- **`client_order_id`**: 1–36자, `[A-Za-z0-9-_]`, **계정 내 유니크 필수**. 생성·취소·조회·WS 전 구간 관통.
- **Private WebSocket v2** (`wss://ws-api.bithumb.com/websocket/v2/private`, 핸드셰이크 `Authorization: Bearer {JWT}`):
  - **`myOrder`**: 주문 생명주기 `wait→trade→done`/`cancel` 실시간 push. 필드에 `order_id`, `client_order_id`, `state`, `trade_price`, `executed_quantity`, `remaining_quantity`, `paid_fee`, `cancel_type` 포함 → **client_order_id로 내 주문 실시간 매칭**.
  - **`myAsset`**: 잔고 변동 실시간 push.
  - Private WS **v1은 2026-09-30 종료** 확정(현재 코드는 WS 미사용이라 당장 강제는 아님).

### 2.3 인증·스키마 차이 (마이그레이션 함정)

- **인증 동일** — JWT(HS256) + `query_hash`(SHA512). 현행 `BithumbJwtGenerator` **그대로 재사용 가능**. WS는 핸드셰이크에 Bearer JWT(query_hash 불요).
- **요청**: `ord_type`→**`order_type`** 개명, `side`(bid/ask) 유지(REST), 신규 옵션 `time_in_force`·`client_order_id`.
- **응답(중요)**: v2 생성 응답은 `order_id, market, side, order_type, created_at, client_order_id, stp_type, time_in_force`만 반환하며 **v1에 있던 `state`/`executed_volume`/`trades`가 없다**. → **현행 "생성 응답 trades 가중평균" 체결가 추출이 v2에서는 불가**. 체결 확인을 `GET /v1/order` 재조회 또는 WS `myOrder`로 옮겨야 함(이 계획의 핵심 장치).
- **에러**: `400 {error:{name,message}}`. 취소 시 `404`(주문 없음)/`422`(처리 중, 재시도 권장).
- **STP(자기체결방지)**: `stp_type=cancel_taker` 자동.

### 2.4 수수료·레이트리밋

- `GET /v1/orders/chance`에 `bid_fee/ask_fee`(taker) + `maker_bid_fee/maker_ask_fee`(maker) **4필드 분리 존재**. 단 Bithumb 실요율이 maker/taker를 실제 차등하는지는 **문서 미명시(불확실)** — 봇 계정으로 실측 필요. (2026-05-30 감사 결론: maker=taker 0.25%.)
- 주문(생성·취소) **초당 ~10회** 제한(초과 429). 조회/공개는 초당 140/150. 배치 1요청=HTTP 1회지만 내부 건수의 주문-TPS 산입 방식 불확실.

---

## 3. v2가 개선하는 것 / 개선하지 못하는 것 (과잉기대 배제)

### ✅ 실질 개선

| v2 기능 | 개선 발견 | 가치/난이도/리스크 |
|---|---|---|
| **`client_order_id` 멱등키 + 다건 조회** | **P0-2**, P1-1 | 높음 / 중 / 하 — 정공법. null 응답 시 재조회로 접수여부 확정 |
| **WS `myOrder` 실시간 체결** | P0-2, P1-1, **P1-9** | 높음 / 상 / 중 — 감지 창 60초→수 초, `trade_price`/`executed_quantity`/`paid_fee` 실측 |
| **WS `myAsset` 잔고 변동** | P1-1 | 중 / 중 / 하 — 장부-실잔고 드리프트 즉시 경보 |
| `best`+`ioc`/`fok` | P2-9 슬리피지 | 중 / 중 / 중 — 시장가 슬리피지 1호가 캡핑 (**별도 제안으로 분리**, §5 Phase 5) |

### ❌ v2로도 해결되지 않는 것

- **P1-5 (거래소 상주 손절) — 불가능.** `order_type`은 4종뿐, 트리거 가격·`stop_limit`·OCO 없음(llms.txt 전수 확인). TWAP은 시분할 실행이지 조건부 아님. **손절/트레일링은 여전히 앱 폴링 의존 → 앱 다운 시 무방비.** 대체는 API가 아니라 앱 생존성(§9): systemd 자동재기동, WS+워치독, [P1-3](audit/coin-trading-operational-review-2026-07-06.md) 재시작 복원.
- **수수료 EV 문제.** `post_only`는 maker=taker 0.25% 구조에서 수수료를 안 줄임 → 1분봉 타임프레임-수수료 불일치는 **전략 문제로 잔존**. 진짜 레버는 쿠폰(감사 결론 유지).
- **P1-2(캔들 동결), P0-1(무인증 API), P0-3(킬스위치), P1-3/P1-6/P1-7, 백테스트 부재** — 전부 내부 설계 문제로 거래소 API와 무관.
- **WS는 REST 정합화의 대체가 아니라 보강** — 단선 구간 이벤트 유실이 있어 `myOrder`만 믿으면 P0-2가 다른 단일장애점으로 바뀔 뿐.

---

## 4. 목표 아키텍처

**원칙: 파사드(`BithumbApiClient`)를 seam으로 유지하고 그 뒤에서 버전 분기. 전면 `ExchangePort` 추출은 보류.**

1. **`BithumbV2OrderApi` 신설** (`infrastructure/api/`) — `POST /v2/orders` 생성 + `DELETE /v2/order` 취소만 담당하는 얇은 클라이언트. `BithumbJwtGenerator` 재사용. 요청 맵 빌드/응답 매핑(순수 함수)과 HTTP 전송을 메서드 분리해 테스트 용이하게.
2. **응답 정규화(anti-corruption)를 어댑터 내부에서** — v2 생성 응답엔 `trades`가 없으므로, 생성 직후 `GET /v1/order?client_order_id=` 재조회(짧은 폴링)로 기존 `BithumbOrderResponse`(trades 포함)를 채워 반환. **application 계층은 무변경** — 이것이 "빅뱅 금지"의 핵심.
3. **파사드 내부 버전 라우팅** — `trading.bithumb.order-api-version: v1|v2`(기본 v1)를 보고 `BithumbPrivateApi` 또는 `BithumbV2OrderApi`로 위임. **모드 게이트는 라우팅보다 상위**에 유지(단, §8-A 게이트 커버리지 수리 전제).
4. **v1 유지 대상(변경 금지)**: 캔들/호가/체결틱/계좌/chance/**주문 조회**.
5. **`ExchangePort` 정식 추출 보류** — 지금 필요한 다형성은 "같은 거래소 두 엔드포인트"라 파사드 분기로 충분. 다중 거래소 요구 시 별도 ADR.
6. **후속: `BithumbPrivateWsClient`** (`infrastructure/api/ws/`) — Reactor Netty WebSocket, `myOrder`/`myAsset` 구독. **관측·경보 전용 보강 채널**(REST 재조회가 유일 진실원). 신규 의존성 없음.

---

## 5. 단계별 계획 (적대적 검토 반영·교정본)

> 각 단계는 **설정 플래그 원복만으로 롤백**. Phase 0a→0b→1→2→3 순차, Phase 4는 3 이후 독립.

### Phase 0a — 읽기 전용 정찰 (무위험)
- **목표:** v2 결정에 필요한 실측 확보.
- **변경:** (1) 봇 계정으로 `GET /v1/orders/chance` 1회 실측 — `bid_fee`/`ask_fee` vs `maker_bid_fee`/`maker_ask_fee` 4필드 값 확인(→ `post_only` 채택 여부 결정), `min_total` 재확인. (2) 라이브 v1 주문 JSON(생성/조회/취소) fixture 캡처 → `src/test/resources/trading/fixtures/`.
- **게이트:** 실측 수치 확보. 코드 변경 없음.

### Phase 0b — P0-2 멱등화를 v1에서 완결 (침습적 라이브 변경 — 독립 게이트)
- **목표:** v2 전환의 전제인 "선영속화 → 실패 시 조회 정합화"를 v1에서 먼저 완성.
- **변경:** (1) 파사드 시그니처에 `clientOrderId` 통과(`placeMarket*`/`placeLimit*`), `BithumbPrivateApi.placeLimitOrder`에도 `client_order_id` 부착. (2) `TradingBotService`: 주문 전 `Trade`를 `SUBMITTED`+`client_order_id`로 **선영속화**, 응답 null 시 `getOrderByClientOrderId` 재조회, **틱 진입 전 미결(SUBMITTED) 주문을 `GET /v1/orders?client_order_ids[]=`로 일괄 스윕**. (3) **스윕 의무(§8-B)**: 체결 확인된 매수는 Trade 정합화뿐 아니라 **SL/TP 포함 Position 생성**까지. (4) `clientOrderIdEnabled` 소액 검증 후 기본 ON.
- **선행조건:** §8-A(모드 게이트 수리), **MockWebServer 도입**(§7·§9 결정).
- **게이트:** MockWebServer로 "타임아웃→null→재조회→스윕 수습→Position 생성" 전 경로 결정적 테스트 그린 **+** 소액 라이브 왕복 1회(정상 경로 실증). 실패 시 플래그 OFF 원복.

> 참고: 현재 워킹트리에 이 Phase의 down-payment가 이미 있음 — `clientOrderIdEnabled` 플래그, `BithumbPrivateApi` 3-인자 오버로드, `getOrderByClientOrderId`. Phase 0b가 이를 흡수(선영속화·스윕·지정가 부착·Position 생성 추가).

### Phase 1 — `BithumbV2OrderApi` 어댑터 (기본 OFF)
- **목표:** v2 생성/취소 + 응답 정규화를 코드로 완성하되 운영은 v1 유지.
- **변경:** (1) `BithumbV2OrderApi` 신설: `POST /v2/orders`(`order_type`=limit/price/market, `client_order_id` 항상 부착), `DELETE /v2/order`. 429 백오프 이식하되 **주문 생성은 타임아웃 시 재전송 금지**(멱등 조회로 대체). (2) 정규화: 생성 응답 후 `GET /v1/order` 재조회 1~3회로 `BithumbOrderResponse` 완성; 전부 실패 시 `state=UNKNOWN` 반환(§8-D) → Phase 0b 스윕이 수습. (3) `orderApiVersion`(기본 v1, **enum/@Validated fail-fast** §8-F) 추가 + 파사드 라우팅. (4) `422`(처리 중) 취소 1회 재시도. (5) **중복 `client_order_id` 에러는 실패가 아니라 "주문 존재 가능" → 즉시 재조회로 분기**(§8-E). (6) ADR 신규(trading/infrastructure).
- **게이트:** `orderApiVersion=v1` 기본이라 운영 무영향. 전체 테스트 그린 + 1·2층 테스트 커버(§7).

### Phase 2 — v2 소액 라이브 검증
- **목표:** PAPER는 v2 HTTP를 안 타므로 소액 실주문으로 v2 계약 실증.
- **변경:** 코드 무변경. `orderApiVersion=v2` + 수동 트리거로 최소주문 규모 실행. 검증: v2 생성 응답 파싱, 재조회 정규화로 trades 확보, `client_order_id` 취소, `422` 재시도. 응답 원문 재수확→fixture 갱신.
- **게이트:** 소액 매수→체결확인→매도 왕복 1회 + 취소 1회 성공. 실패 시 `orderApiVersion=v1` 즉시 원복.

### Phase 3 — v2 전량 전환 + 체결확인 체인 정리 (**가장 위험 §8**)
- **목표:** 운영 기본을 v2로, v1 주문 경로는 롤백용으로만 유지.
- **변경:** (1) 기본 `orderApiVersion=v2`. (2) 체결확인 체인 정돈: 정규화 응답 trades(1차) → `client_order_id` 재조회(2차) → 현재가 폴백(최후, **경보 동반**). **어댑터가 재조회를 소유**하므로 서비스 계층 `extractExecutedPriceWithRetry` 자체 폴링 제거(§8 폴링 중복 제거). (3) **Position `volume`을 주문금액/체결가 유도값이 아니라 재조회 `executed_volume` 실측으로 대체**(§8 드리프트 근원 수리). (4) `CLAUDE.md`(trading) 갱신 + Phase 1 ADR Status 확정.
- **게이트(정량):** 라이브 왕복 **각 10건 이상** + 중복주문 **0** + 현재가 폴백 **0회** + 재조회 1회내 성공률 **≥95%**. `UNKNOWN` 1건 발생 시 v1 원복 후 원인 규명.

### Phase 4 — WS Private v2 (관측·경보 전용)
- **목표:** 체결 감지 push화(60초→수 초), `myAsset` 드리프트 즉시 경보. **REST 재조회가 유일 진실원 — WS는 승격하지 않음**(자기모순 제거).
- **변경:** (1) `BithumbPrivateWsClient`(Reactor Netty, Bearer JWT). 구독 `[{ticket},{type:myOrder,codes:[KRW-ADA]},{type:myAsset}]`. (2) 수신 이벤트를 인메모리 캐시에 적재 + `myAsset` 변동 시 Position 대조 → 드리프트면 `TradingEvent` WARN. (3) 재연결 지수 백오프(WS 연결 IP당 초당 10회 준수), 단선 후 REST 스윕 1회로 갭 복구. (4) **WS 계층 장애가 주문 경로를 절대 막지 않도록 격리**(try-catch, 캐시 miss→REST). (5) ADR 신규(WS 채널).
- **게이트:** `ws-enabled=true` + **LIVE 주문 미개입 관찰**(PAPER는 실주문 없어 `myOrder` 미발생) 2주 — WS vs REST 불일치율·단선 빈도 측정. WS 장애 격리 확인.

### Phase 5 — best+ioc — **본 문서에서 제외 → 별도 제안서**
- 단일 페어·소액 규모에서 1호가 캡핑의 슬리피지 절감은 미미한 반면 "ioc 잔량 취소·부분체결 회계"라는 신규 실패 모드를 진입 경로에 주입. 지배 비용은 수수료지 슬리피지 아님. **4주 슬리피지 실측 후 별도 제안**으로 분리.

---

## 6. 호환성·롤백 전략

- **전환 플래그:** `trading.bithumb.order-api-version: v1|v2`(기본 v1). 분기는 파사드 내부 단 한 곳. application 서비스는 시그니처·반환타입(`BithumbOrderResponse`) 불변.
- **v1 경로 영구 유지** — 삭제 금지. 조회·계좌·chance·캔들·호가는 v2가 없어 영구 v1이기도.
- **PAPER 유지** — 모드 게이트가 라우팅보다 상위(§8-A 수리 전제). PAPER/BACKTEST는 v1/v2 HTTP 미접촉. 단 **PAPER는 v2 계약을 검증 못 함** → 검증은 소액 라이브 게이트가 담당(명시적 한계).
- **`client_order_id` 버전 프리픽스**(예: `t-v2-{epoch}-{seq}`, 36자 준수)로 추적. 조회는 v1/v2 생성분 모두 `GET /v1/order?client_order_id=`로 단일화.
- **롤백:** Phase 2~3 `orderApiVersion=v1` 원복+재시작(배포 불필요). Phase 4 `ws-enabled=false`. 각 단계 롤백 단위 독립.
- **역방향 리스크:** REST `POST /v1/orders`가 문서 인덱스에서 제거됨(종료일 미공지·불확실). v1 경로가 예고 없이 4xx를 반환하기 시작하면 플래그를 v2로 올리는 역롤백이 되도록 Phase 1 완료를 우선.

---

## 7. 테스트 전략 (MockWebServer 부재 대응)

- **1층 — 순수 함수 단위테스트(신규 코드 주 방어선):** `BithumbV2OrderApi`의 (a) 요청 맵 빌드(order_type/side/tif/client_order_id 조합 규칙: best는 ioc/fok 필수, 시장가는 tif 금지), (b) 응답 JSON→DTO, (c) 에러 스키마 파싱을 HTTP 전송과 분리해 Jackson + fixture(문서 예시 + Phase 0a 캡처)로 검증. `query_hash`는 고정입력→SHA512 스냅샷. WS 프레임 파서도 fixture 기반.
- **2층 — 오케스트레이션 Mockito(기존 패턴 확장):** `BithumbApiClientModeTest`/`TradingBotServicePersistenceTest` 스타일로 버전 라우팅(v1→privateApi·v2→v2Api·PAPER→둘 다 미호출), 정규화 재조회 백오프, 선영속화→스윕 상태 전이, `422` 재시도, WS 캐시 hit/miss→REST 폴백. 시간 의존은 `Clock` 주입.
- **3층 — 계약 검증은 소액 라이브 게이트로:** 실제 직렬화·인증 헤더 수용·실서버 스키마는 mock 불가 → Phase 0a/2 소액 실주문을 공식 검증 절차로, 응답 원문을 fixture로 재수확해 1층을 실데이터로 갱신하는 루프.
- **MockWebServer(okhttp3) 도입 권고 → 결정 필요(§9):** 타임아웃→재조회 정합화(P0-2 핵심 경로)는 소액 라이브로 재현 불가 → 이 결정적 테스트를 위해 `testImplementation 'com.squareup.okhttp3:mockwebserver'` 추가. **Phase 0b 게이트의 전제**로 승격 권고.

---

## 8. 핵심 안전 요구사항 (적대적 검토가 발견한 필수 항목)

- **A. 모드 게이트 커버리지 수리 (Phase 1 선행 필수):** 현재 `BithumbApiClient`의 모드 게이트는 **시장가 매수/매도 2개에만** 있다(`:136-154`). `placeLimitBuyOrder/SellOrder`(`:121-129`), `cancelOrder`(`:239`), `getPendingOrders`/`cancelAllPendingOrders`(`:246`)는 게이트 없이 privateApi 직행이며, **`cancelAllPendingOrders`는 `RiskManagementService:436`(emergencyClose)에서 실제 호출**된다. 이 수리 없이 `orderApiVersion=v2`+PAPER로 기동하면 **PAPER가 실계정에 `DELETE /v2/order`를 날린다.** 취소는 PAPER에서 no-op+로그, 지정가는 simulate로 게이트 확장.
- **B. 스윕은 Position(SL/TP)까지 생성:** `SUBMITTED` 스윕이 체결 확인된 매수에 대해 Trade 정합화 + **SL/TP 포함 Position 생성**을 의무화 → "체결됐으나 Position 없는 무보호 창" 제거. `UNKNOWN` 발견 틱에서는 신규 매수 차단(서킷브레이커 연동).
- **C. 체결량 실측화:** Position `volume`을 `주문금액/체결가` 유도값(`TradingBotService:407`)이 아니라 재조회 `executed_volume` 실측으로 → 장부-실잔고 드리프트 근원 수리.
- **D. `UNKNOWN` 상태 격리:** 합성값 `state=UNKNOWN`이 기존 `done/wait/cancel` 파싱 분기에 오인 유입되지 않도록 Phase 1 단위테스트로 고정.
- **E. 중복 키 에러 의미론:** 429 재전송 후 "중복 `client_order_id`" 에러는 실패가 아니라 "주문 존재 가능" → 즉시 `getOrderByClientOrderId` 재조회로 분기. 정확한 에러 코드는 Phase 2에서 실측·fixture화.
- **F. 설정 fail-fast:** `orderApiVersion` enum 바인딩/@Validated로 기동 시 검증(오타값이 조용히 기본 동작으로 빠지지 않게).
- **G. 재시작 절차:** 롤백/전환 시 — 신규 매수 차단 → 미결 주문 취소·스윕 정합 확인 → 재시작 → 기동 직후 `client_order_ids` 스윕 1회. (재시작 중 손절 감시 공백 최소화.)

**가장 위험한 변경(Phase 3):** 체결확인 진실원이 "생성 응답"에서 "재조회 정규화"로 이동. 재조회가 조용히 열화되면(조회 API 지연·`wait` 지속) 매수는 체결됐는데 장부는 `UNKNOWN` → 손절 없는 유령 포지션 시스템적 양산 위험 = P0-2를 고치려다 더 넓은 단일장애점(조회 API) 생성. 완화: B(Position 생성 의무) + 경보 0회 게이트 + `UNKNOWN` 시 진입 차단 + MockWebServer 결정적 테스트 + 정량 롤백 트리거(재조회 성공률 <95% or `UNKNOWN` 1건 → v1 원복).

---

## 9. 결정 필요 사항 (사용자)

1. **MockWebServer(okhttp3) 테스트 의존성 추가** — 타임아웃→재조회 경로를 결정적으로 재현할 유일 수단. **권고: 추가**(Phase 0b 전제).
2. **타임라인 공격성** — (a) 보수: Phase 0~3만 이번 분기, WS는 다음. (b) 표준: Phase 0~4, 8~10주. (c) 공격: 일괄. REST v1 종료일 미공지라 외부 강제 시한 없음. **권고: (a) 또는 (b).**
3. **`post_only` 채택** — Phase 0a `orders/chance` 실측에서 `maker_bid_fee < bid_fee` 차등 확인 시에만 리밸런스 매도 한정 재검토. 차등 없으면 영구 불채택.
4. **Phase 5(best+ioc)** — 본 문서 제외, 4주 슬리피지 실측 후 별도 제안. 진입만 적용 권고(손절은 시장가 유지).
5. **P1-5(상주 손절) 대체 우선순위** — v2로 불가. systemd/컨테이너 자동재기동 / 프로세스 워치독 / 사전 지정가 매도 상주(트레일링 포기 트레이드오프) 중 무엇을 먼저.
6. **P0-1(무인증 API)은 이미 수정 완료** — 이 마이그레이션의 라이브 게이트(수동 트리거)는 관리자 인증 하에서만 접근 가능해야 하므로 P0-1이 선행 조건. (완료됨: [ADR 0003](adr/common/security/0003-admin-only-trading-control-api.md).)

---

## 10. 진행 중 코드 처리

현재 워킹트리의 v1 슬라이스(미커밋):
- `TradingProperties.Bithumb.clientOrderIdEnabled` — **유지**(Phase 0b에서 사용).
- `BithumbPrivateApi.placeMarketBuy/SellOrder` 3-인자 오버로드 + `getOrderByClientOrderId` — **유지**(Phase 0b·1에서 재사용; 조회는 v1/v2 공통).
- `BithumbApiClientIdempotencyTest`(작성 중이던 것) — Phase 0b 테스트로 완성.

→ 되돌릴 것 없음. Phase 0b가 이 위에 선영속화·틱 스윕·지정가 부착·Position 생성·모드 게이트 수리를 얹는다.

---

## 부록. 조사 신뢰도

- 확실(high): `client_order_id` 존재·제약·조회 경로, v2 order_type/tif 값, WS `myOrder`/`myAsset` 존재·필드, 스탑 주문 부재, 인증 스킴 동일.
- 불확실(재확인 필요): v1 `POST /v1/orders` 정확한 종료일, 동일 `client_order_id` 중복 시 에러 코드, Bithumb 실 maker/taker 요율 차등 여부, 배치 내 주문의 TPS 산입 방식, WS `codes` 생략 동작. — Phase 0a 실측 + Phase 2 소액 라이브에서 확정.
- 본 문서는 특정 코인 매수/매도를 추천하지 않으며 수익률을 보장하지 않는다.
