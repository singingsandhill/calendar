# ADR-0011: 일정 설명 필드 — 링크 공개 편집, 링크 판별은 서버 한 곳

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-11 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 197 (참여자 한도 10명과 합본 — 공유 파일 때문) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

- use-case 문구 ko 13곳·en 12곳이 "페이지 설명(description)에 회의 길이·규칙·링크를 적으세요" 라고 안내했지만 일정에는 설명 필드가
  없었다(`Schedule` = id·owner·연월·주 수·생성일·참여자). 스터디 FAQ 는 없는 "일정 제목" 까지 안내했다.
- 문구만 고치는 안과 필드를 만드는 안 중 사용자가 필드를 택했다(2026-10-10).
- 일정은 소유자 인증이 없는 링크 공개 모델이다 — 참여·투표·일정 삭제까지 링크를 아는 누구나 한다.
- 설명에는 Notion·단톡방 같은 링크가 들어가는데, 사용자 입력 HTML 을 그대로 그리면 XSS 이고, 웹 JS 에는 테스트 도구가 없다.

## Decision — 무엇을 골랐나

일정에 설명 하나(최대 500자)를 두고, 링크 판별은 서버 Java 한 곳에서 해 조각 목록으로 내려준다.

- `Schedule.description` + `changeDescription(String)` — strip, 빈 값·null → null(지우기), 500자(`MAX_DESCRIPTION_LENGTH`) 초과 거부.
  컬럼은 null 허용 `varchar(500)`, `ddl-auto: update` 가 추가(마이그레이션 없음).
- 편집은 기존 `PATCH /api/owners/{id}/schedules/{y}/{m}` 의 `description` — null 이면 그대로, `""` 면 지우기. 권한은 기존 PATCH 와 같은 링크 공개.
- `DescriptionLinks.split` 이 `http(s)://` + RFC 3986 ASCII 문자 구간만 링크로 나눈다(끝 문장부호·짝 없는 `)` 제외, 다른 스킴은 평문).
  `ScheduleDetailResponse` 가 원문 `description` 과 `descriptionSegments` 를 함께 내린다.
- 웹은 조각을 `th:text`/`th:href` 로만 그린다(`th:utext`·innerHTML 없음), 링크는 새 탭 + `rel="nofollow ugc noopener noreferrer"`.
  저장 후 새로 고침으로 서버 렌더를 다시 받는다.
- 앱인토스 미니앱은 읽기 전용이고 URL 도 평문(외부 링크 제한 정책 — 메뉴 URL 과 같은 처리).
- 제목 필드는 만들지 않는다 — 스터디 FAQ 는 "설명 첫 줄" 로 고쳤다. 개인정보 처리방침 수집 항목에 일정 설명(과 누락됐던 시간 후보)을 넣었다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 문구만 실제 가능한 방법으로 고치기 | 코드 변경 없음 | 사용자 결정 — 주최자 메모 자리가 실제로 필요 |
| 웹 JS·미니앱 TS 가 각자 링크 판별 | 서버 응답 단순 | 보안 로직 두 벌, 웹 쪽은 테스트 불가 |
| 마크다운 | 서식 표현 | 요구 없음, HTML 정제기 위험 |
| 소유자만 편집 | 장난 수정 차단 | 인증 경계를 새로 만들어야 함 — 로그인 안 하는 대다수 일정은 설명을 못 씀 |
| **(선택) 링크 공개 편집 + 서버 링크 판별 + 조각 응답** | 기존 권한 모델 그대로, 판별 로직 1곳(JUnit) | — |

## Consequences — 영향

- **긍정:** use-case 문구가 사실이 됐다. XSS 경로가 템플릿 이스케이프 하나로 닫힌다(`ScheduleDescriptionRenderingTest` 가 `<script>`·`javascript:` 를 확인).
- **부정:** 링크 공개라 누구나 설명을 지우거나 바꿀 수 있다(장소·메뉴 투표와 같은 수준). 장난 링크도 클릭 가능 — 일정 페이지는
  `noindex, nofollow` 이고 링크엔 `nofollow ugc` 를 붙였다. 한글이 그대로 들어간 주소는 한글 앞에서 끊긴다(퍼센트 인코딩은 정상).
- **배포·롤백:** 컬럼 추가뿐 — 옛 코드는 컬럼을 무시한다. JSON 필드 추가라 미니앱 구버전과 호환.
- **범위 밖:** 생성 모달 입력, 카카오톡 미리보기(og:description) 노출, 대시보드 표시, 미니앱 편집·링크 이동.

## References

- 관련 코드: `datedate/domain/schedule/Schedule.java`, `datedate/presentation/support/DescriptionLinks.java`,
  `datedate/presentation/dto/response/ScheduleDetailResponse.java`, `templates/schedule/view.html`, `static/js/schedule/description.js`
- 테스트: `ScheduleTest`, `DescriptionLinksTest`, `ScheduleServiceTest`, `ScheduleApiControllerTest`,
  `ScheduleDescriptionPersistenceTest`, `ScheduleDescriptionRenderingTest`, 미니앱 `ScheduleDescription.test.tsx`
- 설계: `docs/superpowers/specs/2026-10-11-schedule-description-design.md`
