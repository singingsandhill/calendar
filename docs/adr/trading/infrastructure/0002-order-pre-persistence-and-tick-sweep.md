# ADR-0002 (trading/infrastructure): 주문 선영속화(SUBMITTED) + 틱 스윕 + Position 생성 (§8-B)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-08 |
| 도메인 | trading (코인) |
| 관심사 | 인프라 / 주문 신뢰성 |
| 관련 | 운영 감사 `docs/audit/coin-trading-operational-review-2026-07-06.md` (§8-B), 마이그레이션 계획 `docs/trading-bithumb-v2-migration-plan.md`, ADR-0003(v2 마이그레이션) |

## Context — 무엇이 문제였나

주문 전송 후 응답이 유실(타임아웃/연결 종료)되거나, v2 생성 응답처럼 체결 정보가 없어
재조회마저 실패(`state=UNKNOWN`)하면 좁은 갭이 남는다: **거래소에서는 체결됐는데 DB 에는
아무 기록이 없다.** 특히 매수가 이 갭에 빠지면 Position(SL/TP)이 생성되지 않아 손절/익절
리스크 루프의 보호를 받지 못하는 **무보호 포지션**이 된다. P0-2 의 즉시 재조회는 "주문
직후 1회"만 수습할 뿐, 재조회 자체가 실패하면 영구히 잊힌다.

## Decision — 무엇을 골랐나

**all-or-nothing 3종 세트** (선영속화만 넣으면 SUBMITTED 만 쌓이므로 반드시 함께):

1. **선영속화** — `executeBuy` 가 주문 전송 **전에** `Trade(SUBMITTED, client_order_id)` 를
   저장한다. 응답 정상 체결이면 같은 Trade 를 DONE 으로 갱신(uuid 를 거래소 값으로 교체),
   null/`UNKNOWN`/예외면 SUBMITTED 로 남긴다(FAILED 로 덮지 않음 — 접수 여부 불명).
2. **틱 스윕** — `executeTradeLoop` 시작부의 `reconcileSubmittedOrders(market)` 가
   grace(10초) 를 넘긴 SUBMITTED 를 `GET /v1/order?client_order_id=` 로 재조회:
   체결 확인 → Trade DONE + **매수면 Position(SL/TP) 생성**(무보호 창 제거) /
   취소 확인 → CANCEL / 만료(2분) 후에도 미발견 → FAILED(거래소 미도달 간주) /
   wait·미확보 → 다음 틱 유지. 스윕 실패는 리스크 체크를 막지 않는다(자체 try-catch).
3. **신규 매수 차단** — 미해결 SUBMITTED 가 있는 동안 `executeBuy` 진입 거부
   (미정합 상태에서의 중복 진입 방지).

**매도 확장 (2026-07-08 갱신):** `executeSell`(신호 매도)에도 동일 패턴 적용 —
주문 전 `Trade(SUBMITTED, cid, positionId 연결)` 선영속화, 스윕이 체결 확인 시 **연결
포지션 청산**(`CloseReason.SIGNAL`, 수수료 반영) + 서킷브레이커 집계. 다른 경로(리스크
청산 등)가 이미 닫은 포지션이면 Trade 만 정합화(이중 청산 금지). 같은 포지션에 미해결
SUBMITTED 매도가 있으면 재매도 금지(이중 매도 방지). **기동 스윕(§8-G):** `start()` 직후
스윕 1회 — 재시작 공백 동안의 갭 복구.

**게이트:** 선영속화는 cid 가 실제로 주문에 부착되는 구성
(`supportsClientOrderId()` = v2 **또는** v1+`clientOrderIdEnabled`)에서만. 기본 구성
(V1 + OFF)에서는 기존 동작 그대로 — 새 플래그 없이 기존 플래그가 게이트를 겸한다.
스윕 자체는 무조건 실행(과거 게이트 ON 시절의 잔여 SUBMITTED 도 수습).

**스키마:** `TradeStatus.SUBMITTED` 추가, `Trade.clientOrderId`(nullable) +
`trading_trades.client_order_id` 컬럼(nullable — dev file DB 하위호환). 선영속화 시점엔
거래소 uuid 가 없어 uuid 자리에 cid 를 담고(NOT NULL·unique 충족), 체결 확인 시
`assignExchangeUuid` 로 교체.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| 즉시 재조회만 (P0-2 현행) | 재조회 실패 시 영구 유실 — 무보호 포지션 갭이 그대로 |
| 현재가 폴백으로 DONE 처리 | 가짜 체결가로 장부 오염 — 롤아웃 게이트 지표(현재가폴백 0)와 상충 |
| WS(myOrder) 실시간 추적 | REST 재조회가 유일 진실원(계획 §Phase 4 — WS 는 관측 전용). 스윕이 전제 |
| 별도 스케줄러 잡 | 트레이딩 루프와 경합/중복 — 루프 시작부 스윕이 순서 보장(정합화 후 매매 판단) |
| **(선택) 선영속화 + 틱 스윕 + 진입 차단** | 응답 유실·UNKNOWN 전 경로를 결정적으로 수습, 기본 구성 무변경 |

## Consequences — 영향

- **긍정:** "체결됐으나 Position 없는 무보호 창" 제거. 주문의 전 생애가 DB 에 남아
  (SUBMITTED→DONE/CANCEL/FAILED) 운영 추적성 확보. v2 전환(ADR-0003)의 안전 전제 충족.
- **비용:** 매수마다 저장 1회 추가(선영속화), 틱마다 `findByStatus(SUBMITTED)` 1회.
  기본 구성(V1+OFF)에서는 선영속화가 꺼져 있어 운영 동작 불변.
- **한계:** v1+플래그 OFF 구성에서는 cid 가 부착되지 않아 스윕이 재조회할 수 없다 —
  이 구성의 응답 유실은 기존과 동일하게 "미접수 간주"(감수, v2 전환으로 해소).
  `manualBuy`/`manualSell` 및 리스크·리밸런스 청산 경로는 선영속화 미적용(신호 매수·매도만
  적용 — 리스크 경로는 자체 `closingAttempted` 재시도 추적 보유).
- **매직넘버:** grace 10초(전송 직후 in-flight 보호), 만료 2분(미발견 시 미도달 간주) —
  `TradingBotService` 상수.

## References

- 코드: `TradingBotService.executeBuy`/`reconcileSubmittedOrders`/`confirmSubmittedTrade`,
  `BithumbApiClient.placeMarketBuyOrder(amount, cid)`/`supportsClientOrderId`/`getOrderByClientOrderId`,
  `Trade.createSubmittedBuy`/`assignExchangeUuid`
- 테스트: `TradingBotServiceOrderReconciliationTest`(선영속화 순서·스윕 상태 전이·진입 차단),
  `TradingV2LostResponseSweepTest`(응답 유실→스윕 수습→Position 생성 E2E)
