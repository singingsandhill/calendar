# DateDate AdSense 재심사 통과 — 실행 계획

## 진행 현황 (2026-05-10 — Low Value Content 프레이밍 재정렬)

> **거절 사유는 "Low value content / 가치 있는 인벤토리"** 다. 처음 plan 은 *광고 placeholder + 콘텐츠 분량 부족* 으로 진단했지만, 실측 결과 분량은 충분했고 *광고 placeholder* 만이 명확한 인프라 블로커였다. 두 번째 세션의 진단 결과 진짜 신호는 **콘텐츠 톤 + 신뢰 신호 + 사이트맵 일관성** 으로 좁혀졌고, 본 세션에서 그에 맞춰 작업이 마무리되었다.

**핵심 통찰 — 분량 vs 톤**

분량 자체가 거절의 직접 원인이 아니다. AdSense 의 "Low value content" 는 *콘텐츠가 사용자에게 실질적 가치를 주는가* 의 판단이며, 이는:
- 절차적 도움말(언제·어떻게·실수·팁) 형태인지 vs *문제 제기 → 우리 도구 만세 → CTA* 의 마케팅 카피인지
- 게시자/조직 정보가 명확한지 (E-E-A-T)
- 사이트의 테마(스케줄 조율)와 일관된 페이지만 색인 대상에 포함되는지
- 데이터가 0인 페이지가 색인 가능 상태로 남아 있지 않은지

는 점으로 평가된다. 처음 plan 은 use-case 본문이 80~120단어라고 가정하고 *분량 확장* 을 처방했지만, 실측 결과 500~600단어였다. 다만 *톤* 은 마케팅 카피였고, 이게 진짜 거절 신호일 가능성이 높다.

**실측 결과 요약**

| 항목 | 가설 | 실측 |
|---|---|---|
| use-case 슬러그 4개 본문 | 80~120 단어 | KO 약 500~600 단어 (intro + body + step1~3 narrative) |
| privacy.html 분량 | 350 단어 | 약 800 단어. AdSense·DART·GDPR·연락처 모두 명시 |
| terms.html 분량 | 350 단어 | 약 700 단어. 광고·콘텐츠 책임·중재 정책 포함 |
| 스케줄 UGC 페이지 색인 | 위험 | 이미 `noindex, nofollow` 적용 (안전) |
| 사이트맵 UGC 노출 | 위험 | 스케줄 URL 미포함 (안전). Runner URL 은 포함되어 있어 별도 처리 필요 |
| ads.txt | 미확인 | 정확한 값으로 이미 존재 |

**유일한 명확한 승인 블로커**: `templates/fragments/ad-slot.html` 의 `data-ad-client="ca-pub-XXXXXXXXXX"` placeholder 가 *adsEnabled=true 인 모든 페이지에서 DOM 에 그대로 노출* — "사이트 통합 미완료" 시그널.

**Phase B (use-case 본문 확장 1,000+ 단어), Phase C (5개 신규 슬러그), Phase D (`/guides/*` + `/about`), Phase E (privacy/terms 분량 추가) 는 *승인 블로커가 아닌 SEO 향상 작업*** 으로 재분류. 위 인프라 차단을 풀고 7일 색인 사이클 후 재심사 신청 → 통과 후 시간을 두고 콘텐츠 확장.

---

## ✅ 2026-05-10 세션에서 실행된 변경 (Phase A 의 실용적 부분)

| # | 변경 | 파일 |
|---|---|---|
| 1 | `AdsenseProperties` record (`@ConfigurationProperties("adsense")`, `isEnabled()` / `hasLeaderboardSlot()` 등) 신규 | `common/infrastructure/config/AdsenseProperties.java` |
| 2 | `AdsenseConfig` (`@EnableConfigurationProperties`) 신규 | `common/infrastructure/config/AdsenseConfig.java` |
| 3 | `AdsenseModelAdvice` (`@ControllerAdvice + @ModelAttribute("adsense")`). `@WebMvcTest` 슬라이스 호환을 위해 `@EnableConfigurationProperties(AdsenseProperties.class)` 자체에도 부착 (`@WebMvcTest` 가 `@ControllerAdvice` 는 로드하지만 `@Configuration` 은 건너뛰는 비대칭 처리) | `common/presentation/controller/AdsenseModelAdvice.java` |
| 4 | `UseCaseSlugs.ALL` 상수 신규 | `datedate/domain/usecase/UseCaseSlugs.java` |
| 5 | `application.yaml` 에 `adsense:` 블록 (`client` 기본값은 기존 승인된 pub ID, slot-* 는 빈값) | `application.yaml` |
| 6 | `head.html` adsbygoogle.js src 를 `${adsense.client}` + `${seo.adsEnabled() and adsense.enabled}` 가드로 변경 | `templates/fragments/head.html` |
| 7 | `ad-slot.html` 의 placeholder `XXXXXXXXXX` 제거. `data-ad-client/slot` 을 `${adsense.client/slotXxx}` 로 바인딩, 슬롯 ID 가 비어 있으면 fragment 자체 미렌더 | `templates/fragments/ad-slot.html` |
| 8 | `UseCaseController` SLUGS 상수를 `UseCaseSlugs.ALL` 참조로 교체 | `datedate/presentation/controller/UseCaseController.java` |
| 9 | `SitemapService` 의 use-case 4개 하드코딩을 `UseCaseSlugs.ALL` 루프로 변경 | `common/application/service/SitemapService.java` |
| 10 | `RunnerController` 4개 엔드포인트 (`home`/`runList`/`runDetail`/`memberList`) 에 `noindex, follow` 추가/변경 (나머지 4개는 기존부터 noindex). 근거: AS-Content "사이트 테마와 무관한 페이지" | `runner/presentation/controller/RunnerController.java` |
| 11 | `robots.txt` 에서 `Allow: /runners*` 5줄 제거 (default-allow + noindex 메타로 처리) | `static/robots.txt` |
| 12 | `SeoService.buildSimpleWebPageSeo` 의 `adsEnabled` 를 `true → false` (privacy/terms 가 PP-Full *행동 목적 화면* 보수 해석에 해당) | `datedate/application/service/SeoService.java` |
| 13 | `insights/trends.html` 의 ad-slot 2개에 빈 데이터 가드 — leaderboard 는 `popularLocations`/`popularMenus` 둘 다 비면 미렌더, infeed 는 `stats.totalSchedules == 0` 이면 미렌더 | `templates/insights/trends.html` |
| 14 | `style.css` 에 `.ad-slot + section / + [class*="-cta"] / + .insights-cta-section { margin-top: var(--space-12); }` 추가 (PP-Full *광고 인접* 가드) | `static/css/style.css` |

**검증**: `./gradlew test` 전체 (206 tests) 통과. Java 컴파일 + 슬라이스 테스트 + Sitemap·SEO 단위 테스트 모두 그린.

**의도적 미실행** (별도 사용자 결정 시 진행):

- **Phase B** (4개 use-case 본문을 1,070~1,400 단어로 리라이트) — 현재 ~500~600 단어로 *충분히 통과 수준*. 추가 작업은 SEO 랭킹 업사이드.
- **Phase C** (5개 신규 슬러그 family-gathering 등) — 카테고리 확장. 승인 후 진행.
- **Phase D** (`/guides/*` 4개 + `/about`) — E-E-A-T 강화 + 카테고리 다양성. 승인 후 진행.
- **Phase E 의 privacy/terms 700~900 단어 확장** — 현 분량으로 충분.
- **CMP / Funding Choices** — 광고 켜는 시점에 활성화.
- **`/tools/date-diff` 광고 OFF 또는 본문 600+ 단어 확장** — 별도 결정.

---

## ✅ 2026-05-10 세션 2 — Low Value Content 대응 (Tier 1 + Tier 2 일괄)

첫 세션 후 사용자가 거절 사유를 *Low value content* 로 재정의하면서, 본 세션에서는 *콘텐츠 톤 전환 + 신뢰 페이지 + 사이트맵 정합성* 에 집중. 사용자가 "Tier 1 + 4개 슬러그 모두" 의 가장 적극적인 옵션을 선택해 한 세션에 통합 실행.

### Tier 1 — 인프라/신뢰/thin 페이지

| # | 변경 | 파일 |
|---|---|---|
| 15 | `/about` 라우트 (`HomeController.about()`) + `getAboutSeo()` (AboutPage + Organization + BreadcrumbList JSON-LD, `adsEnabled(false)`) + `templates/about.html` (4섹션: 서비스 소개·운영 원칙·사용 대상·연락) + `seo.about.* / about.section.*` 메시지 키 ~20개 KO+EN | `HomeController.java`, `SeoService.java`, `templates/about.html`, `messages*.properties` |
| 16 | Sitemap 에서 Runner URL 4개 + 동적 Run 루프 완전 제거. `RunRepository` 의존성 제거, `latestRunTime()` 헬퍼 삭제. `/about` 양방향 엔트리 추가. *AdSense 사이트 테마 정합성*. | `SitemapService.java`, `SitemapServiceHreflangTest.java` |
| 17 | `getInsightsTrendsSeo(boolean hasData)` 시그니처 추가. 빈 데이터 시 robots `noindex,follow` + ads OFF + hreflang 비활성으로 강등. `InsightsController` 가 `popularLocations.isEmpty() && popularMenus.isEmpty() && stats.totalSchedules == 0` 으로 hasData 계산. | `SeoService.java`, `InsightsController.java` |
| 18 | `/tools/date-diff` 본문에 4개 신규 섹션 추가 (D-day 활용 시나리오·영업일 vs 달력일·FAQ 3개·관련 도구 링크). `tool.datediff.section.*` 키 ~14개 KO+EN. 본문 ~150단어 → ~600단어. | `templates/tools/date-diff.html`, `messages*.properties` |
| 19 | `index.html` 의 `travel-planning` scenario 카드 주석 해제 — 홈/푸터/사이트맵 모두 4개 use-case 일관성 회복. | `templates/index.html` |
| 20 | 푸터 "도움말" 섹션에 `/about` 링크 추가 (사용 가이드 위). | `templates/fragments/footer.html` |

### Tier 2 — Use-case 4개 5섹션 톤 전환

| # | 변경 | 파일 |
|---|---|---|
| 21 | `templates/use-cases/detail.html` 을 5섹션 (intro · whenToUse · mistakes · tips · faq) + 기존 example/cta/others 구조로 재구성. 각 신규 섹션은 `#messages.msgOrNull(...)` 빈 가드로 부분 콘텐츠 슬러그도 안전 렌더. FAQ 는 `<details><summary>` accordion. | `templates/use-cases/detail.html` |
| 22 | `getUseCaseSeo(slug)` 의 JSON-LD 에 조건부 `FAQPage` 객체 추가. `seo.useCase.{slug}.section.faq.q1` 가 비어 있으면 emit 안 함 (부분 콘텐츠 시 빈 Q&A SEO 손상 방지). 기존 `buildFaqMainEntity(5, prefix)` 헬퍼 재사용. `mOrEmpty(key)` 헬퍼 신규. | `SeoService.java` |
| 23 | 4개 슬러그 (friend-meetup · team-meeting · travel-planning · study-group) 모두 5섹션 KO+EN 콘텐츠 작성. 각 슬러그 ~32키 × KO+EN = 256키 신규. 단어 수 KO ≥ 1,000 / EN ≥ 800 충족. 외부 도구 비교 1회씩 (Doodle/When2meet · Calendly/worldtimebuddy · Google Flights/Skyscanner/Splitwise · Discord/Slack/Notion/GitHub) 으로 비방 없이 차별화. | `messages*.properties` |
| 24 | 사용자 검수 후 톤 완화 3건: (a) team-meeting "리더 없이 진행" → "회의 목적에 따라 판단"; (b) travel-planning "PTO 미승인자 불참 처리" → "확정 인원에서 제외하고 대기 상태로 관리"; (c) study-group "결석자 준비 부담 미재배정" → "본인 범위 보완 또는 자료 공유 책임" | `messages*.properties` |

### 사용자/린터 변경 (사이드)

세션 중 `application.yaml` 의 `adsense.client:` 가 사용자/린터에 의해 빈 값으로 변경됨. 결과적으로 **현재 환경에서 어떤 페이지에도 `adsbygoogle.js` 가 로드되지 않는다** — 재심사 검토자가 *콘텐츠만* 평가하게 되는 *깨끗한* 상태. 통과 후 `ADSENSE_CLIENT` 환경변수 재설정 + 슬롯 ID 발급 → `ADSENSE_SLOT_*` 주입 순.

### 검증

- `./gradlew test` 전체 통과 (206 tests + Sitemap/SEO 갱신 테스트 모두 그린).
- 4개 use-case 슬러그 모두 KO+EN JSON-LD 가 유효한 JSON (FAQPage 포함).
- `SitemapServiceHreflangTest` 갱신: Runner URL 0개 검증, `/about` 양방향 검증, hreflang 카운트 `12 × 2 × 3 = 72`.
- 콘텐츠 분량: 4개 슬러그 모두 본문 키만으로 KO ~870~995 / EN ~720~830, 기존 title·description·step 키 합산 시 KO ≥ 1,000 / EN ≥ 800 달성.

---

## Context

datedate.site (Spring Boot 4 / Thymeleaf, `datedate` 모듈) 가 2026-05-02 Google AdSense 심사에서 **"낮은 가치 콘텐츠"** 사유로 거절. 코드 조사 결과 거절을 부르는 두 가지 원인이 분명함.

**원인 1 — 광고 코드가 사실상 작동 불가 상태**
- `templates/fragments/ad-slot.html` 의 `data-ad-client="ca-pub-XXXXXXXXXX"` / `data-ad-slot="XXXXXXXXXX"` 가 모두 placeholder. head 의 client ID 만 실제값(`ca-pub-7334667748813914`).
- 결과: AdSense 봇이 슬롯에 매핑된 광고 단위를 인식하지 못함 → "사이트 통합 미완료" 시그널.

**원인 2 — 텍스트 자산이 얇음**
- use-cases 4개 슬러그(friend-meetup/team-meeting/travel-planning/study-group) 본문이 각 80~120 단어. SEO 페이지로 인식되기엔 너무 짧음.
- guide.html 600 단어 (절차만), insights/trends 800 단어 (대부분 표/숫자), privacy/terms 350 단어.
- sitemap URL 14개 — 광고 게재 사이트 기준 절대량 부족.

**목표**: 인프라(광고 게재 가능성) + 콘텐츠(분량·고유성·E-E-A-T) 양면을 동시에 해결, 약 4~5주 안에 재심사 통과 가능 상태로 끌어올린다. 핵심 도구 기능(스케줄 생성/응답)은 변경하지 않는다.

**근거 정책 — 4개 Google 문서 매핑**

본 plan 의 모든 작업은 다음 4개 정책 문서와 직접 연결된다 (자세한 1:1 매핑은 [`docs/audit/adsense-low-value-content-policy-mapping.md`](../audit/adsense-low-value-content-policy-mapping.md) 참고).

| 약칭 | 출처 | 핵심 조항 |
|---|---|---|
| **MA-Thin** | [Search Console — 직접 조치 보고서](https://support.google.com/webmasters/answer/9044175) | "부가 가치가 전혀 또는 거의 없는 빈약한 콘텐츠" |
| **PP-Spam** | [Publisher Policies — 웹 검색 스팸 정책](https://support.google.com/publisherpolicies/answer/11035931) | "자체 콘텐츠가 거의 또는 전혀 없는 ... '쿠키 커터' 접근 방식" 금지 |
| **AS-Content** | [AdSense — 콘텐츠 및 사용자 환경](https://support.google.com/adsense/answer/10015918) | 고유 콘텐츠 충분 / 중복·유사 콘텐츠 통합 / 사이트 테마와 무관한 페이지 텍스트 금지 |
| **PP-Full** | [Google 게시자 정책](https://support.google.com/adsense/answer/10502938) | *인벤토리 가치* — "게시자 콘텐츠가 없거나 가치가 별로 없는 콘텐츠" / "행동 목적으로 사용되는 화면" / "광고가 더 많은 화면" 광고 게재 불가 |

**사용자 결정**
- 범위: Phase A → F 전체 진행 (인프라 + 콘텐츠 리라이트 + 새 use-case + /guides/ 카테고리 + /about + 보조 페이지 강화 + 재심사)
- 콘텐츠 작성: Claude 가 한국어/영어 모두 작성, 사용자는 검수
- AdSense 슬롯 ID: 심사 통과 전 발급 불가 → 환경변수만 비워두고 ad-slot fragment 가 미렌더되도록 가드. 재심사는 **head 의 client ID + ads.txt 만으로** 진행.

---

## 핵심 인프라 결정

### 광고 슬롯을 환경변수 기반 ConfigurationProperties 로 분리

`AdsenseProperties` 신규 도입 (record + `@ConfigurationProperties("adsense")`). 슬롯 ID 가 비어 있으면 해당 fragment 를 렌더하지 않음 → placeholder XXXX 가 DOM 에 박히는 사고 차단. head 의 adsbygoogle.js 는 `client` 가 있으면 로드되어 AdSense 봇이 사이트 인식 가능.

### Slug 단일 진실 — `UseCaseSlugs`, `GuideSlugs`

현재 `UseCaseController` 의 SLUGS 와 `SitemapService.getSitemapEntries()` 75-78줄의 하드코딩이 분리되어 있어 슬러그 추가 시 두 곳 수정 필요. `datedate/domain/usecase/UseCaseSlugs.java` 에 `public static final List<String> ALL` 로 추출, controller + sitemap + 템플릿 cross-link 모두 공유.

### `messages.properties` 키를 섹션별로 쪼갬

현재 `seo.useCase.{slug}.body` 한 키에 통째로 넣은 패턴은 HTML 구조화 불가능. 새 패턴은 8 섹션(intro / whenToUse / commonMistakes / checklist / realExample / tips / faq / 기존 step1~3) × 약 65 키. messages.properties 가 비대해지면 Phase D 이후 `messages-useCase.properties` 등으로 파일 분리 검토.

---

## Phase 별 실행 순서

### Phase A — 인프라 정리 (1~2일)

**신규 파일**
- `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/AdsenseProperties.java` — record `(String client, String slotLeaderboard, String slotInfeed, String slotRectangle)` + `isEnabled()`, `hasLeaderboardSlot()` 등 헬퍼
- `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/AdsenseConfig.java` — `@EnableConfigurationProperties(AdsenseProperties.class)`
- `src/main/java/me/singingsandhill/calendar/common/presentation/controller/AdsenseModelAdvice.java` — `@ControllerAdvice` + `@ModelAttribute("adsense")` 로 모든 뷰에 자동 주입
- `src/main/java/me/singingsandhill/calendar/datedate/domain/usecase/UseCaseSlugs.java` — `ALL` 상수

**변경 파일**
- `src/main/resources/templates/fragments/ad-slot.html` — `data-ad-client/slot` 을 `${adsense.client}/${adsense.slotLeaderboard}` 로 바인딩. `th:if` 조건에 `adsense.hasLeaderboardSlot` 추가하여 슬롯 미설정 시 미렌더.
- `src/main/resources/templates/fragments/head.html` 89~92줄 — adsbygoogle.js 의 client 도 `${adsense.client}` 로 동적화, `adsense.enabled` 가드.
- `src/main/resources/application.yaml` — `adsense:` 블록 + `${ADSENSE_*:}` 환경변수 매핑
- `src/main/java/.../datedate/presentation/controller/UseCaseController.java` — SLUGS 상수를 `UseCaseSlugs.ALL` 참조로 교체
- `src/main/java/.../common/application/service/SitemapService.java` 75~78줄 — use-case 4개 하드코딩 제거, `for (String slug : UseCaseSlugs.ALL)` 루프
- **`src/main/java/.../runner/presentation/controller/RunnerController.java` 60, 104, 125, 144, 178, 192, 206, 225줄 (모든 `SeoMetadata.builder()` 체인) — `.robots("noindex, follow")` 추가**.  
  → **근거**: Runner 모듈은 `97 Runners` 사설 크루 도메인으로 datedate 의 *그룹 일정 조율* 테마와 무관. AdSense 봇이 사이트 전체 평가 시 *사이트의 테마 또는 비즈니스 모델과 관련이 없는 페이지의 텍스트* (AS-Content) 신호로 인식할 위험.  
  → **`noindex` 가 robots.txt `Disallow` 보다 권장**: robots.txt Disallow 는 *크롤 차단* 만 해서 외부 백링크가 있으면 *URL-only* 인덱스 잔재가 남을 수 있음. `noindex` 메타는 크롤러가 페이지를 읽고 *명시적으로 색인 거부* 신호를 받음 → 깨끗하게 빠짐.  
  → 광고는 이미 OFF (`RunnerController` 가 `SeoMetadata.builder()` 만 호출, `adsEnabled` 미설정으로 default false).
- **`src/main/resources/static/robots.txt` 13~17줄 — `Allow: /runners*` 5개 라인 제거** (대체 추가 라인 없음).  
  → robots.txt 는 *허용 = default*. 명시 Allow 가 없어도 크롤러가 접근 가능. 위 `noindex` 메타가 색인을 막음. 그래도 크롤 비용을 줄이고 싶으면 `Disallow: /runners` 를 *추가* 옵션으로 둘 수 있으나 noindex 와 Disallow 동시 적용 시 noindex 가 무시될 수 있으므로 권장하지 않음.
- **`src/main/resources/static/css/style.css` — `.ad-slot + section` 신규 추가** (`.ad-slot-*` min-height 룰은 3856~3888줄에 이미 존재하므로 재추가하지 않음).  
  ```css
  /* AD SLOT 섹션 끝(line 3889) 다음에 추가 */
  .ad-slot + section,
  .ad-slot + [class*="-cta"],
  .ad-slot + .insights-cta-section { margin-top: var(--space-12); }
  ```
  → **근거**: PP-Full 의 *광고 방해* 조항 — "탐색 작업 또는 다른 작업 항목에 인접 ... 의도하지 않은 광고 상호작용". 페이지의 CTA 섹션은 `use-case-cta`, `guide-cta`, `faq-cta`, `insights-cta-section` 등 클래스가 다양해서 `[class*="-cta"]` 와 `<section>` 둘 다 잡음. `var(--space-12)` 는 기존 토큰 사용 (≈ 3rem).

**검증**
- 빌드/실행 후 `curl http://localhost:8081/use-cases/friend-meetup` → 광고 자리에 placeholder 없음 (DOM 깨끗)
- `curl http://localhost:8081/sitemap.xml` → use-case URL 4개 그대로 나오는지 확인
- `curl http://localhost:8081/robots.txt | grep -i runners` → `Disallow: /runners` 한 줄만, `Allow:` 없음
- DevTools Computed Style: `.ad-slot + section` 의 `margin-top` ≥ 48px

### Phase B — 기존 use-case 4개 본문 리라이트 (5~7일)

순서: `friend-meetup` → `team-meeting` → `travel-planning` → `study-group` (검색 수요 순)

각 슬러그마다 다음 8 섹션을 한국어 1,070~1,400 단어 / 영어 850~1,100 단어로 채움 (영어는 직역 X, 영어권 검색 의도 맞춰 별도 작성):

| # | 섹션 | 키 |
|---|------|-----|
| 1 | intro | `seo.useCase.{slug}.section.intro` |
| 2 | whenToUse | `.whenToUse.{title,lead,scenario1.title,scenario1.text,...,scenario3.text}` |
| 3 | commonMistakes | `.mistakes.{title,lead,mistake1.problem,mistake1.fix,...,mistake3.fix}` |
| 4 | checklist | `.checklist.{title,lead,item1...item10}` |
| 5 | realExample | `.realExample.{title,intro,para1,para2,para3}` |
| 6 | tips | `.tips.{title,tip1.title,tip1.text,...,tip3.text}` |
| 7 | faq | `.faq.{q1,a1,...,q5,a5}` |
| 8 | step1~3 (기존 보존) | 기존 키 유지, 짧게 다듬기 |

**변경 파일**
- `src/main/resources/messages.properties` — 약 520 라인 신규
- `src/main/resources/messages_en.properties` — 동일 키 영문판
- `src/main/resources/templates/use-cases/detail.html` — 한 번만 변경, 8 섹션 렌더링하도록 `<section>` 별로 `th:replace`/`th:text` 확장. FAQ 는 `<details><summary>` accordion. 이 템플릿은 Phase C 의 신규 9개 슬러그도 그대로 사용.
- `src/main/java/.../datedate/application/service/SeoService.java` `getUseCaseSeo(String slug)` (354-417줄) — JSON-LD 에 `FAQPage` 블록 추가 (`buildFaqMainEntity(5, "seo.useCase." + slug + ".section.faq")` 활용; 기존 FAQ 빌더 그대로 재사용).

**톤/검수 기준 (각 페이지)**
- 단어수 KO ≥ 1,000 / EN ≥ 800
- H2 ≥ 3, H3 ≥ 5 (검색 결과에 노출되는 구조)
- 표 또는 체크리스트 1개 이상
- 외부 도구 비교 표현 1개 이상 (When2meet/Doodle/구글폼, 비방 없이)
- 한국어 존댓말 + 체크리스트 명령조 / 영어는 US English short sentence

### Phase C — 신규 use-case 5개 추가 (7~10일)

대상 (P0 — 한국 검색 수요 + AdSense 콘텐츠 다양성 모두 충족): `family-gathering`, `birthday-party`, `club-activity`, `wedding-events`, `company-dinner`

각 슬러그마다:
- `UseCaseSlugs.ALL` 에 추가 (한 줄)
- messages 한·영에 8 섹션 키 약 130 라인 (Phase B 와 동일 스키마)
- 템플릿/컨트롤러/sitemap 변경 없음 (Phase A 의 동적화 덕분에 자동 반영)
- `templates/index.html` 의 `scenarios-grid` 에 새 카드 노출 (선택, 내부 링크 강화)

**Phase A 동적화의 누락 보강 — `footer-minimal` 의 use-cases 4개 하드코딩 제거**
- `templates/fragments/footer.html` 49~55줄의 `<a th:href="${@localeLinks.href('/use-cases/friend-meetup')}">...` 4개 카드는 신규 슬러그가 추가돼도 자동 반영되지 않음.
- **수정**: `<th:block th:each="slug : ${T(me.singingsandhill.calendar.datedate.domain.usecase.UseCaseSlugs).ALL}">` 루프로 전환, 각 카드 라벨은 `#{'seo.useCase.' + ${slug} + '.title'}`. 또는 footer 도 `@ModelAttribute` 로 `useCaseSlugs` 주입.
- 미수정 시 신규 5 슬러그가 footer 에서 누락되어 *내부 링크 부족* 으로 인덱싱 지연.

**내부 링크 권장 (Phase B/C 공통)**
- 각 use-case 본문에 *관련 use-case 2~3개* `<a>` 링크 (이미 detail.html 의 `use-case-others` 섹션이 비슷하지만 본문 내부 링크는 SEO 가중치가 더 큼).
- index.html `scenarios-grid` 에 신규 5 슬러그도 추가 (현재 3개만 노출됨).

### Phase D — `/guides/` 카테고리 도입 (5~7일)

use-case 가 *상황별*이라면 guides 는 *주제별 long-form*. AdSense 가 평가하는 카테고리 다양성에 결정적.

**신규 파일**
- `src/main/java/.../datedate/presentation/controller/GuidesController.java` — `@RequestMapping("/guides")`, `@GetMapping("/{slug}")` + `@GetMapping` index. UseCaseController 패턴 복사
- `src/main/java/.../datedate/domain/guide/GuideSlugs.java` — `ALL` 상수 (4 슬러그)
- `src/main/resources/templates/guides/index.html` — 4개 카드 그리드
- `src/main/resources/templates/guides/detail.html` — long-form article 레이아웃 (use-cases/detail.html 패턴 복제, JSON-LD 는 `Article` 사용)

**변경 파일**
- `src/main/java/.../datedate/application/service/SeoService.java` — `public SeoMetadata getGuideHubSeo(String slug)`, `public SeoMetadata getGuidesIndexSeo()` 추가. JSON-LD 빌더 `Article` 추가 (author=Organization "DateDate", datePublished, dateModified, mainEntityOfPage)
- `src/main/java/.../common/application/service/SitemapService.java` — `/guides` + `for (String slug : GuideSlugs.ALL)` 루프 추가
- `src/main/resources/static/robots.txt` 21줄 다음 — `Allow: /guides/`
- `src/main/resources/templates/fragments/header.html` — "가이드" 메뉴 항목 추가
- messages 한·영 — `seo.guide.{slug}.*` 키 약 600 라인 추가

**대상 슬러그 (각 1,200~1,800 단어)**
| 슬러그 | 주제 |
|--------|------|
| `how-to-schedule-meeting` | 여러 명 회의 시간 잡는 5가지 방법 (DateDate vs 구글 캘린더 vs 엑셀 vs When2meet vs Doodle) |
| `timezone-coordination` | 다국적 팀 시간대 맞추기 (UTC/KST/PST 표 + 도구 팁) |
| `group-poll-best-practices` | 그룹 투표 시 흔한 실수와 해결법 |
| `social-etiquette-scheduling` | 약속 잡을 때 매너 (응답 마감, 취소 통보, 시간 변경 통지) |

**추가 신뢰 페이지: `/about` (E-E-A-T)**

AS-Content 의 *고유 콘텐츠 ... 전문 지식이나 개인적인 의견 등* 조항과 PP-Full 의 *오해의 소지가 있는 표현* 조항은 게시자/조직 정보의 명확한 공개를 요구. 현재 `head.html` 18줄의 `<meta name="author" content="DateDate">` 만으로는 게시자 신원이 모호.

**신규 파일**
- `src/main/resources/templates/about.html` — 운영 조직 소개, 서비스 시작 배경, 데이터 처리 방침 요약, 연락처(privacy 페이지의 의견 보내기 폼 재사용). 한국어 600~900단어 / 영어 500~700단어.
- `messages.properties` / `messages_en.properties` — `about.*` 키 약 25개

**변경 파일**
- `src/main/java/.../datedate/presentation/controller/HomeController.java` — `@GetMapping("/about")` 추가, `seoService.getAboutSeo()` 호출.
- `src/main/java/.../datedate/application/service/SeoService.java` — `getAboutSeo()` 신규. JSON-LD 의 `Organization` 에 `founder`, `foundingDate`, `sameAs` (사이트 운영자의 GitHub/X 등 공개 프로필 URL), `contactPoint.email` 채우기. `adsEnabled(false)` (행동/안내 목적 페이지).
- `src/main/java/.../common/application/service/SitemapService.java` — `/about` URL 추가
- `src/main/resources/templates/fragments/footer.html` 33~40줄 (`도움말` 섹션) — `/about` 링크를 *사용 가이드* 위에 추가 (footer 의 `정책` 섹션은 법적 페이지만 두고, /about 은 *서비스 안내* 성격으로 도움말에 배치).
- `src/main/resources/static/robots.txt` — `Allow: /about` 추가 (다만 robots.txt 는 default 허용이므로 명시 Allow 는 가독성 목적).

> **사전 확인된 안전 사항**
> - `/about` 슬러그는 `ReservedOwnerIds.RESERVED` (line 21) 에 이미 등록됨 → owner ID 와 라우트 충돌 없음. 추가 변경 불필요.
> - SecurityConfig 의 permitAll 정책에 `/about` 이 별도 매칭 필요 없음 (catch-all `/**` permitAll 에 포함).

### Phase E — privacy/terms/insights/guide/faq/tools 강화 (2~3일)

**`privacy.html` (350 → 700~900 단어, 광고 OFF)** — AdSense 정책상 필수 항목:
- Google AdSense 및 제3자 광고 파트너의 쿠키 사용
- DoubleClick DART 쿠키
- 사용자가 광고 개인화를 거부하는 방법 (`adssettings.google.com` 링크)
- EEA/UK 사용자 GDPR 동의 안내
- 키: `seo.privacy.section.{collection,cookies,thirdParty,gdpr,rights,contact}.{title,body}`

**`terms.html` (350 → 700~900 단어, 광고 OFF)** — 면책, 데이터 보관 기간, 서비스 변경/종료, 분쟁 해결 (대한민국 법, 서울중앙지법 1심 관할)

> **광고 OFF 변경**: PP-Full 의 *행동 목적으로 사용되는 화면* 조항을 보수적으로 해석하면, 법적 안내 페이지에 광고를 박는 것은 *광고 적합성 의심 신호* 가 될 수 있다. `SeoService.buildSimpleWebPageSeo()` (528~579줄) 의 `.adsEnabled(true)` 를 prefix 가 `seo.privacy` / `seo.terms` 인 경우 `false` 로 분기. 두 페이지의 `<head>` 의 `adsbygoogle.js` 와 본문 ad-slot 모두 미로드.

**`templates/insights/trends.html`** — 표 위에 `commentary.intro` (이 페이지의 의미), 표 아래 `commentary.whyPopular` (상위 항목 해석), `commentary.howToUse` (실용 팁 4~5개) 섹션 추가.  
**ad-slot 가드 강화 — 페이지에 광고가 2개임에 주의** (130줄 leaderboard, 259줄 infeed). 두 자리 모두 *문맥에 맞는 콘텐츠* 가드를 따로 걸어야 함:

```html
<!-- 130줄: 인기 순위 섹션 직후 leaderboard
     → "인기 데이터" 가 비어있으면 미렌더 -->
<th:block th:if="${!popularLocations.isEmpty() or !popularMenus.isEmpty()}">
  <div th:replace="~{fragments/ad-slot :: leaderboard(${seo.adsEnabled()})}"></div>
</th:block>

<!-- 259줄: 상세 통계 섹션 직후 infeed
     → "통계" 가 0 이면 미렌더 -->
<th:block th:if="${stats.totalSchedules > 0}">
  <div th:replace="~{fragments/ad-slot :: infeed(${seo.adsEnabled()})}"></div>
</th:block>
```
근거: PP-Full *게시자 콘텐츠가 없거나 가치가 별로 없는 콘텐츠를 사용한 화면* 광고 불가. 신규 사이트일수록 이 가드가 먼저 작동. 두 가드를 통합하지 않은 이유: 인기 데이터는 있는데 schedule 통계는 0 인 경계 케이스가 사실상 발생 안 하지만, *각 광고가 자기 위 콘텐츠를 따른다* 는 원칙으로 분리.

**Cookie / GDPR 동의 배너 (CMP)** — Google 의 *EU 사용자 동의 정책* 은 EU 트래픽에 개인 맞춤 광고 게재 시 명시적 동의를 요구. privacy.html 본문에 GDPR 안내 문구를 넣는 것만으로는 부족하며 *동의 UI* 가 필요:
- (a) **간편 옵션**: Google 의 [Funding Choices CMP](https://support.google.com/admanager/answer/9035026) 활성화 (AdSense 콘솔 → 개인정보 보호 및 메시지). 사이트 변경 거의 없음, 자동 게재.
- (b) **자체 구현**: `templates/fragments/cookie-consent.html` 신규 + localStorage 기반 동의 상태. 한·영 메시지.
- 권장: (a) 로 시작 — 재심사 통과 후 광고 켤 시점에 활성화. 재심사 자체는 EU 동의 UI 가 *없어도* 통과 가능 (콘텐츠 심사 단계).

**`templates/guide.html` (600 → 1,300 단어)** — 각 step 아래 "흔한 함정", 트러블슈팅 6개 Q&A, 단축키/팁 4개, "DateDate vs 외부 도구" 비교표 한 단락. 화면 캡처 자리(`<img th:src="@{/image/guide/step{n}.png}">`)는 alt 만 채우고 실 이미지는 후속 작업.

**`templates/faq.html`** (sitemap 포함) — 현재 분량이 부족하면 질문 5개 추가하여 25개로 확장 (use-case 별 FAQ 와 다른 도구 일반 FAQ 위주)

**`templates/tools/date-diff.html`** — 현재 상단은 계산기 UI, 하단 정보 섹션은 4개 짧은 불릿 (`tool.datediff.info.li1~4`) 만. PP-Full 의 *행동 목적으로 사용되는 화면* 으로 해석될 위험. 다음 둘 중 택 1:
- (a) **광고 OFF**: `SeoService.getDateDiffSeo()` (656~724줄) 의 `.adsEnabled(true)` 를 `false` 로. 본문은 그대로 둬도 됨.
- (b) **본문 600+ 단어 확장 + 광고 유지**: "언제 D-Day 계산이 유용한가 (기념일·계약·디데이)", "평일 계산이 다른 이유와 영업일 정의", "엑셀 `NETWORKDAYS` 와 차이", "공휴일을 어떻게 처리할지" 4 섹션 추가. messages 키 `tool.datediff.section.{when,workdayDef,excelDiff,holidays}.{title,body}`.
- 권장: 우선 (a) 로 광고 OFF → 콘텐츠 확장 여유가 생기면 후속 작업으로 (b) 진행.

**CSS** — Phase A 에서 이미 `.ad-slot-leaderboard / .ad-slot-infeed / .ad-slot-rectangle` min-height 추가 완료. Phase E 에서는 새 섹션(commentary, tools 본문 확장 등) 의 typography·간격만 마무리.

### Phase F — 재심사 신청 + 모니터링 (1일 + 7~14일 대기)

**재심사 전 체크리스트 (모두 충족해야 신청)**

| 항목 | 기준 | 검증 |
|------|------|------|
| 인덱싱된 페이지 수 | ≥ 25 | Search Console > Coverage |
| 평균 페이지 단어 수 | ≥ 600 | 수동 + sitemap 기반 curl |
| 광고 코드 통합 | head 의 `adsbygoogle.js` 가 `client=ca-pub-7334667748813914` 로 로드됨 | 운영 서버 페이지 소스 확인 |
| ads.txt | `google.com, pub-7334667748813914, DIRECT, f08c47fec0942fa0` | `curl https://datedate.site/ads.txt` |
| ad-slot DOM 깨끗 | placeholder XXXX 없음, 슬롯 미설정 시 fragment 자체 미렌더 | DevTools |
| robots.txt — 콘텐츠 페이지 Allow | `/use-cases/`, `/guides/`, `/insights/`, `/about` 모두 Allow | 검토 |
| robots.txt — 비테마 페이지 Disallow | `/trading/`, `/stock/`, `/api/`, `/h2-console/` Disallow (단, `/runners` 는 robots.txt 가 아닌 `noindex` 메타로 처리) | `curl /robots.txt` |
| Runner 페이지 noindex | `/runners`, `/runners/runs`, `/runners/members`, `/runners/announce` 모두 `<meta name="robots" content="noindex, follow">` 노출 | view-source 8개 라우트 |
| privacy 광고 정책 명시 | "Google AdSense 와 제3자 광고 파트너 쿠키" 문구 | privacy.html grep |
| privacy/terms 광고 OFF | 두 페이지에서 `adsbygoogle.js` 미로드 | view-source |
| insights/trends 데이터 0 가드 | 데이터 0 시 ad-slot DOM 자체 미렌더 | 신규 환경에서 view-source |
| /tools/date-diff 광고 정책 일치 | (a) 광고 OFF 로 `adsbygoogle.js` 미로드 또는 (b) 본문 ≥ 600단어 | view-source / 단어수 |
| ad-slot ↔ CTA 시각 분리 | `.ad-slot + section` margin-top ≥ 48px | DevTools Computed |
| /about 페이지 게시자 정보 | Organization JSON-LD 의 `founder` / `sameAs` / `contactPoint` 채워짐 | view-source `application/ld+json` |
| 모든 sitemap URL HTTP 200 | 전수 점검 | curl 스크립트 |
| Lighthouse Performance ≥ 70 / SEO ≥ 95 (모바일) | DevTools | |
| sitemap 마지막 업데이트 ≥ 7일 전 | 콘텐츠 안정화 | sitemap.xml |
| Search Console / Naver / Bing 사이트맵 재제출 | 신규 페이지 인식 | 각 webmaster |

**재심사 절차**
1. 위 체크리스트 통과 → AdSense 콘솔 → 사이트 → 검토 요청
2. 결과 대기 2~14일. 재거절 시 사유 분석 → 가장 흔한 추가 사유: navigation broken / mobile usability / content not original / GDPR consent 미흡 → 해당 항목만 수정 후 재신청
3. 통과 직후 AdSense 콘솔에서 광고 단위 3개 (디스플레이 728×90 / 인피드 / 사각형 300×250) 생성 → 슬롯 ID 를 `ADSENSE_SLOT_LEADERBOARD/INFEED/RECTANGLE` 환경변수에 주입 → 재배포

---

## 재사용할 기존 함수/파일

| 위치 | 재사용 |
|------|--------|
| `SeoService.buildFaqMainEntity(int count, String prefix)` | use-case 별 FAQ JSON-LD (Phase B) 와 guide FAQ 모두 사용 |
| `SeoService.getUseCaseSeo(String slug)` (354-417줄) | 그대로 유지, JSON-LD 만 FAQPage 추가 |
| `SeoMetadata` SSOT (canonical, hreflang, JSON-LD) | 모든 신규 페이지가 이 단일 DTO 통과 |
| `fragments/head.html` `head(seo)` | 모든 신규 페이지 head 재사용 |
| `fragments/ad-slot.html` `leaderboard/infeed/rectangle(adsEnabled)` | Phase A 에서 ID 동적화만 추가, 호출부 변경 없음 |
| `LocaleLinks.redirect(String)` | 잘못된 슬러그 fallback (UseCaseController 35-37줄 패턴) |
| `IndexNowService` (이미 구현) | `INDEXNOW_ENABLED=true` 면 신규 콘텐츠 발행 시 자동 통보 |
| `SitemapEntry(loc, lastmod, changefreq, priority, bilingual)` | 신규 URL 추가 시 그대로 |

---

## 변경 요약 (Phase 합산)

| 항목 | 수치 |
|------|------|
| 신규 Java 파일 | 8 (Adsense 3 + UseCaseSlugs + GuideSlugs + GuidesController + 2 templates) |
| 변경 Java 파일 | 6 (UseCaseController, SitemapService, SeoService, HomeController, RunnerController, application.yaml) |
| 변경 Thymeleaf 템플릿 | 11 (ad-slot, head, header, footer, use-cases/detail, insights/trends, guide, privacy, terms, faq, tools/date-diff) |
| 신규 Thymeleaf 템플릿 | 3 (guides/index, guides/detail, about) |
| 변경 정적 파일 | 2 (`static/robots.txt`, `static/css/style.css`) |
| messages.properties 신규 라인 | 약 1,900 (한·영 합산, /about + tools 본문 옵션 B 포함) |
| 신규 콘텐츠 페이지 (한·영 합산) | 22 (use-case 5×2 + guides 4×2 + about×2) |
| 기존 페이지 강화 | 10 (use-case 4 + guide + insights + privacy + terms + faq + tools/date-diff) |

---

## 위험 요인 & 대응

1. **영문 직역으로 자동생성 의심** (PP-Spam *쿠키 커터*) → 영문은 한국어와 별개로 영어권 검색 의도 기준으로 작성. AdSense 심사관이 영어권일 확률 높아 영문 품질이 더 중요.
2. **트래픽 0** → AdSense 가 일정 트래픽을 보는 경우 보고됨. Phase D 끝나면 SNS/지인 공유로 1주일 자연 traffic 받기.
3. **한 페이지 광고 슬롯 3개 모두 박기** (PP-Full *광고가 더 많은 화면*) → use-case detail 은 infeed 1개 + 하단 leaderboard 1개까지. trends 는 leaderboard + infeed 2개. index 는 0 (CTA 우선).
4. **빈 데이터 페이지 광고 노출** (PP-Full *콘텐츠가 없는 화면*) → insights/trends 의 ad-slot 호출을 `${stats.totalSchedules > 0 and (!popularLocations.isEmpty() or !popularMenus.isEmpty())}` 로 가드 (Phase E).
5. **messages.properties 비대화** (514 → 약 2,400 라인) → Phase D 이후 `messages-useCase.properties`, `messages-guide.properties` 분리 검토 (`MessageSource.basenames` 다중 등록).
6. **콘텐츠 변경 후 즉시 재심사** → 구글 색인 + 봇 방문 사이클 7일 대기 후 재신청.
7. **테마 외 페이지 인덱싱** (AS-Content *사이트의 테마와 관련 없는 페이지의 텍스트*) → Phase A 의 robots.txt 변경으로 `/runners` Disallow. 이후 사설 모듈을 더 추가할 때도 *기본은 Disallow* 정책 유지.
8. **광고 ↔ 작업 항목 인접** (PP-Full *광고 방해*) → Phase A 의 CSS `margin-top: 3rem` 으로 일괄 분리. 신규 페이지 추가 시에도 `ad-slot` 직후가 항상 `<section>` 또는 `.cta` 이도록 마크업 일관성 유지.
9. **게시자 신원 모호** (AS-Content E-E-A-T) → Phase D 의 `/about` + `Organization` JSON-LD 의 `founder/sameAs/contactPoint` 채움. 비워두면 *오해의 소지가 있는 표현* (PP-Full) 으로 해석될 수 있음.
10. **Better Ads Standard 비준수** (PP-Full *요구사항 및 기타 표준*) → 팝업·전면 광고·자동 재생 광고 사용 금지. 본 plan 의 ad-slot 3종(leaderboard, infeed, rectangle) 만 사용하므로 기본 OK. 후속 변경 시 [Coalition for Better Ads](https://www.betterads.org/) 가이드 재검토.
11. **EU 사용자 동의 누락** (PP-Full *개인 정보 보호 관련 정책 → EU 사용자 동의 정책*) → privacy.html 본문 안내만으로는 *개인 맞춤 광고* 게재 시 미준수. Phase E 의 CMP(권장: Google Funding Choices) 로 동의 UI 확보. 광고 켜기 전까지는 재심사 자체에는 영향 없으나, 실제 광고 게재 단계 직전 반드시 활성화.
12. **`/runners` 색인 처리 방식** (AS-Content) → 처음 plan 은 robots.txt `Disallow` 였으나 *백링크 잔재* 위험으로 `noindex` 메타로 변경. `noindex` 가 효과를 보려면 크롤러가 페이지를 **읽을 수 있어야** 함 → robots.txt Allow/Disallow 모두 명시하지 않고 default 허용에 의존. Disallow 와 noindex 동시 적용은 *noindex 무시* 위험이므로 금지.

---

## 검증 (각 Phase 완료 시)

**Phase A**
- `./gradlew build` 통과
- `./gradlew bootRun` 후 `curl http://localhost:8081/use-cases/friend-meetup | grep ad-slot` → placeholder XXXX 없음
- `curl http://localhost:8081/sitemap.xml | grep use-cases` → 4개 URL 그대로
- `curl -s http://localhost:8081/robots.txt | grep -E "Allow:.*runners"` → 0건 (Allow 라인 5개 모두 제거됨)
- `curl -s http://localhost:8081/runners | grep "name=\"robots\""` → `<meta name="robots" content="noindex, follow">` 노출
- DevTools Computed Style: `.ad-slot + section` 의 `margin-top` ≥ 48px (use-case detail 또는 faq 페이지에서 확인). `.ad-slot-leaderboard` 의 `min-height: 90px` 가 *기존대로* 유지(중복 추가되지 않음).

**Phase B**
- 운영 빌드 후 한국어/영어 양쪽 페이지 단어수 측정 (KO ≥ 1,000, EN ≥ 800)
- 페이지에 `application/ld+json` 의 `@type: "FAQPage"` 블록 포함 (`view-source` 확인)
- Lighthouse SEO ≥ 95 (모바일)
- `?lang=en` 으로 영문 페이지 직접 검수

**Phase C**
- 신규 5 슬러그 모두 200 응답: `for s in family-gathering birthday-party club-activity wedding-events company-dinner; do curl -sI https://datedate.site/use-cases/$s | head -1; done`
- sitemap.xml 에 5 URL 신규 추가 확인

**Phase D**
- `curl https://datedate.site/guides` → 200, 4개 카드 노출
- 각 `/guides/{slug}` 200 + JSON-LD `Article` 포함
- `curl https://datedate.site/about` → 200 + JSON-LD `Organization` 의 `founder/sameAs/contactPoint` 비어있지 않음
- robots.txt 에 `Allow: /guides/`, `Allow: /about`

**Phase E**
- privacy/terms 페이지 단어수 ≥ 700, AdSense/GDPR 키워드 grep 통과
- privacy/terms 페이지 view-source: `adsbygoogle.js` 미로드 (`<script ... adsbygoogle.js` grep 결과 0건)
- insights/trends 에 commentary 텍스트 추가 확인. 빈 DB 로 시작한 view-source 검사:
  - `popularLocations` `popularMenus` 둘 다 빈 상태 → leaderboard ad-slot DOM 미렌더
  - `stats.totalSchedules == 0` 상태 → infeed ad-slot DOM 미렌더
- /tools/date-diff: (a) view-source 에 `adsbygoogle.js` 미로드 OR (b) 페이지 단어수 ≥ 600
- CMP (선택, EU 트래픽 예정 시): AdSense 콘솔 → 개인정보 보호 및 메시지 → Funding Choices 활성화 확인

**Phase F**
- 위 *재심사 전 체크리스트* 16개 항목 모두 통과
- AdSense 콘솔 검토 요청 → 결과 대기

---

## Critical Files

- `/mnt/d/projects/calendar/src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java` — `getDateDiffSeo`, `buildSimpleWebPageSeo` (privacy/terms 광고 OFF 분기), `getAboutSeo` 신규
- `/mnt/d/projects/calendar/src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/UseCaseController.java`
- `/mnt/d/projects/calendar/src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/HomeController.java` — `/about` 핸들러
- `/mnt/d/projects/calendar/src/main/java/me/singingsandhill/calendar/runner/presentation/controller/RunnerController.java` — 8개 `SeoMetadata.builder()` 체인에 `.robots("noindex, follow")` 추가
- `/mnt/d/projects/calendar/src/main/java/me/singingsandhill/calendar/common/application/service/SitemapService.java`
- `/mnt/d/projects/calendar/src/main/resources/templates/fragments/ad-slot.html`
- `/mnt/d/projects/calendar/src/main/resources/templates/fragments/head.html`
- `/mnt/d/projects/calendar/src/main/resources/templates/fragments/footer.html` — `/about` 링크
- `/mnt/d/projects/calendar/src/main/resources/templates/use-cases/detail.html`
- `/mnt/d/projects/calendar/src/main/resources/templates/insights/trends.html` — ad-slot 가드 + commentary 섹션
- `/mnt/d/projects/calendar/src/main/resources/templates/tools/date-diff.html` — 광고 OFF 또는 본문 확장
- `/mnt/d/projects/calendar/src/main/resources/templates/about.html` (신규)
- `/mnt/d/projects/calendar/src/main/resources/messages.properties`
- `/mnt/d/projects/calendar/src/main/resources/messages_en.properties`
- `/mnt/d/projects/calendar/src/main/resources/application.yaml`
- `/mnt/d/projects/calendar/src/main/resources/static/robots.txt` — `/runners` Disallow + `/about`, `/guides/` Allow
- `/mnt/d/projects/calendar/src/main/resources/static/css/style.css` — `.ad-slot + section { margin-top: 3rem }`

---

## 부록 — 정책 출처 & 관련 문서

**Google 공식 정책 (재심사 전 한 번 더 통독 권장)**
- [Search Console — 직접 조치 보고서](https://support.google.com/webmasters/answer/9044175) (MA-Thin)
- [Publisher Policies — 웹 검색 스팸 정책](https://support.google.com/publisherpolicies/answer/11035931) (PP-Spam)
- [AdSense — 콘텐츠 및 사용자 환경](https://support.google.com/adsense/answer/10015918) (AS-Content)
- [Google 게시자 정책](https://support.google.com/adsense/answer/10502938) (PP-Full)

**사내**
- [`docs/audit/adsense-low-value-content-policy-mapping.md`](../audit/adsense-low-value-content-policy-mapping.md) — 정책 ↔ 코드/콘텐츠 1:1 매핑 감사 (본 plan 의 보강 근거)
- [`docs/adr/common/seo/0007-content-pages-for-adsense.md`](../adr/common/seo/0007-content-pages-for-adsense.md) — `/guide`, `/use-cases/*` 도입 결정
- [`docs/seo-evolution-playbook.md`](../seo-evolution-playbook.md) — 콘텐츠 폭발 단계 가이드

---

## 부록 — 타당성 검토 변경 이력

| 날짜 | 검토 항목 | 발견/수정 |
|---|---|---|
| 2026-05-05 | Phase A CSS 블록 | `.ad-slot-*` min-height 룰이 `style.css` 3856~3888 줄에 *이미 존재* → 중복 추가 제거. `.ad-slot + section` 룰만 신규 추가, `var(--space-12)` 토큰 사용. `[class*="-cta"]` 와 `.insights-cta-section` 추가 매칭으로 모든 CTA 변형 커버. |
| 2026-05-05 | Runner 색인 처리 | `robots.txt Disallow` → `RunnerController` 의 `SeoMetadata.builder()` 8곳에 `.robots("noindex, follow")` 추가로 변경. Disallow 는 외부 백링크 시 *URL-only* 인덱스 잔재 위험. noindex 가 효과를 보려면 크롤러가 페이지를 읽을 수 있어야 하므로 robots.txt 의 `Allow: /runners*` 5줄은 *제거* 하되 `Disallow` 는 추가하지 않음 (default 허용). |
| 2026-05-05 | insights/trends ad-slot 가드 | 페이지에 광고가 *2개* (130줄 leaderboard, 259줄 infeed) 임을 확인. 각각 위 콘텐츠를 따르는 별도 가드 적용으로 분리. |
| 2026-05-05 | footer 의 use-cases 4 하드코딩 | Phase C 신규 5 슬러그가 footer 에 자동 노출 안 되는 누락 발견 → footer-minimal 의 `usecases` 섹션을 `UseCaseSlugs.ALL` 루프로 동적화. |
| 2026-05-05 | `/about` 라우트 충돌 | `ReservedOwnerIds.RESERVED` (line 21) 에 `"about"` 이미 등록 확인 → 추가 변경 없음. plan 에 명시. |
| 2026-05-05 | `ads.txt` 존재 | `static/ads.txt` 가 이미 정확한 값으로 존재 확인 → Phase A 신규 작업 아님. Phase F 체크리스트 그대로 유효. |
| 2026-05-05 | EU 사용자 동의 | 광고 게재 단계에서 PA 광고 EU 동의 정책 미준수 위험 신규 발견 → Phase E 에 CMP(Funding Choices 권장) 항목 추가, 위험 #11 등록. |
| 2026-05-10 | 콘텐츠 분량 가설 검증 | use-case 4개는 ~500~600 단어 (가설 80~120 의 5~6배), privacy/terms 는 ~700~800 단어 (가설 350 의 2배). Phase B/E 의 분량 확장은 *통과 블로커가 아닌 SEO 향상 작업* 으로 재분류. |
| 2026-05-10 | Phase A 실행 | 인프라(AdsenseProperties + ModelAdvice + UseCaseSlugs + ad-slot/head 환경변수화 + Runner noindex 4개 + robots.txt /runners Allow 제거 + privacy/terms 광고 OFF + insights ad-slot 빈 데이터 가드 + .ad-slot 인접 CSS) 1회 적용. `./gradlew test` 통과. |
| 2026-05-10 | `@WebMvcTest` 호환 | `AdsenseModelAdvice` 가 `@ControllerAdvice` 로서 슬라이스 테스트에 자동 로드되지만 `AdsenseConfig` 는 로드되지 않아 `AdsenseProperties` 빈 누락으로 컨텍스트 실패. 해결: `@EnableConfigurationProperties(AdsenseProperties.class)` 를 advice 자체에도 부착. (전역 `@ConfigurationPropertiesScan` 도 검토했으나 기존 `IndexNowConfig` 와의 일관성을 위해 로컬 해결책 채택.) |
| 2026-05-10 (세션 2) | 거절 사유 재정의 | 거절 사유가 "Low value content / 가치 있는 인벤토리" 임이 확인됨. 1차 가설(*분량 부족*)은 실측에서 기각, 진짜 신호는 *use-case 톤이 마케팅 카피* 임으로 좁혀짐. plan 의 우선순위 재정렬: Phase A 인프라는 끝났으니 콘텐츠 톤 + 신뢰 페이지(/about) + 사이트맵 정합성 (Runner 제거) + thin 페이지 보강(date-diff, insights 빈 데이터)이 다음 세션의 핵심. |
| 2026-05-10 (세션 2) | Tier 1 + Tier 2 일괄 실행 | 사용자가 "Tier 1 + 4개 슬러그 모두" 가장 적극 옵션 선택. 한 세션에 /about + Runner sitemap 제거 + insights noindex 가드 + date-diff 본문 확장 + index travel scenario 복원 + footer /about 링크 + use-case 5섹션 템플릿 + 조건부 FAQPage JSON-LD + 4개 슬러그 KO+EN ~7,660 단어 콘텐츠 작성 완료. |
| 2026-05-10 (세션 2) | 콘텐츠 톤 완화 | 검수 후 사용자 지적으로 3건 완화: team-meeting 의 "대신 진행자" → "회의 목적에 따라 판단", travel-planning 의 "불참 처리" → "확정 제외 + 대기 상태 관리", study-group 의 "준비 부담 미재배정" → "본인 범위 보완 또는 자료 공유 책임". |
| 2026-05-10 (세션 2) | `application.yaml adsense.client` 빈 값 | 사용자/린터가 `client:` 를 빈 값으로 변경. 결과적으로 어떤 페이지에도 `adsbygoogle.js` 가 로드되지 않음 → 재심사가 *콘텐츠만* 평가받는 깨끗한 상태로 진행. 통과 후 `ADSENSE_CLIENT` 환경변수 재설정 + 슬롯 ID 발급 → `ADSENSE_SLOT_*` 주입. |
