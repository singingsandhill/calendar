# 01. Current State — 활성 자산 인벤토리

스냅샷 일자: 2026-08-09 (직전 2026-05). 모든 ID·경로·연결 상태를 한 곳에 정리.

---

## 1. GA4 (Google Analytics 4)

| 항목 | 값 |
|---|---|
| 속성 ID | 516824378 |
| 속성명 | datedate |
| 데이터 스트림 ID | 13159527689 |
| 데이터 스트림 이름 | datedate |
| 데이터 스트림 URL | `https://www.datedate.site/` (※ 라벨, 실제 사이트는 non-www) |
| 측정 ID | `G-9QTMK4CDDF` |
| Enhanced Measurement | 기본값 (page_view, scroll, click, form_*, file_download 등) |
| BigQuery export | ✅ 활성 |
| Search Console 연결 | ✅ 2026-01-16, 비-www URL 접두사 |
| AdSense 연결 | ⏸ AdSense 미승인 |

> 주의: 스트림 URL 만 `www` 가 붙어있는 라벨 불일치. 데이터 수신엔 영향 없음.
> sitemap / canonical / robots / `app.base-url` 은 모두 non-www (`https://datedate.site`).

## 2. GTM (Google Tag Manager)

| 항목 | 값 |
|---|---|
| 컨테이너 ID | `GTM-PFPKQT7W` |
| 워크스페이스 | datedate |
| Google Tag 이름 | datedate |
| Google Tag — 태그 ID (legacy) | `G-ERBDZ6V6VN` |
| Google Tag — 태그 ID (new) | `GT-5MCR2RS2` |
| Google Tag — 대상(destination) ID | `G-9QTMK4CDDF` |

**적용 범위**:

| 페이지군 | 헤더 fragment | GTM 적용 |
|---|---|---|
| 공개 SEO 페이지 (`/`, `/guide`, `/about`, `/insights/trends`, `/use-cases/*`, `/privacy`, `/terms`, `/faq`, `/tools/date-diff`) | `fragments/head.html` | ✅ |
| UGC 일정 페이지 (`/{ownerId}`, `/{ownerId}/{y}/{m}`) | `fragments/head.html` | ✅ |
| Runner 공개 페이지 (`/runners`, `/runners/runs`, `/runners/members`, `/runners/announce`) | `fragments/head.html` | ✅ |
| Runner Admin (`/runners/admin/**`) | `fragments/head.html` | ✅ |
| **Trading 모듈** (`/trading/**`) | (별도 헤더, GTM 없음) | ❌ 의도적 제외 |
| **Stock 모듈** (`/stock/**`) | (별도 헤더, GTM 없음) | ❌ 의도적 제외 |

> runner 템플릿 11개(공개 7 + admin 4)는 전부 `fragments/head :: head(seo)` 를 참조한다.
> `templates/runners/fragments/header.html` 의 `head(seo)` fragment 는 어느 템플릿도 참조하지
> 않는 dead code 이고, 실제 참조되는 것은 같은 파일의 `navbar` fragment 뿐이다.

> Trading/Stock 은 private 봇 페이지로 robots.txt + SecurityConfig 에서 차단되며 추적·광고 제외가 맞다.

## 3. Search Console

| 항목 | 값 |
|---|---|
| 속성 형태 | URL 접두사 |
| 속성 URL | `https://datedate.site/` (non-www) |
| 검증 메타 | `YVK_KclWiLH24rqy7kAI9iNYSA5No9ljXbnSOvsQB4k` |
| 검증 위치 | `templates/fragments/head.html:23-25` — env `seo.google-site-verification` 기반(`application.yaml:18` 기본값이 운영 토큰). `templates/runners/fragments/header.html:19` 에도 하드코딩돼 있으나 그 `head` fragment 는 미참조 dead code |
| GA4 연결 일자 | 2026-01-16 |
| 연결한 계정 | cheongyakplanet@gmail.com |

## 4. Naver Search Advisor

| 항목 | 값 |
|---|---|
| 검증 토큰 | `52cf63f6fb22d9c9f017934c5d0b7d5c` |
| 검증 파일 | `static/naver52cf63f6fb22d9c9f017934c5d0b7d5c.html` |
| 메타 옵션 | `seo.naver-site-verification` 환경변수로 활성 (현재 비어있음) |
| 콘솔 등록 상태 | (사용자 확인 필요) |

## 5. IndexNow (Bing / Yandex / Naver 호환)

| 항목 | 값 |
|---|---|
| 키 | `1dfcb4404e1d4f6fae3423fd163f97b8` |
| 키 파일 위치 | `static/1dfcb4404e1d4f6fae3423fd163f97b8.txt` |
| 호스트 | `datedate.site` |
| 엔드포인트 | `https://api.indexnow.org/indexnow` |
| 활성 플래그 | `indexnow.enabled` (env `INDEXNOW_ENABLED`, 기본 false) |
| 스케줄러 | `IndexNowScheduler` — 매일 03:30 KST |
| 동작 | `SitemapService.getSitemapEntries()` 의 모든 URL (bilingual ko/en) POST |
| 실패 정책 | fail-soft (WARN 로그만, 스로우 X) |

> 이게 Google 색인 가속과 별개로 작동. Google 은 IndexNow 미참여 → GSC 사이트맵 ping 에 의존.

## 6. AdSense

| 항목 | 값 |
|---|---|
| 게시자 ID (`ads.txt`) | `pub-7334667748813914` — `static/ads.txt` 배포 완료 |
| `adsense.client` 설정값 | ⏸ **공란** (`application.yaml:26`). 비면 `AdsenseProperties.isEnabled()` 가 false → `adsbygoogle.js` 자체를 로드하지 않음 |
| 사이트 승인 상태 | ⏸ 미승인 (대기 중) |
| 코드 위치 | `templates/fragments/head.html:92-94` — `seo.adsEnabled() and adsense.enabled` 동시 충족 시에만 스크립트 삽입 |
| 광고 슬롯 ID | ⏸ 미설정 — `ADSENSE_SLOT_LEADERBOARD` / `_INFEED` / `_RECTANGLE` 환경변수 공란 (`application.yaml:27-29`). 슬롯 ID 가 없으면 `ad-slot.html` fragment 가 렌더되지 않아 광고 DOM 이 아예 없다. **placeholder `XXXXXXXXXX` 는 저장소에 0건** |
| ad-slot 호출 페이지 (5종) | `/guide`, `/faq`, `/insights/trends`, `/use-cases/{slug}`, `/tools/date-diff` |
| GA4 연결 | ⏸ 사이트 승인 후 가능 |

## 7. BigQuery Export

| 항목 | 값 |
|---|---|
| 활성화 | ✅ |
| 프로젝트 ID | (사용자 확인) |
| 데이터셋 | `analytics_NNNNNNNNN` (속성 ID 516824378 기준 자동 명명) |
| 데이터셋 위치 | (사용자 선택값 — `asia-northeast3` 권장) |
| 빈도 | (daily / streaming — 사용자 선택값) |
| 첫 적재 시각 | (사용자 활성화 후 24~48시간 내) |
| 비용 | 본 트래픽 규모에서 free tier 안 (10GB 스토리지 + 1TB 쿼리/월 무료) |

## 8. Looker Studio

| 항목 | 값 |
|---|---|
| 활성화 | ✅ |
| 데이터 소스 | BigQuery (events_*) + Search Console + GA4 connector |
| 대시보드 | (사용자 구성) |

## 9. dataLayer 비즈니스 이벤트 (코드 적용 완료)

`G-9QTMK4CDDF` 로 송신할 이벤트 **11종** 이 코드 **13개 지점** 에서 푸시된다. 아래 표는 12행이며,
`vote_cast` 행 하나가 장소·메뉴 2개 지점을 겸한다 (`schedule_created` 는 2행으로 분리 표기).
**GTM 측 트리거·태그 매핑은 대기 중**.

| 이벤트 이름 | 코드 위치 | 발화 시점 | 파라미터 |
|---|---|---|---|
| `schedule_created` | `static/js/create-schedule-modal.js` | `api.createSchedule()` 성공 후 | `owner_id, year, month` |
| `schedule_created` | `templates/schedule/create.html` | `fetch().ok` 후 | `owner_id, year, month` |
| `participant_added` | `static/js/schedule/participants.js` | `addParticipant()` 성공 후 | `schedule_id, participant_count_after` |
| `selections_saved` | `static/js/schedule/calendar.js` | `updateSelections()` 성공 후 | `schedule_id, days_count` |
| `vote_cast` | `static/js/schedule/voting.js` (×2) | toggle 성공 후 | `target('location'\|'menu'), target_id, action('vote'\|'unvote')` |
| `run_created` | `templates/runners/admin/run-form.html` | form submit (생성 시만, `th:if="${run == null}"`) | `category` |
| `attendance_marked` | `static/js/run-detail.js` | `response.ok` 분기 | `run_id, distance` |
| `link_shared` | `static/js/schedule/utils.js` | clipboard / execCommand 성공 후 | `schedule_id, share_method` |
| `location_added` | `static/js/schedule/voting.js` | `addLocation()` 성공 후 | `schedule_id, location_count_after` |
| `menu_added` | `static/js/schedule/voting.js` | `addMenu()` 성공 후 | `schedule_id, menu_count_after` |
| `schedule_viewed` | `templates/schedule/view.html` (인라인) | DOMContentLoaded | `schedule_id, is_owner, participant_count` |
| `owner_dashboard_viewed` | `templates/owner/dashboard.html` (인라인) | DOMContentLoaded (비동기 해시 후) | `owner_id_hash, schedule_count` |

## 10. 도메인 DB

`application.yaml` 의 활성 프로파일 기준:

| 항목 | 값 |
|---|---|
| Dev | H2 file (`./data/scheduledb`), MySQL 호환 모드 |
| Test | H2 in-memory, create-drop (`application.yaml:377-386`, `on-profile: test`) |
| 운영 | **H2 file** — `.env` 의 `DB_URL=jdbc:h2:file:./data/scheduledb;DB_CLOSE_DELAY=-1;MODE=MySQL`, `application.yaml:47` 이 `driver-class-name: org.h2.Driver` 로 고정, `build.gradle:49` 의 DB 드라이버는 `com.h2database:h2` 단독. 운영 전용 프로파일 없음 |

> H2 file 이므로 Datastream 등 CDC 기반 BigQuery 미러링은 불가하다. 애플리케이션이 직접 덤프를
> 내보내는 경로(GCS JSONL → BQ 외부 테이블, [04-todo P1-1](04-todo.md)) 가 사실상 유일한 선택지다.

테이블 인벤토리는 [02-data-inventory.md](02-data-inventory.md) 의 §2.

## 11. 변경 이력

| 일자 | 이벤트 |
|---|---|
| 2026-01-16 | GA4 ↔ Search Console 연결 |
| 2026-05 | dataLayer 비즈니스 이벤트 6종 코드 적용 |
| 2026-05 | BigQuery export 활성화 |
| 2026-05 | Looker Studio 파이프라인 구성 |
| 2026-05 | troubleshooting/lighthouse-performance-audit.md B 항목 정정 (`G-ERBDZ6V6VN` 오진 → 실제는 GTM Tag ID, destination = `G-9QTMK4CDDF`) |
| 2026-05-24 | datedate dataLayer 이벤트 5종 추가 (`link_shared`, `location_added`, `menu_added`, `schedule_viewed`, `owner_dashboard_viewed`) — GTM 매핑 (P0-1) 함께 갱신 필요 |
