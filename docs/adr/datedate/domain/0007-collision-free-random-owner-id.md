# 0007. 랜덤 owner ID — 미사용 보장 + 공간 1000배 확대

- Status: Accepted
- Date: 2026-08-13

## Context

홈(`/`)의 "랜덤 생성" 버튼은 순수 클라이언트 함수였다. 형용사 12 × 명사 12 × 숫자 100 =
**14,400 조합**을 뽑을 뿐, 그 ID 가 이미 쓰이는지 확인하지 않았다 (`index.html` 에 `fetch`
0건, 서버에도 가용성 확인 엔드포인트 없음).

`POST /start` 는 `OwnerService.getOrCreateOwner` — get-**or**-create 라 이미 존재하는 ID 여도
예외 없이 통과하고 `/{ownerId}` 로 리다이렉트한다. 그래서 충돌 시 두 가지가 일어났다.

1. **남의 대시보드로 착지.** `OwnerController.dashboard` 는 소유권 검사 없이 그 owner 의
   일정 전체를 렌더링한다.
2. **남의 페이지 선점.** 카카오 로그인 상태이고 기존 owner 가 미연결이면
   `getOrCreateOwner(ownerId, userId)` 가 `linkUser()` 를 호출한다. 원 생성자는 이후 영구히
   연결 불가 (`Owner.linkUser` → `OwnerAlreadyLinkedException`). ADR
   [0005](0005-user-activity-event-recap.md) 는 이 first-claim 을 수용하면서 "악의 선점 구제는
   수동(DB)" 이라 적어 뒀다 — 그 수동 구제가 사고로 발동될 확률이 문제였다.

생일 문제로 랜덤 생성 누적 **141건이면 충돌 확률 50%**, 300건이면 96%
(`1 − e^(−n(n−1)/28800)`). 충돌은 예외가 아니라 기대값이었다.

## Decision

미사용 보장을 세 층으로 나눈다. 한 층만으로는 보장이 성립하지 않는다.

1. **공간을 14,400 → 14,400,000 으로 (1000배).**
   `OwnerIdGenerator` (도메인, Spring 무관) 가 `{adj}-{noun}-{NNNN}` 을 만든다 —
   형용사 40 × 명사 40 × 1000~9999. 단어는 각 2~7자로 제한해 최장 조합도 owner ID 상한
   20자를 넘지 않는다. `RandomGenerator` 를 주입받아 테스트에서 시드를 고정한다
   (stock 모듈이 `Clock` 을 주입하는 것과 같은 방식). 기본값 `SecureRandom` — 예측 가능한
   시퀀스면 다음 사용자에게 제안될 ID 를 미리 선점해 방해할 수 있다.
2. **제안 시점 확인.** `GET /api/owner-ids/random` 이
   `OwnerService.generateAvailableOwnerId()` 로 `existsById` 를 통과한 후보만 반환한다
   (최대 10회 재추첨, 소진 시 `IllegalStateException` → 500 → 클라이언트가 재시도).
   **DB 쓰기가 없어** ADR [0004](0004-no-owner-auto-create-on-get-dashboard.md) 의
   "GET 무변형 / 봇 발 row 생성 차단" 원칙을 유지한다.
3. **제출 시점 재확인 = 실제 보장.** 폼의 hidden `generated` 플래그가 켜져 있으면
   `POST /start` 는 `getOrCreateOwner` 대신 `OwnerService.createOwner` 를 호출한다 —
   이미 존재하면 `OwnerIdTakenException`(409). 사용자가 입력칸을 한 글자라도 고치면
   `input` 핸들러가 플래그를 내려 기존 재진입 경로로 돌아간다.

### 엔드포인트가 `/api/owners/**` 아래에 없는 이유

`OwnerPathInterceptor` 가 `/api/owners/**` 의 첫 세그먼트를 ownerId 로 간주해 형식·예약어를
검증하며, `"random"` 은 `ReservedOwnerIds` 의 예약어라 400 이 된다. 보안 인터셉터에 경로
예외를 뚫는 대신 자원 자체를 분리했다 — 반환값은 owner 가 아니라 "아직 owner 가 아닌 ID
후보" 이므로 `/api/owner-ids` 가 의미상으로도 맞다.

## 기각한 대안

| 대안 | 기각 이유 |
|---|---|
| 클라이언트가 생성하고 `GET /api/owners/{id}` 로 폴링 | N 왕복 + 여전히 제안 시점 보장뿐. 단어 목록이 JS·서버 이중 관리 |
| 제안 시점에 owner row 를 선점(예약) | ADR 0004 의 GET 무변형 위반 + 미제출 쓰레기 row |
| `POST /start` 를 전면 strict-create 로 | 가입 없는 서비스의 재진입(같은 ID 재입력 = 내 페이지 복귀) UX 파괴 |
| 숫자만 4자리로 (12×12×9000) | 공간 90배에 그치고 단어쌍 반복이 그대로 노출 |
| base36 접미 4자 | 26.9억 조합이나 공개 URL 을 말로 전달하기 어려움 (0/o, 1/l 혼동) |
| `OwnerPathInterceptor` 에 `/api/owners/random` 예외 추가 | 인증·검증 경계에 리터럴 구멍. 자원 분리로 회피 가능 |

## Consequences

- 랜덤 버튼은 이제 네트워크에 의존한다. 1회 클릭 안에서 3회(300ms → 900ms 백오프) 재시도하고,
  그래도 실패하면 `index.id.generateFailed` 를 노출한다. 로컬 생성 폴백은 두지 않는다 —
  보장이 깨지고 단어 목록이 이중 관리된다.
- 완전 동시(같은 밀리초) 2건이 같은 ID 를 뽑는 극단 케이스는 둘 다 `existsById` 를 통과한 뒤
  INSERT 하므로 PK 제약에서 한쪽이 실패 → `errors.startFailed` "다시 시도해 주세요".
  **남의 페이지로 들어가는 실패 모드는 없다.** (Hibernate merge→INSERT 의미론에 근거한
  추론이며 동시성 테스트로 측정하지는 않았다.)
- 기존에 만들어진 짧은 형식 ID(`happy-cat-42`)는 그대로 유효 — 마이그레이션 없음.
- 사용자가 직접 입력하는 경로의 동작은 불변 (`getOrCreateOwner` 재진입).
  `POST /api/owners` 와 `ScheduleService` 의 owner 생성 경로도 그대로다.
- 14,400,000 은 열거가 불가능한 크기는 아니다. owner 페이지는 원래 링크 공유 전제의 공개
  URL 이므로 (ADR 0005) 이번 결정의 범위 밖이다.
- 회귀 가드: `OwnerIdGeneratorTest`(제약·공간·결정성), `OwnerServiceTest`(재추첨·strict-create),
  `OwnerIdApiControllerTest`(무인증 200·인터셉터 회피), `HomeControllerTest`(generated 분기와
  직접 입력 재진입 유지).
