# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Group scheduling web application (약속 잡기) built with Spring Boot 4.0.0 and Java 21. Allows owners to create schedules and participants to mark their availability.

## Build Commands

```bash
./gradlew build                              # Full build with tests
./gradlew bootRun                            # Run application (http://localhost:8080)
./gradlew test                               # Run all tests
./gradlew test --tests ScheduleServiceTest  # Run specific test class
./gradlew test --tests "*ServiceTest"       # Run pattern-matched tests
```

## Architecture

The project follows **Hexagonal Architecture** (Ports & Adapters) with four layers:

```
presentation/          # Controllers (MVC + REST API), DTOs
  ├── controller/      # Thymeleaf template controllers
  ├── api/             # REST API controllers
  └── dto/             # Request/Response DTOs

application/           # Business logic orchestration
  ├── service/         # OwnerService, ScheduleService, ParticipantService
  └── exception/       # Custom business exceptions

domain/                # Pure domain models (no framework dependencies)
  ├── owner/           # Owner aggregate
  ├── schedule/        # Schedule aggregate
  └── participant/     # Participant aggregate

infrastructure/        # External concerns
  ├── config/          # JpaConfig, WebConfig
  └── persistence/     # JPA entities, repository adapters, converters
```

**Key Pattern:** Domain layer defines repository interfaces (ports); infrastructure layer implements them (adapters).

## Domain Model

Three aggregates:
- **Owner** - Creates and manages schedules (validated ID: 2-20 chars, lowercase/numbers/hyphens)
- **Schedule** - Calendar for a specific year/month with participant availability
- **Participant** - Marks availability on a schedule (max 8 per schedule)

## REST API

Base path: `/api/`

- `GET/POST /api/owners/{ownerId}` - Owner operations
- `GET/POST/PATCH/DELETE /api/owners/{ownerId}/schedules/{year}/{month}` - Schedule CRUD
- `POST/PATCH/DELETE /api/schedules/{scheduleId}/participants/{participantId}` - Participant operations

## Database

- **Development:** H2 file-based (`./data/scheduledb`), MySQL compatibility mode
- **Test:** H2 in-memory with create-drop DDL
- **H2 Console:** http://localhost:8080/h2-console (user: sa, no password)

## Testing

Tests use JUnit 5 with Mockito. Test structure mirrors main source:
- `domain/` - Domain model unit tests
- `application/service/` - Service layer tests with mocked repositories
- `presentation/api/` - API controller tests

## Package Documentation

Each package has its own CLAUDE.md with detailed guidance:

### Java Packages
| Package | Path | Description |
|---------|------|-------------|
| application | `src/main/java/.../application/CLAUDE.md` | Services, exceptions, transaction patterns |
| domain | `src/main/java/.../domain/CLAUDE.md` | Aggregates, value objects, repository interfaces |
| infrastructure | `src/main/java/.../infrastructure/CLAUDE.md` | JPA entities, adapters, converters |
| presentation | `src/main/java/.../presentation/CLAUDE.md` | Controllers, DTOs, endpoints |

### Static Resources
| Directory | Path | Description |
|-----------|------|-------------|
| css | `src/main/resources/static/css/CLAUDE.md` | CSS variables, components, responsive |
| js | `src/main/resources/static/js/CLAUDE.md` | API client, calendar utilities |

### Templates
| Directory | Path | Description |
|-----------|------|-------------|
| fragments | `src/main/resources/templates/fragments/CLAUDE.md` | Header, footer fragments |
| owner | `src/main/resources/templates/owner/CLAUDE.md` | Dashboard template |
| schedule | `src/main/resources/templates/schedule/CLAUDE.md` | Calendar view template |
