# ADR-0008: 종목명 데이터 소스를 주식기본조회(CTPF1002R, search-stock-info)로 추가

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-31 |
| 도메인 | stock |
| 관심사 | 인프라, 관측성 |
| 관련 커밋 | `91ce7b6`(placeholder `// 종목명은 추후 업데이트` 도입), `3906844`(TODO 주석 삭제 + score/legacy 두 곳으로 복제) |

## Context — 무엇이 문제였나

- 2026-08-31 09:20 스크리닝 메일의 종목 표가 `종목코드 Q530107 | 종목명 Q530107` 처럼 두 칸에
  같은 값을 인쇄했다. 대시보드 워치리스트(`/api/stock/monitoring/active` → `dashboard.html` 의
  `Code | Name`)도 동일. 포지션·거래·시그널·이벤트·일일 리포트·history 는 종목명 필드가 아예 없어 무관.
- 직접 원인은 `ScreeningService` 의 두 생성 지점(score 경로·legacy 경로)이
  `new Stock(stockCode, stockCode, tradingDate)` 로 **이름 자리에 코드를 넣는 placeholder** 였다.
  `Stock.stockName` 은 final·non-blank 라 "일단 코드라도" 넣어야 생성이 됐고, 이후 어떤 경로도
  이름을 고치지 않는다 — `StockRepositoryAdapter.updateEntity` 는 이름을 갱신하지 않고
  `StockJpaEntity.setStockName` 은 호출처가 0 이다.
- 그 시점에 이름이 없는 구조적 이유: 스크리닝이 종목당 부르는 TR 3종 —
  시세(`FHKST01010100`, inquire-price)·체결(`FHKST01010300`, inquire-ccnl)·호가(`FHKST01010200`) —
  의 응답에는 **종목명 필드가 없다**(inquire-price 는 `bstp_kor_isnm`(업종명)·`rprs_mrkt_kor_name`(시장명)뿐).
  `KisQuoteResponse` 에 이름이 없는 것은 DTO 누락이 아니라 원천 부재다.
- 이름이 오는 TR 은 있으나 쓸 수 없었다: 거래량순위(`FHPST01710000`)는 행에서 `mksc_shrn_iscd` 만 꺼내
  `List<String>` 으로 반환(정적 폴백 70종목·pinned 는 이 응답을 거치지 않음), 잔고·당일주문체결의
  `prdt_name` 은 보유/주문 종목만 — 스크리닝 후보에 적용 불가.
- 이력: 최초 커밋 `91ce7b6` 에 `// 종목명은 추후 업데이트` 주석과 함께 들어왔고 `3906844` 가 리팩터링
  중 주석을 지우며 두 곳으로 복제했다. 어떤 테스트도 `stockName` 을 단언하지 않아 CI 에 보이지 않았고,
  `docs/stock/bot.md` §10 은 메일 표를 `종목코드 | 종목명 | …` 으로 명세하고 있어 문서와 동작이 어긋났다
  — 결정이 아닌 결함.

## Decision — 무엇을 골랐나

종목명은 **주식기본조회(`CTPF1002R`, `GET /uapi/domestic-stock/v1/quotations/search-stock-info`,
`PDNO=<종목코드>&PRDT_TYPE_CD=300`)** 응답의 `prdt_abrv_name`(약칭)에서 얻고, 없으면 `prdt_name`.

- `KisRestClient.getStockName(stockCode)` 신설 — `getTradeStrength` 와 같은 골격:
  `isConfigured()` 가드, `buildAuthHeaders("CTPF1002R")`, `executeGetWithRetry`(GET 재시도·
  `Semaphore(8)` 게이트·`apiCallsLast5min` 집계 경유), 반환은 DTO 없이 `String`.
- **fail-soft**: 실패/`rt_cd≠0`/이름 필드 부재는 **null**. `ScreeningService.resolveStockName` 이 null 을
  종목코드로 대체(종전 동작)하므로 선정 결과에는 영향이 없다. 필드 부재 시 `outputKeys` 를 WARN 으로
  남긴다 — 스펙 미검증 필드 매핑 사고 2회 전력([0007](0007-trade-strength-source-inquire-ccnl.md) 의 `cttr`)
  때문에, 필드명이 틀려도 화면 파손 없이 다음 거래일 로그로 판별되게 한다.
- 호출은 **Floor 통과 종목만**(콜 예산 = floorPassed 건, ADR 0007 과 같은 원칙). 요약에
  `ScreeningStats.nameUnresolved` 카운터 → 0 초과일 때만 WARN 1줄 + `SCREENING_SUMMARY` 이벤트 키.
  `StockBotMetrics.recordScreeningResult` 시그니처는 불변.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 정적 종목명 테이블(yaml/DB) | 호출 0 | 유니버스가 거래량순위 동적 소스라 커버 불가, 상장·변경 갱신 부담 |
| 거래량순위 응답의 `hts_kor_isnm` 재사용 | 추가 호출 0, TR 은 라이브 검증됨 | rank 소스 종목만 커버 — 정적 폴백 70종목·pinned 는 이름 없음(08-03 처럼 rank 1건인 날 커버리지 0); `getTopVolumeCodes` 계약·`UniverseBuilder` 스냅샷 변경 범위가 큼; 필드명도 미검증 |
| 잔고/당일주문체결의 `prdt_name` | 이미 파싱 중 | 보유·주문 종목만 — 진입 전 후보에 적용 불가 |
| **CTPF1002R 종목기본조회 (선택)** | 전 소스 커버, Floor 통과 종목당 GET +1, 변경 지점이 생성 지점에 국한 | — |
| 종목명 컬럼 폐기 | 코드 최소 | 메일·대시보드 계약 변경, 문제 회피일 뿐 |

## Consequences — 영향

- **긍정:** 메일·대시보드·monitoring API 의 종목명이 실명으로 복원. 필드 부재 시 `outputKeys` 계측으로
  필드명 오추정을 로그만으로 판별. 회귀 가드 4종 — `KisRestClientStockNameTest`(엔드포인트·tr_id·
  파라미터·폴백·재시도·null 경로), `ScreeningStockNameTest`(이름 반영·fail-soft·콜 예산·legacy 경로),
  `StockMailServiceTest`(종목코드 셀 1회), `StockRepositoryAdapterTest`(이름 라운드트립).
- **부정:** 09:20 스크리닝 시 Floor 통과 종목당 KIS GET +1(1회성, Semaphore 8 내 — 시세·체결강도·호가에
  이어 4번째 콜). 실전투자 전용 TR 이라 `stock.kis.production=false` 환경에서는 항상 null → 코드 대체
  (별도 분기 없음 — fail-soft 로 충분).
- **미검증 가정:** 필드명 `prdt_abrv_name`/`prdt_name` 과 `PRDT_TYPE_CD=300` 은 공식 스펙 기준이며
  실API 로는 아직 확인 전이다. 첫 거래일 `SCREENING_SUMMARY.nameUnresolved` 가 `floorPassed` 와
  같으면(전 종목 미확보) TR/필드 매핑을 재검토한다. `stock_name` 컬럼 100자 vs 스펙 String(60) 은
  여유가 있어 truncation 방어는 넣지 않았다.
- **이미 저장된 행:** 거래일 단위 데이터(`stock_monitoring`)라 백필하지 않는다 — 다음 스크리닝부터 실명.
- **범위 밖:** 증상 종목 `Q530107` 은 ETN 이다. 거래량순위가 ETF/ETN 을 걸러내지 않는 문제
  (`FID_TRGT_EXLS_CLS_CODE="0000000000"`)는 [algorithm/0010](../algorithm/0010-universe-degradation-threshold.md)
  이 스펙 확인 전 보류한 항목으로, 이 ADR 이 다루지 않는다.

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java`(`getStockName`),
  `.../api/KoreaInvestmentApiClient.java`, `.../application/service/ScreeningService.java`(`resolveStockName`,
  `ScreeningStats.nameUnresolved`)
- 관련 테스트: `KisRestClientStockNameTest`, `ScreeningStockNameTest`, `StockMailServiceTest`, `StockRepositoryAdapterTest`
- KIS 공식 스펙: 국내주식 기본조회(주식기본조회, `CTPF1002R`) — `PDNO`·`PRDT_TYPE_CD` 입력,
  `prdt_name`/`prdt_abrv_name`/`prdt_eng_name` 출력; 시세 `inquire_price` 출력 82필드에 종목명 없음
- 관련 ADR: [infrastructure/0007](0007-trade-strength-source-inquire-ccnl.md)(스펙 미검증 필드 사고·fail-soft 계측),
  [infrastructure/0001](0001-kis-rate-limit-semaphore.md)(호출량 게이트),
  [algorithm/0005](../algorithm/0005-dynamic-universe-volume-rank.md)·[algorithm/0010](../algorithm/0010-universe-degradation-threshold.md)(유니버스 소스)
- 명세 문서: `docs/stock/bot.md` §10 이메일 구성
