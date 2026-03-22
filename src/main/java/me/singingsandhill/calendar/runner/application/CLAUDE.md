# Runner Application Layer

## Services

- **RunService** - Run CRUD (date, time, location, category)
- **AttendanceService** - Register attendance with distance, rankings (top 10 by count/distance), member stats by category
- **RunnerAdminService** - Admin account management (password encoded)

## Exceptions

- `RunNotFoundException` (404), `DuplicateAttendanceException` (409) - both extend BusinessException

## Transaction Pattern

Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`.
