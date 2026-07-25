# 입력 검증 UX 가이드 (Inline vs Toast)

폼·입력 단계의 피드백은 **인라인**, 비동기 결과의 알림은 **토스트**로 통일한다.
같은 화면에서 두 채널이 섞이면 사용자가 "지금 보고 있는 인풋과 화면 우상단 알림이
같은 내용을 다른 톤으로 말하는" 인지 부조화를 겪는다.

## TL;DR

| 상황 | 채널 | 위치 | 톤 |
|---|---|---|---|
| 길이·허용문자·예약어 등 사용자가 즉시 고칠 수 있는 입력 단계 검증 | **Inline** | 인풋 바로 아래 헬퍼 영역 | 빨간 텍스트 + `aria-invalid`, 제출 버튼 disabled |
| 저장 성공/실패, 네트워크 오류, 서버 충돌 등 비동기 결과 알림 | **Toast** | 화면 우상단 | 성공/오류 색상, 자동 dismiss |

## Inline (인풋 헬퍼 영역)

**언제 쓰는가** — 사용자가 입력하는 그 자리에서 즉시 고칠 수 있는 검증.

- 형식 검증: 길이, 허용 문자, 정규식, 패턴
- 정적 사전 검증: 예약어 차단 등 클라이언트가 사전에 알 수 있는 규칙
- 클라이언트가 캐시한 중복 검사 결과 (예: 미리 가져온 사용 중인 ID 목록)

**규칙**
1. 인풋 직하의 단일 헬퍼 슬롯 하나에서만 메시지를 갱신한다 (다중 슬롯 금지).
2. 에러 상태에서는 다음을 동시에 적용한다:
   - 헬퍼 텍스트를 에러 메시지로 교체 + 빨간색 톤 (`.input-helper--error`)
   - 인풋 테두리 빨간색 (`.input-group-minimal--error`)
   - `aria-invalid="true"`
   - 제출 버튼 `disabled` + `aria-disabled="true"`
3. 에러 메시지는 **무엇이 잘못됐는가 + 어떻게 고치는가**를 한 문장으로.
   ❌ "Invalid input"  ✅ "예약된 ID입니다. 다른 ID를 입력해 주세요."
4. 입력이 비어 있을 때는 에러가 아닌 **기본 힌트** (`defaultMsg`) 를 표시.
5. 정상 상태에서도 헬퍼 슬롯의 높이는 유지 (CLS 방지). 본 프로젝트는
   `.input-helper { min-height: 1.2em }` 로 처리.
6. KO/EN 메시지는 `messages.properties` / `messages_en.properties` 에 함께 등록한다.

**구현 패턴 (DateDate 홈 ID 인풋)**
- 메시지: `index.id.tooShort` / `index.id.tooLong` / `index.id.invalid` / `index.id.reserved`
- 예약어 사전: 서버 `ReservedOwnerIds.RESERVED` Set → `HomeController` 가
  `reservedOwnerIds` 모델 어트리뷰트로 노출 → 템플릿에서
  `const RESERVED_IDS = new Set(/*[[${reservedOwnerIds}]]*/ []);` 로 동기화.
- 단일 진실 공급원: 사전을 추가/수정할 때는 `ReservedOwnerIds` 만 고치면 된다
  (서버 검증·클라이언트 인라인·`ReservedOwnerIdMigrationCheck` 모두 같은 Set 참조).

## Toast (화면 우상단 알림)

**언제 쓰는가** — 사용자가 액션을 트리거한 뒤 비동기적으로 확정되는 결과.

- 폼 제출 성공/실패 (서버 응답 도착 후)
- 서버 충돌: 클라이언트 사전에 없는 "이미 사용 중 (taken)" 같은 race condition
- 네트워크 오류, 5xx, 시간 초과
- 저장됨/복사됨/삭제됨 등의 작업 완료 토스트

**규칙**
1. 입력 단계에서 즉시 고칠 수 있는 검증을 토스트로 띄우지 않는다.
   (사용자는 인풋을 보고 있는데 시선이 우상단으로 튄다)
2. 토스트 메시지에는 무엇이 일어났는가만. 인라인과 메시지가 겹치지 않게 한다.
3. 서버에서 내려온 충돌은 인라인을 같이 갱신해도 좋지만, **첫 채널은 토스트**.
   (인라인은 입력 검증의 자리, 토스트는 서버 응답의 자리)

**구현 패턴**
- `static/js/toast.js` 의 전역 `toast.error(msg)` / `toast.success(msg)` 사용.
- 서버 측 `BusinessException` → `RedirectAttributes.addFlashAttribute("errorMessage", ...)`
  → `index.html` 하단의 토스트 트리거 스크립트가 자동으로 `toast.error()` 호출.

## 안티패턴 (이 PR 이전의 상태)

DateDate 홈의 ID 인풋은 길이/허용문자는 인라인, 예약어 차단만 우상단 토스트로
처리되어 같은 종류의 검증이 두 채널로 갈라져 있었다. 사용자는 "guide" 를 입력해도
인풋이 valid 처럼 보였고, "Get Started" 를 눌러야만 우상단 토스트로 차단을 인지할
수 있었다. 이 가이드는 그런 톤·위치 불일치를 막기 위한 단일 규칙이다.

## 결정 흐름 (이 검증을 어디로 보낼까?)

```
사용자가 입력하는 동안 즉시 판단 가능한가?
  ├─ Yes → 클라이언트 사전·정규식으로 인라인
  └─ No  → 서버 응답을 기다려야 함
            ├─ 결과가 입력 한 칸에 종속된 검증 (e.g. 중복 검사 API) → 인라인 (디바운스)
            └─ 결과가 액션의 성패 (제출/저장/삭제 등) → 토스트
```
