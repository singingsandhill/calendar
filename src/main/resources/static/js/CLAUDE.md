# JavaScript Modules

Vanilla JavaScript (no build tools or frameworks).

## Files

### api.js
REST API client wrapper using Fetch API.

**Core Pattern:**
```javascript
const api = {
  async request(url, options = {}) { ... },
  // Domain-specific methods
};
```

**Owner API:**
- `getOwner(ownerId)` → GET `/api/owners/{ownerId}`
- `createOwner(ownerId)` → POST `/api/owners`
- `getOwnerSchedules(ownerId)` → GET `/api/owners/{ownerId}/schedules`

**Schedule API:**
- `getSchedule(ownerId, year, month)` → GET `/api/owners/{ownerId}/schedules/{year}/{month}`
- `createSchedule(ownerId, year, month, weeks)` → POST `/api/owners/{ownerId}/schedules`
- `updateSchedule(ownerId, year, month, weeks)` → PATCH
- `deleteSchedule(ownerId, year, month)` → DELETE

**Participant API:**
- `getParticipants(scheduleId)` → GET `/api/schedules/{scheduleId}/participants`
- `addParticipant(scheduleId, name)` → POST
- `deleteParticipant(participantId)` → DELETE `/api/participants/{participantId}`
- `updateSelections(participantId, selections)` → PATCH `/api/participants/{participantId}/selections`

**Error Handling:**
- Throws `Error` with message from response body
- Handles 204 No Content gracefully

---

### calendar.js
Pure utility functions for date calculations.

| Function | Returns | Description |
|----------|---------|-------------|
| `getDaysInMonth(year, month)` | Number | Days in month (handles leap years) |
| `getFirstDayOfWeek(year, month)` | 0-6 | Day of week (0=Sunday) |
| `formatDate(year, month, day)` | String | "YYYY-MM-DD" format |
| `formatMonthKorean(year, month)` | String | "2024년 12월" format |
| `getMonthNameKorean(month)` | String | "1월", "2월", etc. |
| `calculateWeeks(year, month)` | Number | Rows needed for calendar grid |
| `isToday(year, month, day)` | Boolean | Check if date is today |
| `isPastDate(year, month, day)` | Boolean | Check if date is before today |

**Korean Locale:**
Functions support Korean date formatting for UI display.
