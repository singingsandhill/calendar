# Schedule Application Layer

## Services

- **OwnerService** - Idempotent owner creation (`getOrCreateOwner`)
- **ScheduleService** - CRUD by (ownerId, year, month), auto-creates owner
- **ParticipantService** - Max 8 participants per schedule, duplicate name check
- **LocationService** - Location voting (add/delete/vote/unvote)
- **MenuService** - Menu voting with URL (add/delete/vote/unvote)
- **PopularityService** - Time-weighted scoring for popular locations/menus
- **SeoService** - SEO metadata per page type

## Transaction Pattern

Class-level `@Transactional(readOnly = true)`, write methods override with `@Transactional`.
