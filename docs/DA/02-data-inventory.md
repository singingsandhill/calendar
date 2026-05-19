# 02. Data Inventory — 분석 가능한 데이터 카탈로그

분석에 사용할 수 있는 모든 데이터 소스의 스키마·의미·접근 방법.

---

## 1. GA4 이벤트

### 1.1 자동 수집 (Enhanced Measurement)

| 이벤트 | 트리거 | 주요 파라미터 |
|---|---|---|
| `page_view` | 모든 페이지 로드 | `page_location`, `page_title`, `page_referrer` |
| `session_start` | 세션 시작 | `session_id`, traffic source/medium/campaign |
| `first_visit` | 신규 사용자 첫 페이지뷰 | — |
| `user_engagement` | 활성 engagement (10초+) | `engagement_time_msec` |
| `scroll` | 90% 스크롤 | — |
| `click` | outbound link 클릭 | `link_url`, `link_domain` |
| `form_start` | 폼 첫 인터랙션 | `form_id`, `form_name` (라벨 부재) |
| `form_submit` | 폼 제출 | `form_id` |
| `file_download` | 다운로드 | `file_extension`, `file_name` |

### 1.2 커스텀 이벤트 (코드 푸시, GTM 매핑 대기)

`G-9QTMK4CDDF` 로 송신될 6종. 상세는 [01-current-state.md §9](01-current-state.md#9-datalayer-비즈니스-이벤트-코드-적용-완료).

각 이벤트의 분석 의미:

| 이벤트 | 분석 의미 |
|---|---|
| `schedule_created` | 핵심 conversion. 신규 일정 생성률, 트래픽 소스 attribution |
| `participant_added` | 일정 활성화 시그널. 평균 참가자 수, drop-off (참가자 1명 만 추가 후 이탈) |
| `selections_saved` | 핵심 engagement. days_count 분포로 "전체 가능"/"몇 일만 가능" 패턴 |
| `vote_cast` | 일정 후속 활동. 장소·메뉴 어느 쪽이 더 활발한가, vote vs unvote 비율 |
| `run_created` | Runner admin 활동. 크루 운영자 활성도 |
| `attendance_marked` | 크루 멤버 활성도. distance 분포, 출석 시간대 |

### 1.3 통합 데이터 소스

| 소스 | 연결 상태 | 데이터 |
|---|---|---|
| Search Console | ✅ | 검색쿼리, 페이지별 클릭/노출/CTR/평균순위 |
| AdSense | ⏸ | 광고 노출/클릭/RPM/수익 (사이트 승인 후) |
| Google Signals | (확인 필요) | 추정 인구통계, cross-device |
| Google Ads | (미연동) | 캠페인 성과, conversion (Ads 운영 시) |

## 2. 도메인 DB 테이블

### 2.1 datedate 모듈 (UGC, noindex, 분석 풍부)

| 테이블 | 컬럼 | 핵심 의미 |
|---|---|---|
| `owners` | id, owner_id (slug), created_at | 신규 owner 코호트 진입 |
| `schedules` | id, owner_id, year, month, weeks (4 또는 7), created_at | 일정 생성 활동, 모드 채택률 |
| `participants` | id, schedule_id, name, color (0-7), **selections (JSON `List<Integer>`)**, updated_at | **selections 가 핵심 자산** — 일정의 day-index 가용성 |
| `locations` | id, schedule_id, name, created_at | 일정의 장소 후보 |
| `location_votes` | id, location_id, voter_name | 장소 투표 |
| `menus` | id, schedule_id, name, **url**, created_at | 일정의 메뉴 후보 (외부 URL 포함) |
| `menu_votes` | id, menu_id, voter_name | 메뉴 투표 |

**Selections JSON 구조**:

- `weeks=7` (확장 모드): 49일 (`1` ~ `49`) 의 day index
- `weeks=4` (legacy): 해당 달의 일수 (`1` ~ `daysInMonth`) 의 day index
- 1번 day 가 어느 실제 날짜에 매핑되는지는 `Schedule.firstDayOfWeek` + `year/month` 로 계산
- 각 참가자가 자신의 가능 일자 집합을 저장

→ JSON unnest + day index → 실제 날짜 매핑 → 요일 추출 → **요일별 가용성 히트맵** 가능.

### 2.2 runner 모듈

| 테이블 | 컬럼 | 핵심 의미 |
|---|---|---|
| `runs` | id, date, time, location, category (RunCategory enum), created_at | 런 빈도, 시간대·요일 패턴, 장소 인기 |
| `attendances` | id, run_id, participant_name, **distance (BigDecimal precision=4 scale=1)**, created_at | 멤버별 누적 거리, 출석률, 거리 분포 |
| `runner_admins` | (보안) | — |

**Unique constraint**: `attendances(run_id, participant_name)` — 한 멤버는 한 런에 한 번만 출석.

### 2.3 stock / trading 모듈

분석 대상에서 **제외**. private 봇 운영 데이터로 GA4·BQ 와 결합 의미 없음.

(단 운영 모니터링용 별도 분석은 `docs/stock-bot.md`, `docs/trading-bot.md` 참조)

## 3. BigQuery — events_* 테이블 구조

```
프로젝트: (사용자 GCP 프로젝트)
└─ analytics_NNNNNNNNN (NNN = 속성 ID 516824378 기반)
    ├─ events_YYYYMMDD          ← daily, 다음 날 적재
    ├─ events_intraday_YYYYMMDD ← streaming (활성 시), 거의 실시간
    ├─ pseudonymous_users_YYYYMMDD
    └─ users_YYYYMMDD
```

`events_*` 핵심 컬럼:

| 컬럼 | 타입 | 의미 |
|---|---|---|
| `event_date` | STRING (YYYYMMDD) | 이벤트 일자 |
| `event_timestamp` | INT64 (microsec) | 이벤트 시각 |
| `event_name` | STRING | `page_view`, `schedule_created` 등 |
| `event_params` | REPEATED RECORD | key + value (string/int/double/float) |
| `user_pseudo_id` | STRING | 가명 사용자 ID (cookie 기반) |
| `user_id` | STRING | 명시적 user_id 송신 시 (현재 미사용) |
| `user_properties` | REPEATED RECORD | user property (현재 미사용) |
| `device.*` | RECORD | 카테고리, 브랜드, 모바일 모델, OS, 브라우저 |
| `geo.*` | RECORD | 국가, 도시, 지역 |
| `traffic_source.*` | RECORD | source, medium, campaign |
| `page_location` | (event_params 안) | URL 전체 |

→ `UNNEST(event_params)` 로 풀어서 SQL JOIN 가능.

## 4. 도메인 DB → BigQuery 미러링 (미구현)

GA4 events 와 결합 분석하려면 도메인 DB 데이터가 BQ 안에 있어야 한다. 미구현 상태.

옵션:

| 옵션 | 작업량 | 적합도 |
|---|---|---|
| GCS daily JSONL dump → BQ external table | 작음 | ✅ 본 프로젝트 추천. H2 SQL 로 daily 추출 후 GCS 업로드, BQ 외부 테이블만 등록 |
| Datastream (GCP managed CDC) | 중간 | DB가 PostgreSQL/MySQL 일 때만. H2 미지원 |
| Application 코드에서 직접 BQ insert | 작지만 결합도 ↑ | 비추 — 도메인 코드 오염 |
| dbt Cloud + BQ | 중간~큼 | 데이터 모델링 본격화 시 |

### 권장 미러링 테이블 스펙 (목표 스키마)

```sql
-- BQ dataset: warehouse
-- 매일 자정 KST 직후 full snapshot 적재 (점진 변경 추적은 추후)

CREATE TABLE warehouse.owners_YYYYMMDD AS
  SELECT id, owner_id, created_at FROM owners;

CREATE TABLE warehouse.schedules_YYYYMMDD AS
  SELECT id, owner_id, year, month, weeks, created_at FROM schedules;

CREATE TABLE warehouse.participants_YYYYMMDD AS
  SELECT id, schedule_id, name, color, selections, updated_at FROM participants;

-- 등...
```

→ 자세한 구현 계획은 [04-todo.md](04-todo.md) §P1 참조.

## 5. 데이터 흐름도

```
[브라우저]
   ↓ page_view (자동)
   ↓ dataLayer.push (코드 6종)
   ↓
[GTM-PFPKQT7W]
   ↓ Google Tag (G-ERBDZ6V6VN / GT-5MCR2RS2)
   ↓ destination: G-9QTMK4CDDF
   ↓
[GA4 속성 516824378]
   ├─→ GA4 보고서 / Explorations (UI)
   ├─→ Search Console 통합
   └─→ BigQuery export (daily)
              ↓
       [BigQuery analytics_*]
              ↓
       [Looker Studio]
              ↑
       [BigQuery warehouse_*]   ← 도메인 DB 미러링 (미구현)
              ↑
       [H2 file DB / 운영 DB]
```

## 6. PII 고려사항

GA4 정책상 PII (이메일, 전화번호, 이름) 의 **이벤트 파라미터 송신은 금지**. 본 프로젝트의
주의 지점:

| 데이터 | PII 가능성 | 현재 처리 |
|---|---|---|
| `owner_id` (slug) | 사용자가 자기 이름·이메일을 슬러그로 쓰면 PII | 현재 그대로 송신 (URL path 에 이미 노출) |
| `participant.name` | 사람 이름 | dataLayer 에 포함 안 함 |
| `voter_name` | 사람 이름 | dataLayer 에 포함 안 함 |
| `participant_count_after` | 카운트 | 안전 |
| `distance` | 운동 거리 | 안전 |
| GA4 `user_pseudo_id` | 가명 (쿠키) | 안전 |

→ 향후 owner_id 를 SHA-256 해시 후 user_property 로 송신하는 것이 권장. [04-todo.md §P2](04-todo.md) 참조.
