# DateDate 코드 아키텍처 분석 보고서

## Context

DateDate는 Spring Boot 4 / Java 21 / Thymeleaf SSR 기반 그룹 일정 조율 서비스로, 현재는 동작 수준에서 안정적이지만 도메인 복잡도가 늘어날 때 **어디가 먼저 구조적으로 깨질 것인가**를 식별하는 것이 이번 분석의 목적이다. 본 보고서는 실제 소스(도메인 11파일 / 애플리케이션 26파일 / 인프라 17파일 / 프레젠테이션 21파일, 약 4,351 LOC + 프론트 1,163 LOC)를 직접 읽고 도출한 결과이며, 일반론이 아니라 **파일/라인 기준의 실행 가능한 권고**를 담는다.

---

## A. 현재 구조 요약

### A-1. 레이어 구성
```
datedate/
├── domain/         ← JPA-free 순수 POJO/Record. 포트 인터페이스도 여기 있음
│   ├── owner/      Owner, OwnerRepository(port)
│   ├── schedule/   Schedule, YearMonth(Record), ScheduleRepository
│   ├── participant/ Participant, ParticipantColor(Record), ParticipantRepository
│   ├── location/   Location, LocationRepository
│   └── menu/       Menu, MenuRepository
├── application/
│   ├── service/    8개 Service (Owner/Schedule/Participant/Location/Menu/Popularity/Insights/Seo)
│   ├── exception/  12개 BusinessException 서브클래스
│   └── dto/        통계용 DTO 3개
├── infrastructure/persistence/
│   ├── entity/     JPA entity 7종 (Schedule/Participant/Owner/Location/Menu/LocationVote/MenuVote)
│   ├── repository/ 5개 JpaRepository
│   ├── adapter/    5개 Repository Adapter (port 구현)
│   └── converter/  SelectionConverter (hand-rolled JSON)
└── presentation/
    ├── api/        5개 REST Controller
    ├── controller/ 5개 MVC Controller
    └── dto/        request 8 + response 6
```

레이어 분리는 **헥사고날 원칙에 상당히 충실**하다. Domain이 JPA 의존성 없이 순수 POJO로 유지되고, Repository Adapter가 JPA 엔티티↔도메인 모델 매핑을 담당한다.

### A-2. 핵심 도메인 흐름
1. `GET /{ownerId}/{year}/{month}` → `ScheduleController.viewSchedule` (L42)
2. `ownerService.getOrCreateOwner(ownerId)` — 없으면 생성 (idempotent)
3. `scheduleService.findScheduleByOwnerAndYearMonth(...)` — null 반환
4. null이면 `scheduleService.createSchedule(...)` — **GET 요청이 쓰기** ⚠️
5. `LocationService.getLocationsByScheduleId()`, `MenuService.getMenusByScheduleId()` 추가 조회
6. `ScheduleDetailResponse.from(...)`으로 조합 → `schedule/view.html` 렌더
7. 프론트는 `window.SCHEDULE_DATA`에 전체 payload 주입 → `schedule-view.js`가 DOM 렌더

---

## B. 잘 설계된 점 (보존 권장)

1. **Domain의 JPA 분리가 실제로 지켜짐** — `Schedule.java`, `Participant.java`, `Owner.java` 모두 `jakarta.persistence` import 0개. Adapter에서 매핑(`ScheduleRepositoryAdapter`). 많은 헥사고날 프로젝트가 "포트만 있고 도메인은 JPA 엔티티"인데, 여기는 진짜 분리돼있다.
2. **YearMonth 값 객체** (`domain/schedule/YearMonth.java`) — Record로 구현, compact constructor 범위 검증, `indexToDate/dateToIndex`로 **7주 49칸 그리드 ↔ LocalDate 변환을 한 곳에 응집**. 윤년/월말 경계는 `java.time.YearMonth.lengthOfMonth()` 위임으로 버그 없음.
3. **DTO가 모두 record + `from()` 정적 팩토리** — 불변, 생성자 중복 없음, 테스트 용이.
4. **도메인 검증을 compact constructor / private validate에 집중** — Owner regex (L13, L40), Participant 이름 길이 (L42), YearMonth 범위 (L12) 모두 **생성 시점에 실패**하는 구조.
5. **Location/Menu 조회는 `LEFT JOIN FETCH` 처리됨** (`LocationJpaRepository.java` L13, `MenuJpaRepository` 동일). 투표 리스트 N+1은 이미 예방됐음.
6. **BusinessException 2-layer 분기 (`GlobalExceptionHandler` REST vs `MvcExceptionHandler` MVC)** — 에러 응답 포맷 일관성이 유지됨. 새 예외 12개가 모두 이 베이스를 상속.
7. **테스트 커버리지가 경계값 중심** — `YearMonthTest`, `ParticipantTest`, `ParticipantServiceTest`가 mock 기반으로 "8명 초과 / 중복 이름 / day 범위"를 직접 검증.

이 7가지는 **과한 리팩토링 대상이 아니다.** 이미 견고하므로 손대지 말 것.

---

## C. 핵심 위험 지점 Top 10

### 1. 🔴 [상] VARCHAR(100) 선택 저장 오버플로우
**문제:** `ParticipantJpaEntity.selections` 가 `@Column(length = 100)` (L33). `SelectionConverter.toJson([1,2,...,49])`는 `"[1,2,...,49]"` 문자열 → 실제 길이 **139자** (`2+9+80+48=139`).
**현재 버티는 이유:** 사용자가 49일 중 많아야 절반 선택 → 100자 미만으로 맞음.
**터지는 시점:** 7주 확장 모드(`EXTENDED_WEEKS=7`, `FIXED_TOTAL_DAYS=49`) 사용자가 전체를 선택하거나, **25일 이상(~100자) 선택 시 SQL 데이터 절삭** → `fromJson` 파싱 실패로 `NumberFormatException` → 500 에러.
**검증:** `[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25]` = 76자. 26번째부터는 27자 추가로 100자 초과.
**우선순위: 상 (실제 데이터 손상 가능)**

### 2. 🔴 [상] 권한 검증 완전 부재
**문제:** `SecurityConfig` L21, L47, L48에서 `/api/**`, `/*`, `/*/*/*` 모두 `permitAll`. Service/Controller 어디에도 "이 participantId가 이 ownerId 소유냐" 검증 없음.
**현재 버티는 이유:** 링크를 아는 사람만 접근 → security-by-obscurity.
**터지는 시점:**
- `DELETE /api/participants/123` — 누구나 아무 참여자 삭제 가능.
- `DELETE /api/owners/evil-user/schedules/2026/4` — scheduleId만 알면 타인 일정 삭제.
- `scheduleId`는 auto-increment `Long` → 순차 열거 공격 가능.
- **GDPR/개인정보 관점에서도 문제** — SEO 공개 여부와 무관하게 참여자 이름이 유출됨.
**우선순위: 상 (하지만 현재 규모에서는 당장 치명적이지 않음)**

### 3. 🔴 [상] GET 요청이 상태를 변경
**문제:** `ScheduleController.viewSchedule` (L58-60): URL 방문만으로 Owner + Schedule을 DB에 생성.
```java
if (schedule == null) {
    schedule = scheduleService.createSchedule(ownerId, year, month, null);
}
```
**현재 버티는 이유:** UX 의도(링크 공유 후 바로 열람). H2 로컬 DB라 부하 없음.
**터지는 시점:**
- 크롤러/프리로드/검색엔진 프리뷰가 `/random-123/2099/12` 방문만으로 쓰레기 Schedule 양산.
- sitemap/SEO 크롤링이 쓰기 트래픽이 됨. HEAD 요청은 읽기여야 하는데 `create`는 HTTP 캐시/프록시 관점에서 부적절.
- `@Transactional(readOnly=true)` 서비스를 호출하고 내부에서 `@Transactional` 쓰기 메서드가 호출되는 **트랜잭션 전파 혼란**.
**우선순위: 상**

### 4. 🟠 [중] 도메인 규칙이 Service에서 재구현됨 (도메인 공동화)
**문제:** Schedule의 도메인 메서드가 대부분 **미사용**:
- `Schedule.canAddParticipant()` (L106) ← 서비스는 `participantRepository.countByScheduleId()` 재조회
- `Schedule.hasParticipantWithName()` (L117) ← 서비스는 `participantRepository.existsByScheduleIdAndName()` 사용
- `Schedule.addParticipant()` (L110) ← 완전 미사용
- `MAX_PARTICIPANTS=8`이 `Schedule.java` L12 AND `ParticipantService.java` L22 **두 곳에 하드코딩**

**현재 버티는 이유:** 둘 다 정답(8)이라 동작은 맞음.
**터지는 시점:**
- "크루/팀 모임은 최대 12명, 친구 모임은 6명" 같이 상수가 분기되면 두 파일을 동시에 고쳐야 함 → 실수 보장.
- 참여자 초과 체크가 race condition일 때 도메인 invariant가 보장되지 않음 (count 쿼리 ↔ insert 사이에 누가 끼어들면 9명).
**우선순위: 중**

### 5. 🟠 [중] 선택 데이터 구조가 시간 확장 불가
**문제:** `Participant.selections: List<Integer>` → DB에는 `"[1,2,3]"` JSON 문자열. 시간대/응답상태 확장 시 **스키마·DTO·컨버터·검증 전부 재작성** 필요.
**현재 버티는 이유:** 현재는 "해당 날짜 가능/불가" 이진 상태만 필요.
**터지는 시점:** 요구사항 "오전/오후 분리", "가능/불가/미정" 삼중 상태, "이 날짜에 누가 답했는지 검색"(= 날짜별 집계 쿼리) 추가 시 근본 재설계.
**우선순위: 중**

### 6. 🟠 [중] 공유 링크 토큰 레이어 부재
**문제:** `ownerId`가 URL에 평문. 누구나 `/jane` 추측으로 다른 사람 대시보드 접근 가능. "소유자 관리" vs "참여자 보기" 구분이 **URL 깊이만 다를 뿐 기술적 구분 없음**.
**현재 버티는 이유:** `ownerId` 네이밍 관례상 추측 어려움 + 개인 사용.
**터지는 시점:**
- "읽기 전용 공유 링크"를 원할 때 → 현재 구조로 불가. 읽기 = 쓰기.
- "이 링크는 참여자만 볼 수 있게" → 인증 토큰 개념 자체가 없어 전면 재설계.
**우선순위: 중 (권한 모델 요구사항 나오기 전까지는 대기 가능)**

### 7. 🟠 [중] ScheduleService 이중 인터페이스 (`get` vs `find`)
**문제:** `getScheduleByOwnerAndYearMonth` (예외 throw) + `findScheduleByOwnerAndYearMonth` (null 반환) 두 메서드가 공존 (`ScheduleService.java` L32-39). 호출자가 어느 것을 쓸지 매번 판단.
**현재 버티는 이유:** MVC 컨트롤러만 null 버전을 쓰고, 나머지는 예외 버전 사용.
**터지는 시점:** 신규 개발자가 예외 버전 써야 할 곳에 null 버전을 써서 NPE 분산. `ensureOwnerExists`가 `createSchedule` 내부에 있고 `OwnerService.getOrCreateOwner`와 **동일한 로직 중복**.
**우선순위: 중**

### 8. 🟡 [하] Frontend 전역 상태 + IIFE 혼합
**문제:** `schedule-view.js` (465 LOC, 단일 파일):
- IIFE로 감싼 뒤 `window.onParticipantChange`, `window.saveSelections` 등 **14개 함수를 window에 직접 할당**
- `currentParticipantId`, `selectedDays`가 클로저 안에 갇혀 테스트/공유 불가
- `handleAddParticipant`에서 `window.location.reload()` (line 250) — 전체 페이지 리로드
- 저장 시 `renderCalendar()` **전체 DOM 재생성** (L226) — 49셀 재빌드
- 초기 payload가 `window.SCHEDULE_DATA` (global) → 이후 상태와 SSR 스냅샷이 분리됨

**현재 버티는 이유:** 파일 1개로 전부 커버되는 범위(장소/메뉴/참여자).
**터지는 시점:** 기능 추가(반복 일정, 알림, 댓글 등) 시 각각 전역 함수 하나씩 추가 → window 네임스페이스 오염 급증.
**우선순위: 하 (기능 추가 전까지는 감내 가능)**

### 9. 🟡 [하] `updateSchedule`의 anemic 재생성 패턴
**문제:** `ScheduleService.updateSchedule` (L65-83): `weeks`만 바꾸는데 **새 Schedule 객체 전체를 생성**(7 인자 생성자)하고 save. Domain에 `schedule.changeWeeks(int)` 같은 메서드가 없어 service가 불변성을 우회.
**현재 버티는 이유:** 동작은 맞음.
**터지는 시점:** "메모 / 제목 / 확정일" 필드 추가 시 생성자 인자가 10개로 늘면서 매번 7줄 복붙이 발생. 변경 invariant가 도메인에 없음.
**우선순위: 하**

### 10. 🟡 [하] SelectionConverter 손수 파싱
**문제:** `SelectionConverter.java` L22-34: `replace("[","").replace("]","").split(",").map(Integer::parseInt)` — DB가 오염되면(수동 수정, 마이그레이션 실수) **`NumberFormatException`이 터지고 이는 `BusinessException`이 아니므로 500 + 스택트레이스 노출**.
**현재 버티는 이유:** 쓰기도 같은 컨버터가 담당하므로 데이터가 깨끗함.
**터지는 시점:** 스키마 migration / CSV 일괄 수정 / H2 콘솔 수기 수정 → 복구 불가능한 500 에러.
**우선순위: 하**

---

## D. 관점별 상세 분석

### D-1. 도메인 모델링
- **Aggregate root:** Schedule이 `participants: List<Participant>`를 보유하므로 형태상 aggregate root. 그러나 **ParticipantService가 participantRepository를 직접 호출**(L47, L52, L57)해 Schedule 밖에서 Participant의 생성/삭제를 수행 → aggregate 경계가 깨짐.
- **Location/Menu의 소속 불명확:** 도메인상 Schedule의 하위여야 하지만 별도 aggregate로 취급됨 (`LocationService`가 Schedule 검증 후 `LocationRepository`로 직접 저장). Schedule에는 `locations` 필드가 없음.
- **Value Object 활용:** YearMonth/ParticipantColor는 Record로 잘 설계됨. 반면 `selections: List<Integer>`는 primitive 리스트 → "DaySelection" 같은 VO로 감쌀 여지.

### D-2. 날짜/시간 처리
- `YearMonth` (`domain/schedule/YearMonth.java`)에 **그리드 변환/월 길이/윤년 로직이 완전 응집**돼 있어 현재 최고 수준. `java.time.YearMonth.lengthOfMonth()` 위임으로 경계 버그 없음.
- **취약점:** Participant.selections가 `int day (1~49)` 인덱스 기반. 이건 YearMonth의 49칸 그리드에만 유효 → **7주 고정 모드가 사라지거나 시간대가 추가되면 의미 변경 필요**.
- 서버 렌더링(`ScheduleDetailResponse`)과 JS 렌더링(`schedule-view.js`의 `renderExtendedCalendar`)이 **그리드 좌표 계산을 중복 구현** (Java L38-52, JS L82-124). 규칙 변경 시 동기화 부채.

### D-3. 비즈니스 규칙 배치
| 규칙 | 현재 위치 | 권장 위치 |
|--|--|--|
| 참여자 최대 8명 | Domain(L12, 미사용) + Service(L22) | Domain 단독 |
| 참여자 이름 중복 | Service(L52) + Domain(L117, 미사용) | Domain + DB unique |
| (ownerId, year, month) 중복 | Service(L49) + DB unique | 유지 OK |
| day 선택 범위 | Service(L78) + Domain(L85-94) | Domain 단독 |
| Owner ID 패턴 | Domain(L13, L40) + @Valid | 유지 OK |

→ 중복 체크가 과반수. **"서비스는 오케스트레이션만, 규칙은 도메인"을 선언하고 중복 제거** 필요.

### D-4. 상태/데이터 흐름
- **초기 payload:** Thymeleaf `th:inline="javascript"`로 `window.SCHEDULE_DATA` 주입. 이후 로컬 뮤테이션(`participants.find().selections = ...`).
- **저장 후 UI:** 날짜 저장 → 전체 `renderCalendar()` 재생성(L226). 장소/메뉴 투표는 `updateLocationItemUI`로 해당 셀만 갱신(L359) → **일관성 없음**.
- **참여자 추가:** `window.location.reload()` (L250) — 가장 heavy한 방식.
- **hidden input 미사용**, 모든 데이터는 전역 객체 경유.

### D-5. API 설계
| 리소스 | 현재 URL | 평가 |
|--|--|--|
| Schedule 조회 | `GET /api/owners/{ownerId}/schedules/{year}/{month}` | scheduleId 대신 year/month 경로 → owner 종속성 명확 (좋음) |
| Schedule 삭제 | `DELETE .../schedules/{year}/{month}` | 동일 |
| Participant 추가 | `POST /api/schedules/{scheduleId}/participants` | scheduleId 기반으로 급변 (일관성 ↓) |
| Participant 삭제 | `DELETE /api/participants/{participantId}` | flat resource, parent 불명 |
| 투표 | `POST /api/locations/{id}/votes` | PATCH가 아닌 POST (RESTful 관점 부분적) |
| 투표 취소 | `DELETE /api/locations/{id}/votes/{voterName}` | **voterName을 URL에 노출** (인증 전제 필요) |

→ owner 중심 API와 schedule 중심 API가 섞여 있음. "scheduleId는 내부 PK에 불과하므로 URL에서 owner/year/month로 표현한다"는 원칙을 세우면 통일 가능.

### D-6. 권한/공유 범위
- **현재 = 모든 쓰기 API가 public.** 링크만 알면 삭제/수정/투표 가능.
- **owner vs participant 분리 메커니즘 없음.** URL 패턴만 다를 뿐.
- **단순 개인 도구 수준**에서는 "링크 = 비밀번호" 모델이 현실적. 단, DELETE 계열만이라도 간단한 ownerId 재확인(`/api/owners/{ownerId}/schedules/{year}/{month}` 경로에 이미 들어있음)은 가능.

### D-7. 템플릿/JS/CSS 구조
- **템플릿:** fragments 재사용률 높음(103회 `th:replace/insert`). `schedule/view.html` 228줄, 범위 적절.
- **JS:** `schedule-view.js` 단일 파일 465 LOC. IIFE + `window.*` 혼합 안티패턴. 모듈 분리(달력/참여자/투표) 여지 있음.
- **CSS:** `style.css` 3,990줄 단일 파일. 달력/대시보드/투표 섹션 구분 없음. 하지만 우선순위 낮음.
- **SSR↔JS 경계:** 초기 payload만 SSR, 이후 전체 AJAX. 부분 SSR(fragments 응답)은 사용 안 함 → 현재는 이게 단순해서 OK.

### D-8. 테스트 가능성
- **강점:** 도메인 검증 경계값 테스트(`YearMonthTest`, `ParticipantTest`) 충실. `ParticipantServiceTest`는 Mockito로 8명 제한/중복/범위 검증.
- **약점:**
  - Location/Menu Service 단위 테스트 부재
  - Controller 통합 테스트가 Schedule만 존재
  - Adapter 레이어(JPA 매핑) 테스트 없음 → **SelectionConverter의 VARCHAR 오버플로우를 잡아낼 테스트 부재**
  - "전체 49일 선택" 시나리오가 테스트되지 않음

### D-9. 성능/쿼리
- Schedule 조회: `findByIdWithParticipants` / `findByOwnerIdAndYearMonth` 모두 **JOIN FETCH participants** → N+1 없음.
- Location/Menu 조회: `findAllByScheduleId`도 **JOIN FETCH votes** → 좋음.
- **ScheduleController는 schedule/locations/menus를 각각 3회 쿼리** (L56, L62, L63) — 1회 통합 쿼리는 아님. 현 규모에선 OK.
- **PopularityService는 모든 Location/Menu를 메모리 로드 후 groupBy** → 데이터 100개 수준까지 OK, 1000+는 위험.

### D-10. 확장성
| 미래 기능 | 현재 구조 영향 |
|--|--|
| 반복 일정 | 불가능 — `(ownerId, year, month)` UNIQUE가 막음. Schedule이 단일 월 컨테이너. |
| 일정 제목/메모 | 쉬움 — 필드 추가로 대응 |
| 확정일 | 중간 — Schedule에 `confirmedDay`/`confirmedDate` 추가. 단 비즈니스 로직 위치 결정 필요 |
| 시간대별 가능 여부 | **어려움** — selections 문자열 재설계 필수 (#5) |
| 응답 상태(가능/불가/미정) | **어려움** — List<Integer> 기반이라 삼중값 불가. Map/별도 엔티티 필요 |
| 마감일/알림 | 쉬움 — 스케줄러 모듈 + 필드 |
| 읽기 전용 공유 | **어려움** — 토큰 레이어 없음 (#6) |

---

## E. 리팩토링 권장안

### 1. Quick Win (이번 스프린트, 1~3시간씩)

#### E-1-a. `selections` 컬럼 길이 확장 (Top10 #1)
- **무엇:** `ParticipantJpaEntity.selections` `@Column(length = 100)` → `@Column(length = 500)` 또는 `@Lob`/`TEXT`.
- **왜:** 49일 전체 선택 시 현재 스키마로 저장 실패. 기존 DB는 H2 file 기반이므로 create-drop 시 반영, 운영 배포 시 ALTER TABLE 필요.
- **기대효과:** 데이터 손상 리스크 제거.
- **영향:** `ParticipantJpaEntity.java` 1파일, Migration 스크립트 1개. 테스트 추가: "49일 선택 후 roundtrip".

#### E-1-b. `MAX_PARTICIPANTS` 중복 제거
- **무엇:** `ParticipantService.MAX_PARTICIPANTS` 상수 제거 → `Schedule.MAX_PARTICIPANTS` 참조 또는 `Schedule.canAddParticipant()` 호출.
- **왜:** Top10 #4 — 도메인 공동화. 한 줄 참조로 도메인이 규칙의 단일 출처가 됨.
- **기대효과:** 상수 변경 시 단일 지점 수정.
- **영향:** `ParticipantService.java` L22, L48 변경.

#### E-1-c. GET에서 Schedule 생성 제거
- **무엇:** `ScheduleController.viewSchedule` L58-60 제거. Schedule이 없으면 `redirect:/create?year=...&month=...` 같은 생성 폼으로 보내거나, 즉시 POST 트리거를 프론트에서 실행.
- **왜:** Top10 #3.
- **기대효과:** 크롤러 대응, 트랜잭션 의미 명확화.
- **영향:** `ScheduleController.java`, `owner/dashboard.html` (생성 플로우 UX 약간 조정).

#### E-1-d. `find` vs `get` 네이밍 컨벤션 정리
- **무엇:** `findScheduleByOwnerAndYearMonth`를 제거하고, 호출부(`ScheduleController`)에서 `Optional<Schedule> findSchedule(...)`을 직접 노출. 또는 `Optional` 반환으로 통일.
- **왜:** Top10 #7.
- **기대효과:** 호출자가 `Optional.ifPresent`로 명시적 분기.
- **영향:** `ScheduleService.java` 1파일, 호출자 2곳.

### 2. Mid-term Refactor (1~2주)

#### E-2-a. SelectionConverter를 Jackson 또는 JPA AttributeConverter로 대체
- **무엇:** 현재 static 메서드 → `@Convert(converter = SelectionJpaConverter.class)`로 JPA 표준화. Jackson ObjectMapper 주입해 파싱 실패 시 `BusinessException` 변환.
- **왜:** Top10 #10.
- **기대효과:** 잘못된 데이터에 500 대신 400 에러, 테스트 가능.
- **영향:** `SelectionConverter.java` + `ParticipantJpaEntity.java`.

#### E-2-b. 도메인 규칙을 Schedule aggregate로 집결
- **무엇:**
  - `Schedule.addParticipant(Participant)`에 "8명/중복 이름" 검증 포함 → `ParticipantLimitExceededException` / `DuplicateParticipantException` 던지게 변경.
  - `ParticipantService.addParticipant` = Schedule fetch → `schedule.addParticipant()` 호출 → save.
  - `MAX_PARTICIPANTS` 상수는 `Schedule`에만 존재.
- **왜:** Top10 #4. Aggregate invariant가 domain에 위치.
- **기대효과:** 신규 규칙 추가(예: 반복 일정) 시 Schedule 하나만 수정.
- **영향:** `Schedule.java`, `ParticipantService.java`, 관련 테스트.

#### E-2-c. Schedule 변경 메서드 도입
- **무엇:** `Schedule.changeWeeks(int)`, 향후 `Schedule.setTitle(String)`, `Schedule.confirm(int day)` 등을 도메인 메서드로 도입. Service의 "새 Schedule 재생성" 제거.
- **왜:** Top10 #9.
- **기대효과:** 필드 추가 시 service 보일러플레이트 0.
- **영향:** `Schedule.java`, `ScheduleService.updateSchedule`.

#### E-2-d. ownerId 경로 검증 미들웨어/가드
- **무엇:** `/api/owners/{ownerId}/...`, `/api/schedules/{scheduleId}/...` 쓰기 API에 "path ownerId ↔ scheduleId.ownerId 일치" 체크. Spring AOP `@OwnerCheck` 어노테이션 또는 `HandlerInterceptor`.
- **왜:** Top10 #2 부분 대응. 완전 인증은 아니지만 ID 변조/순차 열거 차단.
- **기대효과:** 타인 schedule 조작 방지.
- **영향:** `common/infrastructure/security/` 신규, 각 ApiController 어노테이션 추가.

### 3. Structural Refactor (1~2달, 요구사항 트리거 시)

#### E-3-a. selections 구조 정규화
- **무엇:** 현재 `selections VARCHAR` → 신규 테이블 `participant_selections(participant_id, day_index)` (또는 day_index + time_slot). Participant 도메인은 `Set<DaySelection>` 보유.
- **왜:** Top10 #5. 시간대/상태 확장, 날짜 역인덱스 조회.
- **기대효과:** "이 날짜에 가능한 참여자 목록 조회" 쿼리가 JOIN 한 번으로 가능.
- **영향:** 대규모. 스키마 마이그레이션 + 도메인/DTO/JS 연쇄 변경. **시간대 요구사항 확정 후 착수**.

#### E-3-b. 공유 토큰 + 권한 레이어
- **무엇:** `Schedule`에 `publicToken` (UUID) 필드. URL: `/s/{publicToken}` (참여자) vs `/{ownerId}` (소유자, 로그인 필요). 소유자 인증은 현재 Runner 어드민처럼 form login 재활용 가능.
- **왜:** Top10 #2, #6.
- **기대효과:** "읽기 전용 링크", "참여자 전용 링크" 구분 가능. SEO 공개 페이지와 비공개 페이지 분리.
- **영향:** 대규모. SecurityConfig, 새 컨트롤러, 스키마 변경, 전체 프론트 URL 재구성.

#### E-3-c. 프론트엔드 모듈화
- **무엇:** `schedule-view.js`를 ES6 module로 쪼갬. `calendar.js` / `participants.js` / `voting.js` / `api.js`. `window.SCHEDULE_DATA` 대신 `data-*` attr + `fetch('/api/schedules/.../detail')`.
- **왜:** Top10 #8.
- **기대효과:** 기능 추가 시 파일 격리. 부분 렌더링 가능.
- **영향:** 모든 datedate 템플릿 `<script>` 태그, `schedule-view.js` 전체.

---

## F. 실제 코드 기준 수정 포인트

### F-1. Backend (Java)
| 파일 | 수정 포인트 |
|--|--|
| `datedate/infrastructure/persistence/entity/ParticipantJpaEntity.java:33` | `length = 100` → `500` 또는 `@Lob` |
| `datedate/infrastructure/persistence/converter/SelectionConverter.java` | JPA `AttributeConverter` 로 교체 + Jackson 사용 |
| `datedate/application/service/ParticipantService.java:22,48` | `MAX_PARTICIPANTS` 상수 제거, `Schedule.canAddParticipant()` 호출 |
| `datedate/domain/schedule/Schedule.java:110-115` | `addParticipant`에 중복 이름 검증 통합, `DuplicateParticipantException` 직접 throw |
| `datedate/application/service/ScheduleService.java:32-39` | `find/get` 이중 메서드 → `Optional` 단일화 |
| `datedate/application/service/ScheduleService.java:65-83` | `new Schedule(...)` 재생성 패턴 → `schedule.changeWeeks(weeks)` 도메인 메서드 호출 |
| `datedate/application/service/ScheduleService.java:85-90` | `ensureOwnerExists` 제거, `OwnerService.getOrCreateOwner` 주입으로 일원화 |
| `datedate/presentation/controller/ScheduleController.java:58-60` | GET 내부 `createSchedule` 호출 제거. `model.addAttribute("needsCreation", true)` 후 뷰에서 처리 |
| `common/infrastructure/config/SecurityConfig.java:47,48` | `/*`, `/*/*/*` permitAll을 유지하되, 쓰기 API는 AOP `@OwnerCheck` 가드로 보완 |

### F-2. Frontend (JS/Template)
| 파일 | 수정 포인트 |
|--|--|
| `static/js/schedule-view.js:226` | `renderCalendar()` 전체 호출 → `updateDotsForDay(currentParticipantId, day)` 부분 갱신 |
| `static/js/schedule-view.js:250` | `window.location.reload()` → `fetch('/api/.../participants').then(updateUI)` |
| `static/js/schedule-view.js` | IIFE 해체 → ES6 module import/export. `window.*` 함수 대신 이벤트 위임(`addEventListener`) |
| `templates/schedule/view.html` | `window.SCHEDULE_DATA` 인라인 주입 → `<main data-schedule-id="..." data-initial-payload-url="/api/...">` + fetch |

### F-3. 테스트 추가
| 파일 | 추가 테스트 |
|--|--|
| `test/.../ParticipantJpaEntityTest` (신규) | "49일 selection roundtrip" (Top10 #1 회귀 방지) |
| `test/.../SelectionConverterTest` (신규) | malformed 문자열 → BusinessException 변환 검증 |
| `test/.../ScheduleTest` | `addParticipant` aggregate invariant 검증 (8명/중복) |
| `test/.../ScheduleApiControllerTest` | 다른 ownerId로 scheduleId 조작 시도 → 403 기대 (권한 가드 도입 후) |

---

## G. 추가 가이드 (CLAUDE.md 증보 후보)

### G-1. 날짜 타입 사용 원칙
- 도메인 레이어는 `java.time.LocalDate` / `java.time.YearMonth` / 프로젝트 고유 `YearMonth` VO만 사용. `int year, int month, int day`는 **YearMonth/LocalDate 생성자 파라미터로만** 허용.
- 문자열 날짜는 presentation 경계에서만(요청 파싱 직후 즉시 타입 변환). DTO record에는 `LocalDate`로 저장.
- `day 1~49` 인덱스는 **YearMonth 그리드 내부 전용 표현**. DB에는 실제 `LocalDate`로 저장하는 것이 안전.

### G-2. Validation 위치 원칙
| 유형 | 위치 |
|--|--|
| 요청 포맷(빈 값, 타입, 길이) | Controller `@Valid` + DTO Bean Validation |
| 엔티티 invariant(ID 패턴, 범위) | Domain 생성자/compact constructor |
| Aggregate invariant(참여자 8명, 중복) | Domain aggregate 메서드 |
| 중복 존재 검증(DB 상태) | Service에서 repo 조회 후 domain 메서드 호출 |
| 권한/접근 | AOP/Interceptor(경계) |

**금지:** Controller에서 비즈니스 규칙 분기, Service에서 엔티티 invariant 중복 재검증.

### G-3. Controller/Service/Domain 책임 분리
- **Controller**: HTTP ↔ DTO ↔ Service 호출. 로직 금지.
- **Service**: 여러 aggregate 조회 + 도메인 메서드 호출 + 트랜잭션 경계. "if 검증" 최소화.
- **Domain**: aggregate invariant. `throw` 는 도메인이 담당, service는 조회만.

### G-4. REST API 네이밍 규칙 제안
- 자원 URL은 소유자 계층을 따른다: `/api/owners/{ownerId}/schedules/{year}/{month}/participants/{participantId}`
- 내부 PK(`scheduleId`)를 URL에 노출하지 않는다 (열거 공격 차단).
- 상태 변경은 PATCH, 삭제는 DELETE, 생성은 POST. "투표 추가" = `POST .../votes` OK.
- 에러는 `ErrorResponse { code, message }` 단일 포맷 (이미 구현됨, 유지).

### G-5. Template/JS 분리 원칙
- 템플릿은 **초기 스냅샷**만 렌더. 이후 상태는 JS가 `/api/...`에서 페치.
- JS 함수를 `window.*`에 할당하지 않음 → `addEventListener` + `data-*` 위임.
- 페이지별 JS는 해당 템플릿에서만 로드 (공통 `scripts.html`에 전부 넣지 않음).

### G-6. 성능/쿼리 원칙
- `@OneToMany` 컬렉션을 DTO에 포함하는 Repository 메서드는 **항상 `JOIN FETCH` 또는 `@EntityGraph` 명시**.
- Service에서 컬렉션을 순회해 개별 aggregate 속성을 읽는 패턴 금지(N+1 재발 지점).
- 통계성 쿼리는 JPQL `GROUP BY` / 전용 projection 사용, 메모리 `stream().collect(groupingBy(...))`로 도피하지 않음.

---

## 검증 계획 (변경 전후)

1. **빌드/테스트:** `./gradlew build test` — 기존 테스트 유지.
2. **회귀 시나리오 (수동):**
   - Owner 생성: `/test-user/2026/4` GET → 페이지 정상 렌더, DB에 Owner+Schedule 1개.
   - 참여자 추가: 8명까지 성공, 9번째 `ParticipantLimitExceededException`.
   - 49일 전체 선택 → 저장 → DB 재조회 시 복원 (F-1 #1 수정 후 통과해야 함).
   - 중복 이름 참여자: 400 응답.
   - 다른 ownerId로 `DELETE /api/participants/{id}` 시도: E-2-d 적용 시 403.
3. **H2 콘솔 확인:** `/h2-console` → `SELECT selections FROM participants` 로 문자열 길이 확인.
4. **E2E (프론트):**
   - 브라우저에서 날짜 토글 → 저장 → 새로고침 후 유지.
   - 참여자 추가 후 전체 페이지 리로드 확인(Quick win 후 부분 갱신으로 변경 시 재검증).

## 적용 순서 권장

**Phase 1 (이번 주):** E-1-a(VARCHAR) → E-1-b(상수 통합) → E-1-c(GET mutation).
**Phase 2 (2주):** E-2-a(Converter) → E-2-b(도메인 규칙 이관) → E-2-c(Schedule 변경 메서드).
**Phase 3 (요구사항 트리거):** E-3-a, E-3-b, E-3-c.
