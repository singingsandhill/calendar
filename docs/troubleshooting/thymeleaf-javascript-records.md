# Thymeleaf + Java Records 이슈

## JavaScript에서 undefined 속성

### 증상
API 호출이 URL에 `undefined`가 포함되어 실패.
```
Failed to load resource: /api/schedules/undefined/participants - 500 error
```

Thymeleaf로 직렬화된 객체에서 속성에 접근하는 JavaScript 코드가 `undefined`를 반환.:
```javascript
console.log(scheduleData.id);  // undefined
```

### 원인
Thymeleaf의 인라인 JavaScript 직렬화(`[[${object}]]`)는 Java Record의 접근자 메서드를 제대로 직렬화하지 않음. 
전체 레코드가 직렬화될 때 속성이 올바르게 매핑되지 않을 수 있음.

**Java Record:**
```java
public record ScheduleDetailResponse(
    Long id,
    String ownerId,
    int year,
    int month
    // ...
) { }
```

**Thymeleaf 템플릿:**
```html
<script th:inline="javascript">
    const scheduleData = [[${schedule}]];  // 객체에 누락된 속성이 있을 수 있음
    console.log(scheduleData.id);          // undefined!
</script>
```

### 해결방법
접근자 메서드 구문을 사용하여 레코드 속성에 명시적으로 접근

```html
<script th:inline="javascript">
    const scheduleData = [[${schedule}]];
    const scheduleId = [[${schedule.id()}]];  // 명시적 접근은 올바르게 작동함

    // API 호출에서 scheduleId를 직접 사용
    await api.addParticipant(scheduleId, name);
</script>
```

### 디버깅
console.log를 추가하여 직렬화된 객체를 검사
```javascript
console.log('scheduleData:', scheduleData);
console.log('scheduleData.id:', scheduleData.id);
console.log('scheduleId:', scheduleId);
```

### 모범 사례

1. **명시적 속성 추출**: Java Record를 Thymeleaf와 함께 사용할 때 필요한 속성을 명시적으로 추출
   ```html
   const id = [[${record.id()}]];
   const name = [[${record.name()}]];
   ```

2. **전체 객체 직렬화에 의존하지 마세요**: `[[${record}]]`가 모든 속성에 접근 가능한 JavaScript 객체를 생성할 것이라고 가정하지 않을 것

3. **개발 중 디버깅 사용**: Thymeleaf를 JavaScript와 통합할 때 항상 `console.log` 문을 추가

### 영향받는 코드
- `src/main/resources/templates/schedule/view.html`
- `[[${javaRecord}]]` 구문을 사용하는 모든 템플릿

### 관련 자료
- Thymeleaf JavaScript 인라이닝: https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#javascript-inlining

---

## 중첩된 Record 직렬화 이슈

### 증상
데이터베이스에 데이터가 존재하지만 화면에 표시되지 않음:
- DB 확인: `#E74C3C, tester, [2], schedule_id=33` 존재
- 브라우저 Console, Spring 로그에 에러 없음
- JavaScript에서 `p.selections`, `p.color` 등이 `undefined`

### 원인
명시적 속성 추출(`[[${schedule.participants()}]]`)을 사용해도 **중첩된 Java Record**의 내부 속성은 여전히 직렬화되지 않음.

**문제 코드:**
```javascript
const participants = [[${schedule.participants()}]];
// participants 배열은 존재하지만, 각 요소의 속성이 undefined
participants.forEach(p => {
    console.log(p.id);         // undefined
    console.log(p.selections); // undefined
});
```

**데이터 흐름:**
```
List<ParticipantResponse>  ← ParticipantResponse도 Java Record
    ↓ Thymeleaf 직렬화
JavaScript Array           ← 각 요소의 속성(id, name, selections)이 누락됨
```

### 해결방법
Thymeleaf의 인라인 반복문(`[# th:each]`)으로 **각 중첩 Record의 속성을 명시적으로 추출**:

```html
<script th:inline="javascript">
    const participants = [
        [# th:each="p, stat : ${schedule.participants()}"]
        {
            id: [[${p.id()}]],
            name: [[${p.name()}]],
            color: [[${p.color()}]],
            selections: [[${p.selections()}]]
        }[# th:unless="${stat.last}"],[/]
        [/]
    ];
</script>
```

**생성되는 JavaScript:**
```javascript
const participants = [
    { id: 1, name: "tester", color: "#E74C3C", selections: [2] },
    { id: 2, name: "user2", color: "#3498DB", selections: [1, 5, 10] }
];
```

### 핵심 사항

| 문법 | 설명 |
|-----|------|
| `[# th:each="p, stat : ${list}"]...[/]` | 인라인 반복문 |
| `[[${p.id()}]]` | Record accessor 메서드 호출 |
| `[# th:unless="${stat.last}"],[/]` | 마지막 요소가 아닐 때만 콤마 추가 |

### 디버깅
```javascript
console.log('participants:', participants);
console.log('first participant:', participants[0]);
console.log('selections:', participants[0]?.selections);
```

### 영향받는 코드
- `src/main/resources/templates/schedule/view.html` (lines 96-106)
- `List<JavaRecord>`를 JavaScript로 직렬화하는 모든 템플릿