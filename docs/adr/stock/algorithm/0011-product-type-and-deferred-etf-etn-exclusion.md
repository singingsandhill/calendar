# ADR-0011 (stock/algorithm): 상품 유형(STOCK/ETF/ETN) 저장 + 일일 리포트 분해 — ETF·ETN 제외는 보류

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-23 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 유니버스 |
| 관련 ADR | [infrastructure/0008 종목명 소스(search-stock-info)](../infrastructure/0008-stock-name-source-search-stock-info.md) 의 같은 응답을 재사용, [modes/0004 PAPER 원장·KRX 호가단위](../modes/0004-paper-cash-ledger.md) 의 후속, [algorithm/0005 거래량순위 유니버스](0005-dynamic-universe-volume-rank.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 → 보류로 재분류 (`docs/prompts/trading-fix-2026-10-20.md` 작업 7) |

## Context — 무엇이 문제였나

- 진단 초안은 유니버스에서 ETF·ETN 을 빼자고 했다. 그러나 8/3~10/20 사후 추정(11:20 보유, SL/TP 미적용)은 반대 방향이다:
  ETF·ETN n=16 평균 +0.75% vs 개별주 n=123 -0.64%, 인버스 n=5 +2.05%. 1,000원 미만 n=12 +1.01%. 표본이 작고 출구 규칙도
  적용하지 않은 추정이라 어느 쪽으로도 결론이 아니다.
- 그런데 판단에 필요한 데이터가 쌓이지 않고 있었다 — `Stock` 에 상품 유형이 없어 리포트를 유형별로 나눌 수 없고, PAPER 체결가의
  KRX 호가단위(modes/0004)도 ETF·ETN 을 주식 표로 반올림했다(ETF·ETN 은 2,000원 이상 5원, 주식은 5,000~20,000원 10원).
- 종목명을 위해 이미 부르는 주식기본조회(CTPF1002R, infrastructure/0008)의 output 에 `scty_grp_id_cd`(증권그룹ID코드)가 있다
  (KIS 공식 샘플 `chk_search_stock_info.py` 의 필드 목록, 2026-10-23 KIS 개발자센터 문서의 코드표: ST.주권 · KN.코넥스주권 ·
  EF.ETF · FE.해외ETF · EN.ETN · EW.ELW · RT.부동산투자회사 …). 추가 호출 없이 유형을 얻을 수 있다.

## Decision — 무엇을 골랐나

제외는 하지 않는다(기본 동작 불변). 유형을 저장하고 매일 분해해 근거를 쌓는다.

- **`Stock.productType`** — `StockProductType.fromSecurityGroupCode`: ST·KN → STOCK, EF·FE → ETF, EN → ETN. 호가 규칙이 확인되지 않은
  나머지 코드(ELW·리츠·투자회사 등)와 부재는 null(모름). `KisRestClient.getStockInfo` 가 종목명과 함께 돌려주고(`KisStockInfo`), 필드가
  없으면 outputKeys 를 WARN(infrastructure/0008 의 계측과 같은 방식). 스크리닝이 Floor 통과 종목에 1회 조회 — 호출 수 불변.
- **스키마:** `stock_monitoring.product_type` nullable VARCHAR(10). 백필 없음 — 종목 행은 거래일마다 새로 만들어지고 과거 행은 모름(null)으로 둔다.
- **PAPER 체결:** `KrxTickSize` 에 종목의 실제 유형을 넘긴다 — 매수는 `openPosition` 의 `Stock`, 매도는 포지션의 `stockId` 로 찾은
  종목(없으면 주식 표).
- **일일 리포트:** `DailyReport.productTypeBreakdown` — STOCK/ETF/ETN/UNKNOWN 별 감시 종목·진입 시도·청산·승·실현손익. 메일 "상품
  유형별" 표, `DAILY_REPORT` 이벤트 `productTypes`.
- **`stock.universe.excluded-product-types`** (기본 `[]`) — 든 유형은 스크리닝에서 뺀다(`productTypeExcluded` 요약). 모르는 유형은 빼지
  않는다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| ETF·ETN 제외 (진단 초안) | 단순 | 데이터가 반대 방향(n=16 +0.75% vs n=123 -0.64%) — 근거 없이 수익 후보를 버린다 |
| 아무것도 안 함 | 변경 없음 | 판단 근거가 계속 안 쌓이고 PAPER 호가 오차도 남는다 |
| 종목코드 규칙으로 추정 (ETN = Q 접두) | 호출 불필요 | ETF 는 코드로 구분 불가. 같은 응답에 공식 필드가 있다 |
| **유형 저장 + 분해 + 기본 빈 제외 키 (선택)** | 근거 축적, 호출 수 불변 | 컬럼 1개·설정 키 1개 |

## Consequences — 영향

- **긍정:** PAPER 체결 100건 이상이 쌓이면 유형별 승률·손익으로 제외 여부를 다시 판단할 수 있다(그때 새 ADR). ETF·ETN PAPER 체결가가
  실제 호가단위를 따른다(modes/0004 후속 해소).
- **부정:** 유형을 모르는 종목(조회 실패·미매핑 코드)은 UNKNOWN 으로 묶인다. 리츠·투자회사가 자주 걸리면 UNKNOWN 이 커진다 — 코드표의
  호가 규칙을 확인한 뒤 매핑을 넓힌다.
- **후속:** 주식 전략 파라미터(갭 구간·풀백 깊이)도 바꾸지 않았다 — 같은 재평가 시점에 함께 본다.

## References

- 코드: `StockProductType.fromSecurityGroupCode`, `KisRestClient.getStockInfo`, `KisStockInfo`, `ScreeningService.isExcludedProductType`,
  `StockPositionService.simulatedBuyPrice/simulatedSellPrice/productTypeOf`, `DailyPerformanceReportService.productTypeBreakdown`,
  `StockMailService`(상품 유형별 표), `StockProperties.Universe.excludedProductTypes`
- 테스트: `StockProductTypeTest`, `KisRestClientStockNameTest`, `ScreeningStockNameTest`, `StockPersistenceRestoreTest.stockProductTypeRoundTrips`,
  `StockPositionServiceTest`(ETF 호가 2건), `DailyPerformanceReportServiceTest.breaksDownByProductType`, `StockMailServiceTest`
- 외부: github.com/koreainvestment/open-trading-api `examples_llm/domestic_stock/search_stock_info/chk_search_stock_info.py`,
  apiportal.koreainvestment.com 주식기본조회 문서(증권그룹ID코드 표)
