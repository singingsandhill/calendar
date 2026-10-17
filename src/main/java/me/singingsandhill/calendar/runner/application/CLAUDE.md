# Runner Application Layer

> 결정 근거: [`docs/adr/runner/`](../../../../../../../../docs/adr/runner/) —
> anonymous 생성 vs 어드민 삭제 권한 분리.

## Services

- **RunService** - Run CRUD (date, time, location, category). `getAllRuns(from, to)` 는 optional 날짜 범위 필터 (null=무제한, 서비스에서 EPOCH/9999-12-31 sentinel 정규화 후 BETWEEN 쿼리)
- **AttendanceService** - Register attendance with distance, rankings (top 10 by count/distance), member stats by category. `getAllMemberStats(from, to)` 도 같은 optional 날짜 범위 계약 (run.date 조인 기준)
- **RunnerAdminService** - Admin account management (password encoded)

## Exceptions

- `RunNotFoundException` (404), `AttendanceNotFoundException` (404),
  `DuplicateAttendanceException` (409) — all extend BusinessException
  (`application/exception/`)

## Transaction Pattern

Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`.
