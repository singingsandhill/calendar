# CSS Styles

Single stylesheet (`style.css`) for the entire application.

## CSS Variables

```css
:root {
  --primary-color: #3498db;
  --primary-dark: #2980b9;
  --danger-color: #e74c3c;
  --success-color: #2ecc71;
  --text-color: #333;
  --text-light: #666;
  --border-color: #ddd;
  --bg-light: #f5f5f5;
  --bg-white: #fff;
  --shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 4px 12px rgba(0, 0, 0, 0.15);
}
```

## Component Sections

| Lines | Component |
|-------|-----------|
| 1-27 | Reset & Base typography |
| 29-44 | Navbar (sticky) |
| 46-51 | Container (max-width centered) |
| 53-68 | Hero section |
| 70-120 | Forms (input groups, hints) |
| 121-180 | Buttons (variants, sizes) |
| 182-225 | How It Works (step cards) |
| 227-295 | Dashboard (schedule cards, empty state) |
| 297-382 | Modal (overlay, form groups) |
| 384-446 | Schedule view (participant section, legend) |
| 448-539 | Calendar (7-column grid, day cells) |
| 541-552 | Footer |
| 554-590 | Responsive (768px breakpoint) |

## Naming Conventions

BEM-inspired:
- `.schedule-card`, `.schedule-card-header`, `.schedule-card-body`
- `.btn-primary`, `.btn-secondary`, `.btn-danger`, `.btn-sm`
- `.calendar-day`, `.calendar-day.selected`, `.calendar-day.empty`

## Button Variants

```css
.btn              /* Base button */
.btn-primary      /* Blue primary action */
.btn-secondary    /* Gray secondary */
.btn-outline      /* Border only */
.btn-danger       /* Red destructive */
.btn-sm           /* Small size modifier */
```

## Calendar Grid

- 7-column CSS Grid for weekdays
- `aspect-ratio: 1` for square day cells
- Participant dots with individual colors
- `.selected` class for chosen days

## Responsive

Single breakpoint at `768px`:
- Grid columns adjust (3 → 2 → 1)
- Font sizes reduce
- Padding/margins decrease
