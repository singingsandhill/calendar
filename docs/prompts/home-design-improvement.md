# 프롬프트: datedate.site 홈 화면 디자인 개선

아래 내용을 Claude Code에 그대로 붙여넣어 사용.

---

datedate.site(DateDate) 홈 화면의 디자인/UX를 개선해줘. 대상 파일은 주로
`src/main/resources/templates/index.html`, `src/main/resources/templates/fragments/header.html`,
`src/main/resources/static/css/style.css`, `src/main/resources/messages.properties`,
`src/main/resources/messages_en.properties`야. 작업 전에 이 파일들과 루트 `CLAUDE.md`를 먼저 읽고 시작해.

## 1. 히어로 소개 문구 — 가독성 + 카피 개선

- `.hero-fullscreen h1`은 `letter-spacing: -0.02em`, `.hero-keyword`는 `font-size: 0.38em`으로
  한글 자간이 좁고 답답함. 한글에는 음수 자간을 쓰지 말고 `0` 또는 소폭 양수로 조정하고,
  `.subtitle`/`.hero-description`의 자간·행간(line-height 1.7 이상)·최대 폭을 재조정해.
- 히어로/본문 텍스트 전반에 `word-break: keep-all` + `overflow-wrap: break-word`를 적용해
  한글 단어 중간 줄바꿈을 막아 (현재 style.css에 keep-all이 한 곳(1935행 부근)에만 있음).
- 카피 개선: `index.hero.subtitle`의 "여러명이서 쉽게 날짜 조율하기"는 어색함
  ("여러명이서"는 비표준 표현). 서비스 가치가 드러나는 자연스러운 문장으로 다듬어
  (예: "모두의 되는 날을 한눈에" 같은 톤 — 최종 문안은 네가 더 낫게 제안).
  `index.hero.keyword`, `index.hero.description`도 함께 검토하고, 영어(messages_en.properties)도
  같은 톤으로 맞춰.
- messages.properties 전체에서 오탈자를 점검해. 최소한 `index.feature2.desc`의
  "격치는 날짜" → "겹치는 날짜"는 확인 후 수정.

## 2. 랜덤 생성 / 시작하기 버튼 — 사용성 개선 (모바일 포함)

현재 `#start-form`의 문제:

- "랜덤 생성"이 `.btn-text`(회색 플레인 텍스트)라 버튼인지 알기 어려움. 아이콘(주사위/새로고침) +
  고스트/아웃라인 버튼 스타일로 어포던스를 줘.
- "시작하기"(`.btn-spring`)가 입력 전 disabled 상태로 시작해 회색으로 보여 CTA가 죽어 있음.
  disabled 스타일을 유지하되 클릭 시 이유를 안내하거나, 활성화 조건을 시각적으로 명확히 해.
- `.form-actions-minimal`이 `space-between`이라 두 버튼의 위계가 없음. "시작하기"를 명확한
  주 CTA(크고 진하게), "랜덤 생성"을 보조 액션으로 위계를 나눠.
- 모바일(≤480px): 버튼을 풀폭 또는 세로 스택으로 배치하고 터치 타겟 최소 44×44px 보장.
  입력창 폰트는 iOS 자동 줌 방지를 위해 16px 이상 유지.
- 랜덤 생성 시 생성된 ID가 바뀌었다는 걸 알 수 있게 짧은 시각 피드백(하이라이트 등)을 추가하면 좋음.

## 3. 카카오 로그인 — 모바일 내비게이션 자연스럽게

- `.nav-kakao-login`은 style.css 4782행에 한 줄짜리 스타일(#FEE500 배경)만 있음.
  카카오 공식 버튼 가이드에 맞게 심볼(말풍선) 포함 여부, 높이, 라운드, hover/active 상태를 정리해.
- 모바일 햄버거 메뉴(`.nav-menu`) 안에서 일반 링크들 사이에 노란 버튼이 뜬금없이 끼어 있는
  형태를 개선해: 메뉴 하단에 구분선과 함께 풀폭 버튼으로 배치하는 등 자연스러운 위치/크기로.
  로그인 후 프로필(닉네임+이미지)·로그아웃도 모바일 메뉴에서 같은 그룹으로 정리.
- `fragments/header.html`의 `header`와 `header-minimal` 두 프래그먼트 모두에 반영.

## 4. Use case 카드 4개 → 밸런스 맞추기

- `UseCaseSlugs.ALL`에는 5개 슬러그(friend-meetup, team-meeting, travel-planning, study-group,
  club-activity)가 있는데 홈 `.scenarios-grid`에는 4개만 노출됨. **동호회 모임(club-activity) 카드를
  추가해 5개로 만들고**, 그리드가 어색하지 않게 레이아웃을 조정해
  (예: 데스크톱 3+2 배치, 태블릿 2열, 모바일 1열 — `auto-fit, minmax(320px,1fr)` 그대로면
  5개일 때 마지막 행이 어정쩡하니 명시적 breakpoint로 제어).
- 새 카드 문구는 기존 카드 톤에 맞춰 `index.scenario.club.*` 키로 ko/en 모두 추가.

## 5. 그 외 스스로 찾아서 개선할 것

홈 화면을 처음 보는 사용자 관점에서 전체를 점검하고, 발견한 문제를 모두 고쳐. 최소한 다음은 확인:

- **인기 장소/메뉴 섹션**: 사용자 입력이 그대로 노출됨("ㅈㄴ맛있는거", "시간입력 0표" 등).
  홈 첫 화면 품질을 해치므로 0표 항목 제외, 최소 표수 기준, 비속어 필터 등 노출 기준을
  `PopularityService` 쪽에서 정리 (표시 로직 변경이면 CLAUDE.md 규칙에 따라 ADR 필요 여부 판단).
- **통계 스트립**: 숫자에 천단위 포맷/카운트업 등 시각적 다듬기 (i18n의 `{n,number,#}` 규칙 주의 —
  연도가 아닌 통계 수치는 그룹화해도 됨).
- **FAQ**: 답변 텍스트 오탈자 점검("장소실메뉴", "확인실관리"처럼 가운뎃점(·)이 깨진 곳이 있는지
  messages.properties 인코딩 확인).
- **일관성**: 섹션 간 여백 리듬, 카드 radius/그림자, hover 상태 통일. `prefers-reduced-motion`
  대응 여부 확인.
- **접근성**: 색 대비(회색 텍스트 vs 밝은 배경), 포커스 링, aria 속성 유지.
- 히어로가 fullscreen이라 모바일에서 폼이 첫 화면에 안 들어오는지 확인하고, 들어오도록 높이 조정.

## 제약 조건 (중요)

- CLAUDE.md의 i18n 규칙 준수: 인자 없는 메시지에 `''`(이중 작은따옴표) 금지, ko/en 두 파일 항상
  동시 수정, `messages.properties`는 유니코드 이스케이프 유지.
- 문구 변경 시 SEO 메타(`SeoService`)와 어긋나지 않는지 확인. `SeoServiceI18nTest` 등 기존 테스트 통과.
- 정책/구조 변경(예: 인기 항목 노출 기준)이 생기면 CLAUDE.md의 ADR 동기화 규칙 적용.
- 다른 모듈(runner/trading/stock) 화면은 건드리지 마.

## 검증

1. `./gradlew test` 통과 (WSL이면 `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"`).
2. `bootRun` 후 http://localhost:8081 을 데스크톱(1280px)과 모바일(375px) 뷰포트에서 확인 —
   히어로 문구 줄바꿈, 버튼 배치, 햄버거 메뉴 내 카카오 로그인, use case 5카드 그리드.
3. `?lang=en`으로 영어 화면도 동일하게 확인.
4. 변경한 메시지 키가 ko/en 양쪽에 모두 존재하는지 diff로 확인.

작업은 항목별로 커밋을 나눠서 진행해.
