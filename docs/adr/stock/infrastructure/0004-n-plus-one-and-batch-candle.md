# ADR-0004: N+1 제거 + CandleService 배치 처리 + KIS 재시도

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-01-23 |
| 도메인 | stock |
| 관심사 | 인프라 / 영속성 / 외부 API |
| 관련 커밋 | `9d6cac5` |
| 관련 이슈 | #17 |

## Context — 무엇이 문제였나

스톡 봇 1차 운영에서 세 가지 성능·안정성 문제가 동시에 드러났다.

1. **N+1 쿼리** — 종목 후보를 순회하며 각 종목의 캔들/포지션을 *각각* 쿼리. 70개
   종목 평가 시 수백 회의 SELECT.
2. **캔들 동기화 1건씩 호출** — `CandleService.sync(code)` 가 종목 단건 KIS 호출.
   Semaphore (당시 미존재) 도 없어 race + 한도 초과.
3. **KIS API 일시 장애 시 즉시 실패** — 토큰 만료/네트워크 흔들림에 재시도 없음 →
   루프 한 번 통째로 실패.

## Decision — 무엇을 골랐나

영속성 레이어와 KIS 클라이언트를 한 번에 정리.

- **N+1 제거** — JPA 쿼리 fetch join / `@EntityGraph` 또는 batch fetch 적용.
- **`CandleService` 배치 처리** — 종목 리스트 단위로 묶어 한 번에 동기화.
- **KIS API 재시도** — `KisRestClient` / `KisAuthService` 에 재시도 로직 (지수
  백오프). 토큰 만료 자동 갱신.

## Rationale — 왜 이 선택인가

이 시점(2026-01) 까지는 단일 스레드로 충분했고, 동시성보다 *한 사이클 안정 완료*
가 우선 과제였다. 따라서 동시성 레이어(ADR-0001 Semaphore, ADR-0002 per-symbol
Lock) 는 후속(2026-05) 으로 미루고, 이번엔 *호출량 줄이기* + *실패 회복* 에 집중.

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| Lazy 로딩 유지 | 코드 변경 적음 | N+1 그대로 |
| 캐시 레이어 (Redis) | 응답 즉답 | 인프라 추가, 데이터 신선도 정책 추가 부담 |
| **(선택) JPA fetch + 배치 + 재시도** | 인프라 추가 0, 즉시 효과 | — |

## Consequences — 영향

- **긍정:**
  - 한 사이클당 KIS 호출 수 감소.
  - 토큰 만료/네트워크 흔들림에 자동 회복.
  - 후속 ADR-0001 (Semaphore) 의 효과 측정에 깨끗한 baseline 제공.
- **부정:**
  - 배치 fetch 가 큰 풀에서 메모리 spike 가능 — 70개 수준에선 문제 없음.
- **후속:**
  - 동시성 3종 셋트(ADR-0001/0002/0003) 가 그 위에 쌓임.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/CandleService.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java`
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisAuthService.java`
- 관련 docs: `docs/troubleshooting/jpa-non-unique-result-exception.md` (관련 JPA 사례)
- 관련 커밋: `git log -1 9d6cac5`
