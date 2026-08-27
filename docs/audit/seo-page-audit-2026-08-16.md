# 페이지별 SEO 점검 — 전 모듈 공개 페이지 전수 (2026-08-16)

> 대상: 뷰를 렌더링하는 **모든 MVC 라우트** (datedate 19 · runners 공개 8 · runners/admin 6 ·
> trading 6 · stock 3 · error 2) 의 `<head>` 메타데이터 — title / description / robots /
> canonical / hreflang / OG / JSON-LD / sitemap 수록 여부.
> 방법: ① 코드 정적 분석 (컨트롤러 → `SeoService`·인라인 빌더 → head fragment 전수 추적)
> ② 배포본 `https://datedate.site` 실측 (측정 시각 2026-08-16 19:01 KST, §6)
> ③ 기존 감사·ADR 과 대조해 **신규 발견과 기지(旣知) 이슈를 분리**.
> 성격: 점검 보고만 — **이 작업으로 코드는 바뀌지 않는다.** 수정은 후속 작업(§7 우선순위).
> 기준 커밋: `edba5b6`. 관련: [sitemap 점검 2026-08-02](sitemap-audit-2026-08-02.md) ·
> [AdSense 저가치 콘텐츠 조치](../seo/adsense-low-value-content-remediation.md) ·
> [ADR common/seo/](../adr/common/seo/).

---

## 1. 한 줄 결론

**색인 표면(인덱서블 13경로 × ko/en = 26 URL)은 건강하다.** 실측에서 26 URL 전부 200 ·
`index, follow` · self-canonical · hreflang 정합, 비색인 표면(noindex/차단)도 전부 의도대로
동작 중이다. 이번 점검의 신규 발견 13건은 **색인 자체를 해치는 결함 0건** — 공유(OG) 카드
정확성, 유지보수 함정(고아 fragment·무가드 호출), 로케일 유실이 주된 축이고 전부 중요도 中 이하다.
아울러 remediation §7 에 미완으로 남아 있던 **실서비스 `/trading` robots 실측을 이번에 수행해
해소했다** (302 → 어드민 로그인, §6).

## 2. 페이지 인벤토리 — 그룹별 점검표

### 2-1. 인덱서블 13경로 (datedate) — 전부 정상

SEO 소스는 모두 `SeoService` + `fragments/head.html :: head(seo)`. 공통으로 description ·
keywords · self-canonical(로케일별) · hreflang 3종(ko/en/x-default) · OG/Twitter ·
JSON-LD · sitemap 수록을 갖춘다. 실측 전부 200 (§6).

| 경로 | SeoService | robots | JSON-LD | ads |
|---|---|---|---|---|
| `/` | `getHomeSeo()` :155 | index, follow | WebApplication+WebSite+Organization+BreadcrumbList+FAQPage | ON |
| `/guide` | `getGuideSeo()` :365 | index, follow | HowTo(5단계)+BreadcrumbList | ON |
| `/about` | `getAboutSeo()` :616 | index, follow | AboutPage+Organization/ContactPoint+BreadcrumbList | OFF |
| `/privacy` `/terms` | `buildSimpleWebPageSeo()` :693 | index, follow | WebPage+BreadcrumbList | OFF |
| `/faq` | `getFaqSeo()` :731 | index, follow | FAQPage(6)+BreadcrumbList | ON |
| `/tools/date-diff` | `getDateDiffSeo()` :788 | index, follow | WebApplication+BreadcrumbList | ON |
| `/insights/trends` | `getInsightsTrendsSeo(hasData)` :572 | 데이터 有 index / 無 noindex,follow | WebPage+BreadcrumbList | =hasData |
| `/use-cases/{slug}` ×5 | `getUseCaseSeo(slug)` :449 | index, follow | WebPage+BreadcrumbList+HowTo+FAQPage(조건부) | ON |

`/insights/trends` 는 실측에서 `index, follow` — 운영 DB 에 인기 데이터가 존재하고 sitemap 에도
수록돼(26 loc) 코드의 조건부 강등 로직과 일관된다.

### 2-2. datedate noindex 7종 — 정상

전부 `noindex, nofollow` + `hreflangEnabled(false)`, sitemap 미수록. `/{ownerId}` ·
`/{ownerId}/{y}/{m}` · `/login` · `/me` · `/recap/{year}` · `/recap/share/{token}` ·
`schedule/create`(뷰 분기). recap 공유 페이지는 OG(article)를 채워 카카오톡 미리보기 대응.
미존재 ownerId 는 404 + noindex (실측 확인, ADR datedate/domain/0004 준수).

### 2-3. runners 공개 8 라우트 — noindex 정상, 품질 이슈 다수 (§4)

`RunnerController` 인라인 `SeoMetadata.builder()`. 전부 `noindex, follow`
(런 생성 폼과 그 에러 재렌더는 `noindex, nofollow`), canonical·ogImage(`crew_logo.png`)
설정, sitemap 미수록(AS-Content 신호 회피 — 의도). 홈만 JSON-LD(SportsTeam+BreadcrumbList)
보유. 실측 `/runners` = `noindex, follow` 일치.

### 2-4. runners/admin 6 · trading 6 · stock 3 — 색인 차단 정상

| 표면 | 차단 방식 | 비고 |
|---|---|---|
| `/runners/admin/**` | `createAdminSeo()` `noindex,nofollow` + robots.txt Disallow + no-cache 헤더 | canonical 이 6페이지 공통 `/runners/admin` (§4-11) |
| `/trading/**` | 템플릿 하드코딩 `noindex,nofollow,noarchive` + `ROLE_ADMIN`(302) + Disallow | **실측 302 → `/runners/admin/login`** — remediation §7 잔여 항목 해소 |
| `/stock/**` | 템플릿 하드코딩 `noindex,nofollow,noarchive` + Disallow | `permitAll` 이라 공개 도달 가능. bare `/stock` 은 robots 미매치(기지 이슈 §5) — 실측에서 메타 noindex 가 동작 확인 |

trading/stock 은 SeoMetadata 미사용 — description/canonical/OG/파비콘 전무, 모듈당 단일
`<title>` (`ADA Trading Bot` / `Stock Trading Bot - Gap & Pullback`). 색인 차단 목적상 문제
없으나 §4-13 참고.

### 2-5. error 2종 — noindex 는 있으나 head 가 자체 구현 (§4-5)

`error/4xx.html:3-17` · `error/5xx.html:3-17` 인라인 head: title + `noindex` + 파비콘뿐.
description·canonical·OG·GTM 부재, Pretendard 를 동기 로드(공용 head 는 preload/onload 비동기).
실측(`/insights/nonexistent` → 404)과 일치.

## 3. 정상 확인 항목 (실측 §6 근거)

| 항목 | 결과 |
|---|---|
| 인덱서블 13경로 (ko) | 전부 200 · `index, follow` · self-canonical · description 1개 · hreflang 존재 |
| `?lang=en` 표본 (`/`, `/guide`) | 200 · en title · canonical 이 `?lang=en` 자기 지시 — 코드(`SeoService.canonicalEn` :94)와 일치 |
| sitemap.xml | 26 loc · 78 xhtml:link — 코드 기대와 일치, 2026-08-02 감사 이후 회귀 없음 |
| robots.txt | 저장소 파일과 일치, `Sitemap:` 라인 존재 |
| `/runners` | `noindex, follow`, hreflang 0 |
| `/stock` | 200 · `noindex, nofollow, noarchive` — bare URL 도 메타로 방어됨 |
| `/trading` | 302 → 어드민 로그인 (미인증 크롤러는 본문 접근 불가) |
| 미존재 ownerId | 404 + `noindex, nofollow` |
| 404 에러 페이지 | `noindex` 존재 |

메시지 카탈로그: `seo.*` 366키 ko/en 완전 쌍, 빈 값 0 — SEO 표면에서 가장 건강한 부분.
기존 회귀 가드(`SitemapServiceHreflangTest` 14 · `SitemapServiceWhitelistTest` 2 ·
`SitemapEndpointTest` 4 · `SeoServiceI18nTest` 15 · `IndexNowServiceTest` 6)도 유효.

## 4. 신규 발견 사항 (이번 점검 — 기존 문서에 없음)

### 中 — 실질 영향 또는 잠재 사고 경로

**4-1. runners 11페이지의 og:image 치수 거짓 선언.** *(→ 2026-08-17 반영 — Commit 149)*
`fragments/head.html:52-53` 이 `og:image:width=1490` / `height=780` 을 **무조건** 출력하는데,
runners·admin 11페이지는 `ogImage` 를 `crew_logo.png` 로 지정하고 실물은 **1280×720** 이다
(파일 실측). 선언 치수를 신뢰하는 스크래퍼(카카오·페이스북)는 크롭/레터박스로 렌더링할 수 있다.
→ 치수를 `SeoMetadata` 필드로 옮기거나 이미지별 조건 출력 필요.

**4-2. 고아 head fragment — 절반 남은 이중화.** *(→ 2026-08-17 반영 — Commit 149)*
`runners/fragments/header.html:4-59` 의 `head(seo)` fragment 는 **소비자 0** (11개 runners
템플릿 전부 `fragments/head :: head` 사용, grep 으로 `:: navbar` 만 참조됨을 확인).
내부에 google-site-verification 토큰 하드코딩(:19 — 공용 head 는 env-driven), `ko-KR`
하드코딩(:15-16), `og:locale=ko_KR` 하드코딩(:37), 자체 치수 1280×720(:34-35) 이 남아 있어,
누군가 이 fragment 를 다시 참조하는 순간 낡은 메타가 살아나는 드리프트 함정이다. → `navbar`
fragment 만 남기고 head 부분 삭제 권고.

**4-3. `head(seo)` 의 `seo` null 무가드 — 신규 페이지 500 함정.** *(→ 2026-08-17 반영 — Commit 150, `HeadSeoSmokeTest`)*
`fragments/head.html:13,16,17,33,36` 등이 `${seo.xxx()}` 를 가드 없이 호출한다. 컨트롤러가
`model.addAttribute("seo", …)` 를 빠뜨리면 SpEL 평가 예외 → 500. 현재 소비 템플릿 27개
전부 설정함을 확인했다(바인딩 에러 재렌더 경로 `RunnerController:233`,
`RunnerAdminController:92/126/171` 포함). 방어 코드 신설이 아니라 **회귀 테스트**(뷰 렌더링
스모크) 또는 `@ControllerAdvice` 기본값 중 택일할 사안 — 실제 발생 전이므로 기록만.

**4-4. 로케일 유실 redirect 4곳.** *(→ 2026-08-17 반영 — Commit 150)*
`RecapController:45` (`redirect:/recap/{year}`), `:64` (공유 후 복귀), `AuthController:23`
(`redirect:/me`), `RunnerController:253` (런 생성 후) 이 `localeLinks.redirect()` 대신 원시
`redirect:` 문자열을 반환 — `?lang=en` 세션이 쿠키 저장 전이면 해당 홉에서 영어가 풀린다.
`HomeController`·`UseCaseController`·`ScheduleController`·`InsightsController` 는 전부
`localeLinks` 를 쓰고 있어 관례 이탈이기도 하다.

**4-5. 에러 페이지 head 자체 구현으로 인한 이중화·기능 공백.** *(→ 2026-08-17 반영 — Commit 151, `ErrorPageRenderingTest`)*
§2-5 참조. noindex 는 있으므로 색인 위험은 없지만, GTM 부재로 에러 유입이 계측되지 않고
폰트 로드 패턴이 본선(preload/onload)과 어긋난다. `head(seo)` 재사용이 자연스러운 수렴점
(BusinessException 뷰 경로는 `MvcExceptionHandler:19-39` 가 모델 없이 뷰만 반환하므로
`seo` 주입 설계가 선행돼야 함 — 4-3 과 같은 사안).
*정정(반영 시점): `head(seo)` 재사용은 기각 — 에러 템플릿은 Spring Boot 컨테이너 에러 경로
(`BasicErrorController` 의 status 계열 뷰 해석)로도 렌더링되며 그 경로엔 `seo` 모델이 없어
무가드 head 는 에러 렌더링 자체를 실패시킨다. 대신 모델 불요의 전용 `fragments/error-head.html`
로 공용화해 GTM(+noscript)·비동기 폰트·파비콘을 본선과 동기화했고, canonical/OG 는 에러 URL 에
무의미해 의도적으로 제외했다.*

### 低 — 무해하거나 발현 조건이 좁음

**4-6. baseUrl 기본값 드리프트.** *(→ 2026-08-17 반영)* `RunnerAdminController:35` 만 `datedate.me`, 나머지 3곳
(`RunnerController:44`, `SeoService:23`, `SitemapService:50`)은 `datedate.site`.
`application.yaml:2` 가 명시 설정이라 실발현 없음 — 통일만 하면 되는 잠복 결함.

**4-7. runners `<html lang="ko">` 하드코딩 vs 공용 head 의 로케일 메타.** runners 11페이지가
`lang="ko"` 고정인데 공용 head 는 `content-language`·`og:locale` 을 요청 로케일로 출력 —
en 방문자에게 `lang="ko"` + `content-language: en-US` 혼합 신호. trading/stock/error 도
`lang="ko"` 고정(이쪽은 head 도 로케일 무관이라 내부 모순은 없음). noindex 라 색인 영향 없음.

**4-8. GTM noscript 부분 커버리지.** `fragments/gtm-noscript` 소비는 datedate 16템플릿뿐.
runners·admin 11페이지는 head 의 GTM JS 로더만 실리고 noscript 폴백이 없다(비JS 유입 미계측).
trading/stock/error 는 둘 다 없음.

**4-9. `schedule/create` 가 `schedule/view` 와 동일 SEO 공유.** `ScheduleController:62` 가
분기(:64-67) 전에 `getScheduleSeo()` 를 넣어, 아직 없는 일정의 생성 페이지가 실재 일정과 같은
title/OG 를 광고. noindex 라 공유 카드 문구 정확성 문제에 그침.

**4-10. noindex 6종에 빈 `<meta name="keywords">` 출력.** *(→ 2026-08-17 반영 — `head.html` keywords 가드, `HeadSeoSmokeTest` 회귀 케이스)* `head.html:17` 무가드 +
`SeoService` 의 `.keywords()` 호출 8곳이 전부 인덱서블 페이지(grep :242,:432,:483,:593,
:658,:713,:750,:819) — dashboard/login/mypage/recap/recapShare/schedule 은 값 없는 메타 출력.

**4-11. runners/admin canonical 6페이지 공통.** `createAdminSeo()` (`RunnerAdminController:50`)
가 전 페이지 `baseUrl + "/runners/admin"` — 5개 페이지에서 부정확하나 noindex 라 무해.

**4-12. 데드코드·잔재.** *(→ ①② 2026-08-17 반영. ③ favicon.ico 의 SVG 서빙은 잔존. `SecurityConfig:53` 의 `/og-image.svg` 화이트리스트 항목은 선재 미커밋 보안 변경(actuator 게이트)과 같은 파일이라 이번에 제거하지 않음 — 무해한 데드 엔트리)* ① `SeoService.getInsightsTrendsSeo()` 무인자 오버로드(:561)는
프로덕션 호출 0, `SeoServiceI18nTest` 4곳만 사용 — javadoc 스스로 레거시 호환용이라 명시.
② `static/og-image.svg` + `StaticResourceController:107-114` 엔드포인트는 어떤 `og:image` 도
참조하지 않음(OG 스크래퍼는 SVG 미지원). ③ `/favicon.ico` 가 SVG 바이트를
`image/svg+xml` 로 서빙(`StaticResourceController:99-105`) — head 의 `type="image/x-icon"`
선언과 불일치, 구형 클라이언트에서 무시될 수 있음.

**4-13. 공유 카드 품질 소항목.** `og-image.png` 586KB(1490×780) — 스크래퍼 타임아웃 여지;
`og:image:alt`·`twitter:site` 부재; trading/stock 은 모듈당 단일 title 이라 브라우저 탭·북마크
구분 불가(색인 무관).

## 5. 기지(旣知) 이슈 — 기존 문서에 이미 기록됨 (재발견, 중복 서술 생략)

| 이슈 | 기록 위치 | 상태 |
|---|---|---|
| bare `/stock` 이 `Disallow: /stock/` 에 미매치, noindex 는 템플릿 하드코딩 | [sitemap 감사 §3-5](sitemap-audit-2026-08-02.md) | 미해결 — 이번 실측으로 메타 방어 동작은 확인 |
| `/use-cases`·`/tools` 허브 404, date-diff 시각 브레드크럼 3단계 vs JSON-LD 2단계 | 동 §7, [ADR common/seo/0008](../adr/common/seo/0008-breadcrumb-item-on-every-listitem.md) | 보류 |
| sitemap lastmod = 배포 시각 | 동 §3-1 | 보류 (옵션 3개 미결정) |
| robots.txt 연도 열거 2035 만료, 알람 없음 | [ADR common/seo/0005](../adr/common/seo/0005-robots-disallow-narrowing.md) | 미해결 |
| `Organization` JSON-LD `founder`/`sameAs` 공백 (E-E-A-T) | [정책 매핑 감사 §3-#6](adsense-low-value-content-policy-mapping.md) | 유일 잔여 권고 |
| `/guides/*` Phase D 미구축, use-case 슬러그 4개 미구현 | [adsense-approval.md](../prompts/adsense-approval.md) Phase C/D | 미착수 |
| CMP/Consent Mode v2 미적용, AdSense lazy-load 미적용 | [Lighthouse 감사](../troubleshooting/lighthouse-performance-audit.md) P2-D/P1-A | 보류 |
| `adsense.client` 공백 → 광고 전면 미출력 | `application.yaml:26` (승인 대기 의도) | 의도적 |
| `INDEXNOW_ENABLED` 운영 상태 미검증, GSC sitemap 리포트 기록 전무 | [data-analysis 04-todo](../data-analysis/04-todo.md) P1-3/4, sitemap 감사 §7 | 미해결 |
| 실서비스 `/trading` robots 실측 미수행 | [remediation §7](../seo/adsense-low-value-content-remediation.md) | **이번 점검으로 해소** (§6: 302 → 어드민 로그인) |
| 문서 간 페이지 수 표기 불일치 (6/11/14/24/26/66/72) | sitemap 감사 §3-5 | 미해결 |

## 6. 실측 로그 (2026-08-16 19:01 KST, 배포본 `https://datedate.site`)

방법: WSL curl, `Accept-Language: ko`, 각 URL 의 상태코드·robots 메타·canonical·description
개수·hreflang 개수·title 추출. hreflang 4건 = head 의 alternate 3종 + 헤더 언어 토글 1건
(ADR common/seo/0004 의 `rel="alternate" hreflang="en"` 토글).

| URL | 상태 | robots | canonical | 비고 |
|---|---|---|---|---|
| `/` `/guide` `/about` `/privacy` `/terms` `/faq` `/tools/date-diff` `/insights/trends` `/use-cases/*`(5종) | 200 | index, follow | self (ko) | desc 1 · hreflang 4 |
| `/?lang=en` `/guide?lang=en` | 200 | index, follow | self (`?lang=en`) | en title 확인 |
| `/runners` | 200 | noindex, follow | `/runners` | hreflang 0 |
| `/stock` | 200 | noindex, nofollow, noarchive | 없음 | bare URL 메타 방어 확인 |
| `/trading` | 302 | — | — | → `/runners/admin/login` |
| `/zz-nonexistent-9999` | 404 | noindex, nofollow | self | 빈 대시보드 렌더 (ADR datedate/domain/0004) |
| `/insights/nonexistent` | 404 | noindex | — | error/4xx 인라인 head 확인 |
| `/sitemap.xml` | 200 | — | — | loc 26 · xhtml:link 78 |
| `/robots.txt` | 200 | — | — | 저장소 파일과 일치 |

## 7. 권고 우선순위 (후속 작업 후보 — 이번 작업 범위 아님)

| 순위 | 항목 | 근거 | 규모 |
|---|---|---|---|
| P1 | ✅ 고아 head fragment 의 head 부분 삭제 (`navbar` 만 유지) — **2026-08-17 반영** | §4-2 드리프트 함정 + 토큰 하드코딩 | 소 |
| P1 | ✅ og:image 치수를 `SeoMetadata` 로 이동 (기본 1490×780, runners 1280×720) — **2026-08-17 반영**, 회귀 가드 `RunnerSeoRenderingTest` | §4-1 | 소 |
| P2 | ✅ 로케일 유실 redirect 4곳 `localeLinks.redirect()` 통일 — **2026-08-17 반영**, 회귀 가드 `LocalePersistenceIntegrationTest` +4 | §4-4 | 소 |
| P2 | ✅ 뷰 렌더링 스모크 테스트로 `seo` 누락 500 가드 — **2026-08-17 반영**, `HeadSeoSmokeTest` 22 라우트 (사보타주 검증 완료) | §4-3 | 중 |
| P3 | ✅ 에러 페이지 head 공용화 — **2026-08-17 반영**, 전용 `error-head` fragment(컨테이너 에러 경로에 seo 모델이 없어 `head(seo)` 대신) + GTM/비동기 폰트, 가드 `ErrorPageRenderingTest` | §4-5 | 중 |
| P3 | ✅ 잔재 정리 — **2026-08-17 반영**: 무인자 오버로드 삭제·og-image.svg 엔드포인트/자산 삭제·keywords 가드·baseUrl 통일. 잔존 2건: favicon.ico SVG 서빙(§4-12③), `SecurityConfig` 의 og-image.svg 데드 화이트리스트 항목(선재 미커밋 보안 변경과 얽혀 보류) | §4-6/10/12 | 소 |
| — | 기지 이슈(§5)는 각 문서의 기존 트래킹을 따름 — 특히 GSC/IndexNow 운영 확인(코드 밖)과 founder/sameAs 채우기 | §5 | — |

> 이 보고서의 신규 발견은 전부 코드 file:line 재확인(에이전트 보고 재검증) + 실측을 거쳤다.
> 수치 근거: `og-image.png` 1490×780 / 600,206 bytes, `crew_logo.png` 1280×720 — `file` 실측.
