# Runner Infrastructure Layer

External concerns: JPA persistence, Spring Security integration, and configuration.

## JPA Entities

### RunJpaEntity
Table: `runs`

| Column | Type | Notes |
|--------|------|-------|
| id | Long | PK, auto-generated |
| date | LocalDate | |
| time | LocalTime | |
| location | String(100) | |
| category | RunCategoryJpa | Enum |
| createdAt | LocalDateTime | |

Relationships: `@OneToMany` to AttendanceJpaEntity (cascade ALL, orphan removal)

### AttendanceJpaEntity
Table: `attendances`

| Column | Type | Notes |
|--------|------|-------|
| id | Long | PK, auto-generated |
| run_id | Long | FK to runs |
| participantName | String(50) | |
| distance | BigDecimal | |
| createdAt | LocalDateTime | |

**Unique Constraint**: (run_id, participant_name)

### RunnerAdminJpaEntity
Table: `runner_admins`

| Column | Type | Notes |
|--------|------|-------|
| id | Long | PK, auto-generated |
| username | String | Unique |
| password | String | BCrypt encoded |
| createdAt | LocalDateTime | |

## Repository Adapters

| Adapter | Implements | Description |
|---------|------------|-------------|
| RunRepositoryAdapter | RunRepository | Run CRUD with entity mapping |
| AttendanceRepositoryAdapter | AttendanceRepository | Attendance + ranking queries |
| RunnerAdminRepositoryAdapter | AdminRepository | Admin user management |

## JPA Repositories

| Repository | Key Queries |
|------------|-------------|
| RunJpaRepository | `findAllOrderByDateDescTimeDesc()` |
| AttendanceJpaRepository | Rankings with GROUP BY, case-insensitive duplicate check |
| RunnerAdminJpaRepository | `findByUsername()`, `existsByUsername()` |

## Security

### RunnerUserDetailsService
Implements `UserDetailsService` for Spring Security authentication.
- Loads admin by username
- Returns UserDetails with `ROLE_ADMIN`

### RunnerAdminInitializer
`CommandLineRunner` that creates default admin on startup if none exists.

Configuration:
```yaml
runner:
  admin:
    username: admin
    password: admin123
```

## Entity Mapping Pattern

```java
// Domain to JPA
RunJpaEntity.fromDomain(Run run)

// JPA to Domain
runJpaEntity.toDomain()
```
