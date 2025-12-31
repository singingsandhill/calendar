# Infrastructure Layer

Persistence implementation using Spring Data JPA with Adapter pattern.

## Structure

```
infrastructure/
├── config/
│   ├── JpaConfig.java      # @EnableJpaRepositories
│   └── WebConfig.java      # WebMvcConfigurer (empty)
└── persistence/
    ├── adapter/            # Domain ↔ JPA bridge
    ├── converter/          # Type converters
    ├── entity/             # JPA entities
    └── repository/         # Spring Data interfaces
```

## JPA Entities

### OwnerJpaEntity
- **Table**: `owners`
- **PK**: `ownerId` (String, natural key)
- **Cascade**: ALL + orphanRemoval to schedules

### ScheduleJpaEntity
- **Table**: `schedules`
- **PK**: `id` (Long, auto-generated)
- **Unique**: `(owner_id, year, month)`
- **FK**: `owner_id` → owners (LAZY)
- **Cascade**: ALL + orphanRemoval to participants
- **Note**: `year` and `month` columns escaped with backticks for MySQL

### ParticipantJpaEntity
- **Table**: `participants`
- **PK**: `id` (Long, auto-generated)
- **FK**: `schedule_id` → schedules (LAZY)
- **Columns**: name (10), color (7), selections (100 - JSON string)

## Repository Adapters

Implement domain repository interfaces, convert between domain and JPA entities.

| Adapter | Implements |
|---------|------------|
| `OwnerRepositoryAdapter` | `OwnerRepository` |
| `ScheduleRepositoryAdapter` | `ScheduleRepository` |
| `ParticipantRepositoryAdapter` | `ParticipantRepository` |

### Conversion Pattern
```java
// Domain → JPA
private Entity toEntity(Domain domain) { ... }

// JPA → Domain (recursive for aggregates)
private Domain toDomain(Entity entity) { ... }
```

## Converter

### SelectionConverter
Converts `List<Integer>` ↔ JSON String for participant selections.
- `toJson([1, 2, 3])` → `"[1,2,3]"`
- `fromJson("[1,2,3]")` → `[1, 2, 3]`

## JPA Repositories

| Repository | Custom Queries |
|------------|----------------|
| `OwnerJpaRepository` | (none - uses JpaRepository defaults) |
| `ScheduleJpaRepository` | `findByOwnerIdAndYearMonth`, `findAllByOwnerId` (sorted), `existsByOwnerIdAndYearMonth` |
| `ParticipantJpaRepository` | `findAllByScheduleId`, `countByScheduleId`, `existsByScheduleIdAndName` (case-insensitive) |

## Database Configuration

**Development** (application.yaml):
- H2 file-based: `jdbc:h2:file:./data/scheduledb`
- DDL: `update`
- H2 Console: `/h2-console`

**Test**:
- H2 in-memory: `jdbc:h2:mem:testdb`
- DDL: `create-drop`
