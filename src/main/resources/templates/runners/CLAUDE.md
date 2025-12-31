# Runner Templates

Thymeleaf templates for runner crew pages.

## Public Pages

| Template | Controller | Description |
|----------|------------|-------------|
| home.html | RunnerController | Home with attendance/distance rankings |
| run-list.html | RunnerController | All runs list |
| run-detail.html | RunnerController | Run detail with participant attendance |
| member-list.html | RunnerController | Member statistics table |
| member-detail.html | RunnerController | Individual member attendance history |
| announce.html | RunnerController | Announcement image generator |

## Admin Pages

| Template | Controller | Description |
|----------|------------|-------------|
| admin/dashboard.html | RunnerAdminController | Admin dashboard with run list |
| admin/login.html | RunnerAdminController | Login form |
| admin/run-form.html | RunnerAdminController | Create/edit run form |

## Fragments

| Fragment | Path | Description |
|----------|------|-------------|
| header | fragments/header.html | Navigation, SEO meta tags |
| footer | fragments/footer.html | Footer content |

## Model Variables

### home.html
| Variable | Type | Description |
|----------|------|-------------|
| attendanceRankings | List<AttendanceRankingDto> | Top 10 by attendance |
| distanceRankings | List<DistanceRankingDto> | Top 10 by distance |
| seo | SeoMetadata | SEO metadata |

### run-list.html
| Variable | Type | Description |
|----------|------|-------------|
| runs | List<RunResponse> | All runs |
| seo | SeoMetadata | SEO metadata |

### run-detail.html
| Variable | Type | Description |
|----------|------|-------------|
| run | RunResponse | Run details |
| attendances | List<AttendanceResponse> | Participants |
| seo | SeoMetadata | SEO metadata |

### member-list.html
| Variable | Type | Description |
|----------|------|-------------|
| members | List<MemberStatsResponse> | Member statistics |
| seo | SeoMetadata | SEO metadata |

### member-detail.html
| Variable | Type | Description |
|----------|------|-------------|
| memberName | String | Participant name |
| attendances | List<AttendanceWithRunResponse> | Attendance history |
| seo | SeoMetadata | SEO metadata |

### admin/dashboard.html
| Variable | Type | Description |
|----------|------|-------------|
| runs | List<RunResponse> | All runs for management |

### admin/run-form.html
| Variable | Type | Description |
|----------|------|-------------|
| run | RunCreateRequest | Form data (null for create) |
| categories | RunCategory[] | Available categories |
| isEdit | boolean | Edit mode flag |

## JavaScript Integration

- announce.html: Uses `/js/announce-generator.js` for image generation
- Attendance form: AJAX POST to `/runners/runs/{runId}/attendance`

## CSS

Uses `/css/runners.css` for runner-specific styles.
