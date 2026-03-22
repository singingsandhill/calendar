# 시스템 아키텍처

## 1. 시스템 개요

4개의 독립 도메인 모듈을 포함하는 멀티도메인 Spring Boot 웹 애플리케이션이다. 각 모듈은 헥사고날 아키텍처를 따르며, 독립적으로 동작한다.

### 기술 스택

| 구분 | 기술 | 버전/비고 |
|------|------|-----------|
| Framework | Spring Boot | 4.0.0 |
| Language | Java | 21 |
| Database | H2 | MySQL 호환 모드 (파일 기반) |
| Template Engine | Thymeleaf | Spring Security 통합 |
| 비동기 HTTP | Spring WebFlux (WebClient) | 외부 API 호출용 |
| 보안 | Spring Security | BCrypt, 폼 로그인 |
| ORM | Spring Data JPA | Hibernate, `open-in-view: false` |
| 인증 라이브러리 | java-jwt (Auth0) | 4.4.0 (빗썸 API용) |
| 빌드 | Gradle | Java 21 toolchain |
| 메일 | Spring Mail | Gmail SMTP |
| 기타 | Lombok | 컴파일 타임 코드 생성 |

### 모듈 요약

| 모듈 | 패키지 | 설명 | 기본 경로 |
|------|--------|------|-----------|
| **Schedule (datedate)** | `datedate` | 그룹 일정 관리, 참여자 투표 | `/api/`, `/owner/` |
| **Runner** | `runner` | 러닝 크루 출석/거리 추적, 랭킹 | `/runners/` |
| **Trading** | `trading` | 빗썸 암호화폐 자동매매봇 | `/trading/`, `/api/trading/` |
| **Stock** | `stock` | 한국투자증권 주식 갭&눌림목 자동매매봇 | `/stock/`, `/api/stock/` |

---

## 2. 아키텍처 원칙

### 헥사고날 아키텍처 (Ports & Adapters)

모든 모듈은 헥사고날 아키텍처를 따른다. 도메인 레이어가 중심에 위치하고, 외부 의존성은 인터페이스(Port)를 통해 격리된다.

```
                    ┌─────────────────────────┐
                    │      Presentation        │
                    │  (Controller, REST API)   │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │       Application        │
                    │   (Service, UseCase)      │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │         Domain           │
                    │ (Entity, Value Object,   │
                    │  Repository Interface)    │
                    └────────────┬────────────┘
                                 │ (Port)
                    ┌────────────▼────────────┐
                    │      Infrastructure      │
                    │  (JPA Adapter, API Client,│
                    │   Scheduler, Config)      │
                    └─────────────────────────┘
```

### 모듈 독립성

- 4개 도메인 모듈은 서로 직접 참조하지 않는다.
- 공유 코드는 `common` 모듈에만 존재한다 (예외 처리, 보안/JPA/웹 설정).
- 각 모듈은 자체 도메인 모델, 서비스, 인프라를 보유한다.

### 공통 패턴

- **트랜잭션**: 클래스 레벨 `@Transactional(readOnly = true)` 기본, 쓰기 메서드에서 `@Transactional`로 오버라이드
- **예외 계층**: 모든 비즈니스 예외는 `BusinessException`을 상속 (HttpStatus + 에러 코드 포함)
- **Repository 어댑터**: 도메인 Repository 인터페이스(Port) → Infrastructure JPA 어댑터(Adapter)
- **엔티티 매핑**: Domain Entity ↔ JPA Entity 간 변환 메서드 (`toJpa()`, `toDomain()`)

---

## 3. 모듈 구성

### 3.1 common 모듈

공유 인프라 컴포넌트를 제공한다.

```
common/
├── application/exception/
│   └── BusinessException.java          # 추상 기본 예외
├── application/service/
│   └── SitemapService.java             # 사이트맵 생성
├── infrastructure/config/
│   ├── JpaConfig.java                  # @EnableJpaAuditing
│   ├── WebConfig.java                  # 정적 리소스, CORS
│   └── SecurityConfig.java            # Spring Security 설정
└── presentation/
    ├── api/GlobalExceptionHandler.java # 전역 예외 핸들러
    ├── controller/StaticResourceController.java
    └── dto/
        ├── ErrorResponse.java          # {code, message}
        └── SeoMetadata.java            # SEO 메타데이터
```

### 3.2 datedate (Schedule) 모듈

그룹 일정 관리 서비스. 오너가 일정을 생성하고, 참여자가 가능 날짜를 마킹한다.

| 구분 | 내용 |
|------|------|
| 핵심 도메인 | Owner, Schedule, Participant, Location, Menu |
| 주요 서비스 | OwnerService, ScheduleService, ParticipantService, LocationService, MenuService, PopularityService |
| API 경로 | `GET/POST /api/owners/{ownerId}`, `GET/POST/PATCH/DELETE /api/owners/{ownerId}/schedules/{year}/{month}` |
| 비즈니스 규칙 | 참여자 최대 8명, ownerId 패턴 `^[a-z0-9-]+$` (2-20자) |

### 3.3 runner 모듈

러닝 크루(97 Runners) 관리 시스템. 출석, 거리 추적, 랭킹을 제공한다.

| 구분 | 내용 |
|------|------|
| 핵심 도메인 | Run, Attendance, Admin |
| 주요 서비스 | RunService, AttendanceService, RunnerAdminService |
| API 경로 | `POST /runners/runs/{runId}/attendance` |
| MVC 경로 | `/runners`, `/runners/runs`, `/runners/members`, `/runners/admin` |
| 보안 | 관리자 대시보드는 `ROLE_ADMIN` 필요 (폼 로그인) |

### 3.4 trading 모듈

빗썸 거래소 암호화폐 자동매매봇. 기술적 분석 기반 시그널 생성과 자동 매매를 수행한다.

| 구분 | 내용 |
|------|------|
| 핵심 도메인 | Candle, Trade, Position, Signal, AccountSnapshot, DailySummary |
| 주요 서비스 | TradingBotService, IndicatorService, SignalService, DivergenceService, RiskManagementService, RebalanceService |
| API 경로 | `GET/POST /api/trading/bot/*`, `GET /api/trading/candles`, `GET /api/trading/trades` |
| 매매 대상 | KRW-ADA (설정 가능) |
| 최대 포지션 | 2개 |

### 3.5 stock 모듈

한국투자증권 API 기반 주식 갭 & 눌림목 자동매매봇. 상태 머신 기반으로 종목을 추적하고 매매한다.

| 구분 | 내용 |
|------|------|
| 핵심 도메인 | Stock, StockPosition, StockTrade, StockSignal, StockCandle |
| 주요 서비스 | GapPullbackBotService, ScreeningService, PullbackDetectionService, StockPositionService, StockRiskService |
| API 경로 | `GET/POST /api/stock/bot/*`, `GET /api/stock/positions/*`, `GET /api/stock/trades` |
| 종목 풀 | 75개 (바이오, 2차전지, 반도체, 코스닥 등) |
| 최대 포지션 | 5개 |

---

## 4. 헥사고날 아키텍처 상세

### Domain 레이어

순수 비즈니스 로직만 포함한다. 프레임워크 의존성이 없다.

- **Entity**: 비즈니스 규칙을 캡슐화한 도메인 객체 (예: `Position.shouldStopLoss()`, `Schedule.canAddParticipant()`)
- **Value Object**: 불변 값 타입 (예: `YearMonth`, `ParticipantColor`)
- **Repository Interface (Port)**: 영속화 추상화 인터페이스
- **Enum**: 상태 머신, 분류 타입 (예: `StockState`, `SignalType`, `TradeStatus`)

### Application 레이어

유스케이스를 오케스트레이션한다. 도메인 객체와 Repository 포트를 조합하여 비즈니스 플로우를 구현한다.

- **Service**: 트랜잭션 경계 관리, 도메인 로직 조합
- **DTO**: 서비스 간 데이터 전달용 레코드 (예: `IndicatorResult`, `DivergenceResult`)
- **Exception**: `BusinessException`을 상속하는 도메인 특화 예외

```java
// 트랜잭션 패턴
@Service
@Transactional(readOnly = true)   // 클래스 레벨: 읽기 전용
public class SomeService {

    @Transactional                 // 쓰기 메서드: 오버라이드
    public void create(...) { ... }
}
```

### Infrastructure 레이어

외부 시스템과의 통합을 담당한다.

- **JPA Entity**: `@Entity` 어노테이션이 붙은 영속화 객체, Domain Entity와 변환 메서드 제공
- **Repository Adapter**: 도메인 Repository 인터페이스를 구현, JPA Repository에 위임
- **API Client**: 외부 거래소 API 호출 (KIS, 빗썸)
- **Scheduler**: `@Scheduled` 기반 주기적 작업 실행
- **Config**: `@ConfigurationProperties` 기반 설정 바인딩

### Presentation 레이어

사용자/클라이언트와의 인터페이스를 담당한다.

- **REST API Controller** (`@RestController`): JSON 응답, `/api/` 경로
- **MVC Controller** (`@Controller`): Thymeleaf 템플릿 렌더링
- **Request/Response DTO**: Record 기반, Validation 어노테이션 포함

---

## 5. 데이터 아키텍처

### DB 구성

| 환경 | 설정 | DDL 전략 |
|------|------|----------|
| 개발 | H2 파일 기반 (`./data/scheduledb`, MySQL 호환 모드) | `update` |
| 테스트 | H2 인메모리 (`jdbc:h2:mem:testdb`) | `create-drop` |

- H2 콘솔: `http://localhost:8081/h2-console` (user: sa)
- `open-in-view: false` (Lazy Loading 이슈 방지)

### 모듈별 테이블

#### datedate (Schedule)
| 테이블 | 설명 |
|--------|------|
| owner | 오너 정보 |
| schedule | 일정 (연/월 단위) |
| participant | 참여자 (이름, 색상, 선택 날짜) |
| location | 장소 후보 |
| location_vote | 장소 투표 |
| menu | 메뉴 후보 (URL 포함) |
| menu_vote | 메뉴 투표 |

#### runner
| 테이블 | 설명 |
|--------|------|
| run | 러닝 일정 (날짜, 시간, 장소, 카테고리) |
| attendance | 출석 기록 (참여자, 거리) |
| runner_admin | 관리자 계정 |

#### trading
| 테이블 | 설명 |
|--------|------|
| candle | OHLCV 캔들 데이터 |
| trade | 매매 주문 (BUY/SELL, MARKET/LIMIT) |
| position | 포지션 (진입/청산 가격, 손익) |
| signal | 기술적 분석 시그널 |
| account_snapshot | 계정 스냅샷 |
| daily_summary | 일별 요약 통계 |

#### stock
| 테이블 | 설명 |
|--------|------|
| stock | 감시 종목 (상태 머신) |
| stock_position | 포지션 (TP 단계별 실행 플래그) |
| stock_trade | 매매 주문 |
| stock_candle | OHLCV 캔들 (분/일/주) |
| stock_signal | 시그널 감사 추적 |

### 엔티티 매핑 전략

Domain Entity와 JPA Entity를 분리하여, 도메인 레이어의 프레임워크 독립성을 보장한다.

```
Domain Entity (POJO)  ←→  JPA Entity (@Entity)
       ↑                         ↑
  비즈니스 로직              영속화 메타데이터
  Repository Port         JPA Repository
```

각 JPA Entity는 `toDomain()` 메서드로 도메인 객체를 생성하고, 도메인 객체에서 JPA Entity로의 변환은 정적 팩토리 메서드나 어댑터에서 수행한다.

---

## 6. 외부 시스템 통합

### 6.1 한국투자증권 (KIS) API

주식 시세 조회 및 주문 실행을 위한 REST API 클라이언트.

**인증 방식**: OAuth2 토큰

| 구분 | 설정 |
|------|------|
| 토큰 저장 | `volatile` 필드 + `ReentrantLock` 동기화 |
| 토큰 갱신 | 만료 30분 전 선제적 갱신 |
| 갱신 재시도 | 최대 3회, 지수 백오프 (1s → 2s → 4s) |
| 4xx 응답 | 재시도 안 함 (429 제외) |

**API 호출 설정**:

| 구분 | 값 |
|------|-----|
| 타임아웃 | 10초 |
| 최대 재시도 | 3회 |
| 초기 백오프 | 1초 |
| 최대 백오프 | 4초 |
| 재시도 대상 | `ConnectException`, `SocketTimeoutException`, `IOException`, HTTP 429/500/502/503/504 |

**주요 API**:
- 시세: 현재가, 호가, 일별 시세, 체결강도
- 계정: 잔고, 매수가능금액, 미체결 주문
- 주문: 매수/매도 (시장가/지정가), 취소
- TR_ID 기반 거래 유형 구분

### 6.2 빗썸 API

암호화폐 시세 조회 및 주문 실행을 위한 REST API 클라이언트.

**인증 방식**: JWT + HMAC-SHA256

| 구분 | 설정 |
|------|------|
| 서명 생성 | `BithumbJwtGenerator` (요청별 JWT 토큰 생성) |
| HTTP 클라이언트 | WebClient (비동기) |
| 타임아웃 | 30초 |
| Rate Limit | 429 응답 시 재시도 |

**주요 API**:
- Public: 캔들, 호가, 체결 내역, 현재가
- Private: 잔고 조회, 주문 기회 조회
- Order: 지정가/시장가 매수·매도, 주문 취소

### 6.3 Gmail SMTP

주식봇 스크리닝 결과를 이메일로 발송한다.

| 구분 | 설정 |
|------|------|
| 호스트 | smtp.gmail.com:587 |
| 인증 | Gmail 앱 비밀번호 |
| TLS | STARTTLS 필수 |
| 타임아웃 | 연결/읽기/쓰기 각 5초 |

---

## 7. 보안 아키텍처

### Spring Security 설정

**공개 엔드포인트** (인증 불필요):

| 경로 패턴 | 대상 |
|-----------|------|
| `/`, `/start`, `/index.html` | 홈페이지 |
| `/api/**` | REST API 전체 |
| `/css/**`, `/js/**`, `/images/**` | 정적 리소스 |
| `/h2-console/**` | H2 콘솔 (개발용) |
| `/runners`, `/runners/runs/**`, `/runners/members/**` | 러너 공개 페이지 |
| `/stock/**`, `/api/stock/**` | 주식봇 |

**보호 엔드포인트**:

| 경로 패턴 | 필요 권한 |
|-----------|----------|
| `/runners/admin/**` | `ROLE_ADMIN` |
| 그 외 | `authenticated` |

### 인증 흐름

- **러너 관리자**: 폼 기반 로그인 (`/runners/admin/login`)
- **비밀번호 인코딩**: BCrypt
- **초기 관리자**: `RunnerAdminInitializer`가 애플리케이션 시작 시 환경변수 기반 관리자 계정 생성

### CSRF 설정

API 엔드포인트와 H2 콘솔은 CSRF를 비활성화한다:

- `/h2-console/**`
- `/api/**`
- `/runners/runs/*/attendance`
- `/runners/runs/create`
- `/runners/admin/attendance/*/delete`

### 시크릿 관리

모든 민감 정보는 환경변수로 관리한다 (`.env` 파일, Spring의 `optional:file:.env[.properties]` 임포트).

| 환경변수 | 용도 |
|---------|------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 데이터베이스 접속 |
| `RUNNER_ADMIN_USERNAME/PASSWORD` | 러너 관리자 계정 |
| `BITHUMB_ACCESS_KEY/SECRET_KEY` | 빗썸 API 인증 |
| `KIS_APP_KEY/APP_SECRET/ACCOUNT_NUMBER` | 한국투자증권 API 인증 |
| `STOCK_MAIL_USERNAME/PASSWORD/TO` | 이메일 발송 |

---

## 8. 스케줄링 아키텍처

각 트레이딩 모듈은 독립적인 스케줄러를 보유한다. `@EnableScheduling`은 모듈별 `SchedulerConfig`에서 활성화된다.

### Trading 모듈 스케줄러

| 스케줄 | 크론 표현식 | 메서드 | 설명 |
|--------|------------|--------|------|
| 매매 루프 | `5 * * * * *` (매분 5초) | `executeTradeLoop()` | 캔들 갱신 → 리스크 → 리밸런싱 → 시그널 → 매매 |
| 캔들 동기화 | `0 */5 * * * *` (5분 간격) | `syncCandles()` | 누락 캔들 보정 |
| 오래된 캔들 정리 | `0 0 0 * * *` (매일 자정) | `cleanupOldCandles()` | 7일 이상 캔들 삭제 |
| 계정 스냅샷 | `0 */5 * * * *` (5분 간격) | `saveAccountSnapshot()` | 자산 현황 기록 |
| 시간별 스냅샷 | `0 0 * * * *` (매시 정각) | `saveHourlySnapshot()` | 시간별 자산 기록 |
| 일별 요약 | `0 1 0 * * *` (매일 00:01) | `generateDailySummary()` | 전일 매매 통계 생성 |

### Stock 모듈 스케줄러

| 스케줄 | 크론/설정 | 메서드 | 설명 |
|--------|----------|--------|------|
| 프리마켓 | `0 30 8 * * MON-FRI` (08:30) | `executePreMarket()` | 전일 데이터 수집 |
| 스크리닝 | `0 5 9 * * MON-FRI` (09:05) | `executeScreening()` | 갭 종목 선별 (최대 10개) |
| 매매 루프 | `fixedRate: 5초` | `executeTradingLoop()` | 09:10~11:20 상태 업데이트 + 진입/청산 |
| 최종 청산 | `0 20 11 * * MON-FRI` (11:20) | `executeFinalExit()` | 보유 포지션 전량 청산 |

### Stock 일일 매매 타임라인

```
08:30          09:00        09:05        09:10                   11:20     11:30
  │              │            │            │                       │         │
  ▼              ▼            ▼            ▼                       ▼         ▼
프리마켓      장 시작      스크리닝     매매 루프 시작           최종 청산   장 마감
(데이터 수집)             (갭 종목     (5초 간격:                (전량
                          필터링)      상태 전이 + 진입/청산)     매도)
```

---

## 9. 트레이딩 봇 아키텍처 (Trading 모듈)

### 매매 루프 플로우

```
매 1분마다 실행:
  1. 캔들 갱신 (BithumbPublicApi → CandleService)
  2. 지표 계산 (MA5/20/60, RSI14, Stochastic%K/%D)
  3. 리스크 체크 (SL -8%, TP +15%, 트레일링)
  4. 리밸런싱 (시장 상태 기반 비율 조정)
  5. 시그널 생성 (점수 기반 BUY/SELL/HOLD)
  6. 매매 실행 (BithumbPrivateApi)
```

### 시그널 스코어링 시스템

시그널은 여러 지표의 점수를 합산하여 결정한다 (최대 ±155점).

| 지표 | 점수 범위 | 기준 |
|------|----------|------|
| MA 크로스 | ±25 | 골든/데스 크로스 |
| MA 추세 | ±15 | 가격 vs MA60 위치 |
| RSI 다이버전스 | ±20 | 가격-RSI 괴리 |
| RSI 레벨 | ±15 | 과매도/과매수 |
| 스토캐스틱 다이버전스 | ±15 | 가격-Stoch 괴리 |
| 스토캐스틱 레벨 | ±15 | 과매도/과매수 |
| 거래량 다이버전스 | ±20 | 가격-거래량 괴리 |
| RSI 추세 | ±10 | RSI 방향성 |

**매매 결정**:
- **매수**: 점수 ≥ 40 AND RSI < 70 AND StochK < 85
- **매도**: 점수 ≤ -40 AND RSI > 30 AND StochK > 15
- **홀드**: 그 외

### 리스크 관리

| 규칙 | 조건 | 동작 |
|------|------|------|
| 손절 (Stop-Loss) | 손익률 ≤ -8% | 전량 매도 |
| 익절 (Take-Profit) | 손익률 ≥ +15% | 전량 매도 |
| 트레일링 스탑 | 최고점 대비 -3% 하락 | 전량 매도 |
| 트레일링 활성화 | 수익률 +10% 도달 시 활성화 | 추적 시작 |

- 수수료율: 0.25%
- 슬리피지 버퍼: 0.5%
- 최소 수익 임계값: 0.6% (이하일 때 매도 스킵)

### 리밸런싱 전략

시장 상태(MA60 대비 가격 위치)에 따라 코인/현금 비율을 동적 조정한다.

| 시장 상태 | 코인 비율 | 현금 비율 |
|----------|----------|----------|
| 강세 (Bullish) | 70% | 30% |
| 기본 (Neutral) | 50% | 50% |
| 약세 (Bearish) | 30% | 70% |

- 트리거: 목표 비율 대비 10% 이상 이탈
- 쿨다운: 4시간
- 최소 주문 금액: 5,000 KRW

---

## 10. 주식봇 아키텍처 (Stock 모듈)

### 상태 머신

각 감시 종목은 `StockState` 상태 머신을 따라 전이한다.

```
WATCHING ──────► HIGH_FORMED ──────► PULLBACK ──────► ENTRY_READY ──────► ENTERED ──────► EXITED
(감시중)         (고점형성)          (눌림목)         (진입대기)          (보유중)        (청산완료)
   │               │                                      │
   │               └──► FILTERED_OUT (제외: 고점 대비 3% 초과 하락)
   │                         ▲
   └─────────────────────────┘
```

| 상태 | 설명 | 전이 조건 |
|------|------|----------|
| `WATCHING` | 갭 스크리닝 통과, 감시 시작 | 갭 2~7%, 시가총액/거래대금 충족 |
| `HIGH_FORMED` | 고점 형성 확인 | 가격 ≥ 시가 × 1.015 |
| `PULLBACK` | 눌림목 발생 | 고점 대비 1.5~3.0% 하락 |
| `ENTRY_READY` | 반등 확인, 진입 대기 | 눌림목 저점 대비 +0.3% 반등, 체결강도 ≥ 105 |
| `ENTERED` | 포지션 보유 중 | 매수 주문 체결 |
| `EXITED` | 포지션 청산 완료 | 익절/손절/시간청산 |
| `FILTERED_OUT` | 감시 제외 | 고점 대비 3.0% 초과 하락 |

**헬퍼 메서드**:
- `isActive()`: WATCHING, HIGH_FORMED, PULLBACK, ENTRY_READY
- `canEnter()`: ENTRY_READY만
- `isHolding()`: ENTERED만
- `isTerminal()`: EXITED, FILTERED_OUT

### 시그널 타입

| 분류 | 시그널 | 설명 |
|------|--------|------|
| 진입 | `GAP_DETECTED` | 갭 감지 |
| 진입 | `HIGH_FORMED` | 고점 형성 |
| 진입 | `PULLBACK_ENTRY` | 눌림목 진입 |
| 청산 | `TP1_EXIT` | 1차 익절 |
| 청산 | `TP2_EXIT` | 2차 익절 |
| 청산 | `TP3_EXIT` | 3차 익절 |
| 청산 | `STOP_LOSS_EXIT` | 손절 |
| 청산 | `TRAILING_EXIT` | 트레일링 청산 |
| 청산 | `TIME_EXIT` | 시간 청산 |
| 청산 | `MANUAL_EXIT` | 수동 청산 |
| 청산 | `EMERGENCY_EXIT` | 긴급 청산 |
| 기타 | `FILTERED_OUT` | 필터 아웃 |

### 다단계 익절 전략

포지션 진입 후, 가격 상승 구간별 분할 매도를 수행한다.

| 단계 | 조건 | 매도 비율 | 설명 |
|------|------|----------|------|
| TP1 | 가격 ≥ 진입가 × 1.015 (+1.5%) | 50% | 원금 회수 |
| TP2 | 가격 ≥ 당일 고점 | 잔량의 60% | 추가 수익 확보 |
| TP3 | 가격 ≥ 당일 고점 × 1.01 | 잔량 전부 | 최대 수익 실현 |
| 트레일링 | 가격 ≤ 트레일링 고점 × 0.992 | 잔량 전부 | 이익 보호 |
| 손절 | 가격 ≤ 진입가 × 0.985 (-1.5%) | 전량 | 손실 제한 |
| 시간 청산 | 11:20 KST | 전량 | 당일 청산 원칙 |

### 스크리닝 필터

| 단계 | 필터 | 기준 |
|------|------|------|
| 1 | 갭 비율 | 1.0% ~ 10.0% |
| 2 | 시가총액 | ≥ 1,000억 원 |
| 3 | 거래대금 | ≥ 5억 원 |
| 4 | 체결강도 | ≥ 110 |
| 5 | 스프레드 | ≤ 0.3% |
| 최종 | 감시 목록 | 최대 10개 |

---

## 11. 에러 처리 및 복원력

### BusinessException 계층 구조

```
BusinessException (abstract)
├── RunNotFoundException
├── AttendanceNotFoundException
├── DuplicateAttendanceException
├── ScheduleNotFoundException
├── ParticipantNotFoundException
├── DuplicateScheduleException
├── LocationNotFoundException
├── MenuNotFoundException
├── StockNotFoundException
├── PositionNotFoundException
├── InsufficientBalanceException
├── KisApiException
├── TradingHoursException
└── ...
```

### GlobalExceptionHandler

`@RestControllerAdvice`로 모든 예외를 일관된 `ErrorResponse`로 변환한다.

| 예외 타입 | HTTP 상태 | 응답 |
|----------|-----------|------|
| `BusinessException` | 예외에서 정의 (`getStatus()`) | `ErrorResponse(code, message)` |
| `MethodArgumentNotValidException` | 400 Bad Request | 유효성 검증 에러 |
| `Exception` | 500 Internal Server Error | 일반 에러 |

### API 클라이언트 재시도 전략

| 클라이언트 | 재시도 | 백오프 | 대상 |
|-----------|--------|--------|------|
| KIS REST | 3회 | 지수 (1s → 2s → 4s, jitter 포함) | 네트워크 오류, HTTP 429/5xx |
| 빗썸 | WebClient 재시도 | 429 Rate Limit 대응 | Rate Limit |

### Graceful Degradation

- 트레이딩 봇은 API 호출 실패 시 해당 사이클을 스킵하고 다음 사이클에서 재시도한다.
- MA60 데이터 부족 시 리밸런싱을 스킵한다 (`skip-when-data-insufficient: true`).
- 봇 상태 제어: `start` / `stop` / `pause` / `resume` API를 통해 안전하게 중단/재개할 수 있다.

---

## 12. 배포 및 운영

### 로컬 개발 환경

WSL2에서 Windows JDK를 사용하여 빌드·실행한다.

```bash
# 빌드
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat build"

# 실행
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat bootRun"

# 테스트
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"

# Java 프로세스 종료 (H2 DB 잠금 해제)
cmd.exe /c "taskkill /F /IM java.exe"
```

### 로깅 설정

| 구분 | 설정 |
|------|------|
| 로그 파일 | `logs/stock-trading.log` |
| 애플리케이션 로그 레벨 | `DEBUG` (`me.singingsandhill.calendar`) |
| Hibernate SQL | `DEBUG` (`org.hibernate.SQL`) |

### H2 콘솔

개발용 DB 모니터링 도구.

| 구분 | 값 |
|------|-----|
| 경로 | `http://localhost:8081/h2-console` |
| JDBC URL | `jdbc:h2:file:./data/scheduledb;DB_CLOSE_DELAY=-1;MODE=MySQL` |
| 사용자 | `sa` |
| 비밀번호 | (없음) |

### 환경변수 목록

| 환경변수 | 필수 | 설명 |
|---------|------|------|
| `DB_URL` | O | H2 JDBC URL |
| `DB_USERNAME` | O | DB 사용자명 |
| `DB_PASSWORD` | O | DB 비밀번호 |
| `RUNNER_ADMIN_USERNAME` | - | 러너 관리자 ID (기본: admin) |
| `RUNNER_ADMIN_PASSWORD` | - | 러너 관리자 PW (기본: admin123) |
| `BITHUMB_ACCESS_KEY` | - | 빗썸 API 키 |
| `BITHUMB_SECRET_KEY` | - | 빗썸 API 시크릿 |
| `TRADING_BOT_ENABLED` | - | 트레이딩 봇 활성화 (기본: false) |
| `KIS_APP_KEY` | - | 한국투자증권 앱 키 |
| `KIS_APP_SECRET` | - | 한국투자증권 앱 시크릿 |
| `KIS_ACCOUNT_NUMBER` | - | 한국투자증권 계좌번호 |
| `STOCK_BOT_ENABLED` | - | 주식봇 활성화 (기본: false) |
| `STOCK_MAIL_ENABLED` | - | 이메일 알림 활성화 (기본: false) |
| `STOCK_MAIL_USERNAME` | - | Gmail 주소 |
| `STOCK_MAIL_PASSWORD` | - | Gmail 앱 비밀번호 |
| `STOCK_MAIL_TO` | - | 수신자 이메일 |

### 서버 설정

| 구분 | 값 |
|------|-----|
| 포트 | 8081 |
| 응답 압축 | 활성화 (1024B 이상, text/html, CSS, JS, JSON) |
| 정적 리소스 캐시 | 604,800초 (7일) |
