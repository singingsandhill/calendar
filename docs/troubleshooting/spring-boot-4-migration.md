# Spring Boot 4.0 마이그레이션 이슈

## WebMvcTest Import 오류

### 증상
```
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
```

`./gradlew build` 실행 시 컴파일 오류로 빌드가 실패.

### 원인
Spring Boot 4.0.0은 모듈 구조를 재구성했. `@WebMvcTest` 어노테이션은 모듈화 작업의 일환으로 새로운 패키지로 이동.

**패키지 변경사항:**
| 어노테이션 | Spring Boot 3.x | Spring Boot 4.0 |
|------------|-----------------|-----------------|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |

### 해결방법

테스트 파일에서 import 문을 업데이트.

```java
// 이전 (Spring Boot 3.x)
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// 이후 (Spring Boot 4.0)
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

### 의존성
`build.gradle`에 테스트 스타터가 포함되어 있는지 확인:
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
```

---

## 기타 일반적인 마이그레이션 이슈

### 스타터 이름 변경
| 기존 이름 | 새 이름 |
|----------|----------|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |

### 테스트 스타터 패턴
Spring Boot 4.0에서는 각 스타터마다 대응하는 테스트 스타터가 존재.
- `spring-boot-starter-webmvc` -> `spring-boot-starter-webmvc-test`
- `spring-boot-starter-thymeleaf` -> `spring-boot-starter-thymeleaf-test`