# CLAUDE.md

Multi-domain Spring Boot 4.0.0 / Java 21 web application with five modules:

| Module | Package | Description |
|--------|---------|-------------|
| Common | `common` | Cross-cutting: config, i18n, exception handling, SEO/sitemap, security |
| Schedule | `datedate` | Group scheduling - owners create schedules, participants mark availability |
| Runner | `runner` | Running crew (97 Runners) - attendance, rankings, admin dashboard |
| Trading | `trading` | Crypto trading bot - Bithumb, technical analysis, automated trading |
| Stock | `stock` | Korean stock Gap & Pullback bot - Korea Investment Securities API |

도메인별 / 관심사별 아키텍처 결정 기록은 [`docs/adr/README.md`](docs/adr/README.md) 참고.

## CLAUDE.md / ADR 동기화 규칙 (중요)

이 파일과 모듈별 `CLAUDE.md` 들은 *현재 코드의 사실* 만 담는다. 결정의 *왜* 는 ADR 에
있다. 두 종류의 변경에는 다음 규칙을 강제한다.

| 변경 유형 | CLAUDE.md | ADR |
|---|---|---|
| 단순 사실 변경 (포트/경로/매직넘버 수치) | 수정 필수 | 불필요 |
| 결정 변경 (정책/계수 임계/구조 전환) | 수정 필수 | **새 ADR 작성 또는 기존 ADR 의 Status 를 Superseded 로 갱신** |
| 새 모듈/도메인 추가 | 표·진입점 추가 | 새 폴더 + ADR 0001 작성 |
| 결정 무효화 (기능 제거) | 해당 항목 삭제 | 기존 ADR 을 `Deprecated` 로 표시 (삭제 X) |

ADR 누락 사고 방지: PR 에서 `CLAUDE.md` 가 수정됐는데 `docs/adr/` 가 그대로면, 위 표의
"결정 변경" 인지 리뷰어가 확인.

## Build Commands

```bash
./gradlew build                              # Full build with tests
./gradlew bootRun                            # Run application (http://localhost:8081)
./gradlew test                               # Run all tests
./gradlew test --tests "*ServiceTest"       # Run pattern-matched tests
```

### WSL Environment

```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat build"
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat bootRun"
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"
cmd.exe /c "taskkill /F /IM java.exe"       # Kill Java (H2 lock release)
```

### Jetson Nano / Linux (OpenClaw 컨테이너)

```bash
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.5+11
export PATH=$JAVA_HOME/bin:$PATH

./gradlew bootRun --no-daemon --project-cache-dir /tmp/gradle-cache-calendar
./gradlew build --no-daemon --project-cache-dir /tmp/gradle-cache-calendar
```

> Java 21 (Temurin): `/usr/lib/jvm/jdk-21.0.5+11`  
> `.env` 파일 위치: `/home/gim/calendar/.env` (H2 file DB, dummy API keys 설정됨)

## Architecture

Hexagonal Architecture (Ports & Adapters). Each module has `domain/` (entities, repository interfaces as ports), `application/` (services), `infrastructure/` (JPA adapters, external APIs, config), `presentation/` (controllers, DTOs).

## Common Module

`common/` 패키지 — 도메인 없음, 순수 인프라/공통 관심사.

**infrastructure/config/**
- `SecurityConfig` — Spring Security 경로별 접근 규칙
- `WebConfig` — `LocaleChangeInterceptor` (`?lang=ko`/`?lang=en`) 등록, ETag 필터
- `CookieThenAcceptLanguageLocaleResolver` — 로케일 해석: cookie `lang` → Accept-Language 헤더 → Korean(ko) 기본값; 쿠키 1년 유지, SameSite=Lax
- `JpaConfig` — JPA 기본 설정

**application/**
- `BusinessException` — abstract base; 서브클래스가 `getStatus()` (HttpStatus), `getCode()` (String) 구현
- `SitemapService` — `/sitemap.xml` 동적 생성
- `SitemapEntry` — 사이트맵 항목 DTO

**presentation/**
- `GlobalExceptionHandler` (`@RestControllerAdvice`) — REST/JSON 오류 응답 → `ErrorResponse { code, message }`
- `MvcExceptionHandler` (`@ControllerAdvice`) — Thymeleaf 에러 페이지 라우팅
- `StaticResourceController` — sitemap.xml 등 SEO 정적 파일 서빙
- `SeoMetadata` — title, description, OG tags, JSON-LD, robots, canonical DTO
- `ErrorResponse` — `{ code, message }` JSON 구조

## i18n

Korean (`ko`, 기본값) / English (`en`) 2개 언어 지원.

- 로케일 해석 순서: cookie `lang` → Accept-Language 헤더 → Korean fallback
- 전환: `?lang=en` 또는 `?lang=ko` URL에 추가 → 이후 쿠키에 저장
- 메시지 파일: `src/main/resources/messages.properties` (한국어, 기본), `messages_en.properties` (영어)
- `MessageSource`: `ReloadableResourceBundleMessageSource`, UTF-8, 시스템 로케일 폴백 없음
- `MessageFormat` 의 number 인자는 `{n,number,#}` 패턴으로 천단위 그룹화 차단 (예: year=2026 이 "2,026" 출력 방지). 회귀 테스트 `SeoServiceI18nTest.scheduleSeo_yearNotGrouped` 가드.

## Exception Handling (Two-Layer)

**Layer 1 — REST APIs** (`GlobalExceptionHandler`):

| Exception | HTTP Status |
|-----------|-------------|
| `BusinessException` | `exception.getStatus()` |
| `MethodArgumentNotValidException` | 400 |
| `IllegalArgumentException` | 400 |
| `MethodArgumentTypeMismatchException` | 400 |
| `NoResourceFoundException` | 404 |
| `Exception` | 500 |

**Layer 2 — MVC views** (`MvcExceptionHandler`):

| Exception | View |
|-----------|------|
| `BusinessException` 4xx | `error/4xx` |
| `BusinessException` 5xx | `error/5xx` |
| `Exception` | `error/5xx` |

새 모듈 예외는 반드시 `BusinessException`을 상속하고 `HttpStatus`와 에러코드 문자열을 반환해야 함.

## Security

`SecurityConfig` — Runner 어드민 전용 폼 로그인.

| Path Pattern | Access |
|--------------|--------|
| `/runners/admin/**` | `ROLE_ADMIN` |
| `/runners/admin/login` | permitAll |
| `/runners/**`, `/insights/**`, `/stock/**`, `/api/**`, `/h2-console/**`, static assets, `/**` | permitAll |

CSRF: `/h2-console/**`, `/api/**`, runner admin 변경 엔드포인트는 비활성화.  
로그인 URL: `/runners/admin/login` → 로그아웃 후 `/runners` 리다이렉트.

## DateDate Module (추가 기능)

- `InsightsService` + `InsightsController` → `/insights/trends.html` (집계 인기 통계)
- `UseCaseController` → `/use-cases/detail.html` (콘텐츠 마케팅 페이지: 친구 모임, 팀 회의, 여행 계획, 스터디 그룹)
- `SeoService` — 페이지별 JSON-LD 스키마 포함 SEO 메타데이터 생성
- `PopularityService` — 시간 가중 점수 기반 장소/메뉴 인기 순위

## Background Schedulers

**Trading** (`TradingSchedulerConfig` → `@EnableScheduling`):

| Scheduler | 주기 |
|-----------|------|
| `CandleScheduler` | 매분 :05초 트레이딩 루프; 5분마다 캔들 동기화; 자정 캔들 정리 |
| `DailySummaryScheduler` | 5분마다 계좌 스냅샷; 00:01 일일 P&L 요약 |

`trading.bot.enabled=false`이면 모든 잡 스킵.

**Stock** (`StockSchedulerConfig` → `@EnableScheduling`):

| Scheduler | 시간 (KST, 평일만) |
|-----------|-------------------|
| `StockTradingScheduler` | 08:30 프리마켓; 09:20 갭 스크리닝; 09:20~11:20 5초마다 트레이딩 루프; 11:20 최종 청산 |

`stock.bot.enabled=false`이면 모든 잡 스킵. 공휴일은 `stock.trading.holidays` (yyyy-MM-dd 리스트) 에서 관리. 자동화 미구현 — 매년 갱신.

## External Integrations

**Spring WebFlux (Reactor)** — Trading 모듈:
- `WebClientConfig` — Netty WebClient: 연결 10초, 읽기/쓰기 30초 타임아웃
- `BithumbApiClient` — `BithumbPublicApi` + `BithumbPrivateApi` 래핑; `BithumbJwtGenerator`로 JWT 인증

**Spring Mail (Gmail SMTP)** — Stock 모듈:
- `StockMailService` — 09:20 스크리닝 후 HTML 결과 메일 발송
- `stock.mail.enabled`, `stock.mail.to` 프로퍼티로 제어

## Stock Bot — 운영 모드와 동시성

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — `KoreaInvestmentApiClient` 의 모든 주문
  진입부에 모드 가드. PAPER/BACKTEST 는 `simulateOrder()` 인메모리 체결.
- **`Clock` 빈 (Asia/Seoul)** — `LocalTime.now(clock)` 사용 → `Clock.fixed` 로 시간 의존
  코드 결정성 테스트.
- **동시성 3-레이어:**
  1. `KisRestClient` 의 `Semaphore(8, fair)` — KIS HTTP 동시 호출 제한.
  2. `StockCodeLocks` — 종목별 `ReentrantLock` 으로 매수/매도 race 차단.
  3. `StockSchedulerConfig` 의 `ThreadPoolTaskScheduler(pool=4)` — 스크리닝과 트레이딩
     루프 병렬.
- **관측성:** `TradeEvents` (`stock.trade` 카테고리) 로 거래 이벤트 한 줄 로깅,
  `logback-spring.xml` 의 KST 자정 회전 + `stock-events.log` / `stock-sql.log` 분리,
  `BotStatus` 에 `lastTradingTickAt` / `lastScreeningResult` / `apiCallsLast5min` 노출.

상세 결정 근거: [ADR stock/](docs/adr/stock/).

## Template Structure

`src/main/resources/templates/` 하위 Thymeleaf 템플릿:

| 디렉토리 | 내용 |
|---------|------|
| `fragments/` | `head.html` (SEO/meta), `header.html` (nav), `footer.html` (footer), `scripts.html` (JS), `ad-slot.html` (AdSense 슬롯) |
| `schedule/` | `view.html` — 참가자 일정 뷰 |
| `owner/` | `dashboard.html` — 오너 대시보드 |
| `runners/` | 홈, 런 목록/상세/폼, 멤버 목록/상세 |
| `runners/admin/` | 어드민 대시보드, 로그인, 런 폼 |
| `trading/` | `dashboard.html`, `trades.html`, `settings.html` |
| `stock/` | `dashboard.html`, `history.html`, `settings.html` |
| `error/` | `4xx.html`, `5xx.html` |
| `insights/` | `trends.html` |
| `use-cases/` | `detail.html` (슬러그 기반 콘텐츠 페이지) |
| (루트) | `index.html`, `guide.html`, `privacy.html`, `terms.html` |

## Database

- **Dev:** H2 file-based (`./data/scheduledb`), MySQL compatibility mode
- **Test:** H2 in-memory, create-drop DDL
- **Console:** http://localhost:8081/h2-console (user: sa, no password)

## Testing

JUnit 5 + Mockito. Tests mirror main source structure.
