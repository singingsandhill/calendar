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
#   (시스템 경로 / 코멘트는 git 에 전달되지 않음).
# 컨텍스트: AdSense 거절 사유가 "Low value content / 가치 있는 인벤토리" 임이
#   확인되어 1차 plan 의 *분량 가설* 폐기. 실측에서 use-case 본문은 ~500-600단어,
#   privacy／terms 는 ~700-800단어로 분량 자체는 충분 — 진짜 신호는 마케팅 카피
#   톤 + 게시자 신원 모호 + 사이트맵 정합성 + thin-page 노출. 본 섹션의 5개
#   커밋이 이 4축을 일괄 처리.
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

# Commit 79 — docs: ADR 0004(카카오 로그인)·0005(활동 이벤트 recap) + CLAUDE.md 동기화 — 마지막 커밋(git-commit.md 포함)
git add docs/adr/common/security/0004-kakao-oauth2-login.md docs/adr/datedate/domain/0005-user-activity-event-recap.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/common/CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/superpowers/plans/2026-07-11-kakao-login-recap.md docs/guides/git-commit.md
git commit -m "docs: ADR 0004(카카오 OAuth2)·0005(활동 이벤트 recap) + CLAUDE.md 동기화" -m "결정 변경 2건 기록: ClientRegistration 빈 등록(프로퍼티 방식이 @WebMvcTest 슬라이스를 깨뜨림)·client_secret_post·진입점 분리(defaultAuthenticationEntryPointFor 마지막 catch-all)·역할 상호 배타 / append-only 이벤트(서비스 레이어 exists-check, DB unique 없음)·first-claim 동시성 race 수용·on-the-fly 집계. CLAUDE.md Security 표·모듈 섹션 갱신. 전체 테스트 스위트 BUILD SUCCESSFUL(410 tests, 0 failures)."

# Commit 80 — fix(datedate): 최종 리뷰 반영
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

# Commit 81 — docs(datedate): 카카오 로그인·recap 로컬 테스트/서버 반영 체크리스트
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

# Commit 92 — feat(datedate): 통계 스트립 천단위 구분
git add src/main/resources/templates/index.html src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeControllerTest.java
git commit -m "feat(datedate): 홈 통계 스트립 숫자 천단위 구분자 적용" -m "stats-strip 3개 수치(생성된 일정·참여자·투표)를 raw th:text 에서 #numbers.formatInteger(v, 1, 'COMMA') 로 — repo 선례 stock/dashboard.html 과 동일 관용구. i18n 의 {n,number,#} 규칙은 연도 그룹화 방지용이며 통계 수치는 그룹화 대상이므로 무관(SeoServiceI18nTest yearNotGrouped 영향 없음, MessageFormat 미경유). 카운트업 애니메이션은 기각 — animate-on-scroll 인프라가 opacity/transform 전용이라 신규 JS 필요, reduced-motion 대응·유지 비용 대비 체감 이득 없음. HomeControllerTest 에 12,345 / 9,012 렌더 어서션 추가."

# Commit 93 — style(datedate): 홈 시각 일관성 + 접근성 폴리시 — 마지막 커밋(git-commit.md 포함)
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
# (신규: RiskManagementServiceTriggerTest 6, PullbackEntryValidationTest 2 포함).
# ⚠️ application.yaml 에 stock(entry·risk)·trading(bot.mode) 변경이 섞여 있음 — 110/112 에서
#    git add -p 로 해당 훅만 스테이징. (앞선 103~109 미실행 상태로 순서대로 실행 시, 공유
#    파일의 이 섹션 변경분이 앞 커밋에 함께 딸려 들어갈 수 있음 — 내용 유실은 없음.)

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
# 배경: 프로젝트 11개 CLAUDE.md 를 소스코드와 1:1 대조 감사. 루트 CLAUDE.md 가 스스로 정한
# 규칙("모듈별 CLAUDE.md 는 현재 코드의 사실만 담는다")에서 이탈한 3종을 정정 — ① 존재하지
# 않는 심볼·경로를 지목한 사실 오류(문서를 믿고 grep 하면 못 찾음), ② 안전·수익에 직결되나
# 어느 문서에도 없던 동작, ③ 표·목록 열거 누락. 문서 전용 변경 — 프로덕션 코드 무변경,
# 전체 스위트 GREEN(85 클래스 / 503 테스트 / 실패 0).
# 검증: 정정 심볼 코드 존재 재grep + 낡은 심볼 0건 + ADR 링크 무결성 + ADR 색인 완전성
#       + 템플릿 13디렉터리 대조 + gradlew test.
# ⚠️ stock/CLAUDE.md · trading/CLAUDE.md · docs/adr/README.md 는 Commit 110/112/113 과
#    공유되는 파일 — 앞 커밋들을 아직 실행하지 않았다면 이 커밋에서는 git add -p 로
#    문서 감사 훅만 스테이징(수치 변경분은 110/112 소유). 내용 유실은 없음.

# Commit 114 — ✅ DONE() docs: CLAUDE.md 전수 감사 — 사실 오류 6건 정정 + 누락 동작·열거 보강
# 주: Commit 113 이 이미 docs/git_commit.md 를 커밋하므로(그 시점 파일에 이 114 섹션도 포함됨)
#     순서대로 실행하면 아래 git_commit.md add 는 no-op 이 된다 — 나머지 문서 파일만 커밋된다.
#     113 을 실행하지 않고 114 만 단독 실행할 때만 git_commit.md 가 실제로 스테이징된다.
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

# Commit 116 — fix(datedate): 카카오 프로필 이미지 URL http→https 정규화 — 마지막 커밋(git-commit.md 포함)
# 주: Commit 115 가 이미 docs/guides/git-commit.md 를 커밋하므로(그 시점 파일에 이 116
#     섹션도 포함됨) 순서대로 실행하면 아래 git-commit.md add 는 no-op 이 된다.
#     115 를 실행하지 않고 116 만 단독 실행할 때만 실제로 스테이징된다.
git add src/main/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfile.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/security/KakaoProfileTest.java docs/guides/git-commit.md
git commit -m "fix(datedate): 카카오 프로필 이미지 URL 을 https 로 정규화" -m "카카오 /v2/user/me 가 profile_image_url(및 properties.profile_image 폴백)을 http 스킴으로 내려주는데 KakaoProfile.from 이 원본을 그대로 통과시켜, HTTPS 로 서빙되는 페이지에서 <img src=\"http://k.kakaocdn.net/...\"> 가 렌더링되고 있었다. 브라우저 콘솔에 Chrome 의 혼합 콘텐츠 경고가 로그인 사용자 전 페이지(공용 헤더 4곳 + 마이페이지 1곳)에서 반복 출력된다. Chrome 81+ 는 이런 수동적 리소스를 자동으로 https 로 올려 재요청하고 실패 시 http 로 폴백하지 않고 차단하므로, 현재는 k.kakaocdn.net 이 HTTPS 를 정상 제공(TLS 핸드셰이크·인증서 검증 통과)해 이미지가 보이지만 CDN 이 특정 경로에서 HTTPS 를 주지 못하면 곧바로 깨진 이미지가 된다 — 브라우저 자동 업그레이드에 의존하지 않도록 서버에서 확정한다. 수정은 파싱 단계 한 곳(KakaoProfile.toHttps): profile 우선·properties 폴백이 firstNonBlank 로 합류한 뒤 한 번만 정규화되므로 두 경로가 함께 커버되고, 렌더 지점 5곳을 각각 손대는 중복이 없다. 저장 값도 자연히 치유된다 — KakaoOAuth2UserService 는 upsert 가 반환한 AppUser.getProfileImageUrl() 을 appProfileImage 속성에 싣고 AppUserService.upsertKakaoUser 는 재로그인마다 refreshProfile 로 갱신하므로, 기존 http 로 저장된 행은 다음 로그인 시점에 https 로 덮인다(별도 데이터 마이그레이션 불필요). http:// 접두사가 아닌 값(이미 https, 프로토콜 상대 //, null)은 그대로 통과. 회귀 가드 2건: KakaoProfileTest 의 upgradesInsecureProfileImageUrl(kakao_account.profile 경로) / upgradesInsecureProfileImageUrlFromProperties(properties 폴백 경로) — 수정 전 RED 확인 후 GREEN, 기존 4건 포함 6건 전부 통과."
