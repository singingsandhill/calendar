# Runner Domain Layer

Pure domain models with no framework dependencies. Defines entities, repository interfaces (ports), and domain DTOs.

## Entities

### Run
Scheduled running event.

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | PK |
| date | LocalDate | NotNull |
| time | LocalTime | NotNull |
| location | String | Max 100 chars |
| category | RunCategory | NotNull |
| createdAt | LocalDateTime | Auto-generated |

### Attendance
Participant attendance record for a run.

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | PK |
| runId | Long | FK to Run |
| participantName | String | Max 50 chars |
| distance | BigDecimal | 0.1 - 100.0 km, 1 decimal |
| createdAt | LocalDateTime | Auto-generated |

**Unique Constraint**: (runId, participantName) - one attendance per participant per run

### Admin
Admin user for dashboard access.

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | PK |
| username | String | 3-50 chars, unique |
| password | String | NotBlank (encoded) |
| createdAt | LocalDateTime | Auto-generated |

## Enums

### RunCategory
```java
REGULAR("정규런"),    // Regular scheduled run
LIGHTNING("번개런")   // Lightning/spontaneous run
```

## Repository Interfaces (Ports)

### RunRepository
```java
Optional<Run> findById(Long id)
List<Run> findAll()
List<Run> findAllOrderByDateDesc()
Run save(Run run)
void deleteById(Long id)
boolean existsById(Long id)
```

### AttendanceRepository
```java
Optional<Attendance> findById(Long id)
List<Attendance> findByRunId(Long runId)
Attendance save(Attendance attendance)
void deleteById(Long id)
boolean existsByRunIdAndParticipantName(Long runId, String name)

// Rankings
List<AttendanceRankingDto> findTop10ByAttendanceCount()
List<DistanceRankingDto> findTop10ByTotalDistance()
List<MemberAttendanceStatsDto> findAllMemberStats()
List<Attendance> findByParticipantName(String name)
```

### AdminRepository
```java
Optional<Admin> findByUsername(String username)
Admin save(Admin admin)
boolean existsByUsername(String username)
```

## Domain DTOs

| DTO | Fields | Purpose |
|-----|--------|---------|
| AttendanceRankingDto | participantName, attendanceCount | Top 10 by attendance |
| DistanceRankingDto | participantName, totalDistance | Top 10 by distance |
| MemberAttendanceStatsDto | participantName, regularCount, lightningCount, totalCount | Member statistics |
