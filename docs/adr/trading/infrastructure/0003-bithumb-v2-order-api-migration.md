# ADR-0003 (trading/infrastructure): Bithumb v2 주문 API 마이그레이션 — 어댑터 정규화 + 버전 라우팅

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-08 |
| 도메인 | trading (코인) |
| 관심사 | 인프라 / 외부 API |
| 관련 | 마이그레이션 계획 `docs/trading-bithumb-v2-migration-plan.md`, 운영 감사 `docs/audit/coin-trading-operational-review-2026-07-06.md`, ADR-0002(선영속화+스윕) |

## Context — 무엇이 문제였나

Bithumb 가 2026-06-30 v2 주문 API 를 릴리스했다. 마이그레이션 범위는 **주문 생성
`POST /v2/orders` + 취소 `DELETE /v2/order` 뿐**이고 조회·계좌·캔들·호가는 영구 v1 이다.
v2 는 `client_order_id`(멱등키)를 공식 지원하지만(v1 은 문서상 불확실), **생성 응답에
state/executed_volume/trades 가 없다** — 기존 application 계층은 생성 응답의 trades 로
체결가/수량을 계산하므로 그대로 바꾸면 전 경로가 깨진다.

## Decision — 무엇을 골랐나

1. **어댑터 정규화** — `BithumbV2OrderApi` 가 v2 생성/취소 후 `GET /v1/order` 재조회로
   기존 `BithumbOrderResponse`(trades 포함)를 완성해 반환한다. application 계층은
   v1/v2 를 구분하지 않는다. 재조회는 최대 3회 선형 백오프(기본 300ms·600ms — 접수 직후
   조회 인덱싱 지연 흡수), 전부 실패 시 cid 재조회 → 그래도 없으면 `state=UNKNOWN` 부분
   응답(§8-D — ADR-0002 의 스윕이 수습).
2. **버전 라우팅** — `trading.bithumb.order-api-version` enum(`V1`|`V2`, 기본 **V1**,
   바인딩 실패 시 기동 중단 fail-fast §8-F). 파사드 `BithumbApiClient` 가 생성·취소를
   버전에 따라 privateApi/v2OrderApi 로 위임. 모드 게이트(§8-A)는 라우팅보다 먼저.
3. **재전송 금지 원칙** — 생성 타임아웃/응답유실/HTTP 에러 시 재전송하지 않고
   `client_order_id` 재조회로 접수 여부를 확인(P0-2). **중복 cid 에러도 실패가 아니라
   "주문 존재 가능"으로 취급, 같은 재조회 경로로 기존 주문을 복구**(§8-E — 이중 체결 방지).
   예외적으로 취소의 **422(주문 처리 중)** 는 짧은 백오프 후 **1회만 재시도**(취소는 멱등).
4. **cid 버전 프리픽스** — `newClientOrderId()` 가 `t1-`/`t2-` 프리픽스(+UUID 32자 = 35자)로
   어떤 API 버전 경로에서 생성된 주문인지 cid 만으로 추적(§6 호환성 — 소액 라이브 검증 시 식별).
5. **범위 제외** — `post_only` 는 수수료 레버 아님(maker=taker 0.25%), best+ioc 는 이 봇
   규모에 과잉, 거래소 상주 스탑 주문은 v2 에도 없음(P1-5 는 앱 생존성으로 대체),
   ExchangePort 정식 추출은 보류(파사드 내부 분기로 충분), **지정가 주문의 v2 라우팅 보류**
   (미사용 경로 — v1 유지 + cid 부착만, Phase 0b 잔여 이행).

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| application 계층이 v2 응답 직접 처리 | 체결정보 없는 응답이 전 경로로 번짐 — 침습 큼 |
| v2 즉시 기본값 전환 | 실계약(중복키 에러코드, order_id 의 v1 조회 가능 여부) 미검증 — 소액 라이브 후 전환 |
| 타임아웃 시 재전송 | 응답만 유실된 경우 이중 체결 — 시장가 주문이라 손실 직결 |
| **(선택) 어댑터 정규화 + 기본 V1 라우팅** | application 무변경, 플래그 원복만으로 롤백, 계약 검증 전 운영 불변 |

## Consequences — 영향

- **긍정:** v2 전환이 설정 1줄(`order-api-version=v2`)로 가능, 롤백도 동일. cid 멱등키가
  공식 지원되어 ADR-0002 선영속화+스윕의 전제가 확정된다.
- **비용:** v2 주문 1건당 재조회 1~3회 추가(생성 rate limit ~10 TPS 와 별개인 조회 호출).
- **불확실(소액 라이브 검증 필요 — 계획 §5 Phase 0a/2/3):** v1 생성의 cid 지원 여부,
  중복 cid 실제 에러코드, v2 order_id 가 `GET /v1/order?uuid=` 로 조회되는지, maker/taker
  차등 여부. 정량 게이트: 왕복 각 10건+ · 중복주문 0 · 현재가폴백 0 · 재조회 성공률 ≥95%,
  UNKNOWN 1건이면 V1 원복.

## References

- 코드: `BithumbV2OrderApi`(placeMarket*/cancelOrder/normalize/requeryWithBackoff),
  `BithumbApiClient.isV2()` 라우팅, `TradingProperties.Bithumb.OrderApiVersion`
- 테스트: `BithumbV2OrderApiTest`(MockWebServer — 요청 형식·정규화 백오프·응답유실·중복키·취소),
  `BithumbApiClientIdempotencyTest`(버전 라우팅·모드 게이트)
