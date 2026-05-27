# DateDate 약속잡기 이벤트 추적 보강 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** datedate 모듈의 약속잡기 funnel 5개 누락 이벤트 (`link_shared`, `location_added`, `menu_added`, `schedule_viewed`, `owner_dashboard_viewed`) 를 GA4 BigQuery 에 보낼 수 있도록 클라이언트 dataLayer 푸시 추가 + 문서 갱신.

**Architecture:** 기존 6개 이벤트와 동일한 패턴 — `window.dataLayer.push({event, ...params})` 인라인 호출, 서버 변경 없음. 단 `is_owner` 판정과 `owner_id_hash` 계산만 신규 헬퍼 모듈 `analytics.js` 에서 제공. localStorage 마커로 owner 식별.

**Tech Stack:** Vanilla JS (ES modules), Web Crypto API (SHA-256), Thymeleaf 인라인 (`th:inline="javascript"`), Spring Boot MVC (변경 없음).

**Spec:** [`docs/superpowers/specs/2026-05-24-datedate-event-tracking-design.md`](../specs/2026-05-24-datedate-event-tracking-design.md)

---

## 파일 변경 맵

| 파일 | 변경 | 책임 |
|---|---|---|
| `src/main/resources/static/js/analytics.js` | **신규** | `sha256Hex()`, `markOwnedSchedule()`, `isOwnedSchedule()` 헬퍼 |
| `src/main/resources/static/js/schedule/utils.js` | 수정 | `copyLink()` / `fallbackCopy()` 성공 분기에 `link_shared` push |
| `src/main/resources/static/js/schedule/voting.js` | 수정 | `addLocation()` / `addMenu()` 성공 분기에 `location_added` / `menu_added` push |
| `src/main/resources/static/js/create-schedule-modal.js` | 수정 | `schedule_created` 발화 직전에 `markOwnedSchedule(ownerId)` 호출 |
| `src/main/resources/templates/schedule/create.html` | 수정 | 동상 (인라인 스크립트 내) |
| `src/main/resources/templates/schedule/view.html` | 수정 | 인라인 `schedule_viewed` push |
| `src/main/resources/templates/owner/dashboard.html` | 수정 | 인라인 `owner_dashboard_viewed` push (해시) |
| `docs/DA/01-current-state.md` | 수정 | §9 dataLayer 표에 5행 추가 |
| `docs/DA/04-todo.md` | 수정 | P0-1 정규식·DLV, P0-2 측정기준 표 갱신 |

**테스트**: 신규 JS 단위 테스트는 작성 안 함 (프로젝트에 JS 테스트 프레임워크 없음). 대신 DevTools Console 수동 검증을 마지막 태스크에. 백엔드 변경 없으므로 기존 Java 테스트 영향 없음 — `./gradlew test` 회귀 확인만.

**커밋 단위**: 각 Task 끝에 1 커밋. 마지막 Task 8 에서 docs 와 manual 검증 결과만 묶어서 머지 PR 으로.

---

## Task 1: `analytics.js` 헬퍼 모듈 신규 작성

**Files:**
- Create: `src/main/resources/static/js/analytics.js`

- [ ] **Step 1: 새 파일 작성**

`src/main/resources/static/js/analytics.js`:

```javascript
// DateDate analytics helpers.
// 기존 6개 dataLayer 이벤트는 인라인 push 유지 (P2-1 마이그레이션 범위).
// 신규 이벤트가 필요로 하는 PII 해시·owner 마커만 여기 정의.

const OWNED_SCHEDULES_KEY = 'dd_owned_schedules';

export async function sha256Hex(input) {
    if (!input || !globalThis.crypto?.subtle) return null;
    try {
        const buf = new TextEncoder().encode(String(input));
        const hash = await crypto.subtle.digest('SHA-256', buf);
        return Array.from(new Uint8Array(hash))
            .map(b => b.toString(16).padStart(2, '0')).join('');
    } catch (_) {
        return null;
    }
}

export function markOwnedSchedule(ownerId) {
    if (!ownerId) return;
    try {
        const raw = localStorage.getItem(OWNED_SCHEDULES_KEY);
        const arr = raw ? JSON.parse(raw) : [];
        if (!Array.isArray(arr)) return;
        if (!arr.includes(ownerId)) {
            arr.push(ownerId);
            localStorage.setItem(OWNED_SCHEDULES_KEY, JSON.stringify(arr));
        }
    } catch (_) { /* localStorage 비활성 / quota 초과 → 무시 */ }
}

export function isOwnedSchedule(ownerId) {
    if (!ownerId) return false;
    try {
        const raw = localStorage.getItem(OWNED_SCHEDULES_KEY);
        if (!raw) return false;
        const arr = JSON.parse(raw);
        return Array.isArray(arr) && arr.includes(ownerId);
    } catch (_) {
        return false;
    }
}

// Classic-script 호환을 위한 전역 노출 (인라인 스크립트와 non-module defer 스크립트 양쪽에서 동일 호출).
// ES module 임포트도 그대로 가능 — 두 인터페이스 병행 유지.
globalThis.DDAnalytics = { sha256Hex, markOwnedSchedule, isOwnedSchedule };
```

- [ ] **Step 2: 파일이 ES module 로 잘 로드되는지 빠르게 확인**

브라우저 콘솔에서:
```javascript
const m = await import('/js/analytics.js');
console.log(await m.sha256Hex('hello'));
// 기대: "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
m.markOwnedSchedule('test-owner');
console.log(m.isOwnedSchedule('test-owner'));
// 기대: true
localStorage.removeItem('dd_owned_schedules');
```

(앱 미실행 상태면 이 검증은 Task 8 의 수동 검증으로 미룸. Step 1 의 파일 작성만 완료하고 다음 Step.)

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/js/analytics.js
git commit -m "feat(analytics): add sha256/owned-schedule helpers for datedate events"
```

---

## Task 2: `link_shared` 이벤트 — `utils.js`

**Files:**
- Modify: `src/main/resources/static/js/schedule/utils.js`

- [ ] **Step 1: state.js 의 schedule import 추가**

`utils.js` 의 import 줄 확인 — 현재 `import { messages } from './state.js';` 만 있음. `schedule` 도 추가:

`src/main/resources/static/js/schedule/utils.js` line 1 변경:

before:
```javascript
import { messages } from './state.js';
```

after:
```javascript
import { messages, schedule } from './state.js';
```

- [ ] **Step 2: `copyLink()` clipboard 성공 분기에 push 추가**

`copyLink()` (line 9~18) 의 `navigator.clipboard.writeText(url)` `.then()` 분기에 push:

before (line 9~18):
```javascript
export function copyLink() {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(url)
            .then(() => alert(messages.linkCopied))
            .catch(() => fallbackCopy(url));
    } else {
        fallbackCopy(url);
    }
}
```

after:
```javascript
export function copyLink() {
    const url = window.location.href;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(url)
            .then(() => {
                pushLinkShared('clipboard');
                alert(messages.linkCopied);
            })
            .catch(() => fallbackCopy(url));
    } else {
        fallbackCopy(url);
    }
}

function pushLinkShared(method) {
    window.dataLayer = window.dataLayer || [];
    window.dataLayer.push({
        event: 'link_shared',
        schedule_id: schedule.scheduleId,
        share_method: method
    });
}
```

- [ ] **Step 3: `fallbackCopy()` execCommand 성공 분기에 push 추가**

`fallbackCopy()` (line 20~34) 의 `try { document.execCommand('copy'); alert(messages.linkCopied); }` 성공 분기에 push:

before (line 27~32):
```javascript
    try {
        document.execCommand('copy');
        alert(messages.linkCopied);
    } catch (err) {
        prompt(messages.linkCopyPrompt, text);
    }
```

after:
```javascript
    try {
        document.execCommand('copy');
        pushLinkShared('execCommand');
        alert(messages.linkCopied);
    } catch (err) {
        prompt(messages.linkCopyPrompt, text);
    }
```

(prompt 폴백은 실제 복사 여부 불명이므로 push 안 함. 스펙 §3.2 명시.)

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/js/schedule/utils.js
git commit -m "feat(analytics): track link_shared dataLayer event on clipboard copy"
```

---

## Task 3: `location_added` + `menu_added` 이벤트 — `voting.js`

**Files:**
- Modify: `src/main/resources/static/js/schedule/voting.js`

- [ ] **Step 1: `addLocation()` 성공 분기에 push 추가**

`addLocation()` (line 36~51). `addLocationToList(newLocation)` **직후** push (DOM 카운트 정확성).

before (line 43~51):
```javascript
    try {
        const newLocation = await window.api.addLocation(schedule.scheduleId, name);
        locations.push(newLocation);
        addLocationToList(newLocation);
        input.value = '';
    } catch (error) {
        window.toast.error(error.message);
    }
```

after:
```javascript
    try {
        const newLocation = await window.api.addLocation(schedule.scheduleId, name);
        locations.push(newLocation);
        addLocationToList(newLocation);
        input.value = '';
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'location_added',
            schedule_id: schedule.scheduleId,
            location_count_after: document.querySelectorAll('#locationList .location-item').length
        });
    } catch (error) {
        window.toast.error(error.message);
    }
```

- [ ] **Step 2: `addMenu()` 성공 분기에 push 추가**

`addMenu()` (line 107~126). `addMenuToList(newMenu)` 직후 push.

before (line 117~125):
```javascript
    try {
        const newMenu = await window.api.addMenu(schedule.scheduleId, name, url);
        menus.push(newMenu);
        addMenuToList(newMenu);
        nameInput.value = '';
        urlInput.value = '';
    } catch (error) {
        window.toast.error(error.message);
    }
```

after:
```javascript
    try {
        const newMenu = await window.api.addMenu(schedule.scheduleId, name, url);
        menus.push(newMenu);
        addMenuToList(newMenu);
        nameInput.value = '';
        urlInput.value = '';
        window.dataLayer = window.dataLayer || [];
        window.dataLayer.push({
            event: 'menu_added',
            schedule_id: schedule.scheduleId,
            menu_count_after: document.querySelectorAll('#menuList .location-item').length
        });
    } catch (error) {
        window.toast.error(error.message);
    }
```

(현 마크업상 menu 항목도 `.location-item` 클래스를 공유하므로 `#menuList` 부모 셀렉터로 한정. `templates/schedule/view.html` line 140~141 확인.)

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/js/schedule/voting.js
git commit -m "feat(analytics): track location_added/menu_added dataLayer events"
```

---

## Task 4: `schedule_created` 발화 시 owner 마커 기록

**Files:**
- Modify: `src/main/resources/static/js/create-schedule-modal.js`
- Modify: `src/main/resources/templates/schedule/create.html`

`is_owner` 판정에 쓰일 localStorage 마커를 owner 가 일정 생성한 시점에 기록.

Task 1 의 `analytics.js` 가 이미 `globalThis.DDAnalytics` 로 헬퍼를 노출하므로 모든 호출 site 는 `window.DDAnalytics?.markOwnedSchedule(ownerId)` 로 동일하게 호출. ES module / classic defer 양 환경에서 작동하므로 import 추가 / `type="module"` 변환 불필요.

- [ ] **Step 1: `create-schedule-modal.js` push 직전에 호출 추가**

현재 라인 확인:
```bash
grep -n "schedule_created" /mnt/d/projects/calendar/src/main/resources/static/js/create-schedule-modal.js
```
기대: line 74 부근 `window.dataLayer.push({ event: 'schedule_created', ... })`.

`create-schedule-modal.js` line 73~75 부근 변경:

before:
```javascript
                window.dataLayer = window.dataLayer || [];
                window.dataLayer.push({ event: 'schedule_created', owner_id: ownerId, year, month });
```

after:
```javascript
                window.DDAnalytics?.markOwnedSchedule(ownerId);
                window.dataLayer = window.dataLayer || [];
                window.dataLayer.push({ event: 'schedule_created', owner_id: ownerId, year, month });
```

- [ ] **Step 2: `templates/schedule/create.html` 인라인 스크립트에도 동일 추가**

`templates/schedule/create.html` line 52~53 변경:

before:
```javascript
                if (!res.ok) throw new Error('create failed: ' + res.status);
                window.dataLayer = window.dataLayer || [];
                window.dataLayer.push({ event: 'schedule_created', owner_id: ownerId, year: year, month: month });
```

after:
```javascript
                if (!res.ok) throw new Error('create failed: ' + res.status);
                window.DDAnalytics?.markOwnedSchedule(ownerId);
                window.dataLayer = window.dataLayer || [];
                window.dataLayer.push({ event: 'schedule_created', owner_id: ownerId, year: year, month: month });
```

- [ ] **Step 3: `analytics.js` 가 두 곳에서 모두 로드되도록 보장**

확인:
```bash
grep -rn "analytics.js" /mnt/d/projects/calendar/src/main/resources/templates/
```

기대: 0 hit (아직 어떤 템플릿도 로드 안 함). 두 곳 (owner dashboard, schedule create) 모두 로드 필요.

`templates/owner/dashboard.html` line 71 뒤, line 77 `create-schedule-modal.js` 앞에 추가:

before (line 70~78 부근):
```html
<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
<th:block th:replace="~{fragments/scripts :: scripts}"></th:block>

<!-- Owner data injection for external script -->
<script th:inline="javascript">
    window.OWNER_DATA = { ownerId: [[${ownerId}]] };
</script>
<script defer th:src="@{/js/create-schedule-modal.js}"></script>
<script defer th:src="@{/js/owner-dashboard.js}"></script>
```

after:
```html
<div th:replace="~{fragments/footer :: [th:fragment='footer']}"></div>
<th:block th:replace="~{fragments/scripts :: scripts}"></th:block>

<!-- Owner data injection for external script -->
<script th:inline="javascript">
    window.OWNER_DATA = { ownerId: [[${ownerId}]] };
</script>
<script defer th:src="@{/js/analytics.js}"></script>
<script defer th:src="@{/js/create-schedule-modal.js}"></script>
<script defer th:src="@{/js/owner-dashboard.js}"></script>
```

`templates/schedule/create.html` 도 확인 — 인라인 스크립트만 있으므로 head 또는 body 끝에 `<script defer src="/js/analytics.js"></script>` 추가 필요. 정확한 삽입 위치는 `create.html` 의 footer fragment 다음 라인.

```bash
sed -n '1,80p' /mnt/d/projects/calendar/src/main/resources/templates/schedule/create.html
```

확인 후 적절한 위치 (footer fragment 후, 기존 inline `<script>` 전) 에 한 줄 삽입:

```html
<script defer th:src="@{/js/analytics.js}"></script>
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/js/create-schedule-modal.js \
        src/main/resources/templates/owner/dashboard.html \
        src/main/resources/templates/schedule/create.html
git commit -m "feat(analytics): mark owned schedule on creation for is_owner derivation"
```

(analytics.js 자체는 Task 1 에서 이미 커밋되었으므로 본 Task 의 git add 에 포함 안 함.)

---

## Task 5: `schedule_viewed` 이벤트 — `view.html` 인라인

**Files:**
- Modify: `src/main/resources/templates/schedule/view.html`

- [ ] **Step 1: `analytics.js` 를 view.html 에서도 로드**

view.html line 200~256 영역. 기존 `create-schedule-modal.js` 와 `main.js` 사이에 추가.

before (line 199~200):
```html
<script defer th:src="@{/js/create-schedule-modal.js}"></script>

<!-- Schedule data injection for external script -->
```

after:
```html
<script defer th:src="@{/js/create-schedule-modal.js}"></script>
<script defer th:src="@{/js/analytics.js}"></script>

<!-- Schedule data injection for external script -->
```

(주의: `defer` 라 module `main.js` 보다 먼저 실행됨. `main.js` 가 module 이라 module 큐는 나중이지만, `analytics.js` 가 classic script 로 더 빠르게 `window.DDAnalytics` 노출 → `main.js` 가 임포트해도 race 없음.)

- [ ] **Step 2: `schedule_viewed` push 인라인 스크립트 추가**

view.html line 254~256 부근. 기존 SCHEDULE_DATA 블록 직후, `main.js` 로딩 직전에 새 `<script>` 블록 삽입.

before (line 253~256):
```html
    };
</script>
<script type="module" th:src="@{/js/schedule/main.js}"></script>
</body>
```

after:
```html
    };
</script>
<script th:inline="javascript">
    (function () {
        var ownerId = /*[[${ownerId}]]*/ '';
        var scheduleId = /*[[${schedule.id()}]]*/ 0;
        var participantCount = /*[[${schedule.participants().size()}]]*/ 0;
        var fire = function () {
            var isOwner = !!(window.DDAnalytics && window.DDAnalytics.isOwnedSchedule(ownerId));
            window.dataLayer = window.dataLayer || [];
            window.dataLayer.push({
                event: 'schedule_viewed',
                schedule_id: scheduleId,
                is_owner: isOwner,
                participant_count: participantCount
            });
        };
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', fire);
        } else {
            fire();
        }
    })();
</script>
<script type="module" th:src="@{/js/schedule/main.js}"></script>
</body>
```

- [ ] **Step 3: 빌드 + bootRun 후 수동 검증**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat bootRun"
```

브라우저에서 `http://localhost:8081/{ownerId}/{year}/{month}` 진입.

DevTools Console:
```javascript
window.dataLayer.filter(e => e.event === 'schedule_viewed')
```

기대: 객체 1개 — `{ event: 'schedule_viewed', schedule_id: N, is_owner: false, participant_count: N }`.

owner 본인이 만든 일정이면 `is_owner: true` 확인 (Task 4 의 마커가 이미 기록돼 있어야 함).

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/schedule/view.html
git commit -m "feat(analytics): track schedule_viewed dataLayer event with is_owner flag"
```

---

## Task 6: `owner_dashboard_viewed` 이벤트 — `dashboard.html` 인라인

**Files:**
- Modify: `src/main/resources/templates/owner/dashboard.html`

- [ ] **Step 1: 인라인 스크립트로 비동기 해시 push**

Task 4 에서 이미 `analytics.js` 가 dashboard.html 에 로드되도록 추가됐음. 이번 Task 는 push 만.

dashboard.html line 73~78 부근. OWNER_DATA 블록 다음에 신규 블록 삽입.

before (line 73~78):
```html
<!-- Owner data injection for external script -->
<script th:inline="javascript">
    window.OWNER_DATA = { ownerId: [[${ownerId}]] };
</script>
<script defer th:src="@{/js/analytics.js}"></script>
<script defer th:src="@{/js/create-schedule-modal.js}"></script>
<script defer th:src="@{/js/owner-dashboard.js}"></script>
```

after:
```html
<!-- Owner data injection for external script -->
<script th:inline="javascript">
    window.OWNER_DATA = { ownerId: [[${ownerId}]] };
    window.OWNER_DATA.scheduleCount = /*[[${schedules.size()}]]*/ 0;
</script>
<script defer th:src="@{/js/analytics.js}"></script>
<script defer th:src="@{/js/create-schedule-modal.js}"></script>
<script defer th:src="@{/js/owner-dashboard.js}"></script>
<script th:inline="javascript">
    (function () {
        var ownerId = /*[[${ownerId}]]*/ '';
        var scheduleCount = /*[[${schedules.size()}]]*/ 0;
        var fire = function () {
            if (!window.DDAnalytics || !window.DDAnalytics.sha256Hex) return;
            window.DDAnalytics.sha256Hex(ownerId).then(function (hash) {
                if (!hash) return; // PII 누출 방지: 해시 실패 시 push 안 함
                window.dataLayer = window.dataLayer || [];
                window.dataLayer.push({
                    event: 'owner_dashboard_viewed',
                    owner_id_hash: hash,
                    schedule_count: scheduleCount
                });
            });
        };
        if (document.readyState === 'loading') {
            window.addEventListener('load', fire);
        } else {
            fire();
        }
    })();
</script>
```

(주의: `defer` 스크립트 (analytics.js) 가 DOMContentLoaded 전에 실행되지만, 인라인 module 안 쓰는 인라인 script 자체도 `defer` 없으면 그 자리에서 즉시 실행. `window.DDAnalytics` 가 그 시점에 정의돼 있어야 함 → `load` 이벤트로 늦춤. `if (document.readyState === 'loading')` 분기는 readyState=interactive/complete 시 즉시 fire. 두 케이스 모두 `window.DDAnalytics` 존재 보장.)

- [ ] **Step 2: 수동 검증**

브라우저에서 `http://localhost:8081/{ownerId}` 진입.

DevTools Console:
```javascript
window.dataLayer.filter(e => e.event === 'owner_dashboard_viewed')
```

기대: 객체 1개 — `{ event: 'owner_dashboard_viewed', owner_id_hash: '<64자 hex>', schedule_count: N }`.

`owner_id_hash` 가 64자 hex 문자열 (`/^[0-9a-f]{64}$/.test(hash)`) 인지 확인.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/owner/dashboard.html
git commit -m "feat(analytics): track owner_dashboard_viewed with hashed owner_id"
```

---

## Task 7: 문서 갱신 — `docs/DA/01-current-state.md` & `04-todo.md`

**Files:**
- Modify: `docs/DA/01-current-state.md`
- Modify: `docs/DA/04-todo.md`

- [ ] **Step 1: `01-current-state.md` §9 표 5행 추가**

§9 (line 115~128) 의 마지막 행 (`attendance_marked`) 다음에 5행 추가:

```markdown
| `link_shared` | `static/js/schedule/utils.js` | clipboard / execCommand 성공 후 | `schedule_id, share_method` |
| `location_added` | `static/js/schedule/voting.js` | `addLocation()` 성공 후 | `schedule_id, location_count_after` |
| `menu_added` | `static/js/schedule/voting.js` | `addMenu()` 성공 후 | `schedule_id, menu_count_after` |
| `schedule_viewed` | `templates/schedule/view.html` (인라인) | DOMContentLoaded | `schedule_id, is_owner, participant_count` |
| `owner_dashboard_viewed` | `templates/owner/dashboard.html` (인라인) | DOMContentLoaded (비동기 해시 후) | `owner_id_hash, schedule_count` |
```

또한 §11 변경 이력에 추가:
```markdown
| 2026-05-24 | datedate dataLayer 이벤트 5종 추가 (`link_shared`, `location_added`, `menu_added`, `schedule_viewed`, `owner_dashboard_viewed`) — GTM 매핑 (P0-1) 함께 갱신 필요 |
```

- [ ] **Step 2: `04-todo.md` P0-1 정규식·DLV 갱신**

P0-1 (line 10~32) 의 "사용자 정의 변수" 와 "트리거" 갱신.

**사용자 정의 변수** (line 16~17) 행에 신규 7개 추가:
```markdown
`DLV - share_method`, `DLV - location_count_after`, `DLV - menu_count_after`, `DLV - is_owner`, `DLV - participant_count`, `DLV - owner_id_hash`, `DLV - schedule_count`
```

**트리거 정규식** (line 19~22):

before:
```
   - 이벤트 이름: `^(schedule_created|participant_added|selections_saved|vote_cast|run_created|attendance_marked)$`
```

after:
```
   - 이벤트 이름: `^(schedule_created|participant_added|selections_saved|vote_cast|run_created|attendance_marked|link_shared|location_added|menu_added|schedule_viewed|owner_dashboard_viewed)$`
```

**태그 매핑** (line 24~28) 의 "12개 DLV 매핑" 을 "19개 DLV 매핑" 으로 수정.

- [ ] **Step 3: `04-todo.md` P0-2 측정기준 표 갱신**

P0-2 (line 34~58) 의 표 (line 41~54) 마지막에 7행 추가:

```markdown
| Share Method | `share_method` | event | P1 |
| Location Count After | `location_count_after` | event | P1 |
| Menu Count After | `menu_count_after` | event | P1 |
| Is Owner | `is_owner` | event | P0 |
| Participant Count | `participant_count` | event | P1 |
| Owner ID Hash | `owner_id_hash` | event | P0 |
| Schedule Count | `schedule_count` | event | P1 |
```

line 56 의 "본 프로젝트엔 충분" 위 행에 총 측정기준 수 명시:
```markdown
> 총 측정기준 수: 12 (기존) + 7 (본 작업) = 19. 무료 한도 50 내.
```

- [ ] **Step 4: Commit**

```bash
git add docs/DA/01-current-state.md docs/DA/04-todo.md
git commit -m "docs(da): record 5 new datedate dataLayer events + GTM/dimensions backlog"
```

---

## Task 8: 통합 회귀 + 수동 검증 + Build 확인

**Files:** (코드 변경 없음)

- [ ] **Step 1: Java 회귀 테스트**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"
```

기대: BUILD SUCCESSFUL. 본 작업이 백엔드 무변경이라 모든 기존 테스트 통과해야 함. 실패 시 로그 확인 후 원인 분석.

- [ ] **Step 2: 전체 빌드**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat build"
```

기대: BUILD SUCCESSFUL.

- [ ] **Step 3: bootRun + 5개 이벤트 발화 매뉴얼 검증**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat bootRun"
```

각 이벤트별 시나리오:

| # | 액션 | URL / 단계 | 확인 콘솔 명령 |
|---|---|---|---|
| 1 | 일정 생성 (owner 마커 부수 효과) | `/test-owner-{rand}` → 모달 → 생성 | `localStorage.getItem('dd_owned_schedules')` 에 `test-owner-{rand}` 포함 |
| 2 | `owner_dashboard_viewed` | `/test-owner-{rand}` 재방문 | `window.dataLayer.filter(e => e.event === 'owner_dashboard_viewed')` 길이 ≥ 1, `owner_id_hash` 64자 hex |
| 3 | `schedule_viewed` (owner) | 위 일정의 yy/mm URL 진입 | `window.dataLayer.filter(e => e.event === 'schedule_viewed')` `is_owner: true` |
| 4 | `link_shared` | 링크 복사 버튼 클릭 | `window.dataLayer.filter(e => e.event === 'link_shared')` `share_method: 'clipboard'` |
| 5 | `location_added` | 장소 추가 | `window.dataLayer.filter(e => e.event === 'location_added')` `location_count_after` ≥ 1 |
| 6 | `menu_added` | 메뉴 추가 | `window.dataLayer.filter(e => e.event === 'menu_added')` `menu_count_after` ≥ 1 |
| 7 | `schedule_viewed` (non-owner) | 시크릿 모드에서 같은 URL | `is_owner: false` |

전 항목 통과 시 본 PR 머지 준비 완료.

- [ ] **Step 4: 회귀 확인 — 기존 6 이벤트 영향 없음**

같은 세션에서 추가로:
- 일정 생성 → `schedule_created` 발화 확인 (Task 4 변경 영향 없는지)
- 참가자 추가 → `participant_added` 발화 확인
- 가용일자 저장 → `selections_saved` 발화 확인
- 장소/메뉴 투표 → `vote_cast` 발화 확인

```javascript
window.dataLayer
    .filter(e => ['schedule_created','participant_added','selections_saved','vote_cast'].includes(e.event))
    .map(e => e.event)
```

기대: 4개 이벤트 모두 발화 흔적.

- [ ] **Step 5: PR 작성 (사용자 확인 후)**

본 단계는 사용자의 명시적 요청 이후에만 실행. 자동 push/PR 생성 금지.

PR 제목 예시: `feat(analytics): datedate funnel 5 events for GA4/BigQuery`

PR 본문 시드:
```
## Summary
- 약속잡기 funnel 5개 이벤트 (link_shared, location_added, menu_added, schedule_viewed, owner_dashboard_viewed) dataLayer 추가
- analytics.js 헬퍼 (sha256Hex, owned schedule 마커)
- docs/DA/01-current-state.md, 04-todo.md GTM/측정기준 백로그 갱신

## P0 후속 (콘솔 작업, 본 PR 외)
- GTM trigger 정규식 + 7개 신규 DLV 등록 (04-todo.md P0-1)
- GA4 맞춤 측정기준 7개 등록 (04-todo.md P0-2)

## Test plan
- [x] ./gradlew test 통과
- [x] bootRun → DevTools 에서 5개 이벤트 dataLayer 진입 확인 (owner / non-owner 양분기)
- [x] 기존 4 이벤트 (schedule_created, participant_added, selections_saved, vote_cast) 회귀 없음
```

---

## Self-Review

### 1. Spec coverage

| 스펙 항목 | 구현 Task |
|---|---|
| §3.2 `link_shared` | Task 2 |
| §3.2 `location_added` | Task 3 |
| §3.2 `menu_added` | Task 3 |
| §3.2 `schedule_viewed` (+ `is_owner` localStorage) | Task 4 (마커 기록) + Task 5 (push) |
| §3.2 `owner_dashboard_viewed` (+ `owner_id_hash`) | Task 6 |
| §3.3 `analytics.js` 헬퍼 | Task 1 |
| §5.1 `01-current-state.md` §9 갱신 | Task 7 Step 1 |
| §5.2 `04-todo.md` P0-1 갱신 | Task 7 Step 2 |
| §5.3 `04-todo.md` P0-2 갱신 | Task 7 Step 3 |
| §7.1 수동 검증 | Task 8 Step 3 |
| §8 위험 — `crypto.subtle` 비동기 | Task 6 의 `load` 이벤트 분기로 처리 |
| §8 위험 — `is_owner` 서버 컨텍스트 부재 | Task 4 의 localStorage 마커로 처리 |

모든 스펙 요구사항이 Task 로 매핑됨.

### 2. Placeholder scan

- ✅ TBD/TODO 없음
- ✅ 모든 코드 블록 실제 코드
- ✅ 모든 명령에 기대 결과 명시
- ✅ 셀렉터, 파일 라인, 함수명 정확

### 3. Type consistency

- `markOwnedSchedule(ownerId)`, `isOwnedSchedule(ownerId)`, `sha256Hex(input)` 시그니처 Task 1, 4, 5, 6 에서 동일하게 사용
- `window.DDAnalytics.*` 전역 노출 형태 Task 1, 4, 5, 6 에서 일관
- `schedule.scheduleId` (utils.js / voting.js — `state.js` 의 `schedule` 객체) 와 `schedule.id()` (Thymeleaf — Schedule 도메인 객체) 의 두 표기는 각각 정확한 context 에서 사용. 혼용 없음.
- localStorage key `dd_owned_schedules` Task 1 에서 정의, Task 1 `OWNED_SCHEDULES_KEY` 상수로 캡슐화. 외부에서 직접 안 참조 (Task 8 검증에서만 raw 로 조회).
