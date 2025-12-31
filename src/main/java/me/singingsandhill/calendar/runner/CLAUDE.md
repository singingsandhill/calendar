# Runner Module

Running crew (97 Runners) attendance management system. Tracks runs, participant attendance with distance, member rankings, and provides an admin dashboard for run management.

## Architecture

Follows **Hexagonal Architecture** with four layers:

```
runner/
├── domain/           # Pure domain models, repository interfaces
├── application/      # Business logic services, exceptions
├── infrastructure/   # JPA entities, adapters, security, config
└── presentation/     # Controllers (MVC + REST), DTOs
```

## Domain Model

Three main entities:
- **Run** - Scheduled running event with date, time, location, category
- **Attendance** - Participant attendance record with distance (km)
- **Admin** - Admin user for dashboard access

```
Run (1) ──── (*) Attendance
```

## REST API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/runners/runs/{runId}/attendance` | Register attendance |

## MVC Routes

| Path | Description |
|------|-------------|
| `/runners` | Home page (rankings) |
| `/runners/runs` | Run list |
| `/runners/runs/{id}` | Run detail |
| `/runners/members` | Member stats |
| `/runners/members/{name}` | Member detail |
| `/runners/announce` | Announcement generator |
| `/runners/admin` | Admin dashboard |
| `/runners/admin/login` | Admin login |

## Configuration

```yaml
runner:
  admin:
    username: admin      # Default admin username
    password: admin123   # Default admin password (change in production)
```

## Package Documentation

| Package | Path | Description |
|---------|------|-------------|
| domain | `runner/domain/CLAUDE.md` | Entities, repositories, DTOs |
| application | `runner/application/CLAUDE.md` | Services, exceptions |
| infrastructure | `runner/infrastructure/CLAUDE.md` | JPA, security, config |
| presentation | `runner/presentation/CLAUDE.md` | Controllers, request/response DTOs |
