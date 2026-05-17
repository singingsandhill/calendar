# Runner Application Layer

> 결정 근거: [`docs/adr/runner/`](../../../../../../../../docs/adr/runner/) —
> anonymous 생성 vs 어드민 삭제 권한 분리.

## Services

- **RunService** - Run CRUD (date, time, location, category)
- **AttendanceService** - Register attendance with distance, rankings (top 10 by count/distance), member stats by category
- **RunnerAdminService** - Admin account management (password encoded)

## Exceptions

- `RunNotFoundException` (404), `DuplicateAttendanceException` (409) - both extend BusinessException

## Transaction Pattern

Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`.
