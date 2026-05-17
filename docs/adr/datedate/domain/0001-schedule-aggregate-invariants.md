# ADR-0001: Schedule 애그리거트가 참가자 한도 / 중복 / 주차 변경 불변식 담당

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04-20 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 |
| 관련 커밋 | `7012cc0` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

`datedate-architecture-review.md` 의 Top 10 리스크 중 다수가 *비즈니스 규칙이
서비스 레이어에 분산* 되어 있다는 지적이었다. 예:

- 참가자 한도(예: 8명) 검증을 `ParticipantService` 가 직접 수행.
- 같은 이름의 참가자 중복 방지 로직이 *컨트롤러 + 서비스* 양쪽에서 분기.
- 주차(week) 변경 시 기존 selections 가 무효해지는 처리 부재 — `Schedule.updateWeek()`
  같은 의도된 진입점 없음.

결과: 새 호출자(예: 어드민 백오피스, 마이그레이션 스크립트) 가 추가될 때마다 같은
규칙을 재구현해야 하는 회귀 위험.

## Decision — 무엇을 골랐나

`Schedule` 애그리거트가 모든 불변식을 *직접* 담는다.

- **`Schedule.addParticipant(name, color, ...)`** — 한도 / 중복 검증을 애그리거트
  내부에서. 위반 시 `BusinessException` (`ScheduleException` 서브클래스) 발생.
- **`Schedule.changeWeek(weekStart)`** — 주차 변경 시 *기존 selections 무효화* 정책을
  애그리거트가 책임.
- **서비스 레이어** — 트랜잭션 경계 / 리포지토리 호출 / 도메인 메서드 위임만.
- 도메인 메서드가 던지는 예외는 ADR 0001 (common/error-handling) 의
  `BusinessException` 베이스 상속.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 서비스 레이어에 규칙 유지 | 코드 변경 적음 | 호출자별 규칙 누락 위험 (DDD 의 invariants 침해) |
| 별도 Validator 클래스 | 단일 책임 | 분리되어 호출자가 깜빡 |
| **(선택) 애그리거트 메서드 + 예외** | 호출 시점 강제 | — |

DDD 의 *애그리거트 = 일관성 경계* 원칙. 외부에서 setter 직접 호출 금지 → 도메인
메서드만 노출.

## Consequences — 영향

- **긍정:**
  - 새 호출자가 추가돼도 규칙 자동 적용.
  - 단위 테스트가 `Schedule` 한 클래스에 집중 가능.
- **부정:**
  - 도메인 메서드 시그니처가 늘어나 발견 비용 ↑.
  - 일부 *부분 갱신* (예: 단일 필드 수정) 케이스에서 도메인 메서드를 추가해야 함.
- **후속:**
  - ADR-0002 (Selection JSON 컨버터) 가 selections 의 영속성 단순화로 도메인 책임
    경계 더 명확.
  - ADR-0003 (자동 생성 제거) 가 *Schedule 미존재* 케이스의 진입점 분리.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/Schedule.java`
  - `src/main/java/me/singingsandhill/calendar/datedate/application/service/ScheduleService.java`
- 관련 docs: `docs/datedate-architecture-review.md` (E-2-c, E-2-b)
- 관련 커밋: `git log -1 7012cc0`
