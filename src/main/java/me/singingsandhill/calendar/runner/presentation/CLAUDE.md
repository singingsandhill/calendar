# Runner Presentation Layer

REST API controllers, MVC controllers, and request/response DTOs.

## REST API Controller

### RunnerApiController
Base path: `/runners`

| Method | Path | Request | Response | Status |
|--------|------|---------|----------|--------|
| POST | `/runs/{runId}/attendance` | AttendanceCreateRequest | AttendanceResponse | 201 |

## MVC Controllers

### RunnerController
Public-facing runner crew pages with SEO metadata.

| Path | Template | Description |
|------|----------|-------------|
| GET `/runners` | runners/home | Home with rankings |
| GET `/runners/runs` | runners/run-list | All runs list |
| GET `/runners/runs/{id}` | runners/run-detail | Run with attendances |
| GET `/runners/members` | runners/member-list | Member statistics |
| GET `/runners/members/{name}` | runners/member-detail | Individual member history |
| GET `/runners/announce` | runners/announce | Announcement image generator |

### RunnerAdminController
Admin dashboard with authentication (SEO: noindex, nofollow).

| Path | Template | Description |
|------|----------|-------------|
| GET `/runners/admin/login` | runners/admin/login | Login form |
| GET `/runners/admin` | runners/admin/dashboard | Dashboard (run list) |
| GET `/runners/admin/runs/new` | runners/admin/run-form | Create run form |
| POST `/runners/admin/runs` | - | Create run (redirect) |
| GET `/runners/admin/runs/{id}/edit` | runners/admin/run-form | Edit run form |
| POST `/runners/admin/runs/{id}` | - | Update run (redirect) |
| POST `/runners/admin/runs/{id}/delete` | - | Delete run (redirect) |

## Request DTOs

### RunCreateRequest (Record)
```java
@NotNull LocalDate date
@NotNull LocalTime time
@NotBlank @Size(max=100) String location
@NotNull RunCategory category
```

### AttendanceCreateRequest (Record)
```java
@NotBlank @Size(max=50) String participantName
@DecimalMin("0.1") @DecimalMax("100.0") BigDecimal distance
```

## Response DTOs

### RunResponse (Record)
| Field | Type | Notes |
|-------|------|-------|
| id | Long | |
| date | LocalDate | |
| time | LocalTime | |
| location | String | |
| category | RunCategory | |
| categoryDisplayName | String | Korean display name |
| createdAt | LocalDateTime | |
| formattedDate | String | "yyyy년 M월 d일 (E)" |
| formattedTime | String | "HH:mm" |

### AttendanceResponse (Record)
| Field | Type |
|-------|------|
| id | Long |
| runId | Long |
| participantName | String |
| distance | BigDecimal |
| createdAt | LocalDateTime |

### AttendanceWithRunResponse (Record)
Combines Attendance + Run data for member detail page.

### MemberStatsResponse (Record)
| Field | Type |
|-------|------|
| name | String |
| regularCount | int |
| lightningCount | int |
| totalCount | int |
