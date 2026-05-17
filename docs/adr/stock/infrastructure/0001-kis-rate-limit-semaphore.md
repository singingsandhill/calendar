# ADR-0001: KIS rate-limit Semaphore(8, fair) + apiCallsLast5min 메트릭

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 인프라 / 동시성 / 외부 API |
| 관련 커밋 | `docs/git_commit.md` Commit 5 (PR-5) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

KIS (한국투자증권) Open API 는 호출량 한도가 좁다 (초당/분당 제한). 기존
`ScreeningService` 는 70+ 종목을 평가할 때 종목 사이에 `Thread.sleep(100)` 으로
간격을 두는 방식이었다.

문제:

1. **고정 sleep 은 동시성을 막지만 비효율** — 빠른 호출과 느린 호출이 같은 100ms
   를 쉰다. 시장 데이터가 즉시 도착하는 시간대에는 1.5분 가까이 잠자고 있다.
2. **다른 경로의 KIS 호출이 게이트 밖** — 트레이딩 루프, BotStatus 폴링, 토큰 갱신
   같은 호출이 sleep 게이트 밖에서 동시에 발생해 합산 한도 초과.
3. **호출량 가시성 없음** — 한도에 가까워지는지 운영자가 알 수 없음.

## Decision — 무엇을 골랐나

`KisRestClient` 진입부에 공정 Semaphore 게이트를 두고 카운터를 같이 기록.

- **`Semaphore(8, fair=true)`** — 동시 KIS HTTP 호출을 8개로 제한. 공정 모드로 starvation
  방지.
- **`acquire` 타임아웃 15s** — 게이트 통과 못하면 명시적 실패 (스레드 끊김 방지).
- **`apiCallsLast5min` 메트릭 카운터 동시 기록** — 슬라이딩 윈도우, ADR-0001
  (관측성) 의 `StockBotMetrics` 가 노출.
- `ScreeningService` 의 종목 사이 `Thread.sleep(100)` 제거 — 게이트가 같은 역할 수행.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| Resilience4j RateLimiter | 표준화된 토큰 버킷 | 라이브러리 추가, 동시성 제한 ≠ 호출량 제한이지만 우리는 *동시성* 만 막아도 충분 |
| Spring Reactor `concatMap` 직렬화 | 100% 순차 | 처리량 1/8 — 시장 변화 빠른 시간대에 진입 누락 |
| Thread.sleep 유지 | 코드 변경 0 | 위 3가지 문제 그대로 |
| **(선택) Semaphore(8, fair) + 카운터** | 한 곳 게이트 + 가시성 | — |

`fair=true` 의 비용은 약간의 throughput 감소이지만, 진입 결정은 *시간 정밀도가
중요* — 늦게 도착한 요청이 영원히 밀리면 같은 종목의 후속 폴링이 무효화될 수 있다.

## Consequences — 영향

- **긍정:**
  - 모든 KIS 호출이 한 게이트를 통과 → 토큰 갱신 / 트레이딩 / 스크리닝의 합산 한도
    제어.
  - `apiCallsLast5min` 으로 한도 근접 시 사전 신호.
  - `Thread.sleep(100)` 제거로 한가한 시간대 처리량 회복.
- **부정:**
  - 8 동시 호출 한도가 KIS 의 실제 한도(시기별 변동)와 다를 수 있음 — 관측 후 조정.
  - 15s 타임아웃 시 호출 측에서 명시적 실패 처리 필요 (이전엔 sleep 만 했으므로
    예외 케이스 없음).
- **후속:**
  - ADR-0002 (per-symbol ReentrantLock) 가 같은 종목의 매수/매도 race 를 막는
    별도 레이어로 보완.
  - ADR-0003 (ThreadPoolTaskScheduler) 가 풀 사이즈 4 로 잡아 Semaphore 8 보다
    낮춰 안전 마진.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java`
  - `src/main/java/me/singingsandhill/calendar/stock/observability/StockBotMetrics.java`
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/ScreeningService.java`
- 관련 ADR: [stock/observability/0001](../observability/0001-trade-events-logger-and-bot-metrics.md), [stock/infrastructure/0002](0002-per-symbol-reentrant-lock.md), [stock/infrastructure/0003](0003-thread-pool-task-scheduler.md)
- 관련 커밋: `docs/git_commit.md` Commit 5
