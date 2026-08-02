# Thymeleaf SpEL 정수 나눗셈 이슈

## 템플릿에서 계산한 평균이 항상 `0.0` 으로 표시됨

### 증상

`/insights/trends` "이용 현황 > 상세 통계" 에서 원시 카운트는 정상인데 평균만 `0.0`:

```
장소 투표
  등록된 장소        229
  장소 투표 수       200
  장소당 평균 투표   0.0    ← 200/229 ≈ 0.87 이어야 함

메뉴 투표
  등록된 메뉴         87
  메뉴 투표 수        72
  메뉴당 평균 투표   0.0    ← 72/87 ≈ 0.83 이어야 함
```

이 버그의 악질적인 점은 **관측 신호가 전혀 없다는 것**이다.

- 예외 없음 (스택트레이스 0건)
- 로그 없음 (WARN·ERROR 0건)
- 테스트 실패 없음 (템플릿 산술이라 단위 테스트 사각지대)
- 화면상 값이 `0.0` 이라 "아직 투표가 없나 보다" 로 오독되기 쉬움 — `0` 이 아니라 소수점이
  붙은 `0.0` 이라 *계산은 됐는데 결과가 0* 인 것처럼 보인다

### 원인

Thymeleaf 표현식 안의 `/` 는 SpEL 이 평가한다. **피연산자가 둘 다 정수 타입(`long`/`int`)이면
Java 와 동일하게 정수 나눗셈**을 하고 소수부를 버린다.

```html
<!-- 문제 코드: trends.html:236 -->
<span th:text="${#numbers.formatDecimal(stats.totalLocationVotes / stats.totalLocations, 1, 1)}">0.0</span>
```

```java
// ServiceStatsDto — 양쪽 모두 long
public record ServiceStatsDto(
        long totalLocations,
        long totalLocationVotes,
        ...
) {}
```

평가 순서:

| 단계 | 결과 |
|---|---|
| `200 / 229` (long ÷ long) | `0` — 소수부 절삭 |
| `#numbers.formatDecimal(0, 1, 1)` | `"0.0"` — 최소 소수 자릿수 1 이라 `.0` 을 덧붙임 |

즉 **`formatDecimal` 이 절삭된 값을 소수처럼 포장해 버그를 위장**한다. 표시가 `0` 이었다면
바로 눈치챘을 텐데 `0.0` 이라 정상 계산 결과로 보인다.

여기에 방어 코드가 원인 은폐를 한 겹 더한다:

```html
<div class="stats-detail-row" th:if="${stats.totalLocations > 0}">
```

이 `th:if` 는 0 나눗셈 `ArithmeticException` 을 막으려고 둔 가드인데, 결과적으로 **예외라는
유일한 발견 경로까지 차단**해 조용한 실패로 만들었다.

#### 결정적 단서 — 같은 화면의 세 번째 평균은 정상이었다

```html
<!-- trends.html:211 — 이 값만 정상 -->
<div th:text="${#numbers.formatDecimal(stats.avgParticipantsPerSchedule, 1, 1)}">0.0</div>
```

`avgParticipantsPerSchedule` 은 **서비스에서 이미 `double` 로 계산돼 넘어온 필드**라 템플릿은
포맷만 한다. 평균 3종 중 1종만 멀쩡하다는 비대칭이 곧 "산술이 뷰에 있는 것 자체가 원인" 이라는
지목이다. 증상을 볼 때 **정상 케이스와 비정상 케이스의 차이**를 먼저 찾으면 원인이 좁혀진다.

### 재현 조건

몫이 1 미만일 때만 눈에 띄지만, 실제로는 **모든 소수부가 사라진다**.

| 분자/분모 | 실제 값 | 정수 나눗셈 표시 | 발견 난이도 |
|---|---|---|---|
| 3 / 6 | 0.5 | `0.0` | 쉬움 — 명백히 틀림 |
| 200 / 229 | 0.873 | `0.0` | 쉬움 |
| 7 / 2 | 3.5 | `3.0` | **어려움** — 그럴듯해서 통과 |
| 7 / 7 | 1.0 | `1.0` | 발견 불가 — 우연히 일치 |

마지막 두 행이 이 버그가 오래 살아남는 이유다. 분모가 분자보다 작으면 값이 그럴듯해서
아무도 의심하지 않는다.

### 해결방법

**산술을 서비스 계층으로 옮기고 `double` 로 계산한다.** 템플릿은 포맷만 담당.

```java
// InsightsService.getServiceStats()
double avgVotesPerLocation = totalLocations > 0
        ? (double) totalLocationVotes / totalLocations   // ← 캐스트가 핵심
        : 0;
double avgVotesPerMenu = totalMenus > 0
        ? (double) totalMenuVotes / totalMenus
        : 0;
```

```java
// ServiceStatsDto — double 필드로 실어 보낸다
public record ServiceStatsDto(
        ...
        double avgParticipantsPerSchedule,
        double avgVotesPerLocation,
        double avgVotesPerMenu
) {}
```

```html
<!-- trends.html — 산술 제거, 필드만 출력 -->
<span th:text="${#numbers.formatDecimal(stats.avgVotesPerLocation, 1, 1)}">0.0</span>
```

`th:if="${stats.totalLocations > 0}"` 가드는 **유지한다** — 이제 0 나눗셈 방지 역할은
서비스의 삼항 연산자가 맡고, `th:if` 는 등록 0건일 때 의미 없는 행을 숨기는 본래 표시 목적만
남는다.

#### 차선책 — 템플릿 안에서 승격 (권장하지 않음)

```html
<!-- long → double 승격. 동작은 하지만 산술이 뷰에 남는다 -->
<span th:text="${#numbers.formatDecimal(stats.totalLocationVotes * 1.0 / stats.totalLocations, 1, 1)}">0.0</span>
```

diff 는 가장 작지만 **단위 테스트로 고정할 수 없다.** 같은 실수가 다음 템플릿에서 반복되는 것도
막지 못한다. 급한 핫픽스가 아니면 서비스 계층으로 옮길 것.

### 왜 서비스 계층인가 — 테스트 가능성

산술이 뷰에 있으면 검증 수단이 렌더링 테스트뿐이라 사실상 방치된다. 서비스로 내리면 즉시
단위 테스트로 고정된다:

```java
@Test
@DisplayName("평균의 소수부가 보존된다 (정수 나눗셈이면 3.0 이 된다)")
void averageVotesKeepsFractionalPart() {
    givenCounts(1, 1, 2, 7, 4, 10);

    ServiceStatsDto stats = service.getServiceStats();

    assertThat(stats.avgVotesPerLocation()).isEqualTo(3.5);   // 정수 나눗셈이면 3.0 → RED
    assertThat(stats.avgVotesPerMenu()).isEqualTo(2.5);
}
```

`7 / 2 = 3.5` 케이스를 반드시 넣을 것. `0.5` 같은 1 미만 케이스만으로는 "분모 > 분자" 조건에서만
잡히고, 위 재현 표의 세 번째 행(그럴듯해서 통과하는 유형)을 놓친다.

### 디버깅

증상을 보고 원인까지 가는 순서:

1. **표시값이 `0.0` 인데 원시 카운트는 0 이 아닌가?** → 계산 단계 문제로 확정. 데이터 조회를
   의심하며 리포지토리·쿼리를 파는 건 시간 낭비다.
2. **같은 화면에 정상 동작하는 유사 지표가 있는가?** → 있으면 그 둘의 차이가 곧 원인
   (여기서는 "서비스 계산 필드 vs 템플릿 계산").
3. **해당 표현식의 피연산자 타입을 DTO 에서 확인** → 둘 다 정수면 확정.

정수 나눗셈이 화면에 남아 있는지 전수 확인:

```bash
# A) formatDecimal 인자 안에서 나눗셈하는 곳
grep -rn "formatDecimal" src/main/resources/templates/ | grep " / "

# B) th:text SpEL 안의 나눗셈 전반 (A 보다 넓게)
grep -rnE 'th:text="\$\{[^"]*[a-zA-Z0-9)] / [a-zA-Z]' src/main/resources/templates/
```

> 2026-07-30 기준 두 명령 모두 결과 없음(수정 완료 상태). 이 저장소의 `formatDecimal` /
> `formatPercent` 사용처 15곳 중 나머지는 `BigDecimal` 이거나 단일 값이라 무관하다 —
> `stock/settings.html`, `stock/fragments/formats.html`, `trading/settings.html`.
> `BigDecimal.divide()` 는 정수 절삭 문제가 없는 대신 **`scale`/`RoundingMode` 미지정 시
> 무한소수에서 `ArithmeticException`** 이 나므로 별개로 주의.

### 재발 방지 체크리스트

- [ ] 템플릿 표현식(`th:text`, `[[${...}]]`)에서 `/` 를 쓰지 않는다 — 산술은 서비스/DTO 에서
- [ ] 비율·평균을 DTO 에 담을 때 필드 타입을 `double` 로 선언한다 (`long` 이면 호출부에서 절삭)
- [ ] 정수끼리 나눌 때는 `(double)` 캐스트를 분자에 붙인다 — `(double) a / b` (`(double)(a / b)`
      는 이미 절삭된 뒤라 무의미)
- [ ] 0 나눗셈 가드를 계산 지점에 둔다. `th:if` 로 화면에서 막으면 예외가 사라져 **발견 경로까지
      막힌다**
- [ ] 평균/비율 단위 테스트에 **몫이 나누어떨어지지 않는 케이스**(`7/2 = 3.5`)를 반드시 포함

### 영향받는 코드

- `src/main/resources/templates/insights/trends.html` (236, 252행)
- `src/main/java/me/singingsandhill/calendar/datedate/application/service/InsightsService.java`
- `src/main/java/me/singingsandhill/calendar/datedate/application/dto/ServiceStatsDto.java`
- 회귀 가드: `src/test/java/me/singingsandhill/calendar/datedate/application/service/InsightsServiceTest.java`

### 남은 이슈 (표시 버그와 별개)

수정 후 값은 `0.9` / `0.8` 이다. 분모인 `locationRepository.count()` / `menuRepository.count()`
가 **만료·삭제 미필터 전역 카운트**라 한 번도 투표받지 못한 장소·메뉴가 전부 포함되기 때문 —
지표 정의 문제이지 계산 버그가 아니다. "투표받은 장소당 평균" 으로 바꾸려면 분모를
`votes > 0` 으로 좁히는 별도 결정이 필요하다.

### 관련 자료

- Thymeleaf 표준 표현식 문법 (산술 연산):
  https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#arithmetic-operations
- 같은 계열의 함정 — 레코드 직렬화: [Thymeleaf + Java Records](thymeleaf-javascript-records.md)
- i18n 숫자 포맷 함정(`{n,number,#}` 로 천단위 그룹화 차단)은 루트 `CLAUDE.md` §i18n 참고
