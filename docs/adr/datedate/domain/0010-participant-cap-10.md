# ADR-0010: 일정당 참여자 한도 8 → 10명, 한도 값은 서버가 내려준다

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-10 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 197 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

- 사용자 결정(2026-10-07): 일정 하나의 최대 참여 인원을 8명에서 10명으로 늘린다.
- 한도 검사는 `Schedule.MAX_PARTICIPANTS` 한 곳([ADR-0001](0001-schedule-aggregate-invariants.md))이지만, 숫자 8 이 다른 곳에 복제돼 있었다.
  - 웹 일정 페이지: 추가 버튼 `th:disabled="${…size() >= 8}"`, 카운터 메시지 `{0} / 8명`.
  - 앱인토스 미니앱(별도 레포 `datedate-app`): `canAdd = participants.length < 8`.
  - 문구: 홈·가이드·FAQ·소개·use-case 의 "최대 8명" — ko 16키, en 15키.
  상수만 바꾸면 서버는 10명을 받는데 웹 버튼은 8명에서 잠기고 카운터는 "9 / 8명" 이 된다. 이 복제는 어떤 테스트도 잡지 못했다.
- 한도 초과 409 메시지는 영문 고정(`messageKey` 없음)이었다. 웹 `api.js` 의 한국어 매핑은 키가 `MAX_PARTICIPANTS_EXCEEDED` 라
  서버 코드 `PARTICIPANT_LIMIT_EXCEEDED` 와 맞지 않아 한 번도 쓰이지 않았다.

## Decision — 무엇을 골랐나

일정당 참여자 한도를 **10명**으로 올리고, 클라이언트는 한도 숫자를 서버에서 받는다.

- `Schedule.MAX_PARTICIPANTS = 10` — 검사 위치(애그리거트)는 ADR-0001 그대로.
- `ScheduleDetailResponse.maxParticipants` 추가. 웹 뷰 모델과 일정 상세 API(`GET /api/owners/{id}/schedules/{y}/{m}`)가 같은 DTO 라
  템플릿(추가 버튼·카운터 `{0} / {1}명`)과 미니앱이 이 값 하나를 본다.
- `ParticipantLimitExceededException` 에 `errors.participantLimitExceeded` 키 + 한도 인자 — 409 메시지를 요청 로케일로 해석한다
  (`OwnerIdTakenException` 과 같은 패턴). 쓰이지 않던 `api.js` 매핑은 삭제.
- 한도를 숫자로 말하는 문구 키 목록을 `ParticipantCapCopyTest` 가 상수와 대조한다.
- 참여자 색 팔레트도 10색으로 늘린다 — 별도 결정 [ux/0004](../ux/0004-ten-color-participant-palette.md).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 상수만 10 으로 | 1줄 변경 | 웹 버튼이 8명에서 잠기고 카운터가 "9 / 8명" — 테스트로 안 잡힘 |
| 템플릿·메시지·미니앱에 10 을 각각 적기 | DTO 변경 없음 | 다음 변경 때 같은 누락이 반복된다 (이번에 4곳) |
| DTO 에 `canAddParticipant` 불리언도 노출 | 뷰가 규칙을 재구현하지 않음 | 카운터에 숫자가 어차피 필요 — 필드 하나로 충분하고 최종 강제는 서버 |
| **(선택) 서버 상수 → DTO `maxParticipants` → 템플릿·미니앱** | 숫자 원천 1곳, JSON 필드 추가라 하위 호환 | — |

## Consequences — 영향

- **긍정:** 한도를 다시 바꿀 때 서버 상수와 가드 테스트가 가리키는 문구만 고치면 된다. 409 메시지가 ko/en 으로 나간다
  (미니앱은 서버 `message` 를 그대로 보여준다).
- **부정:** 문구 키는 여전히 사람이 고친다 — 대신 `ParticipantCapCopyTest` 가 빠뜨린 키를 빌드에서 막는다.
- **유지되는 한계:** 한도 검사는 락 없이 메모리에서 한다 — 동시에 추가하면 한도를 넘을 수 있다 (8명 때와 같은 기존 동작, 이번에 바꾸지 않음).
- **배포·롤백:** DB 변경 없음. 일정 로드 시 인원을 검증하지 않으므로 롤백해도 9~10명 일정은 정상 표시된다(추가만 막힘).
  미니앱은 `maxParticipants` 가 없으면 8 로 폴백하므로 배포 순서에 묶이지 않는다.
- **후속:** 미니앱 `canAdd` 를 `maxParticipants` 기반으로 바꾼다 (별도 레포·앱인토스 심사).

## References

- 관련 코드: `datedate/domain/schedule/Schedule.java`, `datedate/presentation/dto/response/ScheduleDetailResponse.java`,
  `templates/schedule/view.html`, `datedate/application/exception/ParticipantLimitExceededException.java`
- 테스트: `ScheduleTest`, `ScheduleParticipantCapRenderingTest`, `ParticipantCapCopyTest`, `GlobalExceptionHandlerI18nTest`
- 계획: `docs/superpowers/plans/2026-10-07-participant-cap-10.md`
