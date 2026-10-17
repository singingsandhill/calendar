# 일정 설명(description) 필드 — 설계

| 항목 | 값 |
|---|---|
| 작성일 | 2026-10-11 (설계 대화 2026-10-10) |
| 배경 | use-case 문구 ko 13곳·en 12곳이 "페이지 설명(description)에 적으세요" 라고 안내하지만 일정에 설명 필드가 없다. 스터디 FAQ a3 는 없는 "일정 제목" 을 안내한다 |
| 사용자 결정 | 필드를 실제로 만든다 (문구만 고치는 안 대신) |
| 결정 기록 | ADR datedate/domain/0011 (구현 때 작성) |

## 1. 결정 (설계 대화의 질문과 답)

| 질문 | 답 |
|---|---|
| 누가 쓰고 고치나 | 링크를 아는 누구나 — 참여자·투표·일정 삭제와 같은 링크 공개 모델 (소유자 인증 없음) |
| URL 링크 | `http(s)://` 만 자동 링크. 본문은 전부 이스케이프, 새 탭 + `rel="nofollow ugc noopener noreferrer"` |
| 제목 필드 | 만들지 않음 — 스터디 FAQ a3 를 "설명 첫 줄" 로 고친다 |
| 미니앱 | 읽기 전용 표시. 앱인토스 외부 링크 제한 정책 때문에 URL 은 평문 (메뉴 URL 과 같은 처리) |
| 구현 방식 | A안 — 링크 판별을 서버 한 곳(JUnit 테스트되는 Java)에서 하고, 조각 목록을 응답에 실어 웹은 Thymeleaf `th:text`/`th:href` 로만 그린다 |

기각: B안(웹 JS·미니앱 TS 가 각자 링크 판별 — 보안 로직 두 벌, 웹은 JS 테스트 도구 없음), C안(마크다운 — 요구 없음, HTML 정제기 위험).

## 2. 데이터·도메인·API

- **컬럼:** `ScheduleJpaEntity.description` `@Column(length = 500)`, null 허용. `ddl-auto: update` 가 추가 — 기존 행 null, 마이그레이션 없음, 롤백 시 옛 코드는 무시.
- **어댑터:** `toDomain` 이 읽고, `save` 의 기존 엔티티 갱신 분기가 `weeks` 와 함께 `description` 도 반영.
- **도메인:** `Schedule.description` + `getDescription()`. 전체 생성자 마지막 인자. `Schedule.MAX_DESCRIPTION_LENGTH = 500`.
  `changeDescription(String)` — strip, 빈 값·null → null(지우기), 500자 초과 → `IllegalArgumentException`(400).
- **API:** 기존 `PATCH /api/owners/{id}/schedules/{y}/{m}` 확장. `ScheduleUpdateRequest(Integer weeks, @Size(max = 500) String description)`.
  필드가 없으면(null) 그대로, `""` 면 지우기. `ScheduleService.updateSchedule(ownerId, year, month, weeks, description)` 는 바뀐 것이 있을 때만 저장.
  응답은 기존 `ScheduleResponse`. 주 수만 보내는 기존 호출은 동작 동일.
- **조회 응답 (`ScheduleDetailResponse`):** `String description`(원문, 편집 상자 채우기) + `List<TextSegment> descriptionSegments`(없으면 빈 목록). JSON 필드 추가라 하위 호환.

## 3. 링크 판별

- `presentation/support/DescriptionLinks.split(String) → List<TextSegment>` 순수 함수. `TextSegment(String text, String url)` — `url == null` 이면 평문.
- 매치: `http://`·`https://`(대소문자 무시) + RFC 3986 URL 문자(ASCII)가 이어지는 구간. 한글 조사가 붙어도("https://x.so/a에서") 조사 앞에서 끊긴다.
  한글이 그대로 들어간 주소는 한글 앞에서 잘린다(퍼센트 인코딩은 정상) — 감수.
- 끝의 `. , ; : ! ? '` 와 짝이 맞지 않는 `)` 는 링크에서 뺀다. `javascript:`·`www.` 는 링크가 아니다. 링크 글자는 URL 그대로.

## 4. 웹 화면 (`schedule/view.html`)

- 일정 헤더 바로 아래 카드. 설명이 있으면 조각을 `th:text`(평문)·`th:href`+`th:text`(링크)로 그린다 — `th:utext`·innerHTML 없음.
  `white-space: pre-line`, 긴 URL 줄넘김. "편집" 버튼. 설명이 없으면 "+ 설명 추가" 버튼만.
- 편집: textarea(maxlength 500, 서버가 원문으로 채움) + "n / 500" + 저장·취소. 저장 → `PATCH {description}` → 성공 시 새로 고침, 실패 시 서버 메시지 토스트. 빈 내용 저장 = 지우기.
- JS: `static/js/schedule/description.js`, `main.js` 액션 `description.edit` / `description.cancel` / `description.save`,
  `api.js` 에 `updateScheduleDescription(ownerId, year, month, description)`, `dataLayer` 이벤트 `description_saved`.
- 메시지 키(ko/en): `schedule.description.title` · `.add` · `.edit` · `.save` · `.cancel` · `.placeholder` · `.count`.
- CSS: `style.css` 에 작은 블록(카드·pre-line·링크 줄넘김) — 파일 내 개행 혼재라 바이트 삽입.

## 5. 미니앱 (`/mnt/d/projects/datedate-app/DateDate`)

- `types.ts` `description?: string | null` (배포 전 서버엔 없음).
- 일정 상세 화면에 설명 카드 — 줄바꿈 유지, URL 평문. vitest 컴포넌트 테스트 + `tsc`. 커밋하지 않음(그 레포의 스테이징된 작업 위).

## 6. 문구·개인정보

- ko 13곳의 "description" → 화면 라벨과 같은 "설명" (en 은 그대로 "description").
- 스터디 FAQ a3(ko/en): "일정 제목에 'R3 정규'" → "설명 첫 줄에 'R3 정규'".
- 개인정보 처리방침 수집 정보(사용자 입력)에 **일정 설명**과 **시간 투표 후보**(시간 투표 도입 때 누락) 추가, `privacy.updated` 날짜 갱신 (ko/en).

## 7. 테스트

- 도메인 `changeDescription`: strip, 빈 값·null→null, 500 통과·501 거부.
- `DescriptionLinksTest`: 일반·한글 조사·괄호·끝 문장부호·여러 링크·줄바꿈·`www.`·`javascript:`·빈 입력·대소문자 스킴.
- 서비스: 설명만 수정 저장, 주 수만 수정 시 설명 유지, 아무것도 안 바뀌면 저장 안 함.
- API(`@WebMvcTest`): PATCH 설명 200, 501자 400 `VALIDATION_ERROR`.
- 통합(`@SpringBootTest`+MockMvc): PATCH 후 재조회 유지, `<script>` 이스케이프, 링크 `rel`·`target`, 빈 설명이면 "+ 설명 추가", en 키 누락 없음, JSON `description`·`descriptionSegments`.
- 웹 JS 편집 흐름: 하네스(가짜 api) + 헤드리스 Chrome — 편집 열기, 글자 수, 저장 시 PATCH 인자, 취소.
- 미니앱: 컴포넌트 테스트(텍스트·줄바꿈·URL 평문), `tsc`.

## 8. 문서·커밋

- ADR datedate/domain/0011, ADR README(매트릭스·시간순·폴더 수), 루트 CLAUDE.md DateDate 목록, `datedate/application/CLAUDE.md`, ERD 두 그림 schedules 표에 `description` 행.
- 커밋 로그: 미실행 Commit 197(참여자 한도 10명)과 공유 파일 10개가 겹쳐, 따로 나누면 197 단독 빌드가 깨진다 — 197 하나로 합쳤다(구현 후 결정, 주의 주석).

## 9. 범위 밖

생성 모달의 설명 입력, 카카오톡 미리보기(og:description) 노출, 대시보드 카드 표시, 미니앱 편집·링크 이동, 소유자 전용 편집(인증 경계).
