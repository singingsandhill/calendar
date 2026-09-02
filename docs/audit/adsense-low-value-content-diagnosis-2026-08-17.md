# AdSense "낮은 가치 콘텐츠" 3차 통지 — 진단 + 대응 계획 (2026-08-17)

| 항목 | 값 |
|---|---|
| 작성일 | 2026-08-18 |
| 통지 | "가치가 별로 없는 콘텐츠 (Low value content)" — 2026-08-17, **사용자가 제출한 재검토 요청의 결과**. 사이트 상태는 "승인됨 + 주의 필요" (정책 센터 위반 플래그 병존) |
| 사이트 | datedate.site (Spring Boot 4 / Thymeleaf, `datedate` 모듈) |
| 선행 문서 | [정책 매핑 감사 2026-05-05](adsense-low-value-content-policy-mapping.md) · [2차 조치 보고 2026-06-11](../seo/adsense-low-value-content-remediation.md) · [Lean Strengthen 설계 2026-06-20](../superpowers/specs/2026-06-20-datedate-adsense-lean-strengthen-design.md) · [SEO 전수 점검 2026-08-16](seo-page-audit-2026-08-16.md) · [실행 계획](../prompts/adsense-approval.md) |
| 본 문서 목적 | 3차 통지의 원인 확정, 이번 조치(기술 수정) 기록, 콘텐츠 레이어 후속 로드맵과 재검토 요청 규칙 수립 |
| 조사 방법 | ① 라이브 실측 (2026-08-18: sitemap/robots/head 메타/페이지별 본문 분량 curl+수동) ② 저장소 이력 전수 (감사/스펙/ADR/커밋 로그) ③ 코드 인벤토리 (SitemapService/SeoService/템플릿/i18n 카탈로그 866키 전수) |

---

## 1. 한 줄 결론

**기술 SEO 는 이번에도 원인이 아니다.** 2026-06-20 설계 문서의 진단("The flag is editorial")이
그대로 유효한 상태에서 — 즉 준비돼 있던 escalation(장문 `/guides` 허브)을 실행하지 않은,
6-11 플래그 시점과 사실상 동일한 콘텐츠 믹스로 — 재검토를 요청했고, 같은 판정을 받았다.
**해법은 콘텐츠 레이어이며, 그 전까지 재검토 요청을 다시 제출하지 않는다 (§6).**

## 2. 타임라인 (3 라운드)

| 날짜 | 사건 |
|---|---|
| 2025-12-19 | 거절 #0 (thin content) → `/guide` + use-case 4페이지 신설 (ADR common/seo/0007) |
| 2026-05-02 | **1차 "낮은 가치 콘텐츠" 거절** → Phase A 인프라 + Tier 1/2 콘텐츠 보강 |
| 2026-06-11 | **2차 플래그** → Section P (privacy/terms i18n, owner 404, guide FAQ) + 회귀 가드 6종 |
| 2026-06-29 | Lean Strengthen A–E (club-activity 슬러그, worked example, 홈 에디토리얼) |
| 2026-08-02 | 배포 정합 확인 — 사이트맵 26 URL 전부 200/index ([감사](sitemap-audit-2026-08-02.md)) |
| 2026-08-16/17 | SEO 전수 점검 + 홈 adsEnabled(false) — 커밋 로그 135~155 로 준비 (통지 시점 미배포 추정) |
| 2026-08-17 | **3차 통지** — 재검토 요청 결과로 "승인됨 + 주의 필요(낮은 가치 콘텐츠)" |

## 3. 원인 진단

### 3-1. 본질 원인 — 콘텐츠 믹스 (editorial)

라이브 실측(2026-08-18) 기준 색인 표면은 **13개 고유 페이지 × ko/en = 26 URL** 이 전부다.

| 페이지 | 프로즈 분량 (EN 단어 기준) | 성격 |
|---|---|---|
| `/` | ~950 | 서비스 소개 + 시나리오 + FAQ |
| `/guide` | ~700 | 사용 절차 가이드 |
| `/about` | ~470 | 서비스/운영 소개 |
| `/faq` | ~480 (8문항) | 서비스 FAQ |
| `/privacy` · `/terms` | ~400 각 | 정책 |
| `/tools/date-diff` | ~510 + JS 계산기 | 색인 표면에서 유일하게 서비스 자기서술이 아닌 페이지 |
| `/insights/trends` | ~320 + 데이터 테이블 | 실데이터 기반 |
| `/use-cases/{slug}` ×5 | 각 885~962 (슬러그 고유분 83~85%) | **한 템플릿(`use-cases/detail.html`)이 5 URL 생산** |

- 총 프로즈 ≈ 8,900 EN 단어 / 51,000 KO 자. 콘텐츠 자체는 성실하고(기계 생성·스핀 아님,
  en 완역) 개별 페이지가 극단적으로 얇지도 않다.
- 그러나 리뷰어 관점의 구성이 문제다: **13페이지 중 11페이지가 DateDate 자기서술**
  (소개/가이드/FAQ/정책/유즈케이스)이고, 독립적 정보 가치를 주는 페이지는 date-diff 하나.
  9개 색인 템플릿 중 1개가 5 URL 을 찍어내는 구조는 "같은 사이트 안에서 복제된 템플릿 페이지"
  신호와 "원본 콘텐츠 부족" 신호를 동시에 만족시킨다 — 2026-06-20 설계 문서의 진단 그대로.

### 3-2. 촉발 요인 — escalation 미실행 상태의 재검토 요청

실행 계획(adsense-approval.md)의 Phase C(슬러그 4개)/D(`/guides` 허브)/E(FAQ 확장)는 전부
미착수였고, 설계 문서 §7.4/§8 은 "재거절 시 /guides 허브로 escalate" 를 명시해 두었다.
6-29 Lean Strengthen 이후 색인 콘텐츠에 실질 변화 없이 재검토가 제출됐다.

### 3-3. 부수 신호 (이번 조치로 봉합 — §4)

- **insights 부분 데이터 창**: `hasData` 에 `totalSchedules > 0` 팔이 있어, 일정만 있고
  투표가 없으면 0 값 카드 6개 + 빈 상태 2개뿐인 본문이 색인·광고 대상이었다
  (사이트맵 등재 조건은 장소/메뉴 활동만 인정 — 코드 내부 불일치).
- **소프트 404**: 미지 use-case 슬러그가 홈으로 302.
- **허브 부재**: `/use-cases`, `/tools` 가 permitAll 인데 컨트롤러가 없어 404 — 내부 링크
  구조상 doorway 형 평면 구조.
- **Vary 누락**: `/about` `/faq` `/tools/date-diff` 는 로케일 적응형인데
  `Vary: Cookie, Accept-Language` 미적용 (공유 캐시의 언어 오염 가능).
- **내부 중복**: 홈 FAQ(6) ↔ `/faq`(8) 주제 5개 중복 + FAQPage JSON-LD 이중 발행,
  use-case 제목·설명이 홈/guide/상호링크/푸터 4곳 반복, guide 트러블슈팅(6)이 `/faq` 4항목 재진술.

### 3-4. 기각된 가설

- canonical / hreflang / x-default / robots.txt / 사이트맵 구조 — 라이브 실측 정상
  ([2026-08-16 점검](seo-page-audit-2026-08-16.md)과 일치).
- 광고 과다 — `adsense.client` 공란이라 `adsbygoogle.js` 는 운영에서 로드된 적 없음
  (실측 0건; ads.txt 만 서빙 중).
- en 번역 품질 — messages 카탈로그 866키 완전 패리티, en 자연스러움 실측 확인.

## 4. 이번 조치 (2026-08-18, 기술 수정 — 콘텐츠 집필은 §5)

| # | 조치 | 파일 | 테스트 (RED→GREEN) |
|---|---|---|---|
| 1 | `/about` `/faq` `/tools/date-diff` 에 `Vary: Cookie, Accept-Language` + public 캐시 적용 | `WebConfig` 경로 집합 | `PublicSeoCacheHeadersTest` (신설, 5경로 파라미터라이즈) |
| 2 | 미지 use-case 슬러그 302→**HTTP 404** (`UseCaseNotFoundException`, ADR datedate/domain/0008) | `UseCaseController`, 신규 예외 | `UseCaseLocaleRenderingTest.unknownSlug404` (302 고정 테스트를 404 로 전환) |
| 3 | insights `hasData` 를 사이트맵 조건과 일치화 (`totalSchedules` 팔 제거) | `InsightsController` | `InsightsPartialDataIndexingTest` (신설, 부분 데이터 → noindex) |
| 4 | `/use-cases` 허브 인덱스 페이지 신설 (카드 그리드 + 인트로, 사이트맵/robots 등재) | `UseCaseController`, `SeoService`, `SitemapService`, 신규 템플릿, messages | `SitemapServiceWhitelistTest` 확장 등 |
| 5 | `GET /tools` → `/tools/date-diff` 영구 리다이렉트(308, 기존 `/insights` 루트 패턴 미러) — 단일 도구라 인덱스 페이지는 그 자체가 thin | `HomeController` | 컨트롤러 테스트 |
| 6 | 홈 FAQPage JSON-LD 발행 중단 — FAQPage 는 `/faq` 단독 (구조화 데이터 중복 해소; 본문은 불변) | `SeoService.getHomeSeo` | `SeoServiceI18nTest` |

## 5. 후속 로드맵 — 콘텐츠 레이어 (별도 세션, 재검토 요청의 전제 조건)

> **2026-08-23 실행 완료:** §5-1 의 /guides 허브 + 기사 4편이 구현됐다 (사용자 확정: 기사별
> 전용 템플릿·표시명 "모임 노하우"·4편 전부). 결정 기록 [ADR common/seo/0011](../adr/common/seo/0011-guides-editorial-hub.md),
> 실행 계획·결과 [plans/2026-08-23-datedate-guides-editorial-hub.md](../superpowers/plans/2026-08-23-datedate-guides-editorial-hub.md),
> 커밋 로그 161~165. 아래 원문은 당시 계획으로 보존.

우선순위 순. ①이 핵심이고 나머지는 지원.

1. **`/guides` 에디토리얼 허브 (Phase D)** — use-case 패턴 미러링(GuideSlugs SSOT →
   GuidesController → `templates/guides/` → SeoService Article JSON-LD → 사이트맵 루프 →
   `GuideContentCompletenessTest`). 기사 4편, 각 1,200~1,800 단어 ko+en (슬러그당 메시지
   ~60~90키 ×2로케일, 카탈로그 총 ~550~750줄/파일 증가). **서비스 홍보가 아닌 검색의도형**
   주제 — DateDate 는 비교 항목의 하나로만 등장:
   - 모임 날짜를 실제로 정하는 법 (선택지 수·마감·정족수·리드타임)
   - 일정 조율 방법 5종 비교 (단톡 투표/스프레드시트/공유캘린더/투표 도구/리더 결정 — 비교표)
   - 약속 에티켓 (응답 기한·취소 통보·리스케줄 규범; ko 는 회식/경조사 등 로컬 맥락으로 병렬 집필)
   - 가능 날짜 투표 잘 굴리는 법 (선택지 가지치기·앵커링·교착 타이브레이크)
   - (5번째 후보 timezone-coordination 은 ko 검색의도 최약 — 후순위)

   **구현 시 함정 2건 (선기록):**
   - `SecurityConfig` 에 2-세그먼트 catch-all 이 없다 (`/*` 와 `/*/*/*` 뿐) —
     `/guides/{slug}` 는 **명시 `requestMatchers("/guides", "/guides/**").permitAll()`** 없이는
     `authenticated()` 로 떨어져 카카오 로그인으로 리다이렉트된다. 익명 접근 테스트 필수.
   - `ReservedOwnerIds` 에 `"guide"` 는 있으나 `"guides"` 가 없다 — 추가하지 않으면 오너가
     `datedate.site/guides` 를 선점할 수 있다.
2. **Phase C use-case 슬러그 — 최대 2개로 축소 권고** (family-gathering, company-dinner).
   동일 템플릿 페이지 증식은 "복제 템플릿" 신호를 키운다 — 원계획 4개 전부는 역효과 위험.
   guides 와 경합 시 **항상 guides 우선**.
3. **`/faq` 8→14~16문항** — 신규 주제만 (데이터 보존/삭제, 투표 공개 범위, 저장 후 수정,
   리캡/공유, 언어 전환, 카카오 로그인 유무 차이, ID 재사용, 단톡 투표와의 차이).
   기계적 25문항 채우기는 지양.
4. **프로즈 중복 축소** — 홈 FAQ 를 상위 3문항으로 축소 + `/faq` 링크, guide 트러블슈팅에서
   `/faq` 재진술 ~4건을 절차 참조형으로 재작성, use-case 상호링크 카드의 설명문 제거(제목만).
5. 슬러그별 OG 이미지, ad-slot 예약 높이 재설계(ADR common/seo/0010 후속 — 광고 재개 전 필수).
6. E-E-A-T `founder`/`sameAs` 는 **보류 유지** — 실체 없는 계정 기재는 그 자체가 더 나쁜 신호
   (2026-08 결정 재확인).

## 6. 재검토 요청 규칙 (중요 — 운영 절차)

1. **콘텐츠 레이어(§5-1 최소 3편) 배포 전에는 재검토를 요청하지 않는다.** 이번이 3라운드다 —
   반복 거절은 요청 버튼 쿨다운을 점점 늘린다. 기술 수정(§4)만으로 editorial 판정이 뒤집힐
   근거는 없다.
2. 배포 후: GSC 사이트맵 재제출 + 신규 URL 색인 요청 → **GSC 커버리지에서 guides 기사가
   "색인 생성됨" 으로 확인될 때까지 대기 (~7일)** → 그 후 정책 센터에서 검토 요청.
3. 심사 대기 중(통상 수일~2주) 색인 페이지의 코드 변경 금지. 요청 중복 제출 금지.
4. **광고는 승인 전까지 계속 OFF** — `adsense.client` 공란 유지 (현행). 재개 절차는
   ADR common/seo/0010 의 체크리스트(예약 높이 → CLS 재실측)를 따른다.

## 7. 배포 후 라이브 검증 체크리스트

```bash
curl -sI https://datedate.site/use-cases            # 200 (허브)
curl -sI https://datedate.site/use-cases/none-such  # 404 (소프트 404 아님)
curl -sI https://datedate.site/tools                # 308 → /tools/date-diff
curl -sI https://datedate.site/about | grep -i vary # Vary: Cookie, Accept-Language
curl -sI https://datedate.site/faq | grep -i vary
curl -sI https://datedate.site/tools/date-diff | grep -i vary
# --- guides 레이어 (2026-08-23 추가) ---
curl -sI https://datedate.site/guides                          # 200 (허브)
curl -sI https://datedate.site/guides/how-to-pick-a-date       # 200 (기사 4편 각각)
curl -sI https://datedate.site/guides/none-such                # 404
curl -sI https://datedate.site/guides | grep -i vary           # Vary: Cookie, Accept-Language
curl -s  https://datedate.site/guides/how-to-pick-a-date | grep -c "\"@type\": \"Article\""  # 1
curl -s  https://datedate.site/sitemap.xml | grep -c "<loc>"   # 36 (18페이지 × ko/en)
curl -s  https://datedate.site/robots.txt | grep -c "Allow: /guides"                          # 2
curl -s  https://datedate.site/ | grep -c adsbygoogle          # 0 (광고 OFF 유지)
curl -s  https://datedate.site/ | grep -c "FAQPage"            # 0 (홈 FAQPage 제거)
curl -s  https://datedate.site/faq | grep -c "FAQPage"         # 1
```

## 8. 기각/보류 판정 (이번에 손대지 않음 — 근거 기록)

| 항목 | 판정 |
|---|---|
| Accept-Language:en 요청이 bare URL 에서 en 본문 + `?lang=en` canonical 을 받는 설계 | 의도된 hreflang 체계 (ADR common/seo/0004) — 플래그 동인 아님 |
| `?lang=ko` 변형이 헤더 토글로 전 페이지에 내부 링크됨 | `rel=nofollow` + canonical 로 이미 봉쇄 — 변경 불요 |
| 정적 페이지 사이트맵 lastmod = 빌드 시각 | ADR common/seo/0003 의 알려진 트레이드오프 — 페이지별 버전 레지스트리는 과설계 |
| `SecurityConfig` 의 `/privacy-policy` 매처 + `ReservedOwnerIds` 항목 | 무해한 데드 엔트리 (실 URL 은 `/privacy`) — 관례상 문서화만, 기회적 삭제 금지 |
| robots.txt 연도 열거 2035 만료, bare `/stock` 미매치 | 2026-08-16 점검 §5 기지 이슈 — 이번 플래그와 무관 |
