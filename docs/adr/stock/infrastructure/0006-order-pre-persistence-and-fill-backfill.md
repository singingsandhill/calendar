# ADR-0006: 주문 선영속화 + 실체결 backfill + 고아 체결 스윕

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 인프라 / 주문 신뢰성 |
| 관련 ADR | [stock/infrastructure/0005](0005-non-idempotent-order-no-retry.md) (주문 무재시도 — 본 결정의 전제), [trading/infrastructure/0002](../../trading/infrastructure/0002-order-pre-persistence-and-tick-sweep.md) (크립토 선례) |
| 관련 이슈 | 주식 봇 로직 리뷰 §3-④ (P1-1) |

## Context — 무엇이 문제였나

- **체결가가 픽션이었다**: 매수·매도 모두 주문 *직전* 시세를 체결가로, 수수료를 0 으로 기록
  (`markFilled(currentPrice, qty, ZERO)`). 체결조회 TR(TTTC8001R)은 구현돼 있으나 호출처가
  0곳이었다. 진입가·손절가·TP 앵커가 전부 이 픽션 위에 서 있었다.
- **무보호 포지션 갭**: 주문 전 영속화가 없어, 주문이 KIS 에 도달했는데 응답만 유실되면
  (ADR-0005 로 재시도를 없앤 뒤에는 특히) **실제로는 보유 중인데 시스템에는 없는 포지션**이
  생긴다. 손절·익절 루프가 모르는 포지션은 11:20 청산도 받지 못한다.

## Decision — 무엇을 골랐나

**3종 세트** (크립토 ADR trading/infrastructure/0002 의 주식 버전):

1. **선영속화** — 매수 주문 전송 *전에* `StockTrade(PENDING, orderId="PENDING-<nano>")` 저장.
   응답이 성공이면 `assignBrokerOrderId(ODNO)` 로 교체하고, null/실패면 **PENDING 을 실패로
   덮지 않는다**(접수 여부 불명 → 스윕이 판정). `ORDER_UNCONFIRMED` 이벤트 기록.
2. **실체결 backfill** — 주문 성공 직후 당일주문체결조회로 ODNO 를 찾아 실체결 평균가
   (`avg_prvs`)·체결수량으로 `markFilled`, 수수료는 `체결금액 × commissionRate` 로 산정
   (기존 0 → 모델값). **포지션의 진입가·손절가도 실체결가 기준**으로 생성한다.
   조회 실패 시에는 요청가로 폴백하되 WARN 으로 "장부가 픽션임"을 드러낸다.
3. **고아 체결 스윕** — 트레이딩 루프(5초) 시작부에서 `reconcileUnconfirmedOrders()`:
   미확인 PENDING 매수를 브로커 원장(당일주문체결조회)과 대조해
   - 체결 확인 → 거래 정합화 + **포지션이 없으면 생성**(무보호 포지션 제거), `ORPHAN_FILL_RECOVERED`
   - `MAX_RECONCILE_ATTEMPTS`(12틱 ≈ 1분) 동안 원장에 없음 → 미접수로 간주해 CANCELLED,
     `ORDER_NOT_ACCEPTED`
   스윕 실패는 리스크 체크를 막지 않는다(자체 try-catch).

## Rationale

- KIS `order-cash` 는 클라이언트 주문번호를 지원하지 않아 크립토처럼 cid 로 정확히 대조할 수
  없다. 따라서 매칭 키는 **종목코드 + 주문수량 + 미연결(known orderId 아님)** 조합이다.
  같은 종목·같은 수량의 미확인 주문이 동시에 여럿이면 오매칭 가능성이 있으나,
  종목당 포지션이 1개로 제한되고 스윕이 매칭 즉시 `knownOrderIds` 에 추가하므로
  실사용 시나리오에서는 충돌하지 않는다.
- 시도 횟수 기반(시각 기반 아님) 포기 판정: `StockTrade.orderedAt` 은 영속화 복원 시
  현재시각으로 재설정되는 한계가 있어 신뢰할 수 없다 (별도 후속 과제).

## Consequences

- **긍정:** 장부 P&L 이 실체결 기반이 되고, 응답 유실이 무보호 포지션으로 남지 않는다.
- **주의:** 매도(청산) 경로에는 아직 선영속화가 없다 — 매도는 포지션이 이미 존재해 리스크
  루프의 보호를 받고 있어 우선순위가 낮다(후속 과제).
- 미확인 주문이 있는 동안 틱당 당일주문조회 1콜이 추가된다(최대 12틱).
- 회귀 가드: `StockPositionServiceTest` (backfill·수수료·선영속화·스윕 3종·동적 손절).

## References

- `stock/application/service/StockPositionService.java` (`reconcileUnconfirmedOrders`, `resolveBuyFill`)
- `stock/domain/trade/StockTrade.java` (`PENDING_ORDER_ID_PREFIX`, `assignBrokerOrderId`)
- `stock/application/service/GapPullbackBotService.java` (루프 시작부 스윕 호출)
