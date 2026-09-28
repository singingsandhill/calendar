# ADR-0013: 퍼블리셔 신원 신호 — 연락처 이메일 공개 + Organization `sameAs`(운영자 GitHub)

| 항목 | 값 |
|---|---|
| 상태 | Accepted — [08-17 AdSense 진단 §5-6](../../../audit/adsense-low-value-content-diagnosis-2026-08-17.md) 의 "`founder`/`sameAs` 보류 유지" 를 이 ADR 로 대체 (`sameAs` 한정 — `founder`·개인 저자는 여전히 두지 않음) |
| 날짜 | 2026-09-26 |
| 도메인 | common (SEO) / datedate |
| 관심사 | SEO / E-E-A-T / AdSense 게시자 신원 |
| 관련 커밋 | `docs/guides/git-commit.md` 참조 |
| 관련 문서 | [SEO 종합 점검 2026-09-24](../../../audit/seo-comprehensive-audit-2026-09-24.md) §11 재검증 |

## Context — 무엇이 문제였나

AdSense "낮은 가치 콘텐츠" 3차 통지(2026-08-17)는 "승인됨 + 주의 필요" 상태다. 진단의 본질 원인은
콘텐츠 믹스(editorial)였지만, 게시자 신원 신호도 약한 편이었다.

- 사용자가 볼 수 있는 연락 수단은 Google Form(`/about`, 푸터 "의견 보내기")뿐이었다.
- `Organization` JSON-LD 에는 이름·URL·로고·설명과 `ContactPoint.url`(같은 Form)만 있었다.

이 신호를 보강하는 데에는 기존 제약이 두 가지 있었다.

- **08-17 진단 §5-6** — "E-E-A-T `founder`/`sameAs` 는 **보류 유지** — 실체 없는 계정 기재는 그 자체가
  더 나쁜 신호".
- **ADR-0011 결정 6** — 저자는 Organization 으로만 표기하고 "**실존하지 않는** 개인 프로필·sameAs 계정은
  만들지 않는다".

둘 다 금지한 것은 *허위 신원* 이다. 실제로 존재하고 운영자가 소유한 계정을 연결하는 것 자체를 막은 것은 아니다.

## Decision — 무엇을 골랐나

**사람이 볼 수 있는 연락처는 화면에 공개하고, 실존 운영자 계정은 구조화 데이터로만 연결한다.**

1. **연락처 이메일 공개** — `cheongyakplanet@gmail.com`
   - 푸터: 기본 푸터 내비 `mailto:` 링크 + `footer-minimal` 하단 "문의" 줄
   - `/about` 연락 섹션: 이메일 줄 (기존 Form 링크와 병기)
2. **JSON-LD `email`** — 홈 `Organization`, `/about` 의 `AboutPage.mainEntity` Organization 과
   그 `ContactPoint` 에 같은 주소를 넣는다.
3. **JSON-LD `sameAs`** — `["https://github.com/singingsandhill"]` 를 같은 두 Organization 에 넣는다.
   - 대상은 이 저장소(`singingsandhill/calendar`)의 소유 계정이고 실존을 확인했다(2026-09-26 HTTP 200).
   - **화면(UI)에는 링크를 노출하지 않는다** — 크롤러용 메타데이터에만 둔다(사용자 결정).
4. **유지되는 것** — 기사 저자는 계속 Organization(DateDate)이다. `founder` 나 개인 `Person` 저자는 두지 않는다.
   ADR-0011 결정 6 의 허위 신원 금지는 그대로 준수한다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 판정 |
|---|---|---|
| 보류 유지 (Form 만) | 변경 0 | 게시자에게 직접 연락할 수단이 Form 하나뿐 — 신원 신호 보강 기회를 버림 |
| 개인 저자 `Person` + `founder` | E-E-A-T 신호 최대 | 개인 프로필 공개 부담 + 기사 저자를 개인으로 바꾸는 것은 0011 결정 변경. 이번 범위 아님 |
| `sameAs` 를 UI 에도 노출 | 사람·기계 모두 확인 가능 | 채택하지 않음 — 사용자 결정으로 UI 비노출(아래 근거) |
| **(선택) 이메일 UI+JSON-LD, sameAs 는 JSON-LD 만** | 사람에게는 연락처, 검색엔진에는 엔티티 연결 | — |

`sameAs` 를 UI 에 넣지 않은 이유는 작업 기록에 "일반 UI 에는 GitHub 링크를 노출하지 않고 크롤러 메타데이터에만
반영" 이라는 결정만 남아 있고, 그 이상의 근거는 기록되지 않았다. Google 의 Organization 구조화 데이터는
`sameAs` 를 화면 표시 없이 쓰는 것을 허용한다.

## Consequences — 영향

- **긍정:**
  - 사람이 확인할 수 있는 직접 연락처(이메일)가 모든 색인 페이지 푸터와 `/about` 에 생긴다.
  - 구조화 데이터의 Organization 이 실존 계정과 연결된다.
- **부정 / 확인 필요:**
  - **`sameAs` 는 사람 리뷰어에게 보이지 않는다.** AdSense 심사처럼 사람이 보는 신원 신호로는 효과가 없고,
    검색엔진의 엔티티 이해에만 기여한다.
  - **이메일 로컬파트가 DateDate 와 다른 서비스명(`cheongyakplanet`)으로 읽힌다.** 운영자 공용 주소라면 의도대로지만,
    신원 일관성 관점에서는 확인이 필요하다 (점검 §11 M2).
  - **같은 주소가 6곳에 하드코딩돼 있다** — `SeoService` 3, `about.html` 1, `footer.html` 2. 바꿀 때 누락 위험이 있다
    (점검 §12 수정 계획 P3).
- **후속:**
  - 개인 저자·`founder` 도입은 별도 결정(0011 변경)으로만 다룬다.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java`
    (`getHomeSeo` Organization, `getAboutSeo` mainEntity·ContactPoint)
  - `src/main/resources/templates/fragments/footer.html`, `src/main/resources/templates/about.html`
  - `src/main/resources/messages*.properties` (`footer.contact`, `about.section.contact.emailLabel`)
  - `src/main/resources/static/css/style.css` (`.footer-contact`)
- 관련 ADR: [0011 /guides 에디토리얼 허브](0011-guides-editorial-hub.md) (결정 6 — 허위 신원 금지, 유지)
- 관련 docs: [AdSense 3차 진단 2026-08-17](../../../audit/adsense-low-value-content-diagnosis-2026-08-17.md) §5-6,
  [정책 매핑 감사](../../../audit/adsense-low-value-content-policy-mapping.md) §3-#6
