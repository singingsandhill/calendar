# ADR-0002: UniverseBuilder 스냅샷 캐시 + skipZeroStrength 토글

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 데이터 |
| 관련 커밋 | `d135907`, git_commit.md Commit 3 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

기존 `executeScreeningLoop()` 는 70개 종목을 `getSampleStockCodes()` 로 하드코딩해
사용했다. 두 가지 문제:

1. **하드코딩 풀** — 시장이 바뀌어도 종목 후보가 70개로 고정. 신규 IPO / 거래정지
   대응 불가.
2. **pre-market 루프가 비어 있음** — `executePreMarketLoop()` 는 TODO 였다. 즉
   08:30 에 사전 빌드할 자리가 있었지만 무엇도 하지 않았다.
3. **장 초반 strength=0 처리** — KIS 체결강도 집계가 9:00–9:10 사이는 불완전.
   `strength=0` 종목을 *데이터 부족으로 스킵* 할지 *통과* 시킬지 운영자가 토글하고
   싶어도 코드 수정 필요.

## Decision — 무엇을 골랐나

`UniverseBuilder` 도입 + 스냅샷 캐시 + 운영 플래그.

- **`UniverseBuilder`** — `pinned` (관심 종목) + `fallback-codes` (안정 후보) 합집합으로
  그날의 유니버스를 *스냅샷으로 캐시*. 한 번 빌드하면 거래일 변경 전까지 재사용.
- **rank-api-top placeholder** — KIS 등락률/거래량 순위 API 자리만 마련 (현재는
  pinned + fallback 만 사용, 차후 동적 후보로 확장).
- **`executePreMarketLoop()`** — 08:30 에 `UniverseBuilder.build()` 호출.
- **`executeScreeningLoop()`** — `UniverseBuilder.currentUniverse()` 사용. 빈 유니버스
  시 `SCREENING_SKIPPED` 이벤트 + 조기 리턴.
- **`Screening.skipZeroStrength=true`** (기본) — 장 초반 strength=0 종목을 데이터
  부족으로 스킵. false 면 통과 처리.
- `ScreeningService` 에 metrics 훅 + `SCREENING_SUMMARY` 이벤트 emit.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 매번 KIS 순위 API 로 동적 빌드 | 항상 최신 | 초당 호출 폭증, 같은 거래일 안에서도 점심 전후 다른 풀 → 진입/청산 정합성 깨짐 |
| pinned 만 사용 | 단순 | 거래정지/이상신호 시 후보 0 |
| **(선택) pinned ∪ fallback 합집합 + 거래일 단위 스냅샷** | 안정성 + 단순함, 미래 확장 자리 | — |

`skipZeroStrength` 가 *기본 true* 인 이유: KIS 미집계를 통과시키면 `strength=0` 종목이
floor 통과해 의미 없는 진입 시도가 발생. 안전한 default 가 더 일반적.

## Consequences — 영향

- **긍정:**
  - 70개 하드코딩 풀 사라짐. yaml 만 수정하면 운영자가 풀 변경.
  - 빈 유니버스 시 `SCREENING_SKIPPED` 이벤트로 ADR-0001 (TradeEvents) 채널에 명확 신호.
  - 거래일 변경 시 자동 재빌드 → 자정 경계 문제 없음.
- **부정:**
  - 스냅샷 캐시가 거래일 안에서 종목 거래정지 발생 시 즉시 반영 X. 다음 거래일까지
    풀에 남음 (현재 거래정지 종목은 KIS 가 응답 거부 → 진입 시도가 자연 차단).
- **후속:**
  - ADR-0003 (진입 검증 위양성 제거) 가 strength null/0 처리 정책을 같은 결로 정렬.
  - rank-api-top placeholder 는 다음 ADR 로 이어질 자리.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java`
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java`
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/ScreeningService.java`
  - `src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java`
- 관련 docs: `docs/stock-bot.md` (스크리닝 정책)
- 관련 커밋: `git log -1 d135907`, `docs/git_commit.md` Commit 3
