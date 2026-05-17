# ADR-0003: MessageFormat NumberFormat 천단위 그룹화 차단

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | common |
| 관심사 | i18n |
| 관련 커밋 | `docs/git_commit.md` Commit 7 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

`document.title` 에 "**2,026/5**" 가 출력되는 사용자 신고. 해당 SEO 메시지 키:

```
seo.schedule.title=...{1}년 {2}월 일정...
```

MessageFormat 의 `{1}` 표시는 인자가 `Number` 타입이면 *기본 NumberFormat* 으로
포맷 → 한국어 로케일에서 천단위 쉼표 자동 적용 → year=2026 이 "2,026" 으로 출력.

같은 PR 에서 추가 문제도 다뤄졌다:
- DateDate 스케줄 페이지 / 홈 ID 검증 / 투표 카드의 한국어 리터럴이 EN 모드에서도
  하드코딩되어 출력 — i18n 의 의미 자체가 깨짐.

## Decision — 무엇을 골랐나

MessageFormat 에서 *그룹화 안 된 정수 포맷* 명시 + 하드코딩 KO 문자열을 메시지 키로
외부화.

- **`{1,number,#}` 패턴** — `seo.schedule.{title,description,ogTitle,ogDescription}` +
  `schedule.create.desc` 의 `{1}/{2}` → `{1,number,#}/{2,number,#}`. `#` 패턴은 그룹화
  미적용.
- **하드코딩 KO 문자열 외부화** — `index.html` 의 ID 검증 토스트 3개, `schedule/view.html`
  의 8개 i18n 문자열을 messages 키로.
- **`SCHEDULE_DATA.messages` 인젝션** — Thymeleaf 가 `[[#{...}]]` 인라인 표현으로 JS
  객체에 주입.
- **회귀 테스트** — `SeoServiceI18nTest.scheduleSeo_yearNotGrouped` 추가 (KO/EN 양쪽
  에서 "2,026" 미포함 검증).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 인자를 `String.valueOf(year)` 으로 미리 문자열화 | 즉시 해결 | 다른 메시지에서 같은 함정 반복 — 설계 근본 미해결 |
| Locale 별 NumberFormat 커스텀 | 정밀 | year/month 인자만의 문제 — 과한 일반화 |
| **(선택) `{n,number,#}` 패턴 + 회귀 테스트** | 의도 명확, 미래 회귀 차단 | — |

`{n,number,#}` 는 MessageFormat 표준 — Spring NumberFormat 자동 그룹화 차단의 정석.

## Consequences — 영향

- **긍정:**
  - 타이틀의 "2,026" 사라짐.
  - 회귀 테스트가 미래 메시지 추가 시 같은 함정 재현 차단.
  - EN 모드 하드코딩 KO 문자열 사라짐 → 영문 시장 진입 보강.
- **부정:**
  - 신규 SEO 메시지에 number 인자 추가 시 `{n,number,#}` 명시 필요 — 리뷰 가드.
- **후속:**
  - 이 결정의 회귀 테스트 패턴 (`*I18nTest`) 가 다른 i18n 결정의 가드로 재사용.

## References

- 관련 코드:
  - `src/main/resources/messages.properties`
  - `src/main/resources/messages_en.properties`
  - `src/main/resources/templates/index.html`
  - `src/main/resources/templates/schedule/view.html`
  - `src/main/resources/static/js/schedule/{state,calendar,voting,utils}.js`
  - `src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java`
- 관련 커밋: `docs/git_commit.md` Commit 7
