# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-domain web application built with Spring Boot 4.0.0 and Java 21. Contains three independent domain modules:

| Module | Description | Base Path |
|--------|-------------|-----------|
| **Schedule** | Group scheduling (약속 잡기) - owners create schedules, participants mark availability | `/api/`, `/owner/` |
| **Runner** | Running crew management (97 Runners) - run tracking, attendance, rankings | `/runners/` |
| **Trading** | Cryptocurrency trading bot - Bithumb integration, technical analysis, automated trading | `/trading/`, `/api/trading/` |

## Build Commands

```bash
./gradlew build                              # Full build with tests
./gradlew bootRun                            # Run application (http://localhost:8080)
./gradlew test                               # Run all tests
./gradlew test --tests ScheduleServiceTest  # Run specific test class
./gradlew test --tests "*ServiceTest"       # Run pattern-matched tests
```

### WSL Environment

```bash
# WSL에서 Windows JDK를 사용하여 빌드/실행
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat build"
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat bootRun"
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"

# Java 프로세스 종료 (H2 DB 잠금 해제 시 필요)
cmd.exe /c "taskkill /F /IM java.exe"
```

## Architecture

The project uses **Hexagonal Architecture** (Ports & Adapters) with domain-based modular structure:

```
src/main/java/me/singingsandhill/calendar/
├── common/              # Shared components (exceptions, config, utilities)
│   ├── application/     # BusinessException base class
│   ├── infrastructure/  # JpaConfig, WebConfig, SecurityConfig
│   └── presentation/    # GlobalExceptionHandler, ErrorResponse, SeoMetadata
│
├── domain/              # Schedule module (legacy structure)
├── application/         # Schedule module services
├── infrastructure/      # Schedule module JPA
├── presentation/        # Schedule module controllers
│
├── runner/              # Runner module (self-contained hexagonal)
│   ├── domain/          # Run, Attendance, Admin entities
│   ├── application/     # RunService, AttendanceService
│   ├── infrastructure/  # JPA, Security
│   └── presentation/    # Controllers, DTOs
│
└── trading/             # Trading module (self-contained hexagonal)
    ├── domain/          # Candle, Trade, Position, Signal entities
    ├── application/     # TradingBotService, IndicatorService, etc.
    ├── infrastructure/  # Bithumb API, JPA, Schedulers
    └── presentation/    # Dashboard, REST API
```

**Key Pattern:** Each module follows hexagonal architecture. Domain layer defines repository interfaces (ports); infrastructure layer implements them (adapters).

## REST API

### Schedule API
Base path: `/api/`

- `GET/POST /api/owners/{ownerId}` - Owner operations
- `GET/POST/PATCH/DELETE /api/owners/{ownerId}/schedules/{year}/{month}` - Schedule CRUD
- `POST/PATCH/DELETE /api/schedules/{scheduleId}/participants/{participantId}` - Participant operations

### Runner API
Base path: `/runners/`

- `POST /runners/runs/{runId}/attendance` - Register attendance

### Trading API
Base path: `/api/trading/`

- `GET/POST /api/trading/bot/*` - Bot control (start/stop/pause)
- `GET /api/trading/candles` - Candle data for charts
- `GET /api/trading/ticker` - Real-time price and indicators
- `GET /api/trading/trades` - Trade history
- `GET /api/trading/positions` - Position history
- `GET /api/trading/profit/*` - P&L statistics

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

### Common Package
| Package | Path | Description |
|---------|------|-------------|
| common | `common/CLAUDE.md` | Shared exceptions, config, utilities |

### Schedule Module (Legacy)
| Package | Path | Description |
|---------|------|-------------|
| application | `application/CLAUDE.md` | Services, exceptions, transaction patterns |
| domain | `domain/CLAUDE.md` | Aggregates, value objects, repository interfaces |
| infrastructure | `infrastructure/CLAUDE.md` | JPA entities, adapters, converters |
| presentation | `presentation/CLAUDE.md` | Controllers, DTOs, endpoints |

### Runner Module
| Package | Path | Description |
|---------|------|-------------|
| runner | `runner/CLAUDE.md` | Module overview, API summary |
| runner/domain | `runner/domain/CLAUDE.md` | Run, Attendance, Admin entities |
| runner/application | `runner/application/CLAUDE.md` | Services, exceptions |
| runner/infrastructure | `runner/infrastructure/CLAUDE.md` | JPA, security, config |
| runner/presentation | `runner/presentation/CLAUDE.md` | Controllers, DTOs |

### Trading Module
| Package | Path | Description |
|---------|------|-------------|
| trading | `trading/CLAUDE.md` | Module overview, trading flow |
| trading/domain | `trading/domain/CLAUDE.md` | Candle, Trade, Position, Signal entities |
| trading/application | `trading/application/CLAUDE.md` | Bot, indicator, signal services |
| trading/infrastructure | `trading/infrastructure/CLAUDE.md` | Bithumb API, JPA, schedulers |
| trading/presentation | `trading/presentation/CLAUDE.md` | Dashboard, REST API |

### Static Resources
| Directory | Path | Description |
|-----------|------|-------------|
| css | `static/css/CLAUDE.md` | CSS variables, components, responsive |
| js | `static/js/CLAUDE.md` | API client, calendar utilities |

### Templates
| Directory | Path | Description |
|-----------|------|-------------|
| fragments | `templates/fragments/CLAUDE.md` | Header, footer fragments |
| owner | `templates/owner/CLAUDE.md` | Schedule dashboard template |
| schedule | `templates/schedule/CLAUDE.md` | Calendar view template |
| runners | `templates/runners/CLAUDE.md` | Runner crew templates |
| trading | `templates/trading/CLAUDE.md` | Trading dashboard templates |
