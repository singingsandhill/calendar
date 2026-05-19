# 03. Analysis Playbook — 즉시 실행 가능한 분석

각 분석은 (1) 비즈니스 질문, (2) 사용 데이터, (3) 실행 쿼리/차트, (4) 해석 기준 으로
구성. GA4 단독, BQ SQL, DB 단독, BQ+DB 결합 순.

---

## A. GA4 단독 — 코드 변경 0, 즉시 가능

### A1. 트래픽 소스 → 페이지 인기도

**질문**: 어느 채널(organic/direct/referral)이 어느 페이지로 사람을 데려오나?

**경로**: GA4 → 보고서 → Acquisition → **Traffic acquisition**, 보조 측정기준에 `Landing page`.

**해석**:
- organic 비중이 높은 use-case 슬러그 → SEO 잘 작동
- direct 비중 높은 슬러그 → 북마크/공유 위주 (브랜드 강함)

### A2. /use-cases 콘텐츠 ROI

**질문**: 어느 슬러그가 SEO 트래픽 가져오고 사용자가 머무는가?

**경로**: GA4 → 보고서 → Engagement → **Pages and screens** → 페이지 경로 필터 `/use-cases/`.

**해석 지표**:
- Views, Engagement rate, Average engagement time, Event count
- ROI 신호: `friend-meetup`, `team-meeting`, `travel-planning`, `study-group` 4개 슬러그 비교

### A3. 한/영 사용자 행동 비교

**질문**: ?lang=en 페이지 사용자가 ko 사용자와 행동이 다른가?

**경로**: Explorations → **Free form** → 측정기준 `Page path + query string` (또는 custom dim `Language`), 측정항목 `Engaged sessions`, `Average engagement time`.

**해석**: en 비중이 1% 미만이면 영문 콘텐츠 투자 ROI 낮음. 5% 이상이면 영문 키워드 SEO 적극 검토.

### A4. Search Console 키워드 → 페이지 매칭

**질문**: 어떤 검색어로 들어오고, 어느 페이지가 노출되는가?

**경로**: GA4 → 보고서 → Acquisition → **Search Console → Queries** / **Search Console → Organic search traffic**.

**해석 패턴**:
- 노출 ↑ 클릭 ↓ → title/description 개선 후보
- 클릭 ↑ 짧은 engagement → 콘텐츠 매칭 부족 (반쪽 SEO)
- 평균 순위 5~15 → snippet 강화로 끌어올릴 후보

---

## B. BigQuery SQL — daily export 활성 후

`events_*` 테이블은 적재 후 24~48시간 지연.

### B1. 일자별 이벤트 카운트

```sql
SELECT
  event_name,
  COUNT(*) AS events,
  COUNT(DISTINCT user_pseudo_id) AS users
FROM `project.analytics_NNNNNNNNN.events_*`
WHERE _TABLE_SUFFIX BETWEEN
  FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE('Asia/Seoul'), INTERVAL 7 DAY))
  AND FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE('Asia/Seoul'), INTERVAL 1 DAY))
GROUP BY event_name
ORDER BY events DESC;
```

**활용**: 일별 트래픽·전환 모니터링.

### B2. 핵심 conversion 펀널 — 일정 생성 → 참가자 → 선택 저장 → 투표

```sql
WITH e AS (
  SELECT
    user_pseudo_id,
    event_name,
    event_timestamp,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key='page_location') AS page,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key='owner_id') AS owner_id
  FROM `project.analytics_NNNNNNNNN.events_*`
  WHERE _TABLE_SUFFIX BETWEEN '20260101' AND '20260531'
)
SELECT
  COUNT(DISTINCT IF(event_name='page_view' AND page LIKE '%datedate.site/' , user_pseudo_id, NULL)) AS step1_landed,
  COUNT(DISTINCT IF(event_name='schedule_created', user_pseudo_id, NULL)) AS step2_created,
  COUNT(DISTINCT IF(event_name='participant_added', user_pseudo_id, NULL)) AS step3_invited,
  COUNT(DISTINCT IF(event_name='selections_saved', user_pseudo_id, NULL)) AS step4_saved,
  COUNT(DISTINCT IF(event_name='vote_cast', user_pseudo_id, NULL)) AS step5_voted
FROM e;
```

**해석**: drop-off 가 큰 단계가 UX 개선 우선순위.

### B2-prereq. GA4 맞춤 측정기준 등록 필요

이벤트 파라미터(`owner_id`, `target`, `action` 등)를 GA4 UI 와 BQ 컬럼으로 사용하려면
관리 → 맞춤 정의 → **맞춤 측정기준** 에 등록 (24~48시간 후 적용).

| 파라미터 | 등록 이름 | 범위 | 등록 우선순위 |
|---|---|---|---|
| `owner_id` | Owner ID | event | P0 |
| `schedule_id` | Schedule ID | event | P0 |
| `target` | Vote Target | event | P0 |
| `action` | Vote Action | event | P0 |
| `target_id` | Vote Target ID | event | P1 |
| `days_count` | Days Count | event | P1 |
| `participant_count_after` | Participant Count | event | P1 |
| `category` | Run Category | event | P1 |
| `run_id` | Run ID | event | P1 |
| `distance` | Distance | event | P1 |
| `year`, `month` | Schedule Year/Month | event | P2 |

### B3. 트래픽 소스별 attribution — 일정 생성

```sql
WITH first_session AS (
  SELECT
    user_pseudo_id,
    MIN(event_timestamp) AS first_ts,
    ARRAY_AGG(traffic_source.medium ORDER BY event_timestamp LIMIT 1)[OFFSET(0)] AS first_medium,
    ARRAY_AGG(traffic_source.source ORDER BY event_timestamp LIMIT 1)[OFFSET(0)] AS first_source
  FROM `project.analytics_NNNNNNNNN.events_*`
  WHERE event_name='session_start'
  GROUP BY user_pseudo_id
),
created AS (
  SELECT DISTINCT user_pseudo_id
  FROM `project.analytics_NNNNNNNNN.events_*`
  WHERE event_name='schedule_created'
)
SELECT
  fs.first_medium,
  fs.first_source,
  COUNT(DISTINCT c.user_pseudo_id) AS schedules_created,
  COUNT(DISTINCT fs.user_pseudo_id) AS visitors,
  ROUND(COUNT(DISTINCT c.user_pseudo_id) / COUNT(DISTINCT fs.user_pseudo_id) * 100, 2) AS conversion_pct
FROM first_session fs
LEFT JOIN created c USING(user_pseudo_id)
GROUP BY 1, 2
HAVING visitors >= 50
ORDER BY conversion_pct DESC;
```

**해석**: organic search 의 conversion 이 direct 보다 높으면 SEO 가 진짜 성과 채널.

### B4. /use-cases/* 페이지 → 일정 생성 attribution

```sql
WITH usecase_visitors AS (
  SELECT DISTINCT
    user_pseudo_id,
    REGEXP_EXTRACT(
      (SELECT value.string_value FROM UNNEST(event_params) WHERE key='page_location'),
      r'/use-cases/([^/?]+)'
    ) AS slug
  FROM `project.analytics_NNNNNNNNN.events_*`
  WHERE event_name='page_view'
    AND _TABLE_SUFFIX BETWEEN '20260101' AND '20260531'
),
created AS (
  SELECT DISTINCT user_pseudo_id
  FROM `project.analytics_NNNNNNNNN.events_*`
  WHERE event_name='schedule_created'
    AND _TABLE_SUFFIX BETWEEN '20260101' AND '20260531'
)
SELECT
  uv.slug,
  COUNT(DISTINCT uv.user_pseudo_id) AS visitors,
  COUNT(DISTINCT c.user_pseudo_id) AS converted,
  ROUND(COUNT(DISTINCT c.user_pseudo_id) / NULLIF(COUNT(DISTINCT uv.user_pseudo_id), 0) * 100, 2) AS conversion_pct
FROM usecase_visitors uv
LEFT JOIN created c USING(user_pseudo_id)
WHERE uv.slug IS NOT NULL
GROUP BY uv.slug
ORDER BY conversion_pct DESC;
```

**해석**: 어느 use-case 페이지가 진짜 conversion 을 만드는가. 콘텐츠 우선순위.

---

## C. 도메인 DB 단독 — GA4 없이 즉시 가능

H2 콘솔 (`/h2-console`) 또는 운영 DB 에서 직접 SQL.

### C1. 일정당 평균 참가자 수 분포

```sql
SELECT
  weeks,
  COUNT(*) AS total_schedules,
  AVG(participant_count) AS avg_participants,
  STDDEV(participant_count) AS std_participants,
  MAX(participant_count) AS max_participants
FROM (
  SELECT s.id, s.weeks, COUNT(p.id) AS participant_count
  FROM schedules s
  LEFT JOIN participants p ON p.schedule_id = s.id
  GROUP BY s.id, s.weeks
)
GROUP BY weeks;
```

**해석**: 일정당 sweet spot 그룹 사이즈. weeks=7 모드가 더 큰 그룹에 쓰이는지.

### C2. weeks 4 vs 7 모드 채택률

```sql
SELECT
  weeks,
  COUNT(*) AS schedules,
  ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) AS pct
FROM schedules
GROUP BY weeks;
```

**해석**: 7주 확장 모드가 메인 사용 패턴이면 legacy 4주 모드 deprecate 검토.

### C3. Owner 코호트 retention (월별)

```sql
WITH owner_first AS (
  SELECT owner_id, DATE_TRUNC('month', MIN(created_at)) AS cohort_month
  FROM owners
  GROUP BY owner_id
),
owner_activity AS (
  SELECT s.owner_id, DATE_TRUNC('month', s.created_at) AS activity_month
  FROM schedules s
)
SELECT
  of.cohort_month,
  oa.activity_month,
  COUNT(DISTINCT of.owner_id) AS active_owners
FROM owner_first of
LEFT JOIN owner_activity oa ON oa.owner_id = of.owner_id
GROUP BY 1, 2
ORDER BY 1, 2;
```

**해석**: M+1, M+2 활성률. 본 서비스의 주된 사용 패턴이 "한 번 쓰고 끝" 인가, 재사용형인가.

### C4. selections JSON → 요일별 가용성 히트맵

```sql
-- H2 의 JSON 함수는 제한적. JPA 측에서 AttributeConverter 사용 중.
-- BQ 미러링 후 BQ 의 JSON_EXTRACT_ARRAY 로 unnest 권장.
-- 임시 분석은 Java 코드로 추출 (InsightsService 확장).
```

→ DB 미러링 후 BQ SQL 로 처리. C4-bq 참조.

### C4-bq. (BQ 미러링 후) 요일별 가용성

```sql
WITH unnested AS (
  SELECT
    s.year, s.month, s.weeks, s.first_day_of_week,
    p.id AS participant_id,
    day_index
  FROM warehouse.schedules_YYYYMMDD s
  JOIN warehouse.participants_YYYYMMDD p ON p.schedule_id = s.id,
  UNNEST(JSON_EXTRACT_ARRAY(p.selections)) AS day_index_json,
  UNNEST([CAST(day_index_json AS INT64)]) AS day_index
)
SELECT
  EXTRACT(DAYOFWEEK FROM DATE(year, month, 1) + INTERVAL (day_index - 1 - first_day_of_week) DAY) AS day_of_week,
  COUNT(*) AS available_count
FROM unnested
GROUP BY 1
ORDER BY 1;
```

**해석**: 어느 요일이 그룹 일정에 가장 가용성 높은가 (보통 토요일/일요일 예상, 검증).

### C5. 인기 메뉴 카테고리 (URL 도메인 기반)

```sql
SELECT
  CASE
    WHEN url LIKE '%youtube.com%' THEN 'youtube'
    WHEN url LIKE '%blog.naver.com%' THEN 'naver-blog'
    WHEN url LIKE '%instagram.com%' THEN 'instagram'
    WHEN url LIKE '%map.naver.com%' OR url LIKE '%map.kakao.com%' THEN 'map'
    WHEN url IS NULL THEN 'no-url'
    ELSE 'other'
  END AS source,
  COUNT(*) AS menu_count,
  SUM((SELECT COUNT(*) FROM menu_votes mv WHERE mv.menu_id = m.id)) AS total_votes
FROM menus m
GROUP BY source
ORDER BY total_votes DESC;
```

**해석**: 사용자가 메뉴 후보를 어디서 가져오는가 → "가져오기" UX 통합 후보 (예: 네이버 지도 빠른 검색).

### C6. 의사결정 속도 — 일정 생성 → 첫 selections 저장

```sql
SELECT
  AVG(EXTRACT(EPOCH FROM (p.updated_at - s.created_at)) / 60) AS avg_minutes,
  PERCENTILE_DISC(EXTRACT(EPOCH FROM (p.updated_at - s.created_at)) / 60, 0.5)
    WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (p.updated_at - s.created_at)) / 60) AS median_minutes
FROM schedules s
JOIN participants p ON p.schedule_id = s.id
WHERE p.updated_at > p.created_at;  -- 실제 selection 변경된 참가자만
```

**해석**: 일정 만든 직후 분 단위로 활동하는지, 며칠 후 돌아오는지. 알림 기능 우선순위 결정.

### C7. Run 출석률 시계열

```sql
SELECT
  DATE_TRUNC('week', r.date) AS week,
  COUNT(DISTINCT r.id) AS runs,
  COUNT(a.id) AS total_attendances,
  AVG(CAST(a.distance AS DOUBLE)) AS avg_distance_km
FROM runs r
LEFT JOIN attendances a ON a.run_id = r.id
GROUP BY 1
ORDER BY 1;
```

**해석**: 크루 활성도 트렌드. 시즌성, 연말 dip 등.

### C8. 휴면 멤버 식별

```sql
WITH last_attendance AS (
  SELECT participant_name, MAX(created_at) AS last_attended
  FROM attendances
  GROUP BY participant_name
)
SELECT participant_name, last_attended,
  EXTRACT(DAY FROM (CURRENT_DATE - DATE(last_attended))) AS days_inactive
FROM last_attendance
WHERE last_attended < CURRENT_DATE - INTERVAL '30' DAY
ORDER BY last_attended;
```

**해석**: 휴면 멤버 알림 기능 후보.

---

## D. BQ + DB 결합 — 미러링 활성 후

### D1. 트래픽 소스별 owner LTV

```sql
WITH owner_session AS (
  SELECT
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key='owner_id') AS owner_id,
    ARRAY_AGG(traffic_source.medium ORDER BY event_timestamp LIMIT 1)[OFFSET(0)] AS first_medium
  FROM `project.analytics_NNNNNNNNN.events_*`
  WHERE event_name='schedule_created'
  GROUP BY 1
),
owner_value AS (
  SELECT
    s.owner_id,
    COUNT(DISTINCT s.id) AS schedules_made,
    COUNT(DISTINCT p.id) AS total_participants,
    COUNT(DISTINCT lv.id) AS total_location_votes,
    COUNT(DISTINCT mv.id) AS total_menu_votes
  FROM warehouse.schedules_YYYYMMDD s
  LEFT JOIN warehouse.participants_YYYYMMDD p ON p.schedule_id = s.id
  LEFT JOIN warehouse.location_votes_YYYYMMDD lv ON lv.location_id IN (SELECT id FROM warehouse.locations_YYYYMMDD WHERE schedule_id = s.id)
  LEFT JOIN warehouse.menu_votes_YYYYMMDD mv ON mv.menu_id IN (SELECT id FROM warehouse.menus_YYYYMMDD WHERE schedule_id = s.id)
  GROUP BY s.owner_id
)
SELECT
  os.first_medium,
  COUNT(DISTINCT os.owner_id) AS owners,
  AVG(ov.schedules_made) AS avg_schedules,
  AVG(ov.total_participants) AS avg_participants,
  AVG(ov.total_location_votes + ov.total_menu_votes) AS avg_votes
FROM owner_session os
JOIN owner_value ov USING(owner_id)
GROUP BY os.first_medium
ORDER BY avg_schedules DESC;
```

**해석**: 어느 채널의 owner 가 가장 활동적인가. SEO 트래픽 vs 직접 트래픽 비교.

### D2. 페이지별 광고 수익 (AdSense 활성 후)

GA4 ↔ AdSense 연결 + AdSense 사이트 승인 필요. 연결 후 GA4 보고서 → 수익 창출 → 게시자 광고.

---

## E. Looker Studio 권장 대시보드 구성

### E1. Executive Overview (단일 페이지)

| 위젯 | 데이터 |
|---|---|
| 일별 unique users | GA4 |
| 일별 schedule_created | BQ events |
| 채널별 traffic | GA4 Acquisition |
| Top 10 검색어 | Search Console |
| Conversion funnel | BQ events |

### E2. Content Performance

| 위젯 | 데이터 |
|---|---|
| /use-cases/* 페이지별 visitors / engagement / conversion | BQ events |
| Search Console 키워드 → 페이지 매핑 | GSC + BQ |
| /insights/trends, /faq, /guide 비교 | GA4 |

### E3. Schedule Lifecycle

| 위젯 | 데이터 |
|---|---|
| 일정 생성 ~ 첫 selections 분포 | warehouse_* |
| 일정당 참가자 수 분포 | warehouse_* |
| weeks 4 vs 7 채택률 | warehouse_* |
| 메뉴/장소 투표 활성도 | warehouse_* |

### E4. Runner Crew

| 위젯 | 데이터 |
|---|---|
| 주별 런 빈도 / 참여자 | warehouse_runs/attendances |
| 멤버별 누적 거리 랭킹 | warehouse_attendances |
| 휴면 멤버 리스트 | warehouse_attendances |
