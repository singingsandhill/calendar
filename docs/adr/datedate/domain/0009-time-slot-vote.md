# ADR-0009: 시간 투표 — "날짜 + 시간대" 후보 제안·투표, 후보 날짜는 선택된 날로 서버 강제

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-09-24 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 |
| 관련 커밋 | (시간 투표 신설 배치) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

일정 페이지(`/{ownerId}/{year}/{month}`)는 달력 가용일 선택 + 장소 투표 + 메뉴 투표로
구성돼 있었다. 모임 날짜는 모이지만 "몇 시에 만날지" 정할 수단이 없어 결정의 마지막 단계가
페이지 밖(메신저)으로 새어 나갔다.

요청(2026-09-24): 장소·메뉴 투표와 **같은 양식**의 시간 투표, 참여자가 **선택한 날 기반**으로
시간을 고르고, **겹치는 시간**을 시각화.

설계 시점 제약:

1. 날짜는 `Participant.selections` 의 **dayIndex** 로 표현된다 — 7주 확장 모드는 그리드 시작
   (1일이 속한 주의 일요일)부터의 1~49 인덱스, 레거시 모드는 해당 월 일자. 달력 셀은 JS
   (`schedule/calendar.js`)가 이 인덱스로 렌더한다.
2. `/api/**` 는 무인증 공개 + CORS 허용(ADR common/security/0002)이라 UI 제약은 API 로 우회된다.
3. 장소·메뉴 투표는 "후보 엔티티 + 이름 문자열 투표 자식 테이블" 구조(`Location`/`Menu`)다.

## Decision — 무엇을 골랐나

시간 후보(`TimeSlot`)를 장소·메뉴와 같은 구조로 추가하고, 후보 날짜는 "참여자 한 명 이상이
저장한 날"로 서버가 강제한다.

- **모델:** `TimeSlot(dayIndex, startMinute, endMinute, voters)` + `time_slot_votes` 자식 테이블.
  투표는 이름 기반·대소문자 무시 중복 금지(`Location` 과 동일).
- **표현:** 날짜는 `selections` 와 같은 dayIndex, 시간은 자정 기준 분(int). **30분 단위,
  00:00~24:00, 자정 넘김 없음(end > start)** — 도메인 생성자가 검증(`IllegalArgumentException` → 400).
- **선택된 날 강제:** `Schedule.hasAvailabilityOn(dayIndex)` 가 false 면 `InvalidTimeSlotException`(400).
  같은 날짜·시작·종료 후보는 `DuplicateTimeSlotException`(409). 후보 생성 뒤 그 날이 모든 참여자
  선택에서 빠져도 후보는 남는다(자동 삭제 없음).
- **겹침 정의:** 30분 칸마다 "그 칸을 덮는 후보에 투표한 서로 다른 사람 수"(대소문자 무시).
  최대 인원 칸을 **같은 사람 집합**끼리 연속 구간으로 묶어 "최다 겹침 19:00–21:00 · 4명" 으로 요약.
- **렌더링:** 섹션 뼈대만 SSR, 목록·겹침 차트는 `schedule/timeslots.js` 가
  `window.SCHEDULE_DATA.timeSlots` 로 렌더. 일정 삭제는 `ScheduleJpaEntity.timeSlots` cascade 로
  후보·투표를 함께 지운다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 개인별 가능 시간 칠하기 (When2meet 형) | 개인 가용성은 정밀 | 장소·메뉴 투표 양식과 다름 — 요청자가 "후보 제안 + 투표" 를 선택 |
| 날짜를 `LocalDate` 로 저장 | 자기서술적, 주 수 변경에 강함 | "선택된 날" 판정마다 selections 인덱스와 변환 필요 — 인덱스 체계 이중화 |
| 시간을 `LocalTime` 으로 저장 | DB 가독성 | 24:00(하루 끝) 표현 불가 |
| UI 에서만 날짜 제한 | 서버 코드 0 | 공개 API 로 우회 가능. 서버 강제 비용이 도메인 메서드 1개로 작음 |
| 목록 SSR (장소·메뉴와 동일) | no-JS 표시 | dayIndex→날짜 라벨·요일을 Java/JS 양쪽에 두게 됨. 달력 자체가 JS 전용이라 no-JS 이득 없음 |
| 인접 칸을 인원 수만 같으면 병합 | 구현 단순 | 19:00 {A,B} 와 19:30 {C,D} 가 한 구간·첫 칸 이름으로 표시되는 오표시 |
| **(선택) dayIndex + 분 int + 서버 강제 + JS 렌더** | 기존 인덱스·투표 구조 재사용 | — |

범위에서 뺀 것: 후보 삭제 API/UI(장소·메뉴도 UI 없음), 로그인 활동 기록(`UserActivity`) —
Recap 은 `LOCATION_VOTE`/`MENU_VOTE` 만 집계해 기록해도 보이지 않는다, 인기 순위·인사이트 편입.

## Consequences — 영향

- **긍정:** 날짜 → 시간 → 장소 → 메뉴로 결정 흐름이 한 페이지에서 끝난다. 투표 UX·마크업
  (`.location-item`, `toggleVoteFor`)을 재사용해 학습 비용이 없다.
- **부정/트레이드오프:**
  - dayIndex 는 주 수 변경(7주 확장 ↔ 레거시)으로 의미가 바뀐다 — `selections` 가 이미 가진
    한계를 그대로 공유한다.
  - 잘못 만든 후보는 지울 수 없다(장소·메뉴와 동일).
- **후속:** Recap 에 시간 투표를 넣으려면 `ActivityType` 추가 + `RecapService` 집계 변경이 함께 필요.
- **회귀 가드:** `TimeSlotTest`(범위·30분 단위), `ScheduleTest.hasAvailabilityOn_*`,
  `TimeSlotServiceTest`(400/409/404), `TimeSlotApiControllerTest`,
  `TimeSlotPersistenceIntegrationTest.deleteSchedule_cascadesToTimeSlotsAndVotes`(cascade 누락 시 FK 위반으로 RED),
  `ScheduleTimeSlotRenderingTest`(SCHEDULE_DATA 주입·ko/en 키).

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/datedate/domain/timeslot/TimeSlot.java`,
  `datedate/domain/schedule/Schedule.java` (`hasAvailabilityOn`),
  `datedate/application/service/TimeSlotService.java`,
  `src/main/resources/static/js/schedule/timeslots.js`
- 관련 ADR: [0001 Schedule 애그리거트 불변식](0001-schedule-aggregate-invariants.md),
  [0002 Selection JSON 컨버터](0002-selections-json-converter.md),
  [frontend/0001 ES 모듈 분해](../frontend/0001-es-module-decomposition.md)
