# /tools/date-diff 디자인 통일 — 개선 계획

| 항목 | 값 |
|---|---|
| 작성일 | 2026-10-05 |
| 배경 | 날짜 계산기 페이지가 다른 콘텐츠 페이지(/faq·/guides·/insights)와 디자인 통일성이 떨어짐 |
| 기준 커밋 | main `9e1cf97` (코드 정적 분석 기준 — 운영 화면 실측은 미수행) |
| 결정 기록 | ADR 불필요 판단(신규 결정이 아닌 공통 패턴 회귀). 선택 사항: §3 단계 5 참고 |
| 상태 | **구현 완료** (2026-10-05, `docs/guides/git-commit.md` Commit 196 — 계획과 다르게 한 점은 §6) |

## 1. 원인 진단

`/tools/date-diff` 만 `<main class="container tool-page tool-page--almanac">` 수정자를 쓰며,
`static/css/style.css` 4527~5085행(약 560줄)의 `.tool-page--almanac` 스코프가 사이트 토큰을 거의 전부 덮어쓴다.
이 스코프는 `92400e0`(2026-05-25, "Almanac UI 적용")이 ADR 없이 넣었고, 그 커밋의 수용 기준은 오히려
"사이트 기본 톤 유지"였다.

| 항목 | 사이트 공통 (faq·guides·insights) | date-diff (almanac) |
|---|---|---|
| 강조색 | `--primary-color` #3498db | 자체 팔레트 (`--almanac-ink` #1c1714, `--almanac-vermillion` #c43a23) |
| 히어로 | 가운데 정렬, serif h1 2rem, 부제 `--text-light` | 왼쪽 정렬, h1 `clamp(2.4rem, 5.5vw, 4rem)`, 본문까지 serif |
| 모서리·그림자 | `--radius-xl`, 부드러운 그림자 | `border-radius: 0`, 4px 오프셋 하드 섀도 |
| 장식 | 없음 | "Datedate · Almanac N°01" eyebrow, `::before` "INSTRUMENT N°01", `::after` "—— END ——", SVG 괘선 |
| 입력·버튼 | 2px 테두리·8px 모서리 (`.form-group input`) | 밑줄형 입력, 자간 0.4em 대문자 버튼 |
| 본문 폭 | 800px | 920px + 별도 패딩 |
| 하단 CTA | 연파랑 그라데이션·가운데 정렬 (`.faq-cta`, `.insights-cta-section`) | 흰 배경·검은 테두리·왼쪽 정렬 |
| 브레드크럼 | 링크 `--primary-color`, current `--text-color` | current 주홍색, 대문자/자간 |
| FAQ | `.faq-item` + `faq-toggle.js` 아코디언 (faq·guides 12편·index) | `<details class="tool-faq-item">` + 전용 스타일 |

**기본 블록의 상태.** 기본 블록(4339~4525행)의 `.tool-page`·`.tool-hero`·`.tool-cta` 는 이미
`.faq-page`·`.faq-hero`·`.faq-cta` 와 값이 같다. 따라서 almanac 만 걷어내면 폭·히어로·CTA 는 저절로 맞는다.
반대로 `.tool-content`·`.tool-note`·`.tool-related-list`·`.tool-faq-item` 은 기본 블록에 규칙이 **한 번도 없었다**.
`f20a27a` 가 마크업만 추가했고, 이틀 뒤 `92400e0` 이 almanac 스코프 안에만 스타일을 넣었다.
제거 후 새로 정해야 하는 것은 이 넷과 h2 모양뿐이다.

### 함께 발견된 버그

1. **미정의 토큰.** 기본 블록이 `:root` 에 없는 토큰을 참조한다. almanac 을 제거하면 다음이 **실제로 드러난다.**
   - `.workday-toggle input` `accent-color` → `var(--primary)`. 체크박스가 `.form-group` 밖이라 그대로 적용되고, 브라우저 기본색이 된다.
   - `.diff-number` → `var(--text-3xl)`, `var(--primary)`. 숫자 크기·색이 상속값으로 떨어진다.
   - `.diff-error` → `var(--danger)` (정의는 `--danger-color`). 오류 문구가 본문색이 된다.
   - almanac 모바일 규칙의 `var(--almanac-paper)` 는 블록과 함께 삭제된다.

   **가려진 채 남는 것:** `.date-input:focus` 의 `var(--primary)` 는 적용되지 않는 죽은 선언이다.
   입력칸이 `.form-group` 안이라 `.form-group input:focus`(1158행, 명시도 0,2,1)가 `.date-input:focus`(0,2,0)를 이긴다.
   같은 이유로 `.date-input` 의 border·padding·border-radius·font-size 도 `.form-group input`(1149행, 0,1,1)에 가려져 있다.
   입력칸은 이미 사이트 공통 모양이다.
2. **`hidden` 무력화 (확정).** 작성자 스타일 `.diff-card { display: flex }` 가 UA 스타일 `[hidden] { display: none }` 을 이긴다
   (출처 우선순위). almanac 은 `display` 를 건드리지 않으므로 **운영에서 지금도** 깨져 있다.
   "평일만 계산" 모드에서 개월 카드가 갱신되지 않은 값으로 남는다. 선례: 4085행 `.onboarding-banner[hidden]` 명시 차단.
3. **`.btn-outline` 이중 정의** — 628행(1px 회색, 홈 보조 액션)과 914행(2px primary, hover 시 채움). 뒤쪽이 이긴다.
   이번 범위 밖, 보고만.
4. **`.btn-secondary` 이중 정의** — 905행(회색 채움)과 3822행(연회색 + 2px 테두리). 뒤쪽이 이긴다. 범위 밖, 보고만.
5. **`.use-case-faq-item` 무스타일.** `use-cases/detail.html:100` 이 이 클래스를 쓰지만 `static/` 전체에 규칙이 0건이다
   (브라우저 기본 `<details>`). 범위 밖, 보고만.

## 2. 대안

| 대안 | 장단점 | 판단 |
|---|---|---|
| **A. 사이트 공통으로 회귀** | 최소 변경, 기존 패턴(faq·guides) 재사용, 페이지 전용 CSS 560줄 제거 | **채택 권장** — CLAUDE.md "최소 범위·기존 패턴 재사용" 부합 |
| B. almanac 구조 유지 + 색·모서리만 토큰화 | 편집 분위기 일부 유지 | 페이지 전용 CSS 가 남아 같은 편차 재발 |
| C. 편집 스타일을 사이트 전체로 확산 | 브랜드 쇄신 | 요청 범위 밖, 결정 변경이라 ADR 필요 |

## 3. 실행 계획 (A안)

### 단계 0 — 버그 재현 캡처 (RED)

- 운영 `https://datedate.site/tools/date-diff` 에서 "평일만 계산"을 체크한 뒤, 개월 카드가 남아 있는 화면을 캡처한다.
  CSS 버그라 단위 테스트로는 재현할 수 없다. 이 캡처를 RED 증거로 쓰며, 운영 페이지 조회라 앱 기동이 필요 없다.

**검증:** 캡처에 개월 카드가 보인다. 보이지 않으면 §1 버그 2 의 진단을 다시 본다.

### 단계 1 — 템플릿 정리 (`templates/tools/date-diff.html`)

- `tool-page--almanac` 클래스 제거 → `<main class="container tool-page" id="main-content">`
- `.almanac-eyebrow` span, `.almanac-rule` SVG 삭제 (하드코딩 문구 — 메시지 키 영향 없음)
- 화살표 SVG 는 유지한다. 색은 기본 `.date-diff-arrow` 가 이미 `--text-muted` 로 지정하므로 손대지 않는다.
- FAQ 3개를 사이트 공통 아코디언으로 전환한다 (ADR datedate/frontend/0004 가 통일한 방식).
  - 구조: `.faq-list > .faq-item > button.faq-question[aria-expanded][aria-controls] + div.faq-answer[role=region]`.
    `guides/how-to-pick-a-date.html:99-109` 와 같게 `th:each="i : ${#numbers.sequence(1, 3)}"` 로 반복하고,
    id 접두는 `datediff-faq-` 로 한다.
  - 답변은 지금처럼 `th:utext` 를 유지한다. 메시지 키는 바뀌지 않는다.
  - `</body>` 앞에 `<script defer th:src="@{/js/faq-toggle.js}"></script>` 를 추가한다.
  - 답변은 최대 193자(en)라 `.faq-answer` 의 `max-height: 500px` 안에 든다.
  - 기각한 대안: `<details>` 를 유지하고 `.tool-faq-item` 전용 CSS 를 새로 만드는 것. 공통 패턴이 있는데 또 다른 FAQ 모양을 만드는 셈이다.

**검증:** 템플릿에 `almanac` 0건, `faq-question` 1건(반복 대상 3개), `faq-toggle.js` 1건.

### 단계 2 — CSS 정리 (`static/css/style.css`)

- 4527~5085행 almanac 블록을 삭제한다. 주석 머리부터 모바일 미디어쿼리 끝까지이며,
  `@keyframes almanacBloom` 도 포함한다(타 사용처 없음 확인).
- 기본 블록 토큰을 수정한다 (실제로 드러나는 3곳).
  - `.workday-toggle input` `accent-color`: `var(--primary)` → `var(--primary-color)`
  - `.diff-number`: `var(--text-3xl)` → `2rem` (토큰 신설은 1회 사용이라 하지 않음), `var(--primary)` → `var(--primary-color)`
  - `.diff-error`: `var(--danger)` → `var(--danger-color)`
- `.date-input` 은 새 규칙을 쓰지 않고 **줄인다.** `.form-group input` 에 가려진 border·padding·border-radius·font-size 선언과
  `.date-input:focus` 규칙 전체를 삭제한다. color·background·cursor 만 남긴다.
- `.diff-card[hidden] { display: none; }` 를 추가한다.
- 본문 프로즈는 `.tool-info`·`.tool-content` 를 guides 기사와 같은 규칙으로 맞춘다.
  - `.guides-article` 의 h2·p·ul/ol·li·a 규칙에 `.tool-info`, `.tool-content` 선택자를 **덧붙인다.**
    값을 복사하지 않고 원천을 하나로 둔다.
  - 겹치는 기본 규칙 `.tool-info h2` 를 삭제하고, `.tool-use-list` 에서는 color·line-height 만 삭제한다.
  - `.tool-presets h2` 도 같은 h2 모양으로 맞춘다. 한 페이지의 h2 3종(1.2rem / 1.3rem 고딕 / 세리프+밑줄)을 1종으로 줄인다.
  - `.tool-related-list` 는 위 ul·li·a 규칙으로 커버된다.
- `.tool-note` 는 `.insights-info`(3644행)와 같은 톤으로 둔다: `--bg-light` 배경, 4px `--primary-color` 좌측 보더, 12px 모서리,
  1.5rem 패딩. 그 규칙은 max-width·auto margin 이 붙어 있어 선택자 병합 대신 값을 맞춘다.
- **개행 주의:** `style.css` 는 한 파일 안에 CRLF/LF 가 섞여 있다(`git ls-files --eol` → `i/mixed`).
  블록 삭제는 상관없다. 새로 넣거나 고치는 줄은 바이트 단위로 이웃 줄의 개행을 따른다.
  전체 CRLF 정규화는 금지다(LF 223줄이 함께 바뀐다).

**검증:**
- `grep -n -- "var(--primary)\|var(--danger)\|text-3xl\|almanac" style.css` 0건
- `git diff --numstat` 과 `git diff --ignore-cr-at-eol --numstat` 의 수치가 같다 (개행 변경 없음)

### 단계 3 — 회귀 테스트

- 전체 `./gradlew test` (WSL: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"`).
  좁혀 돌릴 때는 `--tests` 에 FQCN 을 쓴다. cmd.exe 를 거치면 `"*Name"` 와일드카드가 동작하지 않는다.
- `SiteChromeConsistencyTest`(헤더·푸터)와 `HeadSeoSmokeTest`(head 메타)가 `/tools/date-diff` 를 렌더한다.
  템플릿 렌더 오류는 잡지만 **디자인은 단정하지 않는다.** 디자인은 단계 4 로만 확인한다.
- `build/test-results/test/*.xml` 로 실제 실행된 테스트 수를 확인한다.

### 단계 4 — 시각·기능 비교

- **앱 기동 주의 (운영 안전 — 우선순위 1).** 로컬 `.env` 가 `TRADING_BOT_ENABLED=true`·`TRADING_BOT_MODE=LIVE`·
  `STOCK_BOT_ENABLED=true` 라, `bootRun` 만 하면 실거래 코인 봇이 켜진다.
  - 기동 전에 사용자 확인을 받는다.
  - 두 봇을 명령줄에서 끈다. OS 환경변수가 `.env`(config import)보다 우선한다.
    `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& set TRADING_BOT_ENABLED=false&& set STOCK_BOT_ENABLED=false&& .\\gradlew.bat bootRun"`
  - 정적 CSS 해시는 부팅 시점 캐시라, CSS 를 고친 뒤에는 앱을 재시작해야 반영된다.
- 1280 / 768 / 375px 에서 `/faq`, `/guides/how-to-pick-a-date`, `/tools/date-diff` 를 나란히 캡처한다.
  - Windows Chrome 을 쓴다(WSL 에는 브라우저가 없다).
  - 375px 는 헤드리스 `--screenshot` 의 최소 창 폭(약 500px) 때문에 찍히지 않는다. CDP 의 `Emulation.setDeviceMetricsOverride` 로 찍는다.
- 체크리스트:
  - [ ] 히어로 정렬·크기·부제 색이 /faq 와 동일
  - [ ] 계산 카드 모서리·테두리·배경이 사이트 카드와 동일
  - [ ] CTA 가 `.faq-cta` 와 동일 (버튼 수 차이는 범위 밖 — §4)
  - [ ] 섹션 h2 가 한 가지 모양이고 guides 기사 h2 와 동일
  - [ ] FAQ 아코디언이 guides 기사 FAQ 와 같은 모양이고, 클릭 시 열림·닫힘과 `aria-expanded` 가 전환됨
  - [ ] 프리셋 3종 동작 (100일·1년·올해 경과일)
  - [ ] 종료일 < 시작일 → 오류 메시지 표시 (색 `--danger-color`)
  - [ ] "평일만 계산" → 개월 카드 숨김(단계 0 RED 캡처와 대비), 라벨 영업일/영업주로 전환, 체크박스 색 `--primary-color`
  - [ ] `?lang=en` 전환 시 레이아웃 깨짐 없음
  - [ ] 375px 에서 입력 세로 스택, 화살표 숨김, 가로 스크롤 없음

### 단계 5 — 문서

- `docs/guides/git-commit.md` 에 규약 형식의 커밋 섹션을 추가한다 (커밋은 사용자 실행).
  - append 직전에 마지막 커밋 번호를 다시 확인한다 (검토 시점 Commit 195). 다른 세션과 번호가 경합한다.
  - add 목록에 이 계획 문서를 포함한다 (`docs/superpowers/plans/` 는 추적 대상).
  - `messages*.properties` 는 다른 작업의 미커밋 변경이 있으므로 이번 작업에서 건드리지 않는다.
- ADR: 공통 패턴 회귀라 불필요하다. almanac 자체가 ADR 없이 들어왔고, FAQ 전환은 기존 ADR datedate/frontend/0004 를 따른다.
  "콘텐츠·도구 페이지는 페이지 전용 팔레트 없이 공통 토큰만 사용" 을 규칙으로 고정하려면 `docs/adr/datedate/frontend/0005` 로 기록한다 (선택).
- CLAUDE.md: almanac·`tool-page` 언급이 없어 수정이 필요 없다.

## 4. 범위 밖 (보고만)

- `.btn-outline` 이중 정의(628·914행) 통합 — 홈 등 타 페이지 영향
- `.btn-secondary` 이중 정의(905·3822행) 통합
- CTA 2버튼 구성 (`/faq`·`/insights` 처럼 `btn-spring` + `btn-secondary` "사용 가이드 →")
  - 디자인 통일에는 필요 없다. 기본 `.tool-cta` 스타일이 이미 `.faq-cta` 와 같다.
  - 새 메시지 키가 필요하면 미커밋 변경이 있는 `messages*.properties` 와 커밋 소유권이 충돌한다. 하려면 별도 커밋으로 한다.
- `.use-case-faq-item` 무스타일 — use-cases 상세 FAQ 도 같은 `.faq-item` 전환 후보
- 브레드크럼 인라인 마크업 fragment 화 — ADR datedate/frontend/0004 후속 항목

## 5. 검토 반영 내역 (2026-10-05, 코드 대조)

| 원안 | 정정 | 근거 |
|---|---|---|
| `.tool-faq-item` 을 기존 `.use-case-faq-item` 규칙과 병합 | `.faq-item` + `faq-toggle.js` 로 마크업 전환 | `.use-case-faq-item` 규칙은 `static/` 전체에 0건 |
| `.date-input` 을 `.form-group input` 과 같은 모양으로 작성 | 가려진 선언·`:focus` 규칙 삭제 | `.form-group input`(0,1,1)·`:focus`(0,2,1)가 이미 이김 — 입력칸은 이미 공통 모양 |
| 미정의 토큰이 "almanac 만 제거하면 즉시 드러난다" | `.date-input:focus` 는 명시도로 가려진 죽은 선언, 드러나는 것은 3곳 | 위와 같음 |
| `hidden` 버그 "가능성이 높다" | 확정, 운영에서 현재 발생 → 단계 0 RED 캡처 추가 | 작성자 `display:flex` > UA `[hidden]` |
| 검증에 `./gradlew bootRun` | 사용자 확인 + 두 봇 비활성 오버라이드, 375px 는 CDP | `.env` 가 코인 봇 LIVE |
| `.tool-content` h2 만 guides 밀도로 | `.tool-info`·`.tool-presets` h2 까지 1종, `.guides-article` 선택자에 병합 | 원안대로면 한 페이지에 h2 3종 |
| `.tool-note` "guides 특수 블록 톤" | `.insights-info` 톤 | guides 특수 블록에는 좌측 보더가 없음 |
| (선택) CTA 2버튼 | 범위 밖으로 이동 | 스타일은 이미 동일, messages 파일 커밋 소유권 충돌 |
| 개행 언급 없음 | `style.css` 혼재 개행 주의 + numstat 검증 추가 | `git ls-files --eol` → `i/mixed` |
| 결정 기록 "§5 참고" | "§3 단계 5 참고" | 원안에 §5 없음 |

## 6. 구현 결과 (2026-10-05)

단계 0~5 모두 수행했다. 실측 수치는 Commit 196 섹션에 있다. 계획과 다르게 한 점:

| 계획 | 실제 | 이유 |
|---|---|---|
| `.tool-use-list` 에서 color·line-height 만 삭제 | 규칙 전체 삭제 | 남는 padding-left 도 병합된 `.tool-info ul`·`.tool-content ul`(0,1,1)에 가려져 죽은 규칙 (값도 같은 1.5rem) |
| `.guides-article` 의 h2·p·ul/ol·li·a 에 선택자 병합 | `section` 여백 규칙에도 `.tool-info`·`.tool-content` 병합, 기본 `.tool-info { margin-bottom }` 삭제 | `.tool-content` 는 기본 블록에 여백이 없어(almanac 이 주던 것) 그대로면 본문 4섹션이 붙는다 |
| 각 규칙에 `.tool-info`·`.tool-content` 둘 다 추가 | 마크업에 있는 조합만 추가 (`.tool-info p/a/ol` 없음) | 쓰이지 않는 선택자를 늘리지 않는다 |
| 체크리스트 "계산 카드 = 사이트 카드" | 기본 `.tool-calculator`(1px `--border-light` + `--radius-xl`) 유지로 충족 판정 | `.faq-cta`·`.insights-cta-section` 과 같은 상자. `.faq-item` 의 그림자 카드와는 다르다 |

남은 것 (범위 밖, 미수정):
추가 수정 (사용자 요청, 같은 날): 올해 경과일 프리셋의 시작일이 KST 에서 전년 12-31 로 밀리던 버그를 고쳤다.
- 원인: `setPreset` 의 `fmt` 와 초기화 IIFE 가 `toISOString()` 으로 로컬 Date 를 UTC 날짜 문자열로 바꿨다.
  같은 원인으로 KST 오전 9시 이전에는 기본 시작일·오늘부터 N일이 전날로, 미국 저녁에는 다음 날로 밀렸다.
- 수정: 로컬 연·월·일로 만드는 `toDateInputValue` 하나로 세 곳을 교체했다.
- 검증: 템플릿의 실제 인라인 스크립트를 node `vm` 으로 고정 시계·TZ 아래 실행했다 (KST 3건·LA 3건, RED 6 FAIL → GREEN 6 PASS). 전체 스위트 804 failures 0.
- 375px 에서 날짜 입력칸이 전체 폭이 아니라 내용 폭이다 (`.date-diff-form` 의 `align-items: center` — almanac 이전 기본 동작).
- `.tool-note` 배경 `--bg-light` 가 페이지 배경과 같아 좌측 보더만 보인다 (`.insights-info` 와 같은 상태).

추가 수정 2 (사용자 요청, 같은 날): 미주 영업일 버그를 고쳤다.
- 원인: `calculateDiff` 가 입력값을 `new Date('YYYY-MM-DD')` 로 읽어 UTC 자정이 됐다. `getDay`·`setDate`·`toLocaleDateString` 은 로컬 기준이라,
  UTC- 지역에서 요일과 요약 날짜가 하루 앞으로 밀렸다 (LA 월~금 4 영업일이 3, 토~월 0 이 1, 요약 10/4~10/8).
- 수정: `parseDateInputValue` 로 로컬 자정 Date 를 만든다. 서머타임 경계의 23/25시간 날은 기존 `Math.round` 일수 계산이 흡수한다.
- 검증: 같은 node `vm` 방식으로 LA 3건 RED → GREEN. LA·뉴욕의 DST 종료·시작 구간, KST 회귀, 프리셋 6건 모두 PASS. 전체 스위트 804 failures 0.
