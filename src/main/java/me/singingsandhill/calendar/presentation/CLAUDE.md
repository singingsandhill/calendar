# Presentation Layer

REST API and MVC controllers with DTOs.

## Structure

```
presentation/
├── api/                    # REST controllers
│   ├── GlobalExceptionHandler.java
│   ├── OwnerApiController.java
│   ├── ParticipantApiController.java
│   └── ScheduleApiController.java
├── controller/             # MVC controllers
│   ├── HomeController.java
│   ├── OwnerController.java
│   └── ScheduleController.java
└── dto/
    ├── request/            # Input DTOs with validation
    └── response/           # Output DTOs with factory methods
```

## REST API Endpoints

### Owner API (`/api/owners`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/{ownerId}` | Get owner |
| POST | `/` | Create owner |
| GET | `/{ownerId}/schedules` | List schedules |

### Schedule API (`/api/owners/{ownerId}/schedules`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/{year}/{month}` | Get schedule |
| POST | `/` | Create schedule |
| PATCH | `/{year}/{month}` | Update schedule |
| DELETE | `/{year}/{month}` | Delete schedule |

### Participant API (`/api`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/schedules/{scheduleId}/participants` | List participants |
| POST | `/schedules/{scheduleId}/participants` | Add participant |
| DELETE | `/participants/{participantId}` | Delete participant |
| PATCH | `/participants/{participantId}/selections` | Update selections |

## MVC Endpoints

| Method | Path | Controller | Template |
|--------|------|------------|----------|
| GET | `/` | HomeController | `index` |
| POST | `/start` | HomeController | redirect |
| GET | `/{ownerId}` | OwnerController | `owner/dashboard` |
| GET | `/{ownerId}/{year}/{month}` | ScheduleController | `schedule/view` |

## Request DTOs

All use Jakarta validation annotations.

| DTO | Fields |
|-----|--------|
| `OwnerCreateRequest` | ownerId (2-20 chars, pattern) |
| `ScheduleCreateRequest` | year (2024-2100), month (1-12), weeks (4-6, optional) |
| `ScheduleUpdateRequest` | weeks (4-6) |
| `ParticipantCreateRequest` | name (max 10 chars) |
| `SelectionUpdateRequest` | selections (List<Integer>) |

## Response DTOs

All are Java Records with `from(Entity)` factory methods.

| DTO | Key Fields |
|-----|------------|
| `OwnerResponse` | ownerId, createdAt, scheduleCount |
| `ScheduleResponse` | id, year, month, weeks, participantCount |
| `ScheduleDetailResponse` | ...above + daysInMonth, firstDayOfWeek, participants[] |
| `ParticipantResponse` | id, name, color, selections[] |
| `ErrorResponse` | code, message |

## Exception Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | Status | Code |
|-----------|--------|------|
| `BusinessException` | (from exception) | (from exception) |
| `MethodArgumentNotValidException` | 400 | VALIDATION_ERROR |
| `IllegalArgumentException` | 400 | INVALID_ARGUMENT |
| `Exception` | 500 | INTERNAL_ERROR |

## Patterns

- Constructor injection for services
- `@Valid` on all `@RequestBody` parameters
- Factory methods: `Response.from(entity)`
- HTTP status: 200 (GET), 201 (POST create), 204 (DELETE)
