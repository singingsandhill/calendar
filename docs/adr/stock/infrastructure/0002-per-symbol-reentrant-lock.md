# ADR-0002: 종목 코드별 ReentrantLock (StockCodeLocks)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 인프라 / 동시성 |
| 관련 커밋 | `docs/git_commit.md` Commit 5 (PR-5) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

ADR-0003 (ThreadPoolTaskScheduler) 가 도입되면 트레이딩 루프(5초)와 스크리닝(20초)
이 병렬로 돌 수 있다. 같은 시각에 다음 두 경로가 *동일 종목* 에 부딪힐 수 있다:

1. `openPosition(code)` — 진입 매수.
2. `executePartialExit(code)` — 부분 익절 매도.

KIS 주문은 비동기 응답이라, 매수 응답이 도착하기 전에 매도가 호출되면 *포지션이
없는데 매도* 되거나, 두 매수가 중첩되어 한도 초과 보유가 발생할 수 있다. JVM 안의
`@Transactional` 만으로는 외부 호출까지 보호되지 않는다 (DB 락은 KIS 호출 동안 잡혀
있어 자원 점유).

## Decision — 무엇을 골랐나

종목 코드별 ReentrantLock 발급기를 두고, 진입/청산 진입부에서 락 획득.

- **`StockCodeLocks`** — `ConcurrentHashMap<String, ReentrantLock>` 발급기. 종목 코드를
  키로 한 ReentrantLock 을 lazy 발급, 처음 요청 시 생성하여 같은 종목 후속 호출은
  같은 락 인스턴스 재사용.
- **`openPosition` / `executePartialExit`** 진입부에서 `lock.lock()` / `unlock()` 으로
  감싸 동일 종목 동시 매수/매도 race 차단.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 전역 락(`synchronized` static) | 단순 | 종목 70개가 한 줄로 직렬화 — 처리량 손실 |
| DB pessimistic lock (`SELECT FOR UPDATE`) | 분산 안전 | 단일 인스턴스 + KIS 외부 호출 동안 락 점유 — DB 자원 낭비 |
| Redis 분산 락 | 미래 다중 인스턴스 대응 | 현재 단일 인스턴스, Redis 미도입 → 인프라 추가 |
| **(선택) 종목별 in-memory ReentrantLock** | 종목 격리 + 단일 인스턴스에서 충분 | — |

JVM 락이 충분한 이유: 봇은 단일 인스턴스로 운영. 향후 액티브-액티브 다중 인스턴스로
확장하면 ADR 을 *Superseded* 처리하고 분산 락 ADR 신설.

## Consequences — 영향

- **긍정:**
  - 동일 종목에 중첩 매수/매도 race 차단.
  - 다른 종목은 영향 없음 — 종목 단위 병렬성 유지.
- **부정:**
  - `ConcurrentHashMap` 의 종목 락 엔트리는 영구 — 거래 정지 종목도 메모리에 남음.
    풀 사이즈 70개 수준이라 무시 가능.
  - 개발 시 락 누락 (try-finally 깜빡)으로 데드락 위험. 코드 리뷰 가드 필요.
- **후속:**
  - ADR-0001 (Semaphore) 와 ADR-0003 (ThreadPool) 와 같이 동시성 3종 셋트로 동작.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/application/concurrency/StockCodeLocks.java`
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java`
- 관련 ADR: [stock/infrastructure/0001 KIS Semaphore](0001-kis-rate-limit-semaphore.md), [stock/infrastructure/0003 ThreadPool](0003-thread-pool-task-scheduler.md)
- 관련 커밋: `docs/git_commit.md` Commit 5
