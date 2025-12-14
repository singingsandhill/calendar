# Schedule Templates

## view.html

Interactive calendar for participant date selection.

### Model Variables

| Variable | Type | Description |
|----------|------|-------------|
| `ownerId` | String | Owner identifier |
| `schedule` | ScheduleDetailResponse | Full schedule with participants |
| `year` | int | Schedule year |
| `month` | int | Schedule month |

### ScheduleDetailResponse Properties
- `id`, `ownerId`, `year`, `month`, `weeks`
- `daysInMonth`, `firstDayOfWeek`
- `participants` (List<ParticipantResponse>)
- `createdAt`

### ParticipantResponse Properties
- `id`, `name`, `color`, `selections[]`, `updatedAt`

### Page Sections

1. **Header**
   - Back link: `/{ownerId}`
   - Title: `${year}년 ${month}월`

2. **Participant Section**
   - Dropdown: `#participantSelect`
   - "Add Participant" button
   - Legend with colored dots

3. **Calendar**
   - Weekday headers (Sun-Sat)
   - Calendar body (`#calendarBody`) - rendered by JS
   - Day cells with participant dots

4. **Actions**
   - Reset button
   - Save button

5. **Add Participant Modal**
   - Name input (maxlength=10)
   - Shows count: `${schedule.participants().size()} / 8`
   - Disabled if >= 8 participants

### Inline JavaScript

```javascript
const scheduleData = [[${schedule}]];

// Core functions
renderCalendar()           // Build calendar grid
onParticipantChange()      // Handle dropdown change
toggleDay(day, cell)       // Toggle selection
updateCalendarDisplay()    // Refresh UI
saveSelections()           // API call
resetSelections()          // Clear current

// State
currentParticipantId       // Selected participant
selectedDays               // Set of selected days
```

### Calendar Rendering

- Empty cells for days before month starts
- Day cells with:
  - Day number
  - Participant dots (colored by participant)
  - Click handler for selection
- `.selected` class on chosen days

### Controller

`ScheduleController.viewSchedule()` → returns "schedule/view"
- Auto-creates schedule if not found
