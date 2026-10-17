# 참여자 한도 8→10명 + 참여자 색 10색 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 일정당 참여자 한도를 10명으로 올리고, 9·10번째 참여자도 고유 색을 받게 하며, 숫자 8을 복제하던 화면·문구·미니앱이 서버 한도 하나를 보게 한다.

**Architecture:** 한도 값의 원천은 `Schedule.MAX_PARTICIPANTS`(애그리거트, ADR datedate/domain/0001) 하나로 두고, `ScheduleDetailResponse.maxParticipants` 로 웹 템플릿과 일정 상세 API(미니앱)에 내려준다. 색 팔레트는 끝에 2색을 덧붙여 10색으로 만든다(인덱스 0~7 불변). 한도 초과 409 메시지는 `BusinessException` 의 messageKey 패턴으로 현지화한다.

**Tech Stack:** Spring Boot 4 / Java 21, Thymeleaf, JUnit 5 + AssertJ + MockMvc, vanilla ES modules, CSS. 미니앱: Vite + React + TypeScript + vitest.

**상태:** Task 0~8 구현 완료 (2026-10-10, `docs/guides/git-commit.md` Commit 197 — 미니앱 Task 8 은 datedate-app 작업 트리에만, 커밋 안 함). 최종 검토의 경미 4건도 같은 날 반영(Commit 197 섹션 "최종 검토" 줄).

**Spec:** 이 문서 §0 — 2026-10-07 세션의 영향 분석(코드 대조, 헤드리스 Chrome 실측, 색 계산)과 사용자 결정("10색으로 확장, 권장사항대로").

## 0. 결정 사항과 근거

| # | 결정 | 근거 |
|---|---|---|
| D1 | `Schedule.MAX_PARTICIPANTS = 10` | 사용자 결정 |
| D2 | 팔레트 끝에 `#92400E`(amber-800, index 8)·`#0E7490`(cyan-700, index 9) 추가 → 10색 | 아래 색 계산 |
| D3 | `ScheduleDetailResponse.maxParticipants` 로 한도를 내려주고, 템플릿의 추가 버튼·카운터가 이 값을 쓴다 | 같은 DTO 가 웹 뷰(`ScheduleController.java:78`)와 상세 API(`ScheduleApiController.java:63`, 미니앱이 소비)에 쓰인다 |
| D4 | `ParticipantLimitExceededException` 에 `errors.participantLimitExceeded` 키 + 한도 인자, `api.js` 의 죽은 매핑 삭제 | `api.js` 의 `MAX_PARTICIPANTS_EXCEEDED` 는 서버 코드 `PARTICIPANT_LIMIT_EXCEEDED` 와 달라 한 번도 쓰이지 않았고, 지금은 한국어 사용자도 영문 메시지를 본다 |
| D5 | 한도를 숫자로 말하는 문구 ko 16키 / en 15키 갱신 + 가드 테스트 | 분석에서 찾은 목록 |
| D6 | `@media (max-width: 320px)` 블록의 참여자 점만 5px·간격 1px | 실측: 10명 가능한 날의 점이 320px 에서만 3줄·셀 밖 13.4px (332~412px 는 8명 때와 같은 2줄) |
| D7 | 미니앱 `canAdd` 를 `maxParticipants` 기반으로 (서버 배포 후, 별도 레포) | `datedate-app/DateDate/src/features/availability/AvailabilityEditScreen.tsx:152` 의 `participants.length < 8` |
| D8 | ADR 2건: datedate/domain/0010(한도 + 한도 값 전달), datedate/ux/0004(10색 — ux/0001 의 8색 spec 대체) | CLAUDE.md "결정 변경" 규칙 + ADR README "결정 1개 = ADR 1개" |

**색 계산 (D2)** — 흰 배경 대비(WCAG), 기존 8색과의 최소 CIEDE2000 색차 (일반 / 녹색맹 deutan / 적색맹 protan, Machado 2009 severity 1.0, 선형 RGB).

| 후보 | 흰 배경 대비 | 기존 8색과 최소 ΔE00 |
|---|---|---|
| **amber-800 `#92400E`** | 7.09:1 | 23.7 / 13.9 / 14.1 |
| **cyan-700 `#0E7490`** | 5.36:1 | 18.0 / 11.9 / 14.2 |
| pink-600 `#DB2777` | 4.60:1 | 12.3 / 12.7 / 17.0 |
| fuchsia-600 `#C026D3` | 4.71:1 | 12.2 / 7.4 / 2.7 |

두 색을 같이 넣었을 때 신규 색이 관여하는 최소 ΔE00 = 18.0 / 11.9 / 14.1. 기존 8색 안의 최근접 쌍은 indigo~violet 6.5(deutan 에서는 blue~violet 0.5)라 신규 2색이 기존 쌍들보다 잘 구분된다. 하네스 스크린샷(범례·셀 점)으로 육안 확인함.

**모바일 실측 (D6)** — 헤드리스 Chrome, 실제 `style.css` + `schedule/main.js`, 10명 가능한 날의 점:

| 폭 | 지금 | 320px 블록만 5px·간격 1px |
|---|---|---|
| 300·320px | 3줄, 셀 밖 13.4px (8명은 2줄·5.4px) | 2줄, 2.4px |
| 332~375px | 2줄, 3.4px | 변화 없음 |
| 390px | 2줄, 1.4px | 변화 없음 |

**범위 밖 (손대지 않음 — 결과 보고에만):**
- 동시 추가 시 한도 초과 가능 — 락 없는 메모리 검사, 8명 때와 같은 기존 동작
- 모바일 두 자리 배지("10")가 날짜 숫자를 더 가림(겹침 면적 +45~57%), 툴팁 `nowrap` 폭(10명×10자 1,108px)
- `api.js` 의 다른 오류 문구가 한국어 고정
- use-case 팀 회의 FAQ q1·a1 의 "시간 슬롯 투표 미지원"(ADR datedate/domain/0009 이후 낡음), club-activity FAQ a3 의 "교체" 안내(참여자 삭제 UI 없음)
- 일반 조언 문구 속 8("6~8명 모임: 후보 6~10개", "5~8인 중규모", "8명이면 복식 코트", "4-8명 회의" 등)
- `SeoService.java:949` `buildFaqMainEntity(8, "faq")`, `SeoServiceI18nTest.java:506` 의 8 — FAQ 문항 수(q1~q8)

## Global Constraints

- **커밋 금지.** `git commit`·`git push` 를 실행하지 않는다. Task 7 에서 `docs/guides/git-commit.md` 에 섹션을 덧붙이는 것으로 대신한다. 그래서 각 Task 끝에 "Commit" 단계가 없다.
- **개행 보존.** CRLF 파일: `Schedule.java`, `ParticipantLimitExceededException.java`, `ScheduleTest.java`, `static/js/api.js`. LF 파일: `ParticipantColor.java`, `ScheduleDetailResponse.java`, `view.html`·`faq.html`·`guide.html`·`index.html`, `ParticipantColorTest.java`, `ParticipantTest.java`, `GlobalExceptionHandlerI18nTest.java`, 모든 `*.md`, 신규 파일. 파일을 고칠 때마다 `git diff --numstat` 과 `git diff --ignore-cr-at-eol --numstat` 이 같아야 한다.
- **`messages*.properties` 는 Appendix A 스크립트로만 편집한다.** 대상 줄은 모두 LF 이고, 파일 끝쪽에 다른 작업이 남긴 CRLF 436줄(내용 변경 0)이 있다. 스크립트는 줄 단위 바이트 치환이라 다른 줄을 건드리지 않고, ko 의 `\uXXXX` 이스케이프 줄은 이스케이프로·raw UTF-8 줄은 raw 로 유지한다.
- **`style.css` 는 Task 5 의 바이트 스크립트로만 편집한다** (한 파일 안에 CRLF/LF 혼재).
- **MessageFormat.** 인자가 있는 메시지(`schedule.participant.count`, `errors.participantLimitExceeded`)만 MessageFormat 을 탄다. 새 문구에 작은따옴표를 넣지 않는다.
- **Gradle 은 cmd.exe 경유 + 정확한 FQCN.** WSL bash, 저장소 루트에서 `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests <FQCN> [--tests <FQCN> ...]"`. 와일드카드 금지. 좁혀 돌렸으면 `build/test-results/test/TEST-<FQCN>.xml` 의 `tests=` 로 실행 수를 확인한다.
- **앱 기동 금지(사용자 확인 없이).** `bootRun` 은 `.env` 때문에 LIVE 매매 봇을 켠다. 화면은 MockMvc 렌더링 테스트와 Appendix B 하네스로 검증한다.
- **"8" 일괄 치환 금지.** §0 범위 밖 목록의 8 은 한도와 무관하다.
- **팔레트는 덧붙이기만.** index 0~7 의 hex 와 순서를 바꾸지 않는다.
- **값:** `MAX_PARTICIPANTS = 10`, index 8 = `#92400E`, index 9 = `#0E7490`, 메시지 키 `errors.participantLimitExceeded`, DTO 필드 `maxParticipants`.

## Review Focus

1. 배포 전부터 8명이던 일정에 9·10번째를 추가하면 `#92400E`·`#0E7490` 를 받고 기존 8색과 겹치지 않아야 한다 — Task 1 `existingEightParticipantSchedule_ninthAndTenthGetNewColors`.
2. 경계값: 9명(한도−1)이면 추가 버튼이 활성, 10명이면 비활성이고 카운터는 "10 / 10명" — Task 2 `belowCap_addButtonEnabled`, `atCap_addButtonDisabled`.
3. 영어 로케일 일정 페이지: 카운터 "10 / 10", 메시지 키 누락(`??…??`) 없음 — Task 2 `english_counterUsesCap`.
4. 페이지를 연 뒤 다른 사람이 10번째를 채운 상태에서 추가를 누르면 409 + 현재 로케일 문장에 "10" (영문 원문도, "8" 도 아님) — Task 3 `participantLimit_*` 2건 + `api.js` 죽은 매핑 제거 확인.
5. 서버 배포 전(응답에 `maxParticipants` 없음)의 미니앱은 8명 기준을 유지하고, 배포 후엔 10명 — Task 8 `canAddParticipant` 테스트.

---

## 변경 파일 지도

`.../` = `me/singingsandhill/calendar`

| 파일 | 변경 | Task |
|---|---|---|
| `src/main/java/.../datedate/domain/schedule/Schedule.java` | `MAX_PARTICIPANTS` 8→10 | 1 |
| `src/main/java/.../datedate/domain/participant/ParticipantColor.java` | 팔레트 2색 덧붙임 | 1 |
| `src/test/java/.../datedate/domain/schedule/ScheduleTest.java` | 한도 고정·색 고유·9·10번째 색 테스트 | 1 |
| `src/test/java/.../datedate/domain/participant/ParticipantColorTest.java` | 10색 기준으로 갱신 | 1 |
| `src/test/java/.../datedate/domain/participant/ParticipantTest.java` | 순환 테스트를 10 기준으로 | 1 |
| `src/main/java/.../datedate/presentation/dto/response/ScheduleDetailResponse.java` | `maxParticipants` 필드 | 2 |
| `src/main/resources/templates/schedule/view.html` | 버튼·카운터가 `maxParticipants` 사용 | 2 |
| `src/main/resources/messages.properties`, `messages_en.properties` | 카운터 `{1}`(2), 신규 오류 키(3), 문구(4) | 2·3·4 |
| `src/test/java/.../datedate/presentation/controller/ScheduleParticipantCapRenderingTest.java` | 신규 | 2 |
| `src/main/java/.../datedate/application/exception/ParticipantLimitExceededException.java` | messageKey + 인자 | 3 |
| `src/main/resources/static/js/api.js` | 죽은 매핑 1줄 삭제 | 3 |
| `src/test/java/.../common/presentation/api/GlobalExceptionHandlerI18nTest.java` | 한도 메시지 ko/en 테스트 | 3 |
| `src/test/java/.../datedate/domain/schedule/ParticipantCapCopyTest.java` | 신규 문구 가드 | 4 |
| `src/main/resources/templates/faq.html`, `guide.html`, `index.html` | 정적 대체 텍스트(렌더 시 교체됨) 8→10 | 4 |
| `src/main/resources/static/css/style.css` | 320px 블록 점 크기 | 5 |
| `docs/adr/datedate/domain/0010-participant-cap-10.md` | 신규 ADR | 6 |
| `docs/adr/datedate/ux/0004-ten-color-participant-palette.md` | 신규 ADR | 6 |
| `docs/adr/datedate/ux/0001-participant-color-cell-sync.md` | 상태 줄(부분 대체) | 6 |
| `docs/adr/README.md` | 매트릭스·시간순·폴더 수 | 6 |
| `src/main/java/.../datedate/application/CLAUDE.md`, `docs/architecture.md`, `docs/datedate/erd.html` | 8→10 사실 갱신 | 6 |
| `docs/guides/git-commit.md` | 커밋 섹션 | 7 |
| `/mnt/d/projects/datedate-app/DateDate/src/api/types.ts`, `src/features/availability/selection.ts`, `selection.test.ts`, `AvailabilityEditScreen.tsx` | 미니앱 (서버 배포 후) | 8 |

---

### Task 0: 사전 점검과 도구 준비

**Files:** 없음 (도구는 저장소 밖에 둔다)

- [ ] **Step 0-1: 작업 트리 기준선 기록**

```bash
cd /mnt/d/projects/calendar
git status --short
git diff --numstat -- src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css
git diff --ignore-cr-at-eol --numstat -- src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css
```

2026-10-07 기준 기대값: messages 두 파일 `436 436`(개행만 다른 줄 — `--ignore-cr-at-eol` 쪽은 출력 없음), style.css `36 598`(미실행 Commit 196 의 date-diff 변경, 두 방식 같음). 다르면 실제 숫자를 적어 두고 이후 비교 기준으로 쓴다. messages 의 개행 노이즈는 사용자의 Windows git(`core.autocrlf=input`)이 add 할 때 LF 로 되돌리므로 손대지 않는다.

- [ ] **Step 0-2: 커밋 로그 소유권 확인**

```bash
grep -nE "^# Commit (19[0-9]|2[0-9]{2})" docs/guides/git-commit.md | cut -c1-120
```

Commit 196(`style.css` 소유)에 ✅ 표시가 있는지 본다. 없으면 Task 5 의 style.css 변경은 196 에 흡수되므로 Task 7 에서 주의 주석을 단다.

- [ ] **Step 0-3: 메시지 편집 스크립트 저장**

Appendix A 의 `props_edit.py` 와 spec JSON 6개를 저장소 밖 작업 디렉터리(세션 스크래치패드, 없으면 `/tmp/cap10`)에 저장하고 그 경로를 `T` 로 쓴다.

```bash
T=/tmp/cap10   # 실제로 저장한 디렉터리
python3 -I -c "import ast,sys; ast.parse(open(sys.argv[1]).read()); print('props_edit.py ok')" "$T/props_edit.py"
python3 -I -c "import json,sys; [json.load(open(p, encoding='utf-8')) for p in sys.argv[1:]]; print('specs ok')" "$T"/task*.json
```

- [ ] **Step 0-4: 기준 테스트가 GREEN 인지 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.domain.schedule.ScheduleTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantColorTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantTest --tests me.singingsandhill.calendar.datedate.application.service.ParticipantServiceTest --tests me.singingsandhill.calendar.common.presentation.api.GlobalExceptionHandlerI18nTest --tests me.singingsandhill.calendar.datedate.presentation.controller.ScheduleTimeSlotRenderingTest --tests me.singingsandhill.calendar.common.i18n.MessageCatalogParityTest"
```

Expected: `BUILD SUCCESSFUL`. 실패하면 이 계획과 무관한 기존 실패이므로 원인을 먼저 보고한다.

---

### Task 1: 도메인 — 한도 10명 + 팔레트 10색

한도와 팔레트는 "한도까지 색이 모두 달라야 한다"는 불변식으로 묶여 있어 한 Task 로 둔다. 한도만 올리면 색 고유 테스트가 RED 가 되는 것을 Step 1-5 에서 직접 확인한다.

**Files:**
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/Schedule.java:14` (CRLF)
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColor.java:7-16`
- Test: `src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleTest.java` (CRLF)
- Test: `src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColorTest.java:60-73`
- Test: `src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantTest.java:34-41`

**Interfaces:**
- Produces: `Schedule.MAX_PARTICIPANTS` = 10 (`public static final int`), `ParticipantColor.ofIndex(8).hexCode()` = `"#92400E"`, `ofIndex(9).hexCode()` = `"#0E7490"`, `ParticipantColor.getPresetColorCount()` = 10.

- [ ] **Step 1-1: 실패 테스트 — `ScheduleTest` 에 3건 추가**

import 블록을 다음처럼 만든다 (`java.time.LocalDateTime`, `java.util.ArrayList`, `ParticipantColor` 추가):

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateParticipantException;
import me.singingsandhill.calendar.datedate.application.exception.ParticipantLimitExceededException;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
```

`addParticipant_exceedsLimit_throws` 메서드 바로 뒤에 넣는다:

```java
    @Test
    @DisplayName("참여자 한도는 10명이다 (ADR datedate/domain/0010)")
    void maxParticipants_isTen() {
        assertThat(Schedule.MAX_PARTICIPANTS).isEqualTo(10);
    }

    @Test
    @DisplayName("한도까지 채운 참여자는 모두 다른 색이다 — 한도를 올리면 팔레트도 늘려야 한다")
    void participantsUpToLimit_haveDistinctColors() {
        Schedule schedule = new Schedule("test-user", 2025, 12);
        for (int i = 0; i < Schedule.MAX_PARTICIPANTS; i++) {
            schedule.addParticipant(new Participant(1L, "user" + i, schedule.nextColorIndex()));
        }

        assertThat(schedule.getParticipants())
                .extracting(Participant::getColorHex)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("배포 전부터 8명이던 일정의 9·10번째 참여자는 새 2색을 받는다")
    void existingEightParticipantSchedule_ninthAndTenthGetNewColors() {
        List<Participant> eight = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            eight.add(new Participant((long) i + 1, 1L, "user" + i, ParticipantColor.ofIndex(i),
                    List.of(), LocalDateTime.now()));
        }
        Schedule schedule = new Schedule(1L, "test-user", 2025, 12, 7, LocalDateTime.now(), eight);

        Participant ninth = new Participant(1L, "ninth", schedule.nextColorIndex());
        schedule.addParticipant(ninth);
        Participant tenth = new Participant(1L, "tenth", schedule.nextColorIndex());
        schedule.addParticipant(tenth);

        assertThat(ninth.getColorHex()).isEqualTo("#92400E");
        assertThat(tenth.getColorHex()).isEqualTo("#0E7490");
    }
```

- [ ] **Step 1-2: 실패 테스트 — `ParticipantColorTest` 갱신**

import 에 `java.util.List`, `java.util.stream.IntStream` 을 추가한다:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
```

60~73행(`ofIndex_wrapsAround`, `getPresetColorCount_returns8`)을 다음으로 바꾼다:

```java
    @Test
    @DisplayName("ofIndex should wrap around for index >= 10")
    void ofIndex_wrapsAround() {
        ParticipantColor color0 = ParticipantColor.ofIndex(0);
        ParticipantColor color10 = ParticipantColor.ofIndex(10);

        assertThat(color0.hexCode()).isEqualTo(color10.hexCode());
    }

    @Test
    @DisplayName("getPresetColorCount should return 10")
    void getPresetColorCount_returns10() {
        assertThat(ParticipantColor.getPresetColorCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("9·10번째 색은 덧붙인 갈색·청록이다 — 기존 0~7 은 그대로 (ADR datedate/ux/0004)")
    void ofIndex_appendedColors() {
        assertThat(ParticipantColor.ofIndex(8).hexCode()).isEqualTo("#92400E");
        assertThat(ParticipantColor.ofIndex(9).hexCode()).isEqualTo("#0E7490");
    }

    @Test
    @DisplayName("프리셋 색은 모두 서로 다르다")
    void presetColors_areDistinct() {
        List<String> hexes = IntStream.range(0, ParticipantColor.getPresetColorCount())
                .mapToObj(i -> ParticipantColor.ofIndex(i).hexCode())
                .toList();

        assertThat(hexes).doesNotHaveDuplicates();
    }
```

- [ ] **Step 1-3: 실패 테스트 — `ParticipantTest` 순환 테스트**

34~41행을 다음으로 바꾼다:

```java
    @Test
    @DisplayName("Color index should wrap around after 10")
    void colorIndex_wrapsAround() {
        Participant p0 = new Participant(1L, "User0", 0);
        Participant p8 = new Participant(1L, "User8", 8);
        Participant p10 = new Participant(1L, "User10", 10);

        assertThat(p8.getColorHex()).isNotEqualTo(p0.getColorHex());
        assertThat(p10.getColorHex()).isEqualTo(p0.getColorHex());
    }
```

- [ ] **Step 1-4: RED 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.domain.schedule.ScheduleTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantColorTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantTest"
```

Expected: FAIL — `maxParticipants_isTen`(8≠10), `existingEightParticipantSchedule_ninthAndTenthGetNewColors`(9번째에서 `ParticipantLimitExceededException`), `ofIndex_wrapsAround`·`getPresetColorCount_returns10`·`ofIndex_appendedColors`, `ParticipantTest.colorIndex_wrapsAround`(p8 == p0). `participantsUpToLimit_haveDistinctColors` 는 지금(한도 8, 8색) 통과하는 게 정상이다.

- [ ] **Step 1-5: 한도만 10 으로 — 색 고유 가드가 잡는지 확인 (RED 2)**

`Schedule.java:14` 한 줄만 바꾼다 (CRLF 파일, 한 줄 Edit):

```java
    public static final int MAX_PARTICIPANTS = 10;
```

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.domain.schedule.ScheduleTest"
```

Expected: `participantsUpToLimit_haveDistinctColors` FAIL(9·10번째가 1·2번째와 같은 색) — 팔레트 없이 한도만 올리면 빌드가 막힌다는 확인. `existingEight…` 도 FAIL(`#E11D48`). `maxParticipants_isTen` 은 PASS.

- [ ] **Step 1-6: 팔레트 덧붙이기**

`ParticipantColor.java` 7~16행:

```java
    private static final List<String> PRESET_COLORS = List.of(
            "#E11D48", // Red    — rose-600
            "#F97316", // Orange — orange-500
            "#CA8A04", // Yellow — amber-700 (AA against white as visual indicator)
            "#16A34A", // Green  — green-600
            "#0D9488", // Teal   — teal-600
            "#2563EB", // Blue   — blue-600
            "#4F46E5", // Indigo — indigo-600
            "#7C3AED", // Violet — violet-600
            "#92400E", // Brown  — amber-800 (9th, ADR datedate/ux/0004)
            "#0E7490"  // Cyan   — cyan-700 (10th, ADR datedate/ux/0004)
    );
```

- [ ] **Step 1-7: GREEN + 실행 수 + 개행 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.domain.schedule.ScheduleTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantColorTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantTest --tests me.singingsandhill.calendar.datedate.application.service.ParticipantServiceTest"
for c in datedate.domain.schedule.ScheduleTest datedate.domain.participant.ParticipantColorTest datedate.domain.participant.ParticipantTest datedate.application.service.ParticipantServiceTest; do
  grep -o '<testsuite [^>]*' build/test-results/test/TEST-me.singingsandhill.calendar.$c.xml | grep -oE '(tests|failures|errors)="[0-9]+"' | tr '\n' ' '; echo " $c"
done
F="src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/Schedule.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColor.java src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColorTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantTest.java"
git diff --numstat -- $F; echo ---; git diff --ignore-cr-at-eol --numstat -- $F
```

Expected: `BUILD SUCCESSFUL`. 실행 수 ScheduleTest 17 · ParticipantColorTest 10 · ParticipantTest 10 · ParticipantServiceTest(변경 없음) — 모두 failures/errors 0. 두 numstat 이 줄마다 같다. `Schedule.java` 는 `1 1`.

`ScheduleTest.java` 의 numstat 이 파일 전체(약 170줄)로 잡히면 Edit 가 LF 를 섞은 것이다. 이 파일은 원래 전 줄 CRLF 이므로 다음으로 복원한다:

```bash
python3 -I -c "import sys; p=sys.argv[1]; d=open(p,'rb').read(); open(p,'wb').write(d.replace(b'\r\n',b'\n').replace(b'\n',b'\r\n'))" src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleTest.java
```

---

### Task 2: 한도 전달 — DTO `maxParticipants` + 추가 버튼·카운터

**Files:**
- Create: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleParticipantCapRenderingTest.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/ScheduleDetailResponse.java:21-22, 43-44`
- Modify: `src/main/resources/templates/schedule/view.html:199-206`
- Modify: `src/main/resources/messages.properties`, `messages_en.properties` — `schedule.participant.count` (Appendix A `task2-*.json`)

**Interfaces:**
- Consumes: `Schedule.MAX_PARTICIPANTS` (Task 1)
- Produces: `ScheduleDetailResponse.maxParticipants()` (`int`, JSON `"maxParticipants"`) — Task 8 의 미니앱이 읽는다. 메시지 `schedule.participant.count` = `{0} / {1}명` / `{0} / {1}` (인자 2개: 현재 인원, 한도).

- [ ] **Step 2-1: 실패 테스트 작성**

`ScheduleParticipantCapRenderingTest.java` (신규, LF):

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

/**
 * 참여자 추가 모달의 한도 표시 — 버튼 비활성·카운터가 서버 한도(ScheduleDetailResponse.maxParticipants)를
 * 따르는지. 템플릿에 숫자를 적어 두면 한도를 바꿀 때 서버는 받는데 웹 버튼만 옛 한도에서 잠긴다
 * (ADR datedate/domain/0010).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleParticipantCapRenderingTest {

    private static final String OWNER_ID = "cap-view-owner";
    private static final int MAX = Schedule.MAX_PARTICIPANTS;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private EntityManager entityManager;

    private void givenParticipants(int count) {
        Schedule schedule = scheduleService.createSchedule(OWNER_ID, 2025, 12, null);
        for (int i = 0; i < count; i++) {
            participantService.addParticipant(schedule.getId(), "p" + i);
            // 운영은 요청마다 새 영속성 컨텍스트 — 1차 캐시의 참여자 컬렉션이 stale 하지 않도록 재현
            entityManager.flush();
            entityManager.clear();
        }
    }

    private String render(String lang) throws Exception {
        return mockMvc.perform(get("/" + OWNER_ID + "/2025/12").param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String addForm(String html) {
        int start = html.indexOf("id=\"addParticipantForm\"");
        return html.substring(start, html.indexOf("</form>", start));
    }

    private static String counter(String html) {
        int start = html.indexOf("class=\"participant-count-info\"");
        int open = html.indexOf('>', start) + 1;
        return html.substring(open, html.indexOf("</p>", open)).strip();
    }

    @Test
    @DisplayName("한도 직전(MAX-1명)에는 추가 버튼이 활성이고 카운터가 서버 한도를 보여준다")
    void belowCap_addButtonEnabled() throws Exception {
        givenParticipants(MAX - 1);

        String html = render("ko");

        assertThat(addForm(html)).doesNotContain("disabled");
        assertThat(counter(html)).isEqualTo((MAX - 1) + " / " + MAX + "명");
    }

    @Test
    @DisplayName("한도(MAX명)에 도달하면 추가 버튼이 비활성이다")
    void atCap_addButtonDisabled() throws Exception {
        givenParticipants(MAX);

        String html = render("ko");

        assertThat(addForm(html)).contains("disabled");
        assertThat(counter(html)).isEqualTo(MAX + " / " + MAX + "명");
    }

    @Test
    @DisplayName("영문 로케일 카운터도 서버 한도를 쓰고 메시지 키 누락이 없다")
    void english_counterUsesCap() throws Exception {
        givenParticipants(MAX);

        String html = render("en");

        assertThat(counter(html)).isEqualTo(MAX + " / " + MAX);
        assertThat(html).doesNotContain("??schedule.participant");
    }

    @Test
    @DisplayName("일정 상세 API 가 maxParticipants 를 내려준다 — 미니앱이 한도를 하드코딩하지 않게")
    void scheduleApi_exposesMaxParticipants() throws Exception {
        givenParticipants(1);

        mockMvc.perform(get("/api/owners/" + OWNER_ID + "/schedules/2025/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxParticipants").value(MAX));
    }
}
```

- [ ] **Step 2-2: RED 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.presentation.controller.ScheduleParticipantCapRenderingTest"
```

Expected: 4건 모두 FAIL — `belowCap`(9명인데 `>= 8` 로 `disabled`), `atCap`·`english`(카운터 "10 / 8명"·"10 / 8"), `scheduleApi`(`No value at JSON path "$.maxParticipants"`).

- [ ] **Step 2-3: DTO 에 `maxParticipants` 추가**

`ScheduleDetailResponse.java` — record 구성요소(21~22행 사이)와 `from()`(43~44행 사이)에 한 줄씩:

```java
        boolean isExtendedMode,
        int maxParticipants,
        List<ParticipantResponse> participants,
```

```java
                schedule.isExtendedMode(),
                Schedule.MAX_PARTICIPANTS,
                schedule.getParticipants().stream()
```

(`Schedule` 은 이미 import 돼 있다. `new ScheduleDetailResponse(` 를 직접 부르는 곳은 `from()` 하나뿐이다.)

- [ ] **Step 2-4: 템플릿이 한도를 DTO 에서 읽게**

`view.html` 199~206행:

```html
                <p class="participant-count-info" th:text="#{schedule.participant.count(${schedule.participants().size()}, ${schedule.maxParticipants()})}">
                    0 / 10명
                </p>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" data-action="participant.closeModal" th:text="#{schedule.participant.cancel}">취소</button>
                    <button type="submit" class="btn btn-primary"
                            th:disabled="${schedule.participants().size() >= schedule.maxParticipants()}"
                            th:text="#{schedule.participant.add.btn}">추가</button>
```

- [ ] **Step 2-5: 카운터 메시지에 한도 인자**

```bash
python3 -I "$T/props_edit.py" "$T/task2-ko.json" "$T/task2-en.json"
```

Expected 출력: 두 파일 각각 `set 1, replace 0, insert 0`. 결과 값: ko `{0} / {1}명`(이스케이프 줄 `{0} / {1}\uBA85`), en `{0} / {1}`.

- [ ] **Step 2-6: GREEN + 실행 수 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.presentation.controller.ScheduleParticipantCapRenderingTest --tests me.singingsandhill.calendar.datedate.presentation.controller.ScheduleTimeSlotRenderingTest --tests me.singingsandhill.calendar.datedate.presentation.api.ScheduleApiControllerTest --tests me.singingsandhill.calendar.common.i18n.MessageCatalogParityTest"
grep -o '<testsuite [^>]*' build/test-results/test/TEST-me.singingsandhill.calendar.datedate.presentation.controller.ScheduleParticipantCapRenderingTest.xml | grep -oE '(tests|failures|errors)="[0-9]+"'
```

Expected: `BUILD SUCCESSFUL`, `tests="4" failures="0" errors="0"`.

- [ ] **Step 2-7: 개행·범위 확인**

```bash
git diff --numstat -- src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/ScheduleDetailResponse.java src/main/resources/templates/schedule/view.html
git diff --ignore-cr-at-eol --numstat -- src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/ScheduleDetailResponse.java src/main/resources/templates/schedule/view.html
git diff --ignore-cr-at-eol --numstat -- src/main/resources/messages.properties src/main/resources/messages_en.properties
```

Expected: DTO `2 0`, view.html `3 3` (두 방식 같음). messages 는 `--ignore-cr-at-eol` 기준 각 `1 1`.

---

### Task 3: 한도 초과 409 메시지 현지화 + `api.js` 죽은 매핑 삭제

**Files:**
- Modify: `src/test/java/me/singingsandhill/calendar/common/presentation/api/GlobalExceptionHandlerI18nTest.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/exception/ParticipantLimitExceededException.java:8-15` (CRLF)
- Modify: `src/main/resources/messages.properties`, `messages_en.properties` — `errors.startFailed` 다음 줄에 새 키 (Appendix A `task3-*.json`)
- Modify: `src/main/resources/static/js/api.js:9` (CRLF, 1줄 삭제)

**Interfaces:**
- Consumes: `Schedule.MAX_PARTICIPANTS` (Task 1)
- Produces: `ParticipantLimitExceededException.MESSAGE_KEY` = `"errors.participantLimitExceeded"`, 인자 `{0}` = 한도. ko `참여자는 일정당 최대 {0}명까지 추가할 수 있습니다.` / en `A schedule can have up to {0} participants.`

- [ ] **Step 3-1: 실패 테스트 — `GlobalExceptionHandlerI18nTest` 에 2건**

import 추가:

```java
import org.springframework.http.HttpStatus;
```

```java
import me.singingsandhill.calendar.datedate.application.exception.ParticipantLimitExceededException;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
```

클래스 끝에 추가:

```java
    @Test
    @DisplayName("ParticipantLimitExceededException — KO 로케일에서 한도가 들어간 한국어 메시지를 반환한다")
    void participantLimit_koreanLocale_returnsKoreanMessageWithCap() {
        LocaleContextHolder.setLocale(Locale.KOREAN);

        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(new ParticipantLimitExceededException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PARTICIPANT_LIMIT_EXCEEDED");
        assertThat(response.getBody().message())
                .isEqualTo("참여자는 일정당 최대 " + Schedule.MAX_PARTICIPANTS + "명까지 추가할 수 있습니다.");
    }

    @Test
    @DisplayName("ParticipantLimitExceededException — EN 로케일에서 한도가 들어간 영어 메시지를 반환한다")
    void participantLimit_englishLocale_returnsEnglishMessageWithCap() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(new ParticipantLimitExceededException());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("A schedule can have up to " + Schedule.MAX_PARTICIPANTS + " participants.");
    }
```

- [ ] **Step 3-2: RED 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.common.presentation.api.GlobalExceptionHandlerI18nTest"
```

Expected: 새 2건 FAIL — 둘 다 실제 메시지가 영문 고정 `Maximum number of participants (10) has been reached`.

- [ ] **Step 3-3: 예외에 messageKey + 한도 인자**

`ParticipantLimitExceededException.java` 8~15행 (CRLF 파일):

```java
public class ParticipantLimitExceededException extends BusinessException {

    public static final String MESSAGE_KEY = "errors.participantLimitExceeded";

    public ParticipantLimitExceededException() {
        super("PARTICIPANT_LIMIT_EXCEEDED",
                "Maximum number of participants (" + Schedule.MAX_PARTICIPANTS + ") has been reached",
                HttpStatus.CONFLICT,
                MESSAGE_KEY,
                new Object[]{Schedule.MAX_PARTICIPANTS});
    }
}
```

(같은 패턴: `OwnerIdTakenException`, `ReservedOwnerIdException`. 영문 fallback 은 키가 없을 때만 쓰인다.)

- [ ] **Step 3-4: 메시지 키 삽입**

```bash
python3 -I "$T/props_edit.py" "$T/task3-ko.json" "$T/task3-en.json"
```

Expected 출력: 두 파일 각각 `set 0, replace 0, insert 1`. ko 새 줄은 이웃 `errors.*` 줄처럼 `\uXXXX` 이스케이프로 들어간다.

- [ ] **Step 3-5: `api.js` 죽은 매핑 삭제**

서버 메시지가 현지화되므로 `api.js` 는 코드 매핑 없이 `error.message` 로 떨어지면 된다 (`api.js:30-32`). CRLF 파일이라 바이트로 지운다:

```bash
python3 -I - <<'EOF'
p = 'src/main/resources/static/js/api.js'
d = open(p, 'rb').read()
line = "    'MAX_PARTICIPANTS_EXCEEDED': '참여자는 최대 8명까지 가능합니다',\r\n".encode('utf-8')
assert d.count(line) == 1, d.count(line)
open(p, 'wb').write(d.replace(line, b''))
print('api.js: removed 1 line')
EOF
```

- [ ] **Step 3-6: GREEN + 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.common.presentation.api.GlobalExceptionHandlerI18nTest --tests me.singingsandhill.calendar.datedate.application.service.ParticipantServiceTest --tests me.singingsandhill.calendar.datedate.domain.schedule.ScheduleTest --tests me.singingsandhill.calendar.common.i18n.MessageCatalogParityTest"
grep -o '<testsuite [^>]*' build/test-results/test/TEST-me.singingsandhill.calendar.common.presentation.api.GlobalExceptionHandlerI18nTest.xml | grep -oE '(tests|failures|errors)="[0-9]+"'
grep -rn "MAX_PARTICIPANTS_EXCEEDED" src/ ; echo "grep exit=$? (1 이면 남은 곳 없음)"
F="src/main/java/me/singingsandhill/calendar/datedate/application/exception/ParticipantLimitExceededException.java src/main/resources/static/js/api.js src/test/java/me/singingsandhill/calendar/common/presentation/api/GlobalExceptionHandlerI18nTest.java"
git diff --numstat -- $F; echo ---; git diff --ignore-cr-at-eol --numstat -- $F
```

Expected: `BUILD SUCCESSFUL`, `tests="4" failures="0" errors="0"`, grep exit=1, 두 numstat 같음 (예외 `5 1`, api.js `0 1`). 예외 파일이 전체 줄로 잡히면 Step 1-7 의 CRLF 복원 한 줄을 이 파일에 쓴다.

---

### Task 4: 한도 문구 갱신 + 문구 가드

**Files:**
- Create: `src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ParticipantCapCopyTest.java`
- Modify: `src/main/resources/messages.properties`, `messages_en.properties` (Appendix A `task4-*.json`)
- Modify: `src/main/resources/templates/faq.html:39`, `guide.html:60,143`, `index.html:169,298` (정적 대체 텍스트)

**Interfaces:**
- Consumes: `Schedule.MAX_PARTICIPANTS` (Task 1), `schedule.participant.count` 의 `{1}` (Task 2)

- [ ] **Step 4-1: 실패 테스트 — 문구 가드**

`ParticipantCapCopyTest.java` (신규, LF):

```java
package me.singingsandhill.calendar.datedate.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 한도를 숫자로 말하는 문구가 Schedule.MAX_PARTICIPANTS 와 같은지 — 한도를 바꾸면 이 키들도 같이 바꿔야 한다.
 * 8→10 변경 때 홈·가이드·FAQ·소개·use-case 문구가 "8명" 으로 남을 뻔했다 (ADR datedate/domain/0010).
 * 일반 조언 속 숫자("6~8명 모임" 등)는 한도와 무관해 목록에 없다.
 */
class ParticipantCapCopyTest {

    private static final int MAX = Schedule.MAX_PARTICIPANTS;

    private static final List<String> CAP_KEYS = List.of(
            "index.feature2.desc",
            "index.feature3.title",
            "index.why.limits.item2",
            "guide.step4.li1",
            "guide.troubleshooting.q3",
            "guide.troubleshooting.a3",
            "faq.a2",
            "about.section.audience.body",
            "seo.useCase.team-meeting.body",
            "seo.useCase.friend-meetup.section.faq.q1",
            "seo.useCase.friend-meetup.section.faq.a1",
            "seo.useCase.travel-planning.section.faq.q1",
            "seo.useCase.travel-planning.section.faq.a1",
            "seo.useCase.club-activity.section.faq.a1",
            "seo.useCase.club-activity.section.faq.a3");

    // 영문 q3 는 "Our team is 11 people" 로 한도를 말하지 않는다
    private static final String KO_ONLY_CAP_KEY = "seo.useCase.team-meeting.section.faq.q3";

    // 카탈로그는 유니코드 escape 와 raw UTF-8 이 혼재 → UTF-8 Reader 로 로드 (MessageCatalogParityTest 와 같은 방식)
    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(
                Objects.requireNonNull(ParticipantCapCopyTest.class.getResourceAsStream("/" + name),
                        name + " not found on classpath"),
                StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    @Test
    @DisplayName("ko 문구가 현재 한도(N명)를 말한다")
    void koreanCopyStatesCurrentCap() throws IOException {
        Properties ko = load("messages.properties");
        Pattern cap = Pattern.compile("(?<!\\d)" + MAX + "명");
        List<String> keys = new ArrayList<>(CAP_KEYS);
        keys.add(KO_ONLY_CAP_KEY);

        for (String key : keys) {
            assertThat(cap.matcher(ko.getProperty(key)).find()).as(key).isTrue();
        }
    }

    @Test
    @DisplayName("en 문구가 현재 한도(숫자 N)를 말한다")
    void englishCopyStatesCurrentCap() throws IOException {
        Properties en = load("messages_en.properties");
        Pattern cap = Pattern.compile("(?<!\\d)" + MAX + "(?!\\d)");

        for (String key : CAP_KEYS) {
            assertThat(cap.matcher(en.getProperty(key)).find()).as(key).isTrue();
        }
    }

    @Test
    @DisplayName("참여자 카운터는 한도를 숫자로 적지 않고 인자 {1} 로 받는다")
    void participantCounterTakesCapAsArgument() throws IOException {
        for (String file : List.of("messages.properties", "messages_en.properties")) {
            String counter = load(file).getProperty("schedule.participant.count");

            assertThat(counter).as(file).contains("{1}");
            assertThat(counter.replaceAll("\\{\\d}", "")).as(file).doesNotContainPattern("\\d");
        }
    }
}
```

- [ ] **Step 4-2: RED 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.domain.schedule.ParticipantCapCopyTest"
```

Expected: `koreanCopyStatesCurrentCap`·`englishCopyStatesCurrentCap` FAIL (첫 키 `index.feature2.desc` 에서 "8명"/"8"), `participantCounterTakesCapAsArgument` PASS(Task 2 에서 이미 `{1}`). 사전 검증(2026-10-07, 같은 목록·정규식으로 현재 카탈로그 대조): 바꾸기 전 ko 16키·en 15키 모두 불일치, Appendix A 적용 후 0건.

- [ ] **Step 4-3: 문구 갱신**

```bash
python3 -I "$T/props_edit.py" "$T/task4-ko.json" "$T/task4-en.json"
```

Expected 출력: ko `set 9, replace 8, insert 0`, en `set 8, replace 8, insert 0`. 바뀌는 문장 전체는 Appendix A 의 `task4-*.json` 에 있다 — 원칙: 숫자만 8→10, 단 사실이 틀린 문장(ko friend-meetup a1 의 "9명 이상이면 기능적으로는 작동", travel a1·en friend-meetup a1 의 "넘으면 색 구분이 어려워" 류)은 10색 기준으로 다시 쓴다. "권장" 어조와 "교체" 안내는 범위 밖이라 유지.

- [ ] **Step 4-4: 정적 대체 텍스트(렌더 시 `th:text` 로 교체되는 글자) 8→10**

화면에는 안 나오지만 다음 사람이 "8명" 을 grep 할 때 헷갈리지 않게 맞춘다 (LF, 한 줄씩 Edit):

| 파일:행 | 바꿀 글자 | 새 글자 |
|---|---|---|
| `faq.html:39` | `일정 하나당 최대 8명이 참여할` | `일정 하나당 최대 10명이 참여할` |
| `guide.html:60` | `>최대 8명까지 참여 가능<` | `>최대 10명까지 참여 가능<` |
| `guide.html:143` | `>8명보다 많은 인원은 어떻게 하나요?<` | `>10명보다 많은 인원은 어떻게 하나요?<` |
| `index.html:169` | `최대 8명이 각자 색상으로` | `최대 10명이 각자 색상으로` |
| `index.html:298` | `>한 페이지 8명까지 권장.<` | `>한 페이지 10명까지 권장.<` |

- [ ] **Step 4-5: GREEN + 확인**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.domain.schedule.ParticipantCapCopyTest --tests me.singingsandhill.calendar.common.i18n.MessageCatalogParityTest --tests me.singingsandhill.calendar.datedate.application.service.SeoServiceI18nTest --tests me.singingsandhill.calendar.datedate.presentation.controller.UseCaseLocaleRenderingTest --tests me.singingsandhill.calendar.datedate.presentation.controller.HomeControllerTest"
grep -o '<testsuite [^>]*' build/test-results/test/TEST-me.singingsandhill.calendar.datedate.domain.schedule.ParticipantCapCopyTest.xml | grep -oE '(tests|failures|errors)="[0-9]+"'
git diff --ignore-cr-at-eol --numstat -- src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/templates/faq.html src/main/resources/templates/guide.html src/main/resources/templates/index.html
git diff --numstat -- src/main/resources/templates/faq.html src/main/resources/templates/guide.html src/main/resources/templates/index.html
```

Expected: `BUILD SUCCESSFUL`, `tests="3" failures="0" errors="0"`. `--ignore-cr-at-eol` 기준 누적(Task 2·3·4): ko `19 18`, en `18 17`, faq.html `1 1`, guide.html `2 2`, index.html `2 2`. 일반 `--numstat` 의 messages 값은 Task 0 기준선(436 436)에 같은 증가분을 더한 값(ko `455 454`, en `454 453`)이어야 한다 — 다르면 다른 줄의 개행이 바뀐 것이다.

---

### Task 5: 320px 이하 — 10명 가능한 날의 점을 2줄로

이 Task 는 다른 Task 와 독립이다. 빼도 나머지는 그대로 성립한다(320px 이하 화면에서 10명 가능한 날만 점 3줄로 남는다).

**Files:**
- Modify: `src/main/resources/static/css/style.css` — `@media (max-width: 320px)` 블록의 `.participant-dots`·`.participant-dot` (2026-10-07 기준 2113~2120행, CRLF 줄)

- [ ] **Step 5-1: RED — 지금 상태 측정**

Appendix B 하네스를 띄우고 Appendix C(자동) 또는 DevTools 콘솔(수동)로 `?n=10` 을 폭 320·360px 에서 잰다.

Expected: 320px 에서 점 8개 이상인 셀(10명 가능한 날 2곳, 9명 1곳)이 `rows: 3`, `pastBottom: 13.4`(8명 셀은 `rows: 2`, `5.4`). 360px 은 모두 `rows: 2`, `3.4`.

- [ ] **Step 5-2: 320px 블록만 바이트 편집**

```bash
python3 -I - <<'EOF'
import re
p = 'src/main/resources/static/css/style.css'
data = open(p, 'rb').read()
pat = re.compile(rb'    \.participant-dots \{\r?\n        gap: 2px;\r?\n    \}\r?\n\r?\n'
                 rb'    \.participant-dot \{\r?\n        width: 6px;\r?\n        height: 6px;\r?\n    \}')
found = list(pat.finditer(data))
assert len(found) == 1, len(found)
m = found[0]
# 가장 가까운 앞쪽 @media 가 320px 블록인지
assert data.rfind(b'@media', 0, m.start()) == data.rfind(b'@media (max-width: 320px)', 0, m.start())
block = m.group(0)
eol = b'\r\n' if b'\r\n' in block else b'\n'
comment = '    /* 10명 가능한 날도 점 2줄 — 셀 내용 폭 약 32px 에 5px 점 5개 + 간격 1px 4개 */'.encode('utf-8') + eol
new = comment + (block.replace(b'gap: 2px;', b'gap: 1px;')
                      .replace(b'width: 6px;', b'width: 5px;')
                      .replace(b'height: 6px;', b'height: 5px;'))
open(p, 'wb').write(data[:m.start()] + new + data[m.end():])
print('style.css: 320px participant dots updated, eol =', eol)
EOF
```

Expected 출력: `eol = b'\r\n'`.

- [ ] **Step 5-3: GREEN — 다시 측정**

같은 하네스로 폭 300·320·344·360·390px 측정.

Expected (2026-10-07 같은 CSS 를 주입해 미리 잰 값): 300·320px 은 10명 셀 `rows: 2`, `pastBottom: 2.4`. 344·360px 은 `rows: 2`, `3.4`, 390px 은 `rows: 2`, `1.4` — 바꾸기 전과 같아야 한다.

- [ ] **Step 5-4: 개행·범위 확인**

```bash
git diff --numstat -- src/main/resources/static/css/style.css
git diff --ignore-cr-at-eol --numstat -- src/main/resources/static/css/style.css
```

Expected: 두 줄 같고, Task 0 기준선(`36 598`)에서 `+4 +3` 만큼 늘어난 `40 601`.

---

### Task 6: 문서 — ADR 2건 + 사실 문서 갱신

**Files:**
- Create: `docs/adr/datedate/domain/0010-participant-cap-10.md`
- Create: `docs/adr/datedate/ux/0004-ten-color-participant-palette.md`
- Modify: `docs/adr/datedate/ux/0001-participant-color-cell-sync.md:5`
- Modify: `docs/adr/README.md:19,23,25`, View 2 표 끝, 폴더 목록 2줄
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md:19`
- Modify: `docs/architecture.md:112`
- Modify: `docs/datedate/erd.html:525`

- [ ] **Step 6-1: ADR datedate/domain/0010 작성**

````markdown
# ADR-0010: 일정당 참여자 한도 8 → 10명, 한도 값은 서버가 내려준다

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-07 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 197 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

- 사용자 결정(2026-10-07): 일정 하나의 최대 참여 인원을 8명에서 10명으로 늘린다.
- 한도 검사는 `Schedule.MAX_PARTICIPANTS` 한 곳([ADR-0001](0001-schedule-aggregate-invariants.md))이지만, 숫자 8 이 다른 곳에 복제돼 있었다.
  - 웹 일정 페이지: 추가 버튼 `th:disabled="${…size() >= 8}"`, 카운터 메시지 `{0} / 8명`.
  - 앱인토스 미니앱(별도 레포 `datedate-app`): `canAdd = participants.length < 8`.
  - 문구: 홈·가이드·FAQ·소개·use-case 의 "최대 8명" — ko 16키, en 15키.
  상수만 바꾸면 서버는 10명을 받는데 웹 버튼은 8명에서 잠기고 카운터는 "9 / 8명" 이 된다. 이 복제는 어떤 테스트도 잡지 못했다.
- 한도 초과 409 메시지는 영문 고정(`messageKey` 없음)이었다. 웹 `api.js` 의 한국어 매핑은 키가 `MAX_PARTICIPANTS_EXCEEDED` 라
  서버 코드 `PARTICIPANT_LIMIT_EXCEEDED` 와 맞지 않아 한 번도 쓰이지 않았다.

## Decision — 무엇을 골랐나

일정당 참여자 한도를 **10명**으로 올리고, 클라이언트는 한도 숫자를 서버에서 받는다.

- `Schedule.MAX_PARTICIPANTS = 10` — 검사 위치(애그리거트)는 ADR-0001 그대로.
- `ScheduleDetailResponse.maxParticipants` 추가. 웹 뷰 모델과 일정 상세 API(`GET /api/owners/{id}/schedules/{y}/{m}`)가 같은 DTO 라
  템플릿(추가 버튼·카운터 `{0} / {1}명`)과 미니앱이 이 값 하나를 본다.
- `ParticipantLimitExceededException` 에 `errors.participantLimitExceeded` 키 + 한도 인자 — 409 메시지를 요청 로케일로 해석한다
  (`OwnerIdTakenException` 과 같은 패턴). 쓰이지 않던 `api.js` 매핑은 삭제.
- 한도를 숫자로 말하는 문구 키 목록을 `ParticipantCapCopyTest` 가 상수와 대조한다.
- 참여자 색 팔레트도 10색으로 늘린다 — 별도 결정 [ux/0004](../ux/0004-ten-color-participant-palette.md).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 상수만 10 으로 | 1줄 변경 | 웹 버튼이 8명에서 잠기고 카운터가 "9 / 8명" — 테스트로 안 잡힘 |
| 템플릿·메시지·미니앱에 10 을 각각 적기 | DTO 변경 없음 | 다음 변경 때 같은 누락이 반복된다 (이번에 4곳) |
| DTO 에 `canAddParticipant` 불리언도 노출 | 뷰가 규칙을 재구현하지 않음 | 카운터에 숫자가 어차피 필요 — 필드 하나로 충분하고 최종 강제는 서버 |
| **(선택) 서버 상수 → DTO `maxParticipants` → 템플릿·미니앱** | 숫자 원천 1곳, JSON 필드 추가라 하위 호환 | — |

## Consequences — 영향

- **긍정:** 한도를 다시 바꿀 때 서버 상수와 가드 테스트가 가리키는 문구만 고치면 된다. 409 메시지가 ko/en 으로 나간다
  (미니앱은 서버 `message` 를 그대로 보여준다).
- **부정:** 문구 키는 여전히 사람이 고친다 — 대신 `ParticipantCapCopyTest` 가 빠뜨린 키를 빌드에서 막는다.
- **유지되는 한계:** 한도 검사는 락 없이 메모리에서 한다 — 동시에 추가하면 한도를 넘을 수 있다 (8명 때와 같은 기존 동작, 이번에 바꾸지 않음).
- **배포·롤백:** DB 변경 없음. 일정 로드 시 인원을 검증하지 않으므로 롤백해도 9~10명 일정은 정상 표시된다(추가만 막힘).
  미니앱은 `maxParticipants` 가 없으면 8 로 폴백하므로 배포 순서에 묶이지 않는다.
- **후속:** 미니앱 `canAdd` 를 `maxParticipants` 기반으로 바꾼다 (별도 레포·앱인토스 심사).

## References

- 관련 코드: `datedate/domain/schedule/Schedule.java`, `datedate/presentation/dto/response/ScheduleDetailResponse.java`,
  `templates/schedule/view.html`, `datedate/application/exception/ParticipantLimitExceededException.java`
- 테스트: `ScheduleTest`, `ScheduleParticipantCapRenderingTest`, `ParticipantCapCopyTest`, `GlobalExceptionHandlerI18nTest`
- 계획: `docs/superpowers/plans/2026-10-07-participant-cap-10.md`
````

- [ ] **Step 6-2: ADR datedate/ux/0004 작성**

````markdown
# ADR-0004: 참여자 색 팔레트 10색 — 갈색·청록 2색을 끝에 덧붙인다

| 항목 | 값 |
|---|---|
| 상태 | Accepted — [0001](0001-participant-color-cell-sync.md) 의 "8색 spec" 결정을 대체 (색 = 1차 신호, 색맹 보조 신호 4종, 칩↔셀 색 동기화는 유지) |
| 날짜 | 2026-10-07 |
| 도메인 | datedate |
| 관심사 | UX / 접근성 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 197 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

- 참여자 한도가 10명이 됐다([domain/0010](../domain/0010-participant-cap-10.md)). 색은 `ParticipantColor.ofIndex(index % 8)`,
  `index = participants.size()` 라 9·10번째가 1·2번째와 같은 빨강·주황이 된다. 범례, 셀 점, 1명만 가능한 날의 셀 배경
  (9번째만 가능한 날이 1번째 사람 색으로 칠해짐), 편집 테두리에서 사람을 구분할 수 없다. 화면 문구는 "고유 색상" 을 약속한다.
- ADR-0001 은 "8색 spec 외 색 사용 금지" 를 정했다. 또 "AA 4.5:1 충족, Tailwind-600 은 흰 배경 위에서 모두 통과" 라고 적었지만
  실측 대비는 orange `#F97316` 2.80:1, yellow `#CA8A04` 2.94:1, green `#16A34A` 3.30:1, teal `#0D9488` 3.74:1 이다.

## Decision — 무엇을 골랐나

팔레트 **끝에** 2색을 덧붙여 10색으로 만든다.

- index 8 = `#92400E` (amber-800, 갈색), index 9 = `#0E7490` (cyan-700, 청록). index 0~7 은 그대로.
- 선정 근거 — 흰 배경 대비, 기존 8색과의 최소 CIEDE2000 (일반 / 녹색맹 deutan / 적색맹 protan, Machado 2009 severity 1.0):

  | 후보 | 흰 배경 대비 | 최소 ΔE00 |
  |---|---|---|
  | **amber-800 `#92400E`** | 7.09:1 | 23.7 / 13.9 / 14.1 |
  | **cyan-700 `#0E7490`** | 5.36:1 | 18.0 / 11.9 / 14.2 |
  | pink-600 `#DB2777` | 4.60:1 | 12.3 / 12.7 / 17.0 |
  | fuchsia-600 `#C026D3` | 4.71:1 | 12.2 / 7.4 / 2.7 |
  | lime-600 `#65A30D` | 3.09:1 | 11.5 / 4.7 / 2.9 |

  기존 8색 안의 최근접 쌍(indigo~violet 6.5, deutan 에서 blue~violet 0.5)보다 두 신규 색이 기존 색과 더 멀다.
- 불변식 가드: 한도까지 `nextColorIndex()` 로 채운 참여자의 색은 모두 다르다(`ScheduleTest.participantsUpToLimit_haveDistinctColors`).
  한도를 다시 올리면 팔레트도 늘려야 빌드가 통과한다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 8색 유지(9·10번째 색 중복 감수) | 변경 없음 | "고유 색상" 문구와 ADR-0001 전제가 깨진다 |
| 팔레트를 색상환 순서로 재배열 | 보기 좋은 순서 | 덧붙이기로 충분 — 재배열하면 테스트·문서의 기존 index 기대값만 흔들린다 |
| 직관적 후보 핑크·퓨샤 | 채도 높음 | 기존 rose·violet 과 가까움(ΔE 12.2~12.3), fuchsia 는 protan 2.7 |
| **(선택) 갈색·청록 덧붙이기** | 기존 색과 가장 멀고 대비 5:1 이상 | — |

## Consequences — 영향

- **긍정:** 10명까지 색이 겹치지 않는다. 색은 참여자별 hex 로 저장돼 기존 데이터·마이그레이션 영향이 없다(롤백해도 hex 형식만 검사).
- **부정:** 색만으로의 구분은 색각 이상 사용자에게 여전히 불완전 — ADR-0001 의 보조 신호(인원 배지·툴팁·aria-label)에 계속 의존한다.
- **같이 고친 것:** 320px 이하 화면에서 10명 가능한 날의 점이 3줄로 셀 밖 13.4px 까지 넘쳤다 → 그 구간만 점 5px·간격 1px 로 2줄
  (미세 CSS, 별도 ADR 없음).
- **남은 것:** 두 자리 인원 배지("10")가 모바일에서 날짜 숫자를 더 가린다(겹침 면적 +45~57%). 툴팁은 10명×10자 이름이면 1,108px.
  둘 다 기존 구조 문제라 범위 밖.

## References

- 관련 코드: `datedate/domain/participant/ParticipantColor.java`, `static/css/style.css` (`@media (max-width: 320px)` 의 `.participant-dot`)
- 테스트: `ParticipantColorTest`, `ParticipantTest`, `ScheduleTest`
- 계획(계산·실측 근거): `docs/superpowers/plans/2026-10-07-participant-cap-10.md` §0
````

- [ ] **Step 6-3: ux/0001 상태 줄**

`docs/adr/datedate/ux/0001-participant-color-cell-sync.md:5`:

```markdown
| 상태 | Accepted — 단, **팔레트 8색 spec 결정은 [0004](0004-ten-color-participant-palette.md) 로 Superseded** (색 = 1차 신호, 색맹 보조 신호 4종, 칩↔셀 색 동기화는 유효) |
```

- [ ] **Step 6-4: ADR README 두 뷰 + 폴더 수**

`docs/adr/README.md`:

| 위치 | 바꾸기 전 | 바꾼 뒤 |
|---|---|---|
| 19행 | `\| **datedate** \| 9 \| — \| 7 \| — \| — \| — \| — \| — \| — \| 16 \|` | `\| **datedate** \| 10 \| — \| 8 \| — \| — \| — \| — \| — \| — \| 18 \|` |
| 23행 | `\| **합계** \| 11 \| 16 \| 7 \| 14 \| 3 \| 5 \| 26 \| 6 \| 7 \| **95** \|` | `\| **합계** \| 12 \| 16 \| 8 \| 14 \| 3 \| 5 \| 26 \| 6 \| 7 \| **97** \|` |
| 25행 | `총 **95개 ADR**.` | `총 **97개 ADR**.` |
| 폴더 목록 | `- [datedate/domain/](datedate/domain/) — 9 ADRs` | `- [datedate/domain/](datedate/domain/) — 10 ADRs` |
| 폴더 목록 | `- [datedate/ux/](datedate/ux/) — 3 ADRs` | `- [datedate/ux/](datedate/ux/) — 4 ADRs` |

View 2 표의 마지막 행(`| 2026-09-27 | datedate/frontend | [0004 사이트 크롬 단일 fragment …`) 바로 뒤에 2행:

```markdown
| 2026-10-07 | datedate/domain | [0010 참여자 한도 10명 + 한도 값 서버 전달(maxParticipants)](datedate/domain/0010-participant-cap-10.md) | 사용자 결정 8 → 10 — 템플릿·미니앱·문구에 복제된 8 이 테스트에 안 잡힘 |
| 2026-10-07 | datedate/ux | [0004 참여자 색 10색 — 갈색·청록 덧붙이기](datedate/ux/0004-ten-color-participant-palette.md) | 한도 10 → 9·10번째가 1·2번째 색과 중복 — ux/0001 의 8색 spec 대체 |
```

README 가 기준선과 다르면(다른 세션이 ADR 을 추가) 숫자는 현재 값에서 +1·+1·+2 로 계산한다.

- [ ] **Step 6-5: 사실 문서 3곳**

`src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md:19` (작업 트리 CRLF·attr `eol=lf` — Edit 로 한 줄):

```markdown
- **ParticipantService** - 스케줄당 최대 10명 / 중복 이름 검증. 한도 값은 `Schedule.MAX_PARTICIPANTS` 하나 — 웹 템플릿과 일정 상세 API(미니앱)는 `ScheduleDetailResponse.maxParticipants` 로 받는다 ([ADR](../../../../../../../../docs/adr/datedate/domain/0010-participant-cap-10.md)).
```

`docs/architecture.md:112` — `참여자 최대 8명` → `참여자 최대 10명`.

`docs/datedate/erd.html:525` — `일정당 참여자 최대 8명` → `일정당 참여자 최대 10명`.

- [ ] **Step 6-6: 확인**

```bash
ls docs/adr/datedate/domain/0010-participant-cap-10.md docs/adr/datedate/ux/0004-ten-color-participant-palette.md docs/adr/datedate/ux/0001-participant-color-cell-sync.md
grep -nE "8명|datedate/domain/\) — 9 ADRs|datedate/ux/\) — 3 ADRs|\*\*95(\*\*|개)" docs/adr/README.md docs/architecture.md docs/datedate/erd.html src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md
git diff --numstat -- docs/adr/README.md docs/adr/datedate/ux/0001-participant-color-cell-sync.md docs/architecture.md docs/datedate/erd.html src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md
```

Expected: 파일 3개 존재. grep 출력 없음. numstat — README `7 5`, ux/0001 `1 1`, architecture.md `1 1`, erd.html `1 1`, CLAUDE.md `1 1`.

---

### Task 7: 전체 검증 + 커밋 로그

**Files:**
- Modify: `docs/guides/git-commit.md` (끝에 섹션 추가)

- [ ] **Step 7-1: 전체 스위트**

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"
python3 -I - <<'EOF'
import glob, re
tot = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
files = glob.glob('build/test-results/test/TEST-*.xml')
for f in files:
    head = re.search(r'<testsuite [^>]*', open(f, encoding='utf-8').read(4000)).group(0)
    for k in tot:
        v = re.search(k + r'="(\d+)"', head)
        tot[k] += int(v.group(1)) if v else 0
print('classes', len(files), tot)
EOF
```

Expected: `BUILD SUCCESSFUL`, failures 0, errors 0. 2026-10-05 Commit 196 기록(127 클래스 804 테스트) 이후 다른 변경이 없었다면 129 클래스 818 테스트(+2 클래스, +14 테스트). 실패하면 임시 우회하지 말고 원인을 조사한다.

- [ ] **Step 7-2: 잔여 확인**

```bash
grep -rn "MAX_PARTICIPANTS_EXCEEDED" src/ ; echo "exit=$? (1 기대)"
grep -n ">= 8" src/main/resources/templates/schedule/view.html ; echo "exit=$? (1 기대)"
J=src/main/java/me/singingsandhill/calendar/datedate; X=src/test/java/me/singingsandhill/calendar
F="$J/domain/schedule/Schedule.java $J/domain/participant/ParticipantColor.java $J/presentation/dto/response/ScheduleDetailResponse.java $J/application/exception/ParticipantLimitExceededException.java $J/application/CLAUDE.md $X/datedate/domain/schedule/ScheduleTest.java $X/datedate/domain/schedule/ParticipantCapCopyTest.java $X/datedate/domain/participant/ParticipantColorTest.java $X/datedate/domain/participant/ParticipantTest.java $X/datedate/presentation/controller/ScheduleParticipantCapRenderingTest.java $X/common/presentation/api/GlobalExceptionHandlerI18nTest.java src/main/resources/templates/schedule/view.html src/main/resources/templates/faq.html src/main/resources/templates/guide.html src/main/resources/templates/index.html src/main/resources/static/js/api.js docs/adr/README.md docs/adr/datedate/ux/0001-participant-color-cell-sync.md docs/architecture.md docs/datedate/erd.html"
git diff --numstat -- $F > /tmp/cap10-a.txt; git diff --ignore-cr-at-eol --numstat -- $F > /tmp/cap10-b.txt; diff /tmp/cap10-a.txt /tmp/cap10-b.txt && echo "EOL OK"
```

Expected: 두 grep exit=1, `EOL OK`. 신규 파일은 untracked 라 numstat 에 안 나온다 — 개행은 Write 가 LF 로 쓰므로 따로 볼 필요 없다. messages·style.css 는 다른 작업의 미커밋 변경이 섞여 있어 여기서 빼고, Task 4·5 에서 기준선 대비로 확인했다. 작업 트리의 `templates/guides/*.html` 개행 노이즈도 이 계획과 무관하다.

- [ ] **Step 7-3: (선택) 실제 앱 화면 확인 — 사용자 확인 후에만**

봇을 끈 채로 띄운다: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& set TRADING_BOT_ENABLED=false&& set STOCK_BOT_ENABLED=false&& .\\gradlew.bat bootRun"`. 일정 페이지에서 10명 추가 → 11번째 시 ko/en 토스트 문구, 범례 색 10개. 종료는 `cmd.exe /c "taskkill /F /IM java.exe"`. 사용자가 원하지 않으면 건너뛰고 보고서에 "MockMvc·하네스로만 검증" 이라고 적는다.

- [ ] **Step 7-4: 커밋 로그 섹션 추가**

덧붙이기 직전에 확인한다:

```bash
grep -oE "^# Commit [0-9]+" docs/guides/git-commit.md | sort -t' ' -k3 -n | tail -1
grep -nE "^# Commit 196" docs/guides/git-commit.md | cut -c1-80
```

- 마지막 번호가 196 이 아니면 아래 197 을 (마지막 + 1)로 바꾸고, ADR 0010·0004 의 "관련 커밋" 칸도 같은 번호로 고친다.
- Commit 196 에 ✅ 가 있으면 첫 번째 `# 주의:` 줄을 지우고 `git add` 의 `api.js` 뒤에 `src/main/resources/static/css/style.css` 를 넣는다.
- 검증 줄의 숫자는 Step 7-1·5-3 의 실제 값으로 쓴다.

```text
# =====================================================================
# 참여자 한도 8 -> 10명 + 참여자 색 10색 (2026-10-07)
# =====================================================================
# 배경: 사용자 결정 — 일정당 최대 인원 8 -> 10, 색은 10색으로 확장. 분석에서 8 의 복제(웹 버튼 th:disabled >= 8, 카운터 '{0} / 8명',
#   미니앱 canAdd < 8, 문구 ko 16키·en 15키)와 8색 순환(9·10번째 = 1·2번째 색)을 확인 — 어떤 테스트도 잡지 못하던 것.
# 계획: docs/superpowers/plans/2026-10-07-participant-cap-10.md
# ADR: datedate/domain/0010(한도 10 + ScheduleDetailResponse.maxParticipants), datedate/ux/0004(10색 — ux/0001 의 8색 spec 대체).
#   README 두 뷰·폴더 수, datedate/application/CLAUDE.md, docs/architecture.md, docs/datedate/erd.html 갱신.
# 검증: 전체 스위트 129 클래스 818 테스트 failures 0 (Step 7-1 실제 값으로). 320px 하네스 실측: 10명 가능한 날 점 3줄·13.4px -> 2줄·2.4px,
#   344·360·390px 불변. 앱 기동 검증: (Step 7-3 을 했으면 결과, 안 했으면 '안 함 — MockMvc 렌더링·하네스로만').
# 참고(미수정, 범위 밖): 동시 추가 시 한도 초과 가능(락 없는 메모리 검사, 기존 동작), 모바일 두 자리 배지·툴팁 폭,
#   api.js 의 다른 오류 문구 한국어 고정, use-case 팀 회의 FAQ q1 '시간 슬롯 투표 미지원'(ADR 0009 이후 낡음),
#   club-activity FAQ a3 '교체' 안내(삭제 UI 없음). 미니앱(datedate-app) 전환은 별도 레포 — 계획 Task 8.

# Commit 197 — feat(datedate): 참여자 한도 10명 + 참여자 색 10색 (ADR datedate/domain/0010, ux/0004) — 마지막 커밋(git-commit.md 포함)
# 주의: style.css(320px 점 크기)는 미실행 Commit 196 의 add 목록이 소유 — 196 을 먼저 실행하면 이 변경이 196 에 흡수된다.
# 주의: git-commit.md 는 미실행 Commit 187~190·195·196 의 add 목록에도 있다 — 먼저 실행되는 커밋에 흡수되고, 여기의 add 는 변경 없음으로 통과한다.
# 주의: messages*.properties 의 개행만 다른 436줄(가이드 블록)은 Windows git(autocrlf=input)이 add 시 LF 로 되돌린다 — 내용 변경 아님.
git add src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/Schedule.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColor.java src/main/java/me/singingsandhill/calendar/datedate/presentation/dto/response/ScheduleDetailResponse.java src/main/java/me/singingsandhill/calendar/datedate/application/exception/ParticipantLimitExceededException.java src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md src/main/resources/templates/schedule/view.html src/main/resources/templates/faq.html src/main/resources/templates/guide.html src/main/resources/templates/index.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/js/api.js src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/ParticipantCapCopyTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantColorTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleParticipantCapRenderingTest.java src/test/java/me/singingsandhill/calendar/common/presentation/api/GlobalExceptionHandlerI18nTest.java docs/adr/datedate/domain/0010-participant-cap-10.md docs/adr/datedate/ux/0004-ten-color-participant-palette.md docs/adr/datedate/ux/0001-participant-color-cell-sync.md docs/adr/README.md docs/architecture.md docs/datedate/erd.html docs/superpowers/plans/2026-10-07-participant-cap-10.md docs/guides/git-commit.md
git commit -m "feat(datedate): 참여자 한도 10명 + 참여자 색 10색 (ADR datedate/domain/0010, ux/0004)" -m "① 한도: Schedule.MAX_PARTICIPANTS 8 -> 10. 검사 위치(애그리거트)는 ADR datedate/domain/0001 그대로. ScheduleTest 에 한도 고정, 한도까지 색 고유(doesNotHaveDuplicates), 배포 전 8명 일정의 9·10번째 색 테스트." -m "② 색: ParticipantColor 팔레트 끝에 #92400E(amber-800)·#0E7490(cyan-700) 덧붙임 — index 0~7 불변, 저장된 색은 참여자별 hex 라 마이그레이션 없음. 선정 근거: 흰 배경 대비 7.09·5.36:1, 기존 8색과 최소 CIEDE2000 18.0(일반)·11.9(deutan)·14.1(protan) — 기존 최근접 쌍 indigo~violet 6.5 보다 멂. ux/0001 의 8색 spec 결정을 ux/0004 로 대체." -m "③ 한도 전달: ScheduleDetailResponse.maxParticipants 추가(JSON 필드 추가, 하위 호환) — 웹 뷰와 일정 상세 API 가 같은 DTO. view.html 의 추가 버튼 th:disabled 와 카운터('{0} / {1}명')가 이 값을 쓴다. 전에는 >= 8 과 '{0} / 8명' 하드코딩이라 상수만 바꾸면 웹 버튼이 8명에서 잠겼다." -m "④ 409 메시지: ParticipantLimitExceededException 에 errors.participantLimitExceeded + 한도 인자(ko/en). api.js 의 MAX_PARTICIPANTS_EXCEEDED 매핑은 서버 코드(PARTICIPANT_LIMIT_EXCEEDED)와 달라 한 번도 쓰이지 않던 줄이라 삭제 — 이제 한국어 사용자도 영문 원문 대신 현지화 문장을 본다." -m "⑤ 문구: 한도를 숫자로 말하는 ko 16키·en 15키를 10 으로(홈·가이드·FAQ·소개·use-case FAQ). 사실이 틀렸던 문장(9명 이상이면 기능적으로는 작동 등)은 10색 기준으로 다시 씀. ParticipantCapCopyTest 가 이 키들과 상수를 대조. 템플릿 정적 대체 텍스트 5곳도 맞춤." -m "⑥ 320px: 10명 가능한 날의 점이 320px 이하에서만 3줄·셀 밖 13.4px -> 그 블록만 점 5px·간격 1px 로 2줄(실측 2.4px). 332px 이상은 변화 없음." -m "검증: 전체 스위트 129 클래스 818 테스트 failures 0 (Step 7-1 실제 값으로)."
```

`-m` 본문에는 큰따옴표를 쓰지 않는다(cmd.exe 호환 — 작은따옴표로 대신함).

---

### Task 8: 미니앱 — `canAdd` 를 서버 한도로 (별도 레포)

`maxParticipants` 가 없으면 8 로 폴백하므로 서버 배포와 순서가 묶이지 않는다(배포 전엔 옛 서버와 같은 8, 배포 후엔 10). 이 레포에는 다른 작업이 스테이징된 채로 있으므로 커밋하지 않고 변경 내용을 사용자에게 보고한다. 앱인토스 빌드·배포(`ait build`, `ait deploy`)와 심사는 사용자가 한다.

**Files (모두 `/mnt/d/projects/datedate-app/DateDate/` 아래):**
- Modify: `src/api/types.ts` — `ScheduleDetailResponse`
- Modify: `src/features/availability/selection.ts`
- Test: `src/features/availability/selection.test.ts`
- Modify: `src/features/availability/AvailabilityEditScreen.tsx:12, 152`

**Interfaces:**
- Consumes: calendar `GET /api/owners/{id}/schedules/{y}/{m}` 응답의 `maxParticipants` (Task 2)
- Produces: `canAddParticipant(detail: Pick<ScheduleDetailResponse, "participants" | "maxParticipants">): boolean`

- [ ] **Step 8-1: 실패 테스트**

`selection.test.ts` 2행의 import 를 바꾸고, 파일 끝에 describe 를 추가한다:

```ts
import { toggled, sortedSelections, isValidName, canAddParticipant } from "./selection";
```

```ts
describe("canAddParticipant", () => {
  const people = (n: number) =>
    Array.from({ length: n }, (_, i) => ({
      id: i + 1,
      name: `p${i}`,
      color: "#E11D48",
      selections: [],
      updatedAt: "t",
    }));

  it("uses the server cap (maxParticipants)", () => {
    expect(canAddParticipant({ participants: people(9), maxParticipants: 10 })).toBe(true);
    expect(canAddParticipant({ participants: people(10), maxParticipants: 10 })).toBe(false);
  });

  it("falls back to the legacy cap of 8 when the server omits maxParticipants", () => {
    expect(canAddParticipant({ participants: people(7) })).toBe(true);
    expect(canAddParticipant({ participants: people(8) })).toBe(false);
  });
});
```

- [ ] **Step 8-2: RED 확인**

```bash
cd /mnt/d/projects/datedate-app/DateDate && npx vitest run src/features/availability/selection.test.ts
```

Expected: FAIL — `canAddParticipant` 가 export 되지 않음. (jsdom 환경 기동에 1분 넘게 걸린다.)

- [ ] **Step 8-3: 구현**

`src/api/types.ts` — `ScheduleDetailResponse` 의 `isExtendedMode: boolean;` 다음 줄:

```ts
  maxParticipants?: number; // 서버 한도 (calendar 2026-10 이후). 필드가 없던 서버는 8
```

`src/features/availability/selection.ts` — 파일 맨 위와 맨 끝:

```ts
import type { ScheduleDetailResponse } from "../../api/types";
```

```ts
// 서버가 한도를 내려주기 전(필드 없음)의 한도 — calendar 의 옛 Schedule.MAX_PARTICIPANTS
const LEGACY_MAX_PARTICIPANTS = 8;

export function canAddParticipant(
  detail: Pick<ScheduleDetailResponse, "participants" | "maxParticipants">,
): boolean {
  return detail.participants.length < (detail.maxParticipants ?? LEGACY_MAX_PARTICIPANTS);
}
```

`AvailabilityEditScreen.tsx` — 12행 import 와 152행:

```ts
import { toggled, sortedSelections, isValidName, canAddParticipant } from "./selection";
```

```ts
  const canAdd = canAddParticipant(data);
```

- [ ] **Step 8-4: GREEN + 타입 검사 + 전체 테스트**

```bash
cd /mnt/d/projects/datedate-app/DateDate
npx vitest run src/features/availability/selection.test.ts
npx tsc -p tsconfig.app.json --noEmit; echo "tsc exit=$?"
npm test
```

Expected: selection 테스트 6건 PASS, `tsc exit=0`(2026-10-07 기준선도 0), 전체 vitest PASS.

---

## Appendix A — 메시지 편집 스크립트

### `props_edit.py`

저장소 루트에서 `python3 -I "$T/props_edit.py" <spec.json> ...` 로 실행한다. 2026-10-07 에 현재 카탈로그 사본으로 Task 2~4 spec 6개를 모두 적용해 확인함: ko 19키·en 18키만 바뀜, 기존 CRLF 436줄과 `\uXXXX` 이스케이프 방식 그대로, ko/en 키 집합 일치.

```python
"""messages*.properties 키 값 교체·부분 치환·삽입 — 줄 단위 바이트 편집.

다른 줄의 바이트(개행 LF/CRLF, \\uXXXX 이스케이프 여부)는 건드리지 않는다.
사용: python3 -I props_edit.py <spec.json> [<spec.json> ...]

spec 예:
{
  "file": "src/main/resources/messages.properties",
  "set":     {"key": "새 값 전체"},
  "replace": {"key": ["바꿀 부분(값 안에 정확히 1번)", "새 부분"]},
  "insert_after": {"앵커키": ["새키", "값"]}
}
값은 사람이 읽는 텍스트로 쓴다. 원래 줄(삽입은 앵커 줄)이 \\uXXXX 이스케이프 줄이면
같은 방식으로 이스케이프하고, 개행도 원래 줄(앵커 줄)을 따른다.
"""
import json
import re
import sys


def escape(text):
    return ''.join(c if ord(c) < 128 else '\\u%04X' % ord(c) for c in text)


def unescape(text):
    return re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), text)


def apply(spec_path):
    spec = json.load(open(spec_path, encoding='utf-8'))
    path = spec['file']
    lines = open(path, 'rb').read().split(b'\n')

    def find(key):
        prefix = (key + '=').encode('ascii')
        idx = [i for i, line in enumerate(lines) if line.startswith(prefix)]
        if len(idx) != 1:
            sys.exit(f'{path}: "{key}=" 줄이 {len(idx)}개 — 정확히 1개여야 한다')
        return idx[0]

    def split(line):
        cr = line.endswith(b'\r')
        body = (line[:-1] if cr else line).decode('utf-8')
        key, value = body.split('=', 1)
        return key, value, cr, '\\u' in value

    def render(key, value, cr, escaped):
        text = escape(value) if escaped else value
        return (key + '=' + text).encode('utf-8') + (b'\r' if cr else b'')

    for key, value in spec.get('set', {}).items():
        i = find(key)
        _, _, cr, escaped = split(lines[i])
        lines[i] = render(key, value, cr, escaped)

    for key, (old, new) in spec.get('replace', {}).items():
        i = find(key)
        _, raw, cr, escaped = split(lines[i])
        value = unescape(raw)
        if value.count(old) != 1:
            sys.exit(f'{path}: {key} 값에 "{old}" 가 {value.count(old)}번 — 정확히 1번이어야 한다')
        lines[i] = render(key, value.replace(old, new), cr, escaped)

    for anchor, (new_key, value) in spec.get('insert_after', {}).items():
        if any(line.startswith((new_key + '=').encode('ascii')) for line in lines):
            sys.exit(f'{path}: {new_key} 가 이미 있다')
        i = find(anchor)
        _, _, cr, escaped = split(lines[i])
        lines.insert(i + 1, render(new_key, value, cr, escaped))

    open(path, 'wb').write(b'\n'.join(lines))
    print(f"{path}: set {len(spec.get('set', {}))}, replace {len(spec.get('replace', {}))}, "
          f"insert {len(spec.get('insert_after', {}))}")


if __name__ == '__main__':
    for arg in sys.argv[1:]:
        apply(arg)
```

### `task2-ko.json` / `task2-en.json`

```json
{"file": "src/main/resources/messages.properties", "set": {"schedule.participant.count": "{0} / {1}명"}}
```

```json
{"file": "src/main/resources/messages_en.properties", "set": {"schedule.participant.count": "{0} / {1}"}}
```

### `task3-ko.json` / `task3-en.json`

```json
{"file": "src/main/resources/messages.properties",
 "insert_after": {"errors.startFailed": ["errors.participantLimitExceeded", "참여자는 일정당 최대 {0}명까지 추가할 수 있습니다."]}}
```

```json
{"file": "src/main/resources/messages_en.properties",
 "insert_after": {"errors.startFailed": ["errors.participantLimitExceeded", "A schedule can have up to {0} participants."]}}
```

### `task4-ko.json`

```json
{
 "file": "src/main/resources/messages.properties",
 "set": {
  "index.feature3.title": "최대 10명 참여",
  "guide.step4.li1": "최대 10명까지 참여 가능",
  "guide.troubleshooting.q3": "10명보다 많은 인원은 어떻게 하나요?",
  "seo.useCase.friend-meetup.section.faq.q1": "친구가 10명 넘으면 어떻게 하나요?",
  "seo.useCase.friend-meetup.section.faq.a1": "DateDate는 한 페이지당 최대 10명까지 참여할 수 있습니다. 참여자마다 다른 색이 붙어 캘린더에서 누가 가능한지 한눈에 구분되는 인원이 10명이기 때문입니다. 11명 이상 모임은 코어 멤버 5-6명으로 먼저 날짜를 정하고, 나머지에게는 결과만 통보하는 구조가 효율적입니다.",
  "seo.useCase.team-meeting.section.faq.q3": "팀이 10명을 넘어 11명입니다. 어떻게 하나요?",
  "seo.useCase.travel-planning.section.faq.q1": "10명을 넘는 여행 페이지를 만들 수 있나요?",
  "seo.useCase.travel-planning.section.faq.a1": "DateDate는 한 페이지에 최대 10명까지 참여할 수 있습니다. 그보다 큰 여행은 결제 분담 계산도 복잡해지므로, 코어 결정 그룹 5-6명이 출발일을 정하고 나머지는 결정에 따르는 구조가 효율적입니다.",
  "seo.useCase.club-activity.section.faq.a1": "DateDate는 한 페이지당 최대 10명을 권장합니다. 20명 규모 동호회는 매 회차 실제 참석 코어 6~10명을 기준으로 페이지를 운영하고, 나머지 회원에게는 확정된 날짜·장소만 단톡방으로 공지하는 구조가 효율적입니다. 출석이 잦은 회원 위주로 페이지를 채우세요."
 },
 "replace": {
  "index.feature2.desc": ["최대 8명이", "최대 10명이"],
  "index.why.limits.item2": ["한 페이지 8명까지", "한 페이지 10명까지"],
  "guide.troubleshooting.a3": ["최대 8명까지", "최대 10명까지"],
  "faq.a2": ["최대 8명이", "최대 10명이"],
  "about.section.audience.body": ["8명 이내의", "10명 이내의"],
  "seo.useCase.team-meeting.body": ["최대 8명까지", "최대 10명까지"],
  "seo.useCase.friend-meetup.section.tips.tip3.text": ["5-8명의", "5-10명의"],
  "seo.useCase.club-activity.section.faq.a3": ["최대 8명 권장 한도", "최대 10명 권장 한도"]
 }
}
```

### `task4-en.json`

```json
{
 "file": "src/main/resources/messages_en.properties",
 "set": {
  "index.feature3.title": "Up to 10 Participants",
  "guide.step4.li1": "Up to 10 participants",
  "guide.troubleshooting.q3": "What if we have more than 10 people?",
  "seo.useCase.friend-meetup.section.faq.q1": "What if more than 10 friends are invited?",
  "seo.useCase.friend-meetup.section.faq.a1": "DateDate caps a page at 10 participants — the most the calendar can show in distinct colors at a glance. For groups of 11 or more, lock the date with a core 5 or 6, then notify the rest. The math improves and so does the reply rate.",
  "seo.useCase.travel-planning.section.faq.q1": "Can I plan a trip with more than 10 people?",
  "seo.useCase.travel-planning.section.faq.a1": "DateDate caps a page at 10. Beyond that, cost-splitting math gets messy anyway. For larger groups, lock the date with a core 5 or 6 and notify the rest. The decision quality goes up as the group shrinks.",
  "seo.useCase.club-activity.section.faq.a1": "DateDate recommends up to 10 people per page. For a 20-person club, run the page around the core 6-10 who actually attend each session and just announce the confirmed date and venue to the rest in the chat. Fill the page with your frequent attendees."
 },
 "replace": {
  "index.feature2.desc": ["Up to 8 participants", "Up to 10 participants"],
  "index.why.limits.item2": ["up to 8 people", "up to 10 people"],
  "guide.troubleshooting.a3": ["up to 8 participants", "up to 10 participants"],
  "faq.a2": ["Up to 8 participants", "Up to 10 participants"],
  "about.section.audience.body": ["up to eight people", "up to 10 people"],
  "seo.useCase.team-meeting.body": ["Up to 8 participants", "Up to 10 participants"],
  "seo.useCase.friend-meetup.section.tips.tip3.text": ["five to eight regulars", "five to ten regulars"],
  "seo.useCase.club-activity.section.faq.a3": ["recommended 8-person limit", "recommended 10-person limit"]
 }
}
```

## Appendix B — 캘린더 셀 측정 하네스 (Task 5)

앱을 띄우지 않고 실제 `style.css` 와 `schedule/main.js` 로 셀을 그린다. 저장소 밖 디렉터리 `H` 에 `index.html` 로 저장하고 `static` 을 심볼릭 링크한 뒤 WSL 에서 서빙한다. Windows Chrome 은 WSL2 localhost 포워딩으로 `http://localhost:8765/` 에 접근한다.

```bash
H=/tmp/cap10-harness; mkdir -p $H && ln -sfn /mnt/d/projects/calendar/src/main/resources/static $H/static
cd $H && python3 -m http.server 8765 --bind 127.0.0.1   # 백그라운드로 실행, 끝나면 종료
```

```html
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>participant cap harness</title>
<link rel="stylesheet" href="/static/css/style.css">
<link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css" rel="stylesheet">
<script>
    // ?n=참여자 수. dayIndex 5+k 에 정확히 k 명이 가능(k=1..n), dayIndex 30 에 전원.
    (function () {
        var n = parseInt(new URLSearchParams(location.search).get('n') || '10', 10);
        var colors = ['#E11D48', '#F97316', '#CA8A04', '#16A34A', '#0D9488', '#2563EB', '#4F46E5', '#7C3AED', '#92400E', '#0E7490'];
        var names = ['민지', '서준', '하은', '도윤', '지우', '예준', '수아', '시우', '지호', '유나'];
        var participants = [];
        for (var i = 0; i < n; i++) {
            var sel = [30];
            for (var k = i + 1; k <= n; k++) sel.push(5 + k);
            participants.push({ id: i + 1, name: names[i], color: colors[i % colors.length], selections: sel });
        }
        window.SCHEDULE_DATA = {
            scheduleId: 1, ownerId: 'harness', year: 2026, month: 10, daysInMonth: 31, firstDayOfWeek: 4,
            totalDays: 49, isExtendedMode: true, participants: participants, locations: [], menus: [], timeSlots: [],
            messages: { selectName: 'x', locationRequired: 'x', menuRequired: 'x', saveSuccess: 'x', linkCopied: 'x',
                linkCopyPrompt: 'x', voteLabel: 'x', votesTemplate: '{0}', timeDayEmpty: 'x', timeEmpty: 'x',
                timeRangeInvalid: 'x', timeNoVotes: 'x', timeDayCountTemplate: '{0}', timeBestTemplate: '{0} {1}' }
        };
        window.api = {};
        window.toast = { error: function () {}, success: function () {} };
    })();
</script>
</head>
<body>
<main class="container" id="main-content">
    <div class="calendar-container">
        <div class="calendar">
            <div class="calendar-header">
                <div class="weekday">일</div><div class="weekday">월</div><div class="weekday">화</div><div class="weekday">수</div>
                <div class="weekday">목</div><div class="weekday">금</div><div class="weekday">토</div>
            </div>
            <div class="calendar-body" id="calendarBody"></div>
        </div>
    </div>
</main>
<script type="module" src="/static/js/schedule/main.js"></script>
</body>
</html>
```

수동 측정: Windows Chrome 으로 `http://localhost:8765/?n=10` → DevTools 기기 툴바에서 폭 지정 → 새로고침 → 콘솔:

```js
[...document.querySelectorAll('.calendar-day')]
  .filter(c => c.querySelectorAll('.participant-dot').length >= 8)
  .map(c => ({
    day: +c.dataset.day,
    dots: c.querySelectorAll('.participant-dot').length,
    rows: new Set([...c.querySelectorAll('.participant-dot')].map(d => Math.round(d.getBoundingClientRect().top))).size,
    pastBottom: +(c.querySelector('.participant-dots').getBoundingClientRect().bottom - c.getBoundingClientRect().bottom).toFixed(1)
  }))
```

## Appendix C — 하네스 자동 측정 (CDP)

WSL 에 브라우저가 없어 Windows Chrome 헤드리스를 CDP 로 조종한다. **반드시 전용 `--user-data-dir`** 로 띄운다 — 없으면 사용자의 실행 중인 Chrome 세션에 붙는다. 스크립트 경로는 따옴표 없이 넘긴다(cmd.exe 가 안쪽 따옴표를 깨뜨림).

```bash
cd /mnt/d && "/mnt/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --hide-scrollbars --no-first-run --no-default-browser-check --remote-debugging-port=9222 "--user-data-dir=C:\\Temp\\cap10-profile" about:blank &
W=$(wslpath -w /tmp/cap10-harness)   # cdp-cells.mjs 를 이 디렉터리에 저장
cd /mnt/d && cmd.exe /c "node --experimental-websocket $W\\cdp-cells.mjs 300,320,344,360,390"
```

`cdp-cells.mjs`:

```js
// node --experimental-websocket cdp-cells.mjs <폭,폭,...> — 각 폭에서 ?n=10 하네스의 8명 이상 셀을 잰다
const widths = process.argv[2].split(',').map(Number);
const target = await (await fetch('http://127.0.0.1:9222/json/new?about:blank', { method: 'PUT' })).json();
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
let seq = 0;
const pending = new Map();
let onLoad = null;
ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
    if (msg.method === 'Page.loadEventFired' && onLoad) { onLoad(); onLoad = null; }
};
const send = (method, params = {}) => new Promise((res) => {
    const id = ++seq;
    pending.set(id, res);
    ws.send(JSON.stringify({ id, method, params }));
});
const MEASURE = `(async () => {
  await document.fonts.ready;
  await new Promise(r => setTimeout(r, 400));
  return [...document.querySelectorAll('.calendar-day')]
    .filter(c => c.querySelectorAll('.participant-dot').length >= 8)
    .map(c => ({
      day: +c.dataset.day,
      dots: c.querySelectorAll('.participant-dot').length,
      rows: new Set([...c.querySelectorAll('.participant-dot')].map(d => Math.round(d.getBoundingClientRect().top))).size,
      pastBottom: +(c.querySelector('.participant-dots').getBoundingClientRect().bottom - c.getBoundingClientRect().bottom).toFixed(1)
    }));
})()`;
await send('Page.enable');
await send('Network.enable');
// http.server 는 Cache-Control 이 없어 같은 브라우저가 옛 style.css 를 휴리스틱 캐시로 재사용한다
await send('Network.setCacheDisabled', { cacheDisabled: true });
for (const width of widths) {
    await send('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 2, mobile: true });
    const loaded = new Promise((r) => { onLoad = r; });
    await send('Page.navigate', { url: 'http://localhost:8765/?n=10' });
    await loaded;
    const r = await send('Runtime.evaluate', { expression: MEASURE, returnByValue: true, awaitPromise: true });
    console.log(width, JSON.stringify(r.result.result.value));
}
ws.close();
```

정리: 브라우저 엔드포인트(`/json/version` 의 `webSocketDebuggerUrl`)로 `Browser.close` 를 보낸 뒤 `cmd.exe /c "rmdir /s /q C:\\Temp\\cap10-profile"`, http.server 종료. `taskkill chrome.exe` 는 사용자 Chrome 까지 죽이므로 쓰지 않는다.
