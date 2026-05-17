# ADR-0002: Participant.selections = JSON 컨버터로 영속화 (List<Integer>)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04-20 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 / 영속성 |
| 관련 커밋 | `7012cc0` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

참가자의 가용일 선택(`selections: List<Integer>`) 을 어떻게 저장할지 두 가지 옵션이
있었다.

1. **별도 테이블 `participant_selection (participant_id, day_index)`** — 정규화. 49 일
   기준 평균 5–10개 선택, 참가자 수만큼 곱해진 row.
2. **`selections` 칼럼에 JSON 직렬화** — 비정규화. 한 row 한 컬럼.

기존 코드는 **양쪽이 섞여 있었다** — 서비스 일부가 정규화 테이블 가정, 일부가 JSON
가정. `datedate-architecture-review.md` 의 E-2-a (`SelectionConverter` JPA) 도 이
혼란 지적.

## Decision — 무엇을 골랐나

`AttributeConverter` 로 JSON 직렬화 — `Participant.selections` 단일 칼럼.

- **`SelectionConverter`** (`AttributeConverter<List<Integer>, String>`) — Jackson
  으로 직렬화/역직렬화.
- **`Participant.selections`** 필드 → DB 컬럼 1개.
- 별도 `participant_selection` 테이블 제거.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 정규화 테이블 | SQL 집계 쉬움 (특정 날짜 가용 인원 카운트) | 우리 도메인은 참가자별로 조회 → 늘 selections 전체를 함께 읽음. JOIN 비용 회피가 더 가치 있음 |
| jsonb 컬럼 (Postgres) | DB 함수로 부분 쿼리 | H2 file-based 환경에서 jsonb 미지원 |
| **(선택) JSON 문자열 + AttributeConverter** | H2/MySQL 호환, 직관적 | — |

가용 인원 카운트 같은 분석 쿼리는 인사이트 페이지(`InsightsService`) 에서 *참가자
전체를 메모리에 로드 후 집계* — JSON 이라도 충분.

## Consequences — 영향

- **긍정:**
  - 참가자 1행 = selections 1 컬럼 → 단순.
  - 도메인 객체에 `List<Integer>` 그대로 — 도메인 메서드 가독성.
- **부정:**
  - "특정 날짜를 선택한 참가자 N명" SQL 직접 쿼리 어려움 — 메모리 집계.
  - JSON 직렬화 실패 시 (스키마 변경) 마이그레이션 필요.
- **후속:**
  - ADR-0001 (Schedule 애그리거트) 가 selections 변경의 진입점을 도메인 메서드로 강제.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/converter/SelectionConverter.java`
  - `src/main/java/me/singingsandhill/calendar/datedate/domain/participant/Participant.java`
- 관련 docs: `docs/datedate-architecture-review.md` (E-2-a)
- 관련 커밋: `git log -1 7012cc0`
