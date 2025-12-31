# Runner Application Layer

Business logic orchestration with services and custom exceptions.

## Services

### RunService
Manages run CRUD operations.

| Method | Description |
|--------|-------------|
| `getAllRuns()` | Get all runs ordered by date desc |
| `getRunById(Long id)` | Get single run, throws RunNotFoundException |
| `createRun(LocalDate, LocalTime, String, RunCategory)` | Create new run |
| `updateRun(Long id, ...)` | Update existing run |
| `deleteRun(Long id)` | Delete run |

### AttendanceService
Manages attendance records and rankings.

| Method | Description |
|--------|-------------|
| `getAttendancesByRunId(Long runId)` | Get all participants for a run |
| `registerAttendance(Long runId, String name, BigDecimal distance)` | Register attendance |
| `getTop10ByAttendanceCount()` | Top 10 by attendance count |
| `getTop10ByTotalDistance()` | Top 10 by total distance |
| `getAllMemberStats()` | Stats grouped by run category |
| `getAttendancesByParticipantName(String name)` | Get all runs for a participant |

### RunnerAdminService
Manages admin user accounts.

| Method | Description |
|--------|-------------|
| `createAdmin(String username, String rawPassword)` | Create admin (password encoded) |
| `getOrCreateAdmin(String username, String rawPassword)` | Get or create admin |
| `existsByUsername(String username)` | Check if admin exists |

## Exceptions

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| RunNotFoundException | 404 NOT_FOUND | Run not found with given id |
| DuplicateAttendanceException | 409 CONFLICT | Participant already registered for run |

Both extend `BusinessException` from common module.

## Transaction Patterns

- Default: `@Transactional(readOnly = true)` at class level
- Write operations: `@Transactional` at method level
- Services use constructor injection for repositories
