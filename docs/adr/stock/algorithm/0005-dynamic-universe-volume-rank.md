# ADR-0005: 동적 유니버스 — KIS 거래량순위 API

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-06-03 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 데이터 |
| 관련 ADR | ADR-0002 (스냅샷 캐시) 의 `rank-api-top` placeholder 를 구현 |
| 관련 이슈 | 운영 로그 분석: 5거래일 연속 `Selected: 0`, 매일 `rank=0, fallback=70` |

## Context — 무엇이 문제였나

ADR-0002 는 `UniverseBuilder` 를 도입하면서 KIS 순위 API 자리를 `rank-api-top`
placeholder 로만 남기고, 실제로는 정적 `fallback-codes` (대형주 70) 만 사용했다.

운영 로그(2026-05-27 ~ 06-02)에서 드러난 결과:

- 매 거래일 `Universe refreshed: 70 codes (pinned=0, fallback=70, rank=0)` — 동적
  소스가 한 번도 동작하지 않고 늘 동일한 대형주 70개.
- 대형주는 장 초반 갭이 거의 없어(예: `[161390] gap=0.4367%`) 스크리닝 Gap 플로어
  (0.5%) 에서 23~50개가 즉시 탈락. **Gap & Pullback 전략이 요구하는 강세 모멘텀 종목을
  유니버스가 구조적으로 담지 못함** → `Selected: 0` 이 매일 반복.

즉 placeholder 가 채워지지 않아 봇이 "볼 종목" 자체가 잘못되어 있었다.

## Decision — 무엇을 골랐나

KIS **거래량순위**(`/uapi/domestic-stock/v1/quotations/volume-rank`, TR `FHPST01710000`)
상위 N 을 그날의 동적 유니버스로 사용한다.

- `KisRestClient.getTopVolumeCodes(int count)` — 거래량순위 호출 + 단축코드
  (`mksc_shrn_iscd`) 파싱. **어떤 실패(미설정/비정상 rt_cd/예외/0건)든 빈 리스트 반환.**
- `UniverseBuilder.refresh()` 우선순위: `pinned` ∪ **거래량순위 상위 N** ∪ (rank 가
  비었을 때만) `fallback-codes`.
- `stock.universe.rank-api-top` 기본 **30** 으로 활성화 (`0` 이면 비활성, 폴백만 사용).
- **거래일 1회만 호출**: `executePreMarketLoop()`(08:30) 에서 `refresh()` 가 호출되어
  스냅샷 캐시 → 같은 거래일 내 모든 스크리닝/트레이딩은 동일 유니버스 사용.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 채택/기각 |
|---|---|---|
| 정적 fallback 유지 | 단순·안정 | 기각 — 대형주 편중으로 갭 종목 부재 (현재 장애의 원인) |
| 등락률순위(top gainers) | 갭 종목 직접 포착 | 보류 — 장 초반 변동 심해 풀이 불안정. 후속 보강 후보 |
| **(선택) 거래량순위 상위 N, 거래일 1회 스냅샷** | 유동성·활성 종목 풀 확보, 폴백 안전망, ADR-0002 정합성 유지 | 채택 |

- **ADR-0002 의 "동적 빌드 기각" 우려를 위배하지 않음**: ADR-0002 는 *매 호출마다*
  동적 빌드(초당 폭증·점심 전후 풀 불일치)를 기각했다. 본 결정은 거래일 **1회**
  pre-market 호출 후 스냅샷 캐시이므로 같은 우려가 없다 — "동적이되 거래일 단위 고정".
- **무회귀 폴백**: rank 가 어떤 이유로든 비면 기존 fallback-codes 경로로 자동 회귀.
  KIS 장애나 TR/파라미터 문제 시에도 봇이 멈추지 않는다.
- 거래량 기준은 *유동성·체결가능성*을 보장. 갭 선별은 스크리닝의 gap/strength/시총/
  거래대금 floor 가 담당하므로 유니버스는 "활성 종목 풀" 역할에 집중.

## Consequences — 영향

- **긍정:**
  - 매일 시장 상황을 반영한 변동성·유동성 종목 풀 → `Floor passed > 0`, `Selected > 0`
    가능. (P0-3 체결강도 수정과 합쳐져 스크리닝 깔때기가 비로소 후보를 배출.)
  - `rank` 카운트가 로그/`UNIVERSE_BUILT` 이벤트에 노출되어 동적 소스 동작을 관측 가능.
- **부정 / 주의:**
  - **라이브 검증 필요**: TR `FHPST01710000`, FID 파라미터, `mksc_shrn_iscd` 응답 필드는
    공식 샘플 기준으로 구현했으나 실계좌 응답으로 1회 확인 권장. 틀려도 폴백으로 안전.
  - pre-market(08:30) 호출 시 거래량순위는 *직전 세션* 기준일 수 있음 — 유동성 풀로는
    충분하나, 당일 갭 종목 즉시 반영이 필요하면 등락률순위 추가(후속) 검토.
- **후속:**
  - 등락률순위 병합(top gainers ∪ top volume) 로 갭 종목 적중률 향상 여지.
  - `FID_BLNG_CLS_CODE`(거래량/거래금액/회전율) 튜닝.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java` (`getTopVolumeCodes`)
  - `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KoreaInvestmentApiClient.java`
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java`
  - `src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java`
- 설정: `stock.universe.rank-api-top` (application.yaml)
- 외부: KIS Open API 거래량순위 [v1_국내주식-047], 공식 샘플 `koreainvestment/open-trading-api`
