# JPA NonUniqueResultException 이슈

## 증상

`/trading` 대시보드 접속 시 500 에러 발생:

```
org.springframework.dao.IncorrectResultSizeDataAccessException:
Query did not return a unique result: 2 results were returned
```

### 에러 로그

```
2026-01-05T14:56:24.846Z ERROR [datedate] [nio-8081-exec-8]
m.s.c.c.p.api.GlobalExceptionHandler: Unhandled exception:
Query did not return a unique result: 2 results were returned

Caused by: org.hibernate.NonUniqueResultException:
Query did not return a unique result: 2 results were returned
    at org.hibernate.query.spi.AbstractSelectionQuery.uniqueElement(...)
    at PositionRepositoryAdapter.findOpenPositionByMarket(PositionRepositoryAdapter.java:43)
    at ProfitService.getProfitSummary(ProfitService.java:72)
    at TradingDashboardController.dashboard(TradingDashboardController.java:33)
```

### SQL 로그

```sql
SELECT p FROM PositionJpaEntity p
WHERE p.market = ? AND p.status = 'OPEN'
-- 결과: 2개 행 반환
```

---

## 원인

### 비즈니스 로직과 쿼리 반환 타입 불일치

| 구분 | 설정/코드 | 의미 |
|------|----------|------|
| **비즈니스 로직** | `maxPositions = 2` | 동일 마켓에 최대 2개 OPEN 포지션 허용 |
| **쿼리 반환 타입** | `Optional<Position>` | 단일 결과만 기대 |
| **결과** | 2개 반환 시 예외 | `NonUniqueResultException` |

### 코드 흐름

```
TradingDashboardController.dashboard()
    ↓
ProfitService.getProfitSummary()
    ↓
PositionRepository.findOpenPositionByMarket(market)
    ↓
JPA Query: Optional<Position> ← 2개 반환 시 예외!
```

### 설정 확인

**TradingProperties.java:**
```java
private int maxPositions = 2;  // 최대 2개의 동시 OPEN 포지션 허용
```

**TradingBotService.java:**
```java
if (openPositionCount < maxPositions) {
    executeBuy(market, signal);  // 포지션 2개까지 생성 가능
}
```

### 왜 배포 환경에서만 발생하는가?

개발 환경에서는 보통 단일 포지션으로 테스트하지만, 배포 환경에서는:
1. 다중 스케줄러 인스턴스 동시 실행
2. 외부 API 응답 지연으로 인한 타이밍 차이
3. 이전 배포에서 OPEN 포지션이 정상 종료되지 않은 상태

---

## 해결방법

### Step 1: PositionRepository 인터페이스 수정

**파일**: `src/main/java/.../trading/domain/position/PositionRepository.java`

```java
// 변경 전
Optional<Position> findOpenPositionByMarket(String market);

// 변경 후
List<Position> findOpenPositionsByMarket(String market);
```

### Step 2: PositionJpaRepository 수정

**파일**: `src/main/java/.../trading/infrastructure/persistence/repository/PositionJpaRepository.java`

```java
// 변경 전
@Query("SELECT p FROM PositionJpaEntity p WHERE p.market = :market AND p.status = 'OPEN'")
Optional<PositionJpaEntity> findOpenPositionByMarket(@Param("market") String market);

// 변경 후
@Query("SELECT p FROM PositionJpaEntity p WHERE p.market = :market AND p.status = 'OPEN' ORDER BY p.openedAt DESC")
List<PositionJpaEntity> findOpenPositionsByMarket(@Param("market") String market);
```

### Step 3: PositionRepositoryAdapter 수정

**파일**: `src/main/java/.../trading/infrastructure/persistence/adapter/PositionRepositoryAdapter.java`

```java
// 변경 전
@Override
public Optional<Position> findOpenPositionByMarket(String market) {
    return jpaRepository.findOpenPositionByMarket(market).map(this::toDomain);
}

// 변경 후
@Override
public List<Position> findOpenPositionsByMarket(String market) {
    return jpaRepository.findOpenPositionsByMarket(market).stream()
            .map(this::toDomain)
            .toList();
}
```

### Step 4: ProfitService 수정

**파일**: `src/main/java/.../trading/application/service/ProfitService.java`

```java
// 변경 전
Optional<Position> openPosition = positionRepository.findOpenPositionByMarket(market);
BigDecimal unrealizedPnl = BigDecimal.ZERO;
BigDecimal unrealizedPnlPct = BigDecimal.ZERO;

if (openPosition.isPresent() && currentPriceBD.compareTo(BigDecimal.ZERO) > 0) {
    unrealizedPnl = openPosition.get().calculateUnrealizedPnl(currentPriceBD);
    unrealizedPnlPct = openPosition.get().calculateUnrealizedPnlPct(currentPriceBD);
}

// 변경 후 (모든 OPEN 포지션의 미실현 손익 합산)
List<Position> openPositions = positionRepository.findOpenPositionsByMarket(market);
BigDecimal unrealizedPnl = BigDecimal.ZERO;
BigDecimal unrealizedPnlPct = BigDecimal.ZERO;

if (!openPositions.isEmpty() && currentPriceBD.compareTo(BigDecimal.ZERO) > 0) {
    for (Position pos : openPositions) {
        unrealizedPnl = unrealizedPnl.add(pos.calculateUnrealizedPnl(currentPriceBD));
        unrealizedPnlPct = unrealizedPnlPct.add(pos.calculateUnrealizedPnlPct(currentPriceBD));
    }
}
```

---

## 디버깅

### SQL 로그 확인

`application.yaml`에서 SQL 로그 활성화:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
```

### H2 Console로 데이터 확인

1. http://localhost:8080/h2-console 접속
2. JDBC URL: `jdbc:h2:file:./data/scheduledb`
3. 쿼리 실행:

```sql
-- OPEN 상태 포지션 개수 확인
SELECT market, COUNT(*) as count
FROM trading_positions
WHERE status = 'OPEN'
GROUP BY market;

-- 중복 OPEN 포지션 상세 확인
SELECT id, market, status, opened_at, entry_price, entry_volume
FROM trading_positions
WHERE market = 'KRW-ADA' AND status = 'OPEN'
ORDER BY opened_at DESC;
```

### 호출 위치 확인

`findOpenPositionByMarket`을 호출하는 모든 위치 검색:

```bash
grep -rn "findOpenPositionByMarket" src/
```

---

## 예방 체크리스트

배포 전 확인사항:

- [ ] `maxPositions > 1` 설정 시 관련 쿼리가 `List`를 반환하는가?
- [ ] 비즈니스 로직에서 다중 결과를 허용하면 Repository도 동일하게 설계했는가?
- [ ] 단일 결과를 기대하는 쿼리에 `LIMIT 1` 또는 `findFirst...` 사용했는가?
- [ ] 배포 전 다중 포지션 시나리오 테스트를 수행했는가?
- [ ] 동시성 이슈로 인한 중복 데이터 가능성을 검토했는가?

---

## 영향받는 파일

| 파일 | 수정 내용 |
|------|-----------|
| `trading/domain/position/PositionRepository.java` | 반환 타입 `Optional` → `List` |
| `trading/infrastructure/persistence/repository/PositionJpaRepository.java` | JPA 쿼리 반환 타입 변경 |
| `trading/infrastructure/persistence/adapter/PositionRepositoryAdapter.java` | 어댑터 구현 변경 |
| `trading/application/service/ProfitService.java` | 다중 포지션 처리 로직 |

---

## 관련 자료

- [Spring Data JPA - Query Methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Hibernate NonUniqueResultException](https://docs.jboss.org/hibernate/orm/current/javadocs/org/hibernate/NonUniqueResultException.html)
