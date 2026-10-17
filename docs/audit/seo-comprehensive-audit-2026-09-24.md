# SEO 종합 점검 — 사이트맵 · 페이지별 메타 · 내부 링크 · 구조화 데이터 + AdSense 관점 (2026-09-24)

> 대상: 색인 표면 전체(사이트맵 19엔트리 × ko/en = 38 URL)와 그 주변 경계(robots.txt, 리다이렉트,
> 비색인 경로, 로케일 해석).
> 방법: ① 코드 정적 분석 (`SitemapService`·`SeoService`·`head.html`·헤더/푸터·컨트롤러)
> ② 배포본 `https://datedate.site` 실측 — 38 URL 전수를 크롤해 head 메타·`<a>` 링크 그래프·JSON-LD를
> 파싱했다(측정 시각 2026-09-24 22:24 KST, §9) ③ Google 공식 문서 대조(소프트웨어 앱 구조화 데이터,
> 검색 갤러리 — 2026-09-24 조회) ④ 기존 감사·ADR 과 대조해 **신규 발견과 기지 이슈를 분리**.
> 기준 커밋: `754205e`. 선행: [사이트맵 점검 08-02](sitemap-audit-2026-08-02.md) ·
> [페이지별 점검 08-16](seo-page-audit-2026-08-16.md) ·
> [AdSense 3차 진단 08-17](adsense-low-value-content-diagnosis-2026-08-17.md).
> 성격: 점검 보고 + 사용자가 고른 두 항목(N1·N2) 수정. 나머지는 권고로 남긴다 (§8).
> **2026-09-26 재검증·수정 계획:** 이후 변경(기사 8편 추가·퍼블리셔 신원)을 반영한 상태표는 §11, 남은 항목의 수정 계획은 §12.

---

## 1. 한 줄 결론

**색인 표면은 기술적으로 건강하다.** 38 URL 전부 200, self-canonical, hreflang 상호 참조, `index, follow`,
유효한 JSON-LD, 제목·설명 중복 0건이다. 이번 점검의 핵심 발견은 **링크 그래프**다. `/faq`·`/use-cases`·
`/guides`(ko) 3개 색인 페이지로 들어오는 `<a>` 링크가 사이트 전체에서 0개였다. 사이트맵으로만 발견할 수
있어 사용자와 AdSense 리뷰어의 탐색 경로에서 빠져 있었다. 허브가 생겼는데도 breadcrumb 는 2단계에 머물러
있었다. 두 항목을 이번에 고쳤고(§7), 나머지 8건은 전부 중요도 中 이하의 권고다.

## 2. 범위·방법

| 축 | 내용 |
|---|---|
| 사이트맵 | 라이브 XML 수집 → loc 38 · `xhtml:link` 114 · lastmod 3종 · 헤더 확인 |
| 페이지별 | 38 URL 각각 상태코드 · title · description · robots · canonical · og:url · og:locale · hreflang 3종 · `<html lang>` · h1 수 · JSON-LD 파싱 |
| 링크 그래프 | 38 페이지의 `<a href>` 전량을 절대 URL 로 정규화 → 사이트맵 URL 별 유입 링크 수 · en 페이지의 ko URL 누수 · 사이트맵 밖 내부 링크 대상 |
| 경계 | http/www · 끝 슬래시 · 대소문자 · 미지 슬러그 · 쿼리 변형(`?lang=fr`, `utm_*`) · 비색인 경로 · `Accept-Language` 변형 |
| 구조화 데이터 | 페이지별 `@type` 구성, 소프트웨어 앱 필수 속성·지원 카테고리, FAQPage 문구 ↔ 화면 문구 대조 |

## 3. 정상 확인 항목 (실측 — §9)

| 항목 | 결과 |
|---|---|
| 사이트맵 | 38 loc (19엔트리 × ko/en) 전부 **200**, well-formed, `application/xml`, `max-age=86400`, 18,762 bytes |
| lastmod | 3종 — 정적 28 URL = 빌드 시각(`2026-09-19T15:26`), 기사 8 URL = `GuideSlugs.modified`, insights 2 URL = 최근 활동 |
| canonical · og:url | 38/38 self (ko 는 무쿼리, en 은 `?lang=en`) |
| hreflang | 38/38 ko·en·x-default 3종, 사이트맵 블록과 페이지 head 모두 상호 참조 |
| robots 메타 | 38/38 `index, follow` |
| 언어 신호 | 38/38 `<html lang>` · og:locale 이 URL 언어와 일치 |
| h1 | 38/38 정확히 1개 |
| JSON-LD | 38/38 유효 JSON. BreadcrumbList 전 항목 `item` 보유 (ADR-0008 가드 유효) |
| title · description | 중복 0건 (38개 전부 고유) |
| 리다이렉트 | `http://`·`www.` → apex https **301**, `/tools`·`/insights` → **308** |
| 오류 경로 | `/guides/nope`·`/use-cases/nope` → **404 + noindex** (소프트 404 아님) |
| 쿼리 변형 | `?utm_source=x`·`?lang=fr` → canonical 은 정규 URL |
| robots.txt | 배포본 == 저장소 파일, `Sitemap:` 줄 존재 |
| 캐시 | 색인 페이지 `public, max-age=3600, s-maxage=86400` + `Vary: Cookie, Accept-Language`, `text/html;charset=UTF-8` |
| 검증·외부 | Google 인증 메타 존재, Naver 인증 파일 200, IndexNow 키 파일 200, `ads.txt` 200 `text/plain` |
| AdSense 정합 | 개인정보처리방침에 AdSense 쿠키·제3자 광고·옵트아웃 고지 존재 (ko/en), 광고 스크립트 로드 0 (승인 전 OFF 정책 유지) |
| 비색인 | `/login` noindex,nofollow · `/runners` noindex,follow · `/stock` noindex,nofollow,noarchive · `/recap`·`/me` → 로그인 302 |

## 4. 신규 발견 (기존 문서에 없음)

### 中

**N1. 색인 페이지 3개가 고아 — 내부 유입 링크 0.** *(→ 이번에 수정, §7)*
라이브 38 페이지의 `<a>` 링크 그래프에서 `/faq`·`/use-cases`·`/guides`(ko)로 들어오는 링크가 0개였다.
en 변형은 같은 페이지의 언어 토글에서만 1개씩 들어온다. 헤더 내비(홈·일정 만들기·가이드·인사이트·날짜 계산기)와 푸터
(개별 use-case·기사 슬러그만 나열)에 허브 링크가 없고, 상세 페이지에도 허브로 돌아가는 링크가 없다.
`/faq` 는 `git log -S"/faq" -- templates` 기준 **도입 이후 한 번도 링크된 적이 없다**. 08-17 진단 §5-4가
"홈 FAQ 축소 + `/faq` 링크" 를 권고했지만 실행되지 않았다. 크롤러는 사이트맵으로 찾을 수 있지만 내부 링크
신호가 0이고, 사람(리뷰어 포함)은 도달할 경로가 없다.

**N2. 허브가 생겼는데 breadcrumb 는 2단계.** *(→ 이번에 수정, §7, [ADR-0012](../adr/common/seo/0012-breadcrumb-hub-hierarchy.md))*
ADR-0008 은 허브가 없어서 2단계로 고정했고 "허브를 만들면 3단계 복원" 을 후속 조치로 적어 두었다.
`/use-cases`(09-08)·`/guides`(09-10) 허브가 생긴 뒤에도 활용 사례 5편·기사 4편의 BreadcrumbList 는
`홈 → 페이지` 였고, 두 템플릿 모두 화면 breadcrumb 가 없었다.

**N3. guides 기사 게시일이 실제보다 앞선다.** *(권고)*
`GuideSlugs` 의 기사 4편은 published·modified 가 모두 `2026-08-23`(ADR-0011 결정일)이다. 그런데 슬러그 등록은
`c26567b`(09-10), 템플릿은 09-10·09-11·09-11·09-12 커밋이다. 이 날짜가 사이트맵 lastmod, Article JSON-LD
`datePublished`, 화면 바이라인 3곳에 그대로 나간다. ADR-0011 이 "정직한 날짜 SSOT" 로 설계한 필드가 계획일을
담고 있는 셈이다. 수정은 날짜 4쌍을 실제 첫 배포일로 바꾸는 것이다(배포는 수동이라 커밋일 ≤ 배포일 — Actions
배포 이력으로 확정).

### 低

**N4. 끝 슬래시 변형이 로그인·404 로 떨어진다.** *(권고)* `/guide/`·`/faq/` → **302 `/login`**(카카오 로그인
랜딩), `/guides/`·`/use-cases/` → **404**. Spring 7 은 끝 슬래시를 매칭하지 않고, `SecurityConfig` 의
`/*` permitAll 이 `/guide/` 를 잡지 못해 `authenticated()` 로 떨어진다. 외부에서 슬래시를 붙여 링크하면
링크 가치가 로그인 페이지로 새고 사용자는 로그인 화면을 본다. 해법: `org.springframework.web.filter.UrlHandlerFilter`
(spring-web 7.0.8 에 존재 확인)를 Security 필터보다 앞 순서로 등록해 정규 URL 로 308 리다이렉트.

**N5. 카카오 OAuth 시작 링크가 크롤 가능.** *(권고)* `/oauth2/authorization/kakao` 가 38 페이지에 총 80회
링크돼 있고 robots.txt 가 막지 않는다. 따라가면 `kauth.kakao.com` 으로 302 되고 요청마다 세션·state 가 생긴다.
`Disallow: /oauth2/` 한 줄이면 막을 수 있다(사이트맵 URL 과 충돌 없음 — `SitemapEndpointTest.robotsTxtDoesNotBlockSitemapUrls` 가 가드).

**N6. date-diff en 페이지의 로케일 누수 2곳.** *(권고)* `tools/date-diff.html:10` breadcrumb 홈 `@{/}` 와
`:155` CTA `href="/#start-form"` 이 `localeLinks.href` 를 거치지 않아 en 페이지에서 ko URL 로 간다. 크롤 결과
en 페이지 중 누수는 이 페이지뿐이다.

**N7. 홈 WebApplication 의 `applicationCategory` 값이 Google 미지원.** *(권고)* `SchedulingApplication` 은 Google
소프트웨어 앱 문서의 지원 목록(BusinessApplication, UtilitiesApplication 등)에 없다(date-diff 는 `UtilitiesApplication`
으로 정상). 같은 문서가 `aggregateRating` 또는 `review` 를 **필수**로 규정하므로 홈·date-diff 의 WebApplication 은
원래 소프트웨어 앱 리치 결과 비적격이다. **평점을 지어내 채우는 것은 정책 위반이라 선택지가 아니다.** 카테고리 값만
지원값으로 교정하고 엔티티 설명용으로 유지하면 된다.

**N8. HowTo·FAQ 는 Google 리치 결과 대상이 아니다.** *(기록)* 2026-09-24 조회한 Google 검색 갤러리에 HowTo·FAQ 가
없다. `/guide`(HowTo)·`/faq`(FAQPage)·use-case(HowTo+FAQPage) 마크업은 무해하고 다른 검색엔진·엔티티 이해에
쓰일 수 있어 유지해도 되지만, ADR-0007 의 "HowTo 스키마로 리치 결과 가능" 서술은 더 이상 사실이 아니다. 이 마크업에
리치 결과를 기대하는 작업은 하지 않는다.

**N9. `/faq` FAQPage 문구가 화면과 다르다.** *(권고)* JSON-LD 는 `seo.faq.q1~q6`(6문항)이고 화면은 `faq.q1~q8`
(8문항)이다. 질문은 전부 화면에 있지만 **답변은 ko 4건·en 1건이 의역**이다(예: ko a2 는 화면 문장의 뒷부분 생략).
Google 구조화 데이터 지침은 마크업 내용이 화면에 보일 것을 요구한다. `faq.*` 키를 직접 쓰도록 `buildFaqMainEntity`
를 돌리고 중복 키 `seo.faq.*` 를 정리하면 하나로 수렴한다.

**N10. 제목 길이·형식.** *(기록)* en title 이 60자를 넘는 곳이 4곳이다(홈 65, insights 78, 기사 how-to-pick-a-date 79·
scheduling-etiquette 78). insights 만 `X | DateDate - …` 로 브랜드가 문구 중간에 온다. Google 은 제목을 자주
재작성하므로 결함은 아니다. ko description 은 120~148자인데, 테스트(`indexablePages_descriptionLengthInRange`)가
120~160자를 의도적으로 고정하고 있어 기록만 한다.

## 5. 기지(旣知) 이슈 — 현재 상태

| 이슈 | 기록 위치 | 2026-09-24 상태 |
|---|---|---|
| 로케일 적응: ko URL 에 `Accept-Language: en` → 영어 본문 + `?lang=en` canonical | [08-17 §8](adsense-low-value-content-diagnosis-2026-08-17.md) "의도된 설계" | **재실측·유지 (사용자 결정: 기록·관찰).** 보강: ko URL 이 hreflang `ko` 이자 `x-default` 라 영어 헤더를 보내는 크롤러에는 "ko URL 의 canonical = en URL" 로 보인다. Googlebot 은 보통 Accept-Language 없이 크롤해 영향이 낮다. 관찰 절차는 §10 |
| 정적 페이지 lastmod = 빌드 시각 | [08-02 §3-1](sitemap-audit-2026-08-02.md) | 보류 유지 (현재 값 `2026-09-19T15:26`) |
| bare `/stock` 이 `Disallow: /stock/` 미매치, noindex 는 템플릿 하드코딩 | 08-02 §3-5 | 미해결 (라이브 메타 noindex 동작 확인) |
| robots 연도 열거 2035 만료 | ADR-0005 | 미해결 |
| og-image 600KB · `og:image:alt`·`twitter:site` 부재 | [08-16 §4-13](seo-page-audit-2026-08-16.md) | 미해결 (600,206 bytes 재실측) |
| `/favicon.ico` 가 SVG 를 서빙 | 08-16 §4-12③ | 잔존 |
| `SecurityConfig` 의 og-image.svg 데드 화이트리스트 | 08-16 §7 | **해소 확인** (`SecurityConfig.java:53` 에 없음) |
| runners `lang="ko"` 하드코딩 · GTM noscript 부재 | 08-16 §4-7·4-8 | 잔존 (noindex 라 색인 무영향) |
| `schedule/create` 가 view 와 같은 SEO | 08-16 §4-9 | 잔존 (`ScheduleController:67`) |
| `/privacy-policy` 데드 매처 | 08-17 §8 | 잔존 — 라이브 404 + noindex, 문서화만 |
| date-diff 화면 breadcrumb 3단계("도구") vs JSON-LD 2단계 | ADR-0008 | 잔존 — `/tools` 가 308 이라 허브 아님, ADR-0012 에서도 제외 |
| IndexNow 운영 플래그 · GSC 사이트맵 리포트 기록 | 08-02 §7, 08-16 §5 | 코드 밖 — 미확인 (키 파일 200 만 확인) |
| 문서 간 수치 드리프트 | 08-02 §3-5 | 계속 — 예: `common/CLAUDE.md` 의 security ADR 수 "5개"(실제 6). SEO 수는 이번에 12 로 정정 |

## 6. AdSense 관점 검토 (사용자 요청)

### 6-1. 현재 위치

- 08-17 3차 통지로 "승인됨 + 주의 필요(낮은 가치 콘텐츠)" 상태다. 진단 결론은 **editorial(콘텐츠 믹스)** 이었고,
  그 해법인 `/guides` 기사 4편은 라이브에 있다(4편 × ko/en 모두 200·index·Article JSON-LD 확인).
- 색인 표면 19페이지의 구성은 **에디토리얼 9**(use-case 5 + 기사 4), **허브 2**, **서비스·정책·도구 8**
  (홈·가이드·소개·FAQ·개인정보·약관·날짜 계산기·인사이트)이다. 08-17 시점의 13페이지 중 11페이지가 자기서술이던
  구성에서 크게 이동했다.
- 광고는 승인 전까지 OFF 정책대로 스크립트 로드 0이다(홈·기사 실측).

### 6-2. 이번 조치의 의미

AdSense 가 보는 **사이트 탐색성** 쪽 결함을 메운다. editorial 판정 자체를 뒤집는 동인은 아니다 — 08-17 결론은
그대로 유효하다. 다만 이미 만든 콘텐츠가 리뷰어 눈에 **발견되는지** 는 별개 문제였고, 여기에 구멍이 있었다.

- 에디토리얼 허브(모임 노하우)가 이제 **1차 내비(헤더)** 에 있다. 이전에는 푸터 슬러그 링크로만 기사에 닿았고
  허브에는 닿을 수 없었다.
- 모든 활용 사례·기사에서 **breadcrumb 로 허브에 돌아간다** — 콘텐츠가 계층으로 묶여 보인다.
- `/faq` 가 모든 색인 페이지 푸터에서 링크된다.

### 6-3. 다음 권고 (기대 효과 순)

1. **재검토 요청 절차를 지킨다** (08-17 §6). GSC 에서 기사 4편이 "색인 생성됨" 인지 확인 → **이 변경을 배포** →
   정책 센터에서 요청한다. **이미 심사 중이면 판정 후에 배포한다** (§6-3: 심사 중 색인 페이지 변경 금지).
2. **N3 게시일 정정** — 신뢰 신호. 날짜 4쌍 변경 + `GuideSlugsTest` 로 끝나는 소규모 작업.
3. **자기서술 중복 축소** (08-17 §5-3·5-4 미실행분) — 홈 FAQ 6문항을 상위 3문항 + `/faq` 링크로 줄이고,
   `/faq` 는 신규 주제(데이터 보존·투표 공개 범위·리캡 공유 등)로만 확장한다. N9 문구 일치도 함께.
4. **기사 추가가 use-case 증식보다 낫다** — 같은 템플릿에서 찍어내는 페이지는 "복제 템플릿" 신호를 키운다(08-17 §3-1).
5. N4~N7 기술 정리는 AdSense 영향이 미미하다 — 여유가 있을 때.

### 6-4. 하지 말 것

- 소프트웨어 앱 리치 결과를 노린 `aggregateRating`·`review` 추가(N7) — 실제 리뷰 없는 평점은 정책 위반.
- 실체 없는 저자 프로필·`sameAs` 계정(ADR-0011, 08-17 §5-6 재확인).
- 문항 수를 채우기 위한 기계적 FAQ 확장, use-case 슬러그 증식.

### 6-5. 광고 재개 전 필수 (승인 이후)

ad-slot 예약 높이 재설계(ADR-0010 체크리스트), EEA·영국·스위스 트래픽 대상 Google 인증 CMP
([Lighthouse 감사](../troubleshooting/lighthouse-performance-audit.md) P2-D 기지 항목),
`ads.txt` 대시보드 상태 재확인(Commit 176 후속).

## 7. 이번에 바꾼 것

| 파일 | 변경 |
|---|---|
| `datedate/application/service/SeoService.java` | `breadcrumbJsonLd` 를 "홈 + 크럼 목록" 루프로 일반화. use-case·기사는 허브 크럼 포함 3단계 (전 항목 `item` 불변식은 한 곳에 유지) |
| `templates/use-cases/detail.html`, `templates/guides/<slug>.html` ×4 | 가시 breadcrumb `홈 / 허브 / 현재` (`faq.html` 마크업·기존 CSS, 기존 메시지 키만 사용) |
| `templates/fragments/header.html` | `header`·`header-minimal` 내비에 "모임 노하우"(`/guides`) — 신규 키 `nav.guides`(ko "모임 노하우", en **"Tips"**) |
| `messages.properties`, `messages_en.properties` | `nav.guides` 1줄씩 (en 라벨을 짧게 — 아래 시각 확인) |
| `templates/fragments/footer.html` | `footer-minimal` 도움말 섹션에 `/faq` — 기존 키 `seo.breadcrumb.faq` |
| `docs/adr/common/seo/0012-…md` (신설), `0008` 상태, `docs/adr/README.md` | 결정 변경 기록 (0008 의 2단계 고정 부분 대체) |
| `datedate/application/CLAUDE.md`, `common/CLAUDE.md` | breadcrumb 사실 · SEO ADR 수 동기화 |
| 테스트 | 신설 `InternalLinkingTest` 2, `SeoServiceI18nTest` +1, `UseCaseLocaleRenderingTest` +1, `GuidesLocaleRenderingTest` +4(파라미터) |

**계획 대비 조정:** 계획에서는 푸터에 허브 링크 2개를 새 메시지 키로 넣으려 했다. 그런데 작업 중에
`messages*.properties`·`style.css` 가 병행 세션의 미커밋 커밋 177(시간 투표) 소유라는 것을 확인했다.
그래서 푸터는 **기존 키만** 쓰는 쪽으로 바꿨다. `/use-cases` 는 5개 상세의 breadcrumb,
`/guides` 는 헤더 내비와 4개 기사의 breadcrumb 에서 링크되고, 고아 0 목표는 `InternalLinkingTest` 로 확인했다.
헤더만은 아래 시각 확인 결과에 따라 사용자 결정으로 전용 키 `nav.guides` 를 추가했다
(키 2줄은 커밋 177 에 흡수 — 커밋 로그 주의 주석).

**헤더 시각 확인:** 앱을 기동하지 않고 확인했다(`.env` 가 LIVE 봇을 켜므로). 라이브 HTML 에 새 링크를 끼우고
로컬 `style.css` 를 붙여 Windows Chrome 헤드리스로 before/after 를 찍었다.

| 폭 | KO | EN "Tips & Guides" (1차안) | EN "Tips" (채택) |
|---|---|---|---|
| ≥1280 | 한 줄 | 한 줄 | 한 줄 |
| 1024 | 한 줄 (before 와 동일) | 4개 항목 두 줄 (before 한 줄) | 2개 항목 두 줄 |
| 약 790~850 | 정상 | KO/EN 토글 화면 밖 | KO/EN 토글 잘림 |
| 769~790 | 정상 | 토글 화면 밖 | 토글 잘림 — **before 도 이미 잘림(기존 문제)** |

EN 라벨을 짧게 해서 회귀 구간이 크게 줄었지만 0 은 아니다. 근본 원인은 EN 데스크톱 헤더가 769~790px 에서
이미 넘치던 여유 없는 레이아웃(항목 `nowrap` 부재, 햄버거 전환 768px)이다 → §8 에 CSS 후속 과제로 올린다.

**회귀 가드 RED → GREEN:**

- `InternalLinkingTest.everySitemapUrlHasInboundLinkFromAnotherSitemapPage` 는 수정 전
  `[/faq, /use-cases, /guides]` 3건으로 실패했다 — 라이브 크롤과 같은 집합.
  - 첫 작성에서는 `<head>` 의 canonical·hreflang `<link href>` 를 링크로 세는 바람에 통과해 버렸다.
    `<a>` 한정으로 고친 뒤 RED 를 재확인했다.
- 3단계 JSON-LD 테스트는 `expected: 3 but was: 2` 로 실패했다.
- 가시 breadcrumb 테스트 5종은 nav 부재로 실패했다.
- 헤더 테스트는 `/guides` 부재로 실패했다.

## 8. 권고 우선순위 (이번 범위 밖) — 09-26 이후 §12 수정 계획으로 대체

| 순위 | 항목 | 근거 | 규모 |
|---|---|---|---|
| P1 | 배포 타이밍 — AdSense 심사 중이면 판정 후, 아니면 배포 후 재검토 요청 | §6-3-1 | 운영 |
| P1 | N3 기사 게시일을 실제 배포일로 | 신뢰 신호, ADR-0011 의도 | 소 |
| P2 | 홈 FAQ 축소 + `/faq` 신규 주제 확장 + N9 문구 일치 | §6-3-3 | 중 |
| P2 | N4 끝 슬래시 308 (`UrlHandlerFilter`, Security 앞 순서) | 링크 가치 누수·UX | 소 |
| P2 | EN 데스크톱 헤더 좁은 폭 넘침 — 769~850px 에서 KO/EN 토글 잘림 (769~790 은 이번 변경 전부터) | §7 시각 확인 | 소 (`style.css` — 병행 세션 커밋 177 이후) |
| P3 | N5 `Disallow: /oauth2/` · N6 date-diff 링크 2곳 · N7 카테고리 값 | 위생 | 소 |
| — | 로케일 적응 관찰 (§10) | §5 | 운영 |

## 9. 실측 로그 (2026-09-24 22:24 KST, 배포본)

- **사이트맵:** loc 38 · xhtml:link 114 · lastmod `2026-09-19T15:26:33.495+09:00`×28 /
  `2026-08-23T00:00:00+09:00`×8 / `2026-09-24T10:15:12.874642+09:00`×2.
- **38 URL 크롤 (Accept-Language 없음 = Googlebot 기본):** 전부 200. canonical·og:url self,
  hreflang 3종, robots `index, follow`, `<html lang>`·og:locale 일치, h1=1, JSON-LD 유효 — 38/38.
- **유입 `<a>` 링크 (다른 사이트맵 페이지 기준):** `/faq` 0 · `/use-cases` 0 · `/guides` 0 (en 변형은 각 1 —
  언어 토글). 나머지는 17~19.
- **사이트맵 밖 내부 링크 대상:** `/oauth2/authorization/kakao` 80, `?lang=ko` 변형 각 2(토글, `rel=nofollow`).
- **경계:**

  | URL | 결과 |
  |---|---|
  | `http://`, `https://www.` | 301 → apex |
  | `/guide/`, `/faq/` | 302 → `/login` |
  | `/guides/`, `/use-cases/`, `/index.html`, `/GUIDE` | 404 |
  | `/privacy-policy` | 404 noindex |
  | `/tools`, `/insights` | 308 |
  | `/guide?lang=ko`·`?lang=fr`·`?utm_source=x` | 200, canonical `/guide` |

- **`Accept-Language` 변형 (`/guide`, `/guides/how-to-pick-a-date`):** 없음 → `lang="ko"` + ko canonical.
  `en-US` 와 `ja-JP,…,en` → `lang="en"` + `?lang=en` canonical. 모두 `Vary: Cookie, Accept-Language`.

재현:

```bash
curl -sS https://datedate.site/sitemap.xml | grep -o '<loc>[^<]*' | sed 's/<loc>//;s/&amp;/\&/g' > locs.txt
while read u; do printf "%s %s\n" "$(curl -sS -o /dev/null -w '%{http_code}' "$u")" "$u"; done < locs.txt
# 유입 링크: 각 페이지의 <a href> 만 추출해 절대 URL 로 정규화 후 집계 (head 의 <link> 는 제외)
for u in $(cat locs.txt); do curl -sS "$u" | grep -o '<a [^>]*href="[^"]*"' | grep -o 'href="[^"]*"'; done | sort | uniq -c | sort -rn
curl -sS -H 'Accept-Language: en-US' https://datedate.site/guide | grep -o '<link rel="canonical"[^>]*>'
curl -sS -o /dev/null -w '%{http_code} %{redirect_url}\n' https://datedate.site/guide/
```

저장소 가드 (정확한 FQCN — `--tests` 와일드카드는 WSL→cmd.exe 경유 시 불가):

```bash
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests me.singingsandhill.calendar.common.presentation.InternalLinkingTest --tests me.singingsandhill.calendar.datedate.application.service.SeoServiceI18nTest --tests me.singingsandhill.calendar.datedate.presentation.controller.UseCaseLocaleRenderingTest --tests me.singingsandhill.calendar.datedate.presentation.controller.GuidesLocaleRenderingTest"
```

## 10. 로케일 적응 관찰 절차 (사용자 결정: 코드 변경 없이 관찰)

1. GSC → URL 검사 → `https://datedate.site/guide`(및 기사 1편) → "Google 에서 선택한 표준 URL" 이 **자기 자신**인지.
   `?lang=en` 으로 잡혀 있으면 ko URL 이 en 에 흡수되고 있다는 뜻이다 → 이 항목을 재상정한다.
2. GSC → 페이지 색인 → "중복, Google 에서 사용자와 다른 표준을 선택함" 목록에 무쿼리 ko URL 이 있는지.
3. 재상정 시 대안: (a) 쿠키가 없으면 Accept-Language 를 무시해 ko URL 을 항상 한국어로 (ADR i18n/0001 결정 변경),
   (b) 첫 방문 en 사용자를 `?lang=en` 으로 302. 각각의 UX 트레이드오프는 이번 점검의 결정 질문에 기록.

---

## 11. 재검증 (2026-09-26) — 이후 변경 반영

> 계기: 09-24 점검 뒤 **퍼블리셔 신원(연락처 이메일·`sameAs`) + guides 기사 8편 추가(4 → 12편)** 가
> 작업 트리에 들어왔다. 이 보고서의 판정이 아직 유효한지 다시 확인했다.
> 방법: ① 라이브 재확인(2026-09-26 13:26 KST) ② 작업 트리 코드·문서 대조 ③ 전체 스위트
> ④ 기사 12편의 템플릿 골격·본문 유사도 정량 비교.

### 11-1. 라이브 — 변경 없음 (미배포)

사이트맵은 여전히 38 loc 이고 정적 lastmod 는 `2026-09-19T15:26`(마지막 배포 빌드)이다.
새 기사(`/guides/timezone-coordination`)는 **404**, 기사 화면 breadcrumb 0, 헤더 `/guides` 링크 0.
따라서 **§3·§9 의 라이브 실측은 그대로 유효**하고, 이 보고서의 코드 수정(§7)과 신규 변경은 모두 배포 대기 상태다.

### 11-2. 코드 — 테스트

전체 스위트 **123 클래스 · 785 테스트 · failures 0** (09-24 761 → 기사 8편 × 파라미터 테스트 3종 = +24).
`InternalLinkingTest`(고아 0)·`GuidesLocaleRenderingTest` 41·`SeoServiceI18nTest` 18·사이트맵 테스트 전부 GREEN.

변경 요약의 주장은 코드와 모두 일치했다.
- 이메일: 푸터 2곳, `/about`, Organization JSON-LD
- `sameAs`: 실존하는 저장소 소유 계정 (HTTP 200)
- 기사: 12편
- 플레이스홀더: 0건
- 요약(meta description): ko 121~141자, en 150~155자
- 사이트맵: 26페이지 × 6 = hreflang 156 (insights 제외 기준)

### 11-3. 기존 발견의 현재 상태

| 항목 | 09-26 상태 |
|---|---|
| N1 허브·FAQ 고아 | **코드 해결 유지** — 12편 기준 `InternalLinkingTest` GREEN. 라이브는 배포 전이라 여전히 고아 |
| N2 breadcrumb 2단계 | **코드 해결 유지** — 새 8편도 화면 breadcrumb + JSON-LD 3단계 (새 템플릿에 직접 포함됨) |
| N3 게시일이 실제보다 앞섬 | **재발** — 새 8편도 `2026-09-24` 로 등록됐는데 라이브 404(배포 전). 기존 4편의 `08-23` 도 그대로 |
| N4 끝 슬래시 → `/login`·404 | 유효 (`SecurityConfig` 무변경) |
| N5 `/oauth2/` 크롤 가능 | 유효 (`robots.txt` 무변경) |
| N6 date-diff en 누수 2곳 | 유효 (템플릿 무변경) |
| N7 `SchedulingApplication` | 유효 (`SeoService.java:180`) |
| N8 HowTo·FAQ 리치 결과 비대상 | 유효 (기록) |
| N9 FAQPage 답변 의역 | 유효 (`seo.faq.*` 무변경) |
| N10 제목 길이 | **악화** — 새 8편 en 제목 90~100자, ko 49~56자(브랜드 접미 포함). 60자 초과 en 제목 4건 → 12건 |
| 로케일 적응 | 유효 (관찰 결정 유지) |
| EN 헤더 좁은 폭 넘침 | 유효 (`style.css` 변경분은 `.footer-contact` 뿐) |

### 11-4. 신규 확인 사항 (M)

**M1. 새 8편의 템플릿 골격이 사실상 하나 — 中~低.**
태그·키 순서열을 비교한 구조 골격 유사도는 다음과 같다.

| 비교 쌍 | 평균 | 최소 |
|---|---|---|
| 새 8편 × 새 8편 | **0.96** | 0.93 (1.00 인 쌍 3개) |
| 기존 4편 × 기존 4편 | 0.69 | 0.49 |

새 8편은 표형 4편 + 목록형 4편의 두 변형으로 수렴한다. ADR-0011 은 use-case 5페이지가 받은 "한 템플릿 복제" 신호를
피하려고 기사별 전용 템플릿(골격 다양성)을 골랐는데, 새 8편은 파일만 나뉘었을 뿐 골격은 같다.

**본문은 고유하다** — 편 사이 8-gram(ko)/5-gram(en) Jaccard ≈ 0, 3편 이상에 똑같이 나오는 25자 이상 문장 0건.
블로그 기사가 같은 레이아웃을 쓰는 것 자체는 정상이라 치명적이지 않다. 다만 ADR-0011 의 전제와 어긋난다.

**M2. `sameAs`·연락처 이메일 = 기존 "보류" 결정의 변경 — 결정 기록 필요 → [ADR-0013](../adr/common/seo/0013-publisher-identity-contact-and-sameas.md) 으로 기록.**
실존 계정이라 허위 신원 문제는 없다(ADR-0011 결정 6 준수). 남은 확인 사항은 셋이다.
- `sameAs` 는 화면에 없어서 사람 리뷰어에게는 효과가 없다.
- 이메일 로컬파트(`cheongyakplanet`)가 DateDate 와 다른 서비스명으로 읽힌다 — 의도인지 확인 필요.
- 같은 주소가 6곳(`SeoService` 3, `about.html` 1, `footer.html` 2)에 하드코딩돼 있다.

**M3. ADR-0011·0012 의 "기사 10편 초과 시 fragment 추출 재검토" 조건 충족.**
breadcrumb·바이라인·CTA·관련 기사 목록이 12개 템플릿에 반복된다.

**M4. 문서 사실 드리프트 → 이번에 정정.**
- 루트 `CLAUDE.md` 의 "기사 4편 / ko ≈3,900자·en ≈1,200단어 / 템플릿 4개" → 12편과 테스트 하한 표기로 바꿨다.
- `common/CLAUDE.md` 의 ADR 수(security 5 → 6, SEO → 13, infrastructure 추가)를 맞췄다.
- `datedate/application/CLAUDE.md` 에 Organization `email`·`sameAs` 사실을 추가했다.

**M5. 커밋 로그 순서 충돌 → 이번에 정정.**
새 작업에 커밋 로그 섹션이 없었다. 그래서 같은 파일을 쓰는 SEO 커밋이 새 변경을 떠안았다.
- 옛 180(`footer.html`)은 `footer.contact` 키와 `.footer-contact` CSS 를 참조한다.
  둘 다 어느 커밋에도 없어서, 그대로 커밋하면 `??footer.contact??` 가 노출되는 중간 상태가 된다.
- 조치: 새 작업을 Commit 180 으로 끼워 넣고, SEO 커밋을 181·182 로 밀었다.
- 흡수 관계는 각 커밋의 주의 주석에 남겼다(`docs/guides/git-commit.md`).

### 11-5. AdSense 관점 갱신

- **콘텐츠 믹스는 크게 좋아졌다** — 코드 기준 색인 26페이지 중 에디토리얼 17(use-case 5 + 기사 12).
- **재검토 요청 순서가 바뀐다:**
  1. ~~N3 날짜를 실제 배포일로 맞춘다.~~ — 사용자 결정으로 제외(§12 R1). 게시일은 현행 SSOT 값 유지.
  2. 배포한다.
  3. GSC 에서 **새 기사 8편이 "색인 생성됨"** 인지 확인한다(약 7일).
  4. 재검토를 요청한다. 심사 중이면 판정 뒤에 배포한다(08-17 §6-3).
- **M1 은 리뷰어 관점에서 "같은 틀의 글 8편이 한 날짜에 등장" 으로 읽힐 수 있다.** 본문이 고유하므로 급하게 고칠
  대상은 아니지만, 이후 기사는 골격을 다양화하는 편이 ADR-0011 취지에 맞다(§12 R10).

---

## 12. 수정 계획 (2026-09-26 수립)

> **진행 상태 (2026-09-26 갱신):** R1 은 사용자 결정으로 **계획에서 제외**.
> R2 는 권장안(기사 `metaTitle` 분리), R6 은 권장값(`UtilitiesApplication`)으로 확정했다.
> **R2~R6 구현 완료** — 결과는 §12-1. Phase 2(R7~R11)는 미착수.

N2 는 코드에서 이미 해결됐다(배포 후 검증만 남음, R12). 나머지를 배포 기준으로 묶는다.
각 항목은 RED 선확인 → GREEN 으로 진행하고, 결정이 필요한 곳은 **[결정]** 으로 표시했다.

### Phase 0 — 다음 배포 전에 필수

| # | 대상 | 변경 | 검증 | 규모 |
|---|---|---|---|---|
| ~~R1~~ | ~~N3 게시일~~ | **제외 (사용자 결정, 2026-09-26)** — N3 는 기록으로만 남는다 | — | — |
| R2 | N10 제목 길이 | **[결정]** 제목 처리 (아래 참고) | 신규 `SeoServiceI18nTest.indexablePages_titleWithinSerpBudget` — 색인 전 페이지 ko/en `<title>` 길이 상한(제안 en ≤ 60자, ko ≤ 35자, 브랜드 접미 포함). RED 로 초과 목록 확인 후 GREEN | 중 |

R1(제외) 참고 — 제외 전 기준안: 새 8편 = 배포 당일, 기존 4편 = 첫 배포일(없으면 커밋일 하한).

R2 선택지:
- **(권장)** 기사별 선택 키 `guides.article.<slug>.metaTitle` 을 두고, 있으면 `<title>`·og:title 에 쓴다.
  h1·JSON-LD headline 은 긴 제목을 유지한다.
  제안 상한 기준 대상은 26페이지 중 ko 12건(insights + 기사 11편), en 12건(홈 65·insights 78 + 기사 10편 78~100)이다.
  insights 는 키 문구만 고친다(브랜드가 중간에 오는 형식 정리).
- **(대안)** `title` 자체를 줄인다 — 새 키가 없지만 h1·허브 카드·breadcrumb 이름도 함께 짧아진다.

### Phase 1 — 같은 배포에 실어도 되는 작은 수정

| # | 대상 | 변경 | 검증 | 규모 |
|---|---|---|---|---|
| R3 | N6 date-diff 누수 | `tools/date-diff.html:10`·`:155` 를 `localeLinks.href` 로 | 신규 `InternalLinkingTest.englishPagesLinkEnglishUrls` — en 사이트맵 페이지의 내부 `<a>` 가 모두 `lang=en` 을 달고 있는지. 예외는 `/oauth2/**`, `mailto:`, `?lang=ko` 토글. RED = date-diff 2건 | 소 |
| R4 | N5 OAuth 크롤 | `robots.txt` 에 `Disallow: /oauth2/` (파일 CRLF 유지) | `SitemapEndpointTest.robotsTxtDoesNotBlockSitemapUrls` GREEN 유지 + 같은 판정기로 `/oauth2/authorization/kakao` 차단 단정 | 소 |
| R5 | N9 FAQ 문구 | `getFaqSeo` 가 화면 키 `faq.q1~q8/a1~a8` 을 쓰도록 `buildFaqMainEntity(8, "faq")`. 중복 키 `seo.faq.q1~q6/a1~a6` 은 ko/en 에서 삭제(다른 사용처 없음 확인) | 신규 `faqSeo_jsonLdMatchesVisibleFaq` — 8문항, 문구가 `faq.*` 와 일치. `MessageCatalogParityTest` | 소 |
| R6 | N7 앱 카테고리 | 홈 `applicationCategory` 를 Google 지원값으로 **[결정 — 제안 `UtilitiesApplication`]**. date-diff·manifest `utilities` 와 일관, 대안은 `BusinessApplication`. 평점은 넣지 않는다 | 신규 단정: 모든 WebApplication JSON-LD 의 카테고리가 지원 목록에 포함 | 소 |

### Phase 2 — 별도 작업 (UX·구조 결정 필요)

| # | 대상 | 변경 | 검증 | ADR | 규모 |
|---|---|---|---|---|---|
| R7 | N4 끝 슬래시 | `UrlHandlerFilter.trailingSlashHandler(...)` 를 308 로 설정하고 `FilterRegistrationBean` 으로 **Spring Security 보다 앞 순서**에 등록한다. 적용 범위는 공개 페이지 경로, `/api/**` 는 제외 | MockMvc: `/guide/`→308 `/guide`, `/guides/`→308 `/guides`, `/guide/?lang=en` 쿼리 보존, `/` 무영향, `POST /api/**` 무영향 | 신규 (URL 정규화 정책) | 중 |
| R8 | EN 헤더 넘침 | `style.css` 헤더 내비: 항목 `white-space: nowrap` + 769~1100px 간격 축소, 또는 햄버거 전환점 상향 **[결정]** | §7 과 같은 하네스(라이브 HTML + 로컬 CSS, Windows Chrome 헤드리스)로 KO/EN × 780·820·860·1024·1280px before/after + 375px CDP | 불필요 | 소~중 |
| R9 | M3 공통 크롬 | 기사 12편의 breadcrumb·바이라인·CTA·관련 기사 목록을 `fragments/guide-article.html` 로 추출. 본문은 기사별 템플릿 유지 | 동작 불변: 12편 × ko/en MockMvc 렌더 HTML 을 전후 바이트 비교 + `GuidesLocaleRenderingTest` 41 GREEN | 0011 갱신 (결정 1 부분 변경) | 중 |
| R10 | M1 골격 다양성 | (콘텐츠) 새 8편 중 일부에 기사 고유 블록을 넣는다(사례 대화, 결정 트리, 타임라인 등) **[결정 — 선택 사항]** | `GuidesLocaleRenderingTest` 분량·마커 유지, 골격 유사도 재측정 | — | 중 |
| R11 | M2 연락처 | ① 이메일 주소가 의도한 것인지 확인 **[결정]** ② 6곳 하드코딩을 한 곳으로 모을지 **[결정]** — `application.yaml` 단일 값을 `SeoService`(`@Value`)와 템플릿(ModelAdvice)에서 쓰는 방식. 필요성 판단 뒤 진행 | 렌더·JSON-LD 가 설정값과 일치 | — | 소 |

### Phase 3 — 배포 후 운영 (코드 없음)

| # | 내용 |
|---|---|
| R12 | 배포 직후 라이브 재실측: 고아 0, 새 URL 200·index, BreadcrumbList 3단계 → Rich Results Test (use-case 1편·기사 1편), GSC 사이트맵 재제출 |
| R13 | GSC 에서 새 기사 색인 확인(~7일) → AdSense 재검토 요청 (§11-5 순서) |
| R14 | 로케일 적응 관찰 (§10) |

### 권장 진행 순서

1. ~~R1·R2 를 결정한다.~~ — R1 제외, R2·R6 권장안 확정 (09-26).
2. Phase 0 + Phase 1(R2~R6)을 **한 배포**로 묶는다 — 구현 완료, 커밋 로그 Commit 178~184.
3. R12 → R13 을 진행한다.
4. Phase 2 는 AdSense 판정과 무관하게 순차 진행한다. 심사 중이면 판정 뒤에 배포한다.

### 12-1. 구현 결과 (2026-09-26) — R2~R6

전부 RED 선확인 → GREEN.

| # | RED (수정 전 실패 내용) | 변경 | 가드 |
|---|---|---|---|
| R2 | 예산 초과 24건 (ko 12 / en 12) | `SeoService.getGuideArticleSeo` 가 `metaTitle` 우선(없으면 `title`), 기사 12편 × ko/en `metaTitle` 추가, insights ko/en·홈 en 제목 단축. h1·headline·breadcrumb 는 긴 제목 유지 | 신규 `SerpTitleBudgetTest` — 결과 ko 27~35자 / en 45~60자. [ADR-0014](../adr/common/seo/0014-serp-title-budget-and-meta-title.md) |
| R3 | `date-diff?lang=en` → `/` 누수 (breadcrumb 홈 + CTA 가 같은 URL 로 정규화돼 1건) | `tools/date-diff.html` 두 링크를 `localeLinks.href` 로 | `InternalLinkingTest.englishPagesLinkEnglishUrls` — en 사이트맵 전 페이지. 예외: `?lang=ko` 토글, `/oauth2/` |
| R4 | `/oauth2/authorization/kakao` 미차단 | `robots.txt` 에 `Disallow: /oauth2/` (CRLF 유지) | `SitemapEndpointTest.robotsTxtBlocksOAuthStart` + 기존 "사이트맵 URL 비차단" 유지 |
| R5 | FAQPage 6문항 (8 기대) | `getFaqSeo` 가 화면 키 `faq.q1~q8/a1~a8` 로 빌드, 중복 키 `seo.faq.q1~q6/a1~a6` 을 ko/en 에서 삭제 | `SeoServiceI18nTest.faqSeo_jsonLdMatchesVisibleFaq` — 8문항·문구 일치 |
| R6 | 홈 `SchedulingApplication` 미지원 | `UtilitiesApplication` | `SeoServiceI18nTest.webApplicationJsonLd_usesSupportedCategory` (Google 지원 목록) |

**검증**
- 대상 7클래스 97테스트 GREEN.
- 전체 스위트 결과는 커밋 로그 섹션에 기록.
- 라이브는 미배포 — R12(배포 후 실측)에서 확인한다.

**M6 (작업 중 신규 발견) — 메시지 파일 줄바꿈 혼재.** `messages.properties`·`messages_en.properties` 는 HEAD 에서
LF 전용인데, 기사 8편 블록(각 428줄)이 CRLF 로 들어와 한 파일에 섞였다.
- `.gitattributes` 에 `.properties` 규칙이 없어 이대로 커밋된다.
- `java.util.Properties` 는 CRLF 를 줄 끝으로 처리하므로 **동작 영향은 없다**.
- 다만 저장소는 같은 유형 사고(`*.md`, `ads.txt`)를 LF 고정 규칙으로 막아 왔다.
- 이번에는 줄마다 이웃 줄의 줄바꿈을 따랐고 정규화하지 않았다. `metaTitle` 8줄이 CRLF 영역에 들어가 CRLF 가 각 436줄이 됐다.
- 권고: 해당 블록을 LF 로 정규화하고, `.gitattributes` 에 `*.properties text eol=lf` 를 추가 — 별도 결정.
