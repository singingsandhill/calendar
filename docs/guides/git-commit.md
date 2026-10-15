### =====================================================================
### git-commit.md — 멀티-PR 커밋 시퀀스 (Windows 호환 형식)
###
### 형식
### - cmd.exe / PowerShell / Git Bash 모두 동작.
### - git add 는 한 줄 (백슬래시 줄바꿈 X), 멀티라인 메시지는 -m 여러 번.
### - 따옴표는 모두 큰따옴표("); cmd.exe 는 작은따옴표 미지원.
###
### 작성 원칙 (이번 정리에서 적용)
### 1) 동일 파일이 여러 커밋에 등장할 때, working-tree 일괄 보유 환경에서는
###    git add -p 없이 분리 커밋 불가. 따라서 "처음 등장하는 커밋이 그 파일을
###    소유" (= 이후 커밋의 add 목록에서 제거). 흡수된 변경은 각 커밋의
###    "주의:" 주석에 명시.
### 2) 모든 파일이 흡수되어 빈 add 목록이 되는 커밋은 삭제 + 흡수처에 메모.
### 3) 이미 main 에 들어간 커밋은 ✅ DONE 마커만 남기고 실행 블록 제거.
### 4) git status 에 있지만 어느 커밋에도 없던 누락 파일은 가장 의미가 가까운
###    커밋에 편입 (특히 Reserved Owner ID 관련 + CLAUDE.md / ADR).
### =====================================================================

# 완료 commit 생략. 이후는 commit 규칙 유지를 위해 남김

# =====================================================================
# stock 봇 — 종목명 데이터 소스 추가 (2026-08-31)
# =====================================================================
# 배경: 09:20 스크리닝 메일·대시보드 워치리스트의 종목명 칸에 종목코드가 인쇄됐다(Q530107 | Q530107).
#   ScreeningService 두 생성 지점이 new Stock(code, code, date) 였고(최초 커밋 91ce7b6 의 placeholder
#   // 종목명은 추후 업데이트 — 3906844 가 주석을 지우며 복제), 스크리닝이 부르는 시세·체결·호가 TR 3종엔
#   종목명 필드가 없어 대체할 값 자체가 없었다. 어떤 테스트도 stockName 을 단언하지 않아 CI 에 보이지 않았다.
# 사용자 결정: 주식기본조회(CTPF1002R, search-stock-info) 로 Floor 통과 종목만 조회, 실패는 종목코드 대체(fail-soft).
# 전 항목 TDD(RED 선확인 → GREEN): 클라이언트 신설은 컴파일 RED(7건 cannot find symbol), 스크리닝은 단정 RED
#   (expected 삼성전자 but was 005930 — score·legacy 경로 2건), 메일·라운드트립 단정은 가드(처음부터 GREEN).
# 전체 스위트 113 클래스 / 698 테스트 / failures 0.

# Commit 166 — ✅ DONE() fix(stock): 종목명 소스로 주식기본조회(search-stock-info) 추가 — 메일·대시보드 종목명 칸 종목코드 인쇄 수정 (ADR stock/infrastructure/0008) — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KoreaInvestmentApiClient.java src/main/java/me/singingsandhill/calendar/stock/application/service/ScreeningService.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClientStockNameTest.java src/test/java/me/singingsandhill/calendar/stock/application/service/ScreeningStockNameTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockMailServiceTest.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/persistence/StockRepositoryAdapterTest.java docs/adr/stock/infrastructure/0008-stock-name-source-search-stock-info.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/application/CLAUDE.md docs/stock/bot.md docs/guides/git-commit.md
git commit -m "fix(stock): 종목명 소스로 주식기본조회(search-stock-info) 추가 — 메일·대시보드 종목명 칸 종목코드 인쇄 수정 (ADR stock/infrastructure/0008)" -m "증상: 09:20 스크리닝 메일 종목 표와 대시보드 워치리스트(/api/stock/monitoring/active)의 종목명 칸에 종목코드가 인쇄됐다. 원인: ScreeningService 의 score·legacy 두 생성 지점이 new Stock(stockCode, stockCode, tradingDate) 로 이름 자리에 코드를 넣는 placeholder 였고(최초 커밋 91ce7b6 의 // 종목명은 추후 업데이트 주석을 3906844 가 리팩터링 중 지우며 두 곳으로 복제), 스크리닝이 종목당 부르는 시세(FHKST01010100)·체결(FHKST01010300)·호가(FHKST01010200) TR 응답에는 종목명 필드가 아예 없어 DTO 누락이 아니라 원천 부재였다. Stock.stockName 은 final·non-blank 라 이후 어떤 경로도 고치지 않았고(updateEntity 는 이름 미갱신, setStockName 호출처 0), 거래량순위 응답의 종목명은 getTopVolumeCodes 가 코드만 꺼내 버리며 잔고·주문체결의 prdt_name 은 보유 종목만이라 후보에 쓸 수 없었다. 결정(ADR stock/infrastructure/0008): KisRestClient.getStockName 신설 — CTPF1002R GET /uapi/domestic-stock/v1/quotations/search-stock-info (PDNO + PRDT_TYPE_CD=300) 의 prdt_abrv_name(약칭), 없으면 prdt_name. getTradeStrength 와 같은 골격으로 executeGetWithRetry(GET 재시도·Semaphore 8·apiCallsLast5min 집계) 경유, 반환은 DTO 없이 String. fail-soft — 실패/rt_cd≠0/필드 부재는 null 을 돌려주고 ScreeningService.resolveStockName 이 종목코드로 대체(종전 동작, 선정 무영향) + ScreeningStats.nameUnresolved 집계 → 0 초과일 때만 요약 WARN 1줄 + SCREENING_SUMMARY 이벤트 키. 필드 부재 시 outputKeys 를 WARN 으로 남긴다 — 스펙 미검증 필드 매핑 사고 2회 전력(ADR 0007 의 cttr) 때문에 필드명이 틀려도 화면 파손 없이 다음 거래일 로그로 판별되게 했다. 호출은 Floor 통과 종목만(콜 예산 = floorPassed, ADR 0007 과 같은 원칙). StockBotMetrics.recordScreeningResult 시그니처는 ScreeningStatsBucketTest 가 정확히 verify 하므로 불변. 검증: KisRestClientStockNameTest(MockWebServer — 엔드포인트·tr_id CTPF1002R·PDNO/PRDT_TYPE_CD·약칭 우선·prdt_name 폴백·5xx 재시도·rt_cd≠0/필드 부재/재시도 소진/미설정 null, 7건) 는 컴파일 RED 선확인 후 GREEN. ScreeningStockNameTest(이름 반영·null 폴백 시 선정 유지·갭 탈락 종목 미조회·legacy 경로, 4건) 는 expected 삼성전자 but was 005930 로 2건 RED 선확인 후 GREEN. StockMailServiceTest 에 종목코드 셀 1회(containsOnlyOnce) 가드, StockRepositoryAdapterTest 에 이름 라운드트립 단정을 추가(둘 다 가드 — 처음부터 GREEN). 이웃 ScreeningFloorStrengthTest·ScreeningStatsBucketTest·ScreeningSelectionTest 무회귀. 전체 스위트 113 클래스 698 테스트 failures 0. 잔여 위험: prdt_abrv_name/prdt_name·PRDT_TYPE_CD=300 은 공식 스펙 기준으로 실API 미확인 — 첫 거래일 SCREENING_SUMMARY.nameUnresolved 가 floorPassed 와 같으면 재검토. 실전 전용 TR 이라 production=false 면 항상 코드 대체(별도 분기 없음). stock_name 100자 vs 스펙 60자라 truncation 미추가. 기존 행 백필 없음(거래일 단위). 문서: ADR 0008 + README 두 뷰(stock 인프라 7→8, 합계 88→89, 시간순 행) + stock CLAUDE.md 2종(개행 정규화로 전체 diff, 실변경 +5/+3) + bot.md 이메일 구성 주석. 범위 밖: Q530107 은 ETN — 거래량순위 ETF/ETN 미제외는 ADR algorithm/0010 보류 항목이라 미수정."

# =====================================================================
# stock 봇 — KIS 신TR 전환 (2026-08-31)
# =====================================================================
# 배경: 사용자 요청으로 KIS 포털 공지(2026-08-27 Backtester·Strategy Builder 보안 업데이트)와 공식
#   GitHub(koreainvestment/open-trading-api) 의 해당사항 점검. 공지 자체는 해당 없음 — 본문이 '일반 API
#   호출만 이용하시는 경우에는 해당사항이 없다' 명시, 해당 커밋 b4e6249 변경 21파일 전부 backtester/·
#   strategy_builder/ 하위(이 저장소는 Python 도구·MCP·웹소켓 미사용). 대신 대조 과정에서 주문·체결조회
#   TR ID 가 구형임을 발견 — 공식 API 문서 tr_id 항목: '구TR은 사전고지 없이 막힐 수 있으므로 반드시
#   신TR로 변경이용 부탁드립니다.' 구TR 차단 시 LIVE 매도(손절·최종청산)가 실패해 포지션이 무보호로
#   남는 실자금 경로.
# 사용자 결정: 신TR 주문 바디의 EXCG_ID_DVSN_CD 를 KRX 로 명시(선택 항목·미입력 기본값 KRX 지만,
#   봇 시세가 KRX 전용 J 라 시세-주문 라우팅 결합을 코드에 고정).
# 판정 요약(그 외 전부 정합·변경 없음): 접근토큰 정책(24h·1일 1회·6h 내 재사용 — 로그 실측으로
#   expires_in=잔여시간 확인)·hashkey(공식 샘플상 선택, 유지)·조회 TR 7종·Commit 166 의
#   CTPF1002R(search-stock-info·PRDT_TYPE_CD=300 도 공식 샘플과 일치).
# TDD: KisRestClientTrIdTest 4건 RED(ArgumentsAreDifferent — 구TR 인자) 선확인 → GREEN.
#   전체 스위트 114 클래스 / 702 테스트 / failures 0 (166 의 698 + 신규 4).


# Commit 167 — ✅ DONE() fix(stock): KIS 주문·체결조회 신TR 전환 (TTTC0012U/0011U·TTTC0081R) + 주문 거래소 KRX 명시 — 마지막 커밋(git-commit.md 포함)
git add src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClientTrIdTest.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java docs/adr/stock/infrastructure/0005-non-idempotent-order-no-retry.md docs/adr/stock/infrastructure/0006-order-pre-persistence-and-fill-backfill.md docs/guides/git-commit.md
git commit -m "fix(stock): KIS 주문·체결조회 신TR 전환 (TTTC0012U/0011U·TTTC0081R) + 주문 거래소 KRX 명시" -m "근거: 공식 API 문서(주식주문(현금)[v1_국내주식-001]·주식일별주문체결조회) tr_id 항목이 '구TR은 사전고지 없이 막힐 수 있으므로 반드시 신TR로 변경이용'을 안내 — 매수 (구)TTTC0802U→(신)TTTC0012U, 매도 (구)TTTC0801U→(신)TTTC0011U, 체결조회 3개월이내 (구)TTTC8001R→(신)TTTC0081R (모의 VTTC 동일). 공식 GitHub 샘플(order_cash.py·inquire_daily_ccld.py)도 신TR 만 사용. 코드는 최초 커밋 91ce7b6 부터 javadoc 은 신TR·실행값은 구TR 로 불일치했다(KisRestClient placeBuyOrder/placeSellOrder/getOrderHistory 분기식 3곳) — 구TR 이 차단되면 LIVE 매도(손절·최종청산)가 실패해 포지션이 무보호로 남는 실자금 경로. 변경: 분기식 3곳 신TR 교체(모의 VTTC 포함), 주문 바디에 EXCG_ID_DVSN_CD=KRX 명시(사용자 결정 — 선택 항목·미입력 기본 KRX 지만 봇 시세가 전부 KRX 전용 FID_COND_MRKT_DIV_CODE=J 라 기본값 변경으로 주문만 SOR/NXT 로 새는 것을 차단. LinkedHashMap 이 hashkey 대상과 동일 객체라 hashkey 자동 반영, 문서상 모의투자는 KRX 만 가능해 production 분기 불필요), 낡은 주석 2곳(KisRestClient executePostNoRetry javadoc·StockPositionService resolveBuyFill javadoc 의 TTTC8001R→TTTC0081R) 동기화. 신TR 체결조회 응답 output1 에 mapToOrderDetail 이 쓰는 13개 필드(odno·pdno·prdt_name·sll_buy_dvsn_cd·ord_qty·ord_unpr·tot_ccld_qty·avg_prvs·ord_tmd·ord_gno_brno·orgn_odno·ord_dvsn_name·ccld_cndt_name) 전부 존재를 공식 문서에서 확인 — 매핑 불변. Commit 166 이 범위 밖으로 보고한 KisOrderDetailResponse javadoc(TTTC0081R) vs 코드(TTTC8001R) 불일치가 이것으로 해소된다(javadoc 쪽이 처음부터 옳았음). 검증: KisRestClientTrIdTest 신설(MockWebServer + verify(buildAuthHeaders) — 매수/매도/체결조회 신TR, production=false 의 VTTC 신TR, 주문 바디 EXCG_ID_DVSN_CD=KRX 포함, 4건) RED(ArgumentsAreDifferent — 구TR 인자) 선확인 후 GREEN. 전체 스위트 114 클래스 702 테스트 failures 0. 문서: 새 ADR 없음 — CLAUDE.md 동기화 표의 단순 사실 변경이고 CLAUDE.md 는 TR ID 미기재. ADR stock/infrastructure/0005·0006 Consequences 에 신TR 교체 각주. 잔여 위험: 신TR 은 공식 문서·샘플 기준으로 실API 미검증 — LIVE 첫 주문의 Order placed/Order failed [msg_cd] 로그로 확인 필요. 범위 밖(보고만): 모의투자 도메인(openapivts:29443)이 코드·설정에 없어 production=false 면 VTTC TR 이 실전 도메인으로 향한다 — PAPER 는 로컬 시뮬레이션(simulateOrder)이라 실영향 없음."

# =====================================================================
# runner — 현황 조회 3페이지 from/to 날짜 범위 필터 (2026-09-01)
# =====================================================================
# 배경: 사용자 요청 "runner 현황 조회에 날짜 filter 기능 추가". 대상 3페이지(출석 현황
#   /runners/members·런 목록 /runners/runs·어드민 대시보드 /runners/admin)와 형태(시작일~종료일
#   범위)는 사용자 선택. 종전엔 runner 모듈 전 목록 엔드포인트가 쿼리 파라미터 없이 전체 조회.
# 설계: stock history 의 from/to String 파라미터 + date input 패턴 재사용, 프리셋 없음. 잘못된
#   값은 무시(무제한) — WebConfig ignoreInvalidLocale 의 봇 스캔 500 방지 원칙. 리포는 non-null
#   BETWEEN + 서비스 sentinel(EPOCH/9999-12-31) 정규화 — 기존 range 쿼리 전례와 일치,
#   (:p IS NULL OR ...) 무전례 패턴 미도입. 파라미터 없으면 기존 무필터 쿼리 분기(종전 동작 동일).
# 설계(UI 2차): 초판이 기존 폼 클래스를 그대로 재사용한 결과가 주변과 어긋난다는 사용자 지적
#   ("필터가 주변 디자인과 안 맞는다") — 필터만 표면이 없고(주변은 --runners-bg-card + 1px border
#   + 12px 라운드), .runners-input(≈48px)과 .runners-btn-sm(≈33px) 높이가 15px 차, color-scheme
#   미선언으로 네이티브 달력 글리프·팝업이 라이트, 라벨이 페이지의 마이크로 라벨 관용구와 불일치,
#   480px 에서 .runners-btn{width:100%} 만 걸려 들쭉날쭉. 사용자 선택은 "패널 카드" — 주변 카드와
#   같은 재질의 패널로 재작성하되 새 색·라운드·그림자 없이 기존 토큰만 쓴다. 순수 presentation —
#   서버·쿼리·파라미터 계약·테스트가 검사하는 value 속성 무변경.
#   모바일: input[type=date] 는 min-content(≈174px) 아래로 줄지 않아 480px 이하 2단 배치는
#   375px 에서 가로 넘침을 만든다 — 그 구간만 세로 스택.
# TDD(RED 선확인 → GREEN): 페이지 테스트 9건 중 필터 6건 AssertionError RED·가드 3건 선GREEN,
#   서비스 단위 8건은 컴파일 RED(cannot find symbol). 전체 스위트 117 클래스 / 719 테스트 /
#   failures 0, RunnerSeoRenderingTest 무회귀.


# Commit 168 — ✅ DONE() feat(runner): 런 목록·출석 현황·어드민 대시보드에 from/to 날짜 범위 필터 — 마지막 커밋(git-commit.md 포함)
git add src/test/java/me/singingsandhill/calendar/runner/presentation/controller/RunnerDateFilterPageTest.java src/test/java/me/singingsandhill/calendar/runner/application/service/RunServiceDateFilterTest.java src/test/java/me/singingsandhill/calendar/runner/application/service/AttendanceServiceDateFilterTest.java src/main/java/me/singingsandhill/calendar/runner/domain/RunRepository.java src/main/java/me/singingsandhill/calendar/runner/domain/AttendanceRepository.java src/main/java/me/singingsandhill/calendar/runner/infrastructure/persistence/repository/RunJpaRepository.java src/main/java/me/singingsandhill/calendar/runner/infrastructure/persistence/repository/AttendanceJpaRepository.java src/main/java/me/singingsandhill/calendar/runner/infrastructure/persistence/adapter/RunRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/runner/infrastructure/persistence/adapter/AttendanceRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/runner/application/service/RunService.java src/main/java/me/singingsandhill/calendar/runner/application/service/AttendanceService.java src/main/java/me/singingsandhill/calendar/runner/presentation/controller/RunnerController.java src/main/java/me/singingsandhill/calendar/runner/presentation/controller/RunnerAdminController.java src/main/resources/templates/runners/run-list.html src/main/resources/templates/runners/member-list.html src/main/resources/templates/runners/admin/dashboard.html src/main/resources/static/css/runners.css src/main/java/me/singingsandhill/calendar/runner/application/CLAUDE.md docs/guides/git-commit.md
git commit -m "feat(runner): 런 목록·출석 현황·어드민 대시보드에 from/to 날짜 범위 필터" -m "사용자 요청 — runner 현황 조회에 날짜 필터. 대상 3페이지(런 목록 /runners/runs·출석 현황 /runners/members·어드민 대시보드 /runners/admin)와 시작일~종료일 범위 형태는 사용자 선택. 파라미터 계약: from/to optional String, 잘못된 값·공백은 무시하고 무제한 취급(WebConfig ignoreInvalidLocale 과 같은 봇 스캔 500 방지 원칙 — stock history 의 raw LocalDate.parse 와 달리 lenient), from 이 to 보다 뒤면 BETWEEN 이 자연히 빈 목록. 파라미터 없으면 기존 무필터 쿼리로 분기해 종전 동작 동일. 날짜 기준은 Run.date 단일 — Attendance 는 자체 날짜가 없어 출석 현황 집계는 a.run.date 조인 BETWEEN 을 기존 GROUP BY 집계 JPQL 에 추가한 신규 메서드로 수행(positional Object[] 매핑 동일 유지). 헥사고날 규약대로 애그리거트당 3파일(JPA repo·adapter·domain port) 확장, 포트 계약은 non-null 경계로 단순화하고 null 무제한은 서비스가 sentinel(EPOCH/9999-12-31 — LocalDate.MIN/MAX 는 H2 DATE 범위 초과)로 정규화. getAllRuns()/getAllMemberStats() 는 (from, to) 시그니처로 in-place 변경 — 호출처 전수 3곳이 컨트롤러뿐, 홈 top-10 랭킹은 별도 메서드라 무변경. UI 는 3템플릿 공통 GET 폼(runners-filter-form — 시작일/종료일 date input·적용 버튼·필터 시에만 전체 보기 링크)을 주변 카드와 같은 재질의 패널 카드로 구성: --runners-bg-card + 1px solid --runners-border + 12px 라운드 표면(.runners-run-card/.runners-member-table 과 같은 계열), 라벨은 .runners-member-table th 의 마이크로 라벨 관용구(.75rem/600/letter-spacing .05em), 날짜 필드는 .runners-input 을 유지한 위에 .runners-filter-input 으로 패딩·폰트만 줄여 버튼과 min-height 2.375rem 통일(2px 테두리·네온 포커스링 그대로)하고 flex: 0 1 11rem 으로 폭을 min-content 근처에 묶어 남는 공간을 먹지 않게 함, color-scheme:dark + ::-webkit-calendar-picker-indicator{filter:invert(1)} 로 다크 필드에서 달력 글리프·팝업 가시화(trading.css 의 color-scheme 전례), 필터가 걸린 목록이면 th:classappend 로 .active — 패널 테두리 네온 + 기존 --runners-shadow 글로우 한 곳만 강조, 전체 보기는 .runners-run-category 와 같은 pill 어휘의 .runners-filter-reset 로 margin-left:auto 우측 고정(나타나도 앞 컨트롤이 밀리지 않음), 480px 에서 패널 패딩 축소 + 두 필드를 flex-basis 100% 로 세로 스택(input[type=date] 는 min-content(≈174px) 아래로 줄지 않아 2단이면 375px 에서 가로 넘침) + .runners-filter-form .runners-btn{width:auto} 로 미디어쿼리의 width:100% 를 특이도로 무력화. 어드민 대시보드는 직전 동작의 피드백인 runners-message 플래시가 필터보다 먼저 오도록 필터를 그 아래로 옮겨 다른 두 페이지의 헤더→필터→목록 순서와 일치시켰다. 빈 상태 문구는 필터 시 '해당 기간에 …' 로 분기. SEO 블록 무변경(canonical 파라미터 없음 유지, 전 페이지 noindex). 보안 무변경 — GET 쿼리 파라미터는 SecurityConfig 매칭에 무영향, /runners/admin 은 종전대로 ROLE_ADMIN. 검증: RunnerDateFilterPageTest 9건(@SpringBootTest+MockMvc+@Transactional, 도메인 포트 시딩 — 범위/단측 from·to/무파라미터/garbage 무시/from 역전 빈 상태/members 집계·총 1명/admin 인증 필터)은 필터 6건 AssertionError RED 선확인 후 GREEN, 가드 3건은 처음부터 GREEN. RunServiceDateFilterTest·AttendanceServiceDateFilterTest 8건(Mockito — 양 null 이면 기존 메서드 위임·단측 sentinel 정규화·양측 통과)은 컴파일 RED(cannot find symbol) 선확인 후 GREEN. 전체 스위트 117 클래스 719 테스트 failures 0, RunnerSeoRenderingTest 무회귀. 문서: ADR 없음 — CLAUDE.md/ADR 동기화 표의 단순 사실 변경. runner/application/CLAUDE.md Services 2줄 갱신(개행 정규화로 전체 diff, 실변경 2줄)."

# =====================================================================
# 배포 파이프라인 — CI 린트 실패 복구 + 세팅 완료 체크리스트 (2026-09-10)
# =====================================================================
# 배경: 사용자 보고 — Actions 의 Lint deploy scripts 단계가 SC1072/SC1073 으로 실패.
#   4d90a70(ci.yml 도입, 2026-09-03) 이후 main 푸시 전건이 같은 지점에서 죽고 있었다. 이 단계는
#   Build & test 보다 앞이고 push-image 는 needs: build-test 라, GHCR 이미지가 한 번도 발행되지
#   않은 상태였다 — 서버 런북 3.6 의 docker pull 도 deploy.yml 의 manifest 선검증도 성립 불가.
# 원인: calendar-deploy-gate.sh:18 의 주석이 shellcheck disable=SC2086 -- 근거 형태였다. directive
#   줄은 key=value 토큰만 받으므로 shellcheck 가 -- 를 다음 키로 읽고 = 를 못 찾아 error 2건.
#   같은 실행에서 deploy.sh 는 무출력(clean) — 결함은 이 한 줄뿐.
# 사용자 결정: ① 스크립트 수정까지 포함(문서화만 아님) ② 새 문서는 기존 운영문서를 재수록하지
#   않는 얇은 체크리스트 ③ 시크릿은 production Environment + required reviewer 승인 게이트.
# 검증(로컬, npx shellcheck@4.1.0 → shellcheck v0.11.0): 수정 전 CI 와 바이트 동일한
#   SC1072/SC1073 RED 재현 → 수정 후 -S warning 임계에서 무출력 exit 0. bash -n 양쪽 통과. severity
#   무제한 실행으로 게이트 스크립트 무출력(SC2086 이 directive 로 실제 억제됨) 확인, directive 를
#   일시 삭제하면 SC2086 이 다시 뜨는 것까지 대조. deploy.sh 의 SC2012 1건은 info 등급 기존
#   항목이라 CI 임계 아래 — 미수정.
# 테스트: ./gradlew test 미실행 — 셸 주석 1줄과 마크다운만 바뀌어 Java 경로 무관. 최종 그린
#   확인은 푸시 후 Actions.
# 범위 밖(보고만): ① Dockerfile 의 ARG JAR_FILE=build/libs/*.jar 는 build.gradle 에
#   jar { enabled = false } 가 없어 -plain.jar 까지 2개가 잡히므로 로컬 docker build . 가 실패한다
#   (CI 는 artifact 1개만 내려받아 우연히 회피). ② ci.yml 의 calendar-0.0.1-SNAPSHOT.jar 하드코딩은
#   version 을 올리면 깨진다. ③ deploy.yml 이 deploy/ 전체를 rsync 해 서버에 미사용
#   ~/apps/calendar/deploy/compose.yaml 사본이 생긴다(compose.yaml 머리말의 SSOT 경고와 상충).
#   ④ compose 의 nginx 서비스에 healthcheck 없음. ⑤ .env.example 에 H2_CONSOLE_ENABLED·
#   GOOGLE_SITE_VERIFICATION·ADSENSE_SLOT_* 미기재(전부 선택값). ⑥ AGENTS.md 는 untracked 인데
#   어느 pending 커밋에도 없다.

# Commit 169 — ✅ DONE() fix(ci): shellcheck directive 문법 오류로 막힌 배포 린트 복구 + 세팅 완료 체크리스트 문서 — 마지막 커밋(git-commit.md 포함)
git add deploy/server/calendar-deploy-gate.sh docs/guides/deploy-setup.md docs/operations/deployment.md docs/guides/git-commit.md
git commit -m "fix(ci): shellcheck directive 문법 오류로 막힌 배포 린트 단계 복구 + 세팅 완료 체크리스트 문서" -m "증상: 4d90a70 으로 ci.yml 이 들어온 뒤 main 푸시 전건이 Lint deploy scripts 단계에서 SC1073(Couldn't parse this shellcheck directive) + SC1072(Expected '=' after directive key) 로 실패했다. 원인은 calendar-deploy-gate.sh:18 한 줄 — 주석이 shellcheck disable=SC2086 -- force 는 정규식으로 ' --force' 만 허용됨 형태였다. shellcheck directive 줄은 key=value 토큰만 받으므로 파서가 -- 를 다음 directive 키로 읽고 = 를 찾지 못해 error 등급 2건을 내고, ci.yml 의 임계가 -S warning 이라 잡이 exit 1 한다. 같은 실행에서 deploy.sh 는 무출력이었으므로 결함은 이 한 줄이 전부였다. 파급이 한 줄보다 크다 — 이 단계는 Build & test 보다 앞에 있고 push-image 는 needs: build-test 라, 4d90a70 이후 어떤 커밋도 이미지가 되지 못했다. GHCR 발행 0개 상태에서는 서버 런북 3.6 의 docker pull ...:latest 도 deploy.yml 의 GHCR manifest 선검증도 통과할 수 없어 배포 세팅 자체가 이 한 줄에 막혀 있었다. 수정: 근거 주석을 directive 위 별도 줄로 올리고 directive 는 disable=SC2086 만 남겨 exec 바로 위에 유지 — 적용 대상은 그대로다. 로컬에서 확인한 v0.11.0 은 트레일링 # 주석 형태를 받지만(빈 스크립트로 두 형태를 대조), CI 는 ubuntu-latest 기본 설치본을 쓰고 버전을 고정하지 않으므로 어느 버전에서나 파싱되는 별도 줄 형태를 쓴다. 동작 영향 0 — 주석 위치만 바뀌었고 force 를 인용 없이 넘기는 동작은 그대로다(BASH_REMATCH[2] 가 ' --force' 또는 빈 문자열이라 워드 스플리팅이 곧 의도한 동작). 저장소의 나머지 shellcheck directive 는 gradlew(업스트림 생성물, CI 린트 대상 아님) 3곳뿐이고 전부 정상 문법이라 같은 결함은 없다. 검증: WSL 에 shellcheck 미설치라 npx shellcheck@4.1.0(= shellcheck v0.11.0) 으로 CI 와 같은 명령을 돌려 수정 전 SC1072/SC1073 을 바이트 동일하게 RED 재현한 뒤 수정 후 무출력 exit 0 GREEN 확인. bash -n 양쪽 통과. severity 필터를 뺀 실행에서 게이트 스크립트가 무출력인 것으로 SC2086 이 directive 에 의해 실제 억제됨을 확인했고, directive 를 일시 삭제한 사본에서는 SC2086 이 다시 뜨는 것까지 대조했다(억제가 살아 있다는 증거). deploy.sh 의 SC2012(ls 대신 find) 1건은 info 등급 기존 항목이라 -S warning 임계 아래 — 미수정. ./gradlew test 는 돌리지 않았다: 바뀐 것이 셸 주석 1줄과 마크다운뿐이라 Java 경로와 무관하다. 문서: docs/guides/deploy-setup.md 신설 — 세팅이 끝나면 역할이 끝나는 한시 체크리스트이며 사용자 결정에 따라 기존 운영문서를 재수록하지 않는다(현재 상태표, CI 실패 원인과 재발 방지 규칙, A 저장소→B GitHub→C 서버→D 리허설 의존성 순서, 다른 문서에 없는 실무 명령, 조사에서 확인된 함정, 푸시 전 로컬 선검증). 실무 명령은 ssh-keygen 2종·ssh-keyscan(포트가 22 가 아니면 -p 없이는 [host]:port 형식이 안 나와 known_hosts 가 매칭되지 않는다)과 시크릿 6개의 값 출처. 시크릿은 deploy.yml 이 선언한 production Environment 레벨에 두고 required reviewer 를 걸되, Prevent users from approving workflow runs that they triggered 는 켜지 말 것을 명시했다 — 1인 운영이라 켜면 본인 dispatch 를 본인이 승인할 수 없어 배포가 영구 대기한다(GitHub 문서상 이 체크박스는 선택 항목). 함정 절에는 조사에서 확인된 것만 담았다: .deploy/env 는 compose 가 APP_REF·HOST_TZ·JAVA_OPTS·APP_GID 를 :? fail-fast 로 요구하는데 커밋된 템플릿이 없어 4줄 전문을 문서에 실었고, /var/www/certbot 는 런북이 8.1 에 두었지만 nginx up 은 7.3 이라 바인드 마운트 소스가 없으면 Docker 가 대신 만들므로 7단계 앞으로 당기라 적었으며, nginx 는 /etc/letsencrypt/live/datedate.site 인증서가 이미 있어야 뜨는데 이를 만들어 주는 코드가 저장소에 없다는 선결 조건, 게이트 스크립트는 서버에서 /usr/local/bin 복사본이라 rsync 로 갱신되지 않고 런북 3.7 의 sudo cp 재실행이 필요하다는 유지보수 항목, 서버 .env 의 boot 필수 키가 DB_URL·DB_USERNAME·DB_PASSWORD 3개뿐이고 spring.config.import 가 optional: 이라 부재 시 DataSource 생성 시점에야 죽는다는 점(그래서 deploy.sh preflight 가 파일 존재를 먼저 검사한다), GHCR PAT 은 GitHub Secret 이 아니라 서버 ~/.docker/config.json 이라는 점을 넣었다. deployment.md 머리말에 이 가이드 링크 1줄을 추가했다 — docs/guides 에는 인덱스가 없어 링크가 없으면 발견되지 않는다. ADR 없음: directive 문법 수정은 버그 수정이고 새 문서는 ADR common/infrastructure/0001·0002 의 결정을 바꾸지 않는다. required reviewer 도 0001 의 '실계좌 서버는 반영 시점을 사람이 고른다' 를 GitHub 설정으로 구현하는 것이라 정책 전환이 아니다. CLAUDE.md 는 무변경 — 배포 절의 서술이 현재도 사실이고 새 문서는 한시 체크리스트다."

# =====================================================================
# 배포 파이프라인 — GHCR 이미지 발행을 deploy 릴리스 브랜치로 분리 (2026-09-10)
# =====================================================================
# 배경: 사용자 요청 "현재 main push 가 배포 트리거인데 deploy 브랜치로 수정". 프레미스를 코드로
#   먼저 확인해 정정했다 — deploy.yml 은 workflow_dispatch 단독이라 main 푸시로 서버가 바뀌지
#   않는다(ADR 0001 이 자동배포를 명시 기각). main 푸시가 실제로 하는 일은 GHCR 이미지 발행뿐.
#   그 사실을 제시하고 세 갈래(발행만 이동 / deploy 푸시 = 서버 자동배포 / 오해였으니 무변경)로
#   물어 사용자가 '이미지 발행만 deploy 로' 를 선택했다. image_tag description 한 줄 추가도 승인.
# 결정: ci.yml 의 발행 잡만 refs/heads/deploy 로. 서버 반영은 0001 그대로 수동 버튼 전용.
#   main 은 계속 빌드+테스트를 돌린다 — deploy 로 올리기 전에 회귀를 잡는 자리다.
# 검증: 두 워크플로 YAML 파싱(python yaml.safe_load) 통과, refs/heads/ 조건식이 deploy 2곳만
#   남은 것 grep 확인, ADR README 매트릭스 행·열 합을 재계산해 90 이 디스크 실파일 수 90 과
#   일치함을 확인, 문서의 file:line 참조를 전수 스크립트로 대조(ci.yml 줄번호가 밀려 2곳을
#   스텝 이름 표기로 교체, deploy.yml 29/36/49 는 유효 확인).
# 테스트: ./gradlew test 미실행 — 워크플로 yaml 과 마크다운만 바뀌어 Java 경로 무관.
#   실제 그린 확인은 푸시 후 Actions.
# ADR: 결정 변경이라 새 ADR common/infrastructure/0003 작성. 0001 은 Superseded 아님 —
#   재시작 배포·다이제스트 롤백·수동 반영·종료 계약은 전부 유효하고 Decision 의 빌드 항목
#   한 줄만 갱신되므로 0001 에 포인터만 걸었다.
# 선결 조건(사용자 몫): deploy 브랜치가 origin 에 없다(로컬 c26567b 만). git push -u origin deploy
#   전에는 발행 트리거가 걸리지 않는다 — deploy-setup.md A 단계에 명령까지 적었다.
# 주의(소유권): docs/adr/README.md 와 CLAUDE.md 는 Commit 165 가 선행 소유 — 규약대로 아래 add
#   목록에서 제거했다. 이 커밋의 변경(README 두 뷰의 0003 행·폴더 카운트, CLAUDE.md 의 발행
#   조건·릴리스 절차·0003 링크)은 먼저 실행되는 165 배치가 흡수한다.
# 주의(소유권): docs/guides/deploy-setup.md·docs/operations/deployment.md·docs/guides/git-commit.md
#   는 Commit 163·169 가 선행 소유 — 먼저 실행되는 배치가 흡수하므로 아래 add 목록에서 제거했다.

# Commit 170 — ✅ DONE() feat(ci): GHCR 이미지 발행을 deploy 릴리스 브랜치로 분리 (ADR common/infrastructure/0003)
git add .github/workflows/ci.yml .github/workflows/deploy.yml docs/adr/common/infrastructure/0003-image-publish-on-deploy-branch.md docs/adr/common/infrastructure/0001-container-restart-deploy-pipeline.md
git commit -m "feat(ci): GHCR 이미지 발행을 deploy 릴리스 브랜치로 분리 — main 은 빌드·테스트만 (ADR common/infrastructure/0003)" -m "사용자 요청은 '현재 main push 가 배포 트리거인데 deploy 브랜치로' 였는데 프레미스가 코드와 달라 먼저 정정했다 — deploy.yml 은 workflow_dispatch 단독이라 main 푸시로 서버가 바뀌지 않는다(ADR 0001 이 자동배포를 대안표에서 명시 기각했고, 근거는 문서 커밋에도 봇이 재시작돼 보호전용 강등 + 서킷브레이커 카운터가 리셋된다는 것). main 푸시가 실제로 하는 일은 GHCR 이미지 발행뿐이라, 세 갈래(발행만 이동 / deploy 푸시를 서버 자동배포로 / 오해였으니 무변경)를 제시하고 사용자가 발행만 이동을 선택했다. 문제: 발행 브랜치가 곧 개발 브랜치라 latest 태그가 개발 tip 을 따라가고(deploy.sh 가 즉시 다이제스트로 고정하므로 배포된 것이 바뀌지는 않지만 지금 latest 가 무엇인가가 릴리스 의도와 무관해진다), 레지스트리에 배포 후보가 아닌 이미지가 쌓이며, main 푸시를 배포 트리거로 보는 오인이 계속 재생산된다. 변경: ci.yml 의 트리거를 push branches [main, deploy] 로 넓히고 Upload jar for image job 과 push-image 잡의 조건을 refs/heads/main 에서 refs/heads/deploy 로 옮겼다. pull_request 대상은 main 유지(리뷰 대상), main 푸시도 계속 전체 빌드+테스트를 돌린다 — deploy 로 올리기 전에 회귀를 잡는 자리이므로 여기를 비우면 릴리스 브랜치에서 처음 발견하게 된다. 결과 계약은 main PR·main 푸시 = 빌드/테스트만, deploy 푸시 = 빌드/테스트 + 이미지, deploy.yml 수동 버튼 = 서버 반영. 릴리스 절차는 main 에서 deploy 로 fast-forward 푸시이고 두 ref 가 같은 커밋을 가리키면 SHA 도 같아 image_tag 가 그대로 맞는다. deploy.yml 의 image_tag description 에 사용자 승인대로 한 줄을 보태 비우면 dispatch 한 브랜치의 SHA 가 쓰이므로 Use workflow from 에서 deploy 를 고르라고 명시했다 — main 을 고르면 아직 이미지가 없는 SHA 가 될 수 있는데 GHCR manifest 선검증이 서버 접속 전에 잡아 주지만 헛걸음이다. ADR: 결정 변경이라 common/infrastructure/0003 신설. 대안표에 main 발행 유지·deploy 푸시 자동배포·git tag 발행·latest 만 수동 승격 4건의 기각 근거를 남겼는데, 특히 deploy 푸시 자동배포는 0001 이 이미 기각한 자동배포를 브랜치로 격리해 되살리는 것이라 반영 시점은 사람이 고른다는 핵심 불변식을 깬다. 0001 은 Superseded 로 두지 않았다 — 재시작 배포·다이제스트 롤백·수동 반영·종료 계약이 전부 유효하고 Decision 의 빌드 항목 한 줄만 갱신되므로 관련 ADR 행과 그 항목에 0003 포인터만 걸었다. docs/adr/README.md 는 두 뷰 모두 갱신(View 1 매트릭스 common 인프라 2에서 3·common 합계 23에서 24·전체 89에서 90, View 2 시간순 2026-09-10 행, 폴더 구조 common/infrastructure 2에서 3 ADRs) 했고 행·열 합을 재계산해 90 이 디스크 실파일 수 90 과 일치함을 확인했다. 문서 동기화: CLAUDE.md 2곳(목표 기반 실행·검증 절의 CI 서술, Deployment 절의 발행 조건 + 릴리스 절차 + 0003 링크), deployment.md 파이프라인 다이어그램을 3행으로 재작성 + 발행 브랜치 불릿 + image_tag 의 브랜치 선택 안내. 검증: 두 워크플로를 yaml.safe_load 로 파싱해 문법 통과를 확인하고, refs/heads/ 조건식이 deploy 2곳만 남았는지 grep 으로 대조했으며, 문서의 file:line 참조를 전수 스크립트로 실검증했다 — ci.yml 을 편집하며 줄번호가 밀린 2곳은 스텝 이름 표기로 바꿔 드리프트 원인을 제거했고 deploy.yml 29/36/49 는 유효를 확인했다. gradlew test 는 돌리지 않았다: 워크플로 yaml 과 마크다운만 바뀌어 Java 경로와 무관하다. 실제 그린 확인은 푸시 후 Actions. 선결 조건: deploy 브랜치가 origin 에 없다(로컬 c26567b 만 존재, b678bce 의 죽은 Cloud Run 워크플로 시절 잔재로 보인다) — git push -u origin deploy 전에는 발행 트리거가 걸리지 않으므로 deploy-setup.md A 단계에 명령까지 적었다. 함정: 브랜치명 deploy 가 저장소의 deploy/ 디렉터리와 모호해 git log deploy 가 fatal: ambiguous argument 로 실패한다(실제로 밟았다) — git log deploy -- 또는 refs/heads/deploy 로 써야 하며 git switch/git push 는 브랜치만 받으므로 영향이 없다. 이름은 사용자 지정이라 유지하고 함정만 문서화했다."

# =====================================================================
# 배포 세팅 — 서버 실측으로 드러난 런북 순서 오류·GCP 제약 정정 (2026-09-10)
# =====================================================================
# 배경: 사용자와 실제 서버 세팅을 진행하며 문서대로 실행할 수 없거나 사실과 다른 지점이
#   드러났다. 전부 서버에서 명령 출력으로 확인한 것만 반영한다.
# 발견 1(순서): 런북 3단계 7번의 sudo cp 가 소스로 쓰는 파일이 9번 첫 번들 반입에서야 도착해
#   그대로 실행하면 No such file. 번들 반입(7) → 키 발급·게이트 설치·authorized_keys(8) →
#   GitHub Secrets(9) 로 재정렬.
# 발견 2(순서): 7단계 nginx up 이 8단계 1번 sudo mkdir -p /var/www/certbot 보다 먼저라
#   바인드 마운트 소스가 없는 상태로 컨테이너가 뜬다. 7단계 2번으로 선생성 삽입 + 이후 번호 재정렬.
# 발견 3(GCP): sudo sshd -T 실측이 authorizedkeyscommand none / authorizedkeysfile
#   .ssh/authorized_keys .ssh/authorized_keys2. 게스트 에이전트가 앞쪽 파일을 덮어쓸 수 있다는
#   공식 문서 경고가 이 서버에 살아 있다는 뜻이라, 공개키를 authorized_keys2 로 옮긴다.
#   위험의 성격은 보안이 아니라 가용성 — 지워지면 배포 키가 통째로 사라질 뿐 게이트가 풀리지 않는다.
# 발견 4(port): sudo sshd -T 가 port 22 → DEPLOY_SSH_PORT 시크릿은 등록하지 않는다.
# 발견 5(known_hosts): ssh-keyscan 은 TOFU 라 MITM 방어선을 신뢰 없는 채널로 만든다. 서버 안에서
#   ssh-keygen -lf 로 지문을 대조한 값이 로컬 ssh 가 물어봤던 지문과 일치함을 확인하고 그 방식으로 교체.
# 발견 6(동적 IP): 외부 주소가 ephemeral 이라 stop/start 시 바뀌고 DEPLOY_SSH_HOST 와
#   DEPLOY_KNOWN_HOSTS 두 시크릿이 동시에 무효가 된다. 호스트 키는 부트 디스크에 남아 안 바뀌므로
#   known_hosts 는 IP 토큰만 교체하면 된다는 점을 포함해 복구 절차를 deployment.md 에 신설.
# 발견 7(GHCR 토큰): 런북 3-6 과 deployment.md 가 'fine-grained PAT(packages:read)' 라고 적고
#   있었으나 공식 문서는 'GitHub Packages only supports authentication using a personal access
#   token (classic)'. 그대로 따르면 docker login 자체가 안 되는 실행 불가 오류라 classic 으로
#   정정하고 발급 절차·스코프·히스토리 회피 형태를 채웠다.
# 발견 8(세부 부족): id -g 를 sudo 로 돌리면 0 이 나오는 함정, rrsync 가 PATH 에 없을 때
#   forced command 가 못 찾는 문제, HOST_TZ 의 정확한 추출 명령이 문서에 없었다.
# 진행 표시: 사용자가 authorized_keys 추가와 authorized_keys2 이관을 같은 작업으로 오해해
#   deploy-setup.md 의 상태 절을 '실측으로 확인된 완료' 와 '남은 것' 두 표로 재작성했다.
# 검증: 런북 전 섹션 번호 연속성 스크립트 통과(불연속 0), 문서 상대링크 전수 검증 통과,
#   게이트 스크립트 주석 변경 후 bash -n + shellcheck -S warning 재통과(exit 0).
# 테스트: ./gradlew test 미실행 — 셸 주석과 마크다운만 바뀌어 Java 경로 무관.

# Commit 171 — ✅ DONE() fix(docs): 서버 준비 런북 정정 — 단계 순서·GHCR classic PAT·authorized_keys2·동적 IP 대응 — 마지막 커밋(git-commit.md 포함)
git add docs/operations/server-migration-runbook.md docs/guides/git-commit.md
git commit -m "fix(docs): 서버 준비 런북 정정 — 단계 순서·GHCR classic PAT·authorized_keys2·동적 IP 대응" -m "사용자와 실제 서버 세팅을 진행하며 문서대로는 실행할 수 없거나 사실과 다른 지점이 드러나 정정한다. 전부 서버 명령 출력으로 확인한 것만 반영했다. 순서 오류 둘: 런북 3단계 7번의 sudo cp 는 ~/apps/calendar/deploy/server/calendar-deploy-gate.sh 를 소스로 쓰는데 그 파일이 9번 첫 번들 반입에서야 도착해 문서 순서대로 실행하면 No such file 이 난다 — 번들 반입(7) 다음에 키 발급·게이트 설치·authorized_keys(8), 그다음 GitHub Secrets(9) 로 재정렬했다. 7단계 nginx 전환은 up -d nginx 가 8단계의 sudo mkdir -p /var/www/certbot 보다 먼저라 바인드 마운트 소스가 없는 채로 컨테이너가 뜨고 Docker 가 그 디렉터리를 대신 만들어 소유권이 의도와 달라진다 — 선생성을 7단계 2번으로 넣고 이후 항목 번호를 재정렬했다(전 섹션 번호 연속성 스크립트로 불연속 0 확인). GCP 제약: sudo sshd -T 실측이 authorizedkeyscommand none 과 authorizedkeysfile .ssh/authorized_keys .ssh/authorized_keys2 였다. AuthorizedKeysCommand 방식이면 게스트 에이전트가 파일을 안 건드릴 것이라 기대했으나 none 이므로 공식 문서의 경고(authorized_keys 가 게스트 에이전트에 덮어써질 수 있다)가 이 서버에 그대로 적용된다. sshd 가 두 파일을 읽고 에이전트가 관리하는 이름은 앞쪽 하나뿐이라 공개키를 authorized_keys2 에 두는 것으로 바꿨다 — sshd 설정 변경도 root 도 재시작도 필요 없는 회피다. 위험의 성격은 보안이 아니라 가용성이다: 배포 공개키는 restrict 접두사가 붙은 줄에만 존재하므로 지워지면 함께 사라질 뿐 게이트만 풀린 키가 남지 않는다. 시크릿 확정: port 22 로 확인돼 DEPLOY_SSH_PORT 는 등록하지 않는다(deploy.yml 폴백). DEPLOY_KNOWN_HOSTS 는 ssh-keyscan 을 버리고 서버 안에서 뜨는 방식으로 교체했다 — keyscan 은 TOFU 라 이 시크릿이 곧 MITM 방어선인데 그 방어선을 신뢰 없는 채널로 만든다. 서버에서 ssh-keygen -lf 로 뜬 지문이 로컬 ssh 가 최초 접속에서 물어봤던 지문과 일치함을 확인해 값 자체도 검증했다. 호스트 토큰이 DEPLOY_SSH_HOST 와 글자 그대로 같아야 한다는 제약도 명시했다. 키 생성 위치도 못박았다: 개인키를 쥐는 쪽은 Actions 러너이고 서버는 공개키만 필요하므로 로컬에서 만들고 서버로는 pub 두 줄만 보낸다 — 서버에서 만들면 개인키를 다시 빼내 지우는 단계가 는다. 동적 IP: 외부 주소가 예약되지 않은 ephemeral 이라 stop/start 시 바뀌고 그 순간 DEPLOY_SSH_HOST 와 DEPLOY_KNOWN_HOSTS 가 동시에 무효가 된다(DNS A 레코드도 함께 깨져 사이트가 내려가므로 복구 순서는 DNS 가 먼저). deployment.md 에 복구 절차 절을 신설했는데 핵심은 호스트 키가 부트 디스크에 있어 stop/start 를 넘어 유지된다는 것이다 — known_hosts 는 IP 토큰만 갈아끼우고 base64 는 재사용하며, 새로 떠야 하는 경우는 인스턴스 재생성·OS 재설치뿐이다. 재발 방지로 고정 IP 승격(사용 중 주소를 그대로 예약하므로 IP 값 유지)과 도메인을 호스트로 쓰는 대안(시크릿을 안 건드리고 DNS 만 갱신, 단 프록시 뒤면 22번이 안 통해 불가)을 함께 적었다. 문서 반영: 런북 3단계 재정렬 + 8번에 키 생성 위치·authorized_keys2·권한·비파괴 검증 명령, 9번에 두 시크릿의 값 출처를 넣었고 7단계에 certbot 디렉터리 선생성을 삽입했다. deployment.md 는 GitHub Secrets 표를 값 출처 중심으로 재작성하고 외부 IP 절을 신설. deploy-setup.md 는 시크릿 6개를 5개로(PORT 제외) 줄이고 함정에 게스트 에이전트와 ephemeral IP 두 건을 추가했다. calendar-deploy-gate.sh 헤더 주석도 authorized_keys 에서 authorized_keys2 로 동기화했다 — 주석 변경 후 bash -n 과 shellcheck -S warning 재통과(exit 0)를 확인했고, 문서 상대링크는 전수 스크립트로 검증했다. gradlew test 는 돌리지 않았다: 셸 주석과 마크다운만 바뀌어 Java 경로와 무관하다. ADR 없음 — 런북 순서는 버그 수정이고 authorized_keys2 이관은 forced-command 게이트라는 기존 결정(ADR common/security/0006, common/infrastructure/0001)의 구현 세부다. ADR 0001 은 authorized_keys 를 언급하지 않아 정정 대상이 없다. GHCR 토큰 종류도 정정했다 — 런북 3-6 과 deployment.md 가 fine-grained PAT(packages:read) 라고 적고 있었는데 공식 문서는 GitHub Packages only supports authentication using a personal access token (classic) 이다. 그대로 따르면 docker login 단계에서 막히는 실행 불가 오류라, classic 발급 경로(Settings, Developer settings, Tokens (classic))와 스코프 read:packages 단독, 만료 지정, 토큰을 셸 히스토리에 남기지 않는 read -rsp 형태, config.json 이 토큰을 암호화가 아니라 base64 인코딩만 해서 담으므로 chmod 600 이 유일한 보호막이라는 사실까지 넣었다. 이 단계가 첫 이미지 발행 이후에만 성공한다는 순서 제약도 명시했다. 세부 보강 셋: id -g 는 배포 유저로 실행해야 하며 sudo id -g 는 0(root) 이 나와 APP_GID 에 들어가면 컨테이너가 root 그룹으로 돈다(3-4 의 명령은 sudo 실행 전에 사용자 셸이 확장하므로 옳지만 sudo bash -c 로 통째로 감싸면 깨진다), HOST_TZ 는 timedatectl 출력 파싱 대신 timedatectl show -p Timezone --value, rrsync 가 PATH 에 없으면 forced command 가 최소 PATH 로 실행돼 /usr/share/rsync/scripts/rrsync 를 못 찾고 증상이 rsync 만 조용히 실패하는 것이라 authorized_keys2 에 절대경로를 써야 한다(1단계 실측표에 경로 확인 추가). .deploy/env 에 커밋된 템플릿이 없다는 사실도 3-5 에 적었다. 진행 표시: 사용자가 authorized_keys 추가와 authorized_keys2 이관을 같은 작업으로 오해했으므로 deploy-setup.md 의 상태 절을 실측으로 확인된 완료와 남은 것 두 표로 재작성하고 완료 쪽에 근거를 함께 적었다(grep -c 2, 권한 750/700/600, sshd -T 의 port 22, 호스트 키 지문 대조). 함정 절에는 classic PAT·rrsync 절대경로·id -g 세 건을 추가하고 authorized_keys2 이관 명령과 게스트 에이전트 재시작으로 즉시 검증하는 법을 넣었으며, 런북을 고쳤으므로 certbot 디렉터리 항목은 수정 전 서술에서 반영 완료로 바꿨다."

# =====================================================================
# 배포 CI/CD — 구축 이슈 트러블슈팅 문서화 (2026-09-11)
# =====================================================================
# 배경: 사용자 요청 "이 세션에서 마주하고 해결한 모든 문제를 docs/troubleshooting 하위에
#   문서화, 어떻게 배포 CICD를 구현하는지도 설명". 파이프라인 구축 과정에서 나온 문제 14건과
#   파이프라인 구조 해설을 한 문서로 묶는다.
# 구성: 1부 파이프라인 해설(제약 4개 → 3단 구조 → 게이트별 방어 대상), 2부 문제 14건
#   (기존 troubleshooting 문서 관용구인 증상/원인/진단/해결방법/확인 구조), 3부 미해결·범위 밖,
#   부록 검증 명령 모음. 절차 자체는 재수록하지 않고 deployment.md·런북·deploy-setup.md 링크.
# 한 문서로 묶은 이유: 문제 14건 중 다수가 파이프라인 구조를 모르면 증상만 보고는 원인을
#   짚을 수 없다(발행 브랜치 분리·다이제스트 고정·forced-command 게이트). 1부가 2부의 전제다.
# 검증: 저장소 전체 마크다운 상대링크 재검사 — docs/ 와 src/ 하위 md 까지 159파일 527링크에서
#   깨짐 0(위 ADR 링크 수정 후). README 인덱스의 deploy-cicd.md 앵커 8개는 GitHub 슬러그 규칙으로
#   재계산해 실제 heading 과 전수 대조(불일치 0).
# 테스트: ./gradlew test 미실행 — 마크다운만 추가·수정.
# ADR: 없음 — 사후 기록 문서이고 새로 정하는 것이 없다.
# ✅ DONE()
# Commit 172 — ✅ DONE() docs(troubleshooting): 배포 CI/CD 구축 이슈 14건 + 파이프라인 구조 해설 — 마지막 커밋(git-commit.md 포함)
git add docs/troubleshooting/deploy-cicd.md docs/troubleshooting/README.md docs/adr/trading/observability/0002-decision-input-persistence.md docs/guides/git-commit.md
git commit -m "docs(troubleshooting): 배포 CI/CD 구축 이슈 14건 + 파이프라인 구조 해설" -m "사용자 요청으로 이번 배포 파이프라인 구축에서 실제로 부딪힌 문제와 해결을 docs/troubleshooting/deploy-cicd.md 한 문서로 모았다. 기존 troubleshooting 문서의 관용구를 따라 문제마다 증상/원인/진단/해결방법/확인 절을 두고 구분선으로 나눴다. 1부는 파이프라인 해설이다 — H2 파일 DB 배타락, 분산락 없는 스케줄러, RAM 958MiB, 실계좌 LIVE 봇이라는 제약 넷이 각각 무엇을 막는지(앞의 셋이 무중단 배포를, 넷째가 자동 배포를) 보이고, 거기서 나오는 3단 구조(main 푸시는 빌드/테스트만, deploy 푸시가 이미지 발행, 수동 버튼이 서버 반영)와 deploy.yml 의 5단계가 각각 어떤 사고를 막는지, deploy.sh 의 게이트 7개가 무엇을 막는지, 종료코드 5종의 의미를 표로 정리했다. 한 문서로 묶은 이유는 문제 14건 중 다수가 구조를 모르면 증상만으로 원인을 짚을 수 없기 때문이다 — 발행 브랜치 분리, 다이제스트 고정, forced-command 게이트가 그렇다. 2부 14건: shellcheck directive 의 -- 파싱 실패가 빌드 앞단을 막아 GHCR 이미지가 0개였던 것, WSL 에 shellcheck 가 없어 CI 를 재현하지 못하던 것과 npx 래퍼 해법, main 푸시가 배포 트리거라는 오인과 그 오인이 재생산되던 구조, 브랜치명 deploy 와 deploy/ 디렉터리의 git 모호성, 런북의 순서 오류 둘(게이트 설치가 번들 반입보다 앞, nginx 기동이 certbot 디렉터리 생성보다 앞), gcloud 로만 접속되던 서버에 맨 ssh 가 거부되던 것, GCP 게스트 에이전트의 authorized_keys 덮어쓰기와 authorized_keys2 회피, GHCR 이 classic PAT 만 받는다는 문서 오류, ephemeral IP 가 바뀌면 시크릿 둘이 동시에 죽는 문제와 호스트 키는 유지되므로 IP 토큰만 교체하면 된다는 사실, ssh-keyscan 의 TOFU 가 MITM 방어선을 무력화하는 설계 결함, 커밋 로그의 백틱과 달러가 Git Bash 명령치환·PowerShell 이스케이프로 깨지는 것, 문서 file:line 이 편집 한 번에 낡는 드리프트, rrsync 가 PATH 에 없을 때 rsync 키만 조용히 실패하는 것, sudo id -g 가 0 을 돌려주는 함정. 3부에는 이번 범위 밖이라 고치지 않은 항목 7건을 근거와 함께 남겼다 — 특히 Dockerfile 의 JAR_FILE 글롭이 plain jar 까지 잡아 로컬 docker build 가 실패하고 CI 는 artifact 1개만 내려받아 우연히 회피 중이라는 것. 부록은 이 세션에서 실제로 돌린 검증 명령 모음이다. 절차 자체는 재수록하지 않고 deployment.md·server-migration-runbook.md·deploy-setup.md 로 링크했으며, 반대 방향으로 deployment.md 와 deploy-setup.md 머리말에 이 문서 링크를 1줄씩 걸었다. troubleshooting/README.md 는 인덱스 표에 행을 추가하고 Quick Reference 에 Deploy / CI-CD Errors 절을 신설해 증상 문자열로 찾아갈 수 있게 했다(SC1073, ambiguous argument, No such file, Permission denied, exit 5 등 8항목). 검증: 저장소 전체 마크다운 상대링크 440개를 스캔해 신규 문서 기인 깨짐이 0 임을 확인했고, README 가 거는 deploy-cicd.md 앵커 8개는 GitHub 슬러그 규칙으로 재계산해 실제 heading 과 전수 대조했다(불일치 0). gradlew test 는 돌리지 않았다: 마크다운만 추가·수정했다. ADR 없음 — 사후 기록 문서이고 새로 정하는 것이 없다. 함께 수정: 링크 전수 검사에서 드러난 기존 깨진 링크 1건을 사용자 요청으로 이번 배치에 포함했다. trading/observability/0002 의 관련 ADR 행이 ../risk/0003-position-risk-guards.md 를 가리키는데 git log 로 확인하니 그 이름의 파일은 존재한 적이 없어 처음부터 오타였다. 링크 텍스트가 trading/risk/0003 이고 그 폴더의 ADR 0003 은 0003-entry-and-time-risk-guards.md 하나뿐이며, 0002 가 영속화하는 적용 주문비중이 0003 의 노출 상한과 직결되므로 문맥상으로도 그것이 맞다. 정정 후 docs/ 와 src/ 하위 마크다운을 다시 스캔해 깨진 상대링크가 0 임을 확인했다. 이어서 사용자 요청으로 전 문서 감사를 돌려 기존 결함 셋을 더 잡았다. troubleshooting/README.md 의 Quick Reference 앵커 2건이 실제 heading 과 어긋나 있었다 — webmvctest-import-error 는 heading 이 WebMvcTest Import 오류라 webmvctest-import-오류가, undefined-property-in-javascript 는 JavaScript에서 undefined 속성이라 javascript에서-undefined-속성이 맞다(영문으로 쓴 앵커가 한글 heading 을 못 가리킨 것). docs/adr/README.md 의 폴더별 구조 목록은 common/seo 를 10(실제 11), datedate/domain 을 7(실제 8)로 적고 있어 목록 합계가 88 이었다 — 총계와 매트릭스는 90 으로 맞아 있었고 목록만 드리프트한 것이라 둘을 올려 디스크 90 · 총계 90 · 목록 합계 90 · 매트릭스 행열합을 전부 일치시켰다. 그 밖에 deploy-setup.md 의 의존성 그래프가 시크릿 6개로 남아 있어 본문의 5개(PORT 제외)와 어긋난 것을 고쳤고, Commit 170 이 docs/adr/README.md 와 CLAUDE.md 를 add 목록에 넣어 Commit 165 와 중복 소유하던 규약 위반도 정리했다(170 에서 제거 + 흡수 주석). 감사 자체의 검증: 마크다운 543링크와 24앵커 전수 대조(깨짐 0·불일치 0), file:line 참조 47개를 소스 인덱스로 해석해 범위초과 0, deploy-cicd.md 의 사실 주장 15항목(헬스 URL·태그 정규식·백업 5세대·틱 회피 구간·시간창·종료코드·게이트 개수·deploy.yml 스텝 수 등)을 실제 소스와 대조해 불일치 0, 런북 번호 연속성 0, 커밋 로그 9줄의 따옴표·메타문자 검사와 add 목록 파일 존재 전수 확인."

# =====================================================================
# 코인 봇 — 매매 전략 수익성 비판 검사 (2026-09-15)
# =====================================================================
# 배경: 사용자 요청 "코인 매매로직 검사. 매매 전략이 수익률을 낼 수 있는지. 비판적으로."
#   선행 감사(05-30 수익성, 07-06 운영)의 권고가 구현된 뒤의 현재 코드가 현재 시장(KRW-ADA 284원,
#   틱 1원=0.35%)에서 양의 기대값을 내는지만 본다. 분석·권고 문서 + 재현 스크립트, 코드 변경 없음.
# 방법: 코드 정독 + Bithumb 공개 API 실측(index 0 = 형성봉, 빈 분 생략, 호가·거래량) + 105일 1분봉
#   33,000개 위에서 매매 로직 전체(지표·다이버전스·점수·게이트·루프 순서·리스크 출구·리밸런싱·
#   서킷브레이커)를 순수 파이썬으로 재현한 리플레이(6변형) + 5렌즈 멀티에이전트 정적 분석.
#   멀티에이전트 적대검증은 세션 한도로 2회 모두 실패 — 병합 16건을 오케스트레이터가 코드를 다시
#   열어 확인했고 문서에 그 사실을 명시(부록 B).
# 결론: 현 상태로는 수익 불가. 주범은 8지표 전략이 아니라 리밸런싱 엔진 — 105일 리플레이 -75%
#   (손절 임계가 체결 낙관 상한 -57%), 리밸런싱 OFF 면 -2.1%, 리밸런스 포지션 손절 면제(05-30 P1-3
#   이전 설계)면 -11.8%(하락장 보유). 원인은 P1-3 이 만든 상호작용: 리밸런스 매수가 -1.5% 손절
#   Position 이 되고 국면 목표에 0% 가 없어 전액 현금에서도 매수하며 리스크 청산이 쿨다운을 안
#   갱신해 손절 -> 8h 후 재매수 -> 손절 루프. 신호 엔진은 휴면(105일 BUY 에피소드 3회, 골든크로스
#   ±25 는 수렴 억제에 99% 차단, 비다이버전스 5컴포넌트 합 최대 33 < 40). 07-06 P1-2 캔들 동결은
#   추정에서 확정으로. 출구 실현 분포는 손절 -1.90% vs 통상 승자 +0.55%(손익분기 승률 ~78%).
#   ADR strategy/0001~0003 이 인용한 백테스트는 저장소·이력 어디에도 없다.
# 산출물: docs/audit/coin-trading-strategy-review-2026-09-15.md(결론·실측·발견 F1~F11·반박·권고·
#   서버 대조법), docs/audit/scripts/coin-strategy-replay-2026-09-15.py(표준 라이브러리만, fetch/run,
#   환경변수 2개로 민감도), trading/CLAUDE.md 에 문서 포인터 2줄, .gitignore 에 스크립트 캐시 2줄.
# 검증: 문서의 file:line 인용 전수를 grep -n 으로 재확인(CandleScheduler 2건 정정 후 불일치 0),
#   git 이력 주장(8aeefa8 2026-01-01 임계 50->40 완화, cbcd9e0 초기값 50) git show 로 확인,
#   저장소 스크립트로 6변형 재실행해 문서 표와 동일 수치 확인(결정적).
# 테스트: ./gradlew test 미실행 — Java 코드 무변경(md·py·gitignore 만).

# Commit 173 — ✅ DONE() docs(trading): 매매 전략 수익성 비판 검사 보고서 + 105일 리플레이 스크립트 — 마지막 커밋(git-commit.md 포함)
git add docs/audit/coin-trading-strategy-review-2026-09-15.md docs/audit/scripts/coin-strategy-replay-2026-09-15.py src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md .gitignore docs/guides/git-commit.md
git commit -m "docs(trading): 매매 전략 수익성 비판 검사 보고서 + 105일 리플레이 스크립트" -m "사용자 요청으로 trading 모듈(Bithumb KRW-ADA)의 매매 전략이 현재 시장에서 양의 기대값을 낼 수 있는지 비판적으로 검사했다. 선행 감사 두 건의 권고가 구현된 현재 코드를 대상으로 하며 코드는 바꾸지 않는다. 방법은 셋이다. 첫째 거래소 공개 API 실측: Bithumb 분봉 API 는 index 0 에 형성 중인 현재 분을 돌려주고 거래가 없는 분은 응답에서 생략한다는 것을 KRW-BTC 와 KRW-ADA 로 확인했고, ADA 는 284원에 호가단위 1원(0.35%), 최근 7일 분의 76% 가 거래 없음, 존재하는 봉의 76% 가 고가=저가, 60분 절대변동 중앙값 0.00% 다. 둘째 105일(06-01~09-15) 1분봉 33,000개 위에서 IndicatorService, DivergenceService, SignalService 의 점수와 게이트, TradingBotService 의 루프 순서와 쿨다운과 가드, RiskManagementService 의 손절/트레일링/익절/시간청산, RebalanceService 의 MA60 국면과 FIFO 수익포지션 청산, 서킷브레이커를 application.yaml 운영값 그대로 순수 파이썬으로 재현한 리플레이 6변형. 셋째 5렌즈 멀티에이전트 정적 분석 — 적대검증 단계는 세션 한도로 두 번 다 실패해 병합 16건을 직접 코드를 열어 확인했고 문서 부록 B 에 그 사실을 적었다. 결론은 현 상태로는 수익 불가이며 손실의 주범은 8지표 전략이 아니라 리밸런싱 엔진이다. 리플레이는 현행 구성이 -75%(손절을 임계가에 체결시키는 낙관 상한으로 -57%), 리밸런싱을 끄면 -2.1%, 리밸런스 포지션을 손절에서 면제하면(2026-05 감사 P1-3 이전 설계) -11.8% 로 같은 기간 가격 -15.8% 의 보유에 가깝다. 원인은 P1-3 이 만든 상호작용이다 — 리밸런스 매수가 -1.5% net 손절이 붙은 Position 이 되고, determineTargetRatio 가 0.7/0.3/0.5 만 돌려줘 전액 현금 상태에서도 편차가 임계를 넘어 항상 매수하며, RiskManagementService.closePosition 이 리밸런스 쿨다운과 lastTradeTime 을 갱신하지 않아 손절 뒤 8시간이면 다시 30~70% 를 사는 루프가 105일간 293회 돌았고 그중 170회가 손절이었다. 리밸런스 매수는 서킷브레이커, 물타기 차단, 노출 상한, maxPositions 를 모두 우회하면서 그 손절은 서킷브레이커에 카운트돼 신호 매수만 막는다. 신호 엔진은 휴면이다: 골든크로스 ±25 는 MA 수렴 0.2% 억제가 교차봉에서 거의 항상 참이라 99%(302/304) 차단되고, 비다이버전스 5개 컴포넌트 합은 최대 33 으로 +40 에 닿지 못해 진입은 다이버전스 우연에만 달려 있으며 105일간 BUY 에피소드 3회다. 07-06 리뷰가 추정으로 남긴 캔들 동결(P1-2)은 index 0 형성봉 실측과 CandleService 의 insert-only 로 확정됐다 — 활성 분의 저장 봉은 :05 부분 스냅샷으로 영구 고정된다. 출구 산술은 진입 284 기준 손절이 mid 280.5 에서 발동해 -1.90% 실현, 약신호 매도와 트레일링 최소가 +0.55% 라 손익분기 승률이 78% 이고, ADR strategy/0005 의 33% 는 모든 승자가 TP 로 나간다는 가정이다. ADR strategy/0001~0003 이 인용한 백테스트는 docs, src, git log --all 어디에도 없고 Mode.BACKTEST 는 스텁이며, 초기 임계 50 이 2026-01-01 같은 날 기준이 높아서 매매가 실행이 안 됨 -> 완화 로 40 이 된 이력을 git show 로 확인했다. 문서는 결론, 실측(API 의미론, 시장 격자, 리플레이 표, 출구 산술), 발견 F1~F11(심각도순, file:line 인용, 선행 감사 대비 상태), 반박과 한계(표본 창은 하락 저변동 105일이며 루프 구조·동결·틱·증거 부재는 국면과 무관, mid 는 체결가 근사, 서버 실계좌 이력 미열람), 수익이 나려면 참이어야 하는 것 넷, 우선순위 권고(0순위는 서버 PAPER 전환 또는 rebalancing.enabled=false, 이후 리밸런싱 재설계·캔들 upsert·틱/ATR 임계·백테스트 엔진·거래량 다이버전스 가중 0·분석 페이지 보정 — 각각 ADR 대상), 서버 analytics 페이지로 예측을 실적과 대조하는 법으로 구성했다. 재현 스크립트는 표준 라이브러리만 쓰고 fetch 와 run 두 명령과 민감도 환경변수 둘을 가지며, 저장소 코드와 DB 를 건드리지 않는다(공개 API GET + 로컬 캐시, 캐시와 산출물은 .gitignore). trading/CLAUDE.md 의 감사 포인터 옆에 이 문서 링크 2줄을 더했다. 검증: 문서의 file:line 인용을 grep -n 으로 전수 재확인해 CandleScheduler 2건을 정정했고, 저장소에 넣은 스크립트로 6변형을 다시 돌려 문서 표와 동일한 수치를 얻었다(결정적). gradlew test 는 돌리지 않았다: Java 코드 변경이 없다. ADR 없음 — 분석 문서이며 결정을 바꾸지 않는다; 권고를 실행할 때 risk/0002 Superseded, strategy/0009 갱신, strategy/0005 후속이 필요하다."

# Commit 174 —  ✅ DONE() fix(trading): 신호 분석(analyze) 트랜잭션 범위 축소 — 90일 조회의 커넥션 점유로 인한 전역 504 재발 방지 (ADR observability/0001 보강) — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsService.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsServiceTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsGateParityTest.java docs/adr/trading/observability/0001-signal-quality-analytics-page.md docs/guides/git-commit.md
git commit -m "fix(trading): 신호 분석(analyze) 트랜잭션 범위 축소 — 90일 조회의 커넥션 점유로 인한 전역 504 재발 방지" -m "사용자가 운영 서버(GCP 소형 인스턴스, mem_limit 700m)에서 trading 대시보드의 신호 품질 분석(/trading/analytics?days=90) 조회 직후 사이트 전체가 504 로 죽는 것을 겪고 app.log(jpa show-sql stdout 리다이렉트) 의 trading_trades status=? 쿼리에서 tail 이 멈춘 것을 제보했다. 코드로 원인을 좁혔다: trading_signals/trading_trades/trading_positions 모두 필요한 복합 인덱스(market+signal_time, market+created_at, market+status+closed_at)가 이미 있어 인덱스 미비는 아니었고, resolveForward 의 전방수익 계산도 투 포인터로 지평별 O(n) 선형이라 병리적 복잡도도 아니었다. 실제 원인은 TradingAnalyticsService 클래스 레벨 @Transactional(readOnly=true) 가 buildReport() 의 순수 Java 집계(90일 ≈ 13만 SignalSample 행, BigDecimal 연산 다수, ADR observability/0001 이 이미 '요청 스레드를 수 초 잡을 수 있다'고 명시한 그 비용)까지 감싸 그 시간 내내 Hikari 커넥션 1개를 점유한 것이었다 — application.yaml 에 풀 크기 override 가 없어 기본값 10, 트레이딩 1분 루프·주식 09:20~11:20 5초 루프와 같은 풀을 공유하는 소형 인스턴스에서 이 점유가 연쇄 고갈로 번져 사이트 전역 504 가 됐다. ProfitService.getProfitSummary() 의 무제한 CLOSED 포지션 조회(15초마다 폴링)도 같이 짚었으나, 트레이드/포지션 행수는 스캘핑 빈도상 90일 누적도 수백~수천 건 수준이라 이번 장애의 주 원인일 가능성은 낮다고 판단해 사용자가 이번 수정 범위에서 제외했다(후속 하드닝 후보로만 남김). 조치는 이 저장소에 이미 있는 패턴 재사용 — RiskManagementService/RebalanceService 의 주문 경로가 쓰는 TransactionTemplate(P0-3b: 영속화만 짧은 트랜잭션, HTTP/sleep 은 밖)와 같은 모양으로, analyze(from,to) 의 읽기 3건(findSamplesByMarketAndSignalTimeBetween, findByMarketAndStatusAndClosedAtBetween, findByMarketAndCreatedAtBetween)만 PlatformTransactionManager 로 만든 TransactionTemplate(readOnly=true) 안에서 실행하고 그 결과를 private record Loaded 로 묶어 buildReport 호출은 트랜잭션·커넥션 밖에서 하도록 옮겼다. 클래스 레벨 @Transactional 어노테이션은 제거 — 이 클래스의 다른 public 메서드(clampDays, defaultFrom)는 저장소 접근이 없어 영향 없음을 먼저 확인했다. 생성자에 PlatformTransactionManager 를 추가하면서 buildReport 를 직접 검증하는 두 테스트(TradingAnalyticsServiceTest, TradingAnalyticsGateParityTest)의 new TradingAnalyticsService(null, null, null, properties) 호출에 5번째 인자 null 을 추가했다 — 두 테스트 모두 저장소 접근 없는 순수 로직만 검증하므로 로직 변경은 없다. 검증: compileTestJava 통과 후 TradingAnalyticsServiceTest(17)·TradingAnalyticsGateParityTest(16)·TradingAnalyticsPageTest(5) 를 개별 실행해 GREEN 확인, trading 패키지 전체(--tests me.singingsandhill.calendar.trading.*)로 33개 테스트 클래스 191개 테스트 전부 GREEN(failures 0, errors 0, build/test-results/test/*.xml 로 실행 개수 실측)까지 회귀 없음을 확인했다. ADR: 새 ADR 이나 Status 전환 없음 — observability/0001 의 결정(온-리드 집계·SignalSample 투영·온디맨드 페이지·JSON 엔드포인트 없음)은 그대로이고 트랜잭션 경계만 좁힌 구현 디테일이라, 그 ADR 의 Consequences 절에 이번 운영 실측(추정했던 '요청 스레드 수 초 점유'가 실제로는 Hikari 커넥션 점유였고 그것이 전역 504 로 번졌다는 사실)과 조치를 한 항목으로 추가했다. 사용자가 이미 서버를 재시작해 장애 자체는 해소된 상태이고, 이 커밋은 재발 방지 코드다 — 서버 반영은 CLAUDE.md 대로 별도 수동 workflow_dispatch 배포이며 이 커밋 자체가 배포를 트리거하지 않는다."

# Commit 175 — ✅ DONE() docs(stock): bot.md 포지션 사이징 섹션 신설 — PAPER 모드 가용현금 미차감 정리 — 마지막 커밋(git-commit.md 포함)
git add docs/stock/bot.md docs/adr/stock/infrastructure/0008-stock-name-source-search-stock-info.md docs/guides/git-commit.md
git commit -m "docs(stock): bot.md 포지션 사이징 섹션 신설 — PAPER 모드 가용현금 미차감 정리" -m "사용자 요청으로 주식 갭&눌림목 봇의 스캔(스크리닝) 이후 종목당 매수 예산과 청산(매매정리) 로직을 코드 기준으로 분석했다. 청산 로직(손절·TP1~3·트레일링·시간청산)은 docs/stock/bot.md 6절(신설 이후 7절)에 이미 정확히 문서화돼 있어 StockRiskService·StockPosition 코드와 대조만 하고 손대지 않았다. 반면 종목당 예산이 어떻게 정해지는지는 이 문서 어디에도 없어 5절(진입 조건)과 6절(청산 전략) 사이에 6절 포지션 사이징을 새로 끼워 넣었다. 내용은 셋이다. 첫째 StockPositionService.calculatePositionSize 의 계산식 — 가용현금(잔고조회 inquire-balance 의 익일정산금액) 에 risk.position-size-ratio(10%) 를 곱한 목표금액을 bot.max-position-size(500만원) 로 캡하고 매수가능조회(inquire-psbl-order) 로 한 번 더 축소한다는 것을 공식과 1000만/5000만/8000만원 세 구간 예시로 적었다. 둘째 GapPullbackBotService.executeEntries 의 max-positions=5 동시보유 제한이 스크리닝 max-watchlist-size=10 과 다른 관문이며, PullbackDetectionService.getEntryReadyStocks 가 정렬 없는 단순 조회라 여러 종목이 같은 틱에 ENTRY_READY 가 되어도 매수 순서가 스크리닝 점수순이 아니라는 점. 셋째 가장 비중 있게 다룬 PAPER 모드 함정 — KoreaInvestmentApiClient.buyMarket/sellMarket 은 bot.mode 가 LIVE 가 아니면 simulateOrder 로 빠져 브로커에 아무 주문도 보내지 않지만, 같은 클래스의 getAvailableCash/getBuyableQuantity 는 이 분기가 전혀 없이 항상 실제 KIS 잔고·매수가능조회를 호출한다. 운영 기본값인 PAPER(.env 에 STOCK_BOT_MODE 미설정 확인 — bot.mode 는 stock.kis.production 과는 별개 축이다) 에서는 시뮬레이션 매수가 예수금을 실제로 차감하지 않으므로 3·4·5번째 포지션도 매번 거의 동일한 실계좌 잔고 기준 10%가 반복 계산된다 — LIVE 라면 실주문이 예수금을 깎아 후속 포지션이 복리로 작아진다. 일일 실적 리포트(11:40)로 쌓는 PAPER 실측을 LIVE 전환 근거로 쓸 때 승률·손익률은 참고할 수 있어도 포지션 크기만은 그대로 대표하지 않는다는 실무적 함의가 있어 경고로 강조하고 12절(구 11절) FAQ 에도 항목을 추가했다. 새 절을 끼워 넣으며 7절부터 12절까지 번호가 하나씩 밀렸고, 4절 상태머신 표가 청산 조건을 섹션 6 으로 가리키던 내부 참조를 섹션 7 로 고쳤다. 이 번호 이동으로 docs/adr/stock/infrastructure/0008-stock-name-source-search-stock-info.md 의 두 참조(이메일 구성 절을 섹션 9 로 지목)도 stale 해져 섹션 10 으로 함께 정정했다 — 저장소 전체에서 bot.md 를 인용하는 문서를 grep 으로 재확인해 섹션 번호를 명시한 참조가 이 두 줄뿐임을 확인했다. ADR 없음 — 기존 코드 동작을 문서화했을 뿐 정책·계수·구조를 바꾸지 않았다. gradlew test 는 돌리지 않았다: 마크다운만 추가·수정했다."

# =====================================================================
# ads.txt AdSense "찾을 수 없음" 조사 (2026-09-23)
# =====================================================================
# 배경: 사용자가 3일 전 배포 이후 AdSense 대시보드의 ads.txt 상태가 "찾을 수 없음"이라고
# 제보. curl 로 https/http × www/non-www 4가지 조합을 전부 확인 — 전부 200, text/plain,
# 정확한 pub ID 로 정상 응답(리다이렉트도 /ads.txt 경로를 그대로 보존). robots.txt 도
# /ads.txt 를 막는 규칙 없음. 즉 지금 이 순간 엔드포인트 자체는 정상.
# 근본 원인 후보 둘을 코드/이력에서 확인:
# ① deploy/nginx/datedate.conf 가 재시작 배포 1~3분 창에 모든 경로(= /ads.txt 포함)를
#    503(유지보수 페이지)으로 응답하도록 의도적으로 설계돼 있음. Google 공식 문서
#    (support.google.com/adsense/answer/7679060) 는 크롤 시점에 soft-404/5xx 를 받으면
#    "not found" 상태가 최대 5일간 유지(retained)된다고 명시 — 3일 전 배포 시점과
#    시간축이 맞아떨어져 가장 근거가 강한 설명. 코드 변경 없이도 배포일+5일 이내 자연
#    해소될 것으로 예상.
# ② static/ads.txt 가 2026-07-12 ads.txt 와 무관한 트레이딩 커밋(7171a3a)에서 편집기가
#    LF → CRLF 로 뒤집은 이력을 git show 로 확인(6994b9e 는 LF, 7171a3a 부터 CRLF).
#    .gitattributes 에 이미 같은 사고가 *.md 에 있었던 전례(주석 참고)가 있으나 ads.txt 는
#    빠져 있었음. IAB ads.txt 스펙(WebSearch 로 확인)은 파서가 CR/LF/CRLF 를 모두 관대하게
#    허용해야 한다고 명시하므로 이것이 "not found" 의 직접 원인일 가능성은 낮지만, 의도치
#    않은 회귀이자 Google 문서가 "invalid whitespace characters" 를 파싱 실패 원인으로
#    거론하므로 사용자 승인 하에 예방적으로 정리.
# 사용자가 보고서의 권장 조치 3번(ads.txt LF 정규화 + .gitattributes 가드)을 실행하라고
# 명시적으로 지시. ①은 코드로 손댈 수 없는 외부 서비스 캐시 지연이라 조치 대상이 아님.
# ADR: 없음 — 정책/임계값/구조 변경이 아니라 실수로 바뀐 줄바꿈 문자를 원복하고 재발
# 방지 가드를 추가하는 사실 정정. StaticResourceController 의 Content-Type 계약(ADR
# common/seo/0006)은 그대로 유지.
# 테스트: 미실행 — 정적 텍스트 리소스 1바이트(CRLF→LF) + .gitattributes 만 변경, Java
# 코드·동작 계약 무관.

# Commit 176 — ✅ DONE() fix(seo): ads.txt CRLF→LF 정규화 + .gitattributes 가드 — AdSense "찾을 수 없음" 조사 후속 — 마지막 커밋(git-commit.md 포함)
git add src/main/resources/static/ads.txt .gitattributes docs/guides/git-commit.md
git commit -m "fix(seo): ads.txt CRLF->LF 정규화 + .gitattributes 가드" -m "사용자가 3일 전 배포 이후 AdSense 대시보드 ads.txt 상태가 '찾을 수 없음'이라고 제보해 조사했다. curl 로 https/http x www/non-www 4가지 조합을 전부 확인한 결과 지금 이 순간은 전부 200/text-plain/정확한 pub-7334667748813914 로 정상 응답하고 있고 robots.txt 도 /ads.txt 를 막지 않는다 - 즉 엔드포인트 자체는 현재 정상이다. 가장 근거가 강한 설명은 deploy/nginx/datedate.conf 의 의도된 설계다: 재시작 배포 1~3분 창에 모든 경로가 503(유지보수 페이지)으로 응답하는데, Google 공식 ads.txt 트러블슈팅 문서는 크롤 시점에 soft-404/5xx 를 받으면 'not found' 상태가 최대 5일간 유지된다고 명시한다 - 3일 전 배포와 시간축이 맞아 코드 수정 없이 배포일+5일 이내 자연 해소될 가능성이 높다고 판단했다(이 부분은 조치 대상이 아니라고 보고에 명시). 별도로 git show 로 static/ads.txt 의 줄바꿈 이력을 확인하니 2025-12(6994b9e)에는 LF 였던 파일이 ads.txt 와 무관한 트레이딩 커밋 7171a3a(2026-07-12)에서 편집기가 CRLF 로 뒤집어 놓은 채 2개월 넘게 방치돼 있었다 - .gitattributes 에 *.md 가 같은 사고를 겪은 전례(주석)가 있었는데 ads.txt 는 그 가드에서 빠져 있었다. IAB ads.txt 스펙은 파서가 CR/LF/CRLF 를 모두 관대하게 처리해야 한다고 명시(WebSearch 로 확인)하므로 이 CRLF 가 'not found' 의 직접 원인일 가능성은 낮다고 판단했지만, Google 문서가 'invalid whitespace characters' 를 파싱 실패 원인으로 거론하기도 해 원인일 가능성 자체를 배제하진 않았다. 사용자가 보고서의 권장 조치 3번(ads.txt LF 정규화 + .gitattributes 가드)을 실행하라고 명시적으로 지시해, static/ads.txt 를 LF 전용으로 다시 쓰고 .gitattributes 에 '/src/main/resources/static/ads.txt text eol=lf' 한 줄을 추가해 앞으로 편집기가 다시 CRLF 로 뒤집는 것을 git 이 막도록 했다. git check-attr 로 text=set/eol=lf 가 적용됨을 확인했다. StaticResourceController.adsTxt() 의 Content-Type 계약(ADR common/seo/0006)은 건드리지 않았다. ADR 신설 없음 - 정책/임계값/구조 변경이 아니라 실수로 바뀐 줄바꿈 문자를 원복하고 재발 방지 가드를 추가하는 사실 정정이다. gradlew test 는 돌리지 않았다 - 정적 텍스트 리소스 1바이트 변경과 .gitattributes 만 바뀌어 Java 코드·동작 계약과 무관하다."

# =====================================================================
# 일정 페이지 시간 투표 신설 (2026-09-24)
# =====================================================================
# 배경: 사용자 요청 — 장소·메뉴 투표와 같은 양식의 시간 투표, 선택된 날 기반 시간 선택,
# 겹치는 시간 시각화. 사용자 결정(질문 2개): ① 방식 = 후보 제안 + 투표(When2meet 형 기각),
# ② 날짜 드롭다운 = 참여자 한 명이라도 저장한 날 전부 + 서버도 강제.
# 기본값으로 정한 것: 30분 단위·00:00~24:00·자정 넘김 없음, 새 후보 기본 18:00~20:00,
# 중복 후보 409, 삭제 API/UI 없음, UserActivity 미기록(Recap 미집계).
# ADR: datedate/domain/0009 신설 — 새 도메인(timeslot) 추가라 동기화 표의 '새 도메인' 행 해당.
# 검증: 전 단계 TDD(RED 선확인 → GREEN), 전체 스위트 122 클래스 753 테스트 failures 0.
#   UI 는 bootRun 대신(.env 가 코인·주식 봇 LIVE) 스크래치 하네스 — 실제 style.css·schedule/*.js
#   + 가짜 SCHEDULE_DATA/api — 를 Windows Chrome 헤드리스 CDP 로 데스크톱·375px 검증.
# 범위 밖 관찰: 375px 에서 달력 오른쪽 열 .cell-tooltip 이 뷰포트를 10px 넘김(기존 문제, 미수정).
#   작업 중 저장소 루트에 빈 package-lock.json(87B, packages 없음)이 생김 — 커밋 대상 아님.

# Commit 177 — ✅ DONE() feat(datedate): 일정 페이지 시간 투표 — 날짜+시간대 후보 제안·투표 + 겹치는 시간 차트
# 주의: messages.properties·messages_en.properties 의 nav.guides 1줄씩(헤더 '모임 노하우'/'Tips')은 SEO 세션 Commit 181(09-26 재번호 전 180)의 변경이 여기 흡수됨 — 181 보다 먼저 커밋돼 있어 헤더 키가 존재한다.
git add src/main/java/me/singingsandhill/calendar/datedate/domain/timeslot/TimeSlot.java src/main/java/me/singingsandhill/calendar/datedate/domain/timeslot/TimeSlotRepository.java src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/Schedule.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/DuplicateTimeSlotException.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/InvalidTimeSlotException.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/TimeSlotNotFoundException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/TimeSlotService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/TimeSlotRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/TimeSlotJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/TimeSlotVoteJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/ScheduleJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/TimeSlotJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/TimeSlotApiController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/ScheduleApiController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/request/TimeSlotCreateRequest.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/TimeSlotResponse.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/ScheduleDetailResponse.java src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css src/main/resources/static/js/api.js src/main/resources/static/js/schedule/main.js src/main/resources/static/js/schedule/state.js src/main/resources/static/js/schedule/utils.js src/main/resources/static/js/schedule/voting.js src/main/resources/static/js/schedule/timeslots.js src/main/resources/templates/schedule/view.html src/test/java/me/singingsandhill/calendar/datedate/domain/timeslot/TimeSlotTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/TimeSlotServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/TimeSlotApiControllerTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/ScheduleApiControllerTest.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/TimeSlotPersistenceIntegrationTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleTimeSlotRenderingTest.java
git commit -m "feat(datedate): 일정 페이지 시간 투표 — 날짜+시간대 후보 제안·투표 + 겹치는 시간 차트" -m "사용자 요청으로 일정 페이지(/{ownerId}/{year}/{month})에 장소·메뉴 투표와 같은 양식의 시간 투표를 신설했다. 날짜는 모이지만 몇 시에 만날지 정할 수단이 없어 결정의 마지막 단계가 페이지 밖으로 새던 문제다. 사용자는 두 질문에 답했다: 방식은 후보 제안 + 투표(개인별 가능 시간 칠하기 기각), 날짜 드롭다운은 참여자 한 명이라도 저장한 날 전부. 도메인: TimeSlot(dayIndex, startMinute, endMinute, voters) — dayIndex 는 Participant.selections 와 같은 인덱스(확장 모드 1~49 그리드, 레거시 일자)라 선택된 날과 그대로 비교되고, 시간은 자정 기준 분 int(LocalTime 은 24:00 표현 불가). 생성자가 dayIndex>=1, 30분 단위, 0<=start<end<=1440 을 검증(IllegalArgumentException -> 400). 투표는 Location 과 같은 이름 기반·대소문자 무시 중복 금지. Schedule.hasAvailabilityOn(dayIndex) 신설 — 비즈니스 규칙은 도메인 메서드(ADR datedate/domain/0001). TimeSlotService(클래스 readOnly, 쓰기만 @Transactional)는 아무도 저장 안 한 날이면 InvalidTimeSlotException 400, 같은 날짜·시작·종료면 DuplicateTimeSlotException 409, 없는 후보 투표는 TimeSlotNotFoundException 404 — 모두 BusinessException 상속. 영속성: time_slots + time_slot_votes(LocationRepositoryAdapter 미러), ScheduleJpaEntity 에 timeSlots cascade 컬렉션을 추가 — 없으면 일정 삭제 시 FK 위반(통합 테스트가 JdbcSQLIntegrityConstraintViolationException 으로 RED 를 먼저 보여준 뒤 추가). ScheduleRepositoryAdapter.save 가 관리 엔티티의 weeks 만 갱신해 orphanRemoval 이 후보를 지울 경로가 없음을 확인했다. 스키마는 ddl-auto update 로 자동 생성(추가 전용). API: POST /api/schedules/{id}/time-slots, POST /api/time-slots/{id}/votes, DELETE /api/time-slots/{id}/votes/{voterName} — /api/** permitAll·CSRF 제외에 이미 포함돼 SecurityConfig 무변경. ScheduleDetailResponse 에 timeSlots 를 더해 페이지와 GET 상세 API 양쪽에 싣는다. 프론트: 섹션은 달력 저장 버튼 아래·장소 투표 위, 뼈대만 SSR 하고 목록·겹침 차트는 신규 ES 모듈 schedule/timeslots.js 가 SCHEDULE_DATA.timeSlots 로 렌더(dayIndex->날짜 라벨이 달력 JS 에 이미 있어 Java/JS 이중 포매터 회피). 목록은 .location-item 마크업을 재사용해 장소·메뉴와 같은 모양, 투표 토글은 voting.js 의 getSelectedVoterName·toggleVoteFor 를 export 해 재사용. 날짜 드롭다운은 참여자 selections 합집합을 '12/20 (토) · 3명' 으로 보여주고, 달력 저장 후 main.js 가 refreshDayOptions 로 갱신(calendar.js 무수정). 겹침 차트는 후보가 있는 날짜마다 한 줄, 모든 줄이 공유하는 정시 축, 30분 칸마다 그 칸을 덮는 후보에 투표한 서로 다른 사람 수를 달력 heat 와 같은 초록 스케일로 칠하고, 최대 인원 칸을 같은 사람 집합끼리만 연속 구간으로 묶어 '최다 겹침 19:00–21:00 · 4명 (이름)' 으로 요약(인원 수만 같은 인접 칸 병합은 오표시라 기각). utils.js 에 dayIndexToDate, api.js 에 3개 메서드, dataLayer 에 time_slot_added 와 vote_cast.target='time'. CSS 는 style.css 에 +119줄(혼합 개행 파일이라 삽입 지점의 CRLF 를 따라 바이트 단위로 삽입 — 첫 시도의 전체 CRLF 정규화 diff 는 원복). i18n schedule.time.* 11개 + alert 1개(ko 는 \uXXXX 이스케이프). 검증(전 단계 RED 선확인 -> GREEN): TimeSlotTest 13, ScheduleTest 14(+2), TimeSlotServiceTest 7, TimeSlotApiControllerTest 6, TimeSlotPersistenceIntegrationTest 3(왕복·exists 쿼리·cascade — 단계 사이 flush+clear 로 요청별 영속성 컨텍스트 재현), ScheduleTimeSlotRenderingTest 3(SCHEDULE_DATA 주입·ko/en 키 누락 없음·상세 API) — 전체 스위트 122 클래스 753 테스트 failures 0 errors 0(build/test-results 실측). UI 는 bootRun 을 띄우지 않고(.env 가 코인·주식 봇 LIVE) 실제 style.css·schedule/*.js 를 가짜 SCHEDULE_DATA·api 로 싣는 스크래치 하네스를 Windows Chrome 헤드리스 CDP 로 돌려 확인: 날짜 라벨·인원, 칸 단계·최다 구간(투표 취소 후 18:00–21:00 · 3명 병합), 이름 미선택·종료<=시작·빈 상태 경고, 중복 토스트, 저장 후 드롭다운 갱신, 투표 후 포커스 복귀, 375px 레이아웃. 실서버 E2E(실 API 왕복)는 미실행 — API 계약은 MockMvc 통합 테스트가 보증."

# Commit 178 — ✅ DONE() docs(datedate): 시간 투표 ADR 0009 + CLAUDE.md·ADR README·데이터 인벤토리 동기화 — 마지막 커밋(git-commit.md 포함)
# 주의: 다음 SEO 세션 변경이 여기 흡수됨 — docs/adr/README.md 의 common/seo 0012·0013 행과 총계 93·common/seo 13(0012 는 Commit 179, 0013 은 Commit 180 소속 ADR),
#   datedate/application/CLAUDE.md 의 SeoService 문장(breadcrumb 3단계·ADR 0012 링크, Organization email·sameAs·ADR 0013 링크),
#   루트 CLAUDE.md 의 guides '기사 12편·템플릿 12개·분량 하한' 사실 정정(09-26 재검증 M4). 링크 대상 ADR 파일은 179·180 에서 커밋된다.
#   09-26 2차: README 의 0014 행·총계 94·common/seo 14, 루트·datedate CLAUDE.md 의 기사 metaTitle·제목 예산·FAQPage 키 사실(ADR 0014 는 Commit 182).
git add docs/adr/datedate/domain/0009-time-slot-vote.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/data-analysis/01-current-state.md docs/data-analysis/02-data-inventory.md docs/data-analysis/04-todo.md docs/guides/git-commit.md
git commit -m "docs(datedate): 시간 투표 ADR 0009 + CLAUDE.md·ADR README·데이터 인벤토리 동기화" -m "Commit 177 의 결정 기록과 사실 문서 동기화. ADR datedate/domain/0009 신설 — 새 도메인(timeslot) 추가라 CLAUDE.md 동기화 표의 새 도메인 행에 해당. 결정: 후보(dayIndex + 30분 단위 시간대) + 이름 투표 모델, dayIndex 저장(LocalDate 는 selections 인덱스와 이중화), 분 int(LocalTime 24:00 불가), 선택된 날 서버 강제(/api/** 공개라 UI 제약은 우회됨), 겹침 = 칸별 서로 다른 투표자 수·같은 사람 집합끼리 구간 병합, 목록·차트 JS 렌더. 범위 밖으로 뺀 것(삭제 API·UserActivity·인기 순위)과 dayIndex 가 주 수 변경 시 의미가 바뀌는 기존 한계 공유를 Consequences 에 기록. ADR README 두 뷰 갱신(datedate 도메인 모델 8->9, 합계 90->91, 시간순 행, 폴더 목록 9 ADRs). 루트 CLAUDE.md DateDate 절에 시간 투표 항목, datedate/application/CLAUDE.md Services 에 TimeSlotService(작업 트리 CRLF 파일 — attr eol=lf 라 커밋 시 LF 정규화, 실변경 +4). 데이터 문서: 01-current-state §9 이벤트 12종·15지점·14행으로 갱신(vote_cast timeslots.js 행, time_slot_added 행) + 변경 이력 행, 02-data-inventory 의 vote_cast 의미와 time_slots·time_slot_votes 테이블, 04-todo 의 GTM 트리거 정규식·DLV 목록(20개)·웨어하우스 덤프 테이블 목록 — GTM 워크스페이스 자체는 외부라 반영 필요(문서의 P0-1 작업). gradlew test 는 이 커밋 단독으로 영향 없음(마크다운만)."

# =====================================================================
# SEO 종합 점검 — 고아 허브 해소 + breadcrumb 3단계 복원 (2026-09-24)
# =====================================================================
# 배경: 사용자 요청 "sitemap.xml, 페이지별 seo 등 seo 요소 종합 점검" + (질문 응답으로) AdSense 통과에 유리한 방향 검토.
# 라이브 38 URL(19엔트리 x ko/en) 크롤 결과 기술 SEO 는 정상(200·self-canonical·hreflang·JSON-LD·중복 0).
# 핵심 발견은 링크 그래프 — /faq·/use-cases·/guides(ko) 로 들어오는 a 링크 0개(N1), 허브 신설 뒤에도 breadcrumb 2단계(N2).
# 보고서: docs/audit/seo-comprehensive-audit-2026-09-24.md (신규 발견 N1~N10, 기지 이슈 상태, AdSense 관점 §6).
# 사용자 결정: 수정 = N1·N2 + AdSense 관점 헤더 내비. 소형 정정(N3 게시일·N5 robots·N6·N7)·끝 슬래시(N4)는 권고만.
#   로케일 적응(ko URL + Accept-Language en -> en 본문/canonical)은 코드 변경 없이 기록·관찰(보고서 §10).
# 헤더: 시각 확인(bootRun 대신 라이브 HTML + 로컬 style.css 하네스, Windows Chrome 헤드리스)에서 EN 'Tips & Guides'
#   가 1024px 줄바꿈·800대 KO/EN 토글 밀림을 일으켜, 사용자 결정으로 전용 키 nav.guides(en 'Tips')로 단축.
#   잔여: EN 769~850px 토글 잘림(769~790 은 변경 전부터) — 보고서 §12 R8 CSS 후속.
# ADR: common/seo/0012 신설 — 0008 의 '2단계 고정' 결정 변경(동기화 표의 결정 변경 행). 0008 상태를 부분 Superseded 로.
#   푸터·헤더 링크 추가는 사실 변경이라 ADR 없음.
#   R2~R6 구현(전부 RED 선확인 -> GREEN). 소유권 규칙상 SeoService.java·SeoServiceI18nTest 변경(R2 코드·R5·R6) -> 179,
#   메시지 키(metaTitle·제목 단축·seo.faq 삭제) -> 180, R3(date-diff 링크 + en 누수 가드) -> 181 로 흡수. 새 커밋은 182(R2 가드 + ADR 0014),
#   183(R4 robots). 보고서 커밋은 184 로 재번호. 중간 상태 판정: 179 는 HEAD 메시지(faq.q1~q8 존재, metaTitle 없음 -> title 폴백)로
#   GREEN, 180 은 seo.faq 삭제 시점에 SeoService 가 이미 faq.* 사용, 제목 예산 가드는 metaTitle 이 들어온 뒤인 182 에서 등장.
#   최종 작업 트리 전체 스위트 124 클래스 790 테스트 failures 0 (09-26).
# 검증: 전 단계 TDD(RED 선확인 -> GREEN). 대상 7클래스 75테스트 GREEN, 전체 스위트 123 클래스 761 테스트 failures 0
#   (병행 세션 미커밋 변경을 포함한 작업 트리 기준). 헤더 라벨 변경 후 관련 6클래스 71테스트 재실행 GREEN.

# Commit 179 — ✅ DONE() feat(seo): 활용 사례·기사 BreadcrumbList 3단계(홈 -> 허브 -> 페이지) + 가시 breadcrumb (ADR common/seo/0012)
# 주의: Commit 180 작업분 흡수 — SeoService.java 의 Organization email·sameAs(홈·about), GuidesLocaleRenderingTest 의 MARKERS 12편 항목.
#   179 단독 상태에서도 무해: GuideSlugs 가 4편이라 추가 MARKERS 는 쓰이지 않고, email·sameAs 는 JSON-LD 문자열뿐이다.
# 주의(09-26 2차): 수정 계획 R2·R5·R6 의 SeoService 변경과 테스트도 여기 흡수 — getGuideArticleSeo 의 metaTitle 우선·title 폴백(R2),
#   getFaqSeo 가 화면 키 faq.q1~q8 로 빌드(R5), 홈 applicationCategory UtilitiesApplication(R6), SeoServiceI18nTest 의
#   faqSeo_jsonLdMatchesVisibleFaq·webApplicationJsonLd_usesSupportedCategory. 179 시점 메시지(HEAD)로 전부 GREEN.
# 주의(09-27): Commit 193 구현 흡수 — use-cases/detail·기사 4편 템플릿의 header/footer 호출부 변경과 인라인 FAQ 스크립트 -> 공용
#   static/js/faq-toggle.js include, GuidesLocaleRenderingTest DisplayName 정리. faq-toggle.js 는 이를 처음 참조하는 여기로 편입(중간 상태 404 방지).
#   179 시점 header/footer 는 HEAD 의 두 벌 fragment 라 [th:fragment=header]/[th:fragment=footer] 는 옛 기본 변형으로 렌더된다(무크래시).
git add src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/resources/templates/use-cases/detail.html src/main/resources/templates/guides/how-to-pick-a-date.html src/main/resources/templates/guides/scheduling-methods-compared.html src/main/resources/templates/guides/scheduling-etiquette.html src/main/resources/templates/guides/group-poll-best-practices.html src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/UseCaseLocaleRenderingTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidesLocaleRenderingTest.java docs/adr/common/seo/0012-breadcrumb-hub-hierarchy.md docs/adr/common/seo/0008-breadcrumb-item-on-every-listitem.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md src/main/resources/static/js/faq-toggle.js
git commit -m "feat(seo): 활용 사례·기사 BreadcrumbList 3단계(홈 -> 허브 -> 페이지) + 가시 breadcrumb (ADR common/seo/0012)" -m "ADR common/seo/0008 은 중간 크럼브가 가리킬 허브가 없어 BreadcrumbList 를 홈 -> 페이지 2단계로 고정하고 '허브를 만들면 3단계 복원' 을 후속으로 남겼다. /use-cases(09-08)·/guides(09-10) 허브가 생겼는데도 활용 사례 5편·기사 4편은 2단계였고 화면 breadcrumb 도 없었다(점검 N2). SeoService.breadcrumbJsonLd 를 '홈 + (이름, 경로) 크럼 목록' 루프 직렬화로 일반화해 전 ListItem 에 item 을 채우는 0008 불변식을 한 메서드에 유지했고, 기존 2단계 호출부 8곳은 오버로드로 그대로 두었다. getUseCaseSeo 는 홈 -> 활용 사례(/use-cases) -> 슬러그, getGuideArticleSeo 는 홈 -> 모임 노하우(/guides) -> 기사를 낸다(허브 이름은 기존 seo.breadcrumb.useCases/guides 키). 같은 계층의 가시 breadcrumb 를 use-cases/detail.html 과 기사 전용 템플릿 4개에 faq.html 과 같은 nav.breadcrumb 마크업·기존 CSS 로 넣었다 — 신규 메시지 키 없음, 허브 링크는 localeLinks.href 로 로케일 유지, 현재 항목은 짧은 navLabel. 구조화 데이터와 화면 계층이 일치하고 상세마다 허브로 가는 본문 링크가 생겨 허브 고아(N1)도 함께 풀린다. RED 선확인: SeoServiceI18nTest.breadcrumbList_useCaseAndGuideArticlesIncludeHub(expected 3 but was 2), UseCaseLocaleRenderingTest.detailBreadcrumbLinksHub·GuidesLocaleRenderingTest.articleBreadcrumbLinksHub x4(nav 부재) -> GREEN, 기존 breadcrumbList_everyItemHasUrl 무변경 통과. ADR 0012 신설, 0008 상태를 '2단계 고정 결정은 0012 로 Superseded(전 항목 item 규칙 유효)' 로, common/CLAUDE.md 의 SEO ADR 수를 12 로 정정. docs/adr/README.md·datedate/application/CLAUDE.md 동기화분은 Commit 178 이 흡수(주의 주석). 함께 담긴 흡수분(주의 주석): Commit 180 의 Organization email·sameAs 와 기사 MARKERS 12편, 수정 계획 R2 코드(기사 metaTitle 우선·title 폴백)·R5(/faq FAQPage 를 화면 키 faq.q1~q8 로 — 6문항 의역 -> 8문항 일치, RED expected 8 but was 6)·R6(홈 applicationCategory SchedulingApplication -> Google 지원값 UtilitiesApplication, RED 로 미지원 값 확인)."

# =====================================================================
# guides 기사 8편 확장 + 퍼블리셔 신원(연락처 이메일·sameAs) (2026-09-24~26)
# =====================================================================
# 배경: AdSense '낮은 가치 콘텐츠' 3차 통지 대응의 콘텐츠·신원 보강(작업 기록상 Phase 1·2). 사용자 작업 —
#   이 섹션은 09-26 재검증 세션이 작업 트리와 작업 요약을 대조해 사후 작성했다(요약의 주장은 코드로 확인, 보고서 §11-2).
# Phase 1 신원: 연락처 cheongyakplanet@gmail.com 을 푸터·/about 에 표시, Organization JSON-LD 에 email + sameAs
#   (https://github.com/singingsandhill — 저장소 소유 실계정, HTTP 200 확인, 화면 비노출). 08-17 진단의 'sameAs 보류' 결정을
#   바꾸는 것이라 ADR common/seo/0013 을 이 커밋에 둔다(ADR-0011 결정 6 의 허위 신원 금지는 준수).
# Phase 2 기사 8편: timezone-coordination, annual-dinner-scheduling, wedding-invitation-gathering, study-team-project-management,
#   calendar-vs-poll-comparison, meeting-place-and-budget, weekend-vs-weekday-meetup, last-minute-cancellation-etiquette.
#   기사별 템플릿·메시지 키 블록(ko/en 각 43~63키) + GuideSlugs SSOT(2026-09-24) + 사이트맵 테스트 26페이지 x 6 = 156.
# 재검증 발견(보고서 §11·§12): 게시일 2026-09-24 가 배포 전 날짜(N3 재발 — R1 은 사용자 결정으로 제외, 현행 유지), en 제목 90~100자(N10 -> R2 완료),
#   새 8편 템플릿 골격 유사도 0.96(본문은 고유 — M1 -> R10), 연락처 하드코딩 6곳·로컬파트 브랜드 확인(M2 -> R11).
# 다른 커밋 소유분: SeoService.java·GuidesLocaleRenderingTest -> 179, footer.html -> 181, CLAUDE.md·README·datedate CLAUDE.md -> 178.

# Commit 180 — ✅ DONE() feat(datedate): guides 기사 8편 확장(4 -> 12편) + 퍼블리셔 연락처·sameAs (ADR common/seo/0013)
git add src/main/java/me/singingsandhill/calendar/datedate/domain/guide/GuideSlugs.java src/main/resources/templates/guides/timezone-coordination.html src/main/resources/templates/guides/annual-dinner-scheduling.html src/main/resources/templates/guides/wedding-invitation-gathering.html src/main/resources/templates/guides/study-team-project-management.html src/main/resources/templates/guides/calendar-vs-poll-comparison.html src/main/resources/templates/guides/meeting-place-and-budget.html src/main/resources/templates/guides/weekend-vs-weekday-meetup.html src/main/resources/templates/guides/last-minute-cancellation-etiquette.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css src/main/resources/templates/about.html src/test/java/me/singingsandhill/calendar/common/application/service/SitemapServiceHreflangTest.java src/test/java/me/singingsandhill/calendar/common/application/service/SitemapServiceWhitelistTest.java docs/adr/common/seo/0013-publisher-identity-contact-and-sameas.md
git commit -m "feat(datedate): guides 기사 8편 확장(4 -> 12편) + 퍼블리셔 연락처·sameAs (ADR common/seo/0013)" -m "AdSense 3차 통지 대응의 콘텐츠·신원 보강. ① 기사 8편 신설 — 시차 조율(시차별 회의 시간대 매트릭스 표), 연말·신년 회식(6단계 체크리스트), 청첩장 모임(초대 문구 템플릿 3종), 조모임·스터디(그라운드 룰 체크리스트), 공유 캘린더 vs 링크 투표(6기준 비교표), 장소·예산(인원수별 가이드표), 주말 낮 vs 평일 저녁(의사결정 매트릭스), 당일 취소·재조율(사과·대안 제안 템플릿 3종). 기사별 전용 템플릿 + guides.article.<slug>.* 키 블록(ko/en 각 43~63키) + GuideSlugs SSOT 등록으로 라우팅·사이트맵·푸터가 자동 반영된다. 각 템플릿은 Commit 179 와 같은 가시 breadcrumb(홈 / 모임 노하우 / 기사)를 포함하고 JSON-LD 3단계는 SeoService 가 낸다. 분량 하한(ko 3,800자·en 1,100단어), 로케일 고유 마커, 미해석 키 부재, 설명 120~160자를 기존 가드가 12편 전부에 적용해 GREEN(작업 기록상 ko 4,250~5,152자·en 1,212~1,454단어, 플레이스홀더 0건 확인). 사이트맵 테스트는 26페이지 x 6 = hreflang 156 으로 갱신. ② 퍼블리셔 신원 — /about 연락 섹션에 이메일 줄(about.section.contact.emailLabel)과 문구 갱신, 푸터 문의 키(footer.contact)·.footer-contact 스타일 추가(푸터 마크업은 Commit 181 소유), Organization JSON-LD 의 email·sameAs(운영자 GitHub, 화면 비노출)는 Commit 179 가 흡수. 08-17 진단의 'sameAs 보류' 를 실존 계정 한정으로 바꾸는 결정이라 ADR common/seo/0013 신설. 메시지 파일에 흡수된 SEO 수정분(주의 주석): 기사 12편 metaTitle(SERP 제목 예산 ko 35/en 60 — 가드는 Commit 182), insights ko/en·홈 en 제목 단축, 미사용이 된 seo.faq.q1~q6/a1~a6 삭제. 알려진 후속(보고서 §12): 게시일 2026-09-24 는 배포 전 날짜(N3 — R1 은 사용자 결정으로 제외), 새 8편 골격 유사도 0.96 — 본문은 편 사이 중복 0(R10), 연락처 하드코딩 6곳(R11), 메시지 파일 CRLF 혼재(M6). 최종 작업 트리 전체 스위트 124 클래스 790 테스트 failures 0."

# Commit 181 — ✅ DONE() fix(seo): 내부 링크 정비 — 고아 허브 해소(헤더 모임 노하우·푸터 FAQ) + en 로케일 누수 가드·date-diff 2곳
git add src/main/resources/templates/fragments/header.html src/main/resources/templates/fragments/footer.html src/main/resources/templates/tools/date-diff.html src/test/java/me/singingsandhill/calendar/common/presentation/InternalLinkingTest.java src/main/resources/templates/index.html src/main/resources/templates/faq.html src/main/resources/templates/guide.html src/main/resources/templates/privacy.html src/main/resources/templates/terms.html src/main/resources/templates/use-cases/index.html src/main/resources/templates/guides/index.html
git commit -m "fix(seo): 내부 링크 정비 — 고아 허브 해소(헤더 모임 노하우·푸터 FAQ) + en 로케일 누수 가드·date-diff 2곳" -m "2026-09-24 점검 N1: 라이브 38 URL 의 a 링크 그래프에서 /faq·/use-cases·/guides(ko) 로 들어오는 링크가 0개였다 — 사이트맵으로만 발견 가능하고 사용자·AdSense 리뷰어는 도달할 경로가 없었다(/faq 는 도입 이후 한 번도 링크된 적 없음, git log -S 확인). 헤더 header·header-minimal 두 내비에 '모임 노하우'(/guides)를 가이드 다음에 추가했다 — 에디토리얼 허브를 1차 내비에 노출하는 AdSense 관점 조치. 라벨은 전용 키 nav.guides(ko '모임 노하우', en 'Tips'): 1차안 'Tips & Guides' 는 시각 확인에서 EN 1024px 줄바꿈과 800대 KO/EN 토글 밀림을 일으켜 사용자 결정으로 단축했다(키 2줄은 Commit 177 흡수, EN 769~850px 잔여 넘침은 보고서 §8 CSS 후속 — 769~790 은 변경 전부터). 푸터 footer-minimal 도움말 섹션에는 /faq 를 기존 키 seo.breadcrumb.faq 로 추가(병행 세션 소유 파일을 피하려 신규 키·CSS 없음). /use-cases 유입은 Commit 179 의 상세 breadcrumb 가 맡는다. 신규 InternalLinkingTest: 사이트맵 XML 의 전 loc(ko+en)을 MockMvc 로 렌더해 a 태그 href 만 모아(head 의 canonical·hreflang link 는 제외 — 첫 작성에서 이를 링크로 세어 RED 가 안 나던 것을 잡아 수정) 각 URL 이 다른 사이트맵 페이지에서 1회 이상 링크되는지 단정, 헤더 두 변형의 /guides 로케일 링크도 단정. RED 선확인: 고아 [/faq, /use-cases, /guides] — 라이브 크롤과 같은 집합 -> GREEN. 09-26 수정 계획 R3(보고서 §12-1): tools/date-diff.html 의 breadcrumb 홈(@{/})과 CTA(href=/#start-form)가 en 페이지에서 ko URL 로 새던 것을 localeLinks.href 로 고치고, InternalLinkingTest.englishPagesLinkEnglishUrls 로 en 사이트맵 전 페이지의 내부 a 링크가 lang=en 을 유지하는지 가드(예외: ?lang=ko 토글·/oauth2/). RED: date-diff?lang=en -> / 1건(두 링크가 같은 URL 로 정규화) -> GREEN."

# Commit 182 — ✅ DONE() fix(seo): SERP 제목 예산 가드 ko 35 / en 60 (ADR common/seo/0014)
# 주의: 구현은 흡수됨 — SeoService 의 metaTitle 우선·title 폴백은 Commit 179, 기사 metaTitle 키·insights/홈 제목 단축은 Commit 180.
#   이 커밋은 가드와 결정 기록만 담는다. 가드가 메시지 반영 뒤인 여기서 처음 등장하므로 179·180 중간 상태를 깨지 않는다.
git add src/test/java/me/singingsandhill/calendar/datedate/application/service/SerpTitleBudgetTest.java docs/adr/common/seo/0014-serp-title-budget-and-meta-title.md
git commit -m "fix(seo): SERP 제목 예산 가드 ko 35 / en 60 (ADR common/seo/0014)" -m "점검 N10: 색인 페이지 title 길이 기준이 없어 기사 8편 추가 뒤 예산 초과가 ko 12건·en 12건(26페이지 기준, en 기사 제목 90~100자)이었다. Google 은 제목을 약 600px 에서 자르므로 브랜드 접미 ' | DateDate' 포함 ko 35자 / en 60자(한글 폭 약 2배) 예산을 신규 SerpTitleBudgetTest 로 고정한다 — 색인 26페이지 x ko/en 전수, 초과 목록 전체를 한 번에 보고. 기사는 title 이 h1·Article headline·breadcrumb·허브 카드와 공유되므로 선택 키 guides.article.<slug>.metaTitle 을 두고 title·og:title 만 짧게 쓴다(사용자가 권장안 채택, 대안인 title 자체 단축은 h1 설명력 손실로 기각). RED: 초과 24건 -> GREEN(ko 27~35자, en 45~60자). 구현 흡수 위치는 주의 주석. ADR common/seo/0014 신설 — 제목 예산이라는 새 임계 정책 + 키 분리 구조라 결정 기록(설명 120~160자 가드는 ADR 없이 도입된 전례가 있으나 이번은 구조 변경을 동반)."

# Commit 183 — ✅ DONE() fix(seo): robots.txt 에 /oauth2/ 크롤 차단
git add src/main/resources/static/robots.txt src/test/java/me/singingsandhill/calendar/common/presentation/controller/SitemapEndpointTest.java
git commit -m "fix(seo): robots.txt 에 /oauth2/ 크롤 차단" -m "점검 N5: 전 페이지 헤더의 카카오 로그인 링크(/oauth2/authorization/kakao)가 38 페이지에 80회 크롤 가능 상태였다 — 따라가면 kauth.kakao.com 으로 302 되고 요청마다 세션·state 가 생길 뿐 색인 가치가 없다. robots.txt 에 Disallow: /oauth2/ 를 추가(파일 CRLF 유지). SitemapEndpointTest 에 robotsTxtBlocksOAuthStart 를 추가해 기존 최장 패턴 우선 판정기로 차단을 단정 — RED(차단 안 됨) -> GREEN. 기존 robotsTxtDoesNotBlockSitemapUrls 도 GREEN 유지(사이트맵 URL 과 충돌 없음). ADR 없음 — 크롤 대상이 아닌 인증 경로 한 줄 차단이라 ADR-0005 의 UGC 차단 설계와 무관한 사실 변경."

# Commit 184 — ✅ DONE() docs(seo): SEO 종합 점검 보고서 2026-09-24 + 09-26 재검증·수정 계획·구현 결과 — 마지막 커밋(git-commit.md 포함)
git add docs/audit/seo-comprehensive-audit-2026-09-24.md docs/guides/git-commit.md
git commit -m "docs(seo): SEO 종합 점검 보고서 2026-09-24 + 09-26 재검증·수정 계획·구현 결과 — 사이트맵·페이지별 메타·링크 그래프·구조화 데이터 + AdSense 관점" -m "08-02 사이트맵·08-16 페이지별·08-17 AdSense 진단 이후 추가된 /use-cases·/guides 허브와 기사 4편을 포함해 라이브 38 URL 을 전수 실측했다. 정상: 38/38 200·self-canonical·og:url·hreflang 3종 상호참조·index,follow·html lang·og:locale·h1 1개·유효 JSON-LD, 제목·설명 중복 0, http/www 301, /tools·/insights 308, 미지 슬러그 404+noindex, robots.txt 배포본 일치, AdSense 쿠키 고지 존재. 신규 발견 N1~N10: 中 = 허브·FAQ 고아(N1, 수정 181)·breadcrumb 2단계(N2, 수정 179)·기사 게시일이 계획일 08-23 로 실제 커밋 09-10~12 보다 앞섬(N3); 低 = 끝 슬래시가 /login 302·404(N4, UrlHandlerFilter 권고)·OAuth 시작 링크 80개 크롤 가능(N5)·date-diff en 로케일 누수 2곳(N6)·홈 applicationCategory 미지원값 + 소프트웨어 앱 리치 결과는 평점 필수라 비적격(N7, 가짜 평점 금지)·HowTo/FAQ 가 Google 검색 갤러리에 없음(N8)·FAQPage 답변 의역(N9)·en 제목 60자 초과 4건(N10). 로케일 적응은 08-17 의 '의도된 설계' 판정에 크롤러 관점 위험과 GSC 관찰 절차만 보강(사용자 결정: 관찰). 기지 이슈 상태표 갱신(og-image.svg 데드 화이트리스트는 해소 확인). §6 AdSense 관점: 이번 조치는 탐색성 보강이고 editorial 판정 동인은 아님, 재검토 요청 절차(심사 중이면 판정 후 배포), 다음 권고(N3·홈 FAQ 축소·FAQ 신규 주제·기사 추가 우선), 하지 말 것(가짜 평점·실체 없는 저자), 광고 재개 전 필수(예약 높이·EEA/UK CMP). §11 09-26 재검증: 라이브 미배포라 실측은 유효, 코드 기준 N1·N2 해결 유지(12편 포함), N3 재발·N10 악화(en 제목 60자 초과 12건), 신규 M1(새 8편 골격 유사도 0.96, 본문 중복 0)·M2(sameAs 결정 변경 -> ADR 0013, 연락처 하드코딩 6곳)·M3(기사 10편 초과 fragment 재검토 조건 충족)·M4(CLAUDE.md 드리프트 정정, Commit 178 흡수)·M5(커밋 로그 순서 충돌 -> 180 삽입·재번호). §12 수정 계획: Phase 0(R1 게시일·R2 제목 — 배포 전 필수), Phase 1(R3 date-diff 누수·R4 robots /oauth2/·R5 FAQ 문구·R6 앱 카테고리), Phase 2(R7 끝 슬래시·R8 EN 헤더·R9 기사 공통 크롬 fragment·R10 골격 다양화·R11 연락처), Phase 3(배포 후 실측·색인 확인·재검토 요청). 09-26 2차: R1 은 사용자 결정으로 제외, R2(metaTitle)·R6(UtilitiesApplication) 권장안 확정, §12-1 에 R2~R6 구현 결과(RED/변경/가드)와 신규 M6(메시지 파일 CRLF 혼재) 기록. 이 커밋은 문서만."

# =====================================================================
# AdSense 심사·게재 준비 상태 재분석 (2026-09-26)
# =====================================================================
# 배경: 사용자 요청 "adsense 심사 관점에서 미비점 재분석 — sitemap.xml, ads.txt 등 기술 요인 및 도메인 유효성 종합 검사,
#   최종 목표는 통과 후 광고 게재". 보고서: docs/audit/adsense-readiness-audit-2026-09-26.md.
# 결론: 기술·도메인 차단 요인 없음(RDAP·DNS·TLS·Safe Browsing·크롤러 UA 3종·ads.txt 4변형·사이트맵 38 URL·헤더·개인정보 고지 정상).
#   심사 미비점은 개선분 미배포(G2). 게재 차단 결함 G1 — adsense.client 에 플레이스홀더가 없고 운영 비밀값은 .env 파일
#   임포트(프로퍼티 소스)라 ADSENSE_CLIENT 설정만으로는 광고가 켜지지 않음. 임시 @SpringBootTest 프로브로 실측
#   (client=[] slotInfeed=[999]) 후 프로브 파일 삭제.
# 코드 변경 없음 — G1 수정은 배포 즉시 서버 .env 의 ADSENSE_CLIENT 잔존 여부에 따라 승인 전 광고를 켤 수 있어,
#   사용자 확인(서버 .env) 뒤로 보류(보고서 §4 G1·§5 로드맵).

# Commit 185 — ✅ DONE() docs(adsense): 심사·게재 준비 상태 재분석 보고서 — 마지막 커밋(git-commit.md 포함)
git add docs/audit/adsense-readiness-audit-2026-09-26.md docs/guides/git-commit.md
git commit -m "docs(adsense): 심사·게재 준비 상태 재분석 — 기술·도메인 정상, 개선분 미배포, ADSENSE_CLIENT 바인딩 결함" -m "AdSense 재검토 통과와 실제 광고 게재를 목표로 미비점을 재분석했다. 라이브 실측(09-26): 도메인 RDAP 등록 2025-12-14·만료 2026-12-14(79일 — 자동 연장 확인 필요), DNS apex·www 동일 A, TLS Let's Encrypt SAN apex+www 만료 11-09(44일 — certbot 타이머 확인 필요), Safe Browsing 위협 플래그 없음, http·www 301 1홉, Mediapartners-Google·AdsBot-Google·Googlebot 으로 주요 경로 전부 200(두 광고 크롤러는 robots 의 * 그룹을 무시 — 공식 문서), ads.txt 는 인증 ID 포함 정상 형식·4변형 200 text/plain 이나 라이브는 아직 CRLF(커밋 176 미배포), 사이트맵 38 URL 에 CSP·COEP·X-Robots-Tag·미완성 문구 0, 개인정보처리방침 AdSense 쿠키·맞춤 광고 해제 고지 존재, 배포 503 에 Retry-After 120. 사이트 소유권 연결은 ads.txt 방식으로 충족(google-adsense-account 메타는 선택). 미비점: G1 게재 차단 — application.yaml 의 adsense.client 가 빈 값 고정이고 운영은 .env 파일 임포트라 ADSENSE_CLIENT 가 바인딩되지 않음(임시 프로브 실측 client 빈 값, 슬롯은 바인딩 — 확인 후 삭제). 문서화된 게재 절차가 조용히 실패하므로 플레이스홀더·.env.example·바인딩 테스트로 고쳐야 하나, 서버 .env 에 ADSENSE_CLIENT 가 남아 있으면 배포 즉시 승인 전 광고가 켜지므로 확인 뒤로 보류. G2 개선분 전부 미배포(재검토는 배포·색인 확인 후). G3 ads.txt CRLF(다음 배포로 해소). G4 도메인·인증서 만료. G5 인피드 예약 높이 200px·자동 광고 결정. G6 EEA/UK/CH 인증 CMP(AdSense 개인 정보 보호 메시지). G7 콘텐츠 잔여 신호(기존 기록). G8 /stock 무로그인 공개(noindex·무링크). G9 저장소 밖 확인 항목. §5 에 게재까지 5단계 로드맵. 코드 변경 없음."

# =====================================================================
# datedate ERD 다이어그램 (2026-09-26)
# =====================================================================
# 배경: 사용자 요청 "draw db erd" (diagram-design 스킬). 사용자 선택 — 범위 datedate 12개 테이블, 기본 스킨, docs/ 에 저장.
#   ER 예산(장당 8개)에 맞춰 2장으로 분할: Fig. 1 계정·소유·참여 / Fig. 2 후보·투표.
# 근거: datedate/infrastructure/persistence/entity/*JpaEntity.java + 도메인·서비스 코드 (문서 인용 없음).
# 코드 변경 없음, 결정 변경 아님(ADR 불필요). 검증: 스킬 self_check 통과, verify-geometry SVG 별 0건
#   (파일 단위 실행 시 1건은 서로 다른 SVG 좌표를 비교한 오탐), Windows 헤드리스 Chrome 렌더 확인.

# Commit 186 — ✅ DONE() docs(datedate): ERD 다이어그램 (계정·소유·참여 / 후보·투표) — 마지막 커밋(git-commit.md 포함)
git add docs/datedate/erd.html docs/README.md docs/guides/git-commit.md
git commit -m "docs(datedate): ERD 다이어그램 — 계정·소유·참여 / 후보·투표 2장" -m "datedate 12개 테이블을 JPA 엔티티 기준으로 그린 단일 HTML(인라인 SVG, diagram-design 기본 스킨). Fig. 1 은 DB FK 로 묶인 익명 일정 영역(owners -> schedules -> participants, 일정당 참여자 0..8)과 FK 없이 user_id·schedule_id 값만 가지는 카카오 로그인 영역(app_users·user_activities·recap_shares)을 나누고, owners.user_id first-claim 연결을 강조. Fig. 2 는 schedules 아래 후보 3종(locations·menus·time_slots)과 후보별 투표 테이블. 하단 카드에 DB 제약(FK·UQ·IDX), 코드만 보장하는 규칙(참여자 8명·이름 중복, 후보·투표 중복, 활동 중복 억제), 삭제 전파(JPA cascade ALL + orphanRemoval, FK 에 ON DELETE 없음, user_activities·recap_shares 미정리)를 정리. docs/README.md 의 datedate 행에 ERD 추가."

# =====================================================================
# trading · stock ERD 다이어그램 (2026-09-26)
# =====================================================================
# 배경: 사용자 요청 "trading, stock ERD도 그려줘" — Commit 186(datedate ERD)과 같은 방식(기본 스킨, 모듈 폴더의 erd.html).
#   두 모듈 모두 JPA 연관관계가 없어 DB FK 가 없다 — 모든 참조를 점선(논리 참조)으로 그렸다.
# 근거: 각 모듈 infrastructure/persistence/entity/*JpaEntity.java + 서비스·스케줄러 코드 (호출처·보관 주기는 grep 으로 확인).
# 코드 변경 없음, 결정 변경 아님(ADR 불필요). 검증: 스킬 self_check·verify-geometry 각 0건, Windows 헤드리스 Chrome 렌더 확인.

# Commit 187 — ✅ DONE() docs(datedate): docs(trading,stock): ERD 다이어그램 — 마지막 커밋(git-commit.md 포함)
# 주의: docs/README.md 의 trading·stock 행 ERD 표기는 Commit 186 이 파일을 먼저 소유해 186 에 흡수됨.
git add docs/trading/erd.html docs/stock/erd.html docs/guides/git-commit.md
git commit -m "docs(trading,stock): ERD 다이어그램 — 코인 봇 7개·주식 봇 6개 테이블" -m "datedate ERD(Commit 186)와 같은 형식의 단일 HTML(인라인 SVG). trading: 신호 -> 주문 <- 포지션을 FK 없는 signal_id·position_id 로 잇는 매매 영역과, id 참조 없이 시간으로만 이어지는 시계열 4개(캔들 90일 보관·5분 계좌 스냅샷·00:01 일일 요약·이벤트 로그)를 나눠 그림. 주문 레코드(trading_trades)를 강조하고, 매수·신규 포지션 동일 트랜잭션 저장, client_order_id 선영속화 멱등키, 캔들 외 삭제 경로 없음을 카드로 정리. stock: 종목·거래일당 1행인 stock_monitoring 을 중심으로 포지션(stock_id)·주문(position_id)은 논리 id 참조, 신호·진입 시도는 stock_code·날짜 자연키 대응으로 구분하고 stock_candles 는 쓰는 코드가 없는 비활성 스캐폴딩으로 표시. 모든 stock 테이블은 삭제 호출처가 없어 누적됨을 기록."
    
# =====================================================================
# trading 매매 시퀀스 다이어그램 (2026-09-27)
# =====================================================================
# 배경: 사용자 요청 "visualize coin trading sequance" (diagram-design 스킬). ERD(Commit 187)와 같은 방식(기본 스킨, 모듈 폴더의 단일 HTML).
#   시퀀스 예산(레인 5·메시지 12·프래그먼트 1)에 맞춰 2장으로 분할: Fig. 1 1분 틱(executeTradeLoop 0~6단계) / Fig. 2 매수 한 건(executeBuy).
# 근거: TradingBotService·RiskManagementService·SignalService·CandleService·BithumbApiClient·CandleScheduler 코드 + application.yaml·TradingProperties 기본값 (문서 인용 없음).
# 코드 변경 없음, 결정 변경 아님(ADR 불필요). 검증: 스킬 self_check 0건, verify-geometry SVG 별 0건, Windows 헤드리스 Chrome 렌더 확인(1x 페이지 + 2x SVG 별).

# Commit 188 — ✅ DONE() docs(trading): 매매 시퀀스 다이어그램 (1분 틱 / 매수 한 건) — 마지막 커밋(git-commit.md 포함)
# 주의: docs/README.md 의 trading 행 "매매 시퀀스 trade-sequence.html" 표기는 Commit 186 이 파일을 먼저 소유해 186 에 흡수됨.
git add docs/trading/trade-sequence.html docs/guides/git-commit.md
git commit -m "docs(trading): 매매 시퀀스 다이어그램 — 1분 틱 / 매수 한 건 2장" -m "ERD(Commit 187)와 같은 형식의 단일 HTML(인라인 SVG, diagram-design 기본 스킨). Fig. 1 은 CandleScheduler(매분 5초) -> TradingBotService.executeTradeLoop 한 틱: 0 SUBMITTED 스윕, 1 캔들 동기화, 2 리스크 체크(강조 — 신호보다 먼저인 순서 불변식), 3 신호 계산·저장, 게이트(청산·stale 캔들·신호 없음·복구 모드면 매매 없이 반환), ALT 로 |점수|>=60 강신호 매매(4) 대 리밸런싱 우선(5) 후 신호 매매(6). Fig. 2 는 executeBuy: 서킷브레이커·미결 주문, 잔고·현재가, 물타기·비중 상한·ATR 사이징, cid 구성일 때만 SUBMITTED 선영속화, BithumbApiClient 모드 게이트 ALT(LIVE 실주문 / PAPER 현재가+0.5% 가상 체결), Position+Trade 한 트랜잭션 저장(강조). 카드에 순서 불변식, 주문 경로 안전장치(트랜잭션 밖 HTTP·미결 주문 차단·스윕 규칙), 기본 구성(PAPER·V1+cid OFF 라 선영속화 비활성)을 정리."

# =====================================================================
# trading 매매 판단 플로차트 (2026-09-27)
# =====================================================================
# 배경: 사용자 요청 "매매 판단의 구체적인 다이어그램도 작성" — Commit 188(매매 시퀀스)의 후속, 같은 형식(기본 스킨, 모듈 폴더의 단일 HTML).
#   플로차트 예산(장당 노드 9·화살표 12)에 맞춰 5장: Fig. 1 신호 판정 / Fig. 2a·2b 신호 매수·매도 게이트 / Fig. 3a·3b 리스크 청산·리밸런싱 (2·3 은 2열 패널).
# 근거: SignalService.determineSignalType·점수 계산, TradingBotService.executeTradeBySignal·executeBuy·isStrongSellSignal,
#   RiskManagementService.checkPositionRisk·shouldTimeExit, RebalanceService.checkAndExecute·sellByClosingProfitablePositions + application.yaml·TradingProperties (문서 인용 없음).
# 코드 변경 없음, 결정 변경 아님(ADR 불필요). 검증: 스킬 self_check 0건, verify-geometry SVG 별 0건(5장), 제너레이터 내장 assert(노드 관통·미처리 교차·라벨 겹침·텍스트 폭),
#   Windows 헤드리스 Chrome 렌더 확인(1x 페이지 + 2x SVG 별).

# Commit 189 — ✅ DONE() docs(trading): 매매 판단 플로차트 (신호 판정 / 신호 매수·매도 / 리스크 청산·리밸런싱) — 마지막 커밋(git-commit.md 포함)
# 주의: docs/README.md 의 trading 행 "매매 판단 trade-decision.html" 표기는 Commit 186 이 파일을 먼저 소유해 186 에 흡수됨.
git add docs/trading/trade-decision.html docs/guides/git-commit.md
git commit -m "docs(trading): 매매 판단 플로차트 — 신호 판정 / 신호가 주문이 되기까지 / 신호 밖의 주문" -m "매매 시퀀스(Commit 188)의 각 단계 안쪽 판단을 조건 단위로 그린 단일 HTML(인라인 SVG 5장, diagram-design 기본 스킨). Fig. 1 신호 판정: 동의 지표 3개(총점과 같은 부호) -> 매수 조건(점수 40·RSI 70·%K 85) -> MA60 아래면 반전 근거(강세 다이버전스·RSI 30·거래량 1.5배) 필수(강조) -> 아니면 매도 조건(−40·30·15) -> HOLD, 8개 지표 점수표(±128) 첨부. Fig. 2 신호 -> 주문: BUY 는 쿨다운 30분·포지션 2개 -> 서킷브레이커·미결 주문 -> KRW 5,000·현재가 -> 물타기·비중 80% -> ATR 비중 15~35%, SELL 은 포지션마다 최소 보유 30분 -> 강한 SELL(−60 또는 약세 다이버전스)이면 순손익 −2% 이상, 아니면 +0.1% 이상. Fig. 3 신호 밖의 주문: 리스크 청산 우선순위(손절 -> 트레일 갱신·이탈 -> 익절 -> 360분 정체+손익분기), 리밸런싱(8시간 쿨다운 -> MA60 목표 70/30/50% -> 편차 10%p·5,000원 -> 부족이면 매수+Position, 과다면 수익 포지션만 FIFO 매도). 카드에 수치 출처를 yaml 운영값 / Java 기본값 / 코드 하드코딩으로 구분."

# =====================================================================
# stock 매매 판단 다이어그램 (2026-09-27)
# =====================================================================
# 배경: 사용자 요청 "stock 봇 매매 판단 다이어그램도 작성" — Commit 189(trading 매매 판단)와 같은 형식(기본 스킨, 모듈 폴더의 trade-decision.html).
#   4장: Fig. 1 09:20 스크리닝(플로차트) / Fig. 2 종목 상태 머신(state) / Fig. 3 진입(플로차트) / Fig. 4 청산(플로차트) + 팩터·청산 표.
# 근거: GapPullbackBotService·ScreeningService·PullbackDetectionService·StockPositionService·StockRiskService, Stock·StockPosition 도메인,
#   application.yaml·StockProperties (문서 인용 없음 — 모듈 CLAUDE.md 는 참고만, 수치·호출처는 코드로 확인).
# 코드 변경 없음, 결정 변경 아님(ADR 불필요). 검증: 스킬 self_check 0건, verify-geometry SVG 별 0건(4장), 제너레이터 assert, Windows 헤드리스 Chrome 렌더(1x 페이지 + 2x SVG 별).
# 참고(코드 관찰, 미수정): ① HIGH_FORMED 의 고점(highAfterOpen)은 +1.5% 를 처음 넘긴 틱 가격으로 고정되고 이후 갱신되지 않음 — 눌림 폭이 실제 고점이 아니라 이 값 기준.
#   ② Stock 의 EXITED 전이는 closePosition(손절·트레일링·시간·긴급) 경로뿐 — 익절로 잔량이 소진되면 ENTERED 로 남음.

# Commit 190 — ✅ DONE() docs(stock): 매매 판단 다이어그램 (스크리닝 / 상태 머신 / 진입 / 청산) — 마지막 커밋(git-commit.md 포함)
# 주의: docs/README.md 의 stock 행 "매매 판단 trade-decision.html" 표기는 Commit 186 이 파일을 먼저 소유해 186 에 흡수됨.
git add docs/stock/trade-decision.html docs/guides/git-commit.md
git commit -m "docs(stock): 매매 판단 다이어그램 — 스크리닝 / 종목 상태 머신 / 진입 / 청산" -m "trading 매매 판단(Commit 189)과 같은 형식의 단일 HTML(인라인 SVG 4장). Fig. 1 09:20 스크리닝: 거래 가능·시가 -> 갭 0.5~15% -> 체결강도 95 -> 시총 500억 -> 5팩터 복합 점수 -> 총점 40 과 갭+체결강도 25 두 관문(강조) -> 최대 10종목 WATCHING, 옆에 팩터 정규화·가중 표. Fig. 2 종목 상태 머신: WATCHING -> HIGH_FORMED(시가 +1.5%) -> PULLBACK(1.5~5%) -> ENTRY_READY(반등 0.2% + 검증 2/3, 강조) -> ENTERED -> EXITED, 5% 초과 눌림은 FILTERED_OUT. Fig. 3 진입: 검증 3개(체결강도 100·잔량비 1.0·눌림 3~15분) 2개 이상 -> 포지션 5 미만 -> 거래 가능 재확인 -> 현금 10%·최대 500만원 -> 시장가 매수. Fig. 4 청산: 손절·트레일링 전량 -> TP3/TP2/TP1 중 가장 강한 가격 조건 -> 순익 게이트(슬리피지 차감, 최소수익 0.5%->0.1% 시간 감쇠, 강조) -> 부분 매도 -> 트레일·당일고가 갱신, 옆에 청산 규칙 표. 카드에 yaml 운영값 / Java 기본값 / yaml 에 있지만 안 쓰는 키(legacy 스크리닝 6개·min-candidates·stop-loss-percent)를 구분."

# =====================================================================
# FE 공통 크롬 통일 — 헤더·푸터 단일 fragment + 푸터 재설계 + trading·stock 토스트 + 러너 어드민 nav (2026-09-27)
# =====================================================================
# 배경: 사용자 지적 "footer 등 FE 요소가 추상화되지 않아 페이지별로 다르다" + "푸터가 데스크톱·모바일 모두 완성도가 떨어진다".
#   분석 후 사용자 결정: 헤더는 홈만 투명 오버레이·나머지 흰색 sticky, 푸터는 전 페이지 단일, 범위는 토스트·러너 어드민 nav·FAQ JS 포함
#   (브레드크럼 fragment화는 후속). 헤더·푸터가 두 벌(header/header-minimal, footer/footer-minimal)이고 어느 벌을 쓸지가 도입 시점으로
#   정해져 굳은 편차가 다음 결함을 만들었다(로컬 8081·운영 헤드리스 실측):
#   A1 고정 header-minimal 밑 브레드크럼 겹침 — 운영 /faq·date-diff, Commit 179 가 guides 12 + use-case 상세로 확산(17페이지)
#   A2 trading·stock 토스트 컨테이너가 fragment 밖 -> 토스트가 한 번도 표시되지 않음(도입 e2e1cc3·910c522 부터)
#   A3 러너 어드민 3페이지 인라인 nav 에 토글 없음 -> <=768px 메뉴·로그아웃 불가
#   A4 스코프 없는 footer{} 요소 선택자가 owner 카드 footer 에 margin 누수
#   A5 .navbar sticky 무효(부모 header 높이 = nav 높이) — 스크롤 시 헤더 사라짐
#   A6 Commit 180·181 이 저작권·문의 줄을 .footer-sections flex 안으로 옮겨 6·7번째 칼럼으로 렌더
# 배포: 179~181 과 191~193 은 한 배포로 나가야 한다 — 179 단독이면 브레드크럼 겹침 17페이지, 181 단독이면 A6 가 노출된다.
# 소유권(처음 등장 커밋이 소유): 크롬 구현 파일은 179·180·181 이 먼저 소유해 흡수했다(각 커밋 '주의(09-27)'). 193 은 가드와 결정 기록만
#   담는다(182 선례). 191·192 는 다른 커밋과 파일을 공유하지 않는다. ADR: datedate/frontend/0004 신설(구조 전환 = 결정 변경).
# 검증: 전 단계 RED 선확인 -> GREEN(SiteChromeConsistencyTest 2 / TradingAnalyticsPageTest +1 / StockDashboardPageTest 1 /
#   RunnerSeoRenderingTest +1). 가드 7클래스 82테스트 GREEN, 전체 스위트 126 클래스 795 테스트 failures 0 (09-27, 병행 세션 미커밋 변경 포함 작업 트리).
#   화면: 떠 있는 앱은 CSS 해시를 부팅 시점에 캐시해(템플릿은 cache false 라 즉시 반영) 하네스(앱이 렌더한 HTML + 작업 트리 static)
#   + Windows Chrome CDP 로 1440/1024/768/375 ko·en 실측 — 브레드크럼이 nav 아래(nav 하단 74 -> 브레드크럼 106px, 모바일 60 -> 92px),
#   1400px 스크롤에도 헤더 top 0, 홈 오버레이 fixed·투명 유지, 모바일 메뉴 280px·18px, 전 폭 가로 넘침 0, FAQ 토글 3페이지 동작.
#   A5 는 수정 전 CSS 하네스로 재현해 확인(1400px 스크롤 시 nav top -1400, computed position sticky) — 헤드리스 --screenshot 의
#   #anchor 캡처는 스크롤이 반영되지 않아 빈 화면이 나오므로 근거로 쓰지 않았다.
#   앱 재시작 후 실제 8081 에서 같은 수치로 재확인했다(새 CSS 해시 서빙, /login 등 서비스 페이지도 단일 푸터,
#   /stock 토스트 컨테이너 fixed·body 직속 1개).
#   owner 카드·러너 어드민·trading 화면은 로그인·데이터가 필요해 미확인(렌더 테스트로만 확인).
# 참고(미수정, 범위 밖): 에러 페이지 nav·푸터 부재 + 홈 링크 @{/} 의 en 누수, 브레드크럼 인라인 16벌·'홈' 메시지 키 6개,
#   짧은 페이지의 푸터 부유, 러너 푸터 (c) 2025 하드코딩, 미사용 footer.usecase.* 키, 800px 대 헤더 2줄(보고서 R8 CSS 후속).

# Commit 191 — ✅ DONE() fix(trading,stock): 토스트 컨테이너를 navbar fragment 안으로 — 토스트 무음 해소
git add src/main/resources/templates/trading/fragments/header.html src/main/resources/templates/stock/fragments/header.html src/test/java/me/singingsandhill/calendar/trading/presentation/controller/TradingAnalyticsPageTest.java src/test/java/me/singingsandhill/calendar/stock/presentation/controller/StockDashboardPageTest.java
git commit -m "fix(trading,stock): 토스트 컨테이너를 navbar fragment 안으로 — 토스트 무음 해소" -m "trading/fragments/header.html 의 #trading-toast-container 와 stock/fragments/header.html 의 #toast-container 가 th:fragment=navbar(nav 요소) 바깥 형제로 놓여 있어, 페이지의 th:replace 가 nav 만 가져오고 컨테이너는 한 번도 렌더되지 않았다. TradingToast.push 와 stock 대시보드 toast() 는 컨테이너가 없으면 조용히 return 하므로 봇 start/stop·리밸런스·verify 결과, 주식 긴급청산 실패 메시지까지 전부 무음이었다(도입 커밋 e2e1cc3·910c522 부터). navbar fragment 를 th:block 으로 감싸 nav + 컨테이너를 함께 포함한다 — 호출부 nav th:replace 는 그대로이고 컨테이너는 position fixed 라 위치 변화가 없다. 주문·봇 제어 코드는 무변경. TDD: TradingAnalyticsPageTest 에 컨테이너 렌더 단언 추가, StockDashboardPageTest 신설(WebMvcTest + 기존 보안 MockitoBean 세트) — 둘 다 컨테이너 부재로 RED 선확인 후 GREEN."

# Commit 192 — ✅ DONE() fix(runner): 어드민 3페이지 공용 admin-navbar — 모바일 메뉴·로그아웃 접근 복구
git add src/main/resources/templates/runners/fragments/header.html src/main/resources/templates/runners/admin/dashboard.html src/main/resources/templates/runners/admin/run-form.html src/main/resources/templates/runners/admin/attendance-form.html src/test/java/me/singingsandhill/calendar/runner/presentation/controller/RunnerSeoRenderingTest.java
git commit -m "fix(runner): 어드민 3페이지 공용 admin-navbar — 모바일 메뉴·로그아웃 접근 복구" -m "admin dashboard·run-form·attendance-form 은 공용 runners navbar 대신 인라인 nav 를 복사했는데 햄버거 토글이 없었다. runners.css 는 768px 이하에서 .runners-nav 를 display none 으로 숨기고 토글로만 열어, 모바일에서 로그아웃·대시보드 링크에 닿을 수 없었다. 로고 문구도 Running Crew 와 97 Runners 로 갈렸다. runners/fragments/header.html 에 admin-navbar fragment(로고 97 Runners, runnersNavToggle/runnersNavMenu, 홈·런 목록·관리자 대시보드·로그아웃 폼)를 추가하고 세 페이지의 인라인 nav 를 교체 — 토글 스크립트는 이미 포함된 runners/fragments/footer 의 것을 재사용한다. 폼 페이지의 '← 대시보드' 는 메뉴 항목 '관리자 대시보드' 가 대신하고 로그아웃이 추가된다. TDD: RunnerSeoRenderingTest 에 /runners/admin·/runners/admin/runs/new 의 토글·메뉴 id·로그아웃 단언 추가, 토글 부재로 RED 선확인 후 GREEN. CRLF 유지."

# Commit 193 — ✅ DONE() refactor(datedate): 사이트 크롬 단일화 가드 + 결정 기록 (ADR datedate/frontend/0004) — 마지막 커밋(git-commit.md 포함)
# 주의: 구현은 흡수됨 — faq-toggle.js·기사 4편·use-case 상세는 179, style.css·메시지·about·기사 8편은 180,
#   header.html·footer.html·date-diff·홈 외 7템플릿·InternalLinkingTest 는 181. 이 커밋은 가드·결정 기록·문서만 담는다.
git add src/test/java/me/singingsandhill/calendar/common/presentation/SiteChromeConsistencyTest.java docs/adr/datedate/frontend/0004-single-site-chrome.md docs/adr/README.md src/main/resources/templates/fragments/CLAUDE.md docs/guides/git-commit.md
git commit -m "refactor(datedate): 사이트 크롬 단일화 가드 + 결정 기록 (ADR datedate/frontend/0004)" -m "헤더·푸터가 두 벌(header/header-minimal, footer/footer-minimal)이고 어느 벌을 쓸지가 페이지 성격이 아니라 도입 시점으로 정해져, nav 변경은 매번 두 번씩 했고 편차가 결함이 됐다: fixed 투명 header-minimal 밑 브레드크럼 겹침(오프셋 보정이 홈 히어로에만 — 179 가 17페이지로 확산), .navbar sticky 무효(부모 header 높이 = nav), 스코프 없는 footer{} 누수, 푸터 칼럼 불균형(4/4/2/5/12 중앙정렬)·영문 대문자 제목·과도한 여백·모바일 1열 27줄. 결정(179~181 에 흡수 구현): header 는 단일 fragment — 홈만 html th:with navOverlay=true 로 투명 fixed, 나머지는 흰색 sticky(sticky 는 header.site-header 에), 링크 클래스 nav-link 하나(다수 22페이지 모습 기준: spring 밑줄·280px 메뉴·1.125rem). footer 는 단일 site-footer 재설계 — 헤더와 같은 1200px 정렬선, 브랜드(워드마크·태그라인·문의) + 서비스·도움말·활용 사례·모임 노하우(12편 2열, 태블릿 3열) + 구분선 아래 정책 줄, sentence case h2, 모바일 2열·링크 행 33px, 기존 토큰만(--text-muted #888 은 AA 미달이라 제외), 활용 사례·기사 전 링크 유지(고아 방지 불변). FAQ 토글 인라인 14벌 -> static/js/faq-toggle.js, /faq 의 hidden 방식도 다수 방식으로. 이 커밋: SiteChromeConsistencyTest(대표 13페이지 — 단일 헤더·푸터, 오버레이 홈 한정, 푸터 구간 안 연락처·정책 각 1회와 전 활용 사례·기사 링크, 옛 클래스 부재; /about 본문 mailto 오탐을 피하려 푸터 구간만 셈) — 구현 전 RED(site-header 0) 선확인 후 GREEN. ADR datedate/frontend/0004 신설(0013 의 '기본 푸터 mailto + footer-minimal 문의 줄' 서술이 단일 푸터 브랜드 블록을 가리키게 됨을 기록), ADR README 두 뷰·집계 95, fragments/CLAUDE.md 표·사용 예시 갱신. 전체 스위트 126 클래스 795 테스트 failures 0, 1440/1024/768/375 ko·en 화면 CDP 실측(브레드크럼 nav 아래·헤더 고정·가로 넘침 0)."

# =====================================================================
# 코인 봇 운영 30일 실적 대조 + 장부 버그 수정 (2026-09-27)
# =====================================================================
# 배경: 사용자 요청 "운영서버의 코인 트레이딩 기록을 바탕으로 현재의 매매 로직 분석" — 대시보드 Recent Trades·Position History 50행·
#   /trading/analytics?days=30 을 받아 09-15 감사 §7 예측과 대조. 사용자 확인: 수수료 0.04% 쿠폰, 리밸런싱은 끈 적 없음,
#   산출물 = 보고서 + 장부 버그 수정(전략 변경은 권고로만).
# 결론: 청산 69건 중 66건이 리밸런싱 기원(진입 간격 481분·mid 진입가), 수수료 전건 0 기록, 보정 손익 약 -1,750원(기록 -412원) vs ADA +20.6%.
#   리밸런싱은 9/20 이후 멈췄지만 꺼진 게 아니라 막힌 상태(코인 잔고 0 -> 계좌 조회 null -> 조용히 스킵 유력) — R0 명시적 비활성화 권고.
# ADR: 없음 — 결정 변경이 아니라 기존 결정(strategy/0004 왕복 수수료 차감, infrastructure/0003 버전 무관 응답)의 전제 복원.
# 검증: 전 단계 RED 선확인 -> GREEN (V1FillTest 6중 5 RED, 연결 테스트 2 RED, 분석 1 RED). 관련 8클래스 GREEN,
#   전체 스위트 127 클래스 804 테스트 failures 0 (09-27, 병행 세션 미커밋 변경 포함 작업 트리). bootRun 미실행(LIVE 봇).
# 배포 후 확인(사용자): 다음 LIVE 체결 fee > 0, /trading/analytics ⑥ 제외 약 66건.

# Commit 194 — ✅ DONE() fix(trading): V1 시장가 응답 체결 정보 정규화(수수료·체결가) + 리밸런싱·수동 매수 Trade-Position 연결
git add src/main/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClient.java src/main/java/me/singingsandhill/calendar/trading/application/service/RebalanceService.java src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java src/main/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsService.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClientV1FillTest.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClientIdempotencyTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/RebalanceServiceAccountingTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceManualTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsServiceTest.java src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md src/main/java/me/singingsandhill/calendar/trading/application/CLAUDE.md
git commit -m "fix(trading): V1 시장가 응답을 체결 정보로 정규화 + 리밸런싱·수동 매수 Trade-Position 연결" -m "운영 30일 기록 대조에서 드러난 장부 버그 3건. ① 수수료 전건 0 기록: 모든 주문 경로가 POST /v1/orders 생성 응답에서 paid_fee 를 읽는데, V1 생성 응답은 접수 시점 스냅샷이라 trades 가 없고 paid_fee 가 0 이다. 체결가 재시도는 GET /v1/order 상세를 받아오면서도 가격만 쓰고 수수료를 버렸다. 그 결과 실현손익 과대, ADR strategy/0004 의 왕복 수수료 차감 전제가 절반만 적용(시간청산·약신호 매도 문턱 +0.25% 로 완화), 서킷브레이커가 실제 적자 청산을 승으로 집계. ② 리밸런싱 진입가 = 호가 mid: buyAndOpenPosition 은 재조회 없이 생성 응답 trades 만 보고 비면 currentPrice(mid)로 폴백 — 운영 47건 중 44건이 x.5원. ①② 를 BithumbApiClient.withFills 한 곳에서 수정: V1 LIVE 시장가 응답에 trades 가 없으면 GET /v1/order 를 최대 3회(300ms 선형 백오프) 재조회해 trades·paid_fee 가 채워진 상세를 반환하고, 못 찾으면 원 응답(기존 동작). v2 어댑터의 정규화와 같은 형태라 application 계층의 모든 경로가 코드 변경 없이 실체결가·수수료·실체결량을 받는다. POST 는 여전히 1회(비멱등 무재시도 불변식 유지), PAPER·V2 경로 무변경. ③ 리밸런싱·수동 매수 Trade 가 positionId 없이 저장돼 분석 ⑥ 이 66건을 인접 신호에 타임스탬프로 붙여 신호 진입으로 오분류(제외 0건). buyAndOpenPosition·manualBuy 를 executeBuy 와 같은 순서(Position 저장 -> setPositionId -> Trade 저장)로 맞추고, 기존 운영 데이터 교정을 위해 분석 ⑥ 은 연결된 BUY 가 없는 포지션을 제외(07-12 이전 미연결 신호 포지션도 빠짐 — 오염 대신 과소집계). 기존 테스트가 운영과 달리 positionId 를 세팅한 픽스처라 놓쳤던 것을 운영 형태 픽스처로 가드. TDD: BithumbApiClientV1FillTest 6(5 RED), 연결 테스트 2(저장 시점 값 캡처, 2 RED), 분석 1 RED -> GREEN. IdempotencyTest 는 wait 응답이 재조회를 타므로 백오프만 0 으로. CLAUDE.md 2곳 사실 갱신."

# Commit 195 — ✅ DONE() docs(audit): 코인 봇 운영 30일 실적 대조 보고서 — 마지막 커밋(git-commit.md 포함)
git add docs/audit/coin-trading-live-record-review-2026-09-27.md docs/guides/git-commit.md
git commit -m "docs(audit): 코인 봇 운영 30일 실적 대조 — 66/69 리밸런싱 기원, 보정 손익 약 -1,750원, 리밸런싱은 막힌 상태" -m "09-15 감사 §7 이 요구한 서버 실적 대조. 자료: 대시보드 Recent Trades·Position History 50행·/trading/analytics?days=30. 기원 판별: 진입 간격 46개 중 24개가 정확히 481분(쿨다운 480+1분 틱), 리밸런싱 진입가 47개 중 44개가 x.5원(mid 폴백), 신호 3건은 정수 KRW(setScale 0) — 청산 69건 중 66건이 리밸런싱 기원이고 F1 손절 루프가 실계좌에서 돌았다(7연속 손절 중에도 매수 지속, 서킷브레이커는 리밸런싱 경로에 없음). 보정 손익: 수수료 0.04%x2 와 리밸런싱 진입 반 틱을 반영하면 기록 -412원 -> 약 -1,750원(자본 대비 약 -6%), 보정 승률 44% vs 손익분기 56%, 같은 기간 ADA +20.6%. 청산 구조: 손절 발동점이 entryFee 0 기록으로 가격 약 -1.25%, TIME_EXIT 100% 승은 착시(+0.17~0.19% 4건은 비용 반영 시 적자)이고 +2~3% 승자 3건은 트레일링 활성 중 360분에 절단, 9/20 시간청산 1분 뒤 같은 가격 재매수 churn. 신호 엔진: BUY 19행/국면 8개/+60분 독립창 0 — 점수 단조성 없음, 추세 요소 조건부 엣지 음수·평균회귀 요소 양수(상쇄), 양수도 실비용 약 0.37% 미만, 임계 30 은 +0.009% — 엣지의 증거 없음. §7 예측 대조: 빈도·기원·손절 적중, fees/gross 는 수수료 미기록으로 검증 불가(장부 버그 발견 -> Commit 194). 리밸런싱은 9/20 16:35 이후 멈췄으나 설정은 enabled — 유력 가설은 ADA 잔고 0 이면 getAccount 가 null 을 주고 checkAndExecute 가 조용히 스킵(신호 매수는 null 을 0 으로 처리해 진행), 서버 조회만으로 확정하는 진단 절차 4단계 수록. 그 null 처리를 고치면 루프가 재가동되므로 R0(명시적 비활성화 + ADR risk/0002 Superseded)를 최우선 권고, R1 taker-fee-rate 0.0004, R2 트레일링 활성 시 시간청산 제외, R3 분석 ⑤ 기원 분리·긴 지평·스프레드, R4 리밸런싱 실체결량."

# =====================================================================
# /tools/date-diff 디자인 통일 — almanac 전용 스타일 제거, 사이트 공통 패턴 회귀 (2026-10-05)
# =====================================================================
# 배경: 사용자 요청 — 날짜 계산기만 /faq·/guides·/insights 와 디자인이 달랐다. 원인은 92400e0 이 ADR 없이 넣은
#   .tool-page--almanac 스코프(style.css 560줄 — 자체 팔레트·0 모서리·하드 섀도·장식 문구).
# 계획: docs/superpowers/plans/2026-10-05-date-diff-design-unification.md (코드 대조 검토 반영본, A안 = 공통 회귀).
# ADR: 없음 — almanac 자체가 ADR 없이 도입됐고, FAQ 전환은 기존 ADR datedate/frontend/0004(FAQ 토글 통일)를 따른다.
#   CLAUDE.md 는 almanac·tool-page 언급이 없어 수정 없음.
# 검증: hidden 버그 RED 를 운영(datedate.site)에서 CDP 로 확인(평일 모드 #monthsCard hidden=true 인데 display flex 로 표시) -> 수정 후 GREEN(display none).
#   전체 스위트 127 클래스 804 테스트 failures 0. bootRun 은 사용자 확인 후 TRADING_BOT_ENABLED=false·STOCK_BOT_ENABLED=false 로 띄워
#   Windows Chrome CDP 로 1280/768/375 × /faq·/guides/how-to-pick-a-date·/tools/date-diff(ko·en) 실측 — 히어로·폭·CTA computed 값이 /faq 와 동일,
#   h2 1종(= guides 기사 h2), FAQ 토글·aria-expanded 동작, 전 폭 가로 넘침 0, 375 에서 입력 세로 스택·화살표 숨김.
#   프리셋 날짜 버그·미주 영업일 버그(사용자 추가 요청): 템플릿의 실제 인라인 스크립트를 node vm 으로 고정 시계·TZ 아래 실행 —
#   프리셋 KST 3건·LA 3건 RED(6 FAIL) -> GREEN, calculateDiff LA 3건 RED -> GREEN(LA·뉴욕 DST 종료·시작 구간, KST 회귀 포함 전부 PASS).
#   수정마다 전체 스위트 804 failures 0 재확인.
# 참고(미수정, 범위 밖): .btn-outline(628·914)·.btn-secondary(905·3822) 이중 정의, use-cases 상세 .use-case-faq-item 무스타일,
#   CTA 2버튼 구성, 브레드크럼 fragment 화.

# Commit 196 — ✅ DONE() fix(datedate): /tools/date-diff 디자인 사이트 공통으로 통일 + 평일 모드 개월 카드·날짜 시간대 버그 — 마지막 커밋(git-commit.md 포함)
# 주의: git-commit.md 는 미실행 Commit 195 의 add 목록에도 있다 — 195 를 먼저 실행하면 이 섹션까지 195 에 흡수되고, 여기의 add 는 변경 없음으로 통과한다.
git add src/main/resources/templates/tools/date-diff.html src/main/resources/static/css/style.css docs/superpowers/plans/2026-10-05-date-diff-design-unification.md docs/guides/git-commit.md
git commit -m "fix(datedate): /tools/date-diff 디자인을 사이트 공통으로 통일 + 평일 모드 개월 카드·날짜 시간대 버그" -m "날짜 계산기만 .tool-page--almanac 스코프(style.css 560줄 — 자체 팔레트 #1c1714/#c43a23, border-radius 0, 4px 하드 섀도, Almanac N°01·INSTRUMENT N°01·END 장식, 밑줄형 입력·자간 대문자 버튼)가 사이트 토큰을 덮어써 /faq·/guides·/insights 와 달랐다. ① almanac 블록 전체 삭제(@keyframes almanacBloom 포함) + 템플릿의 수정자 클래스·eyebrow·괘선 SVG 제거 — 기본 .tool-page/.tool-hero/.tool-cta 는 원래 .faq-page/.faq-hero/.faq-cta 와 같은 값이라 폭·히어로·CTA 가 그대로 맞는다. ② almanac 에 가려져 있던 기본 블록의 미정의 토큰 교정: 체크박스 accent-color·결과 숫자 color var(--primary) -> var(--primary-color), 숫자 크기 var(--text-3xl) -> 2rem, 오류 color var(--danger) -> var(--danger-color). .date-input 은 .form-group input(명시도 0,1,1)에 가려진 border·padding·radius·font-size 선언과 죽은 :focus 규칙을 삭제. ③ 평일 모드 버그: .diff-card { display: flex } 가 UA 의 [hidden] 규칙을 이겨 monthsCard.hidden = true 가 무효였다 — 영업일·영업주 옆에 갱신 안 된 개월 수가 남음(운영 재현). .diff-card[hidden] { display: none } 추가(.onboarding-banner[hidden] 선례). ④ FAQ 를 details 요소(tool-faq-item)에서 사이트 공통 .faq-item + .faq-question + faq-toggle.js 아코디언으로 전환 — guides 기사와 같은 구조, th:each 3개, 메시지 키 불변. ⑤ 본문 섹션(.tool-info·.tool-content)은 값 복사 없이 .guides-article 의 section 여백·h2·p·ul·li·a 규칙에 선택자를 병합해 기사와 같은 독서 밀도로 — 한 페이지의 h2 3종이 1종이 됨. 이로써 무효가 된 기본 .tool-presets h2·.tool-info·.tool-info h2·.tool-use-list 규칙 삭제. .tool-note 는 .insights-info 와 같은 안내 블록 톤(--bg-light, 4px --primary-color 좌측 보더). style.css 의 개행 혼재는 바이트 단위 편집으로 보존(numstat 와 ignore-cr-at-eol numstat 동일 36/598). ⑥ 프리셋·기본 시작일 날짜 버그: setPreset 의 fmt 와 초기화 IIFE 가 toISOString() 으로 로컬 Date 를 UTC 날짜 문자열로 바꿔, KST 에서는 올해 경과일 시작일이 전년 12-31(277일이 278일), 오전 9시 이전 오늘·오늘부터 N일이 전날로, 미국 저녁에는 다음 날로 밀렸다. 로컬 연·월·일로 만드는 toDateInputValue 하나로 세 곳을 교체(calendar.js formatDate 와 같은 형식 — 이 페이지는 calendar.js 를 싣지 않고 초기화가 defer 스크립트보다 먼저 돈다). ⑦ 미주 영업일 버그: calculateDiff 가 입력값을 new Date('YYYY-MM-DD') 로 읽어 UTC 자정이 됐고, getDay·setDate·toLocaleDateString 은 로컬 기준이라 UTC- 지역에서 요일과 요약 날짜가 하루 앞으로 밀렸다(LA 월~금 4 영업일이 3, 토~월 0 이 1, 요약 10/4~10/8). parseDateInputValue 로 로컬 자정 Date 를 만들어 해결 — 서머타임 경계의 23/25시간 날은 기존 Math.round 일수 계산이 흡수한다." -m "검증: 운영 RED(CDP — 평일 모드 #monthsCard hidden=true·display flex·표시) -> 로컬 GREEN(display none). 전체 스위트 127 클래스 804 테스트 failures 0. bootRun(사용자 확인, 두 봇 비활성 오버라이드) + Windows Chrome CDP 1280/768/375 ko·en 실측: 히어로·폭·CTA computed 값 = /faq, h2 1종 = guides 기사 h2, FAQ 토글·aria-expanded 동작, 가로 넘침 0. 프리셋: 템플릿 인라인 스크립트를 node vm 고정 시계로 실행해 KST(10:30 올해 경과일, 00:30 기본 시작일·100일)·LA(20:00 기본 시작일·올해 경과일·1년) 6건 RED -> GREEN, calculateDiff 는 LA 3건 RED -> GREEN(LA·뉴욕 DST 경계·KST 회귀 포함). ADR 없음 — 공통 패턴 회귀이며 FAQ 는 ADR datedate/frontend/0004 의 기존 결정을 따른다."

# =====================================================================
# 참여자 한도 8 -> 10명 + 참여자 색 10색 (2026-10-10) + 일정 설명 필드 (2026-10-11)
# =====================================================================
# 배경: 사용자 결정 — 일정당 최대 인원 8 -> 10, 색은 10색으로 확장. 분석에서 8 의 복제(웹 버튼 th:disabled >= 8, 카운터 '{0} / 8명',
#   미니앱 canAdd < 8, 문구 ko 16키·en 15키)와 8색 순환(9·10번째 = 1·2번째 색)을 확인 — 어떤 테스트도 잡지 못하던 것.
# 계획: docs/superpowers/plans/2026-10-07-participant-cap-10.md
# ADR: datedate/domain/0010(한도 10 + ScheduleDetailResponse.maxParticipants), datedate/ux/0004(10색 — ux/0001 의 8색 spec 대체).
#   README 두 뷰·폴더 수, datedate/application/CLAUDE.md, docs/architecture.md, docs/datedate/erd.html 갱신.
# 검증: 전체 스위트 129 클래스 819 테스트 failures 0 errors 0 (+2 클래스 +15 테스트). Task 마다 RED 확인 후 GREEN —
#   한도만 10 으로 올리면 '한도까지 색 고유' 테스트가 실패하는 것도 확인. 320px 하네스(Windows Chrome 헤드리스 CDP) 실측: 10명 가능한 날 점
#   3줄·셀 밖 13.4px -> 2줄·2.4px (300·320px), 321·344·360·390px 불변. 앱 기동 검증: 안 함 — MockMvc 렌더링·하네스로만 (bootRun 은 LIVE 봇).
# 최종 검토(새 컨텍스트 검토자 1명): Critical·Important 0. 경미 4건 모두 반영 — ERD 다중성 라벨 0..8 -> 0..10, 하드 한도를 '권장'이라 부르던 문구,
#   색 구분을 한도의 이유로 단정하던 문구·en travel a1 의 'anyway', ADR ux/0004 의 '색 안 겹침'에 삭제 시 예외 명시.
# 기존 오류 문구 2건도 반영(사용자 요청): club-activity FAQ a2 '다음 달로 회원 명단 자동 복제'(그런 코드 없음 — 새 달은 빈 일정) ->
#   월 페이지는 7주 달력이라 매주 활동은 같은 페이지, 다음 달은 새 일정에 이름 다시 추가. 홈 한계 안내 '오전/오후 시간대 구분 없음' ->
#   날짜 단위 선택 + 30분 단위 시간 후보 투표(자정 넘김 불가, ADR datedate/domain/0009). 같은 이유로 use-case 팀 회의 FAQ a1
#   '시간 슬롯 투표 미지원, 메뉴 투표로 우회' -> 시간 투표 안내, club-activity FAQ a3 '뜸한 회원과 교체' -> 화면에서 이름 삭제 불가·다음 달 새 명단.
# 참고(미수정, 범위 밖): 동시 추가 시 한도 초과 가능(락 없는 메모리 검사, 기존 동작), 모바일 두 자리 배지·툴팁 폭,
#   api.js 의 다른 오류 문구 한국어 고정,
#   미니앱(datedate-app) 변경(canAdd·설명 표시)은 별도 레포의 작업 트리에만 — 그 레포의 스테이징된 작업 위라 커밋하지 않음.
# --- 일정 설명 필드 (2026-10-11, 같은 커밋) ---
# 배경: use-case 문구 ko 13곳·en 12곳이 없는 '페이지 설명(description)' 을 안내 — 사용자가 문구 수정 대신 필드 신설을 택함.
# 설계: docs/superpowers/specs/2026-10-11-schedule-description-design.md (질문 4개 + A안 — 링크 판별은 서버 한 곳). ADR: datedate/domain/0011.
#   루트 CLAUDE.md DateDate 목록, datedate/application/CLAUDE.md, ADR README, ERD 그림 1 schedules 표 description 행·그림 2 '+ 5 more columns'.
# 검증: 전체 스위트 132 클래스 853 테스트 failures 0 errors 0 (설명 기능 +3 클래스 +34 테스트). 기능마다 RED 후 GREEN —
#   렌더링 테스트는 첫 RED 가 테스트 owner ID 길이(22자) 때문이라 ID 를 고친 뒤 view.html 설명 섹션을 빼는 변이 검사로 올바른 실패를 확인하고 원복.
#   웹 JS 편집 흐름은 하네스(실제 description.js·main.js·style.css + 가짜 api) + 헤드리스 Chrome 단언 8/8, 390px 화면 확인.
#   미니앱: vitest 44/44, tsc 0. ERD 는 렌더 스크린샷으로 겹침 없음 확인.
# 같이 고친 것: 개인정보 처리방침 수집 항목에 일정 설명·시간 투표 후보(시간 투표 때 누락), 최종 수정일 2026-10-11. ERD 그림 1 desc 의 'up to eight participants' -> ten.

# Commit 197 — ✅ DONE() feat(datedate): 참여자 한도 10명 + 참여자 색 10색 + 일정 설명 필드 (ADR datedate/domain/0010·0011, ux/0004) — 마지막 커밋(git-commit.md 포함)
# 주의: 두 기능이 한 커밋인 이유 — 한도 변경(이 커밋)을 실행하기 전에 일정 설명을 구현해 Schedule.java·ScheduleDetailResponse·view.html·메시지 등
#   공유 파일 10개가 겹쳤다. 처음 등장하는 커밋이 소유하므로 따로 나누면 이 커밋이 설명 코드를 일부만 담아 단독 빌드가 깨진다 -> 합본.
# 주의: style.css 의 320px 점 크기 변경은 Commit 196(✅ 14e7b65)에 흡수돼 이미 커밋됐다. 여기의 style.css 는 일정 설명 카드 블록이다.
# 주의: git-commit.md 는 미실행 Commit 189·190·195 의 add 목록에도 있다 — 먼저 실행되는 커밋에 흡수되고, 여기의 add 는 변경 없음으로 통과한다.
# 주의: messages*.properties 의 개행만 다른 436줄(가이드 블록)은 Windows git(autocrlf=input)이 add 시 LF 로 되돌린다 — 내용 변경 아님.
git add src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/Schedule.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColor.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/ScheduleDetailResponse.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/ParticipantLimitExceededException.java src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md src/main/resources/templates/schedule/view.html src/main/resources/templates/faq.html src/main/resources/templates/guide.html src/main/resources/templates/index.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/js/api.js src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ParticipantCapCopyTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColorTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleParticipantCapRenderingTest.java src/test/java/me/singingsandhill/calendar/common/presentation/api/GlobalExceptionHandlerI18nTest.java docs/adr/datedate/domain/0010-participant-cap-10.md docs/adr/datedate/ux/0004-ten-color-participant-palette.md docs/adr/datedate/ux/0001-participant-color-cell-sync.md docs/adr/README.md docs/architecture.md docs/datedate/erd.html docs/superpowers/plans/2026-10-07-participant-cap-10.md src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/ScheduleJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ScheduleRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/OwnerRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/application/service/ScheduleService.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/ScheduleApiController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/request/ScheduleUpdateRequest.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/TextSegment.java src/main/java/me/singingsandhill/calendar/datedate/presentation/support/DescriptionLinks.java src/main/resources/templates/privacy.html src/main/resources/static/js/schedule/description.js src/main/resources/static/js/schedule/main.js src/main/resources/static/css/style.css src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/ScheduleServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/ScheduleApiControllerTest.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ScheduleDescriptionPersistenceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/support/DescriptionLinksTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleDescriptionRenderingTest.java docs/adr/datedate/domain/0011-schedule-description.md CLAUDE.md docs/superpowers/specs/2026-10-11-schedule-description-design.md docs/guides/git-commit.md
git commit -m "feat(datedate): 참여자 한도 10명 + 참여자 색 10색 + 일정 설명 필드 (ADR datedate/domain/0010·0011, ux/0004)" -m "① 한도: Schedule.MAX_PARTICIPANTS 8 -> 10. 검사 위치(애그리거트)는 ADR datedate/domain/0001 그대로. ScheduleTest 에 한도 고정, 한도까지 색 고유(doesNotHaveDuplicates), 배포 전 8명 일정의 9·10번째 색 테스트." -m "② 색: ParticipantColor 팔레트 끝에 #92400E(amber-800)·#0E7490(cyan-700) 덧붙임 — index 0~7 불변, 저장된 색은 참여자별 hex 라 마이그레이션 없음. 선정 근거: 흰 배경 대비 7.09·5.36:1, 기존 8색과 최소 CIEDE2000 18.0(일반)·11.9(deutan)·14.1(protan) — 기존 최근접 쌍 indigo~violet 6.5 보다 멂. ux/0001 의 8색 spec 결정을 ux/0004 로 대체." -m "③ 한도 전달: ScheduleDetailResponse.maxParticipants 추가(JSON 필드 추가, 하위 호환) — 웹 뷰와 일정 상세 API 가 같은 DTO. view.html 의 추가 버튼 th:disabled 와 카운터('{0} / {1}명')가 이 값을 쓴다. 전에는 >= 8 과 '{0} / 8명' 하드코딩이라 상수만 바꾸면 웹 버튼이 8명에서 잠겼다." -m "④ 409 메시지: ParticipantLimitExceededException 에 errors.participantLimitExceeded + 한도 인자(ko/en). api.js 의 MAX_PARTICIPANTS_EXCEEDED 매핑은 서버 코드(PARTICIPANT_LIMIT_EXCEEDED)와 달라 한 번도 쓰이지 않던 줄이라 삭제 — 이제 한국어 사용자도 영문 원문 대신 현지화 문장을 본다." -m "⑤ 문구: 한도를 숫자로 말하는 ko 16키·en 15키를 10 으로(홈·가이드·FAQ·소개·use-case FAQ). 사실이 틀렸던 문장(9명 이상이면 기능적으로는 작동 등)은 다시 쓰고, 하드 한도를 권장이라 부르던 문구(홈 한계 안내, club-activity FAQ)도 최대 10명으로. ParticipantCapCopyTest 가 이 키들이 상수를 말하는지, 권장·recommend 로 부르지 않는지 대조. 덤으로 사실과 달랐던 기존 문구 4건(club-activity FAQ a2 명단 자동 복제·a3 회원 교체, 홈 시간대 구분 없음, 팀 회의 FAQ a1 시간 슬롯 투표 미지원)도 실제 동작대로. 템플릿 정적 대체 텍스트 5곳도 맞춤." -m "⑥ 320px: 10명 가능한 날의 점이 320px 이하에서만 3줄·셀 밖 13.4px -> 그 블록만 점 5px·간격 1px 로 2줄(실측 2.4px). 321px 이상은 변화 없음." -m "⑦ 일정 설명: Schedule.description(최대 500자, strip, 빈 값 = 지우기) + null 허용 컬럼(ddl-auto 추가). 기존 PATCH /api/owners/{id}/schedules/{y}/{m} 에 description(null = 그대로, 빈 문자열 = 지우기), 바뀐 것이 있을 때만 저장. 링크 공개 편집 — 기존 PATCH 와 같은 권한. 링크는 서버 DescriptionLinks 가 http(s) + RFC 3986 ASCII 구간만 조각으로 나누고(끝 문장부호·짝 없는 괄호 제외, 다른 스킴 평문) ScheduleDetailResponse.descriptionSegments 로 내린다. view.html 은 th:text·th:href 로만 그리고(utext 없음) 링크는 새 탭 + rel nofollow ugc noopener noreferrer. 편집은 description.js(편집·취소·저장·글자 수), 저장 후 새로 고침. 미니앱은 평문 읽기 전용(앱인토스 외부 링크 정책). use-case 문구의 description 을 일정 설명으로, 스터디 FAQ 의 없는 일정 제목을 설명 첫 줄로, 개인정보 처리방침 수집 항목 갱신." -m "검증: 전체 스위트 132 클래스 853 테스트 failures 0 errors 0. 설명 편집 흐름 하네스 단언 8/8."

# =====================================================================
# AdSense 4번째 통지(2026-10-12) — ① 확정 결함 수정 (2026-10-13)
# =====================================================================
# 배경: AdSense "가치가 별로 없는 콘텐츠" 4번째 통지(전체 5번째 거절, 10-04 배포 당일 재검토 요청) 진단. 라이브 실측으로 확정한 결함 3개 —
#   ① use-cases/detail.html 의 6개 섹션(소개·예시·사용 시점·실수·팁·FAQ)이 같은 요소에 th:with + th:if 를 둬서 한 번도 렌더되지 않았다.
#      Thymeleaf 는 th:if(우선순위 3)를 th:with(4)보다 먼저 평가 -> 조건 시점에 변수 null. fe46757(2026-05-16)이 무조건 렌더되던 .body 블록을
#      이 섹션들로 교체해, 05-02 거절 뒤의 "보강" 이 실제로는 본문을 없앴다(라이브 use-case = 3문장 + CTA + 다른 사례 카드, en 168단어).
#      08-17 진단의 "use-case 885~962단어" 는 렌더 결과가 아니었다. 임시 프로브로 메커니즘 증명(같은 요소 -> 빈 출력, 중첩 -> 정상) 후 삭제.
#   ② 같은 10 URL 이 FAQPage(질문·답 5쌍)·HowTo(3단계) JSON-LD 를 내보내면서 화면에는 없었다 + guide en 단계명 1건 — 131 항목.
#      Google 구조화 데이터 정책 위반("Don't mark up content that is not visible"). GSC 직접 조치는 없음(사용자 확인).
#   ③ 홈·인사이트 인기 목록이 표수(표시)와 표수+최근 보너스 0~5(정렬)로 기준이 달라 마라탕 4표 > 양식 2표 > 곱창 7표 로 보였다.
# 설계: 대화 안의 bounded 설계를 사용자가 승인(2026-10-13, 3개 결함 + 197 선 staging). 이후 재노출 문구 점검에서 없는 기능(다음 달 명단
#   자동 복제·이월) 서술 6키 x 2로케일을 발견해 같은 커밋에서 고쳤다 — 그중 FAQ a1·a4·a5 는 FAQPage JSON-LD 로 이미 Google 에 노출돼 있었다.
# 검증: RED 선확인 -> GREEN. 전체 스위트 134 클래스 867 테스트 failures 0 errors 0 (197 기준 132/853 에서 +2 클래스 +14 테스트).
#   하네스: MockMvc 렌더 HTML + 실제 style.css·faq-toggle.js 를 Windows Chrome 헤드리스(CDP)로 375·1280px — 7개 블록·진행 순서 3단계 렌더,
#   FAQ 5개 클릭 시 열림(.open), 가로 넘침 없음(scrollWidth = clientWidth). 앱 기동 검증은 안 함(bootRun 은 LIVE 봇).
# 범위 밖(보고만): seo.useCase.<slug>.body 레거시 키 5 x 2 는 fe46757 이후 미사용 — 삭제하지 않음. 미커밋 docs/stock/trade-decision.html(Commit 190)·
#   package-lock.json 은 이 작업과 무관.

# Commit 198 — ✅ DONE() fix(datedate): use-case 본문 섹션 렌더 복구 + 구조화 데이터-화면 정합 가드 + 인기 순위 표수순 (ADR datedate/domain/0012) — 마지막 커밋(git-commit.md 포함)
# 주의: Commit 197 은 사용자가 이미 staging 했다 — 197 은 git commit 만 실행할 것. 197 의 git add 를 다시 실행하면 이 커밋의 변경
#   (messages*.properties·CLAUDE.md 2곳·ADR README·git-commit.md)이 197 에 흡수된다.
# 주의: 세 결함이 한 커밋인 이유 — messages*.properties 와 datedate/application/CLAUDE.md 를 셋이 함께 고친다. 처음 등장하는 커밋이 소유하므로
#   나누면 인사이트 문구(표수순 설명)가 정렬 코드보다 먼저 커밋되는 어긋난 중간 상태가 된다 -> 합본.
# 주의: datedate/application/CLAUDE.md 는 작업 트리가 CRLF(index LF, attr eol=lf) — add 시 LF 로 정규화된다. 실변경은 +4/-2.
git add src/main/resources/templates/use-cases/detail.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/java/me/singingsandhill/calendar/datedate/application/service/PopularityService.java src/test/java/me/singingsandhill/calendar/datedate/application/service/PopularityServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/UseCaseContentSectionsRenderingTest.java src/test/java/me/singingsandhill/calendar/common/presentation/StructuredDataVisibilityTest.java docs/adr/datedate/domain/0012-popularity-order-votes-first.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/guides/git-commit.md
git commit -m "fix(datedate): use-case 본문 섹션 렌더 복구 + 구조화 데이터-화면 정합 가드 + 인기 순위 표수순 (ADR datedate/domain/0012)" -m "① use-case 본문 렌더 복구: use-cases/detail.html 의 6개 섹션(소개·예시·사용 시점·실수·팁·FAQ)이 한 요소에 th:with 와 th:if 를 같이 둬 한 번도 렌더되지 않았다 — Thymeleaf 는 th:if(우선순위 3)를 th:with(4)보다 먼저 평가해 조건 시점에 변수가 null. fe46757(2026-05-16)이 무조건 렌더되던 .body 블록을 이 섹션들로 교체한 뒤 라이브 use-case 5페이지(사이트맵 10 URL)는 3문장+CTA+다른 사례 카드(en 168단어)만 보여줬고, 기존 UseCaseLocaleRenderingTest 는 제목 단어만 확인해 통과했다. 수정: 가드 변수를 바깥 th:block 으로 옮김. 스타일은 새 CSS 없이 .guides-article 프로즈 규칙 공유(날짜 계산기와 같은 방식), FAQ 는 details 대신 사이트 공용 faq-list 아코디언 + faq-toggle.js. 재노출 전 문구 점검: 다음 달로 명단이 자동 복제·이월된다는 없는 기능 서술 6키 x 2로케일(team-meeting 예시, friend-meetup 사용 시점2·FAQ a5, travel-planning FAQ a5, study-group FAQ a1·a4)을 실제 동작(ScheduleService.createSchedule 은 빈 일정 — 새 달은 이름을 다시 추가)으로 고쳤다. FAQ a1·a4·a5 는 FAQPage JSON-LD 로 Google 에 이미 노출돼 있던 문구." -m "② 구조화 데이터-화면 정합: 같은 10 URL 이 FAQPage(질문·답 5쌍)·HowTo(3단계)를 내보내면서 화면에는 없었고, guide en 의 HowTo 단계명(Review & Decide)이 화면(Review Results & Decide)과 달랐다 — 131 항목, Google 정책 'Don't mark up content that is not visible to readers'. HowTo 는 step1~3 을 화면 '진행 순서' 섹션으로 그리고(usecase.steps.title ko/en 신설), en seo.guide.step5.name 을 화면 문구로 맞췄다. 가드 StructuredDataVisibilityTest: generateSitemapXml() 의 loc 전체(ko·en 54 URL)를 MockMvc 로 렌더해 JSON-LD 의 FAQ 질문·답과 HowTo 단계 이름이 화면 텍스트에 있는지 본다. 처음엔 getSitemapEntries() 를 돌려 ko 27 URL 만 검사했다 — en 은 XML 생성 때 덧붙는다 — loc 기준으로 바꿔 en 까지 RED(11 URL 131 항목)를 확인." -m "③ 인기 순위 표수순 (ADR datedate/domain/0012): 홈·인사이트가 표수를 보여주면서 표수+최근 보너스(등록 후 30일 동안 0~5) 점수로 정렬해 마라탕 4표 > 양식 2표 > 곱창 7표 로 보였다. PopularityService 두 파이프라인을 BY_VOTES_THEN_RECENCY(표수 내림차순, 동점은 latestCreatedAt 내림차순)로 바꾸고 점수 함수·MAX_DAYS·TIME_WEIGHT_MAX·ChronoUnit import 를 지웠다(파일 CRLF 유지, +8/-18). 노출 기준(ADR 0006 — 최소 2표·블록리스트)은 그대로. /insights/trends 의 시간 가중치 설명(insights.description·trend.title·trend.desc)과 seo.insights.description 을 ko/en 으로 고쳤다." -m "검증: RED 선확인 — UseCaseContentSectionsRenderingTest 11건 실패(섹션 문구 없음), StructuredDataVisibilityTest 실패(11 URL 131 항목), PopularityServiceTest.ordersByVotesBeforeRecency 실패(실제 [마라탕, 곱창]). breaksVoteTiesByRecency 는 동점 처리 보존 가드(입력 순서 두 가지)라 처음부터 GREEN. GREEN 후 전체 스위트 134 클래스 867 테스트 failures 0 errors 0. 하네스(MockMvc 렌더 HTML + 실제 CSS·JS, Windows Chrome 헤드리스 CDP) 375·1280px: 7개 블록·진행 순서 3단계, FAQ 클릭 시 열림, 가로 넘침 없음. 앱 기동 검증은 안 함(bootRun 은 LIVE 봇). 문서: ADR datedate/domain/0012 신설 + README 두 뷰·폴더 수(98 -> 99), 루트 CLAUDE.md·datedate/application/CLAUDE.md 의 PopularityService 서술과 use-case 구조화 데이터 가드. 범위 밖(보고만): seo.useCase.<slug>.body 레거시 키 5 x 2 는 fe46757 이후 미사용 — 삭제하지 않음."

# =====================================================================
# AdSense ② 도구 중심 콘텐츠 — 신뢰 정보 · /guide 6단계 · 모임 데이터 리포트 (2026-10-14~15)
# =====================================================================
# 배경: AdSense "가치가 별로 없는 콘텐츠" 4번째 통지 진단(①=Commit 198)의 후속. 사이트의 가치·사용자 관심은 도구(공유 링크 일정
#   페이지)에 있는데 심사 표면은 일반론 콘텐츠였다(GA4 90일: 콘텐츠 페이지 각 97회 미만, 검색어 상위 10개 전부 도구 검색).
#   방향 B(승인 후 도구 페이지에도 광고)는 심사와 무관하므로 ②는 "도구의 가치와 실제 사용을 보이게" 하는 작업이다.
# 설계: docs/superpowers/specs/2026-10-14-adsense-tool-content-design.md (질문 6개 — 라이브 리포트, 지표 3종, /guide 보강, GitHub 핸들,
#   이메일 유지, 게시일 정정 + "광고로 비용 충당 문구 모두 제거") / 계획: docs/superpowers/plans/2026-10-14-adsense-tool-content.md
#   (12 태스크, 사용자가 Native 실행 선택). 실행 판정(ruling) 기록은 계획 작업 공간 원장에 남겼다(아래 요약).
# 검증: 태스크마다 RED 선확인 → GREEN. 전체 스위트 140 클래스 904 테스트 failures 0 errors 0 (198 기준 134/867 → +6 클래스 +37 테스트).
#   하네스(MockMvc 렌더 HTML + 실제 CSS·JS, Windows Chrome 헤드리스 CDP): /guide 375·1280px 이미지 6/6 로드·가로 넘침 0,
#   /insights 리포트 375·1280px 막대 15개·가로 넘침 0. 앱 기동 검증은 안 함(bootRun 은 LIVE 봇).
# /guide 스크린샷 재촬영 절차(UI 가 바뀌면): ① 임시 테스트가 테스트 DB 에 예시 일정 2개(ko college-friends: 지민·서준·하은·도윤·수아,
#   en team-dinner: Alex·Jamie·Sam·Taylor·Jordan — 2026-11, 고른 날 6·7·13·14·20·21·27·28, 14일 18:00–20:00/12:00–14:00 시간 후보,
#   장소·메뉴 각 2개 투표)를 넣고 / , /<owner>/2026/12(생성 화면), /<owner>/2026/11 을 ko·en 으로 MockMvc 덤프 → ② HTML 의
#   `-<32hex>.(css|js)` 해시 제거 + static/{css,js,image} 심볼릭 링크 + python http.server → ③ Windows Chrome 헤드리스
#   (--remote-debugging-port, 전용 user-data-dir) + Windows node CDP: 390×844 모바일, deviceScaleFactor 2, 달력 셀이 렌더될 때까지 대기,
#   요소 영역(#start-form / #main-content / .schedule-header / .participant-section∪.calendar-container / .time-section /
#   #locationList 의 .location-section)을 x=0·폭 390 고정으로 잘라 WebP quality 80 → static/image/guide/{ko,en}/step{1..6}.webp
#   → ④ 높이(px)를 messages 의 guide.step{N}.imgHeight 에 반영. 임시 테스트·덤프는 지운다.
# 실행 판정 요약: 커밋 3개(199/200/201) 계획 → 1개로 합본(messages·style.css·CLAUDE.md·ADR README 를 세 파트가 함께 고쳐 처음 등장 커밋
#   소유 규칙상 중간 상태가 어긋남 — 197 전례). 메타 설명 120~160자 가드에 맞춰 en guide·insights 설명 문구 조정. 스크린샷 폭 고정·
#   4단계 영역 합집합·렌더 대기 폴링. 테스트 결함 2건 수정(멀티라인 img 의 src 추출, TIMESTAMP µs 정밀도 비교). 화면 점검으로 리포트
#   "진행 중" 줄바꿈과 개수 천 단위 구분(18240 → 18,240) 수정.
# 최종 리뷰(새 리뷰어 1명, Critical 0 / Important 2 / Minor 10) 반영: 조회 테스트가 1차 캐시만 읽던 결함(flush·clear), 날짜 계산기의
#   '5단계 가이드' 문구, BreadcrumbList 페이지 이름, en 'Median 1 days', 월별 '최근 12개월' 명시, 6단계 문단을 사진(장소 투표)에 맞춤,
#   두 모드 혼합 요일 테스트, Clock 빈 주입, ADR 0011·0012 포인터와 0013 시간대 한계. 리뷰가 짚은 기존 문제(개인정보처리방침에
#   카카오 로그인 항목 없음)도 사용자 요청으로 함께 고침 — 삭제는 문의 요청(사용자 결정, 셀프 탈퇴 기능은 ADR common/security/0004 후속 과제).
# 범위 밖(보고만): RecapService 의 요일 집계가 기존 모드(weeks≠7) 일정도 indexToDate(확장 규칙)로 바꿔 요일이 어긋남 —
#   YearMonth.dateOf 로 고칠 수 있으나 별도 작업. 기사·use-case 페이지 스크린샷·데이터 보강, 캐시, 연락처 이메일 변경은 하지 않음.
#   미커밋 package-lock.json 은 이 작업과 무관.

# Commit 199 — feat(datedate): AdSense ② 도구 중심 콘텐츠 — About 운영자·광고 비용 문구 제거·게시일 정정 + /guide 6단계 실제 화면 + 모임 데이터 리포트 (ADR common/seo/0015, datedate/domain/0013) — 마지막 커밋(git-commit.md 포함)
# 주의: 세 파트(신뢰 정보·/guide·리포트)가 한 커밋인 이유 — messages*.properties·style.css·CLAUDE.md 2곳·ADR README 를 셋이 함께 고친다.
# 주의: datedate/application/CLAUDE.md 의 작업 트리 CRLF 노이즈는 add 시 LF 로 정규화된다(내용 변경 아님).
#   guides/*.html 8개의 개행만 다른 변경은 이 커밋에 넣지 않는다(내용 차이 0 — git diff --ignore-cr-at-eol 기준).
git add src/main/resources/templates/about.html src/main/resources/templates/guide.html src/main/resources/templates/insights/trends.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css src/main/resources/static/image/guide src/main/java/me/singingsandhill/calendar/datedate/domain/guide/GuideSlugs.java src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/YearMonth.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantAvailability.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantRepository.java src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/ParticipantJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/ScheduleJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ParticipantRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ScheduleRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/application/dto/MeetupReportDto.java src/main/java/me/singingsandhill/calendar/datedate/application/service/MeetupReportCalculator.java src/main/java/me/singingsandhill/calendar/datedate/application/service/InsightsService.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/InsightsController.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AboutPageRenderingTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidesLocaleRenderingTest.java src/test/java/me/singingsandhill/calendar/common/application/service/SitemapServiceHreflangTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidePageRenderingTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/YearMonthTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantAvailabilityTest.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/MeetupReportQueryTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/MeetupReportCalculatorTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/InsightsMeetupReportRenderingTest.java docs/adr/common/seo/0015-operator-github-link-on-about.md docs/adr/common/seo/0013-publisher-identity-contact-and-sameas.md docs/adr/datedate/domain/0013-meetup-data-report.md docs/adr/datedate/domain/0012-popularity-order-votes-first.md docs/adr/common/seo/0011-guides-editorial-hub.md docs/adr/common/security/0004-kakao-oauth2-login.md src/main/resources/templates/privacy.html src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/PolicyPagesLocaleRenderingTest.java docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/superpowers/specs/2026-10-14-adsense-tool-content-design.md docs/superpowers/plans/2026-10-14-adsense-tool-content.md docs/guides/git-commit.md
git commit -m "feat(datedate): AdSense ② 도구 중심 콘텐츠 — About 운영자·광고 비용 문구 제거·게시일 정정 + /guide 6단계 실제 화면 + 모임 데이터 리포트 (ADR common/seo/0015, datedate/domain/0013)" -m "② -1 신뢰 정보: About 에 '만든 사람' 섹션(1인 개발자 개인 프로젝트, 2025-12 공개, 시간 투표·일정 설명 추가 — 검증 가능한 사실만) + 운영자 GitHub(singingsandhill) 링크를 화면에 노출(rel=me, ADR common/seo/0015 — 0013 결정 3 의 UI 비노출 대체, 실명·founder·Person 은 두지 않음). 사용자 결정으로 '광고로 운영비 충당' 문구를 모두 제거(About 원칙, About SEO 설명의 '광고 운영 기준', Terms 7조 '서비스 운영을 위해'). 카카오 로그인 시 저장 항목(회원번호·닉네임·프로필 이미지 주소)을 원칙 문장에 명시. About·Terms 갱신일 2026-10-14. 개인정보처리방침에 카카오 로그인 처리 내용을 추가(About 과 일치 — 리뷰 지적): 저장 항목(회원번호·닉네임·프로필 이미지 주소·가입/최근 로그인 시각, scope profile_nickname·profile_image 라 이메일·전화번호 없음), 로그인 중 활동 기록(일정 생성·참여 이름·장소/메뉴 투표 이름·시각)과 연결한 페이지 ID, 리캡 공유 링크로 공개되는 범위(닉네임·리캡 수치·투표 장소/메뉴·자주 함께한 사람 이름, noindex), 목적(마이페이지·연간 리캡), 로그인 세션 쿠키, 보관·삭제(삭제 기능이 없어 이메일·의견 양식 요청 시 운영자가 삭제, 페이지는 연결만 해제 — 사용자 결정; 카카오 쪽 연결 끊기로는 지워지지 않음), 문의 절에 이메일. 갱신일 2026-10-15. ADR common/security/0004 의 '탈퇴는 후속 과제' 에 임시 절차 메모. 기사 12편 게시일을 실제 첫 공개일로 정정(4편 08-23 -> 09-19, 8편 09-24 -> 10-04 — GuideSlugs 만 바꾸면 바이라인·Article JSON-LD·사이트맵 lastmod 가 따라감), 바이라인 저자 'DateDate 팀' -> 'DateDate'." -m "② -2 /guide: 5단계 -> 6단계(5단계 '시간 정하기' 신설 — 가능하다고 고른 날에 30분 단위 후보·투표·겹침 차트, ADR datedate/domain/0009 기준. 기존 5단계는 6단계로). 2단계의 '자동으로 생성' 을 실제 흐름('이 일정 만들기' 버튼, ADR 0003)으로 고치고 일정 설명(최대 500자) 안내 추가, 옮기며 '겨치는' 오타 수정. 6단계 문단은 사진(장소 투표)에 맞춰 투표 결과를 앞세움. 날짜 계산기의 /guide 소개도 '6단계 가이드'. HowTo JSON-LD 6단계(SeoService.getGuideSeo) — 단계 이름은 화면과 같은 문구(StructuredDataVisibilityTest). 단계마다 실제 화면 스크린샷(ko·en 각 6장, WebP 780px, 9.9~38.8KB, 'width/height/alt' + '예시 화면 — 이름과 날짜는 예시입니다' 캡션, 1단계 외 lazy) — 재촬영 절차는 이 섹션 주석." -m "② -3 모임 데이터 리포트(ADR datedate/domain/0013): /insights/trends 에 요일별 가능 비율·월별 일정 수(최근 12개월, 이번 달 진행 중)·미리 잡는 기간(최다 겹침일 − 생성일, 동점은 이른 날, 음수 제외, 0~6/7~13/14~27/28+ 구간 + 아래쪽 중앙값) 세 블록을 렌더 시점에 집계. 표본 하한(요일 300 날짜 / 월별 30 일정 / 리드타임 30 일정) 미달 블록은 숨김, 비율은 최대 잉여 반올림으로 합계 100, 공개는 합계뿐. 읽기는 참여자+일정 JOIN FETCH 1회(ParticipantRepository.findAllAvailability -> ParticipantAvailability record)와 생성 시각 1회(ScheduleRepository.findAllCreatedAt) — 일정마다 지연 로딩하면 2,171+ 쿼리. 날짜 변환은 YearMonth.dateOf(index, weeks) — 7주 확장 모드는 그리드 시작 + (index − 1), 기존 모드는 그 달의 index 일(웹 달력 두 렌더 경로와 같은 규칙). 계산은 스프링 의존 없는 MeetupReportCalculator. 제목·H1·브레드크럼(화면·BreadcrumbList) '모임 데이터 & 인기 트렌드'(ko 26자 / en 39자, SERP 예산 안 — ADR 0012 가 지켰던 옛 제목을 바꾸는 근거는 0013), 설명 120~160자. 기준일은 Asia/Seoul Clock 빈. 월별 설명은 '최근 12개월 · 누적 N개', en 중앙값은 단수 처리(1 day). 알려진 한계: createdAt 은 호스트 시간대로 기록돼 호스트가 UTC 면 KST 00~09시 생성분의 리드타임이 하루 길다 — 보정 불가, ADR 0013 에 기록. 막대는 aria-hidden, 라벨·값은 텍스트." -m "검증: RED 선확인 — AboutPageRenderingTest 8건 전부 실패, 바이라인·lastmod 2건, GuidePageRenderingTest 6단계 2건·스크린샷 2건, HowTo 6단계 1건, YearMonth·ParticipantAvailability·MeetupReportQuery·MeetupReportCalculator 컴파일 실패, InsightsMeetupReportRendering 컴파일 실패, 천 단위 구분 1건 — 모두 GREEN. 최종 리뷰 반영분 RED 4건(조회 테스트 flush·clear 후 µs 절삭으로 실패 — DB 경로를 실제로 타는지 증명, 날짜 계산기 6단계, insights BreadcrumbList 이름, en 'Median 1 day') -> GREEN, 두 모드 혼합 요일 테스트 추가. 개인정보처리방침 카카오 항목 RED 1건(PolicyPagesLocaleRenderingTest — main 본문 기준, 푸터 이메일로 통과하지 않게) -> GREEN. 전체 스위트 140 클래스 904 테스트 failures 0 errors 0. 하네스 375·1280px: /guide 이미지 6/6, 리포트 막대 15, 가로 넘침 0. 문서: ADR common/seo/0015·datedate/domain/0013 신설, common/seo/0013 상태 행, common/seo/0011 상태 행(바이라인 -> 0015), datedate/domain/0012 제목 변경 포인터, common/security/0004 삭제 요청 절차 메모, README 두 뷰·폴더 수(99 -> 101), 루트 CLAUDE.md(InsightsService·/guide 줄), datedate/application/CLAUDE.md(InsightsService·SeoService sameAs). 범위 밖(보고만): RecapService 요일 집계의 기존 모드 날짜 어긋남(YearMonth.dateOf 로 고칠 수 있음), 로케일 쿠키(lang)가 개인정보처리방침 쿠키 목록에 없음."
