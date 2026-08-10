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


### =====================================================================
### [SECTION A] Stock 봇 PR-1 ~ PR-6
### =====================================================================

### Commit 1 — ✅ DONE (9f1a960) feat(stock): 관측성 인프라 추가 - 거래 이벤트 로거 / KST 회전 / BotStatus 메트릭
### Commit 2 — ✅ DONE (5a5be58) refactor(stock): 시간/설정 정합화 - tradingLoopStart 분리 / 휴일 / 매직넘버 외부화
### Commit 3 — ✅ DONE (d135907) feat(stock): UniverseBuilder 도입 및 BotStatus 메트릭 필드 보완
### Commit 4 — ✅ DONE (3334fb18) PR-4 진입 검증 위양성 제거 + 익절 비순차화 + EntryAttempt
### Commit 5 — ✅ DONE (96f9c66e) PR-5 동시성/병렬화
### Commit 6 — ✅ DONE (0a562210)  PR-6 PAPER/BACKTEST 모드 + 단위 테스트 4종
### =====================================================================
### [SECTION B] SEO / i18n 시리즈 (2026-05-01)
### =====================================================================
# Commit 7 — ✅ DONE (c1578d13) fix(i18n): EN 모드 하드코딩 KO 문자열 + 일정 타이틀 천단위 쉼표
# Commit 8 — ✅ DONE (75fd434e) refactor(seo): sitemap lastmod 신뢰도 + ISO 8601 + XML escape
# Commit 9 — ✅ DONE (5e1e14e1) feat(seo): hreflang/og:locale + 네이버 검증 + robots 연도 확장
# Commit 10 — ✅ DONE (3f8621c6) docs: SEO 진화 플레이북
# =====================================================================
# [SECTION C] DateDate UI/UX 정리w
# =====================================================================
# Commit 11 — ✅ DONE (c00b1d7e) fix(datedate): New Schedule 모달 Year/Month 동적 초기화
# Commit 12 — ✅ DONE (5701ef16) fix(datedate): Create Schedule 모달 fragment + 헤더 CTA hijack
# Commit 13 — ✅ DONE (0aaee104) fix(datedate): 참가자 식별 색상 ↔ 셀 강조 색 동기화
# Commit 14 — ✅ DONE (015bfd94) fix(ui): 온보딩 배너 영구화 (localStorage)
# =====================================================================
# [SECTION D] Reserved Owner ID — i18n + UX 정합화
# =====================================================================
# Commit 15 — ✅ DONE (ed5087b0) fix(i18n): 예약 ID 토스트 로케일 해석 + 도메인 정합화
# Commit 16 — ✅ DONE (72b5f41d) docs(ux): UX-validation 가이드
# =====================================================================
# [SECTION E] Lighthouse 페이로드 / 로케일 영속
# =====================================================================
# Commit 17 — ✅ DONE () perf(seo): 폰트/manifest 최적화로 Lighthouse 페이로드 -1.8MB
# Commit 18 — ✅ DONE () fix(i18n): 내부 내비게이션·서버 리다이렉트의 ?lang 쿼리 보존

# =====================================================================
# [SECTION F] CLAUDE.md / ADR / 루트 문서 동기화 (이전 doc 누락분)
# =====================================================================
# Commit 19 — ✅ DONE () docs: CLAUDE.md / ADR / 루트 문서 동기화
# =====================================================================
# [SECTION G] Bing IndexNow 능동 색인 통보 (2026-05-01)
# =====================================================================
# Commit 20 — ✅ DONE () feat(seo): IndexNow 통합 (사이트맵 URL 일 1회 자동 제출)
# =====================================================================
# [SECTION H] DataLayer 비즈니스 이벤트 + GTM 측정 ID 진단 정정 (2026-05-09)
# =====================================================================
# 형식 메모: 본 섹션부터 -m "..." 메시지 본문의 슬래시는 PowerShell 호환을
#   위해 ／ (U+FF0F full-width solidus) 로 표기. git add 의 파일 경로는
#   시스템 경로라 일반 / 유지. 코멘트(#) 의 / 도 유지 (git 에 전달되지 않음).
# Commit 21 — ✅ DONE () feat(analytics): dataLayer 비즈니스 이벤트 6종 + GTM stale 진단 정정
# =====================================================================
# [SECTION I] Data Analytics 문서화 (2026-05-09)
# =====================================================================
# Commit 22 — ✅ DONE () docs(da): Data Analytics 문서 모음 — 현재 상황·인벤토리·플레이북·TODO·확장

# =====================================================================
# [SECTION J] AdSense Low Value Content 재심사 대응 (2026-05-10)
# =====================================================================
# 형식 메모: Section H 이후 컨벤션 유지 — -m "..." 메시지 본문의 슬래시는 ／
#   (U+FF0F full-width solidus) 로 표기. PowerShell + Git for Windows (MSYS) 의
#   인자 path-conversion 회피용. git add 파일 경로 / # 코멘트의 / 는 일반 / 유지
# Commit 23 — ✅ DONE () feat(seo): 환경변수 기반 AdSense + UseCaseSlugs SSOT + Runner noindex + ad-slot 인접 마진
# Commit 24 — ✅ DONE () feat(seo): SeoService 4축 보강 + ／about 신뢰 페이지 + insights hasData + use-case 5섹션 템플릿
# Commit 25 — ✅ DONE () refactor(seo): SitemapService — Runner 4 + 동적 Run 루프 제거 + UseCaseSlugs.ALL 루프 + ／about 양방향 (RunRepository 의존성 제거)

# Commit 26 — ✅ DONE () feat(content): ／tools／date-diff 본문 확장 + use-case 4 슬러그 5섹션 KO+EN 콘텐츠 (~7,660 단어)
# Commit 27 — ✅ DONE () docs(seo): adsense_approval Low Value Content 프레이밍 + 정책 매핑 감사 + 회고 글감


# =====================================================================
# [SECTION K] /tools/date-diff 공개 + 노출 + Editorial 디자인 적용 (2026-05-20)
# =====================================================================
# 컨텍스트: Commit 26 가 /tools/date-diff 본문을 ~600단어로 확장했지만 (a) 페이지가
#   SecurityConfig 의 어떤 permitAll 패턴에도 매치되지 않아 .anyRequest().authenticated()
#   에 잡혀 사실상 로그인 요구 상태였고 (b) header/footer 어느 곳에도 링크가 없어
#   "유령 페이지" 였으며 (c) 다른 도구 페이지가 없는 현 상태에서 단조로운 위젯 UI 만
#   존재했음. 본 세션이 세 축을 일괄 정리.
# Commit 28 — ✅ DONE () feat(tools): /tools/date-diff 보안 공개 + nav/footer 노출 + Editorial Almanac 리디자인

# =====================================================================
# [SECTION L] DateDate funnel 이벤트 5종 추적 보강 (2026-05-24)
# =====================================================================
# Commit 29 — ✅ DONE () docs(analytics): datedate funnel 이벤트 5종 추적 보강 design spec
# Commit 30 — ✅ DONE ()  docs(analytics): datedate 이벤트 추적 보강 구현 plan
# Commit 31 — ✅ DONE ()  feat(analytics): datedate funnel 5 events for GA4／BigQuery

# =====================================================================
# 검증 (Section L — 커밋 전후 자동 점검; PowerShell + cmd.exe 모두 동작)
# =====================================================================

# L-1) Java 회귀 테스트 (백엔드 무변경이라 모든 기존 테스트 통과해야 함)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# L-2) 전체 빌드
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat build"

# L-3) bootRun 후 DevTools Console 매뉴얼 검증 (사용자가 브라우저에서)
# - 일정 생성 → localStorage.getItem('dd_owned_schedules') 에 ownerId 포함
# - 오너 대시보드 진입 → window.dataLayer.filter(e => e.event === 'owner_dashboard_viewed') 길이 ≥ 1, owner_id_hash 64자 hex
# - 일정 yy/mm URL → schedule_viewed is_owner: true
# - 링크 복사 → link_shared share_method: 'clipboard'
# - 장소 ／ 메뉴 추가 → location_added ／ menu_added count_after ≥ 1
# - 시크릿 모드 같은 URL → schedule_viewed is_owner: false
# - 기존 4 이벤트 (schedule_created ／ participant_added ／ selections_saved ／ vote_cast) 회귀 없음

# =====================================================================
# 검증 (Section J — 커밋 전후 자동 점검; PowerShell + cmd.exe 모두 동작)
# =====================================================================

# 1) 전체 빌드 + 테스트
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# 2) Sitemap 회귀 테스트
# - Runner URL 미포함
# - /about 양방향 hreflang 포함
# - hreflang 12*2*3=72 검증
# - lastmod ISO 8601 KST offset 형식 검증 포함
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *SitemapServiceHreflangTest*"

# 3) SEO i18n 회귀 테스트
# - FAQPage JSON-LD KO/EN 양쪽 유효 JSON 검증
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *SeoServiceI18nTest*"

# 4) Reserved Owner ID / 예외 메시지 i18n 회귀 테스트
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *GlobalExceptionHandlerI18nTest* --tests *OwnerServiceTest* --tests *ReservedOwnerIdsTest*"

# 5) Locale persistence / locale link 통합 테스트
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *LocaleLinksTest* --tests *LocalePersistenceIntegrationTest*"

# 6) IndexNow 회귀 테스트
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *IndexNowServiceTest*"

# =====================================================================
# 배포 후 운영 검증
# =====================================================================

# 7) /about 응답 확인
curl.exe -I https://datedate.site/about

# 8) sitemap loc 출력 확인
curl.exe -s https://datedate.site/sitemap.xml | findstr /C:"<loc>"

# 9) sitemap 에서 runners URL 제거 확인
curl.exe -s https://datedate.site/sitemap.xml | findstr /C:"runners"
# 위 결과는 0줄이어야 함.
# Runner URL 이 sitemap 에서 완전 제거되어야 함.

# 10) sitemap lastmod 형식 확인
# ISO 8601 KST offset 이어야 함.
curl.exe -s https://datedate.site/sitemap.xml | findstr /C:"<lastmod>"

# 11) /about JSON-LD Organization 채움 확인
curl.exe -s https://datedate.site/about | findstr /C:"\"@type\": \"Organization\""

# 12) 광고 미로드 확인
# 재심사 통과 전에는 adsense.client 가 빈 값이어야 함.
curl.exe -s https://datedate.site/ | findstr /C:"adsbygoogle.js"
# 어떤 페이지도 adsbygoogle.js 를 로드하지 않으므로 0줄이어야 함.

# 13) Naver 검증 파일 서빙 확인
curl.exe -I https://datedate.site/naver52cf63f6fb22d9c9f017934c5d0b7d5c.html
curl.exe    https://datedate.site/naver52cf63f6fb22d9c9f017934c5d0b7d5c.html

# 14) IndexNow 키 파일 서빙 확인
curl.exe -I https://datedate.site/1dfcb4404e1d4f6fae3423fd163f97b8.txt
curl.exe    https://datedate.site/1dfcb4404e1d4f6fae3423fd163f97b8.txt

# =====================================================================
# PowerShell / cmd.exe 호환 메모
# =====================================================================

# - PowerShell 에서는 curl 별칭 충돌 방지를 위해 curl.exe 사용 권장.
# - 테스트 명령은 cmd.exe /c 로 실행하여 PowerShell 과 cmd.exe 모두에서 동일하게 동작.
# - git commit 명령 작성 시:
#   (a) -m 메시지 본문에는 일반 / 대신 ／ U+FF0F 사용,
#   (b) git add 파일 경로는 일반 / 사용,
#   (c) 멀티 -m 으로 본문 paragraph 분리,
#   (d) 큰따옴표만 사용하여 cmd.exe 작은따옴표 미지원 문제 회피.
# - PowerShell / cmd.exe / Git Bash / WSL bash 4개 환경에서 복사 실행 가능하도록 유지.

# =====================================================================
# [SECTION M] SEO 정합성 보강 — noindex 방어 심층화 ／ 검증 env 일관화 ／ sitemap-noindex 모순 제거 (2026-05-25)
# =====================================================================
# 컨텍스트: SEO 라이브 점검 (sitemap.xml, robots.txt, /trading, /stock, /h2-console,
#   /nonexistent-owner 등) 에서 3개 정합성 갭 발견. 모두 단발성 픽스로 한 커밋에 묶음.
#   (1) /trading, /stock 이 200 HTML 로 응답하지만 자체 head fragment 에 robots
#       메타 자체가 없음 — robots.txt 만으로 인덱싱 차단 (우회 ／ 직접 링크 노출 시 위험).
#   (2) google-site-verification 토큰이 head.html 에 하드코딩 — naver-site-verification
#       은 이미 env 기반인데 일관성 결여.
#   (3) /insights/trends 가 sitemap 에 항상 포함되지만 SeoService 는 인기 데이터
#       (Location ／ Menu) 가 비면 noindex 응답 — sitemap 광고 ↔ noindex 모순 신호.

# Commit 32 — ✅ DONE () feat(seo): trading／stock noindex 메타 + Google 검증 env-driven + insights／trends sitemap 조건부
# Why: 위 3개 정합성 갭 일괄 해소.
#   (a) trading／stock head fragment 에 robots ／ googlebot noindex,nofollow,noarchive
#       메타 추가 — robots.txt 우회 ／ 직접 링크 노출 시에도 인덱싱 차단 (방어 심층화).
#   (b) google-site-verification 을 seo.google-site-verification 프로퍼티화 — 기존
#       토큰을 기본값으로 보존해 운영 무중단, 도메인／환경별 교체 시 GOOGLE_SITE_VERIFICATION
#       env 로 오버라이드. Naver 토큰 (Commit 9) 과 일관성 회복.
#   (c) SitemapService.computeInsightsLastmod → computeInsightsLastmodIfPresent
#       (Optional 반환). 인기 데이터 없으면 /insights/trends 엔트리 자체 미생성 —
#       SeoService.getInsightsTrendsSeo(hasData=false) (Commit 24) 의 noindex 강등 정책과 정합.

# =====================================================================
# 검증 (Section M — 커밋 전후 자동 점검)
# =====================================================================

# M-1) 전체 빌드 + 테스트 (이미 통과 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# M-2) Sitemap 회귀 — /insights/trends 조건부 포함 ／ 카운트 66 검증
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *SitemapServiceHreflangTest*"

# =====================================================================
# 배포 후 운영 검증
# =====================================================================

# M-3) /trading, /stock 에 noindex 메타 적용 확인
curl.exe -s https://datedate.site/trading | findstr /C:"name=\"robots\""
curl.exe -s https://datedate.site/stock   | findstr /C:"name=\"robots\""
# 두 페이지 모두 noindex, nofollow, noarchive 한 줄씩 출력되어야 함.

# M-4) google-site-verification 메타 (env-driven) 정상 노출 확인
curl.exe -s https://datedate.site/ | findstr /C:"google-site-verification"
# YVK_KclWiLH24rqy7kAI9iNYSA5No9ljXbnSOvsQB4k 가 출력되어야 함.

# M-5) Sitemap 의 /insights/trends 조건부 포함 확인
# 실제 운영 환경에 인기 데이터가 있으면 포함, 없으면 제외.
curl.exe -s https://datedate.site/sitemap.xml | findstr /C:"insights/trends"

# =====================================================================
# [SECTION N] 코인 트레이딩 수익성 개선 — 감사 P0/P1/P2 + #3 (2026-05-30)
# =====================================================================
# 멀티에이전트 수익성 감사(docs/audit/coin-trading-profit-audit-2026-05-30.md) 단계적 구현. 대상 KRW-ADA.
# 각 커밋의 *근거·상세*는 ADR(docs/adr/trading/*) 에. 본 파일은 커밋 시퀀스만 담고, 검증은 맨 끝 1회.
# ⚠️ 신호/리스크 변경분은 LIVE 전 PAPER 백테스트 권장. -m 본문 슬래시는 ／(U+FF0F).
#
# 파일 소유(working-tree 일괄 보유 → "처음 등장 커밋이 파일 소유", 이후 동일 파일 변경은 흡수):
#   · 설정/진입(TradingProperties·application.yaml·.env.example·BithumbApiClient) = C34
#   · ADR README·trading CLAUDE.md×2 = C37
#   · TradingBotService·RiskManagementService = C35 / RebalanceService = C38 /
#     IndicatorService = C42 / DivergenceService = C44 / SignalService = C49

# Commit 33 — ✅ DONE() docs: 수익성 감사 보고서 + ADA 결합도 점검
# Commit 34 — ✅ DONE(d368f833) feat(trading): P0-1 PAPER／BACKTEST 모드 가드 + 인메모리 체결 (ADR modes/0001)
# Commit 35 — ✅ DONE() feat(trading): P0-2 서킷브레이커 (연속／일일 손실 한도) — ADR risk/0001
# Commit 36 — chore(trading): P0-4 스케줄러 스레드 풀(spring.task.scheduling.pool.size=4) — application.yaml(C34 흡수), 실행 블록 없음.
# Commit 37 — ✅ DONE() docs(trading): P0/P1-4 ADR(modes-0001／risk-0001) + CLAUDE.md 동기화 + min-sell-pnl 0%
# Commit 38 — ✅ DONE() feat(trading): P1-3 리밸런싱 회계 정합 (Position 생성/청산 기반) — ADR risk/0002
# Commit 39 — ✅ DONE() refactor(trading): P0-3a 주문 실행 트랜잭션 경계 (HTTP/sleep 을 tx 밖으로) — ADR infrastructure/0001 (TradingBotService→C35 흡수)
# Commit 40 — ✅ DONE() fix(trading): P1-8/P1-9 수수료 비용모델 일원화 (순수 마진 임계 + 쿠폰) — ADR strategy/0004 (TradingProperties·yaml→C34)
# Commit 41 — ✅ DONE() fix(trading): P1-1/P1-2 출구 R:R 재보정 (stop -1.5%/TP +3%/trailing) — ADR strategy/0005 (yaml 설정값→C34)
# Commit 42 — ✅ DONE() feat(trading): P2-4 Wilder RSI + P2-13 거래량 MA + P2-14 문서 — ADR strategy/0006 (IndicatorService 소유; P2-5/6/2 변경 흡수)
# Commit 43 — fix(trading): P0-3b 청산/리밸런스 tx 경계 + 리뷰 #4/#6 — 흡수(RiskMgmt→C35, RebalanceService+AccountingTest→C38, #6 yaml→C34). 별도 실행 블록 없음. (ADR infrastructure/0001)
#   리뷰 반박: minSellPnl*100(정상 ratio→percent), Wilder period+1 경계(정상 seed RSI). #5 무해(stop 우선).
# Commit 44 — ✅ DONE() feat(trading): P2-1 다이버전스 피벗 강화 (k봉 + MIN_DISTANCE) — ADR strategy/0007 (DivergenceService 소유; P2-2 흡수)
# Commit 45 — ✅ DONE() feat(trading): P2-8/P2-10/P2-12 진입·시간 리스크 가드 — ADR risk/0003 (RiskMgmt/TradingBotService→C35 흡수)
# Commit 46 — ✅ DONE() feat(trading): P2-5 슬로우 스토캐스틱 + P2-6 RSI 추세 최소 델타 — ADR strategy/0008 (IndicatorService→C42 흡수)
# Commit 47 — ✅ DONE() feat(trading): P2-9 쿨다운 + P2-11 엔진 핑퐁 방지 + #3 수동 매매 Position 정합 — ADR risk/0004 (TradingBotService→C35, RebalanceService→C38 흡수)
# Commit 48 — ✅ DONE() feat(trading): P2-2 형성 중 현재봉 제외 옵션 (기본 OFF) — ADR strategy/0009 (IndicatorService→C42, DivergenceService→C44 흡수)
# Commit 49 — ✅ DONE() refactor(trading): P2-7 모멘텀 가중 하향 (MA Trend ±15→±8, MA State ±10→±5; ±135→±128) — ADR strategy/0010 (CLAUDE.md×2→C37) — 마지막 커밋(git-commit.md 포함)

# =====================================================================
# 검증 (Section N) — 커밋 후 전체 회귀 1회 (신규 트레이딩 테스트 ~46종 포함, 전체 GREEN 확인됨)
# =====================================================================
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# =====================================================================
# [SECTION O] Stock 봇 진단 수정 — 로그 분석 P0/P1 (2026-06-03)
# =====================================================================
# 컨텍스트: stock-trading-*.log(5거래일) 분석 + 15-에이전트 워크플로 검증 결과,
#   봇이 매일 0건 선정(운영 이래 매매 0)인 원인이 3중 인프라 결함으로 확정.
#     P0-3 체결강도 영구 0  — KisQuoteResponse 가 inquire-price 가 주지 않는
#          seln_cntg_smtn／shnu_cntg_smtn 로 계산 → 항상 0. 정작 제공되는 cttr 미사용.
#     P0-2 유니버스 동적 랭킹 미구현 — rank=0, 매일 정적 대형주 fallback 70 (갭 없음).
#     P1  조용한 오설정 — 기동 시 유효설정/위험 가시화 부재 + 0건 선정 침묵.
#   (P0-1 "프로세스 09:20 사망"은 근거 약함 — 5개 로그가 09:20 스크리닝 이메일 첨부
#    스냅샷(StockMailService 첨부명 == 파일명, events／sql 아카이브 부재)이라 아티팩트.
#    라이브 풀데이 로그 확인 후에만 손댈 것. 본 섹션에서 제외.)
#   ⚠️ Bot.mode 기본값 LIVE + yaml 에 mode 키 없음 → 본 수정으로 종목 선정·진입 시
#      실주문 가능. 첫 거래일은 stock.bot.mode=PAPER 로 검증 권장.
#   상세 근거: ADR stock/algorithm/0005, ADR-0002 갱신.
#
# 파일 소유(working-tree 일괄 보유 → "처음 등장 커밋이 파일 소유", 이후 동일 파일 흡수):
#   · KisRestClient.java          = C50 (P0-2 getTopVolumeCodes 변경도 흡수)
#   · stock/CLAUDE.md             = C51 (P1 관측성 항목 변경도 흡수)

# Commit 50 — ✅ DONE() fix(stock): P0-3 체결강도 cttr 매핑 (항상 0 → KIS 직접 제공값)
#   주의: KisRestClient.java 의 P0-2 getTopVolumeCodes 추가도 이 커밋이 소유(흡수).

# Commit 51 — ✅ DONE() feat(stock): P0-2 거래량순위 동적 유니버스 + ADR-0005
#   주의: stock/CLAUDE.md 의 P1 관측성 항목 변경도 이 커밋이 소유(흡수).
#         KisRestClient.getTopVolumeCodes 는 C50 에 흡수(여기서 add 안 함).

# Commit 52 — ✅ DONE() feat(stock): P1 기동 설정 검증기 + 스크리닝 침묵실패 WARN — 마지막 커밋(git-commit.md 포함)

# =====================================================================
# 검증 (Section O) — 커밋 전후 자동 점검
# =====================================================================

# O-1) 전체 빌드 + 테스트 (Spring 컨텍스트 로딩 + 신규 테스트 23종 GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# O-2) 신규 단위 테스트만 (체결강도 6 + 유니버스 8 + 설정검증기 9)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *KisQuoteResponseTest --tests *UniverseBuilderTest --tests *StockBotConfigValidatorTest"

# =====================================================================
# 배포 후 운영 검증 (다음 거래일 stock-trading 로그)
# =====================================================================
# O-3) 기동 로그 — 유효 설정 요약 + LIVE 모드 경고 노출 확인
#   "[StockBot] enabled=true, mode=?, universe=거래량순위 top-30 …" + LIVE 면 "⚠️ LIVE 모드".
# O-4) 08:30 pre-market — "Universe refreshed … rank=30" (rank>0 = 동적 소스 정상; rank=0 이면 폴백 중)
# O-5) 09:20 screening — "Floor passed > 0" → "Selected > 0".
#   0건이면 신규 WARN "0 selected from N universe — 최다 탈락 버킷: …" 으로 사유 즉시 확인.
# O-6) PAPER 검증 시 진입 로그 — "[PAPER] BUY_MARKET simulated …".

# =====================================================================
# [SECTION P] AdSense Low Value Content 재심사 2차 대응 (2026-06-11)
# =====================================================================
# 컨텍스트: 4-에이전트 감사 (repo 라우트／SEO 인프라, 전체 템플릿, i18n 카탈로그,
#   라이브 도메인) 결과 사이트는 대체로 건강 — 남은 실질 갭 3개 + 회귀 가드 부재.
#     (1) privacy／terms 가 하드코딩 한국어 (lang="ko", i18n 키 0개) 인데 sitemap +
#         hreflang 은 ?lang=en 영문 대체 페이지를 광고 — EN 방문자에게 한국어 법적 문서.
#     (2) GET ／{ownerId} 가 getOrCreateOwner 호출 — 임의 URL 이 HTTP 200 + Owner row
#         영속화 (라이브 확인). 소프트 404 + 크롤러 발 DB 오염.
#     (3) guide 에 트러블슈팅 FAQ 부재.
#   ⚠️ 배포 블로커: 라이브가 repo 보다 구버전 — 라이브 ／trading 에 커밋된 noindex
#      메타 부재 (Section M Commit 32 미배포). 재심사 요청 전 반드시 배포.
#   상세: docs/seo/adsense-low-value-content-remediation.md, ADR datedate/domain/0004.
#
# 파일 소유(working-tree 일괄 보유 → "처음 등장 커밋이 파일 소유", 이후 동일 파일 흡수):
#   · messages.properties·messages_en.properties·PolicyPagesLocaleRenderingTest = C55
#     (C56 guide 트러블슈팅 키／렌더 테스트 변경도 흡수)
#   · docs/adr/README.md = C51(Section O) 소유 — ADR-0004 행 추가분은 C51 에 흡수.

# Commit 53 — ✅ DONE() test(seo): AdSense 리뷰 불변식 회귀 가드 4종

# Commit 54 — ✅ DONE() fix(datedate): GET ／{ownerId} owner 자동 생성 제거 (ADR 0004)
#   주의: docs/adr/README.md 의 ADR-0004 행 추가분은 C51(Section O) 이 소유(흡수).

# Commit 55 — ✅ DONE() feat(i18n): privacy／terms 법적 페이지 i18n 전환 + 영문 번역
#   주의: messages*.properties 와 PolicyPagesLocaleRenderingTest 의 C56(guide
#   트러블슈팅) 변경분도 본 커밋이 소유(흡수).

# Commit 56 — ✅ DONE() feat(guide): 트러블슈팅 FAQ 6문항 추가 (ko／en)
#   주의: 트러블슈팅 i18n 키(messages*.properties)와 렌더 테스트 변경분은 C55 가 소유.

# Commit 57 — docs(adsense): low-value-content 재발 방지 보고서 — 마지막 커밋(git-commit.md 포함)

# =====================================================================
# 검증 (Section P — 커밋 전후 자동 점검)
# =====================================================================

# P-1) 신규 회귀 가드 + 통합 테스트 (전체 GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *MessageCatalogParityTest --tests *TemplatePlaceholderHygieneTest --tests *UseCaseContentCompletenessTest --tests *SitemapServiceWhitelistTest --tests *OwnerDashboard404IntegrationTest --tests *PolicyPagesLocaleRenderingTest"

# P-2) 전체 빌드 + 테스트 (2026-06-11 기준 287 tests / 1 failed)
#   ⚠️ 알려진 선재 실패: CorsConfigTest.apiPreflightAllowsCrossOrigin — Section P 와 무관
#   (본 세션 변경분을 stash 한 상태에서도 동일 실패 재현 확인). 이전 CORS 세션의 미커밋
#   작업(@WebMvcTest 슬라이스에 SecurityConfig 의 .cors() 미적용)으로 추정. 별도 수정 필요.
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat build"

# =====================================================================
# 배포 후 운영 검증 (Section P)
# =====================================================================

# P-3) 미존재 owner URL → 404 (배포 전 라이브는 200)
curl.exe -s -o NUL -w "%{http_code}" https://datedate.site/zz-no-such-page-xq9
# 404 가 출력되어야 함.

# P-4) /privacy?lang=en 영문 본문 확인
curl.exe -s "https://datedate.site/privacy?lang=en" | findstr /C:"Information We Collect"
# 1줄 이상 출력되어야 함.

# P-5) /terms?lang=en 영문 본문 + lang=en 확인
curl.exe -s "https://datedate.site/terms?lang=en" | findstr /C:"Service Overview"
curl.exe -s "https://datedate.site/terms?lang=en" | findstr /C:"lang=\"en\""

# P-6) guide 트러블슈팅 섹션 렌더 확인
curl.exe -s https://datedate.site/guide | findstr /C:"guide-faq-1"

# P-7) (Section M 재확인) /trading, /stock noindex 메타 — 배포 블로커 해소 확인
curl.exe -s https://datedate.site/trading | findstr /C:"name=\"robots\""
curl.exe -s https://datedate.site/stock   | findstr /C:"name=\"robots\""
# 두 페이지 모두 noindex, nofollow, noarchive 한 줄씩 출력되어야 함.

# P-8) 광고 미로드 재확인 (승인 전 슬롯 ID 미설정 상태 유지)
curl.exe -s https://datedate.site/ | findstr /C:"adsbygoogle.js"
# 0줄이어야 함.

# =====================================================================
# [SECTION Q] AdSense low-value-content — lean strengthen (2026-06-20)
# =====================================================================
# 설계 문서: docs/superpowers/specs/2026-06-20-datedate-adsense-lean-strengthen-design.md
# 목표: AdSense "가치가 별로 없는 콘텐츠(low value content)" 해소 — 콘텐츠 폭／깊이 보강
#       + 단일 템플릿 4페이지의 cookie-cutter 신호 완화. (기술 SEO 는 이미 정상)

# Q-검증) 변경 영역 타깃 테스트 (GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *UseCaseContentCompletenessTest --tests *MessageCatalogParityTest --tests *SitemapServiceWhitelistTest --tests *SitemapServiceHreflangTest --tests *UseCaseLocaleRenderingTest --tests *SeoServiceI18nTest --tests *PolicyPagesLocaleRenderingTest --tests *TemplatePlaceholderHygieneTest"
# 배포 후: curl.exe -s https://datedate.site/use-cases/club-activity | findstr /C:"동호회"  (ko 본문 충실 렌더 확인)

# =====================================================================
# [SECTION R] noindex 페이지 hreflang 빈 href 결함 수정 (2026-06-25)
# =====================================================================
# 컨텍스트: GSC "적절한 표준 태그가 포함된 대체 페이지" 진단 결과, 리포트의 11개
#   ?lang= URL 대부분은 정상적 canonical 통합(?lang=ko → bare URL)으로 코드 버그 아님
#   — sitemap 에 lang=ko 0건, 대표 URL 자기참조 canonical 정상. "검사 실패"는 alternate
#   URL 이 설계상 색인되지 않아 구조적으로 통과 불가(정상).
#   단, 진단 중 실제 결함 1건 발견: noindex Runner／Admin 페이지(/runners?lang=en 등)가
#   canonicalKo／En 미설정인데 SeoMetadata 의 hreflangEnabled 기본값이 true 라
#   빈 href hreflang(hreflang="ko" href="")＋불필요한 og:locale:alternate 를 출력 —
#   SeoMetadata.java 에 이미 문서화된 "noindex 페이지엔 hreflangEnabled=false" 결정 위반.
#   결정 변경 아님(기존 문서화 결정 준수) → ADR／CLAUDE.md 변경 불필요.
#
# 파일 소유: head.html / RunnerController.java / RunnerAdminController.java 는 Section P/Q
#   어느 pending 커밋에도 없어 본 커밋이 단독 소유. git-commit.md 는 본 섹션 마지막 커밋이 포함.

# Commit 58 — ✅ DONE() fix(seo): noindex 페이지 빈 href hreflang + 불필요한 og:locale:alternate 제거 — 마지막 커밋(git-commit.md 포함)

# =====================================================================
# 검증 (Section R — 커밋 전후 자동 점검)
# =====================================================================

# R-1) 렌더 / SEO / sitemap 회귀 (BUILD SUCCESSFUL 확인됨 — head.html 파싱 정상 + 이중언어 hreflang 무영향)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *LocaleRenderingTest --tests *PolicyPagesLocaleRenderingTest --tests *UseCaseLocaleRenderingTest --tests *SeoServiceI18nTest --tests *SitemapServiceHreflangTest"

# =====================================================================
# 배포 후 운영 검증 (Section R)
# =====================================================================

# R-2) noindex 페이지에 hreflang=ko 링크 미출력 — 빈 href 결함 해소 확인
curl.exe -s "https://datedate.site/runners?lang=en" | findstr /C:"hreflang=\"ko\""
# 0줄이어야 함 (이전: hreflang=\"ko\" href=\"\" 빈 href 출력).

# R-3) 이중언어 페이지 hreflang 정상 유지 — 회귀 없음 확인
curl.exe -s "https://datedate.site/guide?lang=en" | findstr /C:"hreflang=\"ko\""
# 1줄 이상 (href=https://datedate.site/guide 채워진 상태) 출력되어야 함.

# =====================================================================
# [SECTION S] apps-in-toss 미니앱 /api CORS 허용 — 이전 세션 미커밋 작업 회수 (2026-07-01)
# =====================================================================
# 컨텍스트: Section P 검증 노트(P-2)에서 "알려진 선재 실패"로 기록된
#   CorsConfigTest.apiPreflightAllowsCrossOrigin 의 원인 작업 — 이전 CORS 세션이
#   구현만 하고 커밋하지 않은 /api CORS 설정. datedate 모바일(앱인토스 WebView 미니앱,
#   별도 레포·별도 origin)이 https://datedate.site/api/** 를 브라우저 fetch 로 호출할 수
#   있도록 허용한다. /api/** 는 무인증 공개 엔드포인트(permitAll＋CSRF 비활성, 쿠키 미사용)라
#   allowCredentials=false + origin 패턴 폭넓게 허용. 신규 결정 → ADR 0002 + common
#   CLAUDE.md 동기화 (CLAUDE.md / ADR 동기화 규칙 준수).
#   P-2 의 CorsConfigTest 실패 원인: @WebMvcTest 슬라이스가 CorsConfig(빈)만 import 하고
#   .cors() 를 켜는 SecurityConfig 는 미포함 → CORS 필터 미적용. 본 섹션에서 슬라이스에
#   SecurityConfig 를 함께 import 하여 실제 .cors() 배선을 엔드투엔드로 검증 → GREEN.
#
# 파일 소유(working-tree 일괄 보유 → "처음 등장 커밋이 파일 소유"):
#   · CorsConfig.java / SecurityConfig.java / common CLAUDE.md / ADR 0002 = C59 단독 소유.
#     어느 pending 섹션(N/O/P/Q/R)에도 없음.
#   · CorsConfigTest.java = C60 단독 소유.
#   · .gitignore(.claude/*) 는 별개 관심사(로컬 작업 디렉터리 제외) → C61 독립 커밋.
#   git-commit.md 는 본 섹션 마지막 커밋(C61)이 포함.

# Commit 59 — ✅ DONE() feat(security): apps-in-toss 미니앱용 /api CORS 허용 (ADR common/security/0002)

# Commit 60 — ✅ DONE() test(security): /api preflight 교차출처 허용 회귀 테스트 (P-2 선재 실패 해소)

# Commit 61 — chore: .gitignore 에 .claude/ 로컬 작업 디렉터리 제외 — 마지막 커밋(git-commit.md 포함)
git add .gitignore docs/guides/git-commit.md
git commit -m "chore: .gitignore 에 .claude／ 로컬 작업 디렉터리 제외" -m "Claude Code 세션 산출물(.claude／*) 을 추적 대상에서 제외."

# =====================================================================
# 검증 (Section S — 커밋 전후 자동 점검)
# =====================================================================

# S-1) CORS preflight 회귀 테스트 (BUILD SUCCESSFUL 확인됨 — P-2 선재 실패 해소)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *CorsConfigTest"

# S-2) 전체 빌드 (선재 실패였던 CorsConfigTest 포함 GREEN 확인)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat build"

# =====================================================================
# 배포 후 운영 검증 (Section S)
# =====================================================================

# S-3) 라이브 /api preflight 가 교차출처를 허용하는지 확인
curl.exe -s -i -X OPTIONS "https://datedate.site/api/owners" -H "Origin: https://example-miniapp.toss.im" -H "Access-Control-Request-Method: POST" | findstr /C:"Access-Control-Allow-Origin"
# Access-Control-Allow-Origin: https://example-miniapp.toss.im 가 출력되어야 함.

# =====================================================================
# [SECTION T] 코인 트레이딩 P0-1 보안 + P0-2 주문 멱등성 + Bithumb v2 마이그레이션 착수 (2026-07-06)
# =====================================================================
# ⚠️ 선재 미커밋 상태 경고: 이번 세션 시작 시점에 working tree 에 이미 광범위한 미커밋 변경이
#   있었다(session-start git status: CLAUDE.md, build.gradle, SecurityConfig.java,
#   common/CLAUDE.md, docs/troubleshooting/*, 다수 datedate/trading 파일 등이 이미 ' M').
#   본 섹션은 이번 세션의 "논리적 변경"을 관심사별로 정리한 것이다.
#   · [신규] 파일 = 온전히 이번 세션 소유 → 깨끗이 커밋 가능.
#   · [수정] 파일 = 선재 미커밋 변경과 한 파일에 섞여 있을 수 있음 → 커밋 전 반드시
#     `git diff <file>` 로 이번 세션 델타만인지 확인. 이 환경은 `git add -p` 미지원이라
#     파일 단위 커밋만 가능하므로, 선재 변경이 섞였으면 관련 pending 작업과 함께 처리할 것.
#   진행 문서: docs/trading/bithumb-v2-migration-plan.md(계획·진행현황),
#     docs/trading/v2-migration-handoff.md(다음 세션 인계 — 이 문서만 읽고 이어서 작업 가능),
#     docs/audit/coin-trading-operational-review-2026-07-06.md(감사 근거).
#   전체 스위트 BUILD SUCCESSFUL 확인됨(2026-07-06). 각 관심사 테스트 GREEN(아래 검증).

# ---------------------------------------------------------------------
# 관심사 1 — P0-1: 무인증 실주문·봇제어 API 차단 (security)
# ---------------------------------------------------------------------
# 컨텍스트: /api/trading/**(봇 시작·중지·수동매수·매도·긴급청산·리밸런스·실주문 test-order)와
#   /trading(제어 대시보드, 계좌·손익 서버렌더)이 /api/** permitAll + /* permitAll 에 휩쓸려
#   무인증 노출 — 서버 도달 가능한 누구나 curl 로 실계좌 주문 가능. 어떤 ADR 도 승인 안 한 신규 결함.
#   → /api/trading/**, /trading, /trading/** 를 ROLE_ADMIN 으로 분리(포괄 permitAll 보다 먼저
#   선언). 기존 러너 어드민 폼 로그인 재사용. CSRF/CORS: 무자격증명 CORS + ROLE_ADMIN 조합이
#   교차출처를 무력화(§ADR 0003). 결정 변경 → ADR common/security/0003 + CLAUDE.md 2곳 동기화.
# 파일 소유:
#   [수정·선재확인] src/main/.../common/infrastructure/config/SecurityConfig.java
#      (⚠ Section S/C59 가 CORS 로 이미 소유했던 파일 — CORS 커밋 이후 상태에 P0-1 매처 2줄 추가.
#       `git diff SecurityConfig.java` 로 P0-1 델타(/api/trading·/trading hasRole)만인지 확인)
#   [수정·선재확인] CLAUDE.md(root), src/main/.../common/CLAUDE.md — Security 접근 규칙 표 갱신
#   [수정] docs/adr/README.md — ADR 0003 인덱스 행 1줄
#   [신규] docs/adr/common/security/0003-admin-only-trading-control-api.md
#   [신규] src/test/.../trading/presentation/api/TradingApiSecurityTest.java

# Commit 62 — ✅ DONE() fix(security): 트레이딩 봇 제어·실주문 API 관리자 전용 (P0-1, ADR common/security/0003)
# Commit 63 — ✅ DONE() test(security): /api/trading·/trading 무인증/비관리자 차단 회귀 테스트

# ---------------------------------------------------------------------
# 관심사 2 — 문서: 코인 트레이딩 감사 + v2 계획 + 인계 + 트러블슈팅 (docs, 전부 [신규])
# ---------------------------------------------------------------------
# 컨텍스트: 운영 관점 감사(73건 검증)로 P0/P1/P2 도출 → P0-1 수정 + P0-2 근거. v2 마이그레이션
#   계획(적대적 검토 반영)과 다음 세션 인계 문서. 보안 슬라이스 테스트 함정 트러블슈팅.
# 파일 소유:
#   [신규] docs/audit/coin-trading-operational-review-2026-07-06.md
#   [신규] docs/trading/bithumb-v2-migration-plan.md
#   [신규] docs/trading/v2-migration-handoff.md
#   [신규] docs/troubleshooting/spring-security-webmvctest.md
#   [수정] docs/troubleshooting/README.md — 인덱스 행 + Test Errors 참조

# Commit 64 — ✅ DONE() docs(trading): 코인 트레이딩 운영 감사·v2 마이그레이션 계획·인계·보안테스트 트러블슈팅

# ---------------------------------------------------------------------
# 관심사 3 — 코인 주문 신뢰성: P0-2 멱등성 + §8-A 모드게이트 + §8-C 실체결량 + v2 어댑터 착수
# ---------------------------------------------------------------------
# 컨텍스트(계층적, 한 파일에 섞여 파일단위로만 커밋 가능):
#   · §8-A 모드 게이트 커버리지: BithumbApiClient 의 모드 게이트가 시장가 매수/매도에만 있어
#     PAPER 에서 cancelAllPendingOrders(emergencyClose 경로)·지정가·미결조회가 실계정을 타던
#     버그 → 취소·지정가·미결조회까지 게이트 확장.
#   · P0-2 멱등 재조회(v1): 주문 응답 null(타임아웃) 시 재전송이 아니라 client_order_id 로
#     재조회해 접수 여부 확인(clientOrderIdEnabled 기본 OFF — v1 생성의 client_order_id 지원이
#     문서상 불확실하므로 소액 라이브 검증 후 활성화). BithumbPrivateApi 3-인자 오버로드 +
#     getOrderByClientOrderId(GET /v1/order?client_order_id=, v1/v2 공통).
#   · §8-C 실체결량: Position 수량을 '주문금액/체결가' 유도값이 아니라 실체결(trades 합산/
#     executed_volume)로 → 장부>실잔고 드리프트(매도 잔고부족) 근원 수리. executeBuy·manualBuy.
#   · v2 착수: order-api-version enum(기본 V1, fail-fast) + BithumbV2OrderApi(POST /v2/orders
#     생성 + GET /v1/order 재조회 정규화 — v2 생성 응답엔 trades 없음) + 파사드 버전 라우팅.
#     기본 V1 이라 운영 동작 불변. application 계층 무변경(어댑터가 v1 형태로 정규화).
# 파일 소유:
#   [수정·선재확인] build.gradle — testImplementation okhttp3:mockwebserver (⚠ 선재 ' M')
#   [수정·선재확인] src/main/.../trading/infrastructure/config/TradingProperties.java
#      (⚠ 선재 언급 있음 — clientOrderIdEnabled + orderApiVersion enum 추가분만인지 확인)
#   [수정·선재확인] src/main/.../trading/infrastructure/api/BithumbApiClient.java
#      (모드게이트 확장 + 파사드 재조회 + v2 라우팅 + 생성자에 BithumbV2OrderApi 추가)
#   [수정·선재확인] src/main/.../trading/application/service/TradingBotService.java
#      (⚠ 선재 언급 다수 — extractExecutedVolume + executeBuy/manualBuy volume 델타만인지 확인)
#   [수정] src/main/.../trading/infrastructure/api/BithumbPrivateApi.java
#      (placeMarketBuy/SellOrder 3-인자 오버로드 + getOrderByClientOrderId)
#   [신규] src/main/.../trading/infrastructure/api/BithumbV2OrderApi.java
#   [신규] src/main/.../trading/infrastructure/api/dto/BithumbV2OrderCreateResponse.java
#   [수정] src/test/.../trading/infrastructure/api/BithumbApiClientModeTest.java (§8-A + 생성자)
#   [신규] src/test/.../trading/infrastructure/api/BithumbApiClientIdempotencyTest.java (재조회+라우팅)
#   [신규] src/test/.../trading/infrastructure/api/BithumbV2OrderApiTest.java (MockWebServer)
#   [신규] src/test/.../trading/application/service/TradingBotServiceExecutedVolumeTest.java

# Commit 65 — ✅ DONE() chore(build): MockWebServer 테스트 의존성 추가 (타임아웃→재조회 결정적 테스트용)

# Commit 66 — ✅ DONE() feat(trading): 주문 멱등성·모드게이트·실체결량 + Bithumb v2 어댑터/라우팅 착수 (P0-2, §8-A/C)

# Commit 67 — test(trading): 주문 멱등 재조회·버전 라우팅·실체결량·v2 어댑터(MockWebServer) 테스트 — 마지막 커밋(git-commit.md 포함)
git add src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClientModeTest.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClientIdempotencyTest.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbV2OrderApiTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceExecutedVolumeTest.java docs/guides/git-commit.md
git commit -m "test(trading): 주문 멱등 재조회·버전 라우팅·실체결량·v2 어댑터(MockWebServer) 테스트" -m "BithumbApiClientModeTest(§8-A 확장), BithumbApiClientIdempotencyTest(재조회+라우팅), BithumbV2OrderApiTest(MockWebServer: 생성 요청형식·정규화·응답유실→재조회), TradingBotServiceExecutedVolumeTest."

# =====================================================================
# 검증 (Section T — 커밋 전후 자동 점검)
# =====================================================================

# T-1) P0-1 보안 회귀 테스트 (GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *TradingApiSecurityTest --tests *CorsConfigTest"

# T-2) 주문 신뢰성 테스트 (모드게이트·멱등재조회·실체결량·v2어댑터·라우팅, GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *BithumbApiClient* --tests *BithumbV2OrderApiTest --tests *TradingBotServiceExecutedVolumeTest"

# T-3) 전체 빌드 (BUILD SUCCESSFUL 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# =====================================================================
# 배포 후 운영 검증 (Section T)
# =====================================================================

# T-4) /api/trading 봇제어 API 가 무인증으로 접근 불가한지 확인 (P0-1)
curl.exe -s -o NUL -w "%%{http_code}" -X POST "https://datedate.site/api/trading/bot/status"
# 200 이 아니어야 함(302 로그인 리다이렉트 또는 401/403). 인증 없이 봇 상태/제어 접근 차단 확인.
# 주의: order-api-version=v2, clientOrderIdEnabled 활성화는 이 섹션 범위 밖 — 소액 라이브 검증
#   (Phase 0a/2, 계획 문서 §5) 후 별도 전환. 지금은 기본 V1 + 멱등 OFF 로 운영 동작 불변.
# =====================================================================
# [SECTION U] 코인 §8-B 선영속화+틱 스윕 + Bithumb v2 잔여(취소·백오프·§8-E) (2026-07-08)
# =====================================================================
# 컨텍스트: Section T(P0-2·§8-A/C·v2 착수)에 이어 인계 문서 §6 의 남은 작업 수행.
#   · §8-B(핵심): 주문 응답 유실/UNKNOWN 시 "체결됐는데 Position 없는 무보호 창" 제거 —
#     executeBuy 가 주문 전 Trade(SUBMITTED, client_order_id) 선영속화, 루프 시작부
#     reconcileSubmittedOrders 틱 스윕이 cid 재조회로 정합화(체결 → DONE + Position(SL/TP)
#     생성, 취소 → CANCEL, 만료 2분 미발견 → FAILED, grace 10초 이내 in-flight 보호).
#     미해결 SUBMITTED 존재 시 신규 매수 차단. 게이트 = supportsClientOrderId()
#     (v2 또는 v1+clientOrderIdEnabled) — 기본 구성(V1+OFF)은 선영속화 꺼짐, 운영 동작 불변.
#     스키마: TradeStatus.SUBMITTED + Trade.clientOrderId + trading_trades.client_order_id
#     (nullable — dev file DB 하위호환). 선영속화 시 uuid 자리에 cid, 체결 확인 시 거래소
#     uuid 로 교체(assignExchangeUuid). cid 소유권은 파사드 → 서비스로 이동(오버로드).
#   · v2 잔여: 취소 DELETE /v2/order 라우팅(재조회 정규화 동일 패턴), 정규화 재조회
#     1회 → 최대 3회 선형 백오프(기본 300ms, 테스트 주입용 setter), §8-E 중복 cid 에러 =
#     "주문 존재 가능" → 재전송 금지·cid 재조회로 기존 주문 복구(이중 체결 방지).
#   · 결정 변경 2건 → ADR trading/infrastructure/0002(§8-B)·0003(v2 마이그레이션) 신규 +
#     trading CLAUDE.md·docs/adr/README.md 동기화(저장소 CLAUDE.md 규칙).
#   TDD: red-first(컴파일 실패 확인 후 구현) — 파사드·서비스·v2 어댑터 3사이클.
#   전체 스위트 BUILD SUCCESSFUL 확인됨(2026-07-08, 4m1s).
# 파일 소유:
#   [수정] src/main/.../trading/domain/trade/TradeStatus.java — SUBMITTED 추가
#   [수정] src/main/.../trading/domain/trade/Trade.java — clientOrderId·createSubmittedBuy·assignExchangeUuid
#   [수정] src/main/.../trading/infrastructure/persistence/entity/TradeJpaEntity.java — client_order_id 컬럼
#   [수정] src/main/.../trading/infrastructure/persistence/adapter/TradeRepositoryAdapter.java — 매핑
#   [수정·선재확인] src/main/.../trading/infrastructure/api/BithumbApiClient.java
#      (⚠ Section T/C66 소유 파일 — 이번 델타: cid 오버로드·supportsClientOrderId·
#       getOrderByClientOrderId 게이트·v2 취소 라우팅. `git diff` 로 확인)
#   [수정·선재확인] src/main/.../trading/infrastructure/api/BithumbV2OrderApi.java
#      (⚠ Section T/C66 소유 — 이번 델타: cancelOrder·requeryWithBackoff·§8-E 주석)
#   [수정·선재확인] src/main/.../trading/application/service/TradingBotService.java
#      (⚠ Section T/C66 소유 — 이번 델타: 선영속화·스윕·신규매수 차단·루프 스윕 훅)
#   [수정·선재확인] src/test/.../trading/infrastructure/api/BithumbApiClientIdempotencyTest.java
#      (⚠ Section T/C67 소유 — 이번 델타: cid 오버로드·게이트·취소 라우팅 테스트)
#   [수정·선재확인] src/test/.../trading/infrastructure/api/BithumbV2OrderApiTest.java
#      (⚠ Section T/C67 소유 — 이번 델타: 중복키·백오프·취소 테스트)
#   [신규] src/test/.../trading/application/service/TradingBotServiceOrderReconciliationTest.java
#   [신규] src/test/.../trading/application/service/TradingV2LostResponseSweepTest.java (E2E)
#   [신규] docs/adr/trading/infrastructure/0002-order-pre-persistence-and-tick-sweep.md
#   [신규] docs/adr/trading/infrastructure/0003-bithumb-v2-order-api-migration.md
#   [수정] docs/adr/README.md — 인덱스 행 2줄 + 폴더 카운트
#   [수정] src/main/.../trading/CLAUDE.md — §8-B·v2 항목 2건 추가
#   [수정] docs/trading/v2-migration-handoff.md — 진행현황 갱신

# Commit 68 — feat(trading): §8-B 주문 선영속화+틱 스윕+Position 생성 + v2 취소·재조회 백오프·중복키 복구
git add src/main/java/me/singingsandhill/calendar/trading/domain/trade/TradeStatus.java src/main/java/me/singingsandhill/calendar/trading/domain/trade/Trade.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/entity/TradeJpaEntity.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/adapter/TradeRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClient.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbV2OrderApi.java src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java
git commit -m "feat(trading): §8-B 주문 선영속화+틱 스윕+Position 생성 + v2 취소·재조회 백오프·중복키 복구" -m "§8-B: cid 부착 구성이면 executeBuy 가 주문 전 Trade(SUBMITTED, client_order_id) 선영속화, 응답 유실·UNKNOWN 시 SUBMITTED 유지 → 틱 스윕(reconcileSubmittedOrders)이 cid 재조회로 정합화(체결 → DONE+Position(SL·TP), 취소 → CANCEL, 만료 2분 미발견 → FAILED). 미해결 SUBMITTED 존재 시 신규 매수 차단. 기본 구성(V1+멱등 OFF)은 선영속화 꺼짐 — 운영 동작 불변. v2 잔여: DELETE ／v2／order 취소 라우팅, 정규화 재조회 3회 선형 백오프, §8-E 중복 client_order_id 에러는 재전송 금지·재조회로 기존 주문 복구. 근거: ADR trading／infrastructure／0002·0003."

# Commit 69 — test(trading): 선영속화 순서·스윕 상태 전이·진입 차단 + 응답유실→스윕 E2E + v2 취소·백오프·중복키
git add src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceOrderReconciliationTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingV2LostResponseSweepTest.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbApiClientIdempotencyTest.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/api/BithumbV2OrderApiTest.java
git commit -m "test(trading): 선영속화·틱 스윕·진입 차단 + 응답유실→스윕 E2E + v2 취소·백오프·중복키 테스트" -m "TradingBotServiceOrderReconciliationTest(주문 전 SUBMITTED 저장 순서, null·UNKNOWN 시 유지, 스윕 상태 전이, grace·만료, 미해결 시 매수 차단, 스윕 실패가 리스크 체크를 막지 않음), TradingV2LostResponseSweepTest(MockWebServer 응답유실 → 스윕이 체결 발견 → Position 생성 전 경로), 파사드 cid 오버로드·supportsClientOrderId·취소 라우팅, v2 중복키 재조회·백오프·취소."

# Commit 70 — docs: ADR trading/infrastructure 0002·0003 + CLAUDE.md·인계 문서 동기화 — 마지막 커밋(git-commit.md 포함)
git add docs/adr/trading/infrastructure/0002-order-pre-persistence-and-tick-sweep.md docs/adr/trading/infrastructure/0003-bithumb-v2-order-api-migration.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md docs/trading/v2-migration-handoff.md docs/guides/git-commit.md
git commit -m "docs(trading): ADR 0002(선영속화+스윕)·0003(v2 마이그레이션) + CLAUDE.md·인계 문서 동기화" -m "결정 변경 2건의 근거·대안·롤아웃 게이트 기록. trading CLAUDE.md 에 §8-B·v2 현재 사실 반영, 인계 문서 진행현황 갱신."

# =====================================================================
# 검증 (Section U — 커밋 전후 자동 점검)
# =====================================================================

# U-1) §8-B 선영속화·스윕·E2E (GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *TradingBotServiceOrderReconciliationTest --tests *TradingV2LostResponseSweepTest"

# U-2) 파사드·v2 어댑터 (cid 오버로드·라우팅·취소·백오프·중복키, GREEN 확인됨)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests *BithumbApiClientIdempotencyTest --tests *BithumbV2OrderApiTest --tests *BithumbApiClientModeTest"

# U-3) 전체 스위트 (BUILD SUCCESSFUL 4m1s 확인됨, 2026-07-08)
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"

# =====================================================================
# 배포 후 운영 검증 (Section U)
# =====================================================================

# U-4) 기본 구성(V1 + clientOrderIdEnabled=false)에서는 선영속화가 비활성 —
#   trading_trades 에 SUBMITTED 행이 새로 생기지 않아야 함(스키마 컬럼만 추가됨, nullable).
# U-5) clientOrderIdEnabled=true 또는 order-api-version=v2 전환은 소액 라이브 검증
#   (계획 문서 §5 Phase 0a/2/3) 후 — 전환 뒤 로그에서 ORDER_RECONCILE_* 이벤트·
#   ENTRY_BLOCKED_PENDING_ORDER 빈도 관찰(재조회 성공률 ≥95%, UNKNOWN 1건이면 원복).

# =====================================================================
# 카카오 로그인 + 연간 Recap (2026-07-11 설계 착수)
# =====================================================================

# Commit 71 — docs(datedate): 카카오 로그인+연간 recap 설계 스펙
git add docs/superpowers/specs/2026-07-11-kakao-login-recap-design.md docs/guides/git-commit.md
git commit -m "docs(datedate): 카카오 OAuth2 로그인+연간 recap 설계 스펙" -m "확정 결정: 오너 계정 연결+로그인 상태 참여·투표만 UserActivity 이벤트로 기록(소급 없음, 익명 병행), 연간 Wrapped 스타일 recap 상시 조회+공개 토큰 공유. Spring OAuth2 Client(client_secret_post)+커스텀 KakaoOAuth2UserService, 엔트리포인트 분리, first-claim 오너 연결 리스크 명시. 카카오 공식 문서 엔드포인트·scope 검증 포함."

# Commit 72 — feat(common): 카카오 OAuth2 클라이언트 의존성·등록
git add build.gradle src/main/resources/application.yaml .env.example src/main/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoOAuth2ClientConfig.java src/test/java/me/singingsandhill/calendar/common/infrastructure/config/KakaoClientRegistrationTest.java docs/guides/git-commit.md
git commit -m "feat(common): spring oauth2-client 의존성 + Kakao provider 등록" -m "공식 문서 엔드포인트(kauth authorize/token, kapi /v2/user/me), client_secret_post, scope profile_nickname·profile_image. 미설정 환경 부팅용 더미 기본값. KakaoClientRegistrationTest 회귀 가드. ClientRegistration 은 spring.security.oauth2.client.* 프로퍼티 대신 KakaoOAuth2ClientConfig 빈으로 직접 등록 — 프로퍼티 방식은 @WebMvcTest 슬라이스에 OAuth2ClientWebSecurityAutoConfiguration 을 끌어들여 HttpSecurity 부재로 컨텍스트 로드를 깨뜨리는 회귀를 유발함(ScheduleApiControllerTest 등)."

# Commit 73 — feat(datedate): AppUser 도메인·영속성·upsert 서비스
git add src/main/java/me/singingsandhill/calendar/datedate/domain/user/ src/main/java/me/singingsandhill/calendar/datedate/application/exception/UserNotFoundException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/AppUserService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/AppUserJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/AppUserJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/AppUserRepositoryAdapter.java src/test/java/me/singingsandhill/calendar/datedate/application/service/AppUserServiceTest.java docs/guides/git-commit.md
git commit -m "feat(datedate): 카카오 사용자 AppUser 도메인·JPA 영속성·upsert" -m "kakaoId unique, 재로그인 시 닉네임·프로필·lastLoginAt 갱신(Clock 주입 결정성). 헥사고날 domain POJO+port+adapter 패턴."

# Commit 74 — feat(security): 카카오 OAuth2 로그인 통합 + 진입점 분리 (ADR 0004)
git add src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/ src/main/java/me/singingsandhill/calendar/datedate/presentation/support/AuthenticatedUsers.java src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfileTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/DatedateAuthSecurityTest.java src/test/java/me/singingsandhill/calendar/trading/presentation/api/TradingApiSecurityTest.java src/test/java/me/singingsandhill/calendar/common/infrastructure/config/CorsConfigTest.java docs/guides/git-commit.md
git commit -m "feat(security): 카카오 OAuth2 로그인 + 사용자/어드민 진입점 분리" -m "KakaoOAuth2UserService 가 /v2/user/me 파싱→AppUser upsert→ROLE_USER 프린시펄(내부 userId attributes 탑재). /me·/recap/**(share 제외)·/api/me/** ROLE_USER, /recap/share/** 공개. 어드민 영역은 기존 어드민 로그인 진입점 유지, POST /logout 분리. 기존 P0-1 회귀 GREEN.

구현 중 API 조정 2건 (동작은 태스크 브리프 명세 그대로 유지, 원인은 Spring Security 7 DSL 실동작):
1) ExceptionHandlingConfigurer#authenticationEntryPoint(userEntryPoint) 를 defaultAuthenticationEntryPointFor(...) 호출들 뒤에 별도로 붙이면, 명시적 authenticationEntryPoint 필드가 우선시되어 앞서 등록한 admin 매핑 전체가 무시됨(getAuthenticationEntryPoint() 는 explicit 필드가 있으면 DelegatingAuthenticationEntryPoint 를 아예 만들지 않음) — 실제로 /runners/admin 미인증 접근까지 /login 으로 새는 회귀를 DatedateAuthSecurityTest 로 확인 후, userEntryPoint 를 AnyRequestMatcher.INSTANCE 매핑의 마지막 defaultAuthenticationEntryPointFor 항목으로 등록하도록 변경(등록 순서가 admin 매처 우선 평가를 보장).
2) DatedateAuthSecurityTest 의 redirectedUrlPattern(\"**/login\") 은 AntPathMatcher 의 pattern/path 선행 슬래시 일치 검사(doMatch 진입부) 때문에 실제 리다이렉트가 정확히 맞아도 항상 실패 — \"/**/login\", \"/**/runners/admin/login\" 으로 선행 슬래시를 붙여 수정(** 는 0개 세그먼트도 매치하므로 의도한 패턴 매칭 의미는 동일)."

# Commit 75 — feat(datedate): 로그인 페이지·헤더 카카오 로그인 UI·i18n
git add src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthController.java src/main/resources/templates/auth/login.html src/main/resources/templates/fragments/header.html src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/java/me/singingsandhill/calendar/datedate/domain/owner/ReservedOwnerIds.java src/main/resources/static/css/style.css src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AuthControllerTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/owner/ReservedOwnerIdsTest.java docs/guides/git-commit.md
git commit -m "feat(datedate): /login 페이지 + 헤더 카카오 로그인/프로필 UI + 예약어(me·recap·oauth2)" -m "카카오 버튼 공식 디자인(#FEE500), sec:authorize 로 ROLE_USER 분기(어드민 세션 미노출), POST /logout CSRF 폼. SEO noindex. ko/en 메시지 키.

구현 중 조정 2건 (브리프 명세 그대로 동작, 원인은 테스트 슬라이스 컨텍스트 실동작):
1) AuthControllerTest 의 @MockitoBean LocaleLinks 는 필드명만으로는 Thymeleaf SpEL @localeLinks 빈 참조 해석 시 NoSuchBeanDefinitionException 발생(WebMvcTest 슬라이스에서 실제 템플릿 렌더링을 거치는 첫 사례라 이전엔 드러나지 않음) — @MockitoBean(name = \"localeLinks\") 로 빈 이름을 명시해 해결.
2) 기존 ReservedOwnerIdsTest.reservedSetMatchesExpected() 의 containsExactlyInAnyOrder 목록에 신규 예약어(me·recap·oauth2)가 없어 Step 3 적용 시 드리프트 가드가 깨짐 — 목록에 3개 추가."

# Commit 76 — feat(datedate): 오너-카카오 계정 연결 (자동/수동, first-claim)
git add src/main/java/me/singingsandhill/calendar/datedate/domain/owner/ src/main/java/me/singingsandhill/calendar/datedate/application/exception/OwnerAlreadyLinkedException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/OwnerService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/OwnerJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/OwnerJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/OwnerRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/OwnerController.java src/main/resources/templates/owner/dashboard.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/test/java/me/singingsandhill/calendar/datedate/domain/owner/OwnerTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/OwnerServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/MeApiControllerTest.java docs/guides/git-commit.md
git commit -m "feat(datedate): 오너-카카오 계정 연결 — POST /start 자동 + 대시보드 수동 버튼" -m "Owner.userId nullable(first-claim, 타 유저 선점 409). 어댑터 save 는 기존 엔티티 로드 후 갱신(orphanRemoval 로 인한 일정 삭제 방지). findAllByUserId 포트 추가."

# Commit 77 — feat(datedate): UserActivity 활동 이벤트 기록 (로그인 세션 한정)
git add src/main/java/me/singingsandhill/calendar/datedate/domain/activity/ src/main/java/me/singingsandhill/calendar/datedate/application/service/UserActivityService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/UserActivityJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/UserActivityJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/UserActivityRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/presentation/api/ src/test/java/me/singingsandhill/calendar/datedate/application/service/UserActivityServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/api/ParticipantApiActivityRecordingTest.java docs/guides/git-commit.md
git commit -m "feat(datedate): 로그인 사용자 활동 이벤트(참여·투표·일정생성) append-only 기록" -m "(userId,type,targetId) 중복 방지, REQUIRES_NEW+예외 삼킴으로 본 동작 무영향, 익명 경로 무변경. recap 집계 원천 (ADR datedate/domain/0005)."

# Commit 78 — feat(datedate): 연간 recap 페이지·공유 토큰·마이페이지
git add src/main/java/me/singingsandhill/calendar/datedate/domain/recap/ src/main/java/me/singingsandhill/calendar/datedate/application/dto/RecapDto.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/InvalidRecapYearException.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/RecapShareNotFoundException.java src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapService.java src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapShareService.java src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/entity/RecapShareJpaEntity.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/RecapShareJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/RecapShareRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapController.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/MyPageController.java src/main/resources/templates/recap/ src/main/resources/templates/me/ src/main/resources/messages.properties src/main/resources/messages_en.properties src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapShareServiceTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapControllerTest.java docs/guides/git-commit.md
git commit -m "feat(datedate): 연간 Wrapped 스타일 recap + 공개 공유 토큰 + 마이페이지" -m "오너 계열(일정·연인원·요일·월·동행)+활동 계열(참여·선택일·투표 TOP3) on-the-fly 집계, Clock 고정 테스트. (userId,year) 멱등 공유 토큰, OG noindex. i18n {0,number,#} 연도 그룹화 차단. 공유 토큰 생성에도 연도 범위 검증(고아 토큰 차단).

구현 중 API 조정 2건 (동작은 태스크 브리프 명세 그대로 유지, 원인은 @WebMvcTest 슬라이스 실동작):
1) RecapController 는 Clock 을 생성자로 주입받는데, 운영 컨텍스트의 유일한 Clock 빈(StockSchedulerConfig#stockClock)은 @WebMvcTest(RecapController.class) 슬라이스가 명시적으로 @Import 하지 않는 @Configuration 이라 스캔되지 않아 NoSuchBeanDefinitionException 발생 — RecapControllerTest 안에 슬라이스 전용 @TestConfiguration(FixedClockConfig) 을 추가해 Clock.fixed(2026-07-11T03:00:00Z, Asia/Seoul) 하나만 공급(운영 Clock 빈과 별개, 프로덕션 코드에는 새 Clock 빈 추가 안 함).
2) recap.html/share.html 이 쓰는 ${@localeLinks.href(...)} 는 필드명만으로 선언한 @MockitoBean LocaleLinks 로는 SpEL 빈 이름 해석에 실패(Commit 75 AuthControllerTest 에서 이미 확인된 동일 이슈) — @MockitoBean(name = \"localeLinks\") 로 명시해 해결."

# Commit 79 — ✅ DONE() docs: ADR 0004(카카오 로그인)·0005(활동 이벤트 recap) + CLAUDE.md 동기화 — 마지막 커밋(git-commit.md 포함)
git add docs/adr/common/security/0004-kakao-oauth2-login.md docs/adr/datedate/domain/0005-user-activity-event-recap.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/superpowers/plans/2026-07-11-kakao-login-recap.md docs/guides/git-commit.md
git commit -m "docs: ADR 0004(카카오 OAuth2)·0005(활동 이벤트 recap) + CLAUDE.md 동기화" -m "결정 변경 2건 기록: ClientRegistration 빈 등록(프로퍼티 방식이 @WebMvcTest 슬라이스를 깨뜨림)·client_secret_post·진입점 분리(defaultAuthenticationEntryPointFor 마지막 catch-all)·역할 상호 배타 / append-only 이벤트(서비스 레이어 exists-check, DB unique 없음)·first-claim 동시성 race 수용·on-the-fly 집계. CLAUDE.md Security 표·모듈 섹션 갱신. 전체 테스트 스위트 BUILD SUCCESSFUL(410 tests, 0 failures)."

# Commit 80 — ✅ DONE() fix(datedate): 최종 리뷰 반영
git add src/main/java/me/singingsandhill/calendar/datedate/application/service/RecapService.java src/test/java/me/singingsandhill/calendar/datedate/application/service/RecapServiceTest.java src/main/resources/application.yaml src/main/resources/templates/owner/dashboard.html src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/MyPageControllerTest.java docs/guides/git-commit.md
git commit -m "fix(datedate): 최종 브랜치 리뷰 4건 — 미연결 오너 SCHEDULE_CREATED recap 합산, 세션쿠키 SameSite=Lax, 연결버튼 fetch 견고화, /me 렌더 테스트" -m "1) RecapService: 오너 계열 일정 id 집합에 없는 SCHEDULE_CREATED 활동(distinct scheduleId)을 schedulesCreated 에 추가 합산 — 미연결 오너 페이지에서 로그인 상태로 만든 일정이 recap 에서 누락되던 통합 간극 수정(ADR datedate/domain/0005). RecapServiceTest 에 오너 없음+SCHEDULE_CREATED 2종(동일 scheduleId 중복 이벤트 방어 포함) 케이스 추가, 기존 4개 테스트 무변경 통과.
2) application.yaml 최상위 server.servlet.session.cookie.same-site=lax 추가 — 교차 사이트 form POST 에 세션 쿠키 미전송(ADR 0004 /api/** CSRF 유예의 전제), 카카오 로그인 복귀(top-level GET)는 Lax 에서 정상 동작.
3) owner/dashboard.html linkOwnerBtn fetch 견고화: res.redirected 시 res.url 로 이동(세션 만료 시 로그인 페이지로), 기존 ok→reload·에러 body alert 유지, .catch 로 네트워크 실패 alert 추가. 마크업 무변경.
4) MyPageControllerTest 신설: @WebMvcTest(MyPageController.class) 슬라이스, ROLE_USER oauth2Login 으로 GET /me 실제 템플릿 렌더(mypage.html + header ROLE_USER 프로필 블록, 닉네임 '지수' 노출) 검증 + 익명 GET /me 는 /**/login 리다이렉트 검증. RecapControllerTest 의 슬라이스 전용 FixedClockConfig 패턴 재사용.
전체 테스트 스위트 BUILD SUCCESSFUL(413 tests, 0 failures, 0 errors — 기존 410 + 신규 3)."

# =====================================================================
# 수동 QA 체크리스트 (실 키 설정 후 — Task 8 Step 5, 미실행)
# =====================================================================
# 아래는 .env 에 실제 카카오 REST API 키를 설정한 뒤 사람이 직접 확인해야 하는
# 항목이다 (테스트 환경엔 더미 키만 있어 자동화 불가). bootRun 명령:
#   cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat bootRun"
# WSL 에서 앱은 Windows 프로세스로 뜨므로 확인은 `cmd.exe /c curl` 사용.
#
# 1. http://localhost:8081/ 헤더에 "카카오 로그인" 버튼 노출
# 2. 버튼 클릭 → kauth.kakao.com 동의 화면 → 동의 → /me 로 복귀, 닉네임 표시
# 3. 로그인 상태에서 POST /start 로 오너 생성 → /me 에 오너 목록 표시
# 4. 일정 생성 + 참여자 추가 + 장소/메뉴 투표 → /recap/2026 에 수치 반영
# 5. 공유 링크 생성 → 시크릿 창(비로그인)에서 공유 URL 열림 확인
# 6. 시크릿 창에서 /me → /login 리다이렉트 확인
# 7. 러너 어드민 로그인/로그아웃 기존 동작 확인
# 8. ?lang=en 으로 영어 recap 확인

# Commit 81 — ✅ DONE() docs(datedate): 카카오 로그인·recap 로컬 테스트/서버 반영 체크리스트
git add docs/datedate/kakao-login-recap-checklist.md docs/guides/git-commit.md
git commit -m "docs(datedate): 카카오 로그인+recap 로컬 QA·서버 배포 체크리스트" -m "실 키 필요 수동 작업만 분리 정리: 콘솔 준비(Redirect URI·Client Secret 활성화·동의항목), 로컬 QA 시나리오(왕복·오너연결·recap·무회귀), 서버 반영(nginx X-Forwarded-Proto→KOE006 방지, ddl-auto 첫 기동 스키마 확인, SameSite=Lax 스모크, 롤백 노트). git-commit.md 의 수동 QA 주석 블록을 문서로 승격."

# =====================================================================
# 플랜 문서 드리프트 최신화 (2026-07-13)
# =====================================================================

# Commit 82 — ✅ DONE() docs(datedate): 카카오 로그인·recap 플랜 문서를 출하 코드에 맞게 최신화 — 마지막 커밋(git-commit.md 포함)
git add docs/superpowers/plans/2026-07-11-kakao-login-recap.md docs/guides/git-commit.md
git commit -m "docs(datedate): 카카오 로그인+recap 플랜 문서 구현 반영 최신화" -m "ADR common/security/0004 에 기록된 결정 변경 2건을 플랜 본문에 반영해 플랜-코드 드리프트 해소: ① Task 1 Step 4 를 spring.security.oauth2.client.* 프로퍼티 방식에서 KakaoOAuth2ClientConfig 빈 등록 + 최상위 kakao.oauth2 자격증명 프로퍼티로 교체(프로퍼티 방식은 @WebMvcTest 슬라이스에 OAuth2ClientWebSecurityAutoConfiguration 을 끌어들여 컨텍스트 로드를 깨뜨림), ② Task 3 SecurityConfig 스니펫의 authenticationEntryPoint(userEntryPoint) 를 마지막 defaultAuthenticationEntryPointFor(userEntryPoint, AnyRequestMatcher.INSTANCE) catch-all 로 교체(+ AnyRequestMatcher import). Commit 72 스니펫 git add 에 KakaoOAuth2ClientConfig.java 추가, Files 목록 갱신, 문서 헤더에 구현 완료 노트(Task 1~8 완료, 413 tests GREEN, 잔여 작업은 실 키 수동 QA·배포 체크리스트) 추가. 코드 무변경 — 문서만."

# =====================================================================
# 실키 로컬 자동 검증 (2026-07-14)
# =====================================================================

# Commit 83 — ✅ DONE() docs(datedate): 실키 자동 검증 결과를 QA 체크리스트에 반영 — 마지막 커밋(git-commit.md 포함)
git add docs/datedate/kakao-login-recap-checklist.md docs/guides/git-commit.md
git commit -m "docs(datedate): 카카오 실키 로컬 검증 결과 체크리스트 반영" -m "실키 주입 후 검증 완료 항목 [x] 처리. ① curl 기반: 콘솔 준비 5종(인가 요청 KOE006 무발생 → Redirect URI 등록 유효, 토큰 프로브 오답 시크릿 KOE010 vs 실제 시크릿 KOE320 대조로 Client Secret 유효·활성 확정, scope 수락), .env 키·기동, 헤더 카카오 버튼 노출, 익명 플로우 API 스모크(POST /start CSRF 폼 → 일정 → 참여자 → 날짜 선택 → 장소 투표 → 뷰 렌더), 무세션 /me→/login 302, 어드민 진입점(/runners/admin·/trading → /runners/admin/login) 무회귀. ② 사용자 브라우저 QA: 카카오 동의 → /me 닉네임·프로필 이미지 표시, /recap/2026 빈 상태 렌더, 헤더 로그인/비로그인 전환. 검증 증거 요약 블록 추가. 데이터 필요 시나리오(오너 연결·recap 수치·공유 링크·en)와 동의 취소·딥링크 복귀·로그아웃 왕복만 미체크로 남김. 코드 무변경 — 문서만."

# =====================================================================
# 홈 카피 — 카카오 로그인·리캡 반영 (2026-07-14)
# =====================================================================

# Commit 84 — ✅ DONE() feat(datedate): 홈 FAQ·한계 카피에 카카오 로그인·연간 리캡 반영
git add src/main/resources/messages.properties src/main/resources/messages_en.properties
git commit -m "feat(datedate): 홈 FAQ·한계 카피에 카카오 로그인·연간 리캡 반영" -m "새 기능으로 낡아진 기존 카피만 갱신, 새 UI 요소·섹션 추가 없음(익명 우선 히어로 테제 유지 — 헤더 버튼·나의 리캡이 이미 노출 역할). ① index.faq1.a: '가입 없이' 유지 + 카카오 로그인은 선택 사항이며 페이지 계정 연결·연간 리캡 제공 한 문장 추가(헤더 로그인 버튼과의 표면 모순 해소, 리캡의 유일한 홈 노출 지점), ② index.faq4.a: 계정 연결 시 ID 잊어도 마이페이지에서 찾기 한 문장 추가(익명 오너의 ID 분실 페인포인트), ③ index.why.limits.desc: '계정·비밀번호가 없어' → '참여에는 계정이 필요 없어'로 사실 오류 수정(참여자 무계정 개방성은 불변, 오너 계정은 이제 존재). ko/en 동시 갱신, ko FAQ 라인은 기존 native2ascii 스타일 유지. 검증: MessageCatalogParityTest GREEN, ko/en 홈 렌더에서 신규 문구 3곳 확인, 인자 없는 메시지 '' 미사용."

# Commit 85 — ⛔ 실행 금지 (미실행 상태에서 Commit 89로 대체됨) — feat(datedate): 헤더 카카오 로그인 버튼을 공식 이미지로 교체
# ⛔ 이 커밋은 실행하지 마세요: PNG 접근이 커밋되기 전에 홈 개선 배치(86~93)의 Commit 89(CSS 커스텀 버튼)로
#    대체되었고 kakao_login_small.png 는 삭제되어 아래 git add 가 pathspec 오류로 실패합니다.
#    header.html/style.css 의 변경분은 Commit 89 가 흡수합니다.
# git add src/main/resources/static/image/kakao_login_small.png src/main/resources/templates/fragments/header.html src/main/resources/static/css/style.css docs/guides/git-commit.md
# git commit -m "feat(datedate): 헤더 카카오 로그인 버튼을 공식 로그인 버튼 이미지로 교체" -m "카카오 디자인 가이드 공식 에셋 kakao_login_small.png(60x30) 적용. header·header-minimal 양쪽 fragment 의 텍스트 필 버튼(#FEE500 배경 CSS)을 img 버튼으로 교체 — nav-link(-animated) 클래스 제거(텍스트 링크 패딩·호버 불필요), .nav-kakao-login 은 정렬·호버 opacity 전용으로 재정의(배경·패딩은 이미지에 내장). th:alt=#{nav.login.kakao} 로 alt 로컬라이즈(ko '카카오 로그인' / en 'Sign in with Kakao'), width/height 명시로 CLS 방지. /login 페이지의 큰 CTA(심볼+레이블 커스텀 구현)는 소형 이미지 확대 시 품질 저하라 유지. 검증: 홈(minimal)·가이드(기본)·/login 헤더 렌더에서 이미지 버튼 확인(리소스 체인 콘텐츠 해시 URL 200), en alt 확인, AuthControllerTest·MyPageControllerTest GREEN."

# =====================================================================
# 홈 화면 디자인/UX 개선 배치 (2026-07-16)
# =====================================================================
# 실행 노트:
# - 이 배치(86~93)는 Commit 84/85 가 미커밋된 워킹트리 위에서 작성됨.
# - Commit 85 는 실행 금지(위 ⛔ 주석) — PNG 접근이 89 로 대체, PNG 파일 삭제됨.
# - Commit 84 를 원형대로 분리하려면 `git add -p` 로 faq1.a·faq4.a·why.limits.desc
#   훅만 선택해 먼저 커밋하세요. 그냥 순서대로 실행하면 파일 단위 git add 특성상
#   각 파일의 누적 변경이 그 파일을 처음 스테이징하는 커밋에 합류합니다
#   (내용은 모두 보존, 커밋 귀속만 합쳐짐 — 기존 배치 75/76/78 과 동일한 관례).

# Commit 86 — ✅ DONE() fix(i18n): 홈 메시지 오탈자 수정 (격치는→겹치는, 장소실메뉴→장소·메뉴)

# Commit 87 — ✅ DONE() feat(datedate): 히어로 카피 개선 + 한글 타이포그래피
git add src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css
git commit -m "feat(datedate): 히어로 카피 개선 + 한글 타이포그래피 (keep-all·자간·키워드 크기)" -m "① 카피: index.hero.subtitle '여러명이서 쉽게 날짜 조율하기'(비표준 표현) → '모두의 되는 날을 한눈에' (en 'Everyone's free days, at a glance'). seo.home.title 꼬리·ogTitle·orgDescription 의 동일 문구도 정합 갱신 — SeoServiceI18nTest 고정점('약속 잡기' KO title, 'Group Scheduling' EN, appDescription '링크 하나로 날짜')은 불변 유지. hero keyword('그룹 약속 조율 서비스')·description 은 유지(문제는 크기였음). ② 타이포: .hero-fullscreen h1 letter-spacing -0.02em→0 (한글 음수 자간 금지), 라틴 브랜드 전용 .hero-brand 에 -0.02em 이동. .hero-keyword font-size 0.38em(모바일 ~12px)→clamp(1.05rem,2.5vw,1.5rem) em 종속 해제, letter-spacing 0.06em→0.02em. .hero-fullscreen .subtitle line-height 1.6→1.7. ③ body 에 word-break:keep-all + overflow-wrap:break-word 전역 적용 — 한글 단어 중간 줄바꿈 방지(기존에는 .toast 1곳만 keep-all), keep-all 은 CJK 전용이라 EN 무영향, overflow-wrap 이 긴 URL/ID 방어. style.css 는 stock/trading 도 공유 — 표 셀 육안 스팟체크 완료 예정. 인코딩: ko index.* 라인 이스케이프 스타일 보존, seo.home.* raw UTF-8 보존, en 인자 없는 키라 어퍼스트로피 '' 이스케이프 불필요( ' 단일 사용). 검증: MessageCatalogParityTest·SeoServiceI18nTest GREEN."

# Commit 88 — ✅ DONE() feat(datedate): 시작 폼 CTA 위계 + 랜덤 생성 어포던스
git add src/main/resources/templates/index.html src/main/resources/static/css/style.css
git commit -m "feat(datedate): 시작 폼 CTA 위계 정리 + 랜덤 생성 버튼 어포던스" -m "① 랜덤 생성: 플레인 텍스트(.btn-text — 버튼 인지 불가)를 신규 .btn-outline(1px 보더·radius 8px·min-height 44px)으로 교체 + 인라인 SVG 주사위 아이콘(aria-hidden). .btn-text 는 .popular-more·use-cases 가 공유하므로 재정의 대신 새 클래스 신설. ② 시작하기(주 CTA): .form-actions-minimal 스코프로 font-weight 600 + padding 확대, 활성 상태에 primary 그림자 부여로 disabled(회색)→활성 전환을 시각화. disabled 시맨틱스·검증 JS(refresh 의 disabled/aria-disabled 토글)는 불변, aria-describedby=ownerIdHelper 추가로 비활성 사유(입력 규칙 헬퍼)를 스크린리더에 연결. ③ 랜덤 생성 피드백: @keyframes inputFlash(primary-light 배경 0.6s) + generateRandomId 에서 리플로우 재트리거·animationend 정리·input.focus() — prefers-reduced-motion 은 기존 전역 오버라이드가 자동 무력화. ④ 모바일(≤480px): .btn-outline 풀폭 추가(.btn-spring 은 기존 풀폭, column-reverse 로 시작하기가 상단). 터치 타겟 두 버튼 모두 44px 이상, 입력 폰트 16px(iOS 자동 줌 경계) 유지. 검증: bootRun 홈에서 아이콘·플래시·활성 전환·모바일 스택 육안 확인."

# Commit 89 — ✅ DONE() feat(datedate): 카카오 로그인 CSS 커스텀 버튼 + 모바일 메뉴 인증 그룹
git add src/main/resources/templates/fragments/header.html src/main/resources/templates/auth/login.html src/main/resources/static/css/style.css src/main/resources/static/js/navbar.js src/main/resources/messages_en.properties
git commit -m "feat(datedate): 헤더 카카오 로그인을 CSS 커스텀 버튼으로 + 모바일 메뉴 인증 그룹화" -m "미커밋 상태였던 이미지 버튼 접근(구 Commit 85, 실행 전 폐기)을 대체: 60x30 PNG 는 모바일 풀폭 배치 시 래스터 열화·라벨 i18n 이 alt 의존이라 한계. /login 페이지에 이미 있던 가이드 준수 구현(컨테이너 #FEE500·심볼 SVG fill-opacity 0.9·레이블 85%)을 공용 .btn-kakao 로 추출. ① header·header-minimal 양쪽: 카카오 앵커를 심볼 SVG(18px)+메시지 키 레이블로 교체, 비로그인 버튼+로그인 프로필/리캡/로그아웃을 .nav-auth 로 래핑(데스크톱 display:contents 로 기존 flex row 불변). ② 모바일(≤768px): .nav-menu .nav-auth 를 메뉴 하단 구분선+풀폭 블록으로 — 카카오 버튼 44px 풀폭, 로그아웃 버튼 좌정렬 풀폭(기본·미니멀 네비 공통, 둘 다 .nav-menu 클래스라 1곳 선언으로 커버). ③ .btn-kakao 상태: hover brightness(.96)/active(.92), 데스크톱 36px/라운드 6px, 로그인 페이지는 --lg(풀폭·라운드 12px) modifier 로 인라인 스타일 통합. ④ navbar.js close-on-click 셀렉터 .nav-link → a, button[type=submit] 광역화 — minimal 헤더(.nav-link-animated)·카카오 앵커가 같은 페이지 앵커(/#start-form) 이동 시 메뉴+스크롤락을 남기던 잠복 버그 해소. ⑤ en nav.login.kakao 를 가이드 공식 표기 'Login with Kakao' 로. kakao_login_small.png 는 미추적 상태로 삭제(히스토리 무영향). 검증: AuthControllerTest·MyPageControllerTest GREEN, 홈·가이드·/login 헤더 렌더 + 모바일 햄버거 육안 확인."

# Commit 90 — ✅ DONE() feat(datedate): 동호회 시나리오 카드 + 명시적 그리드 + HomeControllerTest
git add src/main/resources/templates/index.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeControllerTest.java
git commit -m "feat(datedate): 홈 시나리오 그리드에 동호회 카드 추가 + 5카드 명시적 브레이크포인트" -m "UseCaseSlugs.ALL 5개 슬러그 중 club-activity 만 홈 그리드에 없던 비대칭 해소. 기존 768px 블록의 레거시 .scenarios-grid 1fr 룰 제거(641~768px 에서 2열을 죽이던 간섭 — CDP 실측 1280→3열/900·768·700→2열/600·375→1열 확인). ① 5번째 .scenario-card(🎾, index.scenario.club.*, 예시 datedate.site/tennis-club = seo.useCase.club-activity.exampleId, 링크 /use-cases/club-activity) — 기존 카드와 동일 구조·animate-on-scroll. ko 신규 3키는 인접 라인과 같은 native2ascii 이스케이프, en 동시 추가(parity GREEN). ② .scenarios-grid: auto-fit minmax(320px,1fr) → 명시적 repeat(3,1fr) / ≤992px 2열 / ≤640px 1열 — 5개일 때 데스크톱 3+2, 태블릿 2+2+1 로 어정쩡한 마지막 행 제거. ③ HomeControllerTest 신설(@WebMvcTest 슬라이스, MyPageControllerTest 패턴): 홈 200+index 뷰, 시나리오 그리드가 UseCaseSlugs.ALL 전 슬러그 카드를 노출하는 동기화 가드(푸터도 전 슬러그 링크를 렌더하므로 scenarios-grid 섹션 범위로 한정 검사 — 누락 재발 방지 핵심), popular 빈 리스트 시 섹션 미노출, 항목 존재 시 이름 렌더. 검증: HomeControllerTest·MessageCatalogParityTest GREEN."

# Commit 91 — ✅ DONE() feat(datedate): 인기 순위 노출 기준 (최소 2표 + 비속어 블록리스트) + ADR 0006
git add src/main/java/me/singingsandhill/calendar/datedate/application/service/PopularityService.java src/test/java/me/singingsandhill/calendar/datedate/application/service/PopularityServiceTest.java docs/adr/datedate/domain/0006-popularity-exposure-criteria.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md
git commit -m "feat(datedate): 인기 순위 노출 기준 도입 — 최소 2표 + 비속어 블록리스트 (ADR 0006)" -m "홈 첫 화면 '지금 인기있는 선택'에 0표·비속어 입력 원문('ㅈㄴ맛있는거' 등)이 그대로 노출되던 문제. 원인: 점수식 voteCount+recency(0~5) 에서 초기 데이터 규모면 0표 항목이 recency 보너스만으로 랭킹 진입. ① 집계 합산 후 totalVotes>=2 필터 (여러 명이 동의한 항목만 — 동명 1표+1표는 합산 2표로 통과, >=1 은 셀프투표 1회로 뚫려 기각), ② 명백한 비속어 토큰 블록리스트(소문자화+공백 제거 containment, best-effort — 자모 변형 우회 수용), ③ 서비스 레이어 전역 적용(홈·/insights/trends·InsightsService top 동일 기준, 세 소비처 모두 빈 결과 안전: th:if/empty-state/null-top). TDD: PopularityServiceTest 7케이스 선작성(RED 4) → 구현(GREEN) — 0·1표 제외, 2표 포함, 집계 합산 임계 통과, 블록리스트 공백 우회, 전량 미달 빈 리스트, 필터 후 정렬·limit 회귀, 메뉴 URL 병합 회귀. 정책 변경이므로 CLAUDE.md 동기화 규칙에 따라 ADR datedate/domain/0006 신규(Accepted) + README 두 뷰 갱신(총 55) + 루트/application CLAUDE.md 의 PopularityService 설명 갱신."

# Commit 92 — ✅ DONE() feat(datedate): 통계 스트립 천단위 구분
git add src/main/resources/templates/index.html src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeControllerTest.java
git commit -m "feat(datedate): 홈 통계 스트립 숫자 천단위 구분자 적용" -m "stats-strip 3개 수치(생성된 일정·참여자·투표)를 raw th:text 에서 #numbers.formatInteger(v, 1, 'COMMA') 로 — repo 선례 stock/dashboard.html 과 동일 관용구. i18n 의 {n,number,#} 규칙은 연도 그룹화 방지용이며 통계 수치는 그룹화 대상이므로 무관(SeoServiceI18nTest yearNotGrouped 영향 없음, MessageFormat 미경유). 카운트업 애니메이션은 기각 — animate-on-scroll 인프라가 opacity/transform 전용이라 신규 JS 필요, reduced-motion 대응·유지 비용 대비 체감 이득 없음. HomeControllerTest 에 12,345 / 9,012 렌더 어서션 추가."

# Commit 93 — ✅ DONE() style(datedate): 홈 시각 일관성 + 접근성 폴리시 — 마지막 커밋(git-commit.md 포함)
git add src/main/resources/static/css/style.css docs/guides/git-commit.md
git commit -m "style(datedate): 홈 카드 hover·섹션 리듬 통일 + 접근성 점검" -m "① .scenario-card 를 .feature-card 와 동일한 엘리베이션 문법으로 통일: box-shadow var(--shadow) + hover translateY(-4px)/var(--shadow-lg) + transition (기존에는 홈 카드 중 유일하게 그림자·hover 없음). ② 홈 섹션 수직 리듬 단일 규칙 4rem/3rem/2rem (데스크톱/≤768/≤480): popular-section 3rem→4rem, stats-strip 2rem→4rem 으로 승격 후 768px 그룹에 두 섹션 추가, 480px 그룹에 stats-strip 추가(popular 는 기존 자체 2rem 룰 유지) — 다섯 섹션(stats·popular·features·scenarios·faq) 클래스는 index.html 전용임을 grep 으로 확인, 타 페이지 무영향. ③ 모바일 메뉴 마지막 링크 border-bottom 제거(a:has(+ .nav-auth)) — nav-auth border-top 과의 이중 헤어라인 방지. ④ 접근성 점검(변경 없음, 확인만): --text-light(#666) on #f5f5f5 대비 ≈5.3:1 AA 통과, 신규 .btn-outline(button)·.btn-kakao(a) 는 기존 전역 button/a:focus-visible 아웃라인 룰에 자동 포함, inputFlash·카드 리프트는 기존 prefers-reduced-motion 전역 오버라이드가 무력화. --text-muted(#888, AA 미달)는 홈 미사용 — 후속 과제로만 기록. 검증: 전체 테스트 스위트 GREEN + bootRun 1280/375px 육안."

# =====================================================================
# 로그인 배너 공유 fragment + 로그인 후 사용법 (2026-07-17)
# =====================================================================

# Commit 94 — ✅ DONE() feat(datedate): 로그인 배너 공유 fragment 추출 + 로그인 후 사용법 3단계 (홈·가이드)
git add src/main/resources/templates/fragments/login-banner.html src/main/resources/templates/index.html src/main/resources/templates/guide.html src/main/resources/static/css/style.css src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/templates/fragments/CLAUDE.md docs/guides/git-commit.md
git commit -m "feat(datedate): 로그인 배너 공유 fragment + 로그인 후 사용법 3단계 (홈·가이드)" -m "홈 전용이던 카카오 로그인 가치 제안 배너를 fragments/login-banner.html 공유 fragment 로 추출하고, 배너 안에 로그인 후 사용법 3단계 번호 목록(① 카카오로 로그인 ② 홈에서 ID 로 페이지 만들기 — 자동 계정 연결 ③ 마이페이지에서 페이지·연간 리캡 확인)을 추가. /guide 에는 5단계 사용법 직후·광고 슬롯 직전에 배치(선택: 계정 연결 안내 위치). 키는 공유에 맞게 index.login.* → loginBanner.*(title/desc/step1~3, ko native2ascii/en) 개명 — 구 키는 미커밋 상태였어 안전. .login-banner-steps CSS(0.85rem, muted, 실제 순서라 ol). fragments/CLAUDE.md 표에 행 추가. 비로그인 한정(sec:authorize)·로그인 상태 UI 무변경. 검증: 홈·가이드 ko/en 렌더에서 배너+3단계 확인, MessageCatalogParityTest·HomeControllerTest GREEN. 주의: 84~93 커밋 실행 순서에 따라 파일 단위 스테이징으로 일부 변경이 앞 커밋에 동반될 수 있음 — 본 커밋은 잔여분을 수거."

# =====================================================================
# datedate SEO·GA4(GTM) 태그 점검 — 메타 디스크립션 보강 + GTM noscript 프래그먼트화 (2026-07-18)
# =====================================================================
# 배경: Bing 웹마스터 도구가 /, /guide, /use-cases/club-activity 3개 페이지에
# "메타 디스크립션 너무 짧음"(권장 150~160자) 권고 발행 → 점검 결과 색인 대상
# 13개 페이지 전반이 동일 문제(ko 56~99자/en 81~127자)라 전체 보강.
# 점검에서 문제없음 확인: og-image.png 1490×780 = head.html 하드코딩 값 일치,
# schedule_created 2곳 정의는 상이한 진입 경로(생성 페이지 vs 모달)라 이중 발화 아님.

# Commit 95 — ✅ DONE() fix(seo): 색인 대상 13개 페이지 메타 디스크립션 길이 보강 (ko 140~160자 / en 150~160자)
git add src/main/resources/messages.properties src/main/resources/messages_en.properties src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java
git commit -m "fix(seo): 색인 대상 13개 페이지 메타 디스크립션 길이 보강 (Bing 권고 대응)" -m "Bing 웹마스터 '너무 짧음' 권고(/, /guide, /use-cases/club-activity) 대응 — 색인 대상 전 페이지로 확대 적용. ① seo.{home,guide,insights,about,faq,privacy,terms,dateDiff}.description + seo.useCase.{5슬러그}.description 13키 × ko/en 26개 문자열 재작성: ko 140~160자(핵심 메시지를 앞 70~80자에 front-load — 한글 SERP 잘림 대비), en 150~160자. 역초과분(club-activity en 165자, about en 167자)은 160자 이하로 손질. 페이지별 고유 키워드(테니스·등산·회차별 참석 / 휴가·겹치는 날 등) 유지. ② SeoServiceI18nTest 에 indexablePages_descriptionLengthInRange 신설: 8개 정적 빌더 + UseCaseSlugs.ALL 순회, 양 로케일 description 120~160자 가드(하한을 목표보다 느슨하게 잡아 카피 미세수정 취약성 방지). 인코딩: ko seo.* 라인 raw UTF-8 보존, 인자 없는 키라 어퍼스트로피 ' 단일 사용(who's 등). JSON-LD 는 별도 키(seo.home.appDescription)라 기존 고정점 불변. 검증: SeoServiceI18nTest·전체 스위트 GREEN, bootRun 에서 6개 URL(3페이지 × ko/en) 렌더 길이 140~158자 확인. 주의: 87·90·94 가 messages 양 파일을 선스테이징하므로 본 커밋은 잔여분(디스크립션 26라인)을 수거."

# Commit 96 — ✅ DONE() refactor(datedate): GTM noscript 공용 프래그먼트화 + 누락 4페이지 보완 — 마지막 커밋(git-commit.md 포함)
git add src/main/resources/templates/fragments/gtm-noscript.html src/main/resources/templates/index.html src/main/resources/templates/guide.html src/main/resources/templates/about.html src/main/resources/templates/faq.html src/main/resources/templates/privacy.html src/main/resources/templates/terms.html src/main/resources/templates/tools/date-diff.html src/main/resources/templates/use-cases/detail.html src/main/resources/templates/insights/trends.html src/main/resources/templates/owner/dashboard.html src/main/resources/templates/schedule/create.html src/main/resources/templates/schedule/view.html src/main/resources/templates/auth/login.html src/main/resources/templates/me/mypage.html src/main/resources/templates/recap/recap.html src/main/resources/templates/recap/share.html src/main/resources/templates/runners/fragments/header.html src/main/resources/templates/fragments/CLAUDE.md CLAUDE.md docs/guides/git-commit.md
git commit -m "refactor(datedate): GTM noscript iframe 을 공용 프래그먼트로 통일 + 누락 4페이지 보완" -m "GTM noscript iframe 이 12개 템플릿에 수기 복붙(ID GTM-PFPKQT7W 하드코딩 14곳)이고 login·mypage·recap·recap-share 4개 datedate 페이지엔 누락(GTM JS 는 head 경유 로드 중)이던 비대칭 해소. ① fragments/gtm-noscript.html 신설(ad-slot.html 구조 관례: 사용법 주석 + head.html JS 로더와 ID 일치 유지 명시). ② 기존 12개 템플릿의 수기 블록을 <div th:replace> include 로 교체 + 누락 4개 템플릿 <body> 직후 추가 — datedate 16개 페이지 전부 통일. ③ runners/fragments/header.html 의 미참조 head(seo) 프래그먼트 내 죽은 GTM 스니펫 삭제(어느 템플릿도 참조 안 함을 grep 확인 — 드리프트 위험 제거, 프래그먼트 전체 정리는 runners 과제로 보류). 결과: ID 하드코딩 14곳→2곳(head.html + gtm-noscript.html). ④ fragments/CLAUDE.md 표·Usage Pattern, 루트 CLAUDE.md fragments 행 갱신(단순 사실 변경 — ADR 불필요). 보고만(변경 없음): 쿠키 동의 배너/Consent Mode 부재는 제품·법무 판단 필요한 별도 과제, 에러 페이지 GTM 미로드는 noindex 의도적 우회 유지, runners 페이지 noscript 누락은 타 도메인 과제. 검증: 전체 스위트 GREEN, bootRun 에서 교체 5페이지 + 신규 /login 의 ns.html iframe 렌더 확인. 주의: 88·90·92·94 가 index/guide/login 등을 선스테이징하므로 본 커밋은 잔여분을 수거."

# =====================================================================
# stock 봇 4거래일 연속 Selected:0 진단·수정 — 유니버스 rank 재시도 + 계측 + 드리프트 정리 (2026-07-24)
# =====================================================================
# 배경: 2026-07-20~23 스크리닝 메일 첨부 로그 분석. ① 08:30 pre-market 의 거래량순위가
# 매일 0건(장 시작 전이라 당일 거래량 부재) → rank=0 폴백 대형주 70종목으로만 운영,
# ② 체결강도(cttr)=0 대량 발생(07-22 엔 갭 통과 63종목 전탈락) 원인 미확정,
# ③ 청산시각 드리프트(cron 11:20 vs yaml final-exit-time 15:20 — 강제청산 후 재진입 창).
# 부수 발견: 첨부 로그가 09:20 시점 스냅샷인데 파일명이 logback 롤 파일과 동일해
# "하루 전체 로그" 로 오인(진단 초기 '앱이 09:20 에 멈춘다' 오판 유발), 크립토 모듈
# 로그가 root appender 로 stock-trading.log 를 하루 ~6천 줄 오염.

# Commit 97 — ✅ DONE() feat(stock): 유니버스 거래량순위 스크리닝 시점 재시도 + cttr/rank 계측 + 청산시각 드리프트 정리 (ADR 0006)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/main/resources/application.yaml src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java docs/adr/stock/algorithm/0006-universe-rank-retry-at-screening.md docs/adr/stock/algorithm/0005-dynamic-universe-volume-rank.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/application/CLAUDE.md
git commit -m "feat(stock): 거래량순위 스크리닝 시점 재시도 — 08:30 rank 0건으로 동적 유니버스 무력화 해소 (ADR 0006)" -m "운영 로그(07-20~23) 4거래일 연속 'Volume-rank returned 0 codes'(08:30, rt_cd=0 성공+0건) → rank=0 폴백 대형주 70종목 → Selected:0. 장 시작 전엔 당일 거래량이 없어 ADR-0005 의 pre-market 1회 호출이 구조적으로 빈손. ① UniverseBuilder.refreshIfDegraded 신설: 09:20 스크리닝 시점에 스냅샷이 rank=0 폴백 전용이면 1회 재조회(rank 성공 스냅샷은 유지 — ADR-0002 거래일 1회 정합성, 재시도도 0건이면 기존 폴백 무회귀), executeScreeningLoop 이 currentUniverse 대신 호출. TDD: 재시도/무회귀/스냅샷유지/비활성/미존재 5케이스 RED 선확인 후 GREEN. ② 계측: getTopVolumeCodes 성공+0건 시 rows 수·첫 행 키 WARN(빈 응답 vs 필드명 불일치 판별), mapToQuoteResponse cttr=0 시 원시 cttr/acml_vol/stck_prpr WARN(07-22 갭 통과 63종목 전탈락 원인 추적 — skip-zero-strength 는 계측 결과 확인까지 현행 유지). ③ exit.final-exit-time 15:20→11:20(청산 cron 과 일치): 11:20 강제청산 후에도 루프 가드가 15:20 까지 열려 재진입 가능하던 드리프트 해소, trading-end 11:30 은 FINAL_EXIT 페이즈 표시 창으로 유효해 유지. 결정 변경이므로 ADR stock/algorithm/0006 신규(Accepted) + 0005 상태 보완 표기 + README 갱신 + stock/application CLAUDE.md 동기화. 검증: UniverseBuilderTest 12케이스·전체 스위트 GREEN."

# Commit 98 — ✅ DONE() feat(stock): 스크리닝 메일 첨부 스냅샷 명시 + logback 크립토/주식 로그 분리 — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/StockMailService.java src/test/java/me/singingsandhill/calendar/stock/application/StockMailServiceTest.java src/main/resources/logback-spring.xml CLAUDE.md docs/guides/git-commit.md
git commit -m "feat(stock): 메일 첨부를 스냅샷 명명으로 + 크립토 로그 stock-trading.log 오염 분리" -m "① 첨부명 stock-trading-YYYY-MM-DD.log → stock-screening-snapshot-YYYY-MM-DD.log: logback 롤오버 파일명과 동일해 '하루 전체 로그'로 오인되던 사고 방지(실제로는 발송 시점 09:20 까지 스냅샷 — 이번 진단에서 '앱이 09:20 에 멈춘다' 오판의 원인), 본문에 스냅샷 안내 문구 추가. TDD: 실제 MimeMessage 조립 후 멀티파트 순회로 첨부명·본문 어서션 RED 선확인 → GREEN. ② logback: me.singingsandhill.calendar.trading 로거를 CRYPTO_FILE(crypto-trading.log, KST 자정 회전 30일/1GB — STOCK_FILE 과 동일 정책) + CONSOLE 로 additivity=false 분리 — root 경유로 stock-trading.log 에 하루 ~6천 줄(Bithumb 야간 연결 오류 등) 섞이던 것 차단. 루트 CLAUDE.md 관측성 항목 동기화(단순 사실 변경 — ADR 불필요). 검증: 테스트 JVM 런타임에서 crypto-trading.log 생성 + m.s.c.t.* 라인 라우팅 확인, 당일 stock-trading.log 에 크립토 라인 0건, 전체 스위트 GREEN. 별도 과제로 기록만: Bithumb 야간 connection reset 반복(GCP 해외 IP 추정)·포지션 347 리스크체크 가격조회 실패, cttr=0 근본 원인은 97 계측 로그 확인 후 결정."

# =====================================================================
# stock 봇 P0 안전화 — 리뷰 보고서(2026-07-24) 치명 결함 5건 수정
# =====================================================================
# 배경: docs/audit/stock-trading-logic-review-2026-07-24.md §9 우선순위 P0 실행.
# 전 항목 TDD (RED 선확인 → GREEN), 전체 스위트 GREEN.

# Commit 100 — ✅ DONE() fix(stock): 상태머신 영속화 복원 + TP1 수량 캡 + 주문 무재시도 (P0-3/5/4)
git add src/main/java/me/singingsandhill/calendar/stock/domain/stock/Stock.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/persistence/adapter/StockRepositoryAdapter.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/persistence/StockRepositoryAdapterTest.java src/main/java/me/singingsandhill/calendar/stock/domain/position/StockPosition.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java src/test/java/me/singingsandhill/calendar/stock/domain/StockPositionTakeProfitTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockPositionServiceTest.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClientOrderRetryTest.java docs/adr/stock/infrastructure/0005-non-idempotent-order-no-retry.md
git commit -m "fix(stock): 상태머신 영속화 복원 + TP1 수량 캡 + 비멱등 주문 무재시도 (리뷰 P0-3/5/4)" -m "① P0-3: toDomain 이 상태머신 작업 필드 5개(highAfterOpen/highFormedAt/pullbackLow/pullbackStartAt/entryPrice)를 미복원 — 5초 틱 재로딩마다 유실되어 calculateDropFromHigh 가 null→0% 를 조용히 반환, HIGH_FORMED 에서 영구 정체(봇이 한 번도 못 산 3번째 독립 원인). Stock.restorePersistedState 신설(전이 메서드와 달리 상태·시각 미변경) + 어댑터 toDomain 에서 호출. 라운드트립 테스트 RED(highAfterOpen null) 선확인. ② P0-5: calculateTp1Quantity 를 (ratio 파라미터, min(entry×ratio, remaining) 캡)으로 — TP2(전고점 회복)가 TP1 보다 선발동하는 통상 경로에서 잔여 초과 매도 차단, 하드코딩 0.5 대신 exit.tp1-ratio 사용, doExecutePartialExit 은 비가역인 매도 주문 *전*에 수량 검증(초과/0 이하 요청은 주문 미발행 — 검증 테스트 2건, 도메인 테스트 3건). ③ P0-4: 주문 POST(/order-cash)가 조회와 같은 Retry.backoff(3,1s)를 타서 응답 유실 시 동일 시장가 주문 중복 전송 위험 — executePostNoRetry 로 전환(Semaphore·타임아웃 유지, retryWhen 제거), MockWebServer 테스트로 주문 5xx→요청 1회 / 시세 5xx→재시도 2회 검증(RED: 주문 2회 전송 확인 후 GREEN). ADR stock/infrastructure/0005 신설."

# Commit 101 — ✅ DONE() feat(stock): PAPER 기본 모드 — LIVE 는 STOCK_BOT_MODE=LIVE opt-in (P0-2, ADR stock/modes/0002)
git add src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java src/main/resources/application.yaml src/test/java/me/singingsandhill/calendar/stock/infrastructure/config/StockPropertiesModeTest.java docs/adr/stock/modes/0002-paper-default-mode.md
git commit -m "feat(stock): 봇 기본 모드 PAPER 전환 — 실주문은 STOCK_BOT_MODE=LIVE 명시적 opt-in (ADR stock/modes/0002)" -m "기본값 LIVE 는 설정 누락만으로 실계좌 주문이 나가는 방향 — 전략이 백테스트·실거래 0회 미검증이고 청산 체인 결함(TP3 도달불가·트레일링 미작동)이 있는 상태라 위험. 크립토 선례(ADR trading/modes/0001)를 따라 Bot.mode 기본 PAPER + setMode(null) 폴백 PAPER + yaml stock.bot.mode: \${STOCK_BOT_MODE:PAPER} 노출. 운영 주의: 기존 서버는 env 미설정 시 LIVE 였으므로 배포 후 실주문 재개는 env 명시 필요(리뷰 P2-5 PAPER 실측 통과 전 비권장). StockPropertiesModeTest RED(LIVE) 선확인 → GREEN, StockBotConfigValidator 의 LIVE 경고는 기존 유지."

# Commit 102 — ✅ DONE() feat(common): 주식 봇 제어 API 관리자 전용 (P0-1, ADR common/security/0005) — 마지막 커밋(git_commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java src/test/java/me/singingsandhill/calendar/stock/presentation/api/StockBotApiSecurityTest.java docs/adr/common/security/0005-admin-only-stock-bot-control-api.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md docs/git_commit.md
git commit -m "feat(common): 주식 봇 제어 API POST 관리자 전용 — 무인증 start/stop/긴급청산 차단 (ADR common/security/0005)" -m "/api/stock/** permitAll + /api/** CSRF 면제로 StockBotApiController 의 start/stop/pause/resume/emergency-close 가 인터넷에서 무인증 POST 가능하던 critical(리뷰 §3-②, 적대적 검증 상향). 크립토 ADR common/security/0003 과 동형으로 POST /api/stock/bot/** 을 hasRole(ADMIN)으로 — /api/** permitAll 보다 먼저 선언, 미인증 진입점은 adminEntryPoint 매핑. GET /api/stock/bot/status 는 공개 대시보드(/stock) 위젯 5곳이 사용하므로 permitAll 유지(조회 API·대시보드 공개 범위 재검토는 ADR 에 P1 후속으로 명시 — 포지션 손익은 계좌 정보). StockBotApiSecurityTest 5케이스(TradingApiSecurityTest 패턴): 미인증 3xx+서비스 미도달 2건 RED 선확인, USER 403, ADMIN ok, status 공개 유지 가드. ADR README 매트릭스 62개 갱신 + 루트/common/stock CLAUDE.md 보안 표·모드 기본값 동기화. 검증: 전체 스위트 GREEN (TradingApiSecurityTest·DatedateAuthSecurityTest 무회귀)."

# =====================================================================
# stock 봇 P1 — 청산 구조 재보정 · 주문 신뢰성 · 운영 복구 (2026-07-24)
# =====================================================================
# 배경: docs/audit/stock-trading-logic-review-2026-07-24.md §9 P1 실행 (P0 완료 후속).
# 사용자 결정 3건: 손절=풀백저가 앵커+(-2% 캡) / TP3=진입가 +10% 고정 / 재시작=보호 전용 재개.
# 전 항목 TDD (RED 선확인 → GREEN), 전체 스위트 GREEN.

# Commit 103 — ✅ DONE() fix(stock): 청산 구조 재보정 — 풀백저가 손절 + 진입가 앵커 TP3 + 트레일링 정상화 (ADR stock/algorithm/0007)
git add src/main/java/me/singingsandhill/calendar/stock/domain/position/StockPosition.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java src/main/resources/application.yaml src/test/java/me/singingsandhill/calendar/stock/domain/StockPositionStopLossTest.java src/test/java/me/singingsandhill/calendar/stock/domain/StockPositionTakeProfitTest.java docs/adr/stock/algorithm/0007-exit-structure-recalibration.md
git commit -m "fix(stock): 청산 구조 재보정 — 풀백저가 손절·진입가 앵커 TP3·트레일링 정상화 (ADR 0007)" -m "리뷰 §4: 승자 트레이드의 이익 실현 경로가 구조적으로 붕괴(손익분기 승률 ~83%). ① TP3 앵커를 매 틱 갱신되는 당일고가(stck_hgpr)에서 진입가 고정으로 — 기존엔 5초 폴링 간격에 +10% 점프를 요구해 KRX VI 하에서 수학적 도달 불가(사문 조항). ② 트레일링 이중 결함 수정: 활성화 조건 tp1Executed → tp1||tp2||tp3 (TP2 가 먼저 발동하는 통상 경로에서 영원히 안 켜지던 문제), 활성화 시점에 스탑가를 max(현재가×(1-3.8%), 손익분기가)로 즉시 초기화(신고가 갱신 블록에서만 세팅해 활성화 직후 하락 시 null 로 미발동하던 문제) — calculateTrailingStop 헬퍼로 중복 제거. ③ 손절을 진입 근거에 묶음: resolveStopLossPrice = max(풀백저가×(1-pullback-stop-buffer-percent 1.0%), 진입가×(1-max-stop-loss-percent 2.0%)) → 통상 진입가 대비 -1.2%, 체결가 드리프트 시 -2% 캡, 풀백저가 없음/비정상(진입가 이상)이면 캡 폴백. 레거시 stop-loss-percent(5.0)는 계산에서 제외(키 보존). 테스트: 손절 4케이스 + TP3 앵커 2 + 트레일링 3 RED 선확인 후 GREEN. 손절 빈도 증가는 의도된 트레이드오프 — PAPER 실측(P2-5) 전 LIVE 금지."

# Commit 104 — ✅ DONE() feat(stock): 주문 선영속화 + 실체결 backfill + 고아 체결 스윕 (ADR stock/infrastructure/0006)
git add src/main/java/me/singingsandhill/calendar/stock/domain/trade/StockTrade.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java src/test/java/me/singingsandhill/calendar/stock/application/StockPositionServiceTest.java docs/adr/stock/infrastructure/0006-order-pre-persistence-and-fill-backfill.md
git commit -m "feat(stock): 주문 선영속화 + 실체결가·수수료 backfill + 고아 체결 스윕 (ADR 0006)" -m "리뷰 §3-④: 체결가가 주문 *전* 시세·수수료 0 의 픽션이었고(체결조회 TR 은 구현됐으나 호출처 0곳), 주문 선영속화가 없어 응답 유실 시 '실제 보유 중인데 시스템에 없는' 무보호 포지션이 생겼다. ① 매수 주문 전 StockTrade(PENDING, orderId=PENDING-<nano>) 선영속화 — 응답 실패 시 PENDING 을 실패로 덮지 않고 ORDER_UNCONFIRMED 이벤트만(접수 여부 불명). ② 성공 시 assignBrokerOrderId(ODNO) + 당일주문체결조회로 실체결 평균가(avg_prvs)·체결수량 backfill, 수수료는 체결금액×commissionRate 로 산정 — 포지션 진입가·손절가도 실체결 기준. 조회 실패 시 요청가 폴백 + WARN(픽션임을 로그로 노출). ③ reconcileUnconfirmedOrders(): 트레이딩 루프 시작부에서 미확인 PENDING 매수를 브로커 원장과 대조 — 체결 확인 시 거래 정합화 + 포지션 없으면 생성(ORPHAN_FILL_RECOVERED, 무보호 제거), 12틱 미발견 시 CANCELLED(ORDER_NOT_ACCEPTED). KIS 는 client order id 미지원이라 매칭 키는 종목+수량+미연결 조합(ADR 에 한계 명시). 스윕 실패는 리스크 체크를 막지 않음. 테스트 7케이스 RED 선확인 후 GREEN."

# Commit 105 — ✅ DONE() feat(stock): 재시작 보호 전용 자동 재개 + 최종청산 재시도·알림 (ADR stock/modes/0003) — 마지막 커밋(git_commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockMailService.java src/main/java/me/singingsandhill/calendar/stock/presentation/api/StockBotApiController.java src/test/java/me/singingsandhill/calendar/stock/application/GapPullbackBotRecoveryTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceTimeExitTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceTimeDecayTest.java src/test/java/me/singingsandhill/calendar/stock/presentation/api/StockBotApiSecurityTest.java docs/adr/stock/modes/0003-protection-only-recovery-on-restart.md docs/adr/README.md docs/audit/stock-trading-logic-review-2026-07-24.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md docs/git_commit.md
git commit -m "feat(stock): 재시작 시 보호 전용 자동 재개 + 11:20 최종청산 재시도·알림 (ADR stock/modes/0003)" -m "리뷰 §6. ① running 이 인메모리라 재배포하면 오픈 포지션의 손절·트레일링·11:20 청산이 전부 멈추던 문제 — ApplicationReadyEvent 에서 bot.enabled && 당일 오픈 포지션 존재 시 recoveryMode 로 자동 재개(리스크 루프·스윕·시간청산은 동작, executeEntries 는 차단), 관리자 start() 시 해제. BotStatus/API 에 recoveryMode 노출 + RECOVERY_RESUMED 이벤트. 부수 효과로 GapPullbackBotService 가 Clock(기존 stockClock 빈)을 주입받아 거래창 가드가 Clock.fixed 로 결정성 테스트 가능해짐(LocalDate/LocalTime.now(KST) → now(clock)). ② 최종청산 원샷 문제 — 종목당 3회 재시도(시세 조회 실패·매도 미완료 모두), 최종 실패 시 ERROR + TIME_EXIT_FAILED 이벤트 + StockMailService.sendTimeExitFailureAlert 메일로 수동 청산 요청(메일 미설정 시 WARN 로그). 이를 위해 closePosition 이 void → boolean(전량 청산 성공 여부)로 — 매도 거부가 조용히 성공처럼 보이던 문제 해소. 테스트 9케이스 RED 선확인 후 GREEN. 문서: ADR 3건 신설(algorithm/0007, infrastructure/0006, modes/0003) + README 매트릭스 65개 + stock/CLAUDE.md Exit Rules 표·운영 항목 갱신 + 감사 보고서 §9 에 P0/P1 완료 상태 표기(P2-5 PAPER 실측 전 LIVE 금지 명시)."

# =====================================================================
# stock 봇 P2 — 선정 규칙·비용 모델·거래정지 가드·실측 리포트 (2026-07-24)
# =====================================================================
# 배경: docs/audit/stock-trading-logic-review-2026-07-24.md §9 P2 실행 (P0·P1 완료 후속).
# 사용자 결정: 스크리닝은 "단계적"(강제선정 제거+신호 게이트, 갭 floor 는 실측 후), 실측 도구는 일일 리포트 생성.
# 전 항목 TDD, 전체 스위트 GREEN. 세율은 웹 확인으로 감사 보고서 권고(0.15%)를 0.20% 로 정정.

# Commit 106 — ✅ DONE() fix(stock): 매도세율 2026 기준 0.20% 정정 + 익절 게이트에 슬리피지 반영 (P2-1)
git add src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/config/StockCostModelTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceSlippageGateTest.java
git commit -m "fix(stock): 매도세율 0.20%(2026 시행) 정정 + 익절 게이트·손익분기 하한에 슬리피지 반영" -m "① sellTaxRate 0.0023(구 세율) → 0.0020. 2026-01-01 이후 양도분부터 코스피 = 증권거래세 0.05% + 농특세 0.15%, 코스닥·K-OTC = 0.20%(농특세 없음)로 두 시장 모두 0.20% — 감사 보고서 §4 의 '0.15%' 권고는 2025년 세율이었어 함께 정정(보고서에 정정 박스 추가). 왕복 비용 = 수수료 0.03% + 세금 0.20% = 0.23%. ② slippageBuffer(0.2%)가 정의만 되고 사용처 0곳이던 문제 — getEffectiveExitCostRate() 신설(왕복+슬리피지), checkTakeProfitLevels 가 순익에서 슬리피지를 차감한 뒤 최소수익 임계와 비교하고 트레일링 손익분기 하한도 이 값을 사용. 모든 주문이 시장가라 명목 익절이 순손실이 되던 경로 차단. TDD: 비용 상수 4케이스 + 게이트 2케이스(슬리피지 유무로 발동/보류 갈림) RED 선확인 후 GREEN."

# Commit 107 — ✅ DONE() feat(stock): 강제 선정 제거 + 신호 팩터 게이트 + 거래정지·VI 가드 (P2-2/3, ADR stock/algorithm/0008)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/ScreeningService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/dto/KisQuoteResponse.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java src/main/resources/application.yaml src/test/java/me/singingsandhill/calendar/stock/application/service/ScreeningSelectionTest.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/dto/KisQuoteTradabilityTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockPositionServiceTest.java docs/adr/stock/algorithm/0008-screening-selection-and-cost-model.md
git commit -m "feat(stock): 점수 미달 강제 선정 제거 + 신호 팩터 게이트 + 거래정지·VI 가드 (ADR 0008)" -m "① min-candidates 강제 선정 제거: 'selected<3 OR score>=40' → 'score>=40 AND 신호게이트'. 엣지 없는 날에도 매일 3건 진입하던 구조 해소 — 조건 미달이면 선정 0건이 정상. ② 신호 팩터 게이트(scoring.signal-min-score=25) 신설: 가중치 합 100 중 유동성 팩터(거래대금20+스프레드15+시총10=45)만으로 총점 임계 40 을 넘기던 왜곡 차단 — gapScore+strengthScore 합이 기준 미달이면 탈락. 갭 floor 0.5% 는 PAPER 표본 확보를 위해 유지(실측 후 상향 재검토 — 사용자 '단계적' 선택). 선정 로직을 selectCandidates() 로 분리해 단위 테스트 가능화(StockCandidate 를 package-private 로). ③ 거래정지·VI 가드: KisQuoteResponse 에 iscd_stat_cls_code/temp_stop_yn/mrkt_warn_cls_code/sltr_yn 4필드 + isTradable() 추가 — 임시정지(VI 포함)·거래정지(58)·관리(51)·투자위험/경고(52/53, 시장경고 02/03)·단기과열(59)·정리매매 배제, 투자주의는 허용. 스크리닝 Floor 0번 + 진입 직전 재확인(09:20 통과 후 VI 발동 대비, ENTRY_BLOCKED_NOT_TRADABLE 이벤트). 필드 부재는 거래 가능으로 간주해 무회귀, 전 종목 부재 시 WARN 으로 필드명 오류 노출(cttr 계측과 동일 패턴). 기존 15인자 생성자는 보조 생성자로 유지해 호출부 호환. TDD 13케이스 RED 선확인 후 GREEN."

# Commit 108 — ✅ DONE() feat(stock): 일일 실적 요약 리포트 + 문서 드리프트 해소 (P2-5/4) — 마지막 커밋(git_commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/DailyPerformanceReportService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockMailService.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/scheduler/StockTradingScheduler.java src/main/java/me/singingsandhill/calendar/stock/application/service/PullbackDetectionService.java src/test/java/me/singingsandhill/calendar/stock/application/service/DailyPerformanceReportServiceTest.java docs/adr/stock/observability/0002-daily-performance-report.md docs/adr/stock/algorithm/0004-tp-independent-triggers.md docs/adr/README.md docs/audit/stock-trading-logic-review-2026-07-24.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/application/CLAUDE.md docs/git_commit.md
git commit -m "feat(stock): 일일 실적 요약 리포트(PAPER 실측 데이터원) + 문서-코드 드리프트 해소 (ADR observability/0002)" -m "① DailyPerformanceReportService 신설 — 최종청산 이후 11:40 cron(평일·휴일 제외)으로 당일 청산/미청산 건수, 승·패와 승률(수수료·세금 반영 실현손익 부호 기준), 실현손익 합계·종목별, 청산 사유 분포, EntryAttempt 거절 사유 분포를 집계해 메일 + DAILY_REPORT 이벤트 + INFO 로그 3중 출력. 매매·시도가 모두 없던 날은 메일 생략(알림 피로 방지, 이벤트·로그는 유지). 리뷰 P2-5 의 'PAPER 2~4주 실측' 이 사람 손 질의 없이 누적되도록 하는 것이 목적 — LIVE 전환 판단 근거. TDD 4케이스 RED 선확인 후 GREEN. ② P2-4 드리프트 해소: stock/CLAUDE.md 상태머신을 하드코딩 수치(1.015/0.985/1.003/0.970)에서 설정 키 표기로 교체(운영값과 불일치 재발 방지), ADR-0004 의 부분청산 비율(1/3·1/3 초안)을 실제 운영값(tp1-ratio 0.5·tp2-ratio 0.6·TP3 잔여)으로 정정, PullbackDetectionService 의 낡은 주석 3곳(-1.5%/-3.0%/+0.3%)을 설정 키 기준으로 동기화, application CLAUDE.md 스크리닝 절차에 거래가능 가드·선정 규칙 반영. ③ ADR README 매트릭스 67개 + 감사 보고서 §9 에 P2 완료 상태와 세율 정정 박스 반영."

# Commit 109 — ✅ DONE() chore(stock): PAPER 모드 체결 조회 스킵 + BACKTEST 서술 정정 (문서-코드 드리프트)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java src/test/java/me/singingsandhill/calendar/stock/application/StockPositionServiceTest.java docs/adr/stock/modes/0001-paper-backtest-mode-and-clock-bean.md CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md TRADING.md docs/git_commit.md
git commit -m "chore(stock): PAPER/BACKTEST 는 체결 조회 스킵 + BACKTEST 실제 동작 서술 정정" -m "① PAPER 실측 준비 중 확인된 로그 노이즈: 주문이 시뮬레이션(SIM-<nano>)이라 브로커 원장에 없어 resolveBuyFill 이 매 진입마다 '체결 확인 불가' WARN 을 남겼다(결과는 정확 — 시뮬레이션은 요청가가 곧 체결가). LIVE 가 아니면 조회 자체를 건너뛰고 DEBUG 로만 기록하도록 수정, 부수적으로 진입당 KIS 호출 1건 절감. 기존 backfill 테스트 2건은 LIVE 모드로 명시(모드별 동작 구분이 테스트에 드러남) + PAPER 스킵 테스트 신설(RED 선확인 → GREEN). ② BACKTEST 문서-코드 드리프트: 주석·ADR·CLAUDE.md 는 '모든 시세/주문을 시뮬레이션(히스토리 fixture)' 이라 적혀 있으나 모드 분기가 isLiveMode() 하나뿐이라 실제로는 시세가 실 API — BACKTEST 는 현재 PAPER 와 동일하다. StockProperties 주석 / ADR stock/modes/0001 Consequences / 루트·stock CLAUDE.md 를 사실대로 정정하고 '실측에는 PAPER 사용' 명시. ③ PAPER 오해 방지 문구 추가: 시세·호가·잔고는 모드 무관 실 API 라 포지션 사이징이 실계좌 예수금·매수가능수량을 참조 — 예수금 0 이면 가상 진입도 발생하지 않는다(ADR modes/0001 에 기록). ④ TRADING.md 주식 봇 섹션이 P1·P2 이전 값(손절 -5%, TP3=DayHigh+10%, 08:30 fallback-only 유니버스)으로 남아 있어 현행 사실로 갱신 + 11:40 일일 리포트 잡 추가."

# =====================================================================
# 수익성 검사 후속 — 진입·트레일링 비용 정합 + 코인 PAPER 기본화 (2026-07-25)
# =====================================================================
# 배경: 코인·주식 수익성 검사(2026-07-25) 결론 실행. 주식 = 비용 게이트를 넘을 수 없는
# 진입 제거 + 러너 정상화 + 버그 3건. 코인 = 감사 P1-5/P1-8/P1-9 해소. 전체 스위트 GREEN

# Commit 110 — ✅ DONE() fix(stock): 진입 풀백 하한 1.5% + 트레일링 2.0% — 비용 게이트 정합 (ADR stock/algorithm/0009)
git add -p src/main/resources/application.yaml
git add src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java docs/adr/stock/algorithm/0009-entry-floor-and-trailing-cost-alignment.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md
git commit -m "fix(stock): 진입 풀백 하한 1.5%·트레일링 2.0% — 비용 게이트 정합 (ADR 0008 후속, ADR 0009)" -m "수익성 검사: ① 풀백 1.0~1.3% 진입은 TP2 기대 이득(d=1.0% 시 총 0.81%, 비용 0.43% 차감 후 0.38%)이 시간감쇠 게이트(09:20 실질 0.489%)를 못 넘어 손절(-1.2%)·본전·11:20 강제청산만 가능한 구조적 비수익 구간 — pullback-min-percent 1.0→1.5 (d=1.5% 시 순 0.89%로 게이트 통과). ② 트레일링 3.8% 는 고점이 진입가 +4.4%(1.0043/0.962)를 넘어야 손익분기 위로 올라와 사실상 본전 스탑 — 2.0 으로 축소해 +2.5% 부터 이익 잠금(러너 40% 기대 기여 0 → 정상화, 손익분기 승률 추정 ~62-64% → ~50%대 복귀). ③ Entry/Exit/Risk Java 기본값을 yaml 운영값과 정합(pullbackMax 3→5, bounce 0.3→0.2, tp1 1.5→5, tp3 1→10, trailing 0.8→2.0) — 키 누락 시 구식 값 회귀 방지(코인 P1-9 동일 원칙). 두 값 모두 이론 보정 — P2-5 PAPER 실측으로 검증 후 확정."

# Commit 111 — ✅ DONE() fix(stock): 진입검증 시간조건 null=FAIL + 매도 원장 비용 기록 + tp2-ratio 배선
git add src/main/java/me/singingsandhill/calendar/stock/application/service/PullbackDetectionService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockPositionService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java src/main/java/me/singingsandhill/calendar/stock/domain/position/StockPosition.java src/test/java/me/singingsandhill/calendar/stock/application/service/PullbackEntryValidationTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockPositionServiceTest.java src/test/java/me/singingsandhill/calendar/stock/domain/StockPositionTakeProfitTest.java
git commit -m "fix(stock): 진입검증 풀백시간 null=FAIL + 매도 원장 비용 기록 + tp2-ratio 팬텀 배선" -m "수익성 검사 발견 3건. ① validateEntryConditions 조건 3(풀백 지속시간)이 pullbackStartAt null 이면 자동 PASS — soft 2/3 검증의 실질 문턱이 1개로 하락(ADR 0003 의 '데이터 부족 ≠ 통과' 원칙 위반). 조건 1·2와 동일하게 FAIL 로 정정 + 회귀 테스트 2케이스(정상 경로 진입 보존 포함). ② 매도 거래 원장이 markFilled(fee=BigDecimal.ZERO)로 저장 — 포지션 손익은 정확하지만 원장 기반 수수료 집계가 매도측(수수료+거래세 0.215%)을 통째로 누락. 포지션과 동일 비용 모델(체결대금 × (commissionRate+sellTaxRate))로 기록 + 테스트(105,000×40주 → 9,030원). ③ exit.tp2-ratio 가 설정만 있고 calculateTp2Quantity() 가 0.6 하드코딩(getTp2Ratio() 호출부 0곳) — tp1 과 동일하게 파라미터로 배선, StockRiskService 가 설정값 전달."

# Commit 112 — ✅ DONE() feat(trading): 기본 모드 PAPER 전환 + Java 기본값 yaml 정합 (ADR trading/modes/0002, P1-8/P1-9)
git add -p src/main/resources/application.yaml
git add src/main/java/me/singingsandhill/calendar/trading/infrastructure/config/TradingProperties.java docs/adr/trading/modes/0002-paper-default-mode.md docs/adr/trading/modes/0001-paper-mode-default-and-order-gate.md src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md
git commit -m "feat(trading): 기본 모드 PAPER 전환 + Java 기본값 yaml 정합 (ADR modes/0002, P1-8/P1-9)" -m "수익성 검사 + 운영 감사(2026-07-06) P1-8/P1-9. ① 기본 모드 LIVE→PAPER, setMode(null)→PAPER — 설정 누락·오타가 실주문으로 폴백하던 fail-dangerous 기본값 제거. 실주문은 TRADING_BOT_MODE=LIVE 명시적 opt-in (stock modes/0002 와 동일 패턴, ADR 0001 의 기본값 결정을 0002 로 대체). ⚠️ 운영 영향: env 미설정 LIVE 배포는 이 변경 후 자동 PAPER — 실주문 지속하려면 TRADING_BOT_MODE=LIVE 명시 필요. +EV 미입증 상태라 PAPER 실측이 당분간의 정상 운영 모드이기도 함. ② Risk Java 기본값(-0.03/0.15/0.03/0.10)이 yaml 유효값(-0.015/0.03/0.008/0.015)과 3~5배 불일치 — 키 하나 누락 시 감사가 '구조적 마이너스 EV' 로 진단한 구식 파라미터로 조용히 회귀하던 P1-9 해소. Bot(cooldown·minHolding 10/15→30/30)·Rebalancing(minSellPnlPct 0.03→0.0, ADR risk/0002 정합) 동일 적용."

# Commit 113 — ✅ DONE() test(trading): 손절/익절/트레일링 트리거 경계 테스트 (P1-5) — 마지막 커밋(git_commit.md 포함)
git add src/test/java/me/singingsandhill/calendar/trading/application/service/RiskManagementServiceTriggerTest.java docs/trading/remaining-work.md docs/adr/README.md docs/git_commit.md
git commit -m "test(trading): 손절/익절/트레일링 트리거 경계 테스트 (P1-5)" -m "자본 보호 핵심 경로(checkPositionRisk)에 테스트가 TimeExit 뿐이던 감사 P1-5 해소. 수수료 차감 net PnL 기준 6케이스: 손절 -1.5% 경계 발동/미발동(990→-1.5% 발동, 991→-1.4% 홀드), 익절 +3% 발동, 트레일링 +1.5% 활성화(스탑 1,021×0.992=1,012 즉시 세팅·청산 없음·영속화 확인), HWM 추적 발동(1,050→스탑 1,041, 1,030 에서 청산), 손익분기 floor(원 스탑 1,001 < 1,005 → floor 로 본전 방어). Clock 주입(시간 의존 경계 결정성)은 잔여 항목으로 remaining-work.md 에 유지. 문서: remaining-work P1-5/P1-8/P1-9 상태 갱신 + ADR README 매트릭스 69개."

# =====================================================================
# CLAUDE.md 전수 감사 — 문서·코드 사실 정합 (2026-07-25)
# =====================================================================

# Commit 114 — ✅ DONE() docs: CLAUDE.md 전수 감사 — 사실 오류 6건 정정 + 누락 동작·열거 보강
git add -p src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md docs/adr/README.md
git add CLAUDE.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md src/main/java/me/singingsandhill/calendar/trading/application/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/application/CLAUDE.md src/main/java/me/singingsandhill/calendar/runner/application/CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md src/main/resources/templates/fragments/CLAUDE.md docs/git_commit.md
git commit -m "docs: CLAUDE.md 전수 감사 — 존재하지 않는 심볼 정정 + 누락 동작·열거 보강" -m "11개 CLAUDE.md 전수 대조. ① 사실 오류 6건: SecurityConfig 에 /** 전역 permitAll 은 존재하지 않음(실제 /* + /*/*/*) — 루트 표를 선언 순서 12행으로 재작성하고 /runners/admin/login(permitAll)이 /runners/admin/**(ADMIN) 보다 먼저여야 로그인이 가능하다는 순서 의존성을 명시(기존 표는 행 순서가 반대로 오해 유발); BithumbApiClient.simulateOrder 없음 → simulateBuy/simulateSell; SelectionConverter → SelectionListConverter; alwaysUseMessageFormat 은 코드베이스 어디에도 설정되지 않음(Spring 기본값 false 이며 WebConfig 는 setFallbackToSystemLocale(false) 만 호출) — 'WebConfig 가 설정' 서술 정정; common security ADR 4개 → 5개; 'trading.bot.enabled=false 이면 모든 잡 스킵' 은 거짓 — CandleScheduler.cleanupOldCandles() 는 가드가 없어 항상 실행(주식은 5개 메서드 전부 가드되어 정확 — 두 모듈의 차이를 양쪽 문서에 명시). ② 누락 동작: /api/trading/verify/test-order 가 시장가 매수를 전송하며 모드 가드는 받지만 bot.enabled·서킷브레이커를 우회한다는 경고 신설(봇 정지 상태에서도 LIVE 면 실주문) + trading·stock presentation 컨트롤러 전체 표; BUY 신호의 MA60 억제 게이트(현재가<MA60 이면 다이버전스·RSI<30·거래량 스파이크 중 하나 없으면 HOLD 강제, Issue #9); 주식 TP1·TP2·TP3 가 전부 순이익 게이트(수수료차감 손익 − 슬리피지 ≥ 시간감쇠 최소수익)를 통과해야 발동하며 TP2(당일고가)도 예외 아님 + 실효 청산비용 0.43% 구성(0.015%×2 + 0.20% + 0.2%); TradingEventService 의 REQUIRES_NEW(호출자 롤백에도 이벤트 잔존) 및 ProfitService; 리밸런스 default-ratio 폴백·NEUTRAL 구간; excludeFormingCandle(기본 OFF)·RSI 추세 lookback. ③ 미반영 Accepted ADR 반영: trading/risk/0004(수동매매 Position 정합·엔진 핑퐁), trading/strategy/0006~0009, stock/infrastructure/0004. ④ 열거 누락: 템플릿 표에 auth·me·recap·tools·runners|stock|trading/fragments 7디렉터리 + about/faq/portfolio/verify/create/announce/attendance-form 누락분 보강(49파일·13디렉터리 대조 완료); common 클래스 목록에 CorsConfig·Adsense·IndexNow·KakaoOAuth2ClientConfig·IndexNowService·LocaleLinks·AdsenseModelAdvice; 스케줄러 표에 IndexNowScheduler(03:30 KST)·주식 11:40 일일 리포트; AuthController·MyPageController·RecapController 잔여 라우트; AttendanceNotFoundException; fragments 표에 create-schedule-modal.html. ⑤ docs/adr/README.md 색인에 누락돼 있던 common/security/0002(CORS) 행 추가 — 매트릭스 합계는 이미 포함돼 있어 수치 변경 없음. 프로덕션 코드 무변경."

# =====================================================================
# recap 공유 링크 og:url 정정 (2026-07-26)
# =====================================================================
# 배경: "recap 을 다른 사람과 공유해서 볼 수 없나" 질문에서 출발한 조사. 공유 자체는
# 정상 동작(/recap/share/{token} permitAll, 무인증 200)이지만, SeoService 가 canonical
# 경로를 토큰 없이 만들어 모든 공유 페이지가 핸들러 없는 404 URL 을 canonical·og:url 로
# 선언하고 있었다. head.html 이 canonical 을 og:url 로 재사용하므로, 미리보기 카드의 탭
# 대상으로 og:url 을 쓰는 메신저에서는 수신자가 404 로 떨어진다.
# 정책 무변경: noindex,nofollow / sitemap·IndexNow 제외 / adsEnabled=false / 인증 규칙
# 모두 그대로 — 결정 변경이 아닌 버그 수정이라 신규 ADR·CLAUDE.md 수정 없음.
# 검증: RED 선확인(expected .../recap/share/abc-token, but was .../recap/share) → GREEN,
#       전체 스위트 85 클래스 / 504 테스트 / 실패 0.

# Commit 115 — ✅ DONE() fix(datedate): recap 공유 페이지 canonical·og:url 에 토큰 포함 — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapController.java src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/RecapControllerTest.java docs/guides/git-commit.md
git commit -m "fix(datedate): recap 공유 페이지 canonical/og:url 에 토큰 포함" -m "getRecapShareSeo 가 path 를 \"/recap/share\" 로 만들어 토큰이 빠져 있었다 — 그 URL 은 @GetMapping(\"/recap/share/{token}\") 만 존재하므로 핸들러가 없는 404 다. fragments/head.html 이 canonical 을 <link rel=canonical> 과 <meta property=og:url> 양쪽에 쓰기 때문에, 발급된 모든 공유 링크가 서로 구분되지 않는 동일한 404 URL 을 자기 자신으로 선언했다. og:title/description/image 는 닉네임·연도가 정상 반영돼 미리보기 카드 자체는 뜨지만, 카드의 탭 대상으로 og:url 을 채택하는 메신저에서는 링크를 받은 사람이 recap 대신 404 를 본다 — ADR datedate/domain/0005 가 의도한 \"링크 수신자가 로그인 없이 recap 을 본다\" 와 어긋난다. path 한 곳에 token 을 붙여 canonical/canonicalKo/canonicalEn 세 필드를 기존 헬퍼 경유로 일괄 정정하고, 호출부(RecapController.sharedRecap)는 이미 @PathVariable 로 갖고 있던 token 을 넘긴다. 프로덕션 호출부는 이 한 곳뿐. 정책은 무변경 — robots noindex,nofollow 유지(canonical 은 noindex 를 무효화하지 않는다), sitemap·IndexNow 제외 유지(SitemapServiceWhitelistTest GREEN), adsEnabled 기본 false 유지, /recap/share/** permitAll · /recap/** ROLE_USER 유지(DatedateAuthSecurityTest GREEN). 토큰은 이미 공유 URL 자체이므로 og:url 노출로 새로 새는 정보는 없다. 회귀 가드: SeoServiceI18nTest.recapShareSeo_canonicalIncludesToken 이 canonical 3종 + robots + hreflangEnabled + adsEnabled 를 함께 고정 — 수정 전 RED(expected .../abc-token, but was .../recap/share) 확인 후 GREEN."

# =====================================================================
# 카카오 프로필 이미지 혼합 콘텐츠(mixed content) 정정 (2026-07-26)
# =====================================================================
# 배경: HTTPS 페이지 콘솔의 "Mixed Content: ... requested an insecure element ...
# This request was automatically upgraded to HTTPS" 경고 분석에서 출발. Chrome 이
# "insecure element" 라고 부르는 대상은 img/audio/video 같은 수동적 하위 리소스뿐이라
# 범인이 <img> 로 좁혀졌고, 전체 템플릿에서 앱 상대경로(th:src="@{...}")·하드코딩
# HTTPS 가 아닌 <img> 는 카카오 프로필 이미지 5곳(fragments/header.html 33·69·117·153,
# me/mypage.html 10)뿐이었다. 개발 DB(data/scheduledb.mv.db)에 실제로 저장된 값이
# http://k.kakaocdn.net/dn/.../img_640x640.jpg 로 확인돼 근원 확정.
# 정책 무변경: 인증·스코프·저장 스키마 모두 그대로 — 결정 변경이 아닌 버그 수정이라
# 신규 ADR·CLAUDE.md 수정 없음(ADR common/security/0004 는 scope 만 기술, URL 스킴
# 처리는 미기술).
# 검증: RED 선확인(expected "https://k.kakaocdn.net/...", but was "http://...") 2건 →
#       GREEN, 전체 스위트 85 클래스 / 506 테스트 / 실패 0.

# Commit 116 — ✅ DONE() fix(datedate): 카카오 프로필 이미지 URL http→https 정규화 — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfile.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfileTest.java docs/guides/git-commit.md
git commit -m "fix(datedate): 카카오 프로필 이미지 URL 을 https 로 정규화" -m "카카오 /v2/user/me 가 profile_image_url(및 properties.profile_image 폴백)을 http 스킴으로 내려주는데 KakaoProfile.from 이 원본을 그대로 통과시켜, HTTPS 로 서빙되는 페이지에서 <img src=\"http://k.kakaocdn.net/...\"> 가 렌더링되고 있었다. 브라우저 콘솔에 Chrome 의 혼합 콘텐츠 경고가 로그인 사용자 전 페이지(공용 헤더 4곳 + 마이페이지 1곳)에서 반복 출력된다. Chrome 81+ 는 이런 수동적 리소스를 자동으로 https 로 올려 재요청하고 실패 시 http 로 폴백하지 않고 차단하므로, 현재는 k.kakaocdn.net 이 HTTPS 를 정상 제공(TLS 핸드셰이크·인증서 검증 통과)해 이미지가 보이지만 CDN 이 특정 경로에서 HTTPS 를 주지 못하면 곧바로 깨진 이미지가 된다 — 브라우저 자동 업그레이드에 의존하지 않도록 서버에서 확정한다. 수정은 파싱 단계 한 곳(KakaoProfile.toHttps): profile 우선·properties 폴백이 firstNonBlank 로 합류한 뒤 한 번만 정규화되므로 두 경로가 함께 커버되고, 렌더 지점 5곳을 각각 손대는 중복이 없다. 저장 값도 자연히 치유된다 — KakaoOAuth2UserService 는 upsert 가 반환한 AppUser.getProfileImageUrl() 을 appProfileImage 속성에 싣고 AppUserService.upsertKakaoUser 는 재로그인마다 refreshProfile 로 갱신하므로, 기존 http 로 저장된 행은 다음 로그인 시점에 https 로 덮인다(별도 데이터 마이그레이션 불필요). http:// 접두사가 아닌 값(이미 https, 프로토콜 상대 //, null)은 그대로 통과. 회귀 가드 2건: KakaoProfileTest 의 upgradesInsecureProfileImageUrl(kakao_account.profile 경로) / upgradesInsecureProfileImageUrlFromProperties(properties 폴백 경로) — 수정 전 RED 확인 후 GREEN, 기존 4건 포함 6건 전부 통과."

# =====================================================================
# 운영 로그(2026-07-27) 문제 3건 수정 — 스크리닝 전멸 / PrematureClose / lang 500
# =====================================================================
# 배경: 트레이딩 로직 수정 후 운영 로그 분석에서 확정된 문제 3건. ① 스크리닝 30종목 전부
# "체결강도(cttr) 미집계 — raw cttr=null" → Selected 0: KIS 공식 스펙(koreainvestment/
# open-trading-api 샘플) 검증 결과 시세 TR(FHKST01010100, 82필드)에는 cttr 도 폴백 필드
# (seln_cntg_smtn/shnu_cntg_smtn)도 없다 — 존재하지 않는 필드 파싱(d8b0158 의 P0-3 "수정"
# 자체가 같은 사고 2회째). 체결강도 실소재는 inquire-ccnl(FHKST01010300) tday_rltv.
# ② PrematureCloseException BEFORE response 가 매분 :05 루프에서 3~10분 간격 반복:
# HttpClient.create() 기본 풀에 idle 폐기 없음 → 서버가 닫은 stale 커넥션 재사용. 부수로
# 조회 실패 시 TradingBotService 진입 가드가 if(price!=null) 구조라 무가드 매수 진행
# (fail-open, javadoc 과 실동작 반대). ③ 봇 스캔 ?lang=../../..가 LocaleChangeInterceptor
# parseLocale IAE → 요청마다 500 + ERROR 스택트레이스.
# 비문제(수정 없음): 08:30 volume-rank rows=0 → 09:20 재시도 성공은 ADR stock/algorithm/
# 0006 의도대로, ThinkPHP 스캔은 Tomcat 파싱 거부(INFO), 메일 첨부는 첨부명만 스냅샷 명명.
# 검증: 4단계 전부 RED 선확인(11테스트 중 8 RED — 500/전멸/가드우회/cttr=0 전부 재현) →
#       GREEN, 전체 스위트 90 클래스 / 514 테스트 / 실패 0.

# Commit 117 — ✅ DONE() fix(stock): 체결강도 소스를 inquire-ccnl(tday_rltv) 로 전환 (ADR stock/infrastructure/0007)
git add src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/dto/KisQuoteResponse.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KoreaInvestmentApiClient.java src/main/java/me/singingsandhill/calendar/stock/application/service/ScreeningService.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClientTradeStrengthTest.java src/test/java/me/singingsandhill/calendar/stock/application/service/ScreeningFloorStrengthTest.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/dto/KisQuoteResponseTest.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/dto/KisQuoteTradabilityTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockPositionServiceTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceSlippageGateTest.java src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceTimeExitTest.java src/test/java/me/singingsandhill/calendar/stock/application/service/PullbackEntryValidationTest.java src/test/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClientOrderRetryTest.java docs/adr/stock/infrastructure/0007-trade-strength-source-inquire-ccnl.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/application/CLAUDE.md
git commit -m "fix(stock): 체결강도 소스를 inquire-ccnl(tday_rltv) 로 전환 (ADR stock/infrastructure/0007)" -m "2026-07-27 09:20 스크리닝 30종목 전멸(gap 11 + dataInsufficient 19, Selected 0)의 근본 원인: 시세 TR(FHKST01010100, inquire-price)의 cttr 필드를 체결강도로 파싱했으나 KIS 공식 스펙(공식 GitHub 샘플 82필드)에 cttr 는 존재하지 않는다 — 폴백 필드(seln_cntg_smtn/shnu_cntg_smtn)도, inquire-price-2(FHPST01010000, 54필드)에도 없음. d8b0158(P0-3) 의 cttr 매핑 결정을 supersede. 체결강도 실소재인 주식현재가 체결(FHKST01010300, inquire-ccnl)의 tday_rltv(최신 행)를 KisRestClient.getTradeStrength() 신설로 조회 — executeGetWithRetry(GET 재시도 유지)·Semaphore 게이트 경유, 실패/빈 응답/필드 부재는 0 이 아닌 null(미집계 구분). 스크리닝은 갭 통과 종목만 조회(+11콜 수준)하고 null 은 기존 skipZeroStrength 경로로 안전 탈락(무회귀). 진입 검증(PullbackDetectionService 조건 1, 기존 상시 FAIL)은 KoreaInvestmentApiClient.getTradeStrength 위임 교체로 자동 연결(null=FAIL 로직 기존 그대로, 콜 수 불변). 존재하지 않는 필드 3개와 calculateTradeStrength() 는 이번 이동으로 전부 미사용이 되어 제거 — 같은 사고(스펙 미검증 필드 매핑) 2회 전력의 3회째를 구조적 차단. 부수: minTradeStrength Java 기본 110→100 (yaml 정합 규칙, 레거시 경로용). 회귀 가드: KisRestClientTradeStrengthTest(실스펙 스텁 — cttr 없는 quote + tday_rltv 있는 ccnl, 최신 행/재시도/null 4케이스) + ScreeningFloorStrengthTest(ccnl 소스로 Floor 3 통과 + null 시 안전 탈락) — 수정 전 RED(0 반환/전멸 재현) 확인 후 GREEN. 잔여 위험: tday_rltv 필드명은 실API 로만 최종 확인 가능 — 부재 시 null 안전 탈락 + WARN 계측으로 다음 거래일 로그에서 판별. 리뷰 보강: 테스트가 tr_id(FHKST01010300)·FID 파라미터·'시세 엔드포인트 미호출'까지 단정(TR 오선택 회귀 가드), 갭 탈락 종목 미조회(콜 예산)·skipZeroStrength=false 시 null→floor(95) 보정 고정 테스트 추가. 부수 정정: docs/adr/README.md View 3 의 common/security 4→5 (기존 오류 — 이번 총계 72 갱신으로 표면화된 자기모순 해소)."

# Commit 118 — ✅ DONE() fix(trading): Reactor Netty 커넥션 풀 idle 폐기 정책 (ADR trading/infrastructure/0004)
# 주의: 루트 CLAUDE.md 는 이 커밋이 소유 — Commit 120 의 lang 사실 1줄 + 기존 미커밋
#       git_commit.md→guides/git-commit.md 경로 정정 2줄이 함께 들어간다(내용 유실 없음).
git add src/main/java/me/singingsandhill/calendar/trading/infrastructure/config/WebClientConfig.java src/test/java/me/singingsandhill/calendar/trading/infrastructure/config/WebClientConfigTest.java docs/adr/trading/infrastructure/0004-reactor-netty-connection-pool-policy.md CLAUDE.md
git commit -m "fix(trading): Reactor Netty 커넥션 풀 idle 폐기 정책 (ADR trading/infrastructure/0004)" -m "운영 로그에 PrematureCloseException(BEFORE response)이 매분 :05 트레이딩 루프에서 3~10분 간격으로 밤새 반복 — 커넥션 ID 접미사(-4~-128)가 풀 재사용 커넥션임을 지시. 원인은 HttpClient.create() 기본 ConnectionProvider: maxIdleTime 무한·evictInBackground 없음이라 Bithumb 이 keep-alive idle 로 닫은 stale 커넥션을 재사용. ConnectionProvider(maxIdleTime 10s / maxLifeTime 5m / evictInBackground 30s) 로 서버보다 먼저 폐기 — 크립토 루프(매분)는 틱 간 폐기, stock 5초 루프는 warm 재사용 유지. 기존 타임아웃 3종 불변. GET 재시도는 보류(원인은 풀이 제거, 잔여 실패는 risk/0005 fail-safe 가 흡수; HttpClient 전역 retry 는 주문 POST 오염 위험이라 금지 — 비멱등 무재시도 불변). 이 빌더 빈은 싱글턴이라 stock KIS 클라이언트도 같은 풀 공유(KisRestClient 는 자체 재시도 보유라 무해). 이 예외의 실영향은 단순 노이즈가 아니었다: 조회 실패 틱에 손절/익절/트레일링 체크 스킵·SELL 스킵·(수정 전) 진입 가드 우회 — 가드 건은 Commit 119. ConnectionProvider 는 @Bean(destroyMethod=dispose) 로 등록해 컨텍스트 종료 시 evict 태스크·풀 정리(테스트 컨텍스트 캐싱 잔존 방지). 수치는 introspection API 부재로 단정 테스트 불가 — 풀 이름 스모크만 두고 완치 판정은 다음날 로그(PrematureCloseException 0건). 부수로 reactor.netty WARN 이 root 로거를 타고 stock-trading.log 를 오염시키던 것도 원인 소멸로 해소."

# Commit 119 — ✅ DONE() fix(trading): 현재가 조회 실패 시 진입 가드 차단 (ADR trading/risk/0005)
git add src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceGuardFailSafeTest.java docs/adr/trading/risk/0005-fail-safe-entry-guard-on-price-unavailable.md src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md
git commit -m "fix(trading): 현재가 조회 실패 시 진입 가드는 우회가 아니라 차단 (ADR trading/risk/0005)" -m "executeBuy(:379)·entryRiskGuardsBlock(:1053) 의 if (currentPriceForGuard != null) 구조는 현재가 조회 실패 시 물타기 차단(P2-10)·코인 노출상한(P2-12) 가드를 평가하지 않고 매수를 진행시켰다(fail-open) — javadoc 은 '보수적으로 통과시키지 않고 스킵' 이라 실동작과 반대. 2026-07-27 로그의 PrematureCloseException 경로(orderbook 실패 → getCurrentPrice null)가 이 우회를 실제로 밟는다: 손실 포지션 보유 중에도 무가드 신규 매수 가능. null 이면 조기 리턴(executeBuy)/차단 true(entryRiskGuardsBlock) 로 반전 — manualBuy 도 같은 가드 공유라 함께 fail-safe(의도된 확장). 청산·리스크 경로(SELL)는 이 가드와 무관해 영향 없음. 비용은 해당 틱 매수 1회 유실뿐(다음 틱 60초 재평가) — 주문 무재시도(불확실하면 돈이 안 나가는 쪽)와 같은 실패 모드 선택. javadoc 정정 포함. 회귀 가드: TradingBotServiceGuardFailSafeTest 2케이스(가드 true / 주문 미전송 verify never) — 수정 전 RED(NeverWantedButInvoked 로 매수 진행 재현) 확인 후 GREEN."

# Commit 120 — ✅ DONE() fix(common): 잘못된 ?lang= 값 무시 (ignoreInvalidLocale) — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/common/infrastructure/config/WebConfig.java src/test/java/me/singingsandhill/calendar/common/infrastructure/config/LocaleChangeInvalidLangTest.java docs/guides/git-commit.md
git commit -m "fix(common): 잘못된 ?lang= 값을 500 없이 무시 (ignoreInvalidLocale)" -m "봇 스캔 ?lang=../../../../tmp/index1 이 LocaleChangeInterceptor.preHandle 의 parseLocale IAE 로 전파돼 요청마다 500 + MvcExceptionHandler ERROR 스택트레이스(에러 디스패치에서 인터셉터가 재차 터져 이중 로깅)를 만들었다 — 공격은 실패하지만 외부 입력만으로 5xx·ERROR 노이즈 유발. setIgnoreInvalidLocale(true) 한 줄: Spring 이 IAE 를 catch 해 DEBUG 만 남기고 진행(6.2.8 동작 확인), 로케일은 기존 리졸버 체인(쿠키→Accept-Language→ko, 화이트리스트 방어 기존대로)로 폴백. 잘못된 lang 값을 넣는 테스트가 리포 전체 0건이던 공백을 LocaleChangeInvalidLangTest 2케이스로 보강(경로조작 값 200 + lang=en Content-Language 회귀 짝) — 수정 전 RED(200 기대, 실제 500) 확인 후 GREEN. 결정 변경이 아닌 결함 수정이라 신규 ADR 없음(비결정성 문턱), 루트 CLAUDE.md WebConfig 사실 1줄은 Commit 118 에 흡수."

# =====================================================================
# 인사이트 트렌드 평균 투표 0.0 표시 버그
# =====================================================================

# Commit 121 — ✅ DONE() fix(datedate): 장소/메뉴당 평균 투표 정수 나눗셈 절삭
git add src/main/java/me/singingsandhill/calendar/datedate/application/dto/ServiceStatsDto.java src/main/java/me/singingsandhill/calendar/datedate/application/service/InsightsService.java src/main/resources/templates/insights/trends.html src/test/java/me/singingsandhill/calendar/datedate/application/service/InsightsServiceTest.java docs/troubleshooting/thymeleaf-spel-integer-division.md docs/troubleshooting/README.md
git commit -m "fix(datedate): 장소/메뉴당 평균 투표가 0.0 으로 절삭되던 문제" -m "insights/trends.html 이 장소·메뉴당 평균 투표를 뷰에서 직접 계산했는데 stats.totalLocationVotes / stats.totalLocations 양쪽이 long 이라 SpEL 이 정수 나눗셈을 수행했다. 운영값 200/229·72/87 은 둘 다 0 으로 잘리고 #numbers.formatDecimal(...,1,1) 이 소수부 .0 을 붙여 '0.0' 이라는 정상처럼 보이는 값을 출력한다 — th:if 등록수>0 가드가 0 나눗셈 예외까지 막아 예외도 로그도 남지 않는 조용한 실패였다. 같은 화면의 '일정당 평균 참여자 수' 만 멀쩡했던 이유는 그 값이 이미 InsightsService 에서 (double) 캐스트로 계산된 필드였기 때문 — 즉 산술이 뷰에 있는 것 자체가 원인이다. 수정은 같은 메서드 안에 이미 있던 avgParticipants 패턴(0 가드 + (double) 캐스트)을 그대로 재사용해 avgVotesPerLocation/avgVotesPerMenu 를 서비스에서 계산하고 ServiceStatsDto 에 double 로 실어 보내며, 템플릿은 필드만 출력한다. 등록 0건일 때 행을 숨기는 th:if 가드는 의미가 남아 유지. 산술이 서비스로 내려오면서 단위 테스트로 고정 가능해졌고 InsightsService 는 그간 테스트가 0건이었다 — InsightsServiceTest 5케이스 신설(운영값 재현 0.873/0.827, 소수부 보존 7/2=3.5, 등록 0건 0 가드, avgParticipantsPerSchedule 회귀, 원시 카운트 통과). 수정 전 RED(신규 접근자 부재로 컴파일 실패 6건) 확인 후 GREEN, 실제 앱 렌더로도 확인(개발 DB 메뉴 3표/6개 → 정수 나눗셈이면 0.0, 실제 0.5 출력). 표시 로직만 바뀌고 집계 소스(count()/countAllVotes())는 불변 — 분모가 만료·삭제 미필터 전역 카운트라 평균이 1 미만인 것은 지표 정의 문제로 별건. 예외·로그·테스트 실패가 전부 0인 조용한 실패라 재발 시 진단 비용이 큰 유형이므로 docs/troubleshooting/thymeleaf-spel-integer-division.md 로 문서화(README 인덱스 + '표시 값 오류(예외·로그 없음)' Quick Reference 추가): 위장 메커니즘(formatDecimal 이 절삭값에 .0 을 덧붙임 / th:if 0가드가 ArithmeticException 이라는 유일한 발견 경로 차단), 같은 화면의 정상 지표와 대조해 원인을 좁히는 진단 순서, 전수 검사 grep 2종(수정 전 236·252 행 적중 확인), 몫이 나누어떨어지지 않는 7/2=3.5 케이스를 테스트에 넣어야 하는 이유(3/6=0.5 형태만 있으면 분모>분자 조건에서만 잡힘)."

# =====================================================================
# GSC BreadcrumbList "'item' 입력란 누락" — 리치 결과 제외 6건
# =====================================================================
# Commit 122 —  ✅ DONE() fix(seo): BreadcrumbList 2단계 + 모든 ListItem 에 item (ADR common/seo/0008)
git add src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java src/main/resources/messages.properties src/main/resources/messages_en.properties docs/adr/common/seo/0008-breadcrumb-item-on-every-listitem.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md
git commit -m "fix(seo): BreadcrumbList 2단계 축소 + 모든 ListItem 에 item URL (ADR common/seo/0008)" -m "GSC 가 2026-05-20 부터 리치 결과 오류 'item 입력란이 누락되었습니다(경로: itemListElement)' 를 6개 URL 에 보고했다 — UseCaseSlugs.ALL 5개와 /tools/date-diff. Google 규격은 마지막을 제외한 모든 ListItem 에 item 을 요구하는데 SeoService 의 breadcrumb 텍스트 블록 7곳이 전부 position 1 에만 item 을 넣고 있었다. 그래서 결과가 계층 깊이에 따라 갈렸다: 2단계 페이지(guide/faq/about/privacy/terms/insights/trends)는 item 없는 크럼브가 마지막이라 규격상 유효해 무사했고, 3단계인 getUseCaseSeo/getDateDiffSeo 만 중간 크럼브(활용 사례/도구)가 걸렸다 — 3단계 메서드가 정확히 이 둘이고 신고된 6개 URL 과 일치한다. 중간 크럼브에 URL 을 부여하는 수정은 불가능했다: /use-cases·/tools 허브 페이지가 존재하지 않는다(UseCaseController 는 @GetMapping(/{slug}) 뿐, HomeController 는 /tools/date-diff 만, 두 템플릿 디렉토리에 index 없음, SitemapService 에도 없음) — 채우면 오류를 404 를 가리키는 품질 문제로 바꿔치기할 뿐이다. 이미 유효하게 동작하던 6개 페이지와 같은 형태로 수렴시키는 쪽을 골라 홈 → 현재 페이지 2단계로 축소하고, 마지막 항목까지 포함해 모든 ListItem 에 item 절대 URL(baseUrl+path, 같은 JSON-LD 의 url 필드와 동일 규약)을 채운다 — 규격상 마지막 item 은 선택이지만 계층이 하나 늘면 기존 마지막이 중간이 되어 곧바로 같은 오류가 되기 때문이고, 이번 사고가 정확히 그 형태였다. 7곳에 복붙돼 있던 블록은 breadcrumbJsonLd(leafName, leafPath) 헬퍼로 단일화해 불변식이 한 곳에만 존재하게 했다(-94줄). 크럼브 1개인 getHomeSeo 와 RunnerController 는 이미 item 이 있어 무변경. 중간 크럼브 제거로 참조가 사라진 seo.breadcrumb.useCases/seo.breadcrumb.tools 를 ko/en 양쪽에서 제거(화면 breadcrumb 은 별개 키 tool.breadcrumb.* 라 무영향). 회귀 가드: SeoServiceI18nTest.breadcrumbList_everyItemHasUrl — ko/en 양쪽 UseCaseSlugs.ALL 전수로 모든 ListItem 의 item 존재·절대 URL·position 연속성을 검증한다. 기존 allJsonLd_validJsonBothLocales 는 readTree 파싱만 해서 이 버그를 통과시켰다(JSON 유효성과 스키마 규격은 다른 층). 수정 전 RED(/guide position=2 에서 item 누락) 확인 후 GREEN, 전체 스위트 91 클래스 520 테스트 실패 0, 실제 렌더로도 확인(bootRun + curl 로 9개 breadcrumb 페이지 x ko/en 18케이스 전부 통과). 남은 불일치: /tools/date-diff 의 화면 breadcrumb(templates/tools/date-diff.html)은 여전히 홈/도구/날짜 계산기 3단계를 표시해 구조화 데이터와 어긋난다 — Google 권장사항상 일치가 바람직하나 오류는 아니고 UI 변경은 별도 판단으로 남긴다. 허브 페이지 신설은 기각이 아니라 보류이며, 만들면 헬퍼 한 곳 수정으로 3단계 복원 가능(ADR 0008 Superseded 검토)."

# =====================================================================
# IndexNow — 코드는 완비, 운영 플래그 미설정으로 도입 후 무동작
# =====================================================================
# 배경: "key 를 주입하면 바로 등록 가능한가" 라는 물음에서 출발. 확인해 보니 주입할
# key 가 없다 — cb257f0(2026-05-18) 한 커밋으로 구현이 끝났고 key/key-location/host/
# endpoint 가 application.yaml 에 리터럴로 들어가 있으며, 키 파일도 저장소에 커밋돼
# 프로덕션에서 이미 200 으로 서빙된다(본문 32바이트, 개행 없이 키와 일치 — 실측).
# 막고 있는 것은 운영 환경의 INDEXNOW_ENABLED 하나뿐이다. 값이 없으면 기본 false 로
# 떨어지고 IndexNowScheduler 의 ConditionalOnProperty 가 빈 등록 자체를 막는다.
# 함정: logs/stock-trading-*.log 에 IndexNow 200/403/host-mismatch 로그가 남아 있어
# 동작 중으로 오독하기 쉽지만, 수십 ms 간격으로 IndexNowServiceTest 6케이스의 기대
# 결과와 같은 순서로 찍힌 테스트 실행 흔적이다. 운영 제출 증거는 없다.
# 문서 결함: docs/data-analysis/README.md 가 존재하지 않는 IndexNow ADR 을 가리키고
# 있었다(grep -ri indexnow docs/adr 0건). 결정 소급 기록으로 참조를 사실로 만든다.
# 코드 변경 없음 — 결정 변경이 아니라 미기록 결정의 소급 문서화.
# 검증: IndexNowServiceTest tests=6 failures=0 errors=0 (결과 XML 확인),
#       프로덕션 키 파일 GET 본문 일치 실측.

# Commit 123 — ✅ DONE() docs(seo): IndexNow 결정 ADR 소급 작성 (ADR common/seo/0009) — 마지막 커밋(git-commit.md 포함)
git add docs/adr/common/seo/0009-indexnow-active-submission.md docs/adr/README.md docs/data-analysis/README.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md .env.example docs/guides/git-commit.md
git commit -m "docs(seo): IndexNow 결정 ADR 소급 작성 + 운영 활성화 안내 (ADR common/seo/0009)" -m "IndexNow 는 cb257f0 한 커밋으로 구현이 끝났고 이후 코드 변경이 없다 — key/key-location/host/endpoint 는 application.yaml 에 리터럴로 박혀 있고, 키 파일(static/1dfcb4404e1d4f6fae3423fd163f97b8.txt, 32바이트·개행 없음)은 StaticResourceController 의 명시 GetMapping 과 SecurityConfig permitAll 로 노출되며 프로덕션에서 실제 200 + 본문 일치를 확인했다. 흔히 남아 있으리라 예상하는 키 주입 단계가 없다는 뜻이다. 그런데도 도입 후 한 번도 제출된 적이 없는데, 운영 환경에 INDEXNOW_ENABLED 가 없어 기본값 false 로 떨어지고 IndexNowScheduler 의 ConditionalOnProperty 가 빈 등록 자체를 막기 때문이다 — docs/data-analysis/04-todo.md P1-4 가 이 상태를 운영 환경 확인 필요로만 열어두고 있었다. 판단을 흐리는 함정이 하나 있다: logs/stock-trading-*.log 에 IndexNow 200/403/host-mismatch 로그가 남아 동작 중처럼 보이지만, 수십 ms 간격으로 IndexNowServiceTest 6케이스의 기대 결과와 정확히 같은 순서로 찍힌 테스트 실행 흔적이다. 이번 커밋은 코드를 건드리지 않고 두 공백만 메운다. 첫째, 결정이 ADR 없이 코드에만 있었다 — 일 1회 03:30 KST 배치, 사이트맵 URL 전량 + bilingual 엔트리의 ?lang=en 확장을 중복 제거해 단일 POST, indexnow.host 와 다른 호스트 URL 사전 필터, 상태코드 예외화 억제 + 바깥 try/catch 로 전 구간 fail-soft, opt-in 기본 false. 게다가 docs/data-analysis/README.md 는 존재하지도 않는 IndexNow ADR 을 가리키고 있었다(grep -ri indexnow docs/adr 0건). common/seo/0009 를 소급 작성해 그 참조를 사실로 만들고 링크를 실제 파일로 교체했다. ADR 에는 결정뿐 아니라 이번에 드러난 한계도 남겼다 — 운영 플래그에 전적으로 의존(코드·키·키 파일이 다 있어도 켜지 않으면 아무 일도 없고 실제로 오래 방치됐다), indexnow.host 와 app.base-url 이 독립 설정이라 어긋나면 전량 탈락 후 WARN 한 줄만 남음, 수동 트리거 부재로 플래그를 켠 뒤 첫 제출까지 최대 24시간, ?lang=en 확장이 SitemapService.appendLangEn 과 IndexNowService.collectSitemapUrls 두 곳에 중복, 키 교체 시 5곳(yaml 2줄·정적 파일명·컨트롤러 매핑·SecurityConfig permitAll·테스트 상수) 동시 수정. 콘솔 등록은 제출의 전제조건이 아니라 결과 확인 수단이라는 점도 명시했다 — 제출 자체는 keyLocation 의 키 파일 검증으로 성립한다. 둘째, .env.example 은 값을 false 로 유지한 채 주석에 운영에서만 true 라는 사실만 덧붙였다 — 제출 URL 이 app.base-url 기준이라 개발 인스턴스에서 켜면 실서비스 URL 을 중복 제출하기 때문이고, 기본값을 끄는 것이 원래 결정의 취지다. ADR 신설에 따라 docs/adr/README.md 인덱스 4곳(매트릭스 common SEO 8→9·행 합계 17→18, SEO 열 합계 8→9, 총계 73→74, 시간순 표에 2026-05-18 행 삽입, 폴더 목록 8→9 ADRs)과 common/CLAUDE.md 의 IndexNowService 항목 ADR 링크(같은 절의 SeoMetadata·SitemapService 는 이미 링크를 달고 있어 일관성 회복)를 갱신했다. 루트 CLAUDE.md 는 사실 변경이 없어 무수정. 검증: main 소스 무변경이라 회귀 확인 목적으로 IndexNowServiceTest 를 FQCN 지정 실행 — tests=6 failures=0 errors=0 을 결과 XML 로 확인했고, 프로덕션 키 파일을 실제로 받아 본문이 키와 정확히 일치함을 실측했다. 운영 활성화 자체는 이 저장소 밖이다 — 원격 서버 .env 에 INDEXNOW_ENABLED=true 를 추가하고 재시작해야 하며, 다음 03:30 KST 로그에서 IndexNow submitted N urls, status=200 을 확인해야 완료다(N 은 현재 사이트맵 13엔트리 x ko/en 기준 26 근처). 아직 미검증이라 data-analysis README 의 IndexNow 상태 표기와 04-todo P1-4 는 이번에 바꾸지 않고 그대로 뒀다 — 로그로 확인된 뒤 갱신한다."

# =====================================================================
# sitemap.xml 점검 — 산출물은 정상, 공백은 문서·테스트 쪽
# =====================================================================
# 배경: sitemap.xml 전반 점검 요청. 코드 정적 분석 + 라이브 배포본 실측(전 URL 상태코드·
# 헤더·canonical·hreflang·title) + 공개 라우트 전수 대조 3방향으로 확인.
# 결과: 죽은 URL 0건(26개 전부 200), 색인 대상인데 누락 0건, 비대상인데 수록 0건,
# hreflang 상호참조·x-default·canonical 정합 100%. 연속 호출 lastmod 동일(요청마다
# 변동하는 안티패턴 아님). 즉 산출물 자체는 손댈 것이 없다.
# 발견 1(보류): lastmod 가 빌드 시각이라 콘텐츠 무관 재배포에도 24개 URL 이 전부 갱신
# 신호를 낸다. 점검 중 실제로 2026-07-26T03:26:02 → 2026-08-02T12:40:54 로 바뀌는 것을
# 관측했고, 최근 30일 커밋 52건 중 수록 페이지 콘텐츠를 바꾼 건 8건뿐이다. 다만 이는
# ADR-0003 이 Rationale 표에서 의식적으로 고른 트레이드오프이고 Google 은 부정확한
# lastmod 를 무시할 뿐 페널티가 없어, 설계 변경은 보류하고 비용만 수치로 남긴다.
# 발견 2: ADR-0003 과 Javadoc 이 코드와 불일치(insights fallback) → 정정.
# 발견 3: robots.txt Allow 목록 반쪽 미러 + 존재하지 않는 규칙을 가리키는 주석 → 정리.
# 발견 4: 사이트맵 HTTP 레벨/robots 정합 회귀 가드 전무 → 테스트 4종 신설.
# 검증: RED 선확인(없는 경로 수록 → 302 로 실패 / robots 에 Disallow 추가 → 차단 판정
#       true 로 실패) 후 임시 변경 되돌리고 GREEN. 사이트맵 출력 바이트 변화 없음.

# Commit 124 — ✅ DONE() docs(seo): sitemap.xml 점검 보고서 + 회귀 가드 4종
# 주의: git-commit.md 는 아래 Stock 유니버스 배치가 뒤에 추가되어 이 커밋에서 빠지고
#       배치 마지막 커밋(Commit 128)이 소유한다.
git add docs/audit/sitemap-audit-2026-08-02.md src/test/java/me/singingsandhill/calendar/common/presentation/controller/SitemapEndpointTest.java src/main/resources/static/robots.txt docs/adr/common/seo/0003-trustworthy-sitemap-lastmod.md src/main/java/me/singingsandhill/calendar/common/application/service/SitemapService.java
git commit -m "docs(seo): sitemap.xml 점검 보고서 + HTTP/robots 회귀 가드 4종" -m "sitemap.xml 을 코드·라이브 배포본·커버리지 3방향으로 점검했다. 산출물은 손댈 것이 없었다: 수록 26 URL(13 엔트리 x ko/en) 전부 HTTP 200 으로 죽은 URL 0건, index,follow 페이지 집합과 사이트맵 집합이 정확히 일치해 누락·과다 수록 0건, 블록당 ko/en/x-default 3개씩 78개 hreflang 이 자기 자신을 포함해 상호참조를 충족하며, 표본 5개 페이지의 canonical 이 사이트맵 loc 과 문자 단위로 같고 en URL 은 self-canonical 이다. 13개 경로 전부 ko/en 의 title 과 html lang 이 달라 hreflang 이 빈 약속도 아니다. 연속 3회 호출 lastmod 가 동일해 playbook Case 6 이 경고하는 요청마다 변동하는 형태도 아니다. 그래서 이번 커밋은 사이트맵 생성 로직을 전혀 건드리지 않으며 출력 바이트도 그대로다. 발견은 네 가지다. (1) lastmod 가 BuildProperties 즉 배포 시각이라 콘텐츠와 무관한 재배포에도 정적 24개 URL 이 전부 갱신 신호를 낸다 — 점검 도중 값이 2026-07-26T03:26:02 에서 2026-08-02T12:40:54 로 바뀌는 것을 실제로 관측했고, 최근 30일 커밋 52건 중 수록 페이지의 템플릿·메시지를 바꾼 것은 8건뿐이라 나머지 배포는 전부 허위 갱신이다. 다만 이것은 결함이 아니라 ADR-0003 이 Rationale 표에서 페이지별 콘텐츠 hash 추적을 인프라 비용으로 기각하며 고른 트레이드오프이고, Google 은 부정확한 lastmod 에 페널티를 주는 게 아니라 그 값을 무시하므로 손해가 신호 하나를 잃는 선에서 끝난다 — 설계 변경은 보류하고 비용을 처음으로 수치화해 보고서에 남기며 대안 3안(페이지별 실제 수정일 맵/lastmod 제거/git 파생)을 비교표로 정리했다. (2) ADR-0003 과 SitemapService Javadoc 이 둘 다 insights 데이터가 없으면 buildTime fallback 이라고 적었지만 코드는 엔트리 자체를 싣지 않는다 — 2026-05-28 061626f 에서 sitemap 광고와 noindex 응답이 모순된다는 이유로 바뀌었는데 ADR 이 따라가지 않았다. 결정 변경이 아니라 문서를 코드에 맞추는 정정이라 신규 ADR 없이 ADR 본문에 정정 각주를 달고 Javadoc 문장을 고쳤다. (3) robots.txt 의 Allow 목록에서 /about·/faq·/tools/date-diff 가 빠져 반쪽 미러였다 — default-allow 라 크롤링 동작에는 무해하지만 목록만 보면 이 셋이 허용 대상이 아닌 것처럼 읽힌다. 세 줄을 채우고, 이미 삭제된 Allow: /runners/runs/ 규칙을 근거로 들던 주석을 최장 패턴 우선 규칙 설명으로 교체했다. Disallow 규칙과 연도 열거는 ADR-0005 결정 그대로 두었다. (4) 기존 테스트 16개(hreflang 14 + whitelist 2)는 생성된 XML 문자열만 보고 그 XML 이 실제로 서빙되는지·광고하는 URL 이 존재하는지·robots.txt 에 막히지 않는지는 아무도 확인하지 않았는데, 이 저장소의 대표적 색인 사고 두 건이 정확히 그 공백에서 났다(2026-01 http/https 불일치 ADR-0002, 2026-04 Disallow: /*/* 로 콘텐츠 페이지 전면 차단 ADR-0005). SitemapEndpointTest 4종을 신설한다: sitemap.xml 이 200/application-xml/max-age=86400 으로 서빙되는지, 모든 loc 이 MockMvc 로 2xx 를 응답하는지(playbook 미완 체크리스트의 모든 URL 200 자동 검증을 닫는다), robots.txt 가 사이트맵 URL 을 막지 않는지(Allow/Disallow 최장 패턴 우선이라는 Google 해석을 그대로 구현해 판정), Sitemap: 줄이 app.base-url 과 일치하는지. 기존 통합 테스트 패턴(SpringBootTest + AutoConfigureMockMvc + ActiveProfiles test + MockMvc 주입)을 그대로 재사용했다. 검증은 RED 선확인부터 했다 — 존재하지 않는 경로를 getSitemapEntries 에 넣으면 302 를 받아 200~299 기대에서 실패하고, robots.txt 에 그 경로 Disallow 를 넣으면 차단 판정이 true 가 되어 실패한다. 두 임시 변경을 되돌린 뒤 사이트맵 3개 테스트 클래스 20개(신규 4 + hreflang 14 + whitelist 2) 전부 GREEN, 결과 XML 로 실행 개수 확인. 조치하지 않고 보고서에만 남긴 것: escapeXml 이 changefreq/priority/lastmod 에 미적용(전부 코드 내 리터럴이라 실제 위험 없음), hreflangEntryCountReasonable 의 12*2*3 하드코딩이 화이트리스트 테스트와 중복(의도된 가드라 유지), 사이트맵 응답마다 findLatestActivity SQL 2회(24시간 캐시 + 저트래픽이라 무시 가능), 그리고 범위 밖 두 건 — /stock* 의 noindex 가 SeoMetadata 가 아니라 템플릿 하드코딩에만 있고 Disallow: /stock/ 이 맨 URL /stock 을 매칭하지 못한다는 점, /use-cases·/tools 허브가 404 라는 ADR-0008 보류 항목."

# =====================================================================
# Stock 봇 — 2026-08-03 유니버스 붕괴 + 스크리닝 관측성
# =====================================================================
# 배경: logs/stock-screening-snapshot-2026-08-03.log 를 출발점으로 개선점 도출 요청.
# 로그가 드러낸 것: 08:30 "Volume-rank returned 1 codes" → "Universe refreshed: 1 codes
# (pinned=0, fallback=0, rank=1)" → 09:20 재조회 없이 "Starting gap screening for 1 stocks"
# → 유일 픽 252670(ETF) 선정. 하루치 유니버스가 1종목이었는데 WARN 은 한 줄도 없었다.
# 원인: 폴백 조건(rankCodes.isEmpty())과 재시도 조건(rankApi == 0)이 둘 다 "비었는가"만
# 검사한다. 1건은 비어 있지 않으므로 70종목 안전망과 ADR-0006 재시도가 동시에 꺼졌다.
# 즉 성공한 API 호출 하나가 안전망 두 개를 껐다.
# 선행 진단 정정: 2026-07-20~23 의 4일 연속 Selected: 0 은 이번 범위가 아니다 — 원인이던
# 체결강도 전 종목 0 은 274f9de(2026-07-29, ADR infrastructure/0007)에서 이미 해결됐고
# 08-03 의 str=164.61 이 정상 동작 증거다. 다만 그 4일의 원인을 로그로 특정할 수 없었던
# 이유(dataInsufficient 가 서로 다른 두 원인을 합산)는 그대로 남아 있어 Commit 126 이 고친다.
# 사용자 결정으로 범위를 P0 유니버스 + P3 관측성으로 한정. 보류 항목은 결과 보고 참고.
# 검증: RED 선확인(임계 로직 변경 전 08-03 재현 테스트 2개만 실패, 나머지 14개 통과) 후
#       GREEN. 전체 스위트 93 클래스 / 534 테스트 / failures 0 / errors 0 을 결과 XML 로 확인.

# Commit 125 — ✅ DONE() fix(stock): 유니버스 열화 판정을 top-N 미달로 (ADR stock/algorithm/0010)
# 주의: GapPullbackBotService 는 Commit 127 의 sendScreeningResult 시그니처 변경도 흡수.
#       stock/CLAUDE.md 는 Commit 126·127·128 의 문서 변경도 흡수.
git add src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/test/java/me/singingsandhill/calendar/stock/application/UniverseBuilderTest.java docs/adr/stock/algorithm/0010-universe-degradation-threshold.md docs/adr/stock/algorithm/0005-dynamic-universe-volume-rank.md docs/adr/stock/algorithm/0006-universe-rank-retry-at-screening.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/stock/CLAUDE.md src/main/java/me/singingsandhill/calendar/stock/application/CLAUDE.md
git commit -m "fix(stock): 유니버스 열화 판정을 0건이 아니라 top-N 미달로 (ADR stock/algorithm/0010)" -m "2026-08-03 거래량순위가 30건 요청에 1건을 반환했고, 그 하루의 스크리닝 유니버스가 1종목이 됐다. 1건은 비어 있지 않다는 것이 유일한 이유다 — UniverseBuilder.refresh 의 usedFallback = rankCodes.isEmpty() 가 false 라 70종목 정적 안전망을 통째로 건너뛰었고, refreshIfDegraded 의 rankApi == 0 도 false 라 ADR-0006 이 도입한 09:20 재시도마저 발동하지 않았다. 성공한 API 호출 하나가 안전망 두 개를 동시에 끈 셈이고, 그 사실을 알리는 WARN 은 없었다. KisRestClient 의 축소 응답 로그가 DEBUG 였기 때문이다. 대칭 위험도 있었다: 07-20 형태의 날(08:30 rank 0건 → fallback 70)에 09:20 재시도가 3건만 반환하면 재시도가 폴백을 대체하는 구조라 유니버스가 70에서 3으로 줄어든다. 열화의 정의를 비었을 때에서 요청한 top-N 에 미달할 때로 바꾸고, 부분 응답은 폴백을 대체하지 않고 합집합으로 보강하게 했다. usedFallback = rankCodes.size() < Math.max(rank-api-top, 1) 이며 Math.max(..,1) 은 rank 비활성(rank-api-top=0)일 때 0 < 1 이 되어 기존 폴백 동작을 그대로 유지한다. refreshIfDegraded 는 rankApi < rank-api-top 으로 판정한다. codes 가 이미 LinkedHashSet 이라 합집합의 중복 제거는 공짜다. 함께 프리마켓에서 거래량순위 호출을 걷어냈다 — refreshStaticOnly() 를 신설해 08:30 에는 pinned ∪ fallback 만 만든다. 그 시각엔 당일 거래량이 없어 rank 가 구조적으로 0~1건이고(ADR-0006 이 4거래일 연속 관측한 그대로, 08-03 은 1건), 그 결과가 스냅샷에 남는 것 자체가 오염원이다. 동적 소스는 09:20 refreshIfDegraded 단독 책임이 된다. 축소 응답을 더 이상 조용히 넘기지 않는다: KisRestClient.getTopVolumeCodes 는 codes.size() < count 이면 WARN 으로 올리고, UniverseBuilder 는 UNIVERSE_DEGRADED 이벤트(requested/returned/fallback)를 남긴다. ADR-0005 가 폴백 조건을 미설정/비정상 rt_cd/예외/0건으로, ADR-0006 이 재시도 조건을 rankApi == 0 으로 명시하고 있어 이번 변경은 CLAUDE.md 동기화 표의 결정 변경에 해당한다 — ADR stock/algorithm/0010 을 신설하고 0005·0006 의 Status 에 보강 관계를 명시했다. 테스트는 RED 선확인부터 했다. refreshStaticOnly 만 먼저 추가하고 임계 로직은 그대로 둔 상태로 돌려, 08-03 을 재현하는 두 케이스(unionsStaticPoolWhenRankResponseIsPartial, retriesRankAtScreeningWhenSnapshotRankIsPartial)만 실패하고 나머지 14개가 통과하는 것을 확인한 뒤 임계를 고쳐 16개 전부 GREEN 으로 만들었다. 기존 계약을 고정하던 테스트 3개도 새 계약으로 갱신했다 — usesVolumeRankWhenEnabled 는 rankTop 을 30 으로 두고 2건만 스텁한 뒤 fallback 이 0 이기를 단정했는데, 그것이 바로 이번에 고친 동작이라 usesVolumeRankWhenResponseIsComplete 로 이름과 전제를 바꿨다. 범위 밖으로 남긴 것: 08-03 의 유일 픽 252670 이 ETF 라는 사실 자체다. 거래량순위 쿼리의 FID_TRGT_EXLS_CLS_CODE 가 0000000000(제외 없음)이라 ETF/ETN 이 유니버스에 들어오는데, 비트마스크 자릿수를 KIS 공식 스펙으로 확인하지 못했다(웹 검색으로 우선주·SPAC·ETF 를 제외하는 파라미터라는 방향까지만 확인). 스펙 미검증 필드 매핑으로 두 번 사고가 난 전례(274f9de 의 cttr)가 있어 추측으로 넣지 않고 ADR-0010 의 후속 항목으로 남겼다. FID_BLNG_CLS_CODE 를 평균거래량에서 거래대금 순위로 바꾸는 것도 같은 이유로 보류했다."

# Commit 126 — ✅ DONE() refactor(stock): 스크리닝 탈락 버킷을 원인별로 분리
git add src/main/java/me/singingsandhill/calendar/stock/application/service/ScreeningService.java src/main/java/me/singingsandhill/calendar/stock/application/observability/StockBotMetrics.java src/test/java/me/singingsandhill/calendar/stock/application/service/ScreeningStatsBucketTest.java
git commit -m "refactor(stock): 스크리닝 탈락 버킷을 원인별로 분리 (동작 불변)" -m "2026-07-20~23 의 4거래일 연속 Selected: 0 에서 로그만으로 근본 원인을 특정할 수 없었다. 매일 gapFiltered + dataInsufficient 가 정확히 70(유니버스 전체)이었는데, dataInsufficient 하나에 시가 미확정과 체결강도 미집계가, gapFiltered 하나에 갭 하한 미달과 상한 초과가 합산돼 있었기 때문이다. 원인이 다르면 조치가 다르다 — 체결강도 0 은 데이터 소스 문제였고 실제로 ADR infrastructure/0007 로 TR 을 교체해야 했지만, 갭 하한 미달이었다면 유니버스 문제, 상한 초과였다면 임계 문제로 완전히 다른 곳을 봐야 했다. ScreeningStats 의 두 카운터를 네 개로 쪼갠다: openPriceMissing(시가 미확정) / zeroTradeStrength(inquire-ccnl tday_rltv 부재) / gapBelowFloor / gapAboveCeiling. 합계는 dataInsufficient()·gapFiltered() 파생 메서드로 유지해 요약 로그의 기존 필드가 그대로 남는다. 노출 지점 세 곳을 함께 갱신했다: 요약 로그는 합계 옆에 내역을 괄호로 병기하고, 침묵 실패 가드 WARN 은 최다 버킷 대신 네 버킷을 모두 인쇄하며, SCREENING_SUMMARY 이벤트는 원인별 키로 바꿨다. StockBotMetrics.recordScreeningResult 시그니처와 ScreeningSnapshot 레코드도 원인별 필드를 받도록 넓혔고, 여기에도 dataInsufficient()·gapFiltered() 파생 접근자를 둬 이 레코드를 그대로 직렬화하는 GET /api/stock/bot/status 응답의 하위호환을 지켰다. legacy 경로(scoring.enabled=false)의 screenSingleStock 도 같은 버킷으로 옮겼다 — 같은 필드를 두 경로가 공유하므로 한쪽만 바꾸면 합계가 어긋난다. 선정 로직은 한 줄도 건드리지 않았다. 갭 판정을 하한/상한 두 분기로 나눈 것이 유일한 제어흐름 변경인데 조건식의 합집합은 기존 || 와 동일하다. 회귀 가드로 ScreeningStatsBucketTest 4종을 신설했다: 체결강도 미집계가 zeroTradeStrength 로만 잡히는지, 시가 미확정이 openPriceMissing 으로만 잡히고 체결강도 조회까지 가지 않는지(ADR 0007 의 콜 예산 규약), 갭 하한 미달과 상한 초과가 각각 다른 버킷으로 가는지, 스냅샷의 파생 합계가 맞는지. 갭 상한 케이스는 실효 임계가 scoring.floor-max-gap(15)이지 yaml 에 보이는 legacy max-gap-percent 가 아니라는 것도 함께 단정한다. 기존 ScreeningFloorStrengthTest 4개는 무수정으로 GREEN 이며, 이것이 동작 불변의 근거다."

# Commit 127 — ✅ DONE() feat(stock): 09:20 스크리닝 메일에 유니버스 크기 + 실효 임계
# 주의: GapPullbackBotService 의 sendScreeningResult 호출부는 Commit 125 가 소유.
git add src/main/java/me/singingsandhill/calendar/stock/application/service/StockMailService.java src/test/java/me/singingsandhill/calendar/stock/application/StockMailServiceTest.java
git commit -m "feat(stock): 09:20 스크리닝 메일에 유니버스 크기 + score 모드 실효 임계" -m "2026-08-03 에 유니버스가 1종목이었다는 사실이 메일 어디에도 없었다. 메일 자체는 발송된다 — buildSubject 에 0건 전용 제목까지 있어 선정 0~1건인 날에도 운영자에게 도착한다. 그런데 본문에는 선정된 종목만 있어서, 선정이 1건인 것이 임계가 빡빡해서인지 볼 종목이 1개뿐이어서인지 구분할 방법이 없었다. sendScreeningResult 에 UniverseBuilder.Snapshot 을 넘겨 본문 최상단에 유니버스 N종목 (거래량순위 R, 정적폴백 F, 핀 P) 한 줄을 찍는다. 크기가 정적 안전망(stock.universe.fallback-codes)보다 작으면 빨간 굵은 글씨로 경고 문구를 덧붙인다 — 열화 판정을 top-N 미달로 바꾼 뒤에는 rank 가 부족해도 폴백이 합집합으로 들어오므로, 유니버스가 안전망보다 작다는 것은 설정이나 API 응답에 문제가 있다는 뜻이다. 임계값은 설정에서 유도하므로 매직넘버가 없다. 두 번째로 스크리닝 조건 블록이 인쇄하던 값을 실제 적용되는 값으로 바꿨다. 이 블록은 min-gap-percent / max-gap-percent / min-market-cap / min-trade-value / min-trade-strength 를 찍었는데, 이들은 legacy 모드(scoring.enabled=false) 전용이고 운영 기본값인 score 모드에서는 아무 역할도 하지 않는다. 08-03 에 실효 갭 상한은 scoring.floor-max-gap 인 15% 였지만 메일은 legacy max-gap-percent 인 10% 를 인쇄했다 — gap 9.76% 픽을 받아 든 운영자가 상한 근처라고 오독하기 딱 좋은 상태였다. scoring.enabled 로 분기해 score 모드에서는 floor-gap-percent ~ scoring.floor-max-gap, floor-trade-strength, scoring.floor-min-market-cap, 그리고 선정 관문인 min-score-threshold 와 signal-min-score 를 인쇄한다. legacy 모드 출력은 그대로 남겨 두 경로 모두 자기 임계를 보여준다. 실효 임계 12개를 application.yaml 로 끌어올리는 작업(현재 Java 기본값에만 존재)은 이번 범위가 아니라 손대지 않았다 — 여기서는 메일이 인쇄하는 값을 실제 적용되는 값에 맞추는 데까지만 했다. 테스트는 기존 attachmentIsNamedAsSnapshotNotAsDailyLogFile 의 발송·파싱 부분을 sendAndCaptureHtml 헬퍼로 뽑아 재사용하고 2개를 추가했다: 유니버스 1종목 스냅샷을 넘기면 본문에 크기와 내역이 찍히고 정적 안전망(3종목)보다 작아 경고 문구가 붙는지, score 모드에서 floor-max-gap 과 신호점수가 인쇄되고 legacy 전용 최소 거래대금 항목은 나오지 않는지. 3개 전부 GREEN."

# Commit 128 — fix(stock): time-decay 종점을 운영 창에서 유도 — 마지막 커밋(git-commit.md 포함)
git add src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java src/test/java/me/singingsandhill/calendar/stock/application/StockRiskServiceTimeDecayTest.java docs/adr/stock/algorithm/0001-time-config-and-magic-numbers-externalized.md docs/adr/stock/algorithm/0009-entry-floor-and-trailing-cost-alignment.md docs/guides/git-commit.md
git commit -m "fix(stock): time-decay 최소수익 종점을 하드코딩 대신 운영 창에서 유도" -m "calculateTimeDecayThreshold 가 LocalTime.of(9, 10) 과 LocalTime.of(15, 15) 를 코드에 박고 있었다. 봇의 실제 운영 창은 trading.trading-loop-start(09:20)에서 exit.final-exit-time(11:20)까지 두 시간이므로, 365분 곡선 중 130분만 지나간 채 강제청산된다. 11:20 시점 임계가 0.358% 로 곡선의 약 36% 지점이고, 설정에 적힌 minProfitThresholdLate 0.1% 와 그 뒤의 0% 종점은 도달할 수 없는 값이었다 — 장 후반으로 갈수록 익절을 쉽게 한다는 설계 의도가 실제로는 3분의 1만 작동한 셈이다. 두 종점을 stockProperties 에서 읽는다. executeTradingLoop 가 이미 같은 두 키를 LocalTime.parse 로 읽고 있어 같은 패턴을 그대로 쓰며, getTradingLoopStart() 는 screeningEnd(09:20) 폴백이 있고 finalExitTime 은 Java 기본값 11:20 이라 둘 다 null 이 될 수 없다. 파싱 실패는 이미 트레이딩 루프에서 먼저 터지므로 새 실패 모드도 아니다. 이것은 동작 변경이다. 같은 시각의 임계가 09:20 에 0.489%에서 0.500%로, 10:20 에 0.423%에서 0.300%로, 11:20 에 0.358%에서 0(구간 종점)으로 바뀐다. TP1·TP2·TP3 공통 순이익 게이트가 장 후반에 실제로 완화되며, 특히 작은 이익에서도 발동할 수 있는 TP2(당일고가 도달)에 영향이 크다. ADR algorithm/0001 이 매직넘버 외부화를 결정하고도 남겨둔 마지막 상수라 그 ADR 의 Status 에 정리 사실을 적었고, 비용 게이트를 다룬 ADR algorithm/0009 의 Status 에는 위 세 시각의 임계 변화를 수치로 남겼다 — 손익분기 계산의 전제가 바뀌기 때문이다. StockRiskServiceTimeDecayTest 는 09:10/15:15 상수를 단정하고 있어 새 종점으로 갱신했고, 종점이 설정에서 온다는 것 자체를 단정하는 케이스를 추가했다 — final-exit-time 을 10:20 으로 좁히면 09:50 의 임계가 0.004 가 아니라 0.003 이 되어야 한다. 5개 전부 GREEN 이고, 같은 클래스를 공유하는 StockRiskServiceSlippageGateTest·StockRiskServiceTimeExitTest 도 무수정 GREEN 이다. 배치 마무리로 전체 스위트를 돌려 93 클래스 534 테스트 failures 0 errors 0 을 결과 XML 로 확인했다."

# =====================================================================
# 코인 봇 — 신호 품질 분석 페이지 + 결정 입력 영속화 (2026-08-06)
# =====================================================================
# 배경: "메일 기능을 활용해 봇 개선용 DB 값을 첨부 발송하면 어떨까" 라는 요청에서 출발.
# 조사 결과 전제가 유리한 쪽으로 틀렸다 — SignalService.generateSignal 이 매 루프 틱마다
# 조건 없이 trading_signals 에 1행을 남기고(HOLD 포함, ~1,440행／일) 삭제 스케줄러가 없어
# 영구 보관된다. 8개 구성점수·원지표·그 시점 현재가가 다 있고, current_price 가 매 분 있어
# 그 테이블 자체가 1분 가격 시계열이다. 즉 필요한 데이터셋은 이미 존재했고 읽을 수단만 없었다.
# 사용자 결정: 산출물은 메일이 아니라 /trading/analytics 관리자 페이지. 데이터 기록 공백은
# 전부 수정. 주기는 스케줄러 없이 온디맨드(권고 채택).
# 동기: docs/audit/coin-trading-profit-audit-2026-05-30.md 가 P2-1(거래량 다이버전스 ±20
# 과대가중)을 "백테스트 없는 prior" 로 low 강등한 채 남겼고, remaining-work.md:100 이
# "나머지 P2 는 라이브 전환 이후 데이터 기반으로" 라고 적어뒀다. 그 데이터가 지금 쌓여 있다.
# 검증: 커밋별 RED 선확인 후 GREEN. 전체 스위트 99 클래스 ／ 585 테스트 ／ failures 0 ／
#       errors 0 을 build/test-results/test/*.xml 로 확인 (기준선 93 ／ 534).
# 주의: 이 배치는 Commit 126~128(stock) 이 아직 미실행인 상태에서 작성됐다. 순서대로
#       126~128 을 먼저 실행할 것. 이 배치의 add 목록에는 stock 파일이 하나도 없어
#       서로 파일이 겹치지 않는다. 단 Commit 128 이 docs/guides/git-commit.md 를 포함하므로
#       128 을 실행하면 아래 129~135 섹션도 함께 커밋된다 — 정상이며, 135 는 그 뒤의
#       DONE 마커 갱신분을 담게 된다.
# 워킹트리 잡음: docs/{stock/bot.md, datedate/architecture-review.md, prompts/*.md} 6개는
#       내용 변경 0 인데 통째로 CRLF 로 바뀌어 raw diff 2,242줄이 잡힌다(.gitattributes 에
#       *.md 규칙 없음). 이 배치에서 전부 제외했다. 별도 위생 커밋으로 LF 복원 + 규칙 추가 권장.

# Commit 129 — fix(trading): 캔들 보관 기간 외부화 + 7일→90일 (ADR infrastructure/0005)
# 주의: application.yaml 은 Commit 145 의 stock.universe 주석 정정 2줄도 흡수한다(주석만).
git add src/main/java/me/singingsandhill/calendar/trading/infrastructure/config/TradingProperties.java src/main/java/me/singingsandhill/calendar/trading/application/service/CandleService.java src/main/resources/application.yaml src/test/java/me/singingsandhill/calendar/trading/application/service/CandleServiceRetentionTest.java docs/adr/trading/infrastructure/0005-candle-retention-for-analysis.md
git commit -m "fix(trading): 캔들 보관 기간 외부화 + 7일→90일 (ADR trading／infrastructure／0005)" -m "CandleService.cleanupOldCandles 가 LocalDateTime.now().minusDays(7) 을 코드에 박고 있었고, 호출자인 CandleScheduler 의 자정 크론에는 다른 잡과 달리 bot.enabled 가드가 없어 봇을 꺼둔 기간에도 실행됐다. 그래서 1분봉 보관 지평이 영구히 1주로 고정돼, 지표 파라미터를 바꿔 과거에 다시 돌려보는 것이 구조적으로 불가능했다. Bithumb 캔들 API 는 과거 구간을 무한정 주지 않으므로 삭제된 구간은 복구할 수 없다 — 이 항목만은 결정을 미루는 것 자체가 매일 손실이라 배치 첫 커밋으로 올린다. TradingProperties.Bot 에 candleRetentionDays=90 을 추가하고 application.yaml 의 trading.bot.candle-retention-days 와 값을 일치시켰다. 새 중첩 클래스를 만들지 않고 Bot 에 둔 것은 그쪽이 이미 market·maxPositions·쿨다운 등 운영 파라미터를 담은 묶음이기 때문이고, Java 기본값을 yaml 과 맞춘 것은 P1-9 의 교훈(키 누락 시 구식 값으로 폴백) 때문이다. 90일 근거는 용량이다 — 1분봉 x 1시장 = 1,440행／일이므로 90일이면 129,600행, 행당 약 200B 로 26MB 미만이다. 현재 DB 파일이 643KB 라 지배적 테이블이 되지만 여전히 사소하고, 365일(약 105MB)은 파일을 다루기 번거롭게 만든다. deleteByDateTimeBefore 는 candle_date_time 단독 조건인데 기존 유니크 인덱스가 market 으로 시작해 이 조건에 쓰이지 않으므로 자정 삭제는 풀스캔이지만, 13만 행에서는 밀리초 단위라 인덱스를 따로 추가하지 않았다. CandleScheduler 의 bot.enabled 가드 부재는 이번 범위 밖으로 남겼다 — CLAUDE.md·모듈 CLAUDE.md 가 명시적으로 문서화한 알려진 동작이고 보관기간을 90일로 늘리면 실질 영향이 거의 사라진다(다만 90일 넘게 꺼두면 여전히 손실이라 ADR Consequences 에 후속으로 적었다). 테스트는 RED 를 두 단계로 확인했다 — 설정 키만 먼저 추가하고 CandleService 는 그대로 둔 상태로 CandleServiceRetentionTest 를 돌려, 2개 중 cutoffComesFromConfigNotHardcodedSevenDays 하나만 실패(컷오프가 여전히 7일 전)하고 javaDefaultMatchesYamlOperationalValue 는 통과하는 것을 본 뒤 CandleService 를 고쳐 2개 전부 GREEN 으로 만들었다. 이 테스트가 단정하는 것은 값이 90이라는 사실이 아니라 값이 설정에서 온다는 사실이다 — 임의의 두 보관기간(1일·90일)에 대해 컷오프가 그만큼 움직이는지를 호출 전후 시각 사이 구간으로 검사한다(stock 의 decayEndpointsComeFromConfigNotConstants 와 같은 기법)."

# Commit 130 — feat(trading): 결정 입력 영속화 — signal_id·ATR·거래량 맥락·적용 주문비중 (ADR observability/0002)
# 주의: TradeJpaEntity·SignalJpaEntity 는 Commit 132 의 @Index 추가도 흡수한다.
#       TradingBotService 는 Commit 131 의 executeTradeLoop 재구조화도 흡수한다.
#       SignalRepositoryAdapter 는 Commit 133 의 투영 매핑도 흡수한다.
#       SignalService·IndicatorService·TradingBotService 는 Commit 144 의 주석 수치 정정도
#       흡수한다(주석만, 동작 불변 — TradingBotService 는 P2-12 javadoc 재부착 포함).
#       TradingBotService 는 Commit 136 의 캔들 실패 가드(ADR risk/0006)도 흡수한다 —
#       executeTradeLoop 1단계 try/catch + candlesFresh 조기 return. 이 커밋의 메시지에는
#       그 근거가 없으므로, 그 변경의 판단 근거는 Commit 136 을 볼 것.
#       (git add -p 미지원 환경이라 같은 파일의 두 관심사를 분리할 수 없다.)
git add src/main/java/me/singingsandhill/calendar/trading/domain/trade/Trade.java src/main/java/me/singingsandhill/calendar/trading/domain/signal/Signal.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/entity/TradeJpaEntity.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/entity/SignalJpaEntity.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/adapter/TradeRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/adapter/SignalRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java src/main/java/me/singingsandhill/calendar/trading/application/service/SignalService.java src/main/java/me/singingsandhill/calendar/trading/application/service/IndicatorService.java src/main/java/me/singingsandhill/calendar/trading/application/dto/IndicatorResult.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceSignalLinkTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/SignalServiceWeightTest.java docs/adr/trading/observability/0002-decision-input-persistence.md
git commit -m "feat(trading): 결정 입력을 결정 시점에 영속화 (ADR trading／observability／0002)" -m "trading_signals 는 결정 입력이 풍부한데 결과 쪽에서 원인을 되짚을 방법이 없었다. trading_trades 에 남는 신호 정보는 signal_score 정수 하나와 signal_reason 문자열뿐이고 signal_id 가 없다. 반대 방향도 막혀 있었다 — trading_signals.executed 는 Signal.markExecuted() 가 저장소 전체에서 한 번도 호출되지 않아 항상 false 인 죽은 컬럼이다. 그래서 이 체결이 어느 신호에서 나왔는가를 타임스탬프 근사로만 추정할 수 있었다. 계산해놓고 버리는 값도 있었다. ATR 은 calculateDynamicOrderRatio 가 주문 크기를 정하는 데 쓰지만 저장하지 않고, 실제 적용된 비중도 마찬가지라 포지션 사이징 결정을 DB 로 재구성할 수 없었다. volumeMa／currentVolume 은 IndicatorResult 에는 있는데 SignalJpaEntity 에 컬럼이 없었고, 이 둘은 MA60 하회 시 매수 확인 게이트의 거래량 스파이크 분기 입력이라 없으면 임계 반사실 분석에서 그 분기를 판정할 수 없다. trading_trades 에 signal_id 와 order_ratio 를, trading_signals 에 atr·atr_percent·volume_ma·current_volume 을 추가했다. 전부 nullable 이라 ddl-auto=update 가 ALTER TABLE ADD COLUMN 으로 처리하며 기존 행은 null 로 남는다. signal_id 는 신호 기반 매수·매도에서만 채우고 리스크 청산·리밸런싱·수동 매매는 의도적으로 null 로 둔다 — null 자체가 신호로 난 체결이 아니라는 정보다. 특히 청산 틱의 동시각 신호를 청산 Trade 에 붙이지 않는다: 그건 이 신호가 이 청산을 유발했다는 거짓을 기록하는 것이고 signal_id 기반 집계를 전부 오염시킨다. order_ratio 는 매수에만 채우며 저장 전 setScale(4, HALF_UP) 을 건다 — 보간 결과가 double 이라 ATR 1.5% 에서 0.29999999999999993 이 그대로 들어가기 때문이고, 테스트가 이 값을 0.30 으로 비교하며 scale 이 4인지까지 단정한다. 필드 추가는 생성자가 아니라 이름 있는 setter 로 했다. Signal 생성자는 이미 25인자인데 여기 4개를 더 붙이면 29인자 위치 호출이 되고, 인접한 BigDecimal 둘이 뒤바뀌어도 컴파일이 통과해 그 뒤 모든 행이 조용히 오염된다 — 이 배치에서 가장 위험한 실패 모드라 판단했다. 같은 클래스의 id·executed 와 Trade 의 positionId·signalScore 가 이미 가변이라 선례도 있다. 다섯 번째 필드가 필요해지면 그때 ScoreBreakdown／IndicatorSnapshot 중첩 레코드로 실제 분해를 하는 것을 ADR 후속으로 적었다. ATR 은 IndicatorService.calculate() 가 이미 로드한 80봉으로 계산한다 — calculateATRPercent(market) 을 부르면 같은 틱에 캔들을 두 번 읽는다(그 메서드는 자체적으로 19봉을 다시 읽는다). 주문 사이징이 쓰는 calculateATRPercent 자체는 건드리지 않았다: 그쪽은 excludeFormingCandle 을 따르지 않아 그 플래그를 켜면 IndicatorResult.atrPercent() 와 값이 갈릴 수 있고, 그래서 재유도 대신 실제 적용된 비중을 따로 기록하는 쪽을 골랐다. IndicatorResult 의 새 인자 atr 은 기존 10개 뒤에 append 했다 — 중간에 끼우면 위치 인자가 밀린다. 이 레코드의 생성 지점은 저장소 전체에 IndicatorService:51 과 SignalServiceWeightTest:25 둘뿐이라 비용이 낮았다. 테스트는 TradingBotServiceSignalLinkTest 6종을 신설했다: 매수·매도가 signal_id 를 남기는지, ATR 미확보 시 기본 비율 0.25 와 고변동성 0.15 가 기록되는지, 보간값이 4자리로 반올림돼 저장 가능한 형태가 되는지, 그리고 수동 매도가 signal_id 를 null 로 두는지(가드 — 변경 전후 모두 통과). manualSell 케이스는 P0-3 킬스위치 때문에 bot.enabled=true 로 열어야 실제 주문 경로를 탄다는 점을 먼저 확인했다. 기존 SignalServiceWeightTest 는 IndicatorResult 인자 추가에 맞춰 null 하나를 더했고 단정은 그대로다. 회귀 증거로 주문 신뢰성 테스트군(TradingBotServiceOrderReconciliationTest·TradingV2LostResponseSweepTest·TradingBotServiceExecutedVolumeTest)이 무수정 GREEN 임을 확인했다 — 새 필드는 순수 데이터라 §8-B 스윕 경로가 읽지 않는다."

# Commit 131 — fix(trading): 리스크 청산 틱에도 신호 기록 (ADR observability/0003)
# 주의: TradingBotService 본체 변경은 Commit 130 이 소유. 여기는 테스트와 ADR 만.
#       observability/0003 은 Commit 136 의 "후속 ② 는 risk/0006 으로 닫혔다" 갱신도 흡수한다.
git add src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceLoopSignalRecordingTest.java docs/adr/trading/observability/0003-signal-series-continuity-on-risk-exit.md
git commit -m "fix(trading): 리스크 청산 틱에도 신호 기록 — 관측성은 주문 경로를 게이팅하지 않는다 (ADR trading／observability／0003)" -m "executeTradeLoop 은 리스크 체크가 CloseReason 을 돌려주면 그 자리에서 return 해 신호 생성 단계까지 가지 않았다. 그래서 손절·익절·트레일링이 발동한 분에는 trading_signals 행이 통째로 없었다 — 분석에 가장 정보량이 큰 순간인 청산 직전의 지표 상태가 1분 관측 시계열에서 정확히 빠져 있었다는 뜻이고, 구멍이 특정 사건과 체계적으로 겹치는 것은 단순 결측보다 나쁘다. 리스크 체크 호출 위치는 그대로 두고 조기 return 을 신호는 기록하되 매매 단계 4~6 은 건너뛴다로 바꿨다. 행동 변화는 청산 틱에 trading_signals 행 1개가 더 생긴다는 것뿐이고 매수·매도·리밸런싱 판단은 이전과 동일하게 억제된다. 신호 생성을 리스크 체크 앞으로 옮기는 대안은 기각했다 — generateSignal 은 캔들 DB 조회와 DivergenceService.detect 와 calculatePreviousMAs 를 하므로 거기서 예외가 나거나 쿼리가 느리면 그 틱의 손절이 지연되거나 실행되지 않는다. 관측성이 자본 보호를 게이팅해선 안 된다는 것이 이 커밋의 요지다. RiskManagementService 가 신호 행을 쓰는 대안(리스크 애그리거트에 신호 도메인 의존을 새로 만든다)과 RISK_EXIT 합성 행을 넣는 대안(점수를 계산한 적 없는 행이 섞여 균일 관측 시계열 전제를 깨뜨린다)도 ADR Rationale 표에 기각 사유와 함께 남겼다. 신호 생성은 try／catch 로 감쌌는데, 삼키는 이유는 없던 운영 경보를 만들지 않기 위해서다 — 삼키지 않으면 바깥 catch 가 lastError 와 LOOP_ERROR 이벤트를 남기지만 이전에는 이 경로에서 신호 생성 자체를 하지 않았으므로 그런 경보가 존재할 수 없었다. generateSignal 이 null 을 돌려주는 경우(캔들 없음)에는 합성 행을 만들지 않는다. 테스트 5종을 신설했고 RED 가 정확히 하나였다 — riskExitTick_stillGeneratesSignal 이 WantedButNotInvoked 로 실패하고 나머지 4개(가드)는 변경 전에도 통과했다. 그 4개가 중요하다: riskCheckRunsBeforeSignalGeneration 은 InOrder 로 순서를 못박아 나중에 누가 신호 생성을 위로 올리면 빌드가 깨지게 하고, riskExitTick_skipsRebalanceAndTrade 는 단계 4~6 억제가 유지되는지를 never() 로 지키며, signalGenerationFailureOnRiskExitTick_doesNotRaiseNewLoopError 는 lastError 가 null 이고 LOOP_ERROR 이벤트가 없음을 단정한다. 범위 밖으로 남긴 구조적 공백 두 가지는 ADR Consequences 에 적었다 — 봇 정지·일시정지 구간, 그리고 executeTradeLoop 의 candleService.fetchAndSaveCandles() 가 try／catch 없이 호출돼 캔들 조회 실패가 그 틱의 리스크 체크까지 통째로 건너뛰게 만드는 문제. 후자는 관측성이 아니라 자본 보호 사안이라 별도 결정으로 다뤄야 한다."

# Commit 132 — perf(trading): 트레이딩 테이블 인덱스 (동작 불변)
# 주의: SignalJpaEntity·TradeJpaEntity 의 @Index 는 Commit 130 이 흡수. 여기는 PositionJpaEntity 만.
git add src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/entity/PositionJpaEntity.java
git commit -m "perf(trading): 트레이딩 테이블 인덱스 추가 (동작 불변)" -m "분석 페이지의 집계는 전부 기간 스캔인데 trading_signals·trading_positions·trading_trades 에 인덱스가 하나도 없었다(indexes 가 선언된 테이블은 trading_events 뿐이다). trading_signals 는 매 분 1행이 쌓여 90일이면 13만 행이므로 무인덱스 범위 스캔은 페이지 로드마다 풀스캔이 된다. 세 테이블에 @Table(indexes=...) 로 선언한다 — trading_signals(market, signal_time), trading_positions(market, status, closed_at)·(market, opened_at), trading_trades(position_id)·(market, created_at). Hibernate 의 SchemaUpdate 는 JDBC 메타데이터에 없는 인덱스명에 CREATE INDEX 를 발행하므로 ddl-auto=update 로 기존 테이블에도 생성되지만, 인덱스명으로 매칭하기 때문에 같은 이름을 다른 컬럼으로 재사용하면 재생성하지 않는다는 점에 주의한다. 실제 생성 여부는 첫 bootRun 후 INFORMATION_SCHEMA.INDEXES 로 확인하는 것이 안전하다. 부수 이득도 있다 — trading_positions(market, status, closed_at) 은 접두 (market, status) 로 findByMarketAndStatus·countByMarketAndStatus·findOpenPositionsByMarket 을 함께 커버하는데 이들은 매 루프 틱마다 실행되고, trading_trades(position_id) 는 FK 없는 평범한 Long 이라 findByPositionId 가 풀스캔이던 것을 없앤다. 비용은 trading_signals 의 분당 insert 마다 인덱스 1개 쓰기가 늘어나는 것이며 모듈 CLAUDE.md 에 그 사실을 적었다. 인덱스는 물리 설계 사실이지 정책·임계·구조 결정이 아니라서 ADR 을 만들지 않았다(docs/adr/README.md 작성 규칙의 비결정성 변경 항목). 새 테스트는 없다 — 스키마 변경이고 동작은 불변이며, 전체 스위트가 무수정 GREEN 인 것이 그 증거다."

# Commit 133 — feat(trading): TradingAnalyticsService — 신호 품질 분석 집계 (ADR observability/0001)
# 주의: SignalRepositoryAdapter 의 투영 매핑은 Commit 130 이 흡수.
#       docs/adr/README.md 는 이 배치의 ADR 4건을 전부 이 커밋이 소유(두 뷰 + 폴더 목록 + 매트릭스).
#       docs/adr/README.md 는 Commit 136 의 risk/0006 등재(총계 79→80, trading 알고리즘 15→16,
#       trading/risk 5→6 ADRs, 날짜순 뷰 1행)도 흡수한다 — 실행 시 총계가 80 인 것이 정상이다.
git add src/main/java/me/singingsandhill/calendar/trading/domain/signal/SignalSample.java src/main/java/me/singingsandhill/calendar/trading/domain/signal/SignalComponent.java src/main/java/me/singingsandhill/calendar/trading/domain/signal/SignalRepository.java src/main/java/me/singingsandhill/calendar/trading/infrastructure/persistence/repository/SignalJpaRepository.java src/main/java/me/singingsandhill/calendar/trading/application/dto/AnalyticsReport.java src/main/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsService.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsServiceTest.java src/test/java/me/singingsandhill/calendar/trading/application/service/TradingAnalyticsGateParityTest.java docs/adr/trading/observability/0001-signal-quality-analytics-page.md docs/adr/README.md
git commit -m "feat(trading): 신호 품질 분석 집계 — 전방수익·구성요소 엣지·임계 반사실 (ADR trading／observability／0001)" -m "감사 문서가 백테스트 없이는 판단 불가로 남긴 항목들(P2-1 거래량 다이버전스 ±20 과대가중, P1-1／P1-2 로 바꾼 손절 -1.5% ／ TP +3% ／ 트레일링 -0.8% 의 실효성)에 데이터로 답하기 위한 집계 서비스다. 7개 섹션을 계산한다: 데이터 충분성, 점수 구간별 전방수익, 구성요소별 조건부 엣지, 임계 반사실, 청산사유별 실현 성과, 진입 맥락→결과, 비용 현실. 전방수익은 trading_candles 가 아니라 신호 자신의 current_price 로 계산한다 — 신호가 매 분 기록되므로 그 테이블 자체가 1분 가격 시계열이고, 캔들은 정리되지만 신호는 영구 보관이라 지평이 길며, 점수 입력과 가격 원천이 같아 기준가 불일치가 없다. 오독을 막는 장치 세 개를 자료구조에 박았다. 첫째 effectiveN — 매 분 관측치가 생기므로 +60분 전방수익은 인접 행끼리 59분이 겹치고 n=10,000 이어도 독립 관측은 약 170이다. HorizonStat 이 resolved 와 effectiveN 을 나란히 들고 effectiveN 30 미만이면 reliable()=false 를 돌려주며, 페이지는 그 칸을 흐리게 렌더한다. 둘째 net 수익률 — 왕복 taker 수수료만 0.50% 라 평균 +0.3% 구간은 gross 로 양수여도 손실이다. 셋째 커버리지 패널을 최상단에 두고 표본 부족 사유를 실제 수치와 함께 나열한다. 구성요소별 엣지에는 잔차점수(총점 - 그 요소 몫의 평균)를 함께 낸다 — 조건부 평균은 교란돼 있어서 어떤 요소가 켜진 부분집합에는 같이 켜진 다른 요소가 전부 섞여 있고, 잔차가 전체 평균보다 크면 그 요소가 아니라 원래 점수가 높은 구간을 보고 있는 것이다. 이 컬럼이 없으면 P2-1 에 대해 확신에 찬 오답이 나온다. 세 가지 설계 결정을 ADR 에 남겼다. (1) 신호 읽기만 엔티티가 아니라 SignalSample 투영을 쓴다 — 90일이 13만 행이고 @Transactional(readOnly=true) 안에서 엔티티로 읽으면 전부 1차 캐시에 남아 요청이 끝날 때까지 힙을 붙잡는데 이 앱은 Jetson Nano 에서도 돈다. 포지션·체결은 수십~수백 건이라 기존 관례대로 엔티티를 읽는다. (2) 매수 게이트를 SignalService 에서 추출하지 않고 복제했다 — 추출하면 분석 기능을 위해 실주문 신호 생성 경로를 수정해야 하는데 그게 이 작업이 통제하려는 위험 그 자체다. 대신 TradingAnalyticsGateParityTest 가 14개 시나리오를 두 구현에 각각 넣어 판정이 갈리면 빌드를 깨뜨린다. 이 테스트가 실제로 드리프트를 잡는지 확인하려고 분석 게이트의 MA60 분기를 일시적으로 무력화해 3건이 실패하는 것을 본 뒤 되돌렸다. (3) 전방 조회에 인덱스 산술(i+15)을 쓰지 않는다 — 시계열에 구멍이 있어 인덱스로 세면 조용히 엉뚱한 시각의 가격을 집는다. 목표 시각 ±90초 안의 최근접 행만 쓰고 없으면 결측으로 버리며, 버린 비율을 커버리지에 표기한다. 진입 맥락 조인은 인과 방향을 지킨다 — 신호가 매수를 유발하므로 신호는 반드시 체결보다 앞선다. 대칭 최근접 매칭을 하면 T+31초에 열린 포지션이 T+60초 신호(거리 29초)에 붙어 그 매매 뒤에 생성된 신호를 원인으로 기록하게 되므로, signal_time <= opened_at 이면서 90초 이내인 가장 최근 신호만 쓴다. 리밸런싱·수동 매수는 신호와 무관해 예측력을 희석하므로 제외하고 제외 건수를 표기한다. 비용 지표의 주 값은 수수료／진입 명목금액이다 — 수수료／gross 는 gross <= 0 이면 부호가 뒤집히거나 0 근처에서 발산해 의미가 없어 null 로 둔다. 임계 반사실은 MA60 하회 시 거래량 스파이크 분기를 판정해야 하는데 그 입력이 observability/0002 이전 행에 없어서, 단일 숫자가 아니라 하한~상한 범위와 판정불가 건수로 낸다. 테스트는 TradingAnalyticsServiceTest 17종과 GateParityTest 16종이다. 전방수익 꼬리 경계에서는 처음에 단정을 틀리게 썼다가 실제 값으로 정정했다 — i=85 는 목표 100분에 대해 마지막 관측치가 99분이라 60초 차로 허용오차 안이고 i=86 부터 결측이다. docs/adr/README.md 는 이 배치의 ADR 4건을 두 뷰와 폴더 목록에 추가하면서 선재 드리프트도 정정했다: 실제 파일이 79개(추가 전 75개)인데 매트릭스 합계는 74였고, 원인은 Commit 125 의 stock/algorithm/0010 이 폴더 목록에만 반영되고 매트릭스 셀(9)이 그대로였던 것이다."

# Commit 134 — feat(trading): /trading/analytics 관리자 분석 페이지
git add src/main/java/me/singingsandhill/calendar/trading/presentation/controller/TradingDashboardController.java src/main/resources/templates/trading/analytics.html src/main/resources/templates/trading/fragments/header.html src/test/java/me/singingsandhill/calendar/trading/presentation/controller/TradingAnalyticsPageTest.java
git commit -m "feat(trading): ／trading／analytics 신호 품질 분석 페이지" -m "TradingAnalyticsService 의 리포트를 Thymeleaf 로 렌더한다. 스케줄러도 스냅샷 저장도 두지 않고 요청 시점에 계산한다 — 페이지는 보낼 것이 없어 주기 갱신 개념이 없고, 신호·포지션이 영구 보관이라 과거 어느 구간이든 다시 계산하면 되며 지난주 대비 비교도 기간만 바꾸면 된다. 구간은 ?days= 쿼리 파라미터(7／14／30／60／90, 기본 30)이고 서비스가 1~180 으로 클램프한다. JSON 엔드포인트는 만들지 않았다 — 기존 ／api／trading／** 는 전부 특정 JS 파일이 폴링하기 때문에 존재하는데 이 페이지에는 폴링할 이유가 없고, 소비자 없이 12개 레코드 트리를 추적하는 표면을 늘릴 이유도 없다. 구간 선택도 GET 폼 하나라 새 JS 파일이 필요 없다. SecurityConfig 는 건드리지 않았다 — ／trading／** 는 규칙 #1 에서 이미 ROLE_ADMIN 이고 포괄 permitAll 보다 앞에 선언돼 있다. 그 사실 자체를 TradingAnalyticsPageTest 가 고정한다: 미인증은 리다이렉트, ROLE_USER 는 403, ROLE_ADMIN 만 200 이다. 템플릿은 커밋된 trading-tw.css 에 이미 존재하는 유틸리티 클래스와 trading.css 의 tr-* 컴포넌트 클래스만 쓴다 — 새 클래스를 쓰면 npx tailwindcss 재생성이 필요해지고 15KB 최소화 CSS 덩어리가 로직 diff 한가운데 들어온다. 커밋 전에 템플릿의 모든 class 토큰을 두 CSS 파일에 대조해 누락이 없음을 확인했다. 빈 데이터 처리는 섹션을 숨기는 대신 표본 부족 사유를 실제 수치와 함께 보여주는 쪽으로 했다 — 점수 구간의 n=0 도 그 점수가 한 번도 안 나왔다는 정보이므로 행을 지우지 않는다. 테스트는 5종인데 그중 adminRendersAllSectionsWhenDataExists 가 핵심이다: 빈 리포트만 렌더하면 th:unless 로 감싼 본문 표 6개가 통째로 건너뛰어져 표 안의 Thymeleaf 표현식 오류를 잡지 못하므로, 채운 리포트를 넣고 응답 HTML 에 각 섹션 제목과 실제 데이터(점수 구간 라벨·청산 사유·구성요소 표시명)가 들어 있는지 문자열로 단정한다. daysParamIsClampedToMaxWindow 는 ?days=9999 로 들어와도 화면에 표시되는 구간이 실제 계산 구간과 일치하는지를 본다."

# Commit 135 — docs(trading): CLAUDE.md·TRADING.md·bot.md 동기화
# 주의: trading/CLAUDE.md 는 Commit 136 의 "캔들 실패 틱" 항목(ADR risk/0006)도 흡수한다.
#       루트 CLAUDE.md 는 Commit 143 의 정정 한 줄(git-commit.md 를 "gitignore 대상" 이라 한
#       서술 → "추적·커밋되는 커밋 로그")과 **LF 개행 정규화 371줄**도 흡수한다. 내용 변경분만
#       보려면 git diff -w 를 쓸 것(3+/1-). 개행 정규화는 Commit 137 의 *.md 규칙과 짝이다.
#       이 커밋이 git-commit.md 를 포함하므로 아래 Commit 136 섹션도 함께 커밋된다 — 정상이다.
#       배치의 마지막 커밋은 이제 136 이다.
git add CLAUDE.md TRADING.md src/main/java/me/singingsandhill/calendar/trading/CLAUDE.md src/main/java/me/singingsandhill/calendar/trading/application/CLAUDE.md docs/trading/bot.md docs/guides/git-commit.md
git commit -m "docs(trading): 분석 페이지·결정 입력 영속화 문서 동기화" -m "CLAUDE.md 동기화 규칙상 이번 배치는 결정 변경에 해당하므로 사실 문서 네 곳을 코드에 맞춘다. 루트 CLAUDE.md 의 Background Schedulers 절에 캔들 보관 기간이 trading.bot.candle-retention-days(기본 90일)이며 삭제 구간은 복구 불가라는 사실을 추가했다(가드 부재 서술은 여전히 사실이라 유지). trading/CLAUDE.md 에는 데이터 기록 & 분석 절을 신설해 보관 키, signal_id 의 채움／null 의미론, order_ratio, trading_signals 의 새 컬럼 4개, ATR 을 재조회 없이 계산한다는 점과 주문 사이징용 calculateATRPercent 는 변경하지 않았다는 점, 청산 틱 신호 기록과 순서 역전 금지 불변식, 인덱스 목록과 분당 쓰기 비용을 적었다. Presentation 표의 TradingDashboardController 행에 ／analytics 를 추가했다. trading/application/CLAUDE.md 에는 TradingAnalyticsService 항목을 지원 서비스 절에 넣고 읽을 때 주의할 네 가지(전방수익 원천이 캔들이 아니라 신호의 current_price 라는 점, n 이 아니라 effectiveN 을 본다는 점, 매수 게이트가 복제본이며 파리티 테스트가 깨지면 리팩터링 중이라도 넘어가면 안 된다는 점, 신호 읽기만 투영을 쓰는 이유)를 적었다. TRADING.md 에는 대시보드 줄 아래에 분석 페이지 URL 과 온디맨드 계산이라는 사실을 추가했다. docs/trading/bot.md 는 편집 전에 줄바꿈을 LF 로 되돌렸다 — 워킹트리 사본이 통째로 CRLF 로 바뀌어 있어 내용 변경이 0인데도 raw diff 가 504+504 로 잡히던 상태였고(.gitattributes 에 *.md 규칙이 없다), 그대로 두면 이 커밋에 1,008줄 잡음이 섞인다. 정규화 후 HEAD 와 완전 동일함을 git diff 로 확인한 뒤 편집했으므로 이 파일의 diff 는 실제 추가분만 남는다. 내용으로는 캔들 정리를 7일이라고 적은 두 곳(데이터 흐름도, 스케줄 타이밍)을 이번 변경이 사실과 어긋나게 만들었으므로 고쳤고, 설정값 절에 candle-retention-days 를 넣었으며, 13절 신호 품질 분석 페이지를 신설해 각 섹션이 답하는 질문과 읽는 법 세 가지 함정(독립창·net·잔차점수), 그리고 임계 반사실의 판정 범위 한계를 적었다. 이 파일에는 이번 범위 밖의 선재 드리프트가 여럿 남아 있다 — 5절의 합산 범위 ±135(실제 ±128), 6절의 쿨다운 10분·최소보유 15분(실제 30／30), 8절의 손절 -3%·익절 +15%·추적손절 -3%(실제 -1.5%／+3%／-0.8%), 11절 설정값 블록의 같은 수치들. 이번 변경이 유발한 것이 아니고 각각 다른 커밋의 관심사라 최소 범위 원칙에 따라 손대지 않았으며 결과 보고에 따로 적었다. 같은 이유로 docs/stock/bot.md 등 CRLF 잡음만 있는 문서 6개도 이 배치에서 제외했다."

# =====================================================================
# 코인 봇 — 캔들 수집 실패가 손절·익절을 막지 않게 (2026-08-08)
# =====================================================================
# 배경: docs/ 128개 파일의 계획·백로그·감사를 전수 조사해 "아직 안 끝난 것"을 가려내는
# 작업에서 나왔다. 245개 항목을 코드로 대조한 결과 절반 이상이 이미 구현된 채 문서만 낡은
# 것이었고, 실제로 남은 것 중 위험 대비 diff 가 가장 작은 한 건이 이것이다.
# ADR trading/observability/0003 이 스스로 발견해 "관측성이 아니라 자본 보호 사안이라 별도
# 결정으로 다뤄야 한다" 며 범위 밖으로 남긴 후속 ② 를 닫는다.
# 검증: RED 3건 선확인(WantedButNotInvoked 2 + ArgumentsAreDifferent 1) → GREEN.
#       전체 스위트 100 클래스 ／ 590 테스트 ／ failures 0 ／ errors 0 을
#       build/test-results/test/*.xml 로 확인 (기준선 99 ／ 585).
# 주의: 이 커밋은 Commit 128~135 를 먼저 실행한 뒤 마지막에 실행한다. 워킹트리 일괄 보유
#       환경이라 TradingBotService.java(→130) ／ docs/adr/trading/observability/0003(→131) ／
#       docs/adr/README.md(→133) ／ trading/CLAUDE.md(→135) 는 각각 먼저 등장하는 커밋이
#       소유하므로, 이 커밋의 add 목록에는 신규 파일 2개만 남는다. 각 흡수처의 "주의:" 에
#       무엇이 함께 들어가는지 적어 뒀다.

# Commit 136 — fix(trading): 캔들 수집 실패가 손절·익절을 막지 않는다 (ADR risk/0006) — 마지막 커밋(git-commit.md 포함)
git add src/test/java/me/singingsandhill/calendar/trading/application/service/TradingBotServiceCandleFailureGuardTest.java docs/adr/trading/risk/0006-candle-sync-failure-does-not-gate-risk-check.md docs/guides/git-commit.md
git commit -m "fix(trading): 캔들 동기화 실패는 리스크 체크를 게이팅하지 않는다 (ADR trading／risk／0006)" -m "executeTradeLoop 은 여섯 단계인데 0단계 미결 주문 스윕과 3단계 신호 생성은 각각 try／catch 로 감싸여 있고 0단계 주석은 실패해도 리스크 체크를 막지 않는다 라고 명시까지 한다. 그 사이 1단계 candleService.fetchAndSaveCandles() 한 줄만 맨몸 호출이었다. 예외가 나면 바깥 catch 로 빠져 lastError 와 LOOP_ERROR 를 남기고 메서드가 끝나므로 그 틱의 손절·익절이 통째로 실행되지 않는다. 양옆 두 줄이 이미 방어돼 있다는 비대칭이 이 결함의 가장 강한 증거다. 발생원은 가설이 아니라 이미 문서화된 둘이다 — Bithumb 캔들 API 장애(같은 WebClient 를 타므로 ADR infrastructure／0004 가 다룬 PrematureCloseException 계열이 그대로 온다)와 캔들 동기화 5분 크론 대 트레이딩 루프 매분 크론의 동시 insert 유니크 위반(감사 P2-8, spring.task.scheduling.pool.size=4 라 실제로 겹칠 수 있다). ADR observability／0003 이 이 항목을 스스로 발견해 Consequences 후속 ② 로 적으면서 관측성이 아니라 자본 보호 사안이라 별도 결정으로 다뤄야 한다며 범위 밖으로 남겼고, 이 커밋이 그 결정이다. 단순히 try／catch 로 감싸기만 하는 안은 기각했다 — 그러면 반대 방향의 회귀가 생긴다. 지금은 캔들 실패 시 틱 전체가 멈춰 매매 판단도 함께 중단되는데, 감싸기만 하면 stale 캔들로 계산한 신호가 4~6단계의 신규 진입과 리밸런싱을 그대로 태운다. 보호를 늘리려다 새 리스크를 만드는 셈이다. 그래서 두 갈래로 나눈다. 2단계 리스크 체크는 실행한다 — RiskManagementService 는 캔들을 전혀 참조하지 않고(그 클래스에 candle grep 0건) bithumbApiClient.getCurrentPrice() 로 판정하므로 캔들이 낡아도 손절·익절이 오발동하지 않는다. 4~6단계는 건너뛴다. 구현은 candlesFresh 플래그 하나이고, 조기 return 은 기존 closeReason != null 바로 아래에 같은 형태로 둔다 — 새 구조를 만들지 않고 이미 있는 억제 지점을 재사용한 것이다. 경보는 유지했다. 이전에는 바깥 catch 가 lastError 와 LOOP_ERROR 를 남겼는데, 루프를 계속 돌리면서 그 경보까지 잃으면 캔들 장애가 조용히 지속된다. lastError 는 BotStatus 로 대시보드에 노출되므로 그대로 채우고 이벤트 타입만 CANDLE_SYNC_FAILED 로 나눴다 — 루프가 실제로는 계속 돌았는데 LOOP_ERROR 로 적는 것은 사실과 다르고, 새 타입은 문자열 하나라 표면이 늘지 않는다. 부수 효과로 캔들 실패가 다른 루프 예외와 섞이지 않고 분리 관측된다. ADR 은 observability 가 아니라 risk 아래 0006 으로 뒀다. 자본 보호 경로의 게이팅 정책이기 때문이고, 같은 계열인 risk／0005(현재가 조회 실패 시 진입 차단)와 나란히 읽히는 것이 맞다. 다만 방향이 하나 더 있다는 점을 Rationale 에 적었다 — 돈을 넣는 판단(진입)은 입력이 불확실하면 막고, 이미 넣은 돈을 빼는 판단(손절)은 불확실해도 막지 않는다. 이 비대칭이 이 결정의 핵심이다. 기각안 넷을 표로 남겼다: 현행 유지, 감싸기만 하기, 신호 기록까지 생략(observability／0003 의 시계열 연속성 결정을 되돌리게 된다), 캔들 수집을 리스크 체크 뒤로 이동(3단계가 캔들을 입력으로 쓰고 riskCheckRunsBeforeSignalGeneration 이 InOrder 로 못박은 불변식과 얽혀 회귀 표면이 넓다). 테스트는 TradingBotServiceCandleFailureGuardTest 5종을 신설했고 RED 가 정확히 셋이었다 — candleSyncFailure_stillRunsRiskCheck 와 stillClosesPositionOnStopLoss 가 WantedButNotInvoked 로, keepsOperatorAlert 가 ArgumentsAreDifferent(LOOP_ERROR 가 기록됨)로 실패하는 것을 먼저 확인했다. 나머지 둘은 가드다: suppressesEntryAndRebalance 는 stale 캔들이 신규 진입을 태우지 않는지를 never() 로 지키고(변경 전에는 틱 전체가 멈춰서 통과했으니, 이 테스트가 지키는 것은 위 기각안 2번으로 되돌아가지 않는 것이다), healthyTick_proceedsToRebalanceAndTrade 는 정상 틱의 4~6단계가 그대로 도는지를 본다. 가드를 처음 썼을 때 정상 틱인데 lastError 가 남아 실패했는데, 원인은 이 저장소 테스트의 알려진 취약점이었다 — 생성자 8번째 positionRepository 에 null 을 넣는 관행 때문에 6단계 countByMarketAndStatus 가 NPE 를 내고 바깥 catch 로 빠진 것이다(감사 P3 가 지적한 null 대량주입 의존). 하네스 아티팩트지 제품 동작이 아니므로 mock 으로 바꿔 6단계까지 실제로 관통시켰다. 같은 루프를 다루는 TradingBotServiceLoopSignalRecordingTest 5종이 무수정 GREEN 인 것을 회귀 증거로 확인했다 — 특히 순서 불변식을 InOrder 로 고정한 riskCheckRunsBeforeSignalGeneration 이 그대로 통과한다. 이 결정은 증상이 자본 보호로 번지는 것을 끊을 뿐 원인(감사 P2-8 동시 insert race, P1-2 캔들 동결)을 고치지 않으며, 그 사실을 ADR Consequences 에 후속으로 적었다."

# =====================================================================
# 문서 정리 — 코드는 끝났는데 낡아 있던 문서 149건 (2026-08-09)
# =====================================================================
# 배경: docs/ 245개 항목 전수 조사에서 DONE_DOC_STALE 54건 / NOT_A_PLAN 23건이 나왔다.
# 즉 "미완료로 보이던 것"의 절반 이상이 이미 끝난 일이었다. 이 배치가 그 문서들을 코드
# 사실에 맞춘다. 코드 변경은 0 이다.
# 방법: 6개 클러스터로 수정 명세를 뽑은 뒤, 각 명세의 모든 수치·심볼·경로를 코드에서
#       재확인하는 적대적 감사를 붙였다(151건 중 CONFIRMED 124 / CORRECTED 25 / DROPPED 2).
#       감사가 실제로 막은 것들: ① P0-3 을 "해결" 로 적으려던 것 — test-order 우회가 남아
#       있어 "부분 해결" 로 정정 ② currentText 가 말줄임표라 Edit 이 100% 실패할 명세 다수
#       ③ architecture.md 보안표의 선언 순서 서술이 거짓(/recap/share/** 는 보호 규칙보다
#       먼저 온다) ④ mksc_shrn_iscd 확인 단정 — 파서에 폴백이 있어 로그로 구분 불가.
# 검증: 코드 미변경 확인 겸 전체 스위트 100 클래스 ／ 590 테스트 ／ failures 0 ／ errors 0
#       (직전 배치와 동일 — 문서만 바뀌었으므로 불변이 정상).
#       파일마다 git diff --numstat 과 git diff -w --numstat 이 같은지, CRLF 줄이 0 인지 확인.
# 주의: Commit 129~136 이 아직 미실행이다. 그 배치가 소유하는 파일에 이번 수정이 얹혀 있다 —
#       CLAUDE.md 는 Commit 135 가, docs/adr/README.md 는 Commit 133 이 가져간다. 각 섹션의
#       "주의:" 에 적어 뒀다. 순서는 129~136 → 137~143 이다.
# 개행: 편집 전에 워킹트리에서 개행만 CRLF 로 뒤집혀 있던 md 6개(docs/stock/bot.md,
#       docs/datedate/architecture-review.md, docs/prompts/*.md 4)를 git checkout 으로 LF 복원했다
#       (git diff -w 가 0줄인 것을 먼저 확인). 복원만 한 파일은 diff 가 0 이라 add 대상이 아니다.
#       docs/architecture.md 와 CLAUDE.md 는 HEAD 부터 전 줄 CRLF 였어서 LF 로 정규화했다 —
#       각각 669줄 / 371줄의 개행 전용 diff 가 내용 diff 와 같은 커밋에 섞인다. git diff -w 로
#       내용 변경분만 분리해 볼 수 있다.

# Commit 137 — chore(docs): .gitattributes 에 *.md LF 규칙
git add .gitattributes
git commit -m "chore(docs): .gitattributes 에 md LF 규칙 추가" -m "규칙이 없던 동안 편집기가 문서를 통째로 CRLF 로 뒤집어, 내용 변경이 0 인데 raw diff 가 수백 줄로 잡히는 일이 반복됐다. docs/guides/git-commit.md:1181 이 같은 증상을 기록하고 있고 이번 정리에서도 6개 파일 1,838줄이 그 잡음이었다. gradlew·bat·jar 만 있던 목록에 *.md text eol=lf 를 더한다. 이 규칙이 붙으면 앞으로 md 를 add 할 때 인덱스가 LF 로 정규화되므로, 아직 CRLF 로 커밋돼 있는 파일은 다음에 그 파일을 만지는 커밋에서 한 번 개행 diff 를 낸다 — 이번 배치에서는 docs/architecture.md(Commit 138)와 CLAUDE.md(Commit 135)가 그 대상이고 두 곳 모두 해당 섹션에 적어 뒀다."

# Commit 138 — docs(architecture): 보안 표를 SecurityConfig 선언 순서대로 정정 (+ 개행 정규화)
# 주의: 이 파일은 HEAD 부터 전 줄 CRLF 였다. LF 정규화 669줄이 함께 들어간다 — 내용 변경분은
#       git diff -w 로 76+/58- 이다.
git add docs/architecture.md
git commit -m "docs(architecture): 보안 표를 SecurityConfig 선언 순서대로 정정 + 스케줄러·지표 수치 현행화" -m "7절 보안 표가 /api/** 와 /stock/** 와 /api/stock/** 를 무조건 permitAll 로 적고 보호 대상에는 /runners/admin/** 만 두고 있었다. 실제 SecurityConfig 는 /api/trading/** 와 /trading(/**) 를 ROLE_ADMIN, POST /api/stock/bot/** 를 ROLE_ADMIN, /me 와 /recap(/**) 와 /api/me/** 를 ROLE_USER 로 두고 이들을 포괄 permitAll 보다 먼저 선언한다. 이 문서를 근거로 SecurityConfig 를 정리하면 트레이딩 실주문 API 와 카카오 사용자 영역이 무인증 공개된다 — 이 배치에서 가장 위험한 항목이라 먼저 올린다. 보호 표를 공개 표 앞으로 옮기고 선언 순서가 곧 매칭 우선순위라는 사실을 표 머리에 적었다. 다만 순서 서술은 정확해야 했다: /recap/share/** 와 /login 은 보호 규칙보다 먼저 오고 /runners/admin/login 도 /runners/admin/** 보다 먼저 오므로, 처음 쓴 문안의 위 보호 규칙 뒤에 선언된다 는 거짓이었다. 자신을 삼킬 수 있는 포괄 permitAll 보다 먼저 로 고쳤고 이 표현은 네 개 보호 행 전부에 대해 참이다. /runners 가 와일드카드가 아니라 개별 매처로 나열된 이유도 함께 적었다. 8절 스케줄러 표에서는 존재하지 않는 saveHourlySnapshot() 행을 지웠다 — 저장소 전체 grep 에서 이 심볼은 DailySummaryScheduler 의 제거됨 주석 두 줄과 이 문서에만 있었다. 캔들 정리 설명을 7일에서 trading.bot.candle-retention-days 기본 90일로 고치고 bot.enabled 가드가 없다는 사실을 덧붙였다(yaml 과 Java 기본값이 모두 90 이라 어느 쪽이 적용돼도 같다). stock 쪽은 스크리닝 09:05 를 실제 cron 인 09:20 으로, 매매 루프 시작 09:10 을 09:20 으로 고치고 11:40 일일 리포트 행을 추가했다 — StockTradingScheduler 의 @Scheduled 다섯 개를 직접 읽어 확정했다. 타임라인 다이어그램의 15:30 장 마감도 봇 기준 종료인 11:30(trading-end)으로 바꿨다. 9절 시그널 최대 점수 ±155 는 SignalService 의 실제 반환값을 합산해 ±128(크로스 이벤트 기준)로 고쳤다 — 25+8+20+15+15+15+20+10 이다. 그 클래스의 javadoc 주석 자체도 ±10·±15 로 낡아 있는데 코드 주석은 이번 범위 밖이라 결과 보고에만 적었다. 리스크 수치 다섯 개(손절·익절·트레일링·활성화·최소수익)와 리밸런싱 쿨다운은 application.yaml 의 trading.risk.* / trading.rebalancing.* 실측값으로 교체했다. 10절은 stock 쪽인데 다단계 익절 표의 TP1 을 +1.5% 에서 +5% 로, TP3 앵커를 당일고점×1.01 에서 진입가 +10% 로, 손절을 진입가 -1.5% 에서 풀백저가 앵커로 고쳤고, 스크리닝 필터 표는 legacy 경로 값(갭 1~10%, 시총 1,000억, 체결강도 110)이 실려 있어 score 모드(scoring.enabled=true)의 Floor 기준으로 다시 썼다. 상태 머신의 FILTERED_OUT 임계 3% 는 코드가 entry.pullback-max-percent 를 쓰므로 5% 로 정정했다. 문서 성격상 테스트가 없으므로 검증은 세 가지로 했다 — 모든 수치를 yaml·Properties·소스에서 재확인, 새로 등장하는 심볼은 grep 으로 존재 확인, git diff -w 로 개행 오염이 없는지 확인. 회귀 가드로 인용하려던 테스트 클래스 세 개는 실재를 확신하지 못해 문안에서 뺐다 — 이 문서의 결함이 정확히 없는 심볼을 지목한 것이었기 때문이다."

# Commit 139 — docs(trading): 리스크 수치 드리프트 해소 + 결정 완료 표기
git add docs/trading/bot.md docs/trading/remaining-work.md docs/trading/bithumb-v2-migration-plan.md docs/adr/trading/infrastructure/0001-order-execution-transaction-boundary.md
git commit -m "docs(trading): bot.md 리스크 수치 드리프트 해소 + 감사·계획 문서의 결정 완료 표기" -m "bot.md 8절과 11절이 stopLoss -3% / takeProfit +15% / trailingStop 3% / trailingActivation +10% / 쿨다운 10분 / 최소보유 15분 / minProfitThreshold 0.6% / minSellPnlPct +3% 로 적혀 있었는데 운영값은 -1.5% / +3% / 0.8% / +1.5% / 30분 / 30분 / 0.1% / 0% 다. 같은 파일 13절이 이미 신값을 적고 있어 한 문서 안에서 정면으로 모순된 상태였다. application.yaml 의 trading.risk.* 와 trading.bot.* 을 TradingProperties 자바 기본값과 함께 대조해 여덟 값을 전부 교체했다. 8절은 값만 고치면 본문이 앞뒤가 안 맞아 리스크 관리 규칙 절을 통째로 다시 썼다 — 6단계였던 것을 실제 checkPositionRisk 순서대로 7단계로 늘리고(TIME_EXIT 이 문서에 아예 없었다), 손익률이 전부 수수료 차감 net 기준이라는 점을 명시했다. 추적손절 시나리오 예시도 +10% 활성화·-3% 추적 전제로 쓰여 있어 신값으로 다시 계산했다. 다만 진입가 1,010원 지점의 net 수익률은 0.5% 로만 적었다 — Position.calculateUnrealizedPnlWithFee 의 예상 매도수수료가 setScale(0, UP) 올림이라 소수 둘째 자리가 수량에 따라 달라져서, 구체 숫자를 박으면 또 틀린 값을 심는 자리다. 5절 점수표의 이론적 최대 ±135 와 실질 최대 ±125 는 SignalService 반환값을 합산해 ±128 / ±108 로 고쳤고 MA 상태유지 ±10 과 MA 추세 ±15 도 실제 값 ±5 / ±8 로 바꿨다. remaining-work.md 의 P0-3 은 구현됨·미검증 이었는데 코드와 테스트가 모두 있어 해결로 올릴 뻔했다. 감사에서 막혔다 — TradingVerificationApiController 의 POST /test-order 가 여전히 bot.enabled 를 보지 않고 실주문을 낸다. 해결(수동 매매 API) 로 범위를 좁히고 잔여를 P1-4 행에서 함께 추적한다고 적었다. 완료로 적었으면 다음 세션이 이 우회를 영영 놓쳤을 것이다. ADR trading/infrastructure/0001 의 남은 한계(수동 매매 Position 미생성·미청산)는 risk/0004 가 닫았으므로 그 참조를 달았다 — Decision 과 Rationale 은 건드리지 않았다. bithumb-v2-migration-plan.md 9절은 결정 필요 사항 인데 세 항목이 이미 결정돼 있었다: MockWebServer 는 build.gradle:56 에 반영됐고, post_only 는 handoff 4절이 채택 보류로, Phase 5(best+ioc)는 확정 제외로 못박았다. 계획서만 읽는 사람이 이미 끝난 결정을 다시 논의하지 않도록 결정 완료 표시와 근거 링크를 달았다. docs/trading/backlog.md 는 손대지 않았다 — 27행에서 문장 중간에 끊긴 미완성 문서이고 remaining-work.md 가 상위호환 인덱스이며 인바운드 링크가 0건이라 삭제·아카이브 후보지만, 미사용 문서 처리는 사용자 판단이라 결과 보고에만 적는다."

# Commit 140 — docs(stock): bot.md 전면 정정 + 감사·ADR 후속 종결
git add docs/stock/bot.md docs/audit/stock-trading-logic-review-2026-07-24.md docs/adr/stock/algorithm/0005-dynamic-universe-volume-rank.md docs/adr/stock/infrastructure/0007-trade-strength-source-inquire-ccnl.md
git commit -m "docs(stock): bot.md 를 현재 파라미터·스크리닝 동작에 맞춰 정정 + 감사·ADR 후속 종결" -m "stock/bot.md 는 1절부터 11절까지 전반이 낡아 있었다. TP1 이 +1.5%(실제 5.0), TP3 가 당일고점×1.01(실제 진입가 +10% 고정), 추적손절 0.8%(실제 2.0), 풀백 1.5~3.0%(실제 1.5~5.0), bounce 0.3%(실제 0.2), 매도세율 0.23%(실제 0.20)이었다. application.yaml 의 stock.* 전 구간을 StockProperties 기본값과 대조해 값을 확정했고, scoring.enabled=true 라 legacy 키(min-gap-percent 등)는 실효값이 아니라는 점을 10절 설정 예시에 주석으로 남겼다. 10절은 키를 camelCase 로 적고 있었는데 relaxed binding 때문에 그대로 먹히면서도 원본과 문자 대조가 안 돼 옛 파라미터로 조용히 되돌아갈 수 있어 전부 kebab-case 로 바꾸고 원본과 같은 표기를 쓰라는 문구를 붙였다. 3절 유니버스 서술은 고정 60개 관심종목 이었으나 실제는 pinned 와 거래량순위 상위 30 과 정적폴백 70종목의 합집합이다. 08:30 에는 당일 거래량이 없어 거래량순위를 호출하지 않고 09:20 에 조회한다는 점도 적었다 — 2026-08-03 로그에는 08:30 에 호출한 기록이 있지만 그건 커밋 e27706c 이전 동작이고, 현재 코드는 GapPullbackBotService 가 08:30 에 refreshStaticOnly 를, 09:20 에 refreshIfDegraded 를 부른다. 오래된 로그를 근거로 현재 동작을 서술할 뻔한 자리다. 종목 선정도 minCandidates 3개 보장 으로 적혀 있었으나 강제 선정은 제거됐고 총점 40 과 신호점수 25 두 관문을 모두 통과해야 하며 조건에 맞는 종목이 없으면 0건이 정상이다. 체결강도는 세 곳에서 서술이 틀렸다 — 소스가 시세 TR 이 아니라 inquire-ccnl 의 tday_rltv 이고, 0(미집계)을 통과시킨다 고 적혀 있었지만 skip-zero-strength 기본값이 true 라 오히려 탈락시킨다. 공휴일은 특히 조심해서 썼다. 필터 미구현 이라는 서술은 낡았지만 그렇다고 완료도 아니다 — 스케줄러 다섯 개 잡에 isTradingDay 가드가 전부 걸려 있는데 application.yaml 의 holidays 가 빈 배열이라 실제로 걸러지는 건 주말뿐이다. 필터는 있으나 리스트가 비어 주말만 차단된다 로 정확히 적었고 FAQ 항목도 같은 취지로 다시 썼다. 6절 청산 우선순위는 아스키 박스 표를 마크다운 표로 바꾸면서 TP1·TP2·TP3 가 독립 트리거라는 점, TP1·TP3 앵커는 진입가 고정이고 손절 앵커는 풀백저가라는 점, 추적손절이 부분익절 이후 활성화된다는 점을 명시했다. 7절 시간 감소 그래프는 09:10~15:15 구간으로 그려져 있었는데 실제 운영 창이 09:20~11:20 이라 다시 그렸고, 두 종점이 상수가 아니라 설정에서 유도된다는 사실과 그 이전에는 곡선의 36%만 지나갔다는 배경을 적었다. 8절 수수료는 증권거래세 0.18% + 농특세 0.05% 표기를 2026 기준 매도측 0.20%(코스피는 0.05+0.15, 코스닥은 0.20)로 고치고 슬리피지 0.2%를 포함한 실효 청산 비용 0.43% 를 함께 적었다. 예시 계산도 TP1 이 +5% 로 바뀌었으므로 다시 계산했다. 9절 첨부파일은 logs/stock-trading.log(당일 로그) 로 적혀 있었는데 실제로는 stock-screening-snapshot-YYYY-MM-DD.log 이고, 이 파일이 09:20 에서 끝나 보이는 것은 앱이 멈춘 게 아니라 발송 시점 스냅샷이기 때문이라는 설명을 덧붙였다 — 실제로 그렇게 오독된 전례가 있다. 반대로 갭 0.5~15% 와 시총 500억은 실효값과 일치해서 손대지 않았다. 감사 문서에서는 5절 표의 min-candidates 강제 선정과 min-score 유동성 팩터 왜곡 두 행에 해결 마커를 달았다 — ScreeningService 가 총점 게이트와 신호점수 게이트를 모두 요구하고 우회 분기가 없다. 10절의 미검증 후속 목록에서도 유동성 팩터 항목만 빼고 나머지 여섯 개는 그대로 뒀다. ADR stock/algorithm/0005 의 라이브 검증 필요 는 2026-08-03 로그로 TR 호출과 단축코드 파싱까지는 확인됐으나 FID 파라미터는 여전히 미확인이라 검증 완료(부분) 으로 적었다 — 파서에 stck_shrn_iscd 폴백이 있어 로그만으로는 어느 필드가 맞았는지 구분되지 않기 때문에 단정하지 않았다. ADR stock/infrastructure/0007 의 tday_rltv 후속은 같은 로그에서 str=164.61 실값이 수신됐고 skip-zero-strength 가 true 라 0 이면 그 자리에서 탈락했을 것이므로 해소로 적었다. LIVE 전환 금지는 이 검증과 무관하게 PAPER 실측이 전제라는 점도 함께 남겼다."

# Commit 141 — docs(datedate): 아키텍처 리뷰 상태 마커 + ADR 심볼 정정
git add docs/datedate/architecture-review.md docs/adr/datedate/domain/0001-schedule-aggregate-invariants.md docs/adr/datedate/domain/0002-selections-json-converter.md docs/superpowers/plans/2026-05-24-datedate-event-tracking.md docs/superpowers/specs/2026-06-20-datedate-adsense-lean-strengthen-design.md
git commit -m "docs(datedate): 아키텍처 리뷰에 현재 상태 마커 + ADR 이 지목한 없는 심볼 정정" -m "architecture-review.md 는 2026-04 시점 스냅샷인데 Top10 중 여섯 건이 이미 해결됐고 본문이 인용하는 코드가 전부 현재와 달랐다. 전면 재작성 대신 상단에 갱신 이력 블록과 Top10 상태 표를 붙이고 각 항목 제목에 마커를 다는 쪽을 골랐다 — 400줄 문서를 다시 쓰면 diff 가 커지고, E/F 절 항목 기호를 ADR 네 건이 참조하고 있어 번호를 흔들 수 없기 때문이다. 마커는 해결·부분·기각·미해결 네 가지로 나눴고 기각 을 따로 둔 것이 중요하다. 5번 선택 데이터 정규화는 ADR datedate/domain/0002 가 참가자별 조회라 늘 전체를 함께 읽는다는 이유로 기각한 항목이고, 8번의 window.SCHEDULE_DATA 제거 권고도 ADR datedate/frontend/0001 이 인젝션 유지를 채택 설계로 확정했다. 이 둘을 해결로 적으면 ADR 이 내린 결정을 리뷰가 뒤집은 것처럼 읽힌다. 2번 권한 검증은 부분으로 뒀다 — OwnerPathInterceptor 가 /api/owners/** 의 ID 형식과 예약어를 검증하지만 DELETE /api/participants/{id} 처럼 ownerId 가 경로에 없는 flat API 는 여전히 소유권을 확인하지 않는다. 10번도 컨버터 전환만 끝나고 파싱 실패 400 변환은 남아 있어 부분이다. D-7 의 schedule-view.js 465 LOC 서술과 F-2 표는 현재 static/js/schedule/ 6모듈 612 LOC 기준으로 고쳤고, F-2 의 네 행은 각각 미해결·미해결·완료·기각으로 갈려서 행별로 표시했다. ADR datedate/domain/0001 은 존재하지 않는 메서드 changeWeek(weekStart) 를 지목하고 있었다 — 실제 시그니처는 changeWeeks(int newWeeks) 이고 인자도 주차 시작일이 아니라 주차 수다. CLAUDE.md 가 문서를 사실의 근거로 인용하지 말라며 든 전례가 정확히 이 유형이다. addParticipant 시그니처와 던지는 예외 타입도 실제와 달라 함께 고쳤고, changeWeeks 의 범위 위반만 IllegalArgumentException 으로 남아 BusinessException 상속 규칙의 예외라는 사실도 적었다. ADR datedate/domain/0002 는 클래스명이 SelectionConverter 로 적혀 있었는데 실제는 SelectionListConverter 라 Decision 과 References 두 곳을 고쳤다. superpowers 문서 세 건은 체크박스를 채우지 않았다 — 2026-07-11 카카오 플랜이 이미 체크박스는 실행 당시 TDD 추적용이라 미체크로 둔다 는 관례를 세워 뒀으므로 같은 형태의 구현 완료 노트를 헤더에 붙이는 쪽으로 맞췄다. 이벤트 트래킹 플랜에는 다섯 개 이벤트의 실제 발화 지점을 파일·라인으로 적고, 계획과 달라진 부분 한 건(analytics.js 가 ES module export 병행이 아니라 classic script IIFE 단일 방식으로 구현돼 window.DDAnalytics 로만 노출된다)을 명시했다 — 계획서의 코드 블록을 정본으로 읽으면 안 되는 자리다. adsense lean-strengthen 스펙은 Status 가 Draft (awaiting user review) 로 남아 있었으나 Component A~E 가 전부 반영돼 Implemented 로 바꾸고, 계획 대비 차이 두 건(별도 fragment 대신 인라인 섹션, 슬러그별 OG 이미지 미적용)을 구현 노트로 남겼다. 카카오 로그인 플랜은 이미 완료 노트가 있어 손대지 않았다."

# Commit 142 — docs(seo,data): AdSense·분석 문서 현행화
git add docs/seo/adsense-low-value-content-remediation.md docs/audit/adsense-low-value-content-policy-mapping.md docs/data-analysis/01-current-state.md docs/data-analysis/04-todo.md docs/data-analysis/README.md docs/troubleshooting/lighthouse-performance-audit.md docs/seo/evolution-playbook.md
git commit -m "docs(seo,data): AdSense 권고 반영 상태·이벤트 수·운영 DB 확정 반영" -m "policy-mapping 의 권고 6건 중 다섯 건이 이미 코드에 반영돼 있었다 — runners 색인 차단, /tools/date-diff 본문 확장, privacy·terms 광고 OFF, insights 광고 가드, 3rem 마진. 3절 표의 반영 상태 열에 근거와 함께 적고 머리말에도 6건 중 5건 반영 이라는 한 줄을 넣었다. 6번 E-E-A-T 만 부분이다 — /about 페이지와 Organization JSON-LD 는 들어갔지만 founder 와 sameAs 는 저장소 전체에 0건이라 미해결로 유지했다. 없는 계정을 sameAs 에 적는 것은 그 자체로 정책 위반이라 채우는 것도 이 작업의 범위가 아니다. remediation 의 배포 블로커 1개 는 해소로 적되 한계를 함께 남겼다 — 블로커 정의가 라이브 /trading 에 noindex 없음 인데 2026-08-02 사이트맵 감사는 /trading 을 측정하지 않았다(그 문서에 trading 문자열이 없다). 무엇으로 해소를 판정했는지(/stock* 실측과 커밋 이력)를 밝히고 /trading 미측정은 미측정으로 적었다. 5절 리뷰 안전 URL 목록은 use-case 4종 기준이라 club-activity 가 빠져 있어 채웠고, 문서마다 6·11·14·24·26·72·78 로 제각각이던 사이트맵 규모 숫자는 단위(엔트리 대 URL 대 hreflang 링크)를 명시해 정합화했다. dataLayer 이벤트 수는 세 문서가 6종 이라고 적고 있었는데 01-current-state 9절 표는 12행이라 같은 파일 안에서 모순이었다. static/js/ 와 templates/ 전체를 grep 해 11종이 13개 지점에서 푸시되는 것을 확인했고(vote_cast 와 schedule_created 가 각각 2지점), 표가 12행인 이유는 vote_cast 한 행이 장소·메뉴 두 지점을 겸하기 때문이라는 설명을 붙였다. 처음 명세는 12지점 이었는데 감사가 13으로 정정했다. 01-current-state 2절 GTM 적용 범위 표의 Runner Admin 은 별도 헤더 서술도 틀렸다 — runner 템플릿 11개가 전부 fragments/head 의 head(seo) 를 쓰고, runners/fragments/header.html 안의 head fragment 는 어느 템플릿도 참조하지 않는 dead code 이며 실제로 쓰이는 건 navbar fragment 뿐이다. GTM 적용 판정 자체는 옳아서 표시는 그대로 뒀다. 10절 운영 DB 형태는 공란이었고 04-todo 는 그것을 BigQuery 미러링의 차단 요건으로 걸어 두고 있었다. .env 의 DB_URL 과 application.yaml 의 driver-class-name 하드코딩과 build.gradle 의 runtimeOnly h2 단일 의존성으로 H2 file 이 확정되므로 채웠고, 그러면 Datastream CDC 가 불가하고 GCS 덤프가 유일 경로라는 결론까지 적어 차단을 풀었다. 04-todo P1-2 와 01-current-state 6절과 lighthouse P3-E 는 이미 존재하지 않는 ad-slot placeholder ca-pub-XXXXXXXXXX 를 작업으로 지목하고 있었다 — ad-slot.html 에 0건이고 세 fragment 모두 hasXxxSlot() 가드가 있어 슬롯 미설정 시 DOM 자체가 나오지 않는다. 남은 작업은 승인 후 환경변수 주입뿐이라는 형태로 다시 썼다. evolution-playbook 9절에는 성격 한 줄을 붙였다 — 서두가 재사용 플레이북이라고 밝히고 있는데도 sitemap 감사가 이 체크리스트의 항목을 이걸로 닫힌다 고 인용한 전례가 있어, 미체크 30개가 이 저장소의 미완료 작업이 아니라는 사실을 9절 자리에서 못박는 편이 안전하다. lighthouse B 항목 재배치는 기각했다 — 제목에 이미 정정 조치 불필요 가 붙어 있어 오독 여지가 실질적으로 없고 순수 구조 변경이라 이 배치의 성격(코드 사실과 어긋난 수치·심볼 정정)에 맞지 않는다."

# Commit 143 — docs: CLAUDE.md·프롬프트 전제 정정 + 커밋 로그 DONE 마커 — 마지막 커밋(git-commit.md 포함)
# 주의: CLAUDE.md 는 Commit 135 가 소유한다. 이번 정정 한 줄과 LF 정규화 371줄이 그쪽으로 흡수되므로
#       이 커밋의 add 목록에는 없다. docs/adr/README.md 도 Commit 133 이 가져간다.
git add docs/prompts/stock-ui.md docs/prompts/home-design-improvement.md docs/prompts/adsense-approval.md docs/guides/git-commit.md
git commit -m "docs: 프롬프트 문서의 낡은 전제 정정 + 커밋 로그 DONE 마커 5건" -m "재사용 프롬프트 세 건이 낡은 전제를 담고 있었다. stock-ui.md 는 Tailwind CDN 기반 이라고 적었는데 실제로는 커밋된 로컬 빌드 산출물이고 CDN 경로는 404 라 로컬 빌드로 대체한 것이며(trading/fragments/header.html 주석에 그 경위가 있다), 대상 화면도 셋으로 나열했지만 settings.html 을 포함해 넷이다. 프롬프트는 그대로 LLM 에 던지는 입력이라 전제가 틀리면 결과가 통째로 어긋난다. home-design-improvement.md 의 다섯 항목은 f0eb6f6·caf93b1·faf5150·94997de·60a17d6 로 전부 커밋됐으므로 제목 직후에 완료 안내를 붙였다. adsense-approval.md 는 상단 의도적 미실행 목록이 하단 실행 기록·실제 코드와 모순이라 상단만 읽으면 이미 끝난 일을 다시 하게 되는 상태였다. 다만 그 목록은 날짜가 박힌 세션 로그 안에 있어서 통째로 덮어쓰면 이력이 훼손된다 — 로그 문단은 그대로 두고 현재 상태를 알리는 부가 노트를 붙이는 쪽으로 바꿨다. footer 동적화 항목만은 실행 결과가 명확해서 본문을 고쳤다: 하드코딩 4개 카드를 T(...).ALL SpEL 로 바꾸라는 제안이었는데 실제로는 UseCaseNavAdvice 의 @ModelAttribute 주입 방식으로 구현돼 footer.html:53 이 th:each 로 순회한다. index.html 의 시나리오 카드가 UseCaseSlugs.ALL 을 순회한다고 적을 뻔했는데 그쪽은 여전히 하드코딩 5개라 그렇게 쓰지 않았다. git-commit.md 에는 Commit 79·80·81·92·93 의 DONE 마커를 달았다 — 각 커밋의 산출물이 전부 HEAD 에 존재하는 것을 확인했고, 마커가 없으면 다음 세션이 미실행으로 오인해 다시 실행하려 한다. 이 배치 전체의 검증은 전체 스위트 100 클래스 590 테스트 failures 0 이며 코드 변경이 0 이므로 직전과 동일한 것이 정상이다. 문서 정확성 자체는 테스트가 보장하지 않으므로, 심는 값을 전부 yaml·Properties·소스에서 재확인하고 새로 등장하는 심볼을 grep 으로 확인하는 방식으로 검증했다."

# =====================================================================
# 코드 주석의 낡은 수치 정정 — 38건 (2026-08-09, 동작 불변)
# =====================================================================
# 배경: 직전 배치(137~143)가 docs/ 문서 149건의 드리프트를 잡으면서, 같은 드리프트가 코드
# 주석에도 남아 있다는 것이 드러났다. 주석 드리프트는 문서보다 위험하다 — 코드를 고치러 온
# 사람이 바로 옆 줄에서 읽는 값이기 때문이다. 실제로 RiskManagementService:125 의 "(+10% 도달
# 시)" 바로 아래 줄이 getTrailingActivation()(0.015 = +1.5%)을 읽고 있었다.
# 방법: 4개 클러스터로 후보 211줄을 훑고 각 정정안을 코드로 반증하는 감사를 붙였다
#       (41건 중 CONFIRMED 38 / CORRECTED 3 / DROPPED 0, 중복 3건 제거 후 38건).
# 검증: ① 코드 무접촉을 기계로 확인 — 선재 미커밋 변경이 없던 12개 파일의 diff 에서
#       주석(//, *, #)이 아닌 +/- 줄이 0 임을 확인했다. 나머지 4개(SignalService·
#       IndicatorService·TradingBotService·application.yaml)의 코드 줄 변경은 전부 Commit
#       129/130/136 의 선재 변경이다. ② 전체 스위트 100 클래스 ／ 590 테스트 ／ failures 0
#       (주석 변경이므로 직전과 동일한 것이 정상). ③ 심는 값을 yaml·Properties·리터럴에서 재확인.
# 원칙: 이력 주석은 손대지 않았다. "return 5;  // P2-7: ±10→±5" 처럼 화살표나 이슈번호가
#       붙은 주석은 변경 기록이라 옛 값이 있어야 뜻이 산다. 정정한 것은 현재 값을 서술하는데
#       값이 다른 주석뿐이고, 반례로 RiskManagementService:147 "왕복 수수료(0.5%)" 는
#       takerFeeRate 0.0025x2 라 정확해서 그대로 뒀다.
# 주의: Commit 129~136 이 미실행이라 SignalService·IndicatorService·TradingBotService 는
#       Commit 130 이, application.yaml 은 Commit 129 가 이 정정을 흡수한다. 각 섹션에 적어 뒀고
#       아래 두 커밋의 add 목록에서는 뺐다.

# Commit 144 — docs(trading): 주석 수치 정정 (동작 불변)
# 주의: SignalService(2건)·IndicatorService(1건)·TradingBotService(3건, P2-12 javadoc 재부착 포함)는
#       Commit 130 이 소유하므로 여기 add 목록에 없다.
git add src/main/java/me/singingsandhill/calendar/trading/application/service/RiskManagementService.java src/main/java/me/singingsandhill/calendar/trading/domain/position/Position.java src/main/java/me/singingsandhill/calendar/trading/presentation/api/TradingVerificationApiController.java
git commit -m "docs(trading): 코드 주석의 낡은 리스크 임계 정정 (동작 불변)" -m "RiskManagementService 의 단계 주석 세 줄이 P1-2 이전 값을 적고 있었다 — 트레일링 활성화 (+10% 도달 시), 추적 폭 (-3% 추적), 익절 (+15%). 세 줄 모두 바로 아래에서 설정값을 읽는데(getTrailingActivation·getTrailingStop·getTakeProfit) 주석만 옛 값에 멈춰 있었고, 운영값은 각각 0.015·0.008·0.03 이다. 숫자만 갈아끼우지 않고 설정 키 이름을 함께 적었다 — 같은 파일 113행이 이미 손절 체크 (stop-loss, 기본 -1.5% — P1-1) 형식이라 그 서식을 따랐고, 이렇게 두면 값이 또 바뀌어도 주석이 가리키는 키는 유효하다. 147행의 왕복 수수료(0.5%) 는 takerFeeRate 0.0025 를 두 번 더한 값이라 정확해서 손대지 않았다 — 이 배치에서 고칠 것과 두어야 할 것을 가르는 기준이 그것이다. Position.shouldRetryClose 의 javadoc 은 수치가 아니라 의미가 틀려 있었다. 시도 횟수가 3회 미만이고 마지막 시도가 5분 이전이거나 시도한 적이 없을 때 라고 적혀 있어 3회 이상이면 영구히 재시도 불가로 읽히지만, 실제 구현은 3회 이상일 때 30분 백오프를 걸 뿐 포기하지 않는다. 세 갈래(미시도·3회 미만·3회 이상)를 그대로 나열하는 쪽으로 다시 썼다. 이 오독은 위험한 종류다 — 청산이 반복 실패하는 포지션을 시스템이 버렸다고 판단하면 수동 개입 시점을 잘못 잡는다. TradingVerificationApiController 의 최소 금액(5,000원)으로 매수 테스트 는 기본값이 5,500원이고 5,000원은 하한 검증값이라 둘을 구분해 적었다. 세 파일 모두 선재 미커밋 변경이 없어서 diff 전체가 이번 변경이고, 주석이 아닌 +/- 줄이 0 인 것을 기계로 확인했다."

# Commit 145 — docs(stock): 주석 수치 정정 (동작 불변) — 마지막 커밋(git-commit.md 포함)
# 주의: application.yaml 의 stock.universe 주석 2줄은 Commit 129 가 소유하므로 여기 없다.
git add src/main/java/me/singingsandhill/calendar/stock/application/service/GapPullbackBotService.java src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java src/main/java/me/singingsandhill/calendar/stock/application/service/UniverseBuilder.java src/main/java/me/singingsandhill/calendar/stock/domain/position/StockCloseReason.java src/main/java/me/singingsandhill/calendar/stock/domain/position/StockPosition.java src/main/java/me/singingsandhill/calendar/stock/domain/stock/Stock.java src/main/java/me/singingsandhill/calendar/stock/domain/stock/StockState.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/api/KisRestClient.java src/main/java/me/singingsandhill/calendar/stock/infrastructure/config/StockProperties.java docs/guides/git-commit.md
git commit -m "docs(stock): 코드 주석을 현재 진입·청산·유니버스 동작에 맞춰 정정 (동작 불변)" -m "stock 모듈 주석의 드리프트는 두 갈래였다. 하나는 ADR algorithm/0007·0009 의 청산 구조 재보정을 따라가지 못한 것이다 — StockCloseReason 의 enum javadoc 이 TP1 을 (+1.5%), TP3 를 (고점 +1%), 손절을 (-1.5%), 트레일링을 (고점 대비 -0.8%) 로 적고 있었는데 실제는 tp1-percent 5.0, tp3-percent 10.0(앵커도 당일고가가 아니라 진입가 고정), 풀백저가 앵커 손절, trailing-stop-percent 2.0 이다. 여기는 숫자를 새로 박는 대신 설정 키를 가리키게 했다 — TP3 앵커처럼 계산 구조 자체가 바뀐 자리는 숫자 하나로 요약하면 또 틀리기 때문이다. StockPosition·Stock·StockState 의 javadoc 도 같은 계열이라 눌림목 범위 -3.0% 를 -5.0% 로, 반등 +0.3% 를 +0.2% 로, FILTERED_OUT 임계를 pullback-max-percent 기준으로 맞췄다. Stock 의 갭 필터 (2.0% ~ 7.0%) 는 감사가 숫자 표기를 기각하고 설정 키로 바꿨다 — 그 메서드가 참조하는 min-gap/max-gap 은 legacy 경로 전용 키라 scoring.enabled=true 인 운영에서는 실효값이 아니고, 숫자를 적으면 그 사실이 가려진다. 다른 하나는 ADR algorithm/0010(유니버스 열화 판정을 top-N 미달로) 이후의 동작 변화다. UniverseBuilder·StockProperties·KisRestClient·application.yaml 이 정적 폴백을 rank 가 비었을 때만 쓰는 안전망 이라고 적고 있었는데, 지금은 거래량순위가 요청한 top-N 에 미달하면 합집합으로 보강한다 — 1건 응답이 정적 안전망을 통째로 무력화했던 2026-08-03 사고가 그 변경의 계기였으므로 주석이 옛 조건을 말하면 사고 원인이 지워진다. 호출 시각도 바뀌었다. GapPullbackBotService 의 클래스 javadoc 타임라인이 09:00~09:10 스크리닝 / 09:10~11:20 매매 로 적혀 있었으나 실제 cron 은 09:20 이고, 프리마켓은 전일 데이터 수집 이 아니라 refreshStaticOnly 로 정적 유니버스만 담는다(거래량순위는 당일 거래량이 없어 호출하지 않는다). UniverseBuilder 와 KisRestClient 의 거래일 1회(pre-market) 호출 서술도 스크리닝 09:20 으로 고쳤다. StockRiskService 는 두 곳이다 — 손절 체크 (-1.5%) 는 현재 손절이 풀백저가 앵커와 진입가 대비 캡의 max 라 그 산식을 가리키게 했고, 시간감쇠 javadoc 의 그 사이 선형 감소 는 구현을 읽어보면 선형 구간이 0 이 아니라 minProfitThresholdLate(0.1%)를 향하고 종점에서만 0 으로 떨어지므로 그렇게 적었다. 이 파일은 최근 커밋 e2a2cf4 로 종점이 설정 유도로 바뀐 참이라 주변 javadoc 은 이미 정확했고 그 부분은 건드리지 않았다. 아홉 파일 모두 선재 미커밋 변경이 없어 diff 전체가 이번 변경이며, 주석이 아닌 +/- 줄이 0 인 것을 기계로 확인했다. 작업 중 실수 하나를 잡아 되돌린 기록을 남긴다 — 일괄 치환 스크립트가 텍스트 모드로 읽고 쓰면서 CRLF 파일 14개를 LF 로 바꿔 약 5,800줄의 개행 잡음을 만들었다. HEAD 가 균일 CRLF 인 것을 확인한 뒤 복원했고, 복원 후 raw diff 와 -w diff 가 일치하는 것으로 잡음이 사라졌음을 확인했다."
