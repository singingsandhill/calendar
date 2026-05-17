# ADR-0003: 진입 검증 위양성 제거 + EntryAttempt 영속화

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 도메인 모델 |
| 관련 커밋 | `docs/git_commit.md` Commit 4 (PR-4) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

`PullbackDetectionService.validateEntryConditions` 가 *데이터가 없을 때 통과* 시키는
위양성을 두 군데 가지고 있었다.

1. **호가창(orderbook) 이 null** 인 경우 — `imbalancePassed=true` 로 처리. 즉 호가
   불균형 정보가 *없는데* 통과. 실제로는 KIS 응답 누락이 원인이라 검증 자체가
   불가능한 상태인데 자동 통과로 진입 결정.
2. **체결강도(tradeStrength) 가 null/0** 인 경우 — 통과 처리. 장 초반 미집계와 동일한
   상황을 진입 가능으로 분류.

추가로 진입이 거절될 때 *왜 거절됐는지* 가 휘발 — 사후 분석/튜닝 시 어떤 조건이
가장 자주 막히는지 추적 불가.

## Decision — 무엇을 골랐나

위양성 두 곳을 FAIL 로 변경하고, 진입 시도 흔적을 도메인 모델로 영속화.

- **orderbook null 시 `imbalancePassed=false`** — 데이터 없으면 통과 X.
- **tradeStrength null/0 시 FAIL** (이전: null=PASS) — 데이터 부족과 진입 가능을
  분리.
- **`ENTRY_ATTEMPT` 이벤트에 `rejectReason` 추가** — 어떤 조건이 막았는지 라벨링.
- **`EntryAttempt` 도메인 + `stock_entry_attempts` 테이블 신설** — 도메인/JPA Entity/
  Adapter/Repository 5개 추가. 모든 진입 시도(통과/거절 모두)를 기록.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 데이터 없을 때 *알 수 없음* 상태 추가 | 의미 정밀 | 3-state 가 매번 코드 분기 추가 — 유지보수 비용 vs 효익 낮음 |
| 로그로만 거절 사유 남기기 | 영속성 부담 0 | 집계 분석/대시보드 어려움. SQL 한 번에 "어제 ENTRY_REJECTED top reason" 쿼리 불가 |
| **(선택) FAIL 로 통일 + EntryAttempt 영속화** | 안전 default + 사후 분석 | — |

체결강도 정책은 ADR-0002 (UniverseBuilder) 의 `skipZeroStrength=true` 와 같은 방향:
*데이터 없음 = 통과 X* 가 일관 정책.

## Consequences — 영향

- **긍정:**
  - "왜 진입했는지 모르는" 진입 사라짐. 거절도 모두 추적 가능.
  - 백테스트/튜닝 시 `SELECT rejectReason, COUNT(*) FROM stock_entry_attempts
    GROUP BY rejectReason` 으로 가장 빈번한 거절 패턴 식별.
- **부정:**
  - 진입 빈도 감소 가능 — 9:00–9:10 사이 strength=0 종목이 자동 탈락.
  - 테이블 1개 추가 (DDL/마이그레이션 부담).
- **후속:**
  - ADR-0004 (TP 비순차화) 와 함께 진입/청산 양 끝의 정합성 작업의 일환.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/PullbackDetectionService.java`
  - `src/main/java/me/singingsandhill/calendar/stock/domain/screening/EntryAttempt.java`
  - `src/main/java/me/singingsandhill/calendar/stock/domain/screening/EntryAttemptRepository.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/persistence/entity/EntryAttemptJpaEntity.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/persistence/repository/EntryAttemptJpaRepository.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/persistence/adapter/EntryAttemptRepositoryAdapter.java`
- 관련 docs: `docs/stock-bot.md` (진입 검증 정책)
- 관련 커밋: `docs/git_commit.md` Commit 4
