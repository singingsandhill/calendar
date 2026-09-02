# Architecture Decision Records (ADR)

이 디렉토리는 Calendar 프로젝트의 굵직한 아키텍처·정책 결정을 **결정 단위**로
보관한다. 새 멤버가 들어왔을 때, PR 리뷰에서 결정을 인용할 때, 비슷한 회귀를
재현하지 않으려 할 때 첫 번째로 펼치는 곳.

각 ADR 은 한 가지 결정만 담고, 외부 트리거(왜 그 결정이 강요됐는가) → 결정 →
대안 비교 → 영향 순서로 서술한다.

작성 컨벤션과 템플릿: [`_template.md`](_template.md).

---

## View 1 — 도메인 × 관심사 매트릭스

| 도메인 \ 관심사 | 도메인 모델 | 인프라/외부 | UX·프론트 | SEO | i18n | 관측성 | 알고리즘 | 모드 | 보안·에러 | 합계 |
|---|---|---|---|---|---|---|---|---|---|---|
| **common**   | — | 2 | — | 11 | 3 | — | — | — | 7 | 23 |
| **datedate** | 8 | — | 6 | — | — | — | — | — | — | 14 |
| **runner**   | 2 | — | — | — | — | — | — | — | — | 2 |
| **trading**  | — | 5 | — | — | — | 3 | 16 | 3 | — | 27 |
| **stock**    | — | 8 (동시성 포함) | — | — | — | 2 | 10 | 3 | — | 23 |
| **합계** | 10 | 15 | 6 | 11 | 3 | 5 | 26 | 6 | 7 | **89** |

총 **89개 ADR**.

---

## View 2 — 시간순 (커밋 author date 기준)

| 시기 | 도메인 | ADR | 트리거 |
|---|---|---|---|
| 2025-12-14 | common/seo | [0001 SeoMetadata SSOT](common/seo/0001-seo-metadata-as-ssot.md) | MVP 출시 #9 |
| 2025-12-19 | common/seo | [0007 콘텐츠 페이지 확장](common/seo/0007-content-pages-for-adsense.md) | AdSense 거절 회복 #14 |
| 2025-12-20 | common/seo | [0006 ads.txt / naver 정적 엔드포인트](common/seo/0006-explicit-static-endpoints.md) | AdSense 인식 실패 #10 |
| 2025-12-27 | common/security | [0001 Runner 어드민 폼 로그인](common/security/0001-runner-admin-only-form-login.md) | 러닝 크루 페이지 #17 |
| 2026-01-01 | trading/strategy | [0001 8개 지표 컨센서스](trading/strategy/0001-multi-indicator-consensus.md) | 코인 봇 초기 구현 #17 |
| 2026-01-06 | common/seo | [0002 HTTP→HTTPS 통일](common/seo/0002-http-to-https-unification.md) | GSC "리디렉션 포함된 페이지" #18 |
| 2026-01-11 | runner | [0002 출석 삭제 어드민 전용](runner/0002-attendance-deletion.md) | 운영 #17 |
| 2026-01-12 | runner | [0001 인증 없이 런 생성](runner/0001-anonymous-run-creation.md) | 가입 마찰 제거 #17 |
| 2026-01-16 | common/error-handling | [0001 REST/MVC 2-layer 예외 처리](common/error-handling/0001-two-layer-exception-handling.md) | 에러 처리 강화 #16 |
| 2026-01-23 | stock/infrastructure | [0004 N+1 제거 + 배치 + KIS 재시도](stock/infrastructure/0004-n-plus-one-and-batch-candle.md) | 봇 알고리즘 1차 정리 #17 |
| 2026-01-25 | trading/strategy | [0003 적자 매매 방지 가드](trading/strategy/0003-loss-prevention-guards.md) | 코인 적자 매매 #17 |
| 2026-04-07 | common/seo | [0005 robots Disallow 좁히기](common/seo/0005-robots-disallow-narrowing.md) | GSC 색인 실패 |
| 2026-04-12 | trading/strategy | [0002 MA 수렴 시 크로스 억제](trading/strategy/0002-ma-convergence-suppression.md) | 횡보장 위신호 |
| 2026-04-13 | common/error-handling | (포함, ADR 0001 참조) | — |
| 2026-04-19 | common/i18n | [0002 로케일 즉시 적용](common/i18n/0002-immediate-locale-application.md) | KO→EN 토글 한 번에 안 먹는 버그 |
| 2026-04-20 | datedate/domain | [0001 Schedule 애그리거트 불변식](datedate/domain/0001-schedule-aggregate-invariants.md) | 도메인 정합 #17 |
| 2026-04-20 | datedate/domain | [0002 Selection JSON 컨버터](datedate/domain/0002-selections-json-converter.md) | 영속성 단순화 |
| 2026-04-20 | datedate/domain | [0003 일정 미존재 시 create 분기](datedate/domain/0003-no-auto-create-on-missing-schedule.md) | 자동 생성 부작용 |
| 2026-04-20 | datedate/frontend | [0001 ES 모듈 분해](datedate/frontend/0001-es-module-decomposition.md) | inline 핸들러 제거 |
| 2026-05-01 | stock/observability | [0001 TradeEvents + KST 회전 + BotStatus 메트릭](stock/observability/0001-trade-events-logger-and-bot-metrics.md) | "봇이 죽었나 후보가 없나" 구분 |
| 2026-05-01 | stock/algorithm | [0001 시간/설정 정합화 + 매직넘버 외부화](stock/algorithm/0001-time-config-and-magic-numbers-externalized.md) | 운영 토글 가능 |
| 2026-05-01 | stock/algorithm | [0002 UniverseBuilder 스냅샷](stock/algorithm/0002-universe-builder-snapshot.md) | pre-market 비어있던 TODO |
| 2026-05-01 | stock/algorithm | [0003 진입 검증 위양성 제거](stock/algorithm/0003-entry-validation-strictness.md) | orderbook null 통과 버그 |
| 2026-05-01 | stock/algorithm | [0004 TP1·TP2·TP3 비순차화](stock/algorithm/0004-tp-independent-triggers.md) | 익절 누락 |
| 2026-05-01 | stock/infrastructure | [0001 KIS rate-limit Semaphore](stock/infrastructure/0001-kis-rate-limit-semaphore.md) | KIS 429 |
| 2026-05-01 | stock/infrastructure | [0002 종목별 ReentrantLock](stock/infrastructure/0002-per-symbol-reentrant-lock.md) | 매수/매도 race |
| 2026-05-01 | stock/infrastructure | [0003 ThreadPoolTaskScheduler](stock/infrastructure/0003-thread-pool-task-scheduler.md) | 스크리닝/트레이딩 병렬 |
| 2026-05-01 | stock/modes | [0001 PAPER/BACKTEST + Clock 빈](stock/modes/0001-paper-backtest-mode-and-clock-bean.md) | 결정성 테스트 |
| 2026-05-01 | common/i18n | [0001 Cookie-then-Accept-Language](common/i18n/0001-cookie-then-accept-language-resolver.md) | KO 기본 + EN 보조 |
| 2026-05-01 | common/i18n | [0003 NumberFormat 천단위 그룹화 차단](common/i18n/0003-message-format-numberformat-grouping.md) | "2,026/5" 타이틀 버그 |
| 2026-05-01 | common/seo | [0003 Sitemap lastmod 신뢰도 회복](common/seo/0003-trustworthy-sitemap-lastmod.md) | Google 2023 lastmod 정책 |
| 2026-05-01 | common/seo | [0004 hreflang + 토글 rel 속성](common/seo/0004-hreflang-canonical-locale-toggle.md) | i18n SEO 색인 |
| 2026-05-01 | datedate/frontend | [0002 공유 Create 모달](datedate/frontend/0002-shared-create-schedule-modal.md) | CTA 컨텍스트 이탈 |
| 2026-05-01 | datedate/frontend | [0003 모달 Year/Month 동적](datedate/frontend/0003-modal-year-month-dynamic-init.md) | 시점 캐시 버그 |
| 2026-05-01 | datedate/ux | [0001 참가자 색 ↔ 셀 강조 동기화](datedate/ux/0001-participant-color-cell-sync.md) | 누가 선택했는지 식별 불가 |
| 2026-05-01 | datedate/ux | [0002 온보딩 배너 영구화](datedate/ux/0002-onboarding-banner-persistence.md) | 다시 열 방법 부재 |
| 2026-05-01 | datedate/ux | [0003 링크 4-state + 카드 어포던스](datedate/ux/0003-link-state-and-card-affordance.md) | 탭 오인 / :visited 보라 |
| 2026-05-18 | common/seo | [0009 IndexNow 능동 색인 제출](common/seo/0009-indexnow-active-submission.md) | 크롤러 재방문 대기로 인한 색인 지연 — Bing/Yandex/Naver 보조 채널 |
| 2026-05-30 | trading/modes | [0001 PAPER 기본 모드 + 주문 게이트](trading/modes/0001-paper-mode-default-and-order-gate.md) | 수익성 감사 P0-1 — 실거래 사고 방지 / 백테스트 |
| 2026-05-30 | trading/risk | [0001 서킷브레이커(일일·연속 손실)](trading/risk/0001-circuit-breaker-daily-and-consecutive-loss.md) | 수익성 감사 P0-2 — 데스스파이럴 차단 |
| 2026-05-30 | trading/risk | [0002 리밸런싱 회계 정합](trading/risk/0002-rebalance-position-accounting.md) | 수익성 감사 P1-3 — 유령 코인/잔고 드리프트 |
| 2026-06-03 | stock/algorithm | [0005 거래량순위 동적 유니버스](stock/algorithm/0005-dynamic-universe-volume-rank.md) | 봇 로그 분석 P0-2 — rank=0 정적 대형주 풀 |
| 2026-05-30 | trading/risk | [0003 진입·시간 리스크 가드](trading/risk/0003-entry-and-time-risk-guards.md) | 수익성 감사 P2-8/10/12 — 정체/물타기/과집중 |
| 2026-05-30 | trading/risk | [0004 수동매매 정합 + 엔진 핑퐁 방지](trading/risk/0004-manual-trade-position-consistency-and-engine-coordination.md) | 수익성 감사 #3/P2-11/P2-9 |
| 2026-05-30 | trading/infrastructure | [0001 주문 실행 트랜잭션 경계](trading/infrastructure/0001-order-execution-transaction-boundary.md) | 수익성 감사 P0-3 — HTTP/sleep in tx |
| 2026-05-30 | trading/strategy | [0004 수수료 비용모델/순수 마진 임계](trading/strategy/0004-fee-cost-model-and-net-margin-threshold.md) | 수익성 감사 P1-8/P1-9 — 수수료 이중계상 |
| 2026-05-30 | trading/strategy | [0005 출구 R:R 재보정](trading/strategy/0005-exit-risk-reward-recalibration.md) | 수익성 감사 P1-1/P1-2 — 보상/위험 역전 |
| 2026-05-30 | trading/strategy | [0006 Wilder RSI](trading/strategy/0006-wilder-rsi.md) | 수익성 감사 P2-4 — 비표준 RSI |
| 2026-05-30 | trading/strategy | [0007 다이버전스 피벗 강화](trading/strategy/0007-divergence-pivot-strengthening.md) | 수익성 감사 P2-1 — 1분봉 잡음 피벗 |
| 2026-05-30 | trading/strategy | [0008 지표 잡음 감소(Slow Stoch/RSI추세)](trading/strategy/0008-indicator-noise-reduction.md) | 수익성 감사 P2-5/P2-6 |
| 2026-05-30 | trading/strategy | [0009 형성봉 제외(기본 OFF)](trading/strategy/0009-exclude-forming-candle.md) | 수익성 감사 P2-2 — 룩어헤드/리페인트 |
| 2026-05-30 | trading/strategy | [0010 모멘텀 가중 하향](trading/strategy/0010-momentum-weight-reduction.md) | 수익성 감사 P2-7 — ADR-0001 ±135 정련 |
| 2026-06-07 | common/security | [0002 앱인토스 미니앱용 /api CORS 허용](common/security/0002-cors-for-apps-in-toss-miniapp.md) | 미니앱(별도 origin) fetch 가 preflight 에서 차단 |
| 2026-06-11 | datedate/domain | [0004 GET owner 자동 생성 제거 → 404 + 빈 대시보드](datedate/domain/0004-no-owner-auto-create-on-get-dashboard.md) | AdSense 재심사 감사 — 소프트 404 / GET mutation |
| 2026-07-06 | common/security | [0003 트레이딩 제어·실주문 API 관리자 전용](common/security/0003-admin-only-trading-control-api.md) | 운영 리뷰 P0-1 — 무인증 실주문 API 노출 |
| 2026-07-08 | trading/infrastructure | [0002 주문 선영속화 + 틱 스윕 + Position 생성](trading/infrastructure/0002-order-pre-persistence-and-tick-sweep.md) | 운영 리뷰 §8-B — 응답 유실 시 무보호 포지션 |
| 2026-07-08 | trading/infrastructure | [0003 Bithumb v2 주문 API 마이그레이션](trading/infrastructure/0003-bithumb-v2-order-api-migration.md) | v2 릴리스(2026-06-30) — 어댑터 정규화 + 기본 V1 라우팅 |
| 2026-07-11 | common/security | [0004 카카오 OAuth2 로그인](common/security/0004-kakao-oauth2-login.md) | datedate "내 기록" 기능 — 선택적 로그인 도입 |
| 2026-07-11 | datedate/domain | [0005 활동 이벤트 테이블 기반 연간 recap](datedate/domain/0005-user-activity-event-recap.md) | 연간 Wrapped 스타일 recap — voters 구조 무변경 |
| 2026-07-17 | datedate/domain | [0006 인기 순위 노출 기준 (2표+블록리스트)](datedate/domain/0006-popularity-exposure-criteria.md) | 홈 첫 화면에 0표·비속어 입력 원문 노출 |
| 2026-07-24 | stock/algorithm | [0006 거래량순위 스크리닝 시점 재시도](stock/algorithm/0006-universe-rank-retry-at-screening.md) | 봇 로그 분석 — 08:30 rank 매일 0건, 4거래일 연속 Selected: 0 |
| 2026-07-24 | common/security | [0005 주식 봇 제어 API 관리자 전용](common/security/0005-admin-only-stock-bot-control-api.md) | 로직 리뷰 P0-1 — 무인증 봇 제어 노출 (critical) |
| 2026-07-24 | stock/modes | [0002 PAPER 기본 모드](stock/modes/0002-paper-default-mode.md) | 로직 리뷰 P0-2 — 미검증 전략 + LIVE 기본값 |
| 2026-07-24 | stock/infrastructure | [0005 비멱등 주문 무재시도](stock/infrastructure/0005-non-idempotent-order-no-retry.md) | 로직 리뷰 P0-4 — 응답 유실 시 중복 시장가 주문 |
| 2026-07-24 | stock/algorithm | [0007 청산 구조 재보정 (풀백저가 손절·고정 TP3·트레일링)](stock/algorithm/0007-exit-structure-recalibration.md) | 로직 리뷰 P1-2/3/4 — 손익분기 승률 83% R:R 역전 |
| 2026-07-24 | stock/infrastructure | [0006 주문 선영속화 + 실체결 backfill + 고아 체결 스윕](stock/infrastructure/0006-order-pre-persistence-and-fill-backfill.md) | 로직 리뷰 P1-1 — 픽션 체결가·무보호 포지션 |
| 2026-07-24 | stock/modes | [0003 보호 전용 복구 + 최종청산 재시도·알림](stock/modes/0003-protection-only-recovery-on-restart.md) | 로직 리뷰 P1-5/6 — 재시작 시 보호 상실·청산 원샷 |
| 2026-07-24 | stock/algorithm | [0008 선정 규칙 강화 · 비용 모델 정정 · 거래정지 가드](stock/algorithm/0008-screening-selection-and-cost-model.md) | 로직 리뷰 P2-1/2/3 — 강제 선정·점수 왜곡·세율 오차·VI 무방비 |
| 2026-07-24 | stock/observability | [0002 일일 실적 요약 리포트](stock/observability/0002-daily-performance-report.md) | 로직 리뷰 P2-5 — PAPER 실측 데이터원 |
| 2026-07-25 | stock/algorithm | [0009 진입 풀백 하한·트레일링 비용 정합](stock/algorithm/0009-entry-floor-and-trailing-cost-alignment.md) | 수익성 검사 — 얕은 풀백 익절 불가·트레일링 본전 퇴화 |
| 2026-07-25 | trading/modes | [0002 PAPER 기본 모드 전환](trading/modes/0002-paper-default-mode.md) | 수익성 검사 + 운영 감사 P1-8 — fail-dangerous 기본값 |
| 2026-07-27 | stock/infrastructure | [0007 체결강도 소스 inquire-ccnl 전환](stock/infrastructure/0007-trade-strength-source-inquire-ccnl.md) | 운영 로그 — cttr 필드 부재로 스크리닝 전멸·진입검증 상시 FAIL |
| 2026-07-27 | trading/infrastructure | [0004 Reactor Netty 커넥션 풀 idle 폐기](trading/infrastructure/0004-reactor-netty-connection-pool-policy.md) | 운영 로그 — stale 커넥션 PrematureCloseException 반복 |
| 2026-07-27 | trading/risk | [0005 현재가 조회 실패 시 진입 가드 차단](trading/risk/0005-fail-safe-entry-guard-on-price-unavailable.md) | 운영 로그 — 조회 실패 시 가드 우회 매수(fail-open) |
| 2026-07-30 | common/seo | [0008 BreadcrumbList 2단계 + 전 항목 item](common/seo/0008-breadcrumb-item-on-every-listitem.md) | GSC "'item' 입력란 누락" — 3단계 중간 크럼브 URL 부재로 6개 URL 리치 결과 제외 |
| 2026-08-03 | stock/algorithm | [0010 유니버스 열화 판정 = top-N 미달](stock/algorithm/0010-universe-degradation-threshold.md) | 운영 로그 — rank 1건 응답이 정적 안전망·09:20 재시도를 동시에 무력화, 하루치 유니버스 1종목 |
| 2026-08-06 | trading/infrastructure | [0005 캔들 보관 90일 + 설정화](trading/infrastructure/0005-candle-retention-for-analysis.md) | 감사 P2 판단 유보 — 7일 하드코딩이 지표 재계산·파라미터 리플레이를 구조적으로 봉쇄 |
| 2026-08-06 | trading/observability | [0001 신호 품질 분석 온-리드 집계 + 온디맨드 페이지](trading/observability/0001-signal-quality-analytics-page.md) | 감사 P2-1·출구 R:R 이 "백테스트 없는 prior" 로 미해결 — 이미 쌓인 신호를 읽을 수단 부재 |
| 2026-08-06 | trading/observability | [0002 결정 입력 영속화 (signal_id·ATR·거래량·주문비중)](trading/observability/0002-decision-input-persistence.md) | 체결↔신호 조인 키 부재 + `executed` 죽은 컬럼 + ATR·적용 비중 계산 후 폐기 |
| 2026-08-06 | trading/observability | [0003 리스크 청산 틱에도 신호 기록](trading/observability/0003-signal-series-continuity-on-risk-exit.md) | 조기 return 으로 손절·익절 발동 분의 지표 상태가 1분 시계열에서 체계적으로 결측 |
| 2026-08-08 | trading/risk | [0006 캔들 동기화 실패는 리스크 체크를 게이팅하지 않는다](trading/risk/0006-candle-sync-failure-does-not-gate-risk-check.md) | observability/0003 후속 ② — 캔들 수집 한 줄이 무방비라 조회 실패가 그 틱의 손절·익절을 통째로 스킵 |
| 2026-08-13 | datedate/domain | [0007 랜덤 owner ID 미사용 보장 + 공간 1000배](datedate/domain/0007-collision-free-random-owner-id.md) | 14,400 조합·중복 무검사 — 누적 141건이면 충돌 확률 50%, 충돌 시 남의 페이지 착지·선점 |
| 2026-08-17 | common/seo | [0010 홈 adsEnabled(false) + CLS 진단 기록](common/seo/0010-home-ads-script-off-and-cls-diagnosis.md) | GSC 데스크톱 CLS 0.15 — 진단 결과 그룹핑 아티팩트(홈 URL CrUX 0.00), 홈 광고 스크립트 게이트만 정책과 불일치 |
| 2026-08-17 | trading/modes | [0003 재시작 시 보호 전용 자동 재개](trading/modes/0003-protection-only-recovery-on-restart.md) | CI/CD 설계 — 재시작 후 수동 start 전까지 손절 보호 공백(P1-3)이 "배포마다 일어나는 일"이 됨 |
| 2026-08-17 | common/security | [0006 actuator health permitAll + denyAll + h2-console 기본 비활성](common/security/0006-actuator-health-and-h2-console-lockdown.md) | 배포 헬스 게이트 필요 + `/*/*/*` permitAll 이 3세그먼트 actuator 를 이미 공개 + 운영 무인증 SQL 콘솔 노출 |
| 2026-08-17 | common/infrastructure | [0001 GHCR + compose 재시작 배포 파이프라인](common/infrastructure/0001-container-restart-deploy-pipeline.md) | 서버 수동 빌드·272MB stdout 로그·롤백 수단 부재 — blue-green 은 H2 배타락·스케줄러 이중실행·RAM 으로 불가 판정 |
| 2026-08-17 | common/infrastructure | [0002 nginx compose 편입 + certbot webroot + 503 유지보수](common/infrastructure/0002-nginx-in-compose-and-certbot-webroot.md) | nginx 설정 서버 유일본 + 배포 중 502 노출(SEO) + 컨테이너화 시 certbot nginx 플러그인 파손 |
| 2026-08-18 | datedate/domain | [0008 미지 콘텐츠 슬러그는 HTTP 404](datedate/domain/0008-unknown-content-slug-404.md) | AdSense 3차 통지 진단 — 미지 use-case 슬러그 302→홈이 소프트 404 신호 (owner 404 와 비대칭) |
| 2026-08-23 | common/seo | [0011 /guides 에디토리얼 허브 — 기사별 전용 템플릿 + 날짜 SSOT](common/seo/0011-guides-editorial-hub.md) | 3차 통지의 editorial 판정 — 검색의도형 장문 카테고리 부재 + use-case 의 "복제 템플릿" 신호 회피 필요 |
| 2026-08-31 | stock/infrastructure | [0008 종목명 소스 search-stock-info](stock/infrastructure/0008-stock-name-source-search-stock-info.md) | 09:20 메일·대시보드 종목명 칸에 종목코드 인쇄 — 스크리닝 TR 3종에 종목명 필드 부재, 최초 커밋부터 placeholder |

---

## 도메인별 폴더 구조

- [common/seo/](common/seo/) — 10 ADRs
- [common/i18n/](common/i18n/) — 3 ADRs
- [common/error-handling/](common/error-handling/) — 1 ADR
- [common/security/](common/security/) — 6 ADRs
- [common/infrastructure/](common/infrastructure/) — 2 ADRs
- [datedate/domain/](datedate/domain/) — 7 ADRs
- [datedate/frontend/](datedate/frontend/) — 3 ADRs
- [datedate/ux/](datedate/ux/) — 3 ADRs
- [runner/](runner/) — 2 ADRs
- [trading/strategy/](trading/strategy/) — 10 ADRs
- [trading/modes/](trading/modes/) — 3 ADRs
- [trading/risk/](trading/risk/) — 6 ADRs
- [trading/infrastructure/](trading/infrastructure/) — 5 ADRs
- [trading/observability/](trading/observability/) — 3 ADRs
- [stock/algorithm/](stock/algorithm/) — 10 ADRs
- [stock/infrastructure/](stock/infrastructure/) — 8 ADRs
- [stock/modes/](stock/modes/) — 3 ADRs
- [stock/observability/](stock/observability/) — 2 ADRs

---

## 작성 규칙

- ADR 번호는 폴더별 0001 부터. 충돌 방지를 위해 도메인 간 번호 공유하지 않음.
- 결정 1개 = ADR 1개. 묶지 않음.
- 한국어 우선. 식별자/메서드명/파일명/이벤트명은 영문 그대로.
- 본문은 자체완결: 외부 docs 를 보지 않아도 결정 의도가 이해되도록 작성.
- 추가 맥락이 필요한 독자를 위해 References 에 cross-link.
- 비결정성 변경(typo, 미세 CSS, dep 버전 업)은 ADR 화하지 않음.

새 ADR 추가 시 위 두 뷰 모두에 한 줄씩 추가할 것.
