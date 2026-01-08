# Application Layer

Business logic orchestration layer between presentation and domain/infrastructure.

## Services

| Service | Purpose |
|---------|---------|
| `OwnerService` | Owner CRUD, getOrCreateOwner() for idempotent creation |
| `ScheduleService` | Schedule CRUD by (ownerId, year, month), auto-creates owner |
| `ParticipantService` | Participant management, selection updates, max 8 limit |
| `LocationService` | Location voting - add/delete locations, vote/unvote |
| `MenuService` | Menu voting - add/delete menus with URL, vote/unvote |
| `PopularityService` | Aggregate popular locations and menus with time-weighted scoring |
| `SeoService` | SEO metadata generation for each page type |

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

**LocationService**
- `getLocation(locationId)` - Get by ID, throws LocationNotFoundException
- `getLocationsByScheduleId(scheduleId)` - List all locations for schedule
- `addLocation(scheduleId, name)` - Add new location, checks duplicates
- `deleteLocation(locationId)` - Delete location
- `vote(locationId, voterName)` - Add vote
- `unvote(locationId, voterName)` - Remove vote

**MenuService**
- `getMenu(menuId)` - Get by ID, throws MenuNotFoundException
- `getMenusByScheduleId(scheduleId)` - List all menus for schedule
- `addMenu(scheduleId, name, url)` - Add new menu with URL, checks duplicates
- `deleteMenu(menuId)` - Delete menu
- `vote(menuId, voterName)` - Add vote
- `unvote(menuId, voterName)` - Remove vote

**PopularityService**
- `getPopularLocations(limit)` - Top locations by time-weighted score
- `getPopularMenus(limit)` - Top menus by time-weighted score

**SeoService**
- `getHomeSeo()` - Landing page SEO (index, follow)
- `getStartPageSeo()` - Start page SEO (index, follow)
- `getDashboardSeo(ownerId)` - Dashboard SEO (noindex)
- `getScheduleSeo(ownerId, year, month)` - Schedule page SEO (noindex)

## Exceptions

All extend `BusinessException` with `code` and `HttpStatus`.

| Exception | HTTP Status | When Thrown |
|-----------|-------------|-------------|
| `OwnerNotFoundException` | 404 | Owner lookup fails |
| `ScheduleNotFoundException` | 404 | Schedule lookup fails |
| `ParticipantNotFoundException` | 404 | Participant lookup fails |
| `LocationNotFoundException` | 404 | Location lookup fails |
| `MenuNotFoundException` | 404 | Menu lookup fails |
| `DuplicateScheduleException` | 409 | Schedule already exists for owner/year/month |
| `DuplicateParticipantException` | 409 | Participant name exists in schedule |
| `DuplicateLocationException` | 409 | Location name exists in schedule |
| `DuplicateMenuException` | 409 | Menu name exists in schedule |
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
