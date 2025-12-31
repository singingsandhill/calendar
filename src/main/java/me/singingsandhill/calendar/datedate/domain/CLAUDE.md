# Domain Layer

Pure domain models following DDD principles. No framework dependencies.

## Aggregates

### Owner (`owner/`)
- **Identity**: `ownerId` (String, 2-20 chars)
- **Validation**: Pattern `^[a-z0-9-]+$` (lowercase, numbers, hyphens only)
- **Contains**: List of Schedules
- **Methods**: `addSchedule()`, `getScheduleCount()`

### Schedule (`schedule/`)
- **Identity**: `id` (Long, DB-generated)
- **Properties**: ownerId, yearMonth, weeks, createdAt
- **Contains**: List of Participants (max 8)
- **Methods**: `canAddParticipant()`, `addParticipant()`, `hasParticipantWithName()`, `getDaysInMonth()`, `getFirstDayOfWeek()`
- **Constant**: `MAX_PARTICIPANTS = 8`

### Participant (`participant/`)
- **Identity**: `id` (Long, DB-generated)
- **Properties**: scheduleId, name (max 10 chars), color, selections, updatedAt
- **Methods**: `updateSelections(selections, daysInMonth)`, `getColorHex()`
- **Mutable**: selections, updatedAt

## Value Objects

### YearMonth (`schedule/YearMonth.java`)
Java Record for year/month encapsulation.
- **Validation**: year (2024-2100), month (1-12)
- **Methods**: `getDaysInMonth()`, `getFirstDayOfWeek()`, `calculateWeeks()`
- **Factory**: `YearMonth.of(year, month)`

### ParticipantColor (`participant/ParticipantColor.java`)
Java Record for hex color codes.
- **Validation**: Pattern `^#[0-9A-Fa-f]{6}$`
- **Preset Colors**: 8 colors (#E74C3C, #3498DB, #2ECC71, #F39C12, #9B59B6, #1ABC9C, #E67E22, #34495E)
- **Factory**: `ParticipantColor.ofIndex(int)` - Wraps around using modulo

## Repository Interfaces

| Interface | Key Methods |
|-----------|-------------|
| `OwnerRepository` | `findById`, `save`, `existsById` |
| `ScheduleRepository` | `findById`, `findByOwnerIdAndYearMonth`, `findAllByOwnerId`, `save`, `delete`, `existsByOwnerIdAndYearMonth` |
| `ParticipantRepository` | `findById`, `findAllByScheduleId`, `save`, `delete`, `countByScheduleId`, `existsByScheduleIdAndName` |

## Validation Summary

| Entity | Rule |
|--------|------|
| Owner ID | 2-20 chars, `^[a-z0-9-]+$` |
| Participant Name | 1-10 chars, non-blank |
| YearMonth | Year 2024-2100, Month 1-12 |
| ParticipantColor | Hex format `#RRGGBB` |
| Selections | Day 1 to daysInMonth |

## Patterns

- **Immutable collections**: `Collections.unmodifiableList()` for getters
- **Defensive copying**: `new ArrayList<>(input)` in constructors
- **Factory methods**: Static `of()` methods for value objects
- **Fail-fast validation**: Constructor-level validation with `IllegalArgumentException`
