# Owner Templates

## dashboard.html

Owner's main dashboard showing all schedules.

### Model Variables

| Variable | Type | Description |
|----------|------|-------------|
| `ownerId` | String | Owner identifier |
| `owner` | Owner | Owner entity |
| `schedules` | List<ScheduleResponse> | Owner's schedules |

### ScheduleResponse Properties
- `id`, `ownerId`, `year`, `month`, `weeks`
- `participantCount`, `createdAt`
- `formattedYearMonth()` → "YYYY-MM"

### Page Sections

1. **Header**
   - Title: `${ownerId}'s Dashboard`
   - "New Schedule" button (opens modal)

2. **Schedule Grid** (if schedules not empty)
   - Iterates: `th:each="schedule : ${schedules}"`
   - Each card shows: year-month, participant count
   - Actions: View, Copy Link, Delete

3. **Empty State** (if schedules empty)
   - Encourages creating first schedule

4. **Create Schedule Modal**
   - Form ID: `createScheduleForm`
   - Year select: 2024-2027
   - Month select: 1-12 (Korean: `${m + '월'}`)

### Data Attributes

Buttons use data attributes for JS:
```html
th:data-owner="${ownerId}"
th:data-year="${schedule.year()}"
th:data-month="${schedule.month()}"
```

### Inline JavaScript

```javascript
const ownerId = [[${ownerId}]];

// Modal functions
openCreateModal(), closeCreateModal()

// API calls
api.createSchedule(ownerId, year, month)
api.deleteSchedule(ownerId, year, month)
```

### Controller

`OwnerController.dashboard()` → returns "owner/dashboard"
