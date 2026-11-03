# ADR-0009: 진입(주문 단계) 실패 상태·사유 영속화 + 재시도 간격·상한

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | stock |
| 관심사 | 인프라 / 관측성 |
| 관련 ADR | [stock/modes/0004](../modes/0004-paper-cash-ledger.md), [stock/infrastructure/0006](0006-order-pre-persistence-and-fill-backfill.md), [stock/algorithm/0003](../algorithm/0003-entry-validation-strictness.md), [stock/observability/0002](../observability/0002-daily-performance-report.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 2) |

## Context — 무엇이 문제였나

- 51거래일 0체결(ADR modes/0004) 동안 `openPosition()` 은 실패해도 `null` 만 돌려줬다. 실패 종목은 `ENTRY_READY`
  에 그대로 남아 **11:20 까지 5초마다** 시세·잔고·매수가능 API 3회를 반복했다(종목당 최대 약 1,200틱).
- 실패 사유는 `POSITION_OPEN_FAILED` 로그 이벤트뿐이라 DB 로는 "왜 0체결인가" 를 볼 수 없었다.
  `EntryAttempt`(ADR algorithm/0003)는 **검증 단계**(체결강도·호가·풀백 시간)만 기록하고, 그 뒤 **주문 단계**
  (시세 재조회·거래가능 재확인·사이징·주문)는 기록이 없었다.
- 부수 발견: `openPosition` 안의 예외가 `@Transactional` 프록시를 넘으면 참여 중인 트레이딩 루프 트랜잭션이
  롤백 전용으로 표시돼, 그 틱의 다른 기록(같은 틱의 청산 기록 포함)까지 커밋되지 않는다.
- 부수 발견: 주문 응답이 유실된 종목(ORDER_UNCONFIRMED)도 `ENTRY_READY` 라 다음 틱(5초 뒤)에 같은 종목을 또
  주문했다 — 미확인 주문 스윕(ADR infrastructure/0006)은 12틱 동안 원장을 찾으므로 LIVE 에서는 응답만 유실된 체결
  위에 중복 매수가 쌓일 수 있다(PAPER 는 시뮬레이션이 항상 응답해 발생하지 않음).

## Decision — 무엇을 골랐나

주문 단계 결과를 사유가 있는 결과 타입으로 돌려주고, 실패를 `Stock` 에 남기며, 재시도를 간격·횟수로 제한한다.

- `StockPositionService.openPosition()` → `OpenResult(EntryOutcome, StockPosition)`.
  `EntryOutcome = OPENED / QUOTE_UNAVAILABLE / NOT_TRADABLE / SIZE_ZERO / ORDER_UNCONFIRMED / ERROR`.
  메서드 안의 RuntimeException 은 프록시를 넘기기 전에 `ERROR` 로 바꾼다(위 롤백 전용 전파 차단).
- `Stock` 에 `entryFailCount`, `lastEntryFailReason`, `lastEntryFailAt` (nullable 컬럼 `entry_fail_count`,
  `last_entry_fail_reason`, `last_entry_fail_at`). `Stock.recordEntryFailure(reason, now, max)` 가 규칙을 갖는다:
  사유·시각은 매번 기록, 횟수는 `ORDER_UNCONFIRMED` 를 **제외**하고 센다, 센 횟수가 `stock.entry.max-open-attempts`
  (기본 3)에 닿으면 `ENTRY_READY → ENTRY_FAILED`.
- 새 상태 `StockState.ENTRY_FAILED` — `isActive()=false`, `isTerminal()=true`. `findActiveStocks` JPQL 은 같은
  활성 집합(WATCHING·HIGH_FORMED·PULLBACK·ENTRY_READY)이라 자동 제외된다.
- 재시도: `GapPullbackBotService.executeEntries` 가 `Stock.isEntryRetryDue(now, stock.entry.open-retry-interval-seconds
  (기본 30))` 이 아니면 그 종목을 건너뛴다. `now` 는 stock `Clock` 기준이고 기록·판정이 같은 시계를 쓴다.
- `ORDER_UNCONFIRMED` 는 상태·횟수를 건드리지 않는다(판정은 스윕). 대신 **같은 종목에 미확인 매수(PENDING- 주문번호,
  PENDING 상태)가 남아 있으면 그 종목의 재주문을 보류**한다 — 스윕이 체결(→ 포지션 생성) 또는 미접수(→ CANCELLED)로
  판정한 뒤에만 다시 시도한다. 코인의 미결 주문 신규 매수 차단(ADR trading/infrastructure/0002)과 같은 원칙.
- 조회: `GET /api/stock/entry-attempts?date=` — `validation`(EntryAttempt) + `orderFailures`(실패 이력이 있는 Stock 의
  상태·횟수·마지막 사유·시각). 일일 리포트(11:40)에 `entryFailureCounts`(종목별 마지막 사유 → 종목 수)와 메일 섹션
  "진입 실패(주문 단계)", `DAILY_REPORT` 이벤트 키 `entryFailures`. 진입 실패만 있던 날도 리포트를 보낸다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| A. 실패 즉시 ENTRY_FAILED | 단순 | 일시적 시세 조회 실패 한 번에 그날 기회를 잃는다 |
| B. 실패마다 EntryAttempt 행 추가(stage 컬럼) | 실패별 정확한 건수 | 테이블 의미(검증 단계)가 섞이고 기존 거절 사유 집계·`entryAttemptCount` 를 모두 손봐야 한다. 최대 3회라 마지막 사유로 충분 |
| C. ORDER_UNCONFIRMED 도 시간 간격만 적용 | 구현 단순 | 스윕은 12틱(약 60초) 동안 원장을 찾는데 30초 간격이면 그 사이 재주문이 나갈 수 있다. 판정은 상태 기반이어야 정확 |
| **D. 사유 결과 + Stock 필드 + 간격·상한 + 미확인 주문 대기 (선택)** | 원인이 DB 에 남고 API 호출이 실패 종목당 최대 3회로 줄어든다 | `Stock` 컬럼 3개 추가(nullable) |

- 횟수 해석: "최대 3회 초과 시 ENTRY_FAILED" 를 "3번째로 센 실패에서 전이(4번째 시도 없음)" 로 구현했다 — 최대 시도
  횟수가 3이라는 뜻.
- `ORDER_UNCONFIRMED` 를 횟수에 넣으면, 실제로 체결된 주문의 응답만 유실된 경우에도 ENTRY_FAILED 가 될 수 있다 — 스윕이
  고아 체결을 복구하면 `markEntered` 로 ENTERED 가 되므로, 상태 판정은 스윕에 맡긴다.

## Consequences — 영향

- **긍정:** "왜 0체결인가" 를 `/api/stock/entry-attempts` 와 11:40 리포트로 바로 본다. 실패 종목의 API 호출이
  5초마다 무한 반복 → 30초 간격 최대 3회로 준다. 예외 한 종목이 같은 틱의 다른 기록을 롤백시키지 않는다.
  LIVE 에서 응답 유실 후 같은 종목 중복 매수 경로가 닫힌다.
- **부정:** 일시적 장애가 3회 겹치면 그 종목은 그날 재시도하지 않는다(의도된 트레이드오프). `ENTRY_FAILED` 종목은
  대시보드 워치리스트(`/api/stock/monitoring/active`)에 나오지 않는다 — 활성 상태가 아니기 때문.
- 주의: 미확인 매수 대기는 `StockTrade` 복원이 CANCELLED 를 PENDING 으로 되돌리지 않아야 정확하다 — 이 복원 결함은
  [stock/observability/0003](../observability/0003-signal-restore-and-mode.md) 에서 함께 고쳤다.
- 회귀 가드: `StockEntryFailureTest`(5), `StockPositionServiceTest` 결과 사유 8건, `GapPullbackBotEntryFailureTest`(5),
  `StockEntryAttemptApiControllerTest`(1), `DailyPerformanceReportServiceTest`·`StockMailServiceTest`·
  `StockRepositoryAdapterTest` 각 1건.

## References

- `stock/domain/stock/{EntryOutcome, Stock, StockState}.java`
- `stock/application/service/StockPositionService.java` (`openPosition`, `recordEntryFailure`, `unconfirmedBuyStockCodes`)
- `stock/application/service/GapPullbackBotService.java` (`executeEntries`)
- `stock/presentation/api/StockEntryAttemptApiController.java`
- `stock/application/service/DailyPerformanceReportService.java`, `StockMailService.java`
