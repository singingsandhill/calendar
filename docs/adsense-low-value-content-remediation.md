# AdSense "Low Value Content" 재심사 2차 대응 — 도메인 기반 페이지별 감사 + 잔여 갭 구현

| 항목 | 값 |
|---|---|
| 작성일 | 2026-06-11 |
| 거절 사유 | "가치가 별로 없는 콘텐츠 (Low value content)" — 2026-05-02 |
| 사이트 | datedate.site (Spring Boot 4 / Thymeleaf, `datedate` 모듈) |
| 감사 방법 | 4-에이전트 병렬 감사: repo 라우트/SEO 인프라, 전체 템플릿, i18n 카탈로그, 라이브 도메인 (curl) |
| 선행 문서 | [`docs/audit/adsense-low-value-content-policy-mapping.md`](audit/adsense-low-value-content-policy-mapping.md) (정책 4문서 1:1 매핑), [`docs/adr/common/seo/0007`](adr/common/seo/0007-content-pages-for-adsense.md) |
| 본 문서 목적 | 1차 대응(Section J/K/M) 이후 잔여 리스크의 페이지별 등급 + 본 세션(Section P)의 구현 내역 + 재심사 전 체크리스트 |

> **승인을 보장하지 않습니다.** 본 문서는 정책 신호를 체계적으로 제거한 기록이며,
> 최종 판정은 Google 리뷰어/봇의 재량입니다.

---

## 1. 요약 (Executive Summary)

**1차 대응(2026-05, Section J/K/M)이 대부분의 구조 문제를 이미 해소했습니다.**
감사 결과 사이트는 다음 상태로 확인됨:

- sitemap = 공개 콘텐츠 페이지 12개 × ko/en 만 포함 (UGC/runner/봇 대시보드 완전 제외)
- robots.txt: /api, /h2-console, /runners/admin, /trading, /stock, UGC 연도 경로 차단
- noindex 메타: owner 대시보드, 스케줄 뷰, runners 전체, trading/stock, 데이터 없는 insights
- 광고 코드: `seo.adsEnabled() and adsense.enabled` + 슬롯 ID 존재 시에만 DOM 생성
  (placeholder 광고 DOM 없음), 콘텐츠 페이지 5종에만 게재
- 본문: use-case 4종 ~1,000단어/페이지, 홈/가이드/FAQ/소개/계산기 모두 실질 본문 보유,
  i18n 756키 100% ko/en 패리티

**본 세션(Section P)이 메운 잔여 갭 3개:**

| # | 갭 | 수정 |
|---|---|---|
| 1 | `/privacy`, `/terms` 가 하드코딩 한국어 (`lang="ko"`, i18n 키 0개) — sitemap/hreflang 은 `?lang=en` 영문 대체 페이지를 광고 중 | 두 템플릿을 about.html 패턴으로 i18n 전환 + 충실한 영문 번역 (신규 법적 주장 없음) |
| 2 | `GET /{ownerId}` 가 `getOrCreateOwner` 호출 — 임의 URL 이 **HTTP 200 + Owner row 영속화** (소프트 404 + 봇 발 DB 오염, 라이브 확인) | GET 무생성 + 미존재 owner 는 동일한 빈 대시보드를 **404** 로 렌더 (ADR datedate/domain/0004) |
| 3 | 가이드에 트러블슈팅 부재 | 실제 제품 동작 기반 6문항 아코디언 FAQ (ko/en) |

**배포 블로커 1개:** 라이브 배포가 repo 보다 구버전 — 라이브 `/trading` 에 커밋된
noindex 메타가 없음 (Section M, Commit 32). **재심사 요청 전 반드시 배포** (§7).

---

## 2. 공개 라우트 인벤토리 + 분류

| 라우트 | 컨트롤러 | 색인 상태 | 광고 | 로케일 | 분류 |
|---|---|---|---|---|---|
| `/` | HomeController | sitemap ✓ | ✗ (CTA 전환 우선) | ko+en | 리뷰 안전 콘텐츠 |
| `/guide` | HomeController | sitemap ✓ | leaderboard 1 | ko+en | 리뷰 안전 콘텐츠 |
| `/about` | HomeController | sitemap ✓ | ✗ (신뢰 페이지) | ko+en | 리뷰 안전 콘텐츠 |
| `/faq` | HomeController | sitemap ✓ | leaderboard 1 | ko+en | 리뷰 안전 콘텐츠 |
| `/privacy` | HomeController | sitemap ✓ | ✗ (정책 페이지) | ko+en (**본 세션 EN 추가**) | 리뷰 안전 콘텐츠 |
| `/terms` | HomeController | sitemap ✓ | ✗ (정책 페이지) | ko+en (**본 세션 EN 추가**) | 리뷰 안전 콘텐츠 |
| `/insights/trends` | InsightsController | sitemap 조건부 (데이터 有 시) / 데이터 無 시 noindex | leaderboard+infeed (데이터 有 시만) | ko+en | 리뷰 안전 콘텐츠 |
| `/tools/date-diff` | HomeController | sitemap ✓ | infeed 1 | ko+en | 리뷰 안전 콘텐츠 (도구 + ~600단어 본문) |
| `/use-cases/{slug}` ×4 | UseCaseController | sitemap ✓ | infeed 1 | ko+en | 리뷰 안전 콘텐츠 (슬러그별 5섹션 고유 본문) |
| `/{ownerId}` | OwnerController | noindex,nofollow + **미존재 시 404** (본 세션) | ✗ | — | UGC — 비수익화 |
| `/{ownerId}/{y}/{m}` | ScheduleController | noindex,nofollow + robots 연도 차단 | ✗ | — | UGC — 비수익화 |
| `/runners/**` | RunnerController | noindex,follow (sitemap 제외) | ✗ | ko | 별도 제품 — 색인/광고 격리 |
| `/runners/admin/**` | — | robots 차단 + ROLE_ADMIN | ✗ | — | 내부 |
| `/trading/**`, `/stock/**` | 봇 대시보드 | noindex,nofollow,noarchive + robots 차단 (**라이브 미반영 — §7**) | ✗ | — | 내부 — 색인/광고 격리 |
| `/api/**` | REST | robots 차단 | ✗ | — | API |
| 오류 페이지 (4xx/5xx) | MvcExceptionHandler | noindex | ✗ | ko+en | 알림 화면 — 비수익화 |
| `/sitemap.xml`, `/robots.txt`, `/ads.txt` | StaticResourceController | — | — | — | 인프라 |

## 3. 페이지별 리스크 등급 (수정 후)

| 페이지 | 등급 | 근거 |
|---|---|---|
| `/privacy`, `/terms` | ~~HIGH~~ → **LOW** | (수정 전) hreflang 이 광고한 EN 페이지가 한국어 법적 문서로 응답 — 정합성 위반. (수정 후) 양 로케일 완전 렌더 + 회귀 테스트 고정 |
| `/{ownerId}` (임의 URL) | ~~MED~~ → **LOW** | (수정 전) 무한 200 URL 공간 + GET mutation. (수정 후) 미존재 owner 404 + 무생성 |
| `/insights/trends` | **LOW** | 데이터 없으면 noindex + sitemap 제외 + 광고 미호출 (3중 가드). 트렌드 산정 방식 본문 설명 존재 |
| `/tools/date-diff` | **LOW** | 1차 대응에서 본문 ~600단어 확장 (사용 사례, 영업일 계산 설명, FAQ 5종) |
| `/use-cases/{slug}` ×4 | **LOW** | 슬러그별 intro/시나리오3/실수3/팁4/FAQ5 고유 본문 ~1,000단어, 키 완결성 테스트 고정 |
| `/`, `/guide`, `/faq`, `/about` | **LOW** | 실질 본문 + 서버 렌더 FAQ + 신뢰 페이지(운영 원칙/연락처/최종수정일). 가이드에 트러블슈팅 6문항 추가 |
| `/runners/**` | **LOW** | noindex + sitemap 제외 — 리뷰어가 nav 로 도달 불가 (datedate nav/footer 에 링크 없음) |
| `/trading`, `/stock` | **LOW** (배포 후) | noindex,nofollow,noarchive + robots 차단. 단 **라이브 미배포 상태 — §7 블로커** |

## 4. 광고 게재 가드 요약

- **2중 게이트:** `head.html` 의 AdSense 스크립트는 `seo.adsEnabled() and adsense.enabled`
  일 때만 로드. `seo.adsEnabled()` 는 SeoService 가 페이지 타입별로 결정 (콘텐츠 페이지만 true).
- **슬롯 게이트:** `ad-slot.html` 의 3개 슬롯 (leaderboard/infeed/rectangle) 은
  `adsEnabled AND 해당 슬롯 ID 설정` 시에만 `<ins class="adsbygoogle">` DOM 생성 —
  승인 전 placeholder 광고 DOM 이 노출되지 않음 (1차 거절 원인 중 하나였던 `XXXXXXXXXX` placeholder 제거됨).
- **광고 게재 페이지 (5종):** `/guide`, `/faq`, `/insights/trends`(데이터 有 시),
  `/use-cases/{slug}`, `/tools/date-diff` — 모두 본문 소비 후·CTA 직전 1~2개.
- **광고 금지 페이지:** `/`(홈), `/about`, `/privacy`, `/terms`, UGC 대시보드/스케줄,
  runners 전체, trading/stock, 오류 페이지 — `adsEnabled=false` 로 스크립트 자체 미로드.
- **밀도:** 페이지당 광고 ≤ 2, 본문 대비 항상 소수. sticky/interstitial/overlay 없음.

## 5. 리뷰 안전 URL (sitemap 화이트리스트 = 재심사 제출 대상)

ko 12개 + en 12개 = 24 URL (insights 는 데이터 존재 시):

```
https://datedate.site/            (+?lang=en)
https://datedate.site/guide       (+?lang=en)
https://datedate.site/about       (+?lang=en)
https://datedate.site/faq         (+?lang=en)
https://datedate.site/privacy     (+?lang=en)
https://datedate.site/terms       (+?lang=en)
https://datedate.site/insights/trends (+?lang=en, 데이터 有 시)
https://datedate.site/tools/date-diff (+?lang=en)
https://datedate.site/use-cases/friend-meetup  (+?lang=en)
https://datedate.site/use-cases/team-meeting   (+?lang=en)
https://datedate.site/use-cases/travel-planning (+?lang=en)
https://datedate.site/use-cases/study-group    (+?lang=en)
```

`SitemapServiceWhitelistTest` 가 이 목록과의 **정확 집합 일치** 를 빌드에서 강제.

## 6. noindex / 제외 URL 매핑 (왜 제외인가)

| URL 패턴 | 메커니즘 | 이유 |
|---|---|---|
| `/{ownerId}` 미존재 | **404** (본 세션) + noindex | 존재하지 않는 페이지 — 소프트 404 제거 |
| `/{ownerId}` 존재 | noindex,nofollow 메타 | UGC 개인 대시보드 — 게시자 콘텐츠 아님 |
| `/{ownerId}/{y}/{m}` | noindex,nofollow + robots `/*/20xx/` | UGC — 참여자 이름 등 사적 데이터 |
| `/runners/**` | noindex,follow 메타 (robots 는 default-allow 유지) | 사이트 테마와 무관한 별도 제품 — "off-theme" 신호 회피는 noindex 로 처리 |
| `/runners/admin/**` | robots 차단 + ROLE_ADMIN 인증 | 내부 관리 |
| `/trading/**`, `/stock/**` | noindex,nofollow,noarchive + robots 차단 | 개인 봇 대시보드 (실계좌 데이터) — 공개 유지하되 색인/리뷰 동선에서 격리 (사용자 결정: 인증 미적용) |
| `/api/**`, `/h2-console/**` | robots 차단 | API/개발 도구 |
| 오류 페이지 | noindex | 알림 화면 |

## 7. 라이브 ↔ repo 불일치 (CRITICAL — 배포 블로커)

라이브 감사 (2026-06-11) 에서 **라이브 배포가 repo HEAD 보다 구버전** 으로 확인:

- 라이브 `/trading` HTML 에 robots 메타 자체가 없음 (repo 는 Section M Commit 32 에서
  noindex,nofollow,noarchive 추가 완료) — `<title>ADA Trading Bot</title>` 이 그대로 노출.
- 본 세션의 수정 3종 (privacy/terms EN, owner 404, 가이드 트러블슈팅) 도 당연히 미반영.

**재심사 요청 전 배포 필수.** 배포 후 스폿 체크 (PowerShell/cmd):

```
curl.exe -s https://datedate.site/trading | findstr /C:"name=\"robots\""        # noindex 1줄
curl.exe -s https://datedate.site/stock   | findstr /C:"name=\"robots\""        # noindex 1줄
curl.exe -s "https://datedate.site/privacy?lang=en" | findstr /C:"Information We Collect"  # 1줄 이상
curl.exe -s "https://datedate.site/terms?lang=en"   | findstr /C:"Service Overview"        # 1줄 이상
curl.exe -s -o NUL -w "%{http_code}" https://datedate.site/zz-no-such-page-xq9  # 404
curl.exe -s https://datedate.site/guide | findstr /C:"guide-faq-1"              # 1줄 이상
curl.exe -s https://datedate.site/ | findstr /C:"adsbygoogle.js"                # 0줄 (승인 전)
```

## 8. 재심사 전 수동 체크리스트

1. **배포** — Section P 커밋 실행 (`docs/git_commit.md` Section P) 후 운영 반영. §7 스폿 체크 통과 확인.
2. **GSC** — sitemap.xml 재제출. `/privacy?lang=en`, `/terms?lang=en` URL 검사로 영문 렌더 확인.
   "발견됨 - 현재 색인되지 않음" 상태의 임의 ownerId URL 들이 404 로 전환되는지 수 주 관찰.
3. **AdSense** — 사이트 대시보드에서 재심사 요청. 광고 단위는 승인 후 슬롯 ID 환경변수
   (`ADSENSE_SLOT_*`) 주입 전까지 DOM 미생성 상태 유지 (현재 정상).
4. **모니터링** — 재심사 기간 중 콘텐츠 변경 자제. insights 데이터가 비면 자동으로
   sitemap 제외 + noindex 되므로 별도 조치 불필요.

## 9. 회귀 보호 (본 세션 추가 테스트 6종)

| 테스트 | 고정하는 불변식 |
|---|---|
| `MessageCatalogParityTest` | ko/en 카탈로그 키 집합 일치 + 빈 값 금지 — "영문 페이지에 한국어 노출" 사고 차단 |
| `TemplatePlaceholderHygieneTest` | 템플릿/카탈로그에 TODO·lorem·coming soon·준비 중·공사 중 금지 — 미완성 화면 신호 차단 |
| `UseCaseContentCompletenessTest` | 4 슬러그 × 5섹션 전체 키 패밀리 양 로케일 존재 — 섹션 조용한 숨김(thin 화) 차단 |
| `SitemapServiceWhitelistTest` | sitemap = 화이트리스트 정확 일치 — UGC/runners/trading/stock/api 누출 차단 |
| `OwnerDashboard404IntegrationTest` | 미존재 owner 404 + 뷰 렌더 + row 미생성 / 존재 owner 200 |
| `PolicyPagesLocaleRenderingTest` | privacy/terms 양 로케일 본문 렌더 + html lang 정합 + 가이드 트러블슈팅 렌더 |
