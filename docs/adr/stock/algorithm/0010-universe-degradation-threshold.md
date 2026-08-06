# ADR-0010: 유니버스 열화 판정 — "비었을 때" 가 아니라 "요청 top-N 미달"

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-03 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 데이터 |
| 관련 ADR | [ADR-0005](0005-dynamic-universe-volume-rank.md) 의 폴백 조건과 [ADR-0006](0006-universe-rank-retry-at-screening.md) 의 재시도 조건을 보강, [ADR-0002](0002-universe-builder-snapshot.md) (거래일 1회 스냅샷) |
| 관련 이슈 | 운영 로그 `logs/stock-screening-snapshot-2026-08-03.log` — 하루치 유니버스 1종목 |

## Context — 무엇이 문제였나

ADR-0005 는 폴백 발동 조건을 "미설정 / 비정상 rt_cd / 예외 / **0건**" 으로 열거했고,
ADR-0006 은 스크리닝 시점 재시도 조건을 `rankApi == 0` 으로 명시했다. 두 규칙 모두
**"비어 있는가"** 만 검사한다.

2026-08-03, 거래량순위가 30건 요청에 **1건**을 반환했다:

```
08:30:01  Volume-rank returned 1 codes
08:30:01  Universe refreshed for 2026-08-03: 1 codes (pinned=0, fallback=0, rank=1)
09:20:00  Starting gap screening for 1 stocks
09:20:01  [252670] Score=54.7 (...) gap=9.7561%
09:20:01  Total: 1, Floor passed: 1, Selected: 1
```

1건은 "비어 있지 않다". 그래서:

- `UniverseBuilder.refresh()` 의 `usedFallback = rankCodes.isEmpty()` 가 `false` →
  **70종목 정적 안전망을 통째로 건너뛴다.**
- `refreshIfDegraded()` 의 `s.rankApi == 0` 이 `false` → **ADR-0006 의 09:20 재시도도
  발동하지 않는다.**

즉 *성공한* API 호출 하나가 안전망 두 개를 동시에 껐다. 그날의 스크리닝은 유일하게 남은
1종목(레버리지·인버스 계열 ETF)으로 진행됐고, 이 사실을 알리는 WARN 은 한 줄도 없었다 —
`KisRestClient` 의 축소 응답 로그가 DEBUG 였기 때문이다.

대칭 위험도 있었다. 2026-07-20 형태의 날(08:30 rank 0건 → fallback 70)에 09:20 재시도가
3건만 반환하면 유니버스가 70 → 3 으로 **줄어든다.** 재시도가 폴백을 *대체*하는 구조라서다.

## Decision — 무엇을 골랐나

**열화(degraded) 의 정의를 "비었을 때" 에서 "요청한 top-N 에 미달할 때" 로 바꾸고,
부분 응답은 폴백을 대체하지 않고 합집합으로 보강한다.**

- `UniverseBuilder.refresh()`:
  `usedFallback = rankCodes.size() < Math.max(rank-api-top, 1)`
  `Math.max(..,1)` 은 rank 비활성(`rank-api-top=0`) 일 때 `0 < 1` 이 되어 기존 폴백 동작을
  그대로 유지한다.
- `UniverseBuilder.refreshIfDegraded()`: `s.rankApi < rank-api-top` 이면 재시도.
- **프리마켓(08:30)은 거래량순위를 아예 호출하지 않는다** — `refreshStaticOnly()` 신설.
  그 시각엔 당일 거래량이 없어 rank 가 구조적으로 0~1건이고(ADR-0006 이 관측한 그대로),
  그 결과가 스냅샷에 남는 것 자체가 오염원이다. 동적 소스는 09:20 `refreshIfDegraded()`
  단독 책임.
- **축소 응답 경보:** `KisRestClient.getTopVolumeCodes` 가 요청보다 적게 받으면 WARN,
  `UniverseBuilder` 가 `UNIVERSE_DEGRADED` 이벤트(requested/returned/fallback)를 남긴다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 채택/기각 |
|---|---|---|
| 최소 유니버스 크기 설정 키 신설 (`min-universe-size`) | 명시적 | 기각 — `rank-api-top` 이 이미 "몇 개를 원하는가"를 담고 있다. 같은 뜻의 키를 둘로 늘리면 드리프트 지점만 는다 |
| 부분 응답이면 rank 를 통째로 버리고 폴백만 사용 | 단순 | 기각 — 1건이라도 그날 거래량 1위는 갭 전략에 유효한 정보다. 버릴 이유가 없다 |
| **(선택) top-N 미달이면 폴백을 합집합으로 보강 + 09:20 재시도** | 안전망이 항상 하한을 보장, 부분 정보도 보존 | 채택 |

- `codes` 가 이미 `LinkedHashSet` 이라 합집합의 중복 제거는 공짜다.
- 완전 응답(30/30)일 때의 동작은 종전과 동일 — rank 가 폴백을 대체한다. "거래일 1회
  스냅샷"(ADR-0002) 정합성도 그대로다.

## Consequences — 영향

- **긍정:** 유니버스 크기의 하한이 정적 안전망 크기로 보장된다. 부분 응답·축소 응답이
  WARN + `UNIVERSE_DEGRADED` 이벤트로 드러난다.
- **변경:** ADR-0005 의 "rank 가 비었을 때만 fallback" 규칙이 "rank 가 미달일 때 fallback"
  으로 바뀐다. 부분 응답 시 유니버스가 종전보다 커진다(1 → 71).
- **주의:** 프리마켓 스냅샷에는 이제 rank 가 절대 포함되지 않는다(`rank=0` 이 정상).
  09:20 이전에 `currentUniverse()` 를 읽는 경로는 정적 유니버스를 본다.
- **후속:** ETF/ETN 이 거래량순위에 섞여 들어오는 문제(08-03 의 `252670`)는 이 ADR 범위
  밖이다. `FID_TRGT_EXLS_CLS_CODE` 는 현재 `"0000000000"`(제외 없음)이며, 비트마스크
  자릿수를 KIS 공식 스펙으로 확인하기 전에는 손대지 않는다 — 스펙 미검증 필드 매핑으로
  두 번 사고가 난 전례가 있다(ADR infrastructure/0007 의 `cttr`).

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java`
    (`refresh`, `refreshStaticOnly`, `refreshIfDegraded`)
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java`
    (`executePreMarketLoop`)
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java`
    (`getTopVolumeCodes` 축소 응답 WARN)
  - `src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java`
    (`unionsStaticPoolWhenRankResponseIsPartial`, `retriesRankAtScreeningWhenSnapshotRankIsPartial`,
    `refreshStaticOnlySkipsRankApiEntirely`)
- 운영 로그: `logs/stock-screening-snapshot-2026-08-03.log`
