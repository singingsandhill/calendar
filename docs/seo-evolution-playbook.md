# SEO 진화 기록 & 다음 프로젝트를 위한 플레이북

DateDate 프로젝트(2025-12 ~ 2026-05)의 SEO 정책·아키텍처가 어떤 결정과 사고를 거쳐
지금에 이르렀는지를 git 기록으로 재구성한 문서.

다음 프로젝트에서 같은 시행착오를 반복하지 않도록 **각 결정의 _왜_** 와 **반복적으로
나타난 함정**을 정리한다. 시간 순서로 따라가는 절반(스토리)과, 주제별로 정리한 절반
(체크리스트)으로 구성.

---

## 1. 한눈에 보는 타임라인

| 시기 | 단계 | 주요 변화 | 트리거 |
|---|---|---|---|
| 2025-12 | Foundation | `SeoService` 도입, 정적 sitemap.xml 1개 URL, 기본 robots.txt | MVP 출시 (#9) |
| 2025-12 | AdSense push | `ads.txt` 명시 엔드포인트, GTM, 소유권 인증 메타 | 광고 수익화 (#10, #15) |
| 2025-12 | PWA | manifest.json, apple-touch-icon | 모바일 |
| 2026-01 | **위기 1** — HTTP/HTTPS 혼재 | robots/sitemap/yaml 모두 https로 통일 | GSC "리디렉션 포함된 페이지" (#18) |
| 2026-01 | 도메인 재구성 | common/datedate/runner 모듈 분리 → SEO 클래스 위치 이동 | 아키텍처 정리 (#17) |
| 2026-01 | 동적 sitemap | 정적 XML → `SitemapService` (Java 생성) | lastmod 자동화 |
| 2026-03 | JSON-LD 확장 | WebApplication 단독 → FAQPage·BreadcrumbList 추가 | 리치 결과 노출 |
| 2026-04 | **콘텐츠 폭발** | sitemap 6→11 페이지, `/guide`·`/use-cases/*` 4개, HowTo 스키마 | 자체 콘텐츠 강화 |
| 2026-04 | **위기 2** — robots `Disallow: /*/*` 제거 | UGC만 차단하도록 `/*/20xx/` 패턴 | GSC 색인 실패 |
| 2026-04 | AdSense 거절 회복 | `adsEnabled` 플래그 → 콘텐츠 페이지에만 광고, UGC 페이지 제외 | 정책 위반 회피 |
| 2026-04 | 에러 페이지 + i18n 인프라 | KO/EN messages.properties 신설 (236 keys) | 다국어 시작 |
| 2026-04 | Cache-Control / Content-Language | 공개 SEO 페이지 캐시 헤더 정책 | GSC 색인 미생성 v2 |
| 2026-04 | SEO 고도화 | `LASTMOD` 상수 도입, Runner Run 상세 sitemap 자동 등록 | 운영 안정 |
| 2026-04 | 연도별 Disallow | `Disallow: /*/20` 광범위 차단 → `2024/`·`2025/`·… 엄격 | Runner 제목 유출 방지 |
| 2026-04 | 로케일 즉시 적용 | `setLocale` 에서 request attribute 캐시 | KO→EN 토글 한 번에 안 먹는 버그 |
| 2026-04 | **i18n SEO 완성** | hreflang + canonical(ko/en) + og:locale + 174 messages keys | EN 검색 시장 진입 |
| 2026-05 (현재) | Sitemap 신뢰도 회복 | `LocalDate.now()` 제거, `BuildProperties` lastmod, ISO 8601, XML escape, `og:locale:alternate`, KO 토글 `rel=nofollow`, naver-site-verification | Google lastmod 신뢰 정책 대응 |

---

## 2. 단계별 세부 결정과 _왜_

### 2.1 Foundation (#9) — 무엇을 미리 갖추었는가

`cdb552e feat: SEO 적용 #9` (2025-12-14):

- `SeoMetadata` record + `SeoService` 도입. **모든 SEO 메타가 컨트롤러에 흩어지는 것을
  애초에 막은** 결정. 이게 없었다면 hreflang/canonical 이중 발행, og 누락 같은 회귀가
  훨씬 잦았을 것이다.
- `StaticResourceController` — robots/sitemap/manifest/favicon 을 명시적 `@GetMapping`
  으로 노출. `/sitemap.xml` 같은 표준 경로가 라우팅 매칭 우선순위에서 밀려나
  `404` 가 되는 사고를 사전에 방지.
- 정적 `sitemap.xml` 단 1줄: `<loc>http://datedate.site/</loc>`. **단순하지만 포함**
  되어 있었다는 사실 자체가 GSC 등록을 가능하게 했다.

**교훈:** 출시 첫 커밋부터 `SeoMetadata` 같은 단일 진실 공급원(SSOT)을 둬라. 나중에
hreflang/og:locale/JSON-LD 가 추가될 때 컨트롤러 수정 없이 한 곳에서 끝난다.

### 2.2 AdSense 도입의 부작용 (#10, #14, #15)

`5b65e53 fix: ads.txt 미식별 문제 해결 #10` 의 커밋 메시지가 핵심:

> 주소로는 접근 가능 / google adsense에서 찾지 못함 / 명시적 엔드포인트 추가

`/static/ads.txt` 가 정적 자원으로는 200 으로 응답하지만 AdSense 크롤러가 인식하지
못했다. 원인: Spring 정적 리소스 핸들러가 `Content-Type: application/octet-stream`
또는 잘못된 캐시 헤더로 응답할 수 있음. **`@GetMapping("/ads.txt", produces=TEXT_PLAIN)`
명시 후 해결.**

`71ab65e refactor: 구글 에드센스 정책 준수 #14` — 인덱스 페이지에 자체 콘텐츠 232줄
추가. AdSense 는 "thin content" 사이트를 거부한다. **랜딩 페이지가 폼 + CTA 만으로
이뤄지면 안 된다**는 교훈. 이후 `/guide`, `/use-cases/*` 도 같은 맥락에서 추가.

**교훈:** AdSense 와 SEO 는 같은 신호를 본다 — "사용자가 머물 만한 콘텐츠가 있는가".
AdSense 거절은 사실상 SEO 약점 알람이다.

### 2.3 위기 1 — HTTP/HTTPS 혼재 (`19f9286`)

GSC "리디렉션이 포함된 페이지" 경고. 원인은 4 곳에서 프로토콜 불일치:

| 위치 | 상태 |
|---|---|
| `robots.txt` | `http://datedate.site/sitemap.xml` |
| `application.yaml` | `http://datedate.site` |
| `SeoService.java` (default) | `http://datedate.site` |
| 정적 `sitemap.xml` | `https://datedate.site/` |

리디렉션 체인:
```
http://www.datedate.site/  →  http://datedate.site/  →  https://datedate.site/
                  (301)                      (301)
```
Google 은 2회 리디렉션을 "색인 부적합" 으로 분류.

**해결**: 4 군데 모두 `https://` 로 통일 + nginx 에서 1회 301 로 직행.
(자세한 절차는 [`docs/troubleshooting/google-search-console-redirect.md`](troubleshooting/google-search-console-redirect.md))

**교훈:**
- baseUrl 은 한 곳에서 정의하고 나머지는 모두 그 한 곳을 참조해야 한다.
- 인프라(nginx) + 코드(Spring) 가 같은 정규화 정책을 공유해야 한다 — 한쪽이라도
  엇나가면 chain redirect 발생.
- 새 도메인 추가 시 `curl -IL https://...` 로 redirect 횟수 1회 확인이 release
  체크리스트 항목이어야 한다.

### 2.4 도메인 재구성의 SEO 부수 효과 (`eedabd5`)

`refactor: 도메인 분리 #17` 에서 `me.singingsandhill.calendar.{datedate,runner,trading,stock,common}` 로
모듈 경계를 그었다. SEO 관점에서 발생한 결과:

- `SeoMetadata` → `common/presentation/dto/`
- `SitemapService`, `StaticResourceController` → `common/application/service/`,
  `common/presentation/controller/`
- 도메인별 `SeoService` 는 각자 패키지에 (예: `datedate.application.service.SeoService`)

이 분리 덕분에 **도메인 추가(예: Runner) 시 SEO 클래스를 새로 만들지 않고
공통 인프라를 그대로 쓸 수 있게** 됐다. `SitemapService` 가 `RunRepository` 를
`@Autowired(required = false)` 로 받는 패턴이 핵심 — 모듈이 빠져 있어도 sitemap 은 동작.

**교훈:** 멀티 도메인 모놀리스에서 SEO 인프라는 _공통 모듈_ 에 두고, 각 도메인은
선택적(optional) 의존성으로 sitemap entry/json-ld 조각만 기여하게 만들어라.

### 2.5 Sitemap 진화 — 정적 → 동적 → 신뢰 가능

3단계로 진화했다:

#### v1: 정적 XML (Dec 2025)
```xml
<urlset>
  <url><loc>http://datedate.site/</loc><lastmod>2025-01-01</lastmod></url>
</urlset>
```
- 손으로 매번 수정. 콘텐츠 추가될 때마다 잊어버림.

#### v2: 동적 + startup time (`7eda6e2`, Jan 2026)
```java
this.startupDate = LocalDate.now();  // ⚠ 함정
```
- 코드에서 5개 URL 생성. 좋아짐.
- 하지만 `LocalDate.now()` 사용 → **재배포할 때마다 lastmod 이 _오늘_ 로 갱신**됨.
  콘텐츠가 안 바뀌어도 신호 발신.

#### v3: 정적 lastmod 상수 + 동적 데이터 (`65a8ff1`, Apr 2026)
```java
private static final LocalDate HOME_LASTMOD = LocalDate.of(2026, 4, 6);
```
- 정적 페이지: 코드 상수, 콘텐츠 변경 시 PR 에 함께 수정
- Runner Run 상세: `run.getCreatedAt().toLocalDate()` 자동
- 인사이트: `LocalDate.now()` (여전히 함정)

#### v4: 신뢰 가능한 lastmod (May 2026, 현재)
- `BuildProperties.getTime()` (배포 시각) → 정적 페이지 lastmod
- `Location/MenuRepository.findLatestActivity()` → 인사이트 페이지 lastmod
- ISO 8601 + KST offset 풀 정밀도
- XML 5 엔티티(`& < > " '`) escape 헬퍼

**교훈:**
- Google 은 2023년부터 "lastmod 신뢰성 낮은 사이트맵 점진적 무시" 정책. `LocalDate.now()`
  같은 거짓 신호는 한두 번 OK 보다 **장기적으로 사이트맵 가치를 깎는다**.
- 정적 lastmod 상수는 좋지만 _수정 망각_ 위험. 빌드 시각/배포 시각 같은 자동화된
  소스가 안전망 역할.
- 사이트맵 출력은 _결정적_ 이어야 한다 — 같은 입력에 같은 출력. 재호출 시마다
  변하면 무언가 잘못됐다는 신호.

### 2.6 robots.txt 의 흥망 — `Disallow: /*/*` 사고

3가지 패턴을 거쳤다:

| 시기 | 패턴 | 결과 |
|---|---|---|
| Dec 2025 | `Disallow: /*/*` | 모든 2-depth URL 차단 |
| Apr 2026 | `Disallow: /*/20` | UGC `/{ownerId}/2026/...` 만 차단 의도 |
| Apr 2026+ | `Disallow: /*/2024/` ~ `/*/2028/` | 연도별 명시 |
| May 2026 | `... ~ /*/2035/` | 향후 10년치 |

`Disallow: /*/*` 는 **GSC 색인 실패의 직접 원인**이었다. `/runners/runs`,
`/runners/members` 같은 정상 페이지까지 다 차단됨.

`Disallow: /*/20` 도 위험: `/runners/2026-recap` 같은 미래 URL 까지 잡음.

**교훈:**
- robots.txt 와일드카드 패턴은 **첫 매치가 아니라 최장 매치 우선** (Google 해석 기준).
  `Allow: /runners/runs` 가 `Disallow: /*/20` 보다 길어야 정상 동작.
- 도메인이 자라면 `Disallow` 패턴이 시한폭탄이 된다. 차라리 **컨트롤러 응답에
  `X-Robots-Tag: noindex` 헤더** 또는 메타 `<meta name="robots" content="noindex">`
  로 페이지별로 제어하는 게 안전.
- robots.txt 에는 character class(`[0-9]`) 가 없다. 연도 차단처럼 패턴이 늘어나는
  케이스는 코드 레벨로 옮겨라.

### 2.7 다국어 SEO — 진정한 분기점 (`c8b970d`)

`refactor: SEO 영문 추가` 는 1612 줄 추가/873 줄 삭제. 단일 커밋으로 KO 전용에서
KO+EN 양방향으로 전환됐다. 핵심 결정:

#### 결정 1: URL 전략 — 쿼리 파라미터
```
KO: https://datedate.site/guide
EN: https://datedate.site/guide?lang=en
```
**왜 path-based(`/en/guide`)가 아니라 쿼리?** 이미 모든 컨트롤러가 단일 path 에 묶여
있었고, 마이그레이션 비용이 너무 컸다. **단점은 인지 중**:
- Googlebot 이 lang 쿼리 파라미터를 무시하고 동일 콘텐츠로 묶을 위험
- GSC URL parameters 도구 deprecated

#### 결정 2: SeoMetadata 확장 — `canonicalKo`, `canonicalEn`, `hreflangEnabled`
```java
public record SeoMetadata(
    String canonical,         // 현재 로케일 self-canonical
    String canonicalKo,       // hreflang ko 대상
    String canonicalEn,       // hreflang en 대상
    String ogLocale,
    boolean hreflangEnabled,  // noindex 페이지에선 false
    ...
)
```
- `canonical = currentCanonical(path)` — 현재 로케일에 따라 ko/en self-canonical
- `canonicalKo`, `canonicalEn` — hreflang 태그용으로 항상 둘 다 채움
- `hreflangEnabled = false` — UGC noindex 페이지에선 hreflang 안 발행 (alt 그룹 충돌 방지)

#### 결정 3: i18n 메시지 174개 추가 → SEO 텍스트 외부화
이전에는 SEO 텍스트가 Java 문자열 리터럴로 박혀 있었다. 영문화하면서 **모든 SEO
메타·JSON-LD 텍스트를 `messages_en.properties` 로 옮김**. JSON-LD 안의 한국어/영어
모두 `messageSource.getMessage("seo.home.appDescription", locale)` 로 해석.

#### 결정 4: bilingual sitemap entries
한 페이지당 sitemap `<url>` 블록 2개 (ko + en) + 각 블록에 xhtml:link rel=alternate 3개
(ko/en/x-default). **이게 Google 이 hreflang 그룹을 인식하는 가장 명확한 신호**.

#### 결정 5: 테스트 기반 i18n 회귀 방지
`SeoServiceI18nTest` 241줄 추가. 모든 페이지에 대해 한/영 양쪽 호출 후:
- `assertThat(en.title()).contains("Schedule")`
- JSON-LD 가 양쪽 모두 valid JSON
- canonical EN 은 `?lang=en` 으로 끝남
- noindex 페이지는 `hreflangEnabled = false`

**교훈:**
- 다국어 SEO 는 _후행_ 작업이 아니라 **canonical 정책 결정 시점에 미리 데이터
  구조에 영입**해야 한다. `SeoMetadata` 가 단일 `canonical` 만 가졌으면 c8b970d 같은
  대수술이 불가능했다.
- SEO 텍스트는 일반 UI 텍스트와 같은 i18n 인프라를 써라. 별도 테이블/별도 파일 만들면
  관리 비용 폭발.
- hreflang 의 핵심 함정은 **noindex 페이지의 hreflang 발행**. 둘이 충돌하면 Google 은
  alt 그룹 인식을 포기한다. `hreflangEnabled` 플래그가 이걸 명시적으로 분리.

### 2.8 로케일 토글이 한 번에 안 먹는 버그 (`e8dde63`)

증상: `?lang=en` 클릭 → 페이지가 KO 로 렌더됨. 새로고침 한 번 더 해야 EN 적용.

원인: Spring `LocaleChangeInterceptor` 는 `preHandle` 에서 `setLocale(en)` 호출 →
쿠키 응답 헤더에만 추가됨. 하지만 같은 요청 처리 중인 컨트롤러는 `LocaleResolver.resolveLocale()`
호출 시 _요청 들어왔을 때의 쿠키_ (= ko) 를 다시 읽음. 결과: 쿠키는 EN 으로 바뀌지만
이번 응답은 KO.

해결:
```java
public void setLocale(...) {
    request.setAttribute(CURRENT_LOCALE_ATTR, resolved);  // 같은 요청 내 캐시
    response.addCookie(...);  // 다음 요청부터
}

public Locale resolveLocale(...) {
    Object cached = request.getAttribute(CURRENT_LOCALE_ATTR);
    if (cached instanceof Locale l) return l;  // 0순위
    // 1. cookie / 2. Accept-Language / 3. ko fallback
}
```

**교훈:** `LocaleResolver` 구현은 항상 _요청 내 setLocale 결과 우선_ 을 first 분기로
넣어라. 안 넣으면 lang 토글이 새로고침 한 번 더 필요한 UX 가 나온다.

### 2.9 가장 최근 (May 2026) — 신뢰성 회복

세 가지 축으로 진행:

1. **Sitemap 신뢰도** (위 v4 참조)
2. **검색엔진별 다양성**: `naver-site-verification` 메타 + `application.yaml` 의 환경변수 주입
3. **canonical noise 제거**: KO 토글 `rel="nofollow"`, EN 토글 `rel="alternate" hreflang="en"`

`og:locale:alternate` 도 추가 — SNS 공유 시 다른 언어 버전 알림 신호.

---

## 3. 다국어 SEO 플레이북 (next project 적용용)

### 3.1 의사결정 트리

```
다국어 출시 예정인가?
├─ Yes → 첫 커밋부터 SeoMetadata 에 canonicalKo/canonicalEn/hreflangEnabled 필드 두기
│        URL 전략은:
│        ├─ 트래픽 기대 큼 + 새 프로젝트  →  /en/path  (서브패스)
│        ├─ 기존 라우팅 마이그레이션 비용 큼  →  ?lang=en  (쿼리, 단점 인지)
│        └─ 도메인 분리 가능  →  en.example.com  (서브도메인)
└─ No  → 단일 canonical 로 시작. 단, SeoMetadata 는 record 로 — 나중에 필드 추가가 쉬움.
```

### 3.2 hreflang 발행 규칙

| 페이지 종류 | hreflang | canonical | sitemap |
|---|---|---|---|
| 공개 색인 페이지 (홈, 가이드, 콘텐츠) | ko + en + x-default | self per locale | bilingual entries |
| UGC noindex (대시보드, 사용자 페이지) | **없음** | KO only | **제외** |
| 어드민/내부 | 없음 | 없음 | 제외 |

UGC 에 hreflang 을 발행하면 Google 이 alt 그룹 인식을 실패한다 (noindex 와 충돌).

### 3.3 로케일 결정 우선순위 (서버사이드 i18n)

1. **현재 요청에서 setLocale 호출됨?** (lang 토글 클릭 시) → 그 값 사용
2. cookie `lang`
3. Accept-Language 헤더
4. 기본값 (한국 시장이면 ko)

1번 누락이 가장 흔한 버그. 토글 클릭이 한 번에 안 먹는 증상의 원인.

### 3.4 sitemap 다국어 패턴

```xml
<url>
  <loc>https://example.com/guide</loc>            <!-- KO self -->
  <lastmod>2026-05-01T11:20:06+09:00</lastmod>
  <xhtml:link rel="alternate" hreflang="ko" href="https://example.com/guide"/>
  <xhtml:link rel="alternate" hreflang="en" href="https://example.com/guide?lang=en"/>
  <xhtml:link rel="alternate" hreflang="x-default" href="https://example.com/guide"/>
</url>
<url>
  <loc>https://example.com/guide?lang=en</loc>    <!-- EN self -->
  <!-- 같은 hreflang 3개 (return tag) -->
</url>
```

핵심: **각 alt 블록은 자기 자신을 포함한 _전체_ hreflang 을 발행**해야 reciprocal 검증 통과.

---

## 4. 사이트맵 성숙도 모델

| Level | 특징 | DateDate 시점 |
|---|---|---|
| L0 | 정적 1줄 sitemap, 손으로 갱신 | Dec 2025 |
| L1 | 동적 생성, 모든 공개 URL 포함 | Jan 2026 (`SitemapService` 도입) |
| L2 | lastmod 정확도 (콘텐츠 변경과 동기화) | Apr 2026 (`*_LASTMOD` 상수) |
| L3 | 다국어 alt 엔트리 (xhtml:link) | Apr 2026 (c8b970d) |
| L4 | **결정적 + 신뢰 가능 lastmod** (재호출 안정) | May 2026 (현재) |
| L5 | sitemap-index, image/video sitemap 분리 | (미래, URL 1000+ 시) |

L0→L1 전환은 비교적 쉽다. L3→L4 가 가장 어렵다 — `LocalDate.now()` 같은 함정을
인지하고 _데이터 변경 추적_ 인프라(`findLatestActivity()` 등)를 갖춰야 한다.

---

## 5. JSON-LD 진화

| 시기 | 추가된 스키마 | 효과 |
|---|---|---|
| Dec 2025 | `WebApplication` (홈) | 앱 카테고리 인식 |
| Mar 2026 | `FAQPage` (홈에 6 Q&A 인라인) | FAQ rich result 노출 가능 |
| Apr 2026 | `Organization`, `BreadcrumbList`, `HowTo` (가이드) | 사이트 구조 + 단계별 가이드 인식 |
| Apr 2026 | `WebSite` + `inLanguage: ["ko-KR","en-US"]` | 다국어 사이트 명시 |
| (미래 후보) | `WebSite` + `SearchAction` (사이트링크 검색박스) | 도메인 검색박스 |
| (미래 후보) | `Event` (일정 페이지) | 일정 noindex 라 우선순위 낮음 |

**교훈:** JSON-LD 는 점진적으로 추가. 처음부터 모든 스키마 욕심내지 말고 _콘텐츠
유형이 명확해진 페이지부터_ 추가해라. JSON 유효성은 자동 테스트(JsonReader)로
검증 — 한 번 invalid 발행하면 Google 이 그 사이트의 schema 신뢰도를 낮춘다.

---

## 6. AdSense + SEO 통합 교훈

1. **AdSense 거절 = SEO 약점 알람**: thin content, 정책 페이지 누락, UGC 위주는 둘 다 타격.
2. **광고 스크립트는 페이지 종류별로 분기**: `SeoMetadata.adsEnabled` 플래그로
   콘텐츠 페이지에만 로드. UGC/대시보드/admin 에는 미로드 → AdSense 정책 위반 회피
   + Core Web Vitals(LCP/CLS) 보호.
3. **`ads.txt` 는 명시적 컨트롤러로**: 정적 리소스 핸들러는 잘못된 Content-Type 줄
   수 있음. AdSense 가 못 찾으면 수익화 전체 막힘.

---

## 7. 트러블슈팅 컴펜디움

### Case 1: GSC "리디렉션 포함된 페이지"
- **증상**: Search Console → 색인 생성 → 페이지 → "리디렉션이 포함된 페이지" 경고
- **원인**: HTTP/HTTPS 혼재 + www 서브도메인 → 2회+ 301 chain
- **점검**: `curl -IL http://www.example.com/` — 301 횟수 1회여야 함
- **해결**: robots/sitemap/yaml/SeoService default 모두 https 통일 + nginx 1회 redirect
- **참고**: `docs/troubleshooting/google-search-console-redirect.md`

### Case 2: GSC "색인이 생성되지 않음 — robots.txt에 의해 차단됨"
- **증상**: 정상 페이지 (`/runners/runs`) 가 색인 안 됨
- **원인**: `Disallow: /*/*` 가 너무 광범위 → 모든 2-depth URL 차단
- **점검**: GSC URL 검사 도구 → "robots.txt에 의해 차단됨" 표시 확인
- **해결**: 패턴 좁히기 (`/*/2024/` 등 명시) 또는 메타 noindex 로 페이지별 제어

### Case 3: AdSense 가 ads.txt 를 찾지 못함
- **증상**: AdSense 대시보드 "Ads.txt 파일을 발견하지 못함"
- **원인**: 정적 리소스 핸들러가 잘못된 MIME 또는 캐시 헤더 응답
- **점검**: `curl -I https://example.com/ads.txt` — `Content-Type: text/plain` 확인
- **해결**: `@GetMapping(value="/ads.txt", produces=TEXT_PLAIN_VALUE)` 명시

### Case 4: 일정 페이지 타이틀에 천 단위 쉼표 (`2,026/5 Schedule`)
- **증상**: `<title>2,026/5 Schedule - test | DateDate</title>`
- **원인**: Spring `MessageFormat` 의 `{1}` placeholder + 인자 `int year` →
  Number 자동 NumberFormat → 그룹 구분자 적용
- **점검**: 메시지 키에서 `{N}` 다음에 `number,#` 가 있는지
- **해결**: `{1,number,#}` 로 그룹 비활성화. 또는 String 으로 변환해서 전달.

### Case 5: 언어 토글이 한 번에 안 먹음
- **증상**: KO → EN 클릭 → 페이지는 KO 그대로. 새로고침 후 EN.
- **원인**: `LocaleResolver.resolveLocale()` 이 응답 처리 중 _요청 시점_ 쿠키만 봄
- **점검**: `setLocale` 후 같은 요청에서 `resolveLocale` 호출 시 결과 확인
- **해결**: `request.setAttribute()` 로 캐시, `resolveLocale` 의 0번 분기에서 우선 반환

### Case 6: sitemap lastmod 이 매일 변경되는데 콘텐츠는 그대로
- **증상**: GSC sitemap 리포트 신뢰도 점진 하락, 색인 갱신 빈도 감소
- **원인**: `LocalDate.now()` 같은 거짓 신호
- **점검**: 같은 sitemap 두 번 호출해서 diff. 차이가 lastmod 만이면 의심.
- **해결**: 콘텐츠/데이터 변경 시점을 추적하는 SoT 만들고 거기서 lastmod 산출.
  fallback 은 `BuildProperties.getTime()` (배포 시각).

### Case 7: hreflang return tag 누락 경고
- **증상**: GSC International Targeting → "No return tag" 오류
- **원인**: A 페이지가 B 를 alternate 로 가리키는데 B 는 A 를 안 가리킴
- **점검**: 양쪽 페이지의 `<link rel="alternate" hreflang="..."` 비교
- **해결**: 모든 alt 블록은 _자기 자신 포함_ 전체 hreflang 발행 (sitemap, head 둘 다)

### Case 8: WSL/Windows curl 로 localhost 서버에 연결 불가
- **증상**: `curl http://localhost:8081/` → Connection refused. 그런데 Windows
  cmd 에서는 응답.
- **원인**: `cmd.exe /c "...gradlew.bat"` 로 실행한 Spring Boot 가 Windows 호스트
  쪽 0.0.0.0/loopback 에 바인드. WSL 의 localhost 는 Windows 와 분리됨.
- **점검**: WSL 에서 `cmd.exe /c "curl -I http://localhost:8081/"` 로 우회 확인
- **해결**: 개발 시에만의 문제. 배포 환경에서는 무관. 단, dev 검증은 cmd.exe 통과
  필요.

---

## 8. 자주 본 안티패턴

1. **canonical 한 곳만 두기**: `SeoMetadata.canonical` 만 두면 다국어 추가 시 대수술.
   처음부터 `canonicalKo`, `canonicalEn` 분리.
2. **robots.txt 와일드카드 광범위 차단**: `/*/*` 같은 광역 패턴 → 정상 페이지까지 막음.
   페이지별 noindex 메타가 더 안전.
3. **sitemap lastmod 에 `LocalDate.now()`**: 매번 변경되는 신호 = Google 신뢰도 하락.
4. **JSON-LD 안에 한국어 리터럴 박기**: 영문화 시 SeoService 전체 재작성. 처음부터
   MessageSource 키로 외부화.
5. **`<title>` 에 직접 `int` 인자 전달**: MessageFormat 자동 NumberFormat → 천단위
   쉼표. `{N,number,#}` 또는 String 변환.
6. **로케일 변경 후 같은 요청에 적용 안 함**: `setLocale` 결과를 request attribute
   캐시 안 하면 토글 한 번 더 필요한 UX.
7. **og:image 단일 언어**: 같은 이미지를 KO/EN 모두 사용 → CTR 손해. 큰 비용 아님 —
   배포 전에 처음부터 둘 다 만들어둬라.
8. **광고 스크립트 모든 페이지 로드**: AdSense 정책 위반 + LCP 악화. `adsEnabled`
   플래그로 콘텐츠 페이지만.
9. **manifest.json 단일 언어**: PWA 설치 시 description 이 한 언어로만. 동적 생성
   라우트로 분리.
10. **연도별 disallow 갱신 망각**: `/*/2024/` ~ `/*/2028/` 만 적었다가 2029 가 되면
    UGC 노출. 향후 7~10년치 미리 또는 코드 레벨 noindex 헤더로 이동.

---

## 9. 다음 프로젝트 출시 전 SEO 체크리스트

### 코드 단계 (D-30)
- [ ] `SeoMetadata` record/class 도입, `canonical`/`canonicalKo`/`canonicalEn` 분리
- [ ] `SeoService` (또는 동등 abstraction) 로 SEO 텍스트 단일 진실 공급
- [ ] 모든 SEO 텍스트는 `messages.properties` / `messages_en.properties` 외부화
- [ ] `LocaleResolver` 의 0번 분기에 _요청 내 캐시_ 우선 반환 로직
- [ ] `MessageFormat` 인자에 `int year` 같은 number 직접 전달 시 `{N,number,#}` 또는
      String 변환 — 천단위 쉼표 방지
- [ ] `SitemapService` 동적 생성, lastmod SoT 구축 (`BuildProperties` + 데이터
      `findLatestActivity()`)
- [ ] sitemap XML escape 헬퍼 + 모든 `<loc>`/`href` 적용
- [ ] sitemap, robots, application.yaml, SeoService default 모두 `https://` 통일
- [ ] UGC 페이지 `<meta name="robots" content="noindex">` + sitemap 제외 + hreflang
      미발행

### 인프라 단계 (D-14)
- [ ] nginx (또는 동등) 에서 www → non-www, http → https 1회 301 chain
- [ ] `curl -IL https://www.example.com/` 로 redirect 횟수 1회 확인
- [ ] HSTS 헤더 (`Strict-Transport-Security`) 설정
- [ ] `Vary: cookie, accept-language, accept-encoding` 헤더 — CDN 캐시 분리
- [ ] `Content-Language` 응답 헤더 (LocaleResolver 결과 기반)
- [ ] gzip/brotli 압축 (sitemap, html, css, js)

### 외부 등록 단계 (D-7)
- [ ] Google Search Console 도메인 속성(DNS TXT) 등록 — URL 속성보다 통합 관리
- [ ] GSC sitemap.xml 제출 → "Success" 확인 후 색인 요청
- [ ] 네이버 서치어드바이저 사이트 등록 + 사이트맵 제출 (한국 시장이면 필수)
- [ ] Bing Webmaster (GSC 에서 1-click import)
- [ ] AdSense 사용 예정이면: ads.txt 명시 컨트롤러, 콘텐츠 페이지 충분 확인,
      정책 페이지(privacy/terms) 사전 작성
- [ ] `<meta name="naver-site-verification">`, `google-site-verification` 토큰 환경
      변수로 주입

### 출시 후 (D+7)
- [ ] GSC URL 검사 도구로 홈/주요 페이지 색인 상태 확인
- [ ] GSC International Targeting 리포트에서 hreflang return tag 오류 없는지
- [ ] sitemap의 모든 URL 200 응답하는지 자동 검증 테스트
- [ ] Core Web Vitals (LCP < 2.5s, CLS < 0.1, INP < 200ms) GSC Page Experience 모니터링
- [ ] AdSense 적용 시: 광고 노출 페이지 LCP 악화 여부 확인

### 회귀 방지 (지속)
- [ ] `SitemapServiceHreflangTest` 같은 sitemap 회귀 테스트 — XML 유효성, 결정성,
      hreflang reciprocal, lastmod 형식
- [ ] `SeoServiceI18nTest` 같은 SEO i18n 테스트 — 모든 페이지 한/영 양쪽 호출,
      JSON-LD 유효성
- [ ] CI 에서 `messages_en.properties` 키 누락 검출 (KO 키 대비)
- [ ] 매월 GSC 리포트 리뷰 — 새 색인 오류 패턴 발견 시 troubleshooting 문서 추가

---

## 10. 참고 문서

- [Google Search Console 색인 리포트 가이드](https://support.google.com/webmasters/answer/7440203)
- [Google Search Central — Sitemaps](https://developers.google.com/search/docs/crawling-indexing/sitemaps/overview)
- [Google Search Central — hreflang](https://developers.google.com/search/docs/specialized/international/managing-multi-regional-sites)
- [네이버 서치어드바이저 도움말](https://searchadvisor.naver.com/guide)
- [Schema.org 스키마 카탈로그](https://schema.org/docs/full.html)
- [`docs/troubleshooting/google-search-console-redirect.md`](troubleshooting/google-search-console-redirect.md) — HTTPS 통일 사례
- [`docs/troubleshooting/nginx-configuration.md`](troubleshooting/nginx-configuration.md) — nginx redirect 설정

---

## 11. 변경 이력 요약 (인용 가능한 commit hash)

```
cdb552e  2025-12-14  SEO 적용 #9 — 인프라 시작점
6994b9e  2025-12-14  Google AdSense 적용 #10
ee59e3b  2025-12-14  AdSense 소유권 인증 #10
5b65e53  2025-12-20  ads.txt 명시 엔드포인트 — Case 3
b9ece5c  2025-12-26  GTM 설치 #15
868e8ba  2025-12-21  PWA (manifest, apple-touch-icon)
71ab65e  2025-12-19  AdSense 정책 준수 — 콘텐츠 강화 #14
eedabd5  2026-01-01  도메인 분리 #17 — common 모듈 신설
19f9286  2026-01-06  HTTPS 통일 #18 — Case 1
7eda6e2  2026-01-06  SEO 최적화 #9 — 동적 sitemap (v2)
c7ddfe8  2026-03-15  FAQ JSON-LD + 정적 sitemap 제거
69b9919  2026-04-07  콘텐츠 페이지 확장, robots /*/* 제거 — Case 2
6535183  2026-04-07  AdSense 거절 회복 — adsEnabled 플래그
af3d212  2026-04-13  에러 페이지 + i18n 인프라 (messages 첫 등장)
34335db  2026-04-14  GSC 색인 미생성 #18 — Cache-Control + Content-Language
65a8ff1  2026-04-16  SEO 고도화 — sitemap v3 (LASTMOD 상수)
3d2a37a  2026-04-17  연도별 robots disallow
e8dde63  2026-04-19  로케일 즉시 적용 — Case 5
c8b970d  2026-04-28  SEO 영문 추가 — i18n SEO 완성 (1612+/873-)
(uncommitted)        sitemap v4 + og:locale:alternate + naver verification — Cases 4, 6
```
