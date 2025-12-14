# Application Layer

Business logic orchestration layer between presentation and domain/infrastructure.

## Services

| Service | Purpose |
|---------|---------|
| `OwnerService` | Owner CRUD, getOrCreateOwner() for idempotent creation |
| `ScheduleService` | Schedule CRUD by (ownerId, year, month), auto-creates owner |
| `ParticipantService` | Participant management, selection updates, max 8 limit |

### Transaction Pattern
- Class-level: `@Transactional(readOnly = true)`
- Write methods override with `@Transactional`

### Key Methods

**OwnerService**
- `getOrCreateOwner(ownerId)` - Idempotent owner creation
- `getOwnerSchedules(ownerId)` - List all schedules

**ScheduleService**
- `getScheduleByOwnerAndYearMonth(ownerId, year, month)` - Throws if not found
- `findScheduleByOwnerAndYearMonth(...)` - Returns null if not found
- `createSchedule(ownerId, year, month, weeks)` - Creates owner if missing

**ParticipantService**
- `addParticipant(scheduleId, name)` - Validates limit (8) and duplicates
- `updateSelections(participantId, selections)` - Validates day range (1 to daysInMonth)

## Exceptions

All extend `BusinessException` with `code` and `HttpStatus`.

| Exception | HTTP Status | When Thrown |
|-----------|-------------|-------------|
| `OwnerNotFoundException` | 404 | Owner lookup fails |
| `ScheduleNotFoundException` | 404 | Schedule lookup fails |
| `ParticipantNotFoundException` | 404 | Participant lookup fails |
| `DuplicateScheduleException` | 409 | Schedule already exists for owner/year/month |
| `DuplicateParticipantException` | 409 | Participant name exists in schedule |
| `ParticipantLimitExceededException` | 409 | Schedule has 8 participants |
| `InvalidSelectionException` | 400 | Day outside valid range |
| `InvalidOwnerIdException` | 400 | Invalid owner ID format |

## Layer Interactions

```
Presentation → Application (Services)
                    ↓
              Domain (Repository interfaces, Entities)
                    ↓
           Infrastructure (Repository adapters)
```

Services depend on domain repository interfaces, not JPA implementations.
