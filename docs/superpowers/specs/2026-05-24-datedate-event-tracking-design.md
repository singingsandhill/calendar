# DateDate 약속잡기 이벤트 추적 보강 — 설계 스펙

- 일자: 2026-05-24
- 상태: Draft
- 범위: datedate 모듈의 약속잡기 funnel을 GA4 / BigQuery에서 측정 가능하게 만들기 위한 클라이언트 이벤트 5종 추가
- 관련 문서: [`docs/DA/01-current-state.md`](../../DA/01-current-state.md), [`docs/DA/04-todo.md`](../../DA/04-todo.md)

---

## 1. 문제 정의

datedate 모듈의 약속잡기 funnel에는 6개의 사용자 액션이 있고, 그중 4개만 클라이언트
dataLayer 이벤트로 푸시되고 있다. 게다가 GTM 트리거·태그 매핑 (P0-1) 과 GA4 맞춤
측정기준 등록 (P0-2) 이 미완료 상태라 푸시된 이벤트도 GA4 / BigQuery 에 아직 도달하지
않는다.

본 스펙은 **코드 측 누락분 5개 이벤트를 추가**한다. P0-1 / P0-2 는 콘솔 작업이므로 본
PR 범위 밖이며, 본 PR 머지 시점에 함께 매핑되도록 04-todo.md 에 반영한다.

### 현재 추적 매트릭스

| 약속잡기 액션 | 코드 푸시 | GA4 도달 | 본 스펙 범위 |
|---|---|---|---|
| 일정 생성 | ✅ | ❌ (P0-1 대기) | — |
| 참가자 추가 | ✅ | ❌ (P0-1 대기) | — |
| 가용 일자 저장 | ✅ | ❌ (P0-1 대기) | — |
| 장소/메뉴 투표 | ✅ | ❌ (P0-1 대기) | — |
| 링크 공유 | ❌ | ❌ | **추가** |
| 장소 추가 | ❌ | ❌ | **추가** |
| 메뉴 추가 | ❌ | ❌ | **추가** |
| 일정 뷰 (funnel 진입점) | ❌ | ❌ | **추가** |
| 오너 대시보드 뷰 | ❌ | ❌ | **추가** |

---

## 2. 목표 / 비목표

### 목표
- datedate funnel의 다음 5개 이벤트를 dataLayer 에 추가:
  `link_shared`, `location_added`, `menu_added`, `schedule_viewed`,
  `owner_dashboard_viewed`
- 기존 6개 이벤트와 동일한 컨벤션 (snake_case, client-side push) 으로 일관성 유지
- PII 정책: 신규 이벤트의 `owner_id` 는 처음부터 SHA-256 해시 (`owner_id_hash`) 로만
  송신 → P2-1 마이그레이션과 충돌 없음
- 문서: `docs/DA/01-current-state.md` §9 와 `docs/DA/04-todo.md` P0-1·P0-2 에 신규
  이벤트·파라미터 반영

### 비목표
- **GTM 콘솔 작업 (P0-1)**: 본 PR과 별개. 본 PR 머지 후 04-todo.md 의 정규식과 DLV
  목록 따라 매핑.
- **GA4 맞춤 측정기준 등록 (P0-2)**: 동상.
- **`schedule_finalized` / 날짜 확정 이벤트**: 도메인에 finalize 개념 자체가 없음.
  도메인 기능 추가 PR과 묶을 일.
- **`schedule_deleted`**: 분석 가치 낮음 (현재 운영 신호로만 의미).
- **서버사이드 Measurement Protocol**: 봇/no-JS 캡처용. 본 트래픽 규모에서 비용 대비
  효용 낮음. P3 로 유보.
- **기존 6개 이벤트의 `owner_id` 해시 마이그레이션**: P2-1 범위. 본 스펙은 신규
  이벤트만 처음부터 해시로.

---

## 3. 신규 이벤트 명세

### 3.1 컨벤션 (기존 일치)
- 이름: snake_case, 과거형 (`*_added`, `*_viewed`, `*_shared`)
- 송신: `window.dataLayer.push({ event: '<name>', <params> })`
- 실행: API 응답 성공 분기 직후 (조회성 이벤트는 `DOMContentLoaded` 1회)
- 실패 시 push 안 함 (현재 패턴 동일)

### 3.2 이벤트별 정의

#### `link_shared`
- **파일**: `src/main/resources/static/js/schedule/utils.js`
- **함수**: `copyLink()` 의 clipboard 성공 분기 (line 13) 와 `fallbackCopy()` 의
  `execCommand('copy')` 성공 분기 (line 28). `prompt()` 폴백은 사용자가 실제로 복사
  했는지 알 수 없으므로 push 안 함.
- **파라미터**:
  - `schedule_id`: number — `window.scheduleState.scheduleId` 에서 조회 (`state.js`)
  - `share_method`: `'clipboard' | 'execCommand'`

#### `location_added`
- **파일**: `src/main/resources/static/js/schedule/voting.js`
- **함수**: `addLocation()` (line 36) 의 `await window.api.addLocation(...)` 성공 후
  `addLocationToList(newLocation)` 호출 **직후** 에 push (DOM 카운트 신뢰성 확보).
- **파라미터**:
  - `schedule_id`: number — `schedule.scheduleId`
  - `location_count_after`: number — `document.querySelectorAll('#locationList .location-item').length`

#### `menu_added`
- **파일**: 동상 (`voting.js`)
- **함수**: `addMenu()` (line 107) 의 `await window.api.addMenu(...)` 성공 후
  `addMenuToList(newMenu)` **직후** push.
- **파라미터**:
  - `schedule_id`: number
  - `menu_count_after`: number — `document.querySelectorAll('#menuList .location-item').length` (현 마크업상 location/menu 둘 다 `.location-item` 클래스를 공유하므로 부모 `#menuList` 로 한정)

#### `schedule_viewed`
- **파일**: `src/main/resources/templates/schedule/view.html` (인라인 `<script
  th:inline="javascript">`)
- **트리거**: `DOMContentLoaded` 시 1회. SPA 가 아니므로 페이지 로드당 1회로 충분.
- **파라미터**:
  - `schedule_id`: number — Thymeleaf 모델에서 인라인 주입
  - `is_owner`: boolean — datedate는 익명 UGC 라 서버측 owner 세션이 없으므로
    **클라이언트 localStorage 마커** 로 판정. 키: `dd_owned_schedules` (JSON 배열).
    `schedule_created` 발화 시점에 path `ownerId` 를 배열에 추가. `schedule_viewed`
    에서 path `ownerId` 가 배열에 포함되면 `true`. 다른 브라우저/시크릿모드에서
    보면 `false` — 정확도 한계 있으나 같은 디바이스 내 owner 식별로 충분.
  - `participant_count`: number — 모델의 참가자 수
- **주의**: 뷰 자체는 GTM 의 enhanced measurement `page_view` 와 별개로, **funnel
  단계 식별** 을 위한 비즈니스 이벤트로 별도 송신. GA4 에서 `page_view` 와 중복돼
  보여도 funnel 정의 시 비즈니스 이벤트만 쓰면 됨.

#### `owner_dashboard_viewed`
- **파일**: `src/main/resources/templates/owner/dashboard.html` (인라인 스크립트)
- **트리거**: `DOMContentLoaded` 1회
- **파라미터**:
  - `owner_id_hash`: string — **SHA-256 hex** (64자). 클라이언트 `crypto.subtle.digest`
    로 비동기 계산 후 push. 계산 실패 시 push 안 함 (PII 누출 방지).
  - `schedule_count`: number — 모델에서 인라인 주입

### 3.3 해시 헬퍼

`src/main/resources/static/js/analytics.js` (신규) — 한 곳에서 관리:

```javascript
// 신규 파일: analytics.js
export async function sha256Hex(input) {
    if (!input || !crypto?.subtle) return null;
    const buf = new TextEncoder().encode(String(input));
    const hash = await crypto.subtle.digest('SHA-256', buf);
    return Array.from(new Uint8Array(hash))
        .map(b => b.toString(16).padStart(2, '0')).join('');
}
```

P2-1 시 기존 이벤트도 이 헬퍼를 재사용.

---

## 4. funnel 가시화 (구현 후)

```
schedule_viewed → link_shared → (초대받은 사용자) schedule_viewed
                                                ↓
                              participant_added → selections_saved
                                                ↓
                          (location_added / menu_added) → vote_cast
```

GA4 Exploration > Funnel 에서 위 단계로 정의 가능. BigQuery 에서는
`events.event_name IN (...)` 로 stage 별 cohort 추출.

---

## 5. 문서 변경

### 5.1 `docs/DA/01-current-state.md` §9 dataLayer 표

다음 5행 추가:

| 이벤트 이름 | 코드 위치 | 발화 시점 | 파라미터 |
|---|---|---|---|
| `link_shared` | `static/js/schedule/utils.js` | clipboard / execCommand 성공 후 | `schedule_id, share_method` |
| `location_added` | `static/js/schedule/voting.js` | `addLocation()` 성공 후 | `schedule_id, location_count_after` |
| `menu_added` | `static/js/schedule/voting.js` | `addMenu()` 성공 후 | `schedule_id, menu_count_after` |
| `schedule_viewed` | `templates/schedule/view.html` | DOMContentLoaded | `schedule_id, is_owner, participant_count` |
| `owner_dashboard_viewed` | `templates/owner/dashboard.html` | DOMContentLoaded | `owner_id_hash, schedule_count` |

### 5.2 `docs/DA/04-todo.md` P0-1 (GTM 매핑)

- 정규식 트리거 패턴 갱신:
  `^(schedule_created|participant_added|selections_saved|vote_cast|run_created|attendance_marked|link_shared|location_added|menu_added|schedule_viewed|owner_dashboard_viewed)$`
- 사용자 정의 변수 (DLV) 추가:
  `DLV - share_method`, `DLV - location_count_after`, `DLV - menu_count_after`,
  `DLV - is_owner`, `DLV - participant_count`, `DLV - owner_id_hash`,
  `DLV - schedule_count`

### 5.3 `docs/DA/04-todo.md` P0-2 (GA4 맞춤 측정기준)

다음 7행 추가 (모두 event scope):

| 등록 이름 | 매개변수 | 범위 | 우선순위 |
|---|---|---|---|
| Share Method | `share_method` | event | P1 |
| Location Count After | `location_count_after` | event | P1 |
| Menu Count After | `menu_count_after` | event | P1 |
| Is Owner | `is_owner` | event | P0 |
| Participant Count | `participant_count` | event | P1 |
| Owner ID Hash | `owner_id_hash` | event | P0 |
| Schedule Count | `schedule_count` | event | P1 |

총 측정기준 수: 기존 12 + 신규 7 = **19** (무료 GA4 한도 50개 내).

---

## 6. 컴포넌트 경계 / 의존성

| 단위 | 책임 | 의존 |
|---|---|---|
| `analytics.js` (신규) | 해시 헬퍼. push 자체는 호출자가. | Web Crypto API |
| `schedule/utils.js` | `copyLink()` 내 push 1줄 추가 | `state.js`, dataLayer |
| `schedule/voting.js` | `addLocation()`, `addMenu()` 내 push 각 1줄 | dataLayer |
| `schedule/view.html` (인라인) | `schedule_viewed` push | Thymeleaf 모델 |
| `owner/dashboard.html` (인라인) | `owner_dashboard_viewed` push | `analytics.js` 의 `sha256Hex` |

기존 함수 시그니처 변경 없음. 추가만.

---

## 7. 테스트

이벤트 로직은 외부 시스템 (dataLayer) 부수효과뿐이라 단위 테스트 가치 낮음. 대신:

### 7.1 수동 검증 체크리스트 (PR 머지 직후, P0-1 매핑 전)
1. 로컬에서 각 액션 수행
2. DevTools > Console 에서 `window.dataLayer` 배열 확인
3. 5개 이벤트의 event 이름·파라미터·타입이 명세대로인지

### 7.2 GTM DebugView (P0-1 완료 후)
1. Tag Assistant 로 사이트 진입
2. 각 액션 수행
3. DebugView 에서 5개 이벤트 + 파라미터 노출 확인

### 7.3 BigQuery 도달 (P0-1 완료 후 24~48h)
```sql
SELECT event_name, COUNT(*) AS n
FROM `proj.analytics_516824378.events_*`
WHERE event_name IN (
  'link_shared','location_added','menu_added',
  'schedule_viewed','owner_dashboard_viewed'
)
  AND _TABLE_SUFFIX BETWEEN FORMAT_DATE('%Y%m%d', CURRENT_DATE() - 7)
                        AND FORMAT_DATE('%Y%m%d', CURRENT_DATE())
GROUP BY event_name;
```

5개 모두 row 가 나오면 성공.

---

## 8. 위험 / 트레이드오프

| 위험 | 완화 |
|---|---|
| `crypto.subtle` 가 비동기 → `DOMContentLoaded` 시점에 push 가 늦어질 수 있음 | `owner_dashboard_viewed` 만 비동기. 페이지 이탈 전 처리될 시간 충분 (수십 ms). |
| `is_owner` 판단을 서버에서 해야 함 (현재 컨텍스트 확인 필요) | 구현 단계에서 `ScheduleController` 검토. owner 식별 컨텍스트가 없으면 cookie/path 기반으로 분기. |
| `participant_count` 가 큰 일정에서 매번 송신 | 정수 1개라 비용 무시 가능. |
| 신규 이벤트가 GTM 매핑 전까지 BQ 에 안 보임 | 본 스펙 §1 에 명시. 04-todo.md 갱신으로 매핑 작업이 자연스럽게 신규 이벤트 포함하게 됨. |
| `vote_cast` 등 기존 이벤트가 `owner_id` raw 송신 중이라 PII 일관성 깨짐 | P2-1 에서 마이그레이션 예정. 본 스펙은 신규 이벤트만 처음부터 해시. |

---

## 9. 작업 분해 (writing-plans 단계 입력)

1. `static/js/analytics.js` 신규 — 해시 헬퍼
2. `static/js/schedule/utils.js` — `link_shared` push 2곳
3. `static/js/schedule/voting.js` — `location_added`, `menu_added` push 2곳
4. `templates/schedule/view.html` — 인라인 스크립트 추가 (`schedule_viewed`)
5. `templates/owner/dashboard.html` — 인라인 스크립트 추가 (`owner_dashboard_viewed`)
6. `ScheduleController` / `OwnerApiController` — 인라인 주입 필드 (`is_owner`,
   `participant_count`, `schedule_count`) 확인·노출
7. `docs/DA/01-current-state.md` — §9 표 5행 추가
8. `docs/DA/04-todo.md` — P0-1 정규식·DLV, P0-2 측정기준 표 갱신
9. 수동 검증 (§7.1)
