# ADR-0006: 유니버스 거래량순위 — 스크리닝 시점 재시도

| 항목 | 값 |
|---|---|
| 상태 | Accepted — 재시도 조건 `rankApi == 0` 은 [ADR-0010](0010-universe-degradation-threshold.md) 으로 `rankApi < rank-api-top` 으로 보강. 08:30 `refresh()` 유지 결정도 0010 에서 `refreshStaticOnly()` 로 대체 |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 데이터 |
| 관련 ADR | ADR-0005 (거래량순위 동적 유니버스) 의 호출 시점을 보완, ADR-0002 (스냅샷 캐시) |
| 관련 이슈 | 운영 로그 분석 (2026-07-20 ~ 07-23): 매일 `Volume-rank returned 0 codes` → `rank=0, fallback=70` → `Selected: 0` |

## Context — 무엇이 문제였나

ADR-0005 는 거래량순위를 **pre-market(08:30) 1회** 호출해 스냅샷 캐시하도록 했다.
그러나 08:30 은 장 시작(09:00) 전이라 **당일 거래량 데이터가 존재하지 않는다**.

운영 로그(2026-07-20 ~ 07-23) 4거래일 연속:

- 08:30 `Volume-rank returned 0 codes` — 예외 없이 rt_cd=0 성공 + 0건.
- `Universe refreshed: 70 codes (pinned=0, fallback=70, rank=0)` — 동적 소스가 매일
  무력화되고 정적 대형주 폴백으로만 운영.
- 결과: ADR-0005 가 해결하려던 "대형주 편중 → 갭 종목 부재 → `Selected: 0`" 이
  그대로 재발. 가용 로그 전 기간에서 `Screening complete: N>0` 이 한 번도 없음.

ADR-0005 의 Consequences 가 "pre-market 호출 시 거래량순위는 직전 세션 기준일 수
있음" 이라고 예상했으나, 실제로는 **직전 세션도 아니고 아예 0건**이었다.

## Decision — 무엇을 골랐나

**스크리닝 시점(09:20) 에 스냅샷이 폴백 전용(rank=0)이면 거래량순위를 1회 재시도**한다.

- `UniverseBuilder.refreshIfDegraded(tradingDate)` 신설:
  - 스냅샷이 없거나 거래일이 다르면 → 기존 `refresh()` (기존 동작 유지).
  - 스냅샷의 `rankApi == 0` 이고 `rank-api-top > 0` 이면 → `refresh()` 재호출.
    09:20 에는 장중 20분치 거래량이 쌓여 순위 데이터가 존재한다.
  - rank 가 이미 있으면 스냅샷 그대로 반환 — **"거래일 1회 스냅샷"(ADR-0002) 정합성 유지**.
- `GapPullbackBotService.executeScreeningLoop()` 이 `currentUniverse()` 대신
  `refreshIfDegraded()` 를 호출.
- 08:30 pre-market 의 `refresh()` 는 유지 (토큰 예열 + 초기 스냅샷).
- 계측 추가: `KisRestClient.getTopVolumeCodes` 가 성공(rt_cd=0)인데 0건이면 rows 수와
  첫 행의 키 목록을 WARN — "응답 자체가 빈 것"(장 시작 전 정상)과 "코드 필드명 불일치"를
  로그만으로 판별.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 채택/기각 |
|---|---|---|
| pre-market 호출을 09:20 으로 이동 | 단순 | 기각 — 유니버스 없이 pre-market 이 끝나고, 스크리닝 실패 시 재시도 창구가 없음 |
| 매 스크리닝 무조건 재조회 | 항상 최신 | 기각 — rank 성공 시에도 재조회하면 ADR-0002 의 "거래일 단위 고정" 위배 |
| **(선택) rank=0 일 때만 스크리닝 시점 재시도** | 성공 시 스냅샷 불변, 실패 시에만 1회 추가 호출 | 채택 |

- 재시도도 0건이면 기존 폴백 경로 그대로 (무회귀).
- 추가 API 호출은 하루 최대 1회.

## Consequences — 영향

- **긍정:** 동적 유니버스가 실제 장중 데이터로 동작 → 갭 종목이 유니버스에 담길 수 있음.
  `rank>0` 로그로 동작 확인 가능.
- **주의:** 09:20 재시도가 성공하면 pre-market 스냅샷과 다른 유니버스로 스크리닝한다
  (의도된 동작 — 08:30 스냅샷은 폴백 전용이었으므로 잃는 것이 없음).
- **후속:** 다음 거래일 로그에서 `Universe refreshed ... rank=30` 확인. 여전히 0건이면
  WARN 의 firstRowKeys 로 필드명/파라미터 문제를 추적.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java` (`refreshIfDegraded`)
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java` (`executeScreeningLoop`)
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java` (`getTopVolumeCodes` 계측)
  - `src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java` (refreshIfDegraded 테스트)
- 운영 로그: `logs/stock-trading-2026-07-20 ~ 23.log` (09:20 스크리닝 메일 첨부 스냅샷)
