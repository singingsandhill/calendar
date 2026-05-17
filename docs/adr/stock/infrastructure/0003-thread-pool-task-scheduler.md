# ADR-0003: ThreadPoolTaskScheduler(pool=4) — 스크리닝과 트레이딩 루프 병렬

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 인프라 / 동시성 / 스케줄러 |
| 관련 커밋 | `docs/git_commit.md` Commit 5 (PR-5) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

Spring 의 기본 `@Scheduled` 풀 사이즈는 1. 단일 스레드가 순차로 모든 잡을 돈다.

스톡 봇의 잡 일정:

| 시각 | 잡 | 예상 소요 |
|---|---|---|
| 09:20 | 갭 스크리닝 | ~20초 |
| 09:20–11:20 | 트레이딩 루프 | 5초마다 |

기본 풀(=1) 이면 09:20 에 스크리닝이 시작되는 동안 5초 트레이딩 틱 4번이 *밀려서
순차 실행*. 스크리닝 끝난 직후 4번이 한꺼번에 몰린다 — 시장 변화 즉시 반응 손실.

## Decision — 무엇을 골랐나

`ThreadPoolTaskScheduler` 빈을 명시 등록 + 풀 사이즈 4.

- **`StockSchedulerConfig`** 에 `taskScheduler` 빈 등록 (`pool=4`).
- 09:20 스크리닝(20초) 진행 중에도 트레이딩 루프(5초) 가 병렬 실행.
- 풀 사이즈 4 = 동시 실행 가능 잡 개수 4. 스크리닝 1 + 트레이딩 1 + 버퍼 2.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 기본 풀(1) 유지 | 단순 | 위 시나리오에서 트레이딩 루프 정확도 손실 |
| 풀 사이즈 8+ | 여유 | KIS Semaphore(8) 와 같거나 큼 → 게이트 의미 약화 |
| 잡을 비동기 (`@Async`) 로 위임 | 풀 사이즈 무관 | 잡 단위 transactional 컨텍스트 분리 부담 |
| **(선택) 풀=4** | KIS Semaphore(8) 보다 낮춰 안전 마진 | — |

풀 사이즈가 KIS 동시 호출 한도(8) 보다 작은 이유: 한 잡이 종목 여러 개를 평가하면
잡 1개가 동시에 다수 KIS 호출. 풀 4 × 종목당 평균 2 호출 = 8 — Semaphore 한도와
정합.

## Consequences — 영향

- **긍정:**
  - 09:20 스크리닝과 트레이딩 루프 동시 실행 → 시장 반응 실시간성 회복.
  - 동시성은 ADR-0001 (Semaphore) + ADR-0002 (per-symbol Lock) 두 레이어가 보호.
- **부정:**
  - 풀 사이즈 추정에 가정 다수 (종목당 호출 수, KIS 한도). 운영 모니터링 필요.
- **후속:**
  - ADR-0001 (Clock 빈) 의 결정성 테스트가 동일 빈 컨텍스트에 함께 등록.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/scheduler/StockSchedulerConfig.java`
- 관련 ADR: [stock/infrastructure/0001 KIS Semaphore](0001-kis-rate-limit-semaphore.md), [stock/infrastructure/0002 Per-Symbol Lock](0002-per-symbol-reentrant-lock.md), [stock/modes/0001 PAPER + Clock](../modes/0001-paper-backtest-mode-and-clock-bean.md)
- 관련 커밋: `docs/git_commit.md` Commit 5
