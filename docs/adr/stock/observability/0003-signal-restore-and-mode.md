# ADR-0003: 영속 시각·수치 복원 + 체결·포지션 운영 모드(mode) 컬럼

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | stock |
| 관심사 | 관측성 / 도메인 모델 |
| 관련 ADR | [stock/observability/0002](0002-daily-performance-report.md), [stock/modes/0004](../modes/0004-paper-cash-ledger.md), [stock/infrastructure/0009](../infrastructure/0009-entry-failure-state-and-reason.md), [trading/observability/0004](../../trading/observability/0004-mode-entry-source-excursion.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 3) |

## Context — 무엇이 문제였나

- 운영 조회에서 `/api/stock/signals`·`/api/stock/events` 가 **요청 시각과 null** 을 돌려줬다.
  `StockSignalRepositoryAdapter.toDomain()` 이 `new StockSignal(code, type)` 만 호출해 `signalTime`·`createdAt` 이
  조회 시각으로 바뀌고 갭·체결강도·고점·풀백·반등·현재가가 전부 null 이었다.
- 같은 패턴(복원 경로가 생성자/팩토리를 불러 시각을 잃는 곳)을 stock·trading 전체 어댑터에서 확인했다:
  - `StockRepositoryAdapter` — `createdAt`·`updatedAt` 이 조회 시각(감시 종목 `createdAt` 이 매번 "지금").
  - `StockTradeRepositoryAdapter` — 팩토리 + `markFilled` 로 복원해 `orderedAt`·`executedAt`·`createdAt` 이 조회 시각,
    **`CANCELLED` 거래가 `PENDING` 으로 복원**됐다(체결 수량이 없으면 `markFilled` 를 건너뛰어 생성자 기본값 PENDING).
    미확인 주문 스윕(ADR infrastructure/0006)이 이미 포기한 PENDING- 거래를 다시 미확인으로 보고, 0009 의 "미확인 매수
    재주문 보류" 가 그 종목을 그날 계속 막는 원인이 된다.
  - `StockPosition.reconstitute()` — `createdAt` 인자를 받고도 버렸다(final 필드가 생성자에서 now).
  - `StockCandleRepositoryAdapter` — `createdAt` 이 조회 시각(호출처 없는 비활성 스캐폴딩이지만 같은 결함).
  - trading 어댑터 7종은 전 필드 생성자로 시각을 복원하고 있어 해당 없음.
- PAPER 와 LIVE 체결이 같은 테이블에 섞여도 구분 컬럼이 없어 주문번호 접두사(`SIM-`)로만 가를 수 있었다. PAPER 가상 예수금
  (ADR modes/0004)으로 PAPER 체결이 실제로 쌓이기 시작하면 손익 집계가 두 모드를 합산하게 된다.

## Decision — 무엇을 골랐나

복원은 저장된 값을 그대로 되돌리는 전용 경로로 바꾸고, 체결·포지션에 `mode` 를 남겨 집계를 현재 봇 모드로 가른다.

- 복원 전용 정적 메서드: `StockSignal.restore(...)`(시각·수치·executed·createdAt), `StockTrade.restore(...)`(상태·체결·
  시각·mode), `StockCandle.restore(...)`(createdAt). `Stock.restoreAuditTimestamps(createdAt, updatedAt)` 는 다른 복원
  호출 뒤 마지막에 불린다. `StockPosition.reconstitute()` 는 `createdAt` 을 실제로 반영하고 `mode` 를 받는다.
- `mode` 컬럼(nullable, `VARCHAR(10)`, 값 = `StockProperties.Bot.Mode` 이름): `stock_trades.mode`, `stock_positions.mode`.
  **처음 저장(insert)할 때 어댑터가 현재 봇 모드를 찍는다** — 모드는 프로세스 수명 동안 바뀌지 않으므로 insert 시점의
  모드가 그 행의 모드다. 생성 지점(매수 선영속화·매도·포지션·고아 체결 복구)을 일일이 고치지 않아 누락이 없다.
- 집계 필터(기본 = **현재 봇 모드**): `StockPositionService` 의 표시·집계용 조회(`getOpenPositions`·`getClosedPositions`·
  `getAllPositions`·`getPositionsInRange` — 대시보드·`/api/stock/positions/**`·이벤트 스트림·히스토리)와 11:40 일일
  리포트. 리스크 루프·최종청산·`countOpenPositions` 는 필터하지 않는다(보호 대상은 모드와 무관).
  `mode` 가 NULL 인 행(아래 백필 전 레거시)은 LIVE 로 취급한다.
- 백필(운영 수동 실행, 멱등 — `WHERE mode IS NULL`):

```sql
UPDATE stock_trades SET mode = CASE WHEN order_id LIKE 'SIM-%' THEN 'PAPER' ELSE 'LIVE' END
 WHERE mode IS NULL;
UPDATE stock_positions SET mode = CASE WHEN EXISTS (
         SELECT 1 FROM stock_trades t WHERE t.position_id = stock_positions.id AND t.order_id LIKE 'SIM-%')
       THEN 'PAPER' ELSE 'LIVE' END
 WHERE mode IS NULL;
```

  H2 2.4.240 MySQL 모드에서 실행·재실행(무변경)을 확인했다. 포지션은 연결된 거래로 판정한다 — 체결 거래가 없는 포지션은
  LIVE 로 분류된다(주식은 매수·매도 모두 `position_id` 를 남겨 해당 사례가 사실상 없다).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| A. 조회 시 주문번호 접두사로 모드 유도 | 스키마 무변경 | 포지션엔 주문번호가 없어 매번 조인, BACKTEST 구분 불가, 접두사 규칙이 집계 코드로 새어 나간다 |
| B. 생성 지점마다 서비스가 mode 를 설정 | 의도가 코드에 보인다 | 생성 지점 4곳(+미래 경로)에서 하나라도 빠지면 그 행이 조용히 LIVE 집계로 섞인다 |
| **C. insert 시 어댑터가 현재 모드를 찍음 (선택)** | 한 곳·누락 불가, 기존 행은 백필 | 어댑터가 `StockProperties` 를 읽는다 |

- NULL 을 LIVE 로 보는 이유: 배포 직후 백필 전에는 모든 과거 행이 NULL 이라, NULL 을 버리면 대시보드 이력이 통째로 사라진다.
  LIVE 로 보면 백필 전에는 종전과 같은 합산이고, 백필 후에는 NULL 이 남지 않는다.

## Consequences — 영향

- **긍정:** 신호·이벤트 API 가 실제 발생 시각과 수치를 돌려준다. 취소된 미확인 주문이 PENDING 으로 되살아나지 않는다.
  PAPER 실측이 LIVE 손익과 섞이지 않는다.
- **부정:** 현재 모드가 PAPER 인 동안 대시보드·히스토리는 PAPER 행만 보인다(과거 LIVE 이력을 보려면 모드를 바꿔야 함 —
  조회 파라미터는 범위 밖). 백필 전에는 레거시 SIM- 행이 LIVE 로 집계된다.
- **후속:** 운영 반영 직후 위 백필 SQL 1회 실행.
- 회귀 가드: `StockPersistenceRestoreTest`(어댑터 5종 복원·mode 찍기), `StockPositionServiceTest`·
  `DailyPerformanceReportServiceTest` 모드 필터.

## References

- `stock/infrastructure/persistence/adapter/*RepositoryAdapter.java`
- `stock/domain/{signal/StockSignal, trade/StockTrade, trade/StockTradeMode, stock/Stock, position/StockPosition, candle/StockCandle}.java`
- `stock/application/service/StockPositionService.java`, `DailyPerformanceReportService.java`
