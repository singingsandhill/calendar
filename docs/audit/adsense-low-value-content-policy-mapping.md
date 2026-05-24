# AdSense "낮은 가치 콘텐츠" 거절 — Google 정책 4개 문서 매핑 감사

| 항목 | 값 |
|---|---|
| 작성일 | 2026-05-05 |
| 거절 사유 | "가치가 별로 없는 콘텐츠 (Low value content)" — 2026-05-02 |
| 사이트 | datedate.site (Spring Boot 4 / Thymeleaf, `datedate` 모듈) |
| 기존 후속 문서 | `docs/prompt/adsense_approval.md`, `docs/adr/common/seo/0007-content-pages-for-adsense.md` |
| 본 문서 목적 | Google 정책 4개 문서의 **구체 조항** ↔ **코드/콘텐츠 증거** 1:1 매핑. 기존 실행 계획의 미반영 항목 보완. |

이 문서는 다음 4개 Google 자료를 직접 읽고 인용한 위에 작성됐습니다.

| # | 짧은 ID | 제목 | 본 문서에서의 약칭 |
|---|---|---|---|
| 1 | answer/9044175 (Search Console) | 직접 조치 보고서 — *부가 가치가 전혀 또는 거의 없는 빈약한 콘텐츠* 섹션 | **MA-Thin** |
| 2 | publisherpolicies/answer/11035931 | Google 웹 검색의 스팸 정책 (Publisher Policies 고객센터) | **PP-Spam** |
| 3 | adsense/answer/10015918 | Google 애드센스 콘텐츠 및 사용자 환경 | **AS-Content** |
| 4 | adsense/answer/10502938 | Google 게시자 정책 (콘텐츠/행동/개인정보/요구사항) | **PP-Full** |

각 정책 인용은 짧은 직접 인용(15단어 이하) + 자체 요약을 섞어서 적었습니다.

---

## 1. 요약 — 어떤 정책 위반이 거절을 유발했나

거절 사유 "가치가 별로 없는 콘텐츠" 는 **PP-Full** 의 *행동 정책 → 인벤토리 가치 → 게시자 콘텐츠가 없는 화면의 Google 게재 광고* 조항을 직접 매핑합니다. 그 조항은 다음 4가지 화면을 광고 부적합으로 명시합니다.

> 게시자 콘텐츠가 없거나 가치가 별로 없는 콘텐츠를 사용한 화면 / 아직 미완성 상태인 화면 / 알림, 탐색 또는 기타 행동 목적으로 사용되는 화면 / *(별도 조항)* 복제된 콘텐츠가 있는 화면

거절을 부른 핵심 신호 두 가지는 다음과 같습니다.

1. **인벤토리 미완성 신호** — `templates/fragments/ad-slot.html` 의 `data-ad-client/data-ad-slot` 이 `XXXXXXXXXX` placeholder. 봇 입장에선 "광고 코드는 있는데 슬롯에 단위가 매핑 안 됨" → *미완성 화면* 으로 간주됨. ([PP-Full] 인벤토리 가치)
2. **콘텐츠 빈약 신호** — `/use-cases/*` 4개 페이지 본문이 각 80~120 단어, 사이트의 인덱스 가능 페이지 총 ≈14개. ([MA-Thin] 빈약한 페이지, [AS-Content] 고유 콘텐츠 충분 제공, [PP-Spam] *자체 콘텐츠가 거의 또는 전혀 없는 제휴 프로그램 등의 다른 '쿠키 커터' 접근 방식*)

기존 `adsense_approval.md` 의 진단과 동일한 결론입니다. 본 감사는 같은 결론을 4개 정책 조항에 1:1 매핑해 *증거 강화* 하고, 기존 plan 이 명시적으로 다루지 않은 부수 위반 위험을 추가합니다.

---

## 2. 정책 ↔ 코드/콘텐츠 매핑

### 2.1 [PP-Full / 인벤토리 가치] 게시자 콘텐츠가 없는 화면

원문: "게시자 콘텐츠가 없거나 가치가 별로 없는 콘텐츠를 사용한 화면" 에 광고 게재 불가.

| 화면 | 코드 위치 | 위반 위험 | 비고 |
|---|---|---|---|
| `/insights/trends` (데이터 0 상태) | `templates/insights/trends.html` 44-46, 82-84 | **HIGH** — `popularLocations.isEmpty()` / `popularMenus.isEmpty()` 일 때 본문이 "아직 등록된 ... 없습니다" + 0 통계 카드. 그 화면에서 그대로 ad-slot leaderboard/infeed 2개 호출(130, 259줄). | 기존 plan Phase E 에서 `<th:block th:if="${stats.totalSchedules > 0}">` 가드 명시. **본 감사는 같은 가드를 `popularLocations.isEmpty() and popularMenus.isEmpty()` 도 추가하라고 권고.** |
| `/tools/date-diff` | `templates/tools/date-diff.html` 80-88, 91 | **MED** — 상단은 계산기 UI 위주, 하단 정보 섹션은 4개 짧은 불릿 (`tool.datediff.info.li1~4`) 만. infeed 광고 1개. | "계산기 + 짧은 불릿 4개" 는 PP-Full 의 *알림, 탐색 또는 기타 행동 목적으로 사용되는 화면* 에 가까움. 본 감사 **권고: 광고 끄거나 본문 600단어 이상으로 확장**. (기존 plan 미반영 — 항목 추가 필요) |
| `/{ownerId}` (대시보드) | `OwnerController` (확인 필요), 노출 안 됨 | LOW (실제 차단됨) | `SeoService.getDashboardSeo` 가 `noindex` + `adsEnabled` 호출 안 함. |
| `/{ownerId}/{year}/{month}` (스케줄 뷰) | `templates/schedule/view.html`, `SeoService.getScheduleSeo` 234-249줄 | LOW (실제 차단됨) | `noindex` + `adsEnabled` 비호출. |
| `/runners` (runner home) | `templates/runners/home.html` | **MED** | `robots.txt` 13~17줄에 `Allow: /runners*`. SeoService 도 호출 안 함 → 광고는 OFF. 그러나 인덱스 가능. AdSense 봇이 사이트 전체를 둘러보는 단계에서 *사이트의 테마와 관련 없는 페이지* (AS-Content) 로 인식될 위험. 본 감사 **권고: `Disallow: /runners` 또는 SEO 메타에 `noindex`** 부여. (기존 plan 미반영) |

### 2.2 [PP-Full / 인벤토리 가치] 게시자 콘텐츠보다 광고가 더 많음

원문: "게시자 콘텐츠보다 광고나 기타 유료 프로모션 자료가 더 많은 화면" 에 광고 게재 불가.

| 화면 | 본문 분량 (현재) | 광고 호출 | 위반 위험 | 비고 |
|---|---|---|---|---|
| `/use-cases/{slug}` | 본문 80~120 단어 | infeed 1개 (`use-cases/detail.html` 31) | **HIGH** | 광고 1개여도 본문이 짧으면 시각적 비율이 1/4 이상 차지 가능. 기존 plan Phase B (1,000+단어) 가 해소함. |
| `/insights/trends` (데이터 0) | "아직 등록된 ..." + 통계 0 + 트렌드 설명 1문장 | leaderboard + infeed = **2개** | **HIGH** | 데이터 0 일 때 본문 ≤ 80단어인데 광고가 2개 ⇒ 명확한 비율 위반. 가드가 핵심. |
| `/faq` | 8 Q&A, 답변 평균 1~2문장 | leaderboard 1개 (107) | LOW~MED | 답변이 짧아 본문 ≈ 250단어. 광고 1개와 비율은 OK 이나 *얇음* 신호. plan Phase E 에서 25개로 확장 명시. |
| `/guide` | 5 step + use case 4 카드 ≈ 600단어 | leaderboard 1개 (85) | LOW | 비율 OK. plan Phase E 에서 1,300단어로 확장. |
| `/privacy`, `/terms` | 350~400단어 | leaderboard 1개 | MED | 본문 OK 이나 *광고 게재 정당성 의심* 페이지. AdSense 검토 시 *법적 페이지에 광고가 박혀있다* 는 자체로 신호. 본 감사 **권고: privacy/terms 는 `adsEnabled(false)` 로 변경** (PP-Full 의 *행동 목적으로 사용되는 화면* 해석 보수). (기존 plan 의 Phase E 는 단어수만 늘리고 광고는 그대로 유지하는데, **본 감사는 광고 OFF 를 권고**.) |
| `/` (index) | 1,300~1,800단어 | 광고 호출 없음 | OK | index 에 광고 없음 — 좋은 결정. |

### 2.3 [PP-Full / 인벤토리 가치] 복제된 콘텐츠가 있는 화면

원문: "논평을 추가하거나, 선별 게재 또는 기타 부가 가치를 추가하지 않고 다른 사용자의 콘텐츠를 삽입 또는 복사한 콘텐츠를 표시하는 화면" 에 광고 게재 불가.

| 화면 | 위반 위험 | 비고 |
|---|---|---|
| `/{ownerId}/{year}/{month}` (UGC 스케줄 뷰) | LOW (실제 차단됨) | 참여자 이름·장소·메뉴 = UGC. 봇 색인 안 되고 광고 OFF. ✓ |
| `/insights/trends` 의 인기 장소·메뉴 표 | LOW | 서비스 자체 집계 결과 + 자체 트렌드 설명. 정책상 *부가 가치(선별 + 점수 가중)* 가 있어 OK. 다만 plan Phase E 의 `commentary.intro/whyPopular/howToUse` 섹션이 추가돼야 *부가 가치 명시화*. |

### 2.4 [PP-Full / 행동 정책] 광고 방해

원문: "오버레이, 탐색 작업 또는 다른 작업 항목에 인접해 있거나, 의도하지 않은 광고 상호작용으로 이어질 수 있는 경우" 광고 불가.

| 화면 | 위반 위험 | 비고 |
|---|---|---|
| `/use-cases/{slug}` | LOW | infeed 광고가 본문 끝 + CTA 사이에 위치. CTA 버튼과 시각적 인접성 ≈ 자연 ad break 거리 (1.5~2 rem). 그러나 *CTA 와 너무 가까우면 의도하지 않은 클릭* 위험. 본 감사 **권고: ad-slot 과 CTA 사이 최소 3rem 마진 보장**. (기존 plan 미반영) |
| 모든 페이지 ad-slot | OK | `aria-label="광고"` (한국어 고정) — i18n 미흡하지만 정책 위반은 아님. **권고: 영문 로케일에선 "Advertisement" 노출되도록 메시지 키화**. |

### 2.5 [AS-Content] 고유 콘텐츠 충분 제공

원문(요약): "Google에서 해당 사이트에 대해 파악할 수 있도록 페이지에 고유 콘텐츠가 충분해야 합니다."

기존 plan 의 Phase B/C/D (use-case 9개 + guides 4개 × 한·영) 가 이 조항을 직접 충족합니다. 본 감사는 다음 *부수 항목* 을 추가합니다.

- **저자/게시자 정보**(E-E-A-T): AS-Content 는 *전문 지식이나 개인적인 의견 등 고유한 콘텐츠를 추가* 하라고 명시. 현재 모든 페이지에 `meta name="author" content="DateDate"` (head.html 18줄) 만 있고 *사람 또는 조직 페이지* 가 없음. 본 감사 **권고: `/about` 또는 `/team` 페이지 추가, JSON-LD `Organization` 의 `founder`, `employee`, `sameAs` 필드 채우기**. (기존 plan 미반영 — Phase D 의 `/guides/` 에 author=Organization 만 명시됨)
- **정기적 업데이트**: AS-Content 는 *사이트를 정기적으로 업데이트* 하라고 명시. ADR-0003 의 sitemap lastmod 는 BuildProperties 기반 → 빌드 시점 = lastmod. **권고: 콘텐츠 페이지별 실제 작성/수정일을 messages 또는 별도 메타에 두고 sitemap lastmod 와 페이지 표시(`<time datetime>`)에 모두 반영**.

### 2.6 [AS-Content] 중복/유사 콘텐츠

원문: "여러 페이지가 유사하거나 매우 유사한 콘텐츠가 많은 경우 각 페이지를 확장하거나 하나로 통합하는 것이 좋습니다."

| 패턴 | 코드 위치 | 위반 위험 |
|---|---|---|
| `/use-cases/{slug}` 4개 템플릿 동일 | `templates/use-cases/detail.html` (5~54줄) — 동일한 hero/body/example/CTA/others 5섹션 구조에 슬러그별 텍스트만 다름 | **HIGH** | 본문 80~120단어 + 동일 템플릿 + 동일 step 패턴 → AS-Content 의 *쿠키 커터 접근 방식* 또는 *프래그먼트 복사* 신호. plan Phase B 의 8섹션 확장 + 영문 별도 작성으로 해소 예정. |
| 모든 페이지 footer-minimal 동일 | `templates/fragments/footer.html` 21~60줄 | LOW | AS-Content 의 *모든 페이지 하단에 긴 저작권 텍스트 삽입 대신 간략한 요약 + 링크* 권고에 부합. 한 줄 카피 + 카테고리별 링크. ✓ |
| 모든 페이지 GTM noscript iframe | 모든 템플릿 5~7줄 | LOW | 정상. |
| 모든 페이지 head 폰트/CSS preload | `head.html` 107~125줄 | LOW | 정상. |
| `/{ownerId}/{year}/{month}` 페이지의 본문 | `view.html` | LOW (차단됨) | UGC. |

### 2.7 [AS-Content] Navigation / 사이트 구조

원문(요약): "정렬 / 가독성 / 기능 / 정확성" 4가지 기준 + "사이트의 테마 또는 비즈니스 모델과 관련이 없는 페이지의 텍스트" 가 없어야.

| 항목 | 코드 위치 | 위반 위험 |
|---|---|---|
| 헤더 nav 링크 정확성 | `templates/fragments/header.html` | LOW | nav 자체는 OK. |
| **`/runners*` 가 인덱스 가능** | `static/robots.txt` 13~17줄 | **MED** | datedate 의 비즈니스(*그룹 일정 조율*) 와 다른 도메인(*러닝 크루 출석/거리 랭킹*). AS-Content 의 *사이트의 테마와 관련이 없는 페이지의 텍스트* 신호. **권고: `Disallow: /runners` + SeoService 호출 시 `noindex` 강제** (위 2.1 와 같은 권고). |
| `/trading/`, `/stock/` | `robots.txt` 31~32 | OK | 이미 `Disallow`. ✓ |

### 2.8 [PP-Spam] 쿠키 커터/도어웨이

원문: "검색엔진 전용으로 제작된 '도어웨이' 페이지 또는 자체 콘텐츠가 거의 또는 전혀 없는 제휴 프로그램 등의 다른 '쿠키 커터' 접근 방식을 만들면 안 됩니다."

| 패턴 | 위반 위험 |
|---|---|
| `/use-cases/*` 4개 — 동일 템플릿 + 본문 짧음 + 검색용 키워드 강화 | **MED~HIGH (현재)** → plan 적용 시 LOW |
| `/guides/{slug}` 4개 (plan Phase D 신규) | LOW (장문 + 카테고리 다양화) |

### 2.9 [MA-Thin] 빈약 콘텐츠 직접 조치 항목

원문: "빈약한 제휴 페이지 / 다른 출처로부터의 콘텐츠 / 도어웨이"

datedate 는 **제휴(affiliate) 페이지 없음**, **다른 출처 스크랩 없음**, **검색 키워드 도어웨이 없음**. MA-Thin 의 3가지 카테고리 자체에는 직접 해당하지 않으나, *부가 가치가 거의 없는 빈약한 콘텐츠* 라는 상위 정의에는 use-case 페이지가 해당. plan Phase B 가 해소함.

---

## 3. 기존 `adsense_approval.md` plan 미반영 항목 (본 감사가 추가)

기존 plan 은 매우 상세합니다. 다음 6개는 plan 에 명시적으로 없거나 약하게만 다뤄진 항목으로, 4개 정책 문서를 직접 읽고 *반드시 같이 처리* 해야 한다고 판단한 보강안입니다.

| # | 항목 | 근거 정책 | 권고 |
|---|---|---|---|
| 1 | `robots.txt` 의 `/runners` 인덱스 차단 | AS-Content 사이트 테마 일관성, PP-Full 인벤토리 가치 | `Allow: /runners*` 라인을 `Disallow: /runners` 로 변경. SeoService 도 RunnerController 가 사용 시 `noindex`. (광고는 이미 OFF.) |
| 2 | `/tools/date-diff` 광고 OFF 또는 본문 확장 | PP-Full 인벤토리 가치 (행동 목적 화면) | 둘 중 택1: (a) `SeoService.getDateDiffSeo` 의 `.adsEnabled(true)` 를 `false` 로, (b) 페이지에 600단어 이상의 *언제 D-Day 계산이 유용한가, 평일 계산이 다른 이유, 사례 5가지* 등을 추가. |
| 3 | `/privacy`, `/terms` 광고 OFF | PP-Full 인벤토리 가치 (행동 목적 화면 보수 해석) | `SeoService.buildSimpleWebPageSeo()` 에서 prefix가 privacy/terms 인 경우 `.adsEnabled(false)` 로 분기. plan 의 단어 확장은 그대로 유지. |
| 4 | `/insights/trends` 광고 가드를 *본문 0* 기준으로 강화 | PP-Full 인벤토리 가치 (콘텐츠 없는 화면) | plan Phase E 의 `${stats.totalSchedules > 0}` 가드를 `${stats.totalSchedules > 0 and (!popularLocations.isEmpty() or !popularMenus.isEmpty())}` 로 강화. |
| 5 | `<a th:href>` CTA 와 `ad-slot` 의 시각적 거리 | PP-Full 광고 방해 | 모든 ad-slot 호출 직후의 CTA 섹션에 `margin-top: 3rem` 보장. CSS 에 `.ad-slot + section { margin-top: 3rem }` 또는 명시 마진. |
| 6 | E-E-A-T (저자/조직 정보) | AS-Content 고유 콘텐츠 + JSON-LD `Organization`/`Person` | `/about` 페이지 추가 (운영 조직, 연락처, 데이터 정책 요약). JSON-LD 의 `Organization` 에 `founder` / `sameAs` (GitHub, X 등) 채우기. plan Phase D 의 author 보강과 함께. |

---

## 4. 정책 인용 출처 (Quote Inventory)

본 감사에서 사용한 짧은 직접 인용은 모두 15단어 이하로 제한했습니다. 정책 적용을 검증하려면 *원문 전체* 를 직접 다시 확인하시기 바랍니다.

| 약칭 | 정책 조항 | 본 문서에서의 사용 |
|---|---|---|
| MA-Thin | "부가 가치가 전혀 또는 거의 없는 빈약한 콘텐츠" | §1, §2.9 |
| PP-Spam | "자체 콘텐츠가 거의 또는 전혀 없는 ... '쿠키 커터' 접근 방식" | §1, §2.8 |
| AS-Content | "고유 콘텐츠가 충분해야" / "유사한 콘텐츠가 많은 경우 ... 통합" | §2.5, §2.6 |
| PP-Full | "게시자 콘텐츠가 없거나 가치가 별로 없는 콘텐츠를 사용한 화면" | §1, §2.1 |
| PP-Full | "게시자 콘텐츠보다 광고나 기타 유료 프로모션 자료가 더 많은 화면" | §2.2 |
| PP-Full | "복제된 콘텐츠가 있는 화면" | §2.3 |
| PP-Full | "광고 방해" | §2.4 |

---

## 5. 다음 단계

1. 본 감사의 §3 권고 6개를 `adsense_approval.md` 의 *Phase A → E* 어디에 끼울지 결정.
2. 결정 후 `adsense_approval.md` 본문을 갱신 (또는 별도 commit).
3. 기존 plan 의 Phase A 부터 진행.
4. Phase F (재심사) 직전 본 문서의 §2 표를 *최종 체크리스트* 로 한 번 더 통과.

## 6. 참조

- [Search Console 직접 조치 보고서](https://support.google.com/webmasters/answer/9044175)
- [Publisher Policies — Google 웹 검색의 스팸 정책](https://support.google.com/publisherpolicies/answer/11035931)
- [AdSense — 콘텐츠 및 사용자 환경](https://support.google.com/adsense/answer/10015918)
- [AdSense — Google 게시자 정책](https://support.google.com/adsense/answer/10502938)
- 사내: `docs/prompt/adsense_approval.md`, `docs/adr/common/seo/0007-content-pages-for-adsense.md`, `docs/seo-evolution-playbook.md`
