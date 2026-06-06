# ADR-0001 (trading/infrastructure): 주문 실행 트랜잭션 경계 — HTTP/sleep 을 트랜잭션 밖으로

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 인프라 / 트랜잭션 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P0-3) |

## Context — 무엇이 문제였나

`TradingBotService.executeTradeLoop` 이 `@Transactional` 이고, 내부에서
`executeTradeBySignal`/`executeBuy`/`executeSell` 을 **같은 빈 self-invocation** 으로
호출했다. 결과:

1. **전체 루프가 하나의 트랜잭션** — 모든 주문 HTTP + 체결가 재시도 `Thread.sleep`
   (최대 ~6s) 가 한 트랜잭션 안에서 실행 → DB 커넥션을 네트워크 I/O 내내 점유,
   체결 성공 후 영속화 실패로 롤백되면 **DB ↔ 거래소 상태 괴리**.
2. **self-invocation 함정** — 내부 메서드의 `@Transactional` 은 *외부 루프 트랜잭션
   덕분에만* 동작. loop `@Transactional` 을 단순 제거하면 내부 트랜잭션이 **조용히
   사라져** Trade/Position 다중 저장의 원자성이 깨진다(프록시 self-invocation).

추가 관찰: 이 모듈은 **어댑터 패턴**(도메인 ↔ JPA 엔티티 변환)이라 서비스 계층이
열린 `EntityManager` 를 들고 있지 않다. 즉 서비스 `@Transactional` 의 *유일한* 실효는
"여러 `adapter.save()` 호출의 원자성" 뿐(각 어댑터 save 는 Spring Data 가 자체 트랜잭션
처리). 이 사실이 해법을 단순화한다.

## Decision — 무엇을 골랐나

`TradingBotService` 의 클래스·메서드 `@Transactional` 을 **모두 제거**하고,
**영속화만** `TransactionTemplate` 으로 감싼다.

- 주문 HTTP(`placeMarket*`)·체결가 재시도(sleep)·가격 계산은 **트랜잭션 밖**.
- `Trade` + `Position` 의 원자적 저장만 `txTemplate.executeWithoutResult(...)` 안에서.
- 단일 저장(manualBuy/manualSell 의 Trade 1건)은 어댑터 자체 트랜잭션으로 충분 →
  별도 래핑 없음.
- self-injection/프록시 우회 대신 **TransactionTemplate** 채택 — 명시적이고
  self-invocation 과 무관.
- **P0-3b (완료):** 동일 패턴을 `RiskManagementService`(클래스/메서드 `@Transactional`
  제거 → `closePosition` 의 Trade+Position 영속화만 `TransactionTemplate`) 와
  `RebalanceService`(`checkAndExecute`/`buyAndOpenPosition` 동일) 에도 적용.
  → 청산·리밸런스 경로도 주문 HTTP/sleep 이 트랜잭션 밖.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| loop `@Transactional` 만 제거 | self-invocation 으로 내부 트랜잭션이 조용히 사라짐 |
| self-injection(ObjectProvider) | 프록시 경유로 `@Transactional` 복구는 되나 HTTP/sleep 이 여전히 (per-op) 트랜잭션 안 |
| 주문 실행을 별도 빈으로 추출 | 가능하나 더 큰 구조 변경 — 어댑터 패턴 덕에 불필요 |
| **(선택) TransactionTemplate 로 영속화만 래핑** | HTTP/sleep 을 확실히 트랜잭션 밖으로, 원자성 유지, 최소 침습 |

## Consequences — 영향

- **긍정:** 루프 mega-transaction 제거, 커넥션 점유를 영속화 순간으로 단축, 신호
  매수/매도 경로에서 HTTP/sleep-in-transaction 제거. `executeTradeBySignal`/loop 은
  비트랜잭션 오케스트레이션, 교차 빈 서비스(candle/signal/risk/rebalance)는 각자
  자기 트랜잭션(루프 전체가 아닌 연산 단위).
- **P0-3b 완료:** `RiskManagementService.closePosition`/`checkAndExecuteRiskRules` 와
  `RebalanceService.checkAndExecute`/`buyAndOpenPosition` 에서도 `@Transactional` 제거 +
  주문 HTTP 를 트랜잭션 밖으로 — 모든 주문 경로에서 HTTP-in-tx 제거.
- **남은 한계:** 수동 매매(`manualBuy`/`manualSell`)는 Position 미생성/미청산 → 계좌-포지션
  정합성 밖(별도 후속 #3).
- **테스트:** tx 의미론은 통합 테스트 영역이라, 특성화 테스트
  (`TradingBotServicePersistenceTest`)로 "주문 성공 → 원자 저장 / 주문 null → 미저장"
  불변식만 가드. 어댑터 패턴이 위험을 낮춘다.

## References

- 코드: `application/service/TradingBotService.java` (`txTemplate`, executeBuy/executeSell)
- 테스트: `TradingBotServicePersistenceTest`
