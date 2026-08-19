# ADR-0007: 체결강도 데이터 소스를 inquire-ccnl(FHKST01010300)의 tday_rltv 로 전환

| 항목 | 값 |
|---|---|
| 상태 | Accepted (supersedes `d8b0158` 의 P0-3 cttr 매핑 결정) |
| 날짜 | 2026-07-27 |
| 도메인 | stock |
| 관심사 | 인프라, 알고리즘 |
| 관련 커밋 | `d8b0158`(superseded), `4d71037`(계측) |

## Context — 무엇이 문제였나

- 2026-07-27 09:20 스크리닝에서 유니버스 30종목 **전부** `체결강도(cttr) 미집계 응답 — raw
  cttr=null` WARN 후 dataInsufficient 19 + gap 11 = Selected 0 으로 전멸했다. `raw cttr=null`
  은 파싱 실패가 아니라 **응답에 키 자체가 없음**을 뜻한다 (`4d71037` 이 심은 계측이 입증).
- 코드는 주식현재가 시세(`FHKST01010100`, `/uapi/domestic-stock/v1/quotations/inquire-price`)
  응답의 `cttr` 필드를 체결강도로 파싱했다. KIS 공식 GitHub(koreainvestment/open-trading-api,
  `examples_llm/domestic_stock/inquire_price`)의 응답 필드 82개를 확인한 결과 **`cttr` 는
  존재하지 않는다**. 과거 폴백이던 `seln_cntg_smtn`/`shnu_cntg_smtn` 도, `inquire-price-2`
  (`FHPST01010000`, 54필드)에도 없다.
- 이력: 최초 구현은 `seln_cntg_smtn`/`shnu_cntg_smtn` 계산식(항상 0 버그) → `d8b0158`(P0-3)이
  `cttr` 직접 매핑으로 "수정"했으나 그 역시 존재하지 않는 필드였다. **같은 원인의 사고 2회** —
  스펙 미검증 필드 매핑.
- 영향은 스크리닝 전멸에 그치지 않는다: `KoreaInvestmentApiClient.getTradeStrength()` 를 쓰는
  진입 검증(PullbackDetectionService 조건 1)도 상시 FAIL 이었다 — soft 2/3 검증에서 나머지
  2개(호가 불균형·풀백 지속)가 모두 통과해야만 진입되는 상태로 운영되고 있었다.

## Decision — 무엇을 골랐나

체결강도는 **주식현재가 체결(`FHKST01010300`, `/uapi/domestic-stock/v1/quotations/inquire-ccnl`)
응답의 `tday_rltv`(당일 체결강도, 최신 체결 행)** 에서 얻는다.

- `KisRestClient.getTradeStrength(stockCode)` 신설 — `executeGetWithRetry`(GET 재시도 유지) +
  `Semaphore(8)` 게이트 경유. 최신(첫) 행의 `tday_rltv` 반환, 실패/빈 배열/필드 부재는 **null**
  (0 이 아님 — 미집계와 "강도 0" 을 구분).
- `KisQuoteResponse` 에서 존재하지 않는 필드 3개(`cttr`, `seln_cntg_smtn`, `shnu_cntg_smtn`)와
  `calculateTradeStrength()` 를 제거 — 이번 전환으로 호출처가 전부 이동해 "새롭게 미사용",
  같은 사고의 3회째를 구조적으로 차단.
- 스크리닝은 Floor 2(갭) **통과 후에만** 체결강도를 조회(탈락 종목에 콜 낭비 없음, 로그 기준
  +11콜 수준). null 은 기존 `skipZeroStrength` 경로로 dataInsufficient 처리(무회귀).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| `getQuote()` 가 ccnl 을 동반 호출해 DTO 에 합성 | 호출측 무변경 | getQuote 호출처(5초 리스크 루프 등) 대부분은 체결강도 불필요 — 트레이딩 루프 콜 수 2배 |
| 체결강도 상위 랭킹(`FHPST01680000`, ranking/volume-power) 조인 | 1콜로 다수 종목 | top-N 랭킹이라 임의 종목 조회 불가 — 유니버스 종목이 랭킹 밖이면 값 없음 |
| **별도 메서드 `getTradeStrength()` (선택)** | 필요한 곳(스크리닝·진입검증)만 +1콜 | — |
| `skipZeroStrength=false` 로 우회 | 코드 무변경 | 전 종목 강도가 floor 보정값(95)으로 뭉개져 strengthScore=0 — 신호 변별력 상실, 원인 미해결 |

## Consequences — 영향

- **긍정:** 스크리닝 Floor 3·진입 검증 조건 1이 실데이터로 동작. 존재하지 않는 필드 파싱
  코드 제거로 동일 사고 재발 경로 차단. 미집계(null)와 강도 0 이 구분됨.
- **부정:** 스크리닝 시 갭 통과 종목당 KIS GET +1 (Semaphore 8 내, 09:20 1회성이라 무해).
  진입 검증은 기존에도 getQuote 1콜이었으므로 콜 수 불변(소스만 교체).
- **후속(해소):** `tday_rltv` 필드명은 2026-08-03 운영 로그로 확인됐다. 그날 유일한 유니버스
  종목이 Floor 를 통과해 `str=164.61` 로 스코어링됐는데, `skip-zero-strength=true` 에서는
  체결강도가 없거나 0 이면 그 자리에서 탈락하므로 실API 가 실제 값을 반환했다는 뜻이다.
  LIVE 전환 금지는 이 검증과 별개로 PAPER 실측(리뷰 P2-5)이 전제이며, 기본 PAPER 정책
  ([modes/0002](../modes/0002-paper-default-mode.md))이 그대로 유지된다.

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java`,
  `.../dto/KisQuoteResponse.java`, `.../application/service/ScreeningService.java`
- 관련 테스트: `KisRestClientTradeStrengthTest`, `ScreeningFloorStrengthTest`
- KIS 공식 스펙: github.com/koreainvestment/open-trading-api `examples_llm/domestic_stock/`
  (`inquire_price` 82필드 / `inquire_price_2` 54필드 — 체결강도 없음, `inquire_ccnl` — `tday_rltv` 존재)
- 관련 ADR: [algorithm/0003](../algorithm/0003-entry-validation-strictness.md)(진입 검증),
  [algorithm/0008](../algorithm/0008-screening-selection-and-cost-model.md)(스크리닝 선정),
  [infrastructure/0001](0001-kis-rate-limit-semaphore.md)(호출량 게이트)
