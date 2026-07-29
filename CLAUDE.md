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

## 작업 원칙 (중요)

위 ADR 동기화 규칙과 같은 강제 규칙이다. 아래 원칙이 프로젝트 규칙과 충돌하면 우선순위 표를 따른다.

### 우선순위

| 순위 | 내용 |
|---|---|
| 1 | 보안·데이터 보호·운영 안전 — 실주문·인증 경계(§Security, §Stock Bot). 실제 자금이 움직이는 코드다 |
| 2 | 사용자가 이번 작업에서 명시한 요구사항 |
| 3 | 저장소의 아키텍처·코딩 규칙 — 헥사고날 계층, 2-레이어 예외, ADR 작성, `@Transactional(readOnly)` 패턴 |
| 4 | 아래 일반 행동 원칙 |
| 5 | 개인적 구현 선호 — 기존 스타일보다 우선하지 않는다 |

**4번은 3번을 덮어쓰지 않는다.** 다음은 과거 감사에서 *더 단순한 버전이 위험하다* 고 판정돼 층을 쌓은
것들이다 — 장황해 보여도 단순화 대상이 아니다: 2-레이어 예외 처리, `requestMatchers` 선언 순서,
비멱등 주문 무재시도, 서킷브레이커, PAPER 기본 모드, 주문 선영속화, 동시성 3-레이어, 도메인 메서드에
두는 비즈니스 규칙.

### 구현 전 확인

- 코드로 확인할 수 있는 것은 사용자에게 묻지 않는다 — 호출 경로·데이터 흐름·기존 테스트를 먼저 읽는다.
- **문서를 사실의 근거로 인용하지 않는다(이 파일 포함).** 심볼·경로·수치는 코드에서 확인한다 —
  문서가 존재하지 않는 심볼을 지목한 전례가 있다 (`docs/guides/git-commit.md` Commit 114).
- 임계값·계수는 세 곳이 어긋날 수 있다: `application.yaml` 운영값 / `*Properties` Java 기본값 /
  ADR 의 근거. 하나만 보고 판단하지 않는다.
- 확인하지 못한 것은 단정하지 않는다. 중요한 가정은 구현 전에 밝힌다.
- 해석이 갈리고 결과가 달라지면 차이를 설명한다. 저장소를 조사한 뒤에도 남는 모호성만 질문한다.
- 요구사항·기존 코드에서 모순을 발견하면 조용히 한쪽을 고르지 않고 알린다.
- 더 단순하거나 안전한 접근이 보이면 구현 전에 제안한다.
- 오탈자·명백한 한 줄 변경에는 이 절차를 강요하지 않는다 (`docs/adr/README.md` 의 "비결정성 변경" 문턱과 같은 기준).

### 단순성 우선

- 요청을 충족하는 가장 작은 변경을 고른다. 요청하지 않은 기능을 미리 만들지 않는다.
- 한 번만 쓰이는 코드에 추상화 계층을 만들지 않는다. 필요성이 입증되지 않은 설정 키·확장점을 늘리지 않는다.
- **이미 있는 패턴을 재사용한다** — `BusinessException` 상속, JPA `AttributeConverter`,
  주문 경로의 `TransactionTemplate`, `StockCodeLocks`, Thymeleaf fragment.
- 실제 발생이 확인되지 않은 상황에 방어 코드를 넣지 않는다.
- 구현이 지나치게 길어지면 더 작은 해법이 있는지 다시 검토한다.
- 단순화는 검증·보안 통제·예외 처리를 빼는 것이 아니다 — 우선순위 3번이 요구하는 층은 유지한다.

### 최소 범위 수정

- 모든 변경 줄은 요청, 또는 그 변경이 직접 유발한 정리에 연결돼야 한다.
- 무관한 코드·주석·이름·포맷을 함께 손보지 않는다. 요청하지 않은 리팩터링을 하지 않는다.
- 기존 스타일이 합리적이면 취향보다 기존 스타일을 따른다.
- 무관한 문제나 미사용 코드는 삭제하지 말고 결과 보고에 따로 적는다 — 전례: `stock/domain/candle` 은
  비활성 스캐폴딩으로 남기고 문서화만 했다.
- 이번 변경으로 새롭게 미사용이 된 import·변수·함수만 정리한다.
- 광범위 자동 포맷으로 diff 를 늘리지 않는다. 커밋된 산출물 `trading-tw.css` 는 §Build Commands 의
  조건(유틸리티 클래스 변경)에 해당할 때만 재생성한다.

### 목표 기반 실행·검증

- 지시를 검증 가능한 목표로 바꾼다. 버그 수정은 실패를 재현하는 테스트를 먼저 확보한다 —
  커밋 로그의 "RED 선확인 → GREEN" 관행.
- 기능 추가는 정상 동작과 주요 실패 조건의 확인 기준을 세운다. 리팩터링은 외부 동작이 같음을 확인한다.
- 여러 단계 작업은 단계마다 검증 방법을 포함한 짧은 계획을 세운다.
- **돌릴 수 있으면 실제로 돌린다** — §Build Commands 의 `./gradlew test`, 좁히려면 `--tests`.
  이 저장소에는 린터·정적분석·CI 가 없다. 검증 수단은 테스트와 실행뿐이다.
- 필터로 좁혀 돌렸으면 실제 실행된 테스트 수를 확인한다 (`build/test-results/test/*.xml`).
- 돌리지 못했으면 돌린 것처럼 쓰지 않는다. 이유와 남은 위험을 적는다.
- 검증이 실패하면 임시 우회로 결과를 감추지 않고 원인을 조사한다.
- 완료 전에 위 ADR 동기화 표의 "결정 변경" 해당 여부를 스스로 점검한다 — 그 표는 확인을 리뷰어에게
  맡기지만, 리뷰 전에 에이전트가 먼저 본다.

### 커밋·되돌리기 어려운 작업

- **커밋·푸시를 직접 실행하지 않는다.** 변경 후 `docs/guides/git-commit.md`(gitignore 대상 — 작성용 스크래치)에
  규약에 맞는 섹션을 덧붙인다.
- 형식 규약은 `docs/guides/git-commit.md` 헤더 — cmd.exe 호환(큰따옴표만), `git add` 한 줄, 같은 파일이
  여러 커밋에 걸리면 처음 등장하는 커밋이 소유, 배치의 마지막 커밋이 로그 파일 자신을 포함.
- 외부로 나가거나 되돌리기 어려운 작업은 실행 전에 확인받는다.

## Build Commands

```bash
./gradlew build                              # Full build with tests
./gradlew bootRun                            # Run application (http://localhost:8081)
./gradlew test                               # Run all tests
./gradlew test --tests "*ServiceTest"       # Run pattern-matched tests
```

트레이딩·주식 봇 페이지 CSS: `static/css/trading-tw.css` 는 커밋된 Tailwind 빌드 산출물.
`templates/trading/`, `templates/stock/` 또는 `static/js/trading-*.js` 의 유틸리티 클래스 변경 시 재생성:

```bash
npx tailwindcss@3.4 -c tailwind.trading.config.js -i tailwind.trading.input.css -o src/main/resources/static/css/trading-tw.css --minify
```

(CDN `tailwindcss@3.4/dist/tailwind.min.css` 는 존재하지 않는 경로(404)라 로컬 빌드로 대체 — `trading/fragments/header.html` 주석 참고. 다크 터미널 테마는 `static/css/trading.css` 의 `body.tr-dark` 스코프 — trading·stock 페이지 공유, stock 은 CSS 변수 오버라이드 방식.)

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
- `WebConfig` — 인터셉터 3개(`LocaleChangeInterceptor` `?lang=ko`/`?lang=en`, 잘못된 lang 값은
  무시(`ignoreInvalidLocale=true` — 봇 스캔 500 방지),
  `cacheControlInterceptor`, `ownerPathInterceptor`) + `ShallowEtagHeaderFilter` +
  `contentLanguageFilter`(`Content-Language` 헤더) + `messageSource()` 빈.
  캐시 정책: `/runners/admin` 은 no-cache, SEO 페이지는 `public, max-age`
- `CookieThenAcceptLanguageLocaleResolver` — 로케일 해석: cookie `lang` → Accept-Language 헤더 → Korean(ko) 기본값; 쿠키 1년 유지, SameSite=Lax
- `CorsConfig` — `/api/**` 한정 `CorsConfigurationSource` (무자격증명)
- `AdsenseConfig` / `AdsenseProperties` — AdSense 슬롯 활성화 설정
- `IndexNowConfig` / `IndexNowProperties` — IndexNow 색인 제출 설정
- `KakaoOAuth2ClientConfig` — 카카오 OAuth2 클라이언트 등록
- `JpaConfig` — JPA 기본 설정

**infrastructure/scheduler/**
- `IndexNowScheduler` — `indexnow.enabled=true` 일 때만 빈 등록, 매일 03:30 KST 제출

**application/**
- `BusinessException` — abstract base; 서브클래스가 `getStatus()` (HttpStatus), `getCode()` (String) 구현
- `SitemapService` — `/sitemap.xml` 동적 생성
- `IndexNowService` — `SitemapService` URL 들을 `api.indexnow.org` 에 POST (전 구간 fail-soft)
- `SitemapEntry` — 사이트맵 항목 DTO

**presentation/**
- `GlobalExceptionHandler` (`@RestControllerAdvice`) — REST/JSON 오류 응답 → `ErrorResponse { code, message }`
- `MvcExceptionHandler` (`@ControllerAdvice`) — Thymeleaf 에러 페이지 라우팅
- `StaticResourceController` — sitemap.xml 등 SEO 정적 파일 서빙
- `AdsenseModelAdvice` (`@ControllerAdvice`) — 뷰 모델에 AdSense 활성 플래그 주입
- `SeoMetadata` — title, description, OG tags, JSON-LD, robots, canonical DTO
- `LocaleLinks` — hreflang/로케일 토글 링크 계산
- `ErrorResponse` — `{ code, message }` JSON 구조

## i18n

Korean (`ko`, 기본값) / English (`en`) 2개 언어 지원.

- 로케일 해석 순서: cookie `lang` → Accept-Language 헤더 → Korean fallback
- 전환: `?lang=en` 또는 `?lang=ko` URL에 추가 → 이후 쿠키에 저장
- 메시지 파일: `src/main/resources/messages.properties` (한국어, 기본), `messages_en.properties` (영어)
- `MessageSource`: `ReloadableResourceBundleMessageSource`, UTF-8, 시스템 로케일 폴백 없음
- `MessageFormat` 의 number 인자는 `{n,number,#}` 패턴으로 천단위 그룹화 차단 (예: year=2026 이 "2,026" 출력 방지). 회귀 테스트 `SeoServiceI18nTest.scheduleSeo_yearNotGrouped` 가드.
- **작은따옴표 이스케이프:** `alwaysUseMessageFormat` 은 어디에서도 설정하지 않으므로 Spring 기본값
  `false` 가 적용된다(`WebConfig.messageSource()` 는 `setBasename`/`setDefaultEncoding`/
  `setFallbackToSystemLocale(false)` 만 호출) → 인자 없는 메시지는 MessageFormat 미적용. 따옴표 `''` 이스케이프는 인자 있는 메시지(`{0}` 포함, 예: `dashboard.title`)에서만 사용. 인자 없는 일반 콘텐츠(use-case·SEO·홈 본문 등)는 `'` 1개를 그대로 쓴다 — 인자 없는 메시지에 `''` 를 쓰면 화면에 `''` 가 노출됨.

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

아래는 `SecurityConfig` 의 **선언 순서**대로다 (첫 매칭 우선이라 순서가 곧 의미다).

| # | Path Pattern | Access |
|---|--------------|--------|
| 1 | `/api/trading/**`, `/trading`, `/trading/**` | `ROLE_ADMIN` (봇 제어·실주문·제어 대시보드, [ADR 0003](docs/adr/common/security/0003-admin-only-trading-control-api.md)) |
| 2 | `POST /api/stock/bot/**` | `ROLE_ADMIN` (주식 봇 제어, [ADR 0005](docs/adr/common/security/0005-admin-only-stock-bot-control-api.md)) — `GET .../status` 는 아래 `/api/**` 로 떨어져 공개 |
| 3 | `/recap/share/**` | permitAll (공유 링크 무인증 — 반드시 `/recap/**` 보다 먼저) |
| 4 | `/login`, `/oauth2/**`, `/login/oauth2/**` | permitAll |
| 5 | `/me`, `/recap`, `/recap/**`, `/api/me/**` | `ROLE_USER` (카카오 로그인, [ADR 0004](docs/adr/common/security/0004-kakao-oauth2-login.md)) |
| 6 | `/`, `/start`, `/index.html`, `/privacy-policy`, `/about`, `/api/**`, static assets, `/h2-console/**` | permitAll |
| 7 | `/runners`, `/runners/announce`, `/runners/runs(/**)`, `/runners/members(/**)`, `/runners/{css,js,images}/**`, **`/runners/admin/login`** | permitAll |
| 8 | `/insights(/**)`, `/use-cases(/**)`, `/tools(/**)` | permitAll |
| 9 | `/runners/admin`, `/runners/admin/**` | `ROLE_ADMIN` |
| 10 | `/stock`, `/stock/**`, `/api/stock/**` | permitAll |
| 11 | `/*`, `/*/*/*` | permitAll (owner 페이지 등 동적 경로 — `/**` 전역 와일드카드는 **없다**) |
| 12 | 그 외 | `authenticated()` |

> 순서상 두 가지가 핵심이다. ① 트레이딩·주식 ADMIN 규칙(1·2)은 포괄 `permitAll`(`/api/**`, `/*`)보다
> **먼저** 와야 보호가 유지된다. ② `/runners/admin/login`(7)은 `/runners/admin/**` ADMIN(9)보다
> **먼저** 선언돼야 한다 — 순서가 뒤집히면 로그인 페이지 자체가 ADMIN 을 요구해 로그인이 불가능해진다.
> 러너 공개 경로도 `/runners/**` 와일드카드가 아니라 개별 매처로 나열된 이유가 이것이다.

CSRF: `/h2-console/**`, `/api/**`, runner admin 변경 엔드포인트는 비활성화.  
로그인/로그아웃 2계열: 폼 로그인 `/runners/admin/login`, `POST /runners/admin/logout` → `/runners` /
카카오 `POST /logout` → `/` (`OrRequestMatcher` 로 두 경로를 받고 URI 접두사로 리다이렉트 분기).
미인증 진입점도 분리 — 어드민 5경로(`/runners/admin/**`, `/trading`, `/trading/**`,
`/api/trading/**`, `/api/stock/bot/**`)는 어드민 로그인, 나머지는 `AnyRequestMatcher` 로 카카오 `/login`.

CORS: `/api/**` 는 앱인토스 미니앱(다른 origin)에서 호출 가능하도록 허용 (`CorsConfig`, 무자격증명).
결정 근거: [ADR 0002](docs/adr/common/security/0002-cors-for-apps-in-toss-miniapp.md). 트레이딩 API 는 `ROLE_ADMIN`(세션 인증) + 무자격증명 CORS 조합으로 교차출처 호출이 차단됨.

## DateDate Module (추가 기능)

- `InsightsService` + `InsightsController` → `/insights/trends.html` (집계 인기 통계)
- `UseCaseController` → `/use-cases/detail.html` (콘텐츠 마케팅 페이지: 친구 모임, 팀 회의, 여행 계획, 스터디 그룹, 동호회). 슬러그 단일 진실원 `UseCaseSlugs.ALL` (라우팅·사이트맵·푸터 자동 반영). 슬러그별 워크드 예시(`sample.*`)로 템플릿 차별화.
- `SeoService` — 페이지별 JSON-LD 스키마 포함 SEO 메타데이터 생성
- `PopularityService` — 시간 가중 점수 기반 장소/메뉴 인기 순위 (노출 기준: 최소 2표 + 비속어 블록리스트 — [ADR 0006](docs/adr/datedate/domain/0006-popularity-exposure-criteria.md))
- 카카오 로그인 (선택적): `KakaoOAuth2UserService` → `AppUser` upsert, 오너 연결(first-claim), `UserActivity` 이벤트 기록
- `RecapService` + `RecapController` → `GET /recap` (당해년 리다이렉트), `/recap/{year}` 연간 리캡,
  `POST /recap/{year}/share` 공유 토큰 발급, `/recap/share/{token}` 공개 공유 (ADR datedate/domain/0005)
- `AuthController` → `GET /login` → `auth/login` (카카오 로그인 랜딩)
- `MyPageController` → `GET /me` → `me/mypage` (로그인 사용자 내 기록)

## Background Schedulers

**Trading** (`TradingSchedulerConfig` → `@EnableScheduling`):

| Scheduler | 주기 |
|-----------|------|
| `CandleScheduler` | 매분 :05초 트레이딩 루프; 5분마다 캔들 동기화; 자정 캔들 정리 |
| `DailySummaryScheduler` | 5분마다 계좌 스냅샷; 00:01 일일 P&L 요약 |

`trading.bot.enabled=false`이면 트레이딩 루프·캔들 동기화·계좌 스냅샷·일일 요약이 스킵된다.
단 **`CandleScheduler.cleanupOldCandles()`(자정 캔들 정리)는 가드가 없어 항상 실행**된다.

**Stock** (`StockSchedulerConfig` → `@EnableScheduling`):

| Scheduler | 시간 (KST, 평일만) |
|-----------|-------------------|
| `StockTradingScheduler` | 08:30 프리마켓; 09:20 갭 스크리닝; 09:20~11:20 5초마다 트레이딩 루프; 11:20 최종 청산; 11:40 일일 실적 리포트 |

`stock.bot.enabled=false`이면 모든 잡 스킵 (5개 메서드 전부 `isEnabled()` 가드 — 트레이딩 모듈과 다름).
공휴일은 `stock.trading.holidays` (yyyy-MM-dd 리스트) 에서 관리. 자동화 미구현 — 매년 갱신.

**Common**:

| Scheduler | 주기 |
|-----------|------|
| `IndexNowScheduler` | 매일 03:30 KST IndexNow 제출 — `indexnow.enabled=true` 일 때만 빈 등록 |

## External Integrations

**Spring WebFlux (Reactor)** — Trading 모듈:
- `WebClientConfig` — Netty WebClient: 연결 10초, 읽기/쓰기 30초 타임아웃 + 커넥션 풀
  idle 폐기(maxIdleTime 10s / maxLifeTime 5m / evictInBackground 30s — stale 커넥션
  `PrematureCloseException` 방지, [ADR trading/infrastructure/0004](docs/adr/trading/infrastructure/0004-reactor-netty-connection-pool-policy.md)).
  이 빌더 빈은 싱글턴이라 **stock 모듈 KIS 클라이언트도 같은 HttpClient·풀 공유**
- `BithumbApiClient` — `BithumbPublicApi` + `BithumbPrivateApi` 래핑; `BithumbJwtGenerator`로 JWT 인증

**Spring Mail (Gmail SMTP)** — Stock 모듈:
- `StockMailService` — 09:20 스크리닝 후 HTML 결과 메일 발송
- `stock.mail.enabled`, `stock.mail.to` 프로퍼티로 제어

## Stock Bot — 운영 모드와 동시성

- **`Bot.Mode {LIVE, PAPER, BACKTEST}`** — `KoreaInvestmentApiClient` 의 모든 주문
  진입부에 모드 가드. PAPER/BACKTEST 는 `simulateOrder()` 인메모리 체결.
  **시세·호가·잔고 조회는 모드 무관 항상 실 API** — 스크리닝·상태머신·리스크 루프·손익 기록은
  PAPER 에서도 전부 동작한다(주문만 가상). `BACKTEST` 는 현재 PAPER 와 동일 — 히스토리
  fixture 시세 소스 미구현.
  **기본값 PAPER** — LIVE 는 `STOCK_BOT_MODE=LIVE` 환경변수로만 활성화
  ([ADR stock/modes/0002](docs/adr/stock/modes/0002-paper-default-mode.md)).
- **주문 무재시도** — 주문 POST(`/order-cash`)는 비멱등이라 재시도 없이 1회만 전송
  (`executePostNoRetry`, [ADR stock/infrastructure/0005](docs/adr/stock/infrastructure/0005-non-idempotent-order-no-retry.md));
  조회(GET) 재시도는 유지.
- **`Clock` 빈 (Asia/Seoul)** — `LocalTime.now(clock)` 사용 → `Clock.fixed` 로 시간 의존
  코드 결정성 테스트.
- **동시성 3-레이어:**
  1. `KisRestClient` 의 `Semaphore(8, fair)` — KIS HTTP 동시 호출 제한.
  2. `StockCodeLocks` — 종목별 `ReentrantLock` 으로 매수/매도 race 차단.
  3. `StockSchedulerConfig` 의 `ThreadPoolTaskScheduler(pool=4)` — 스크리닝과 트레이딩
     루프 병렬.
- **관측성:** `TradeEvents` (`stock.trade` 카테고리) 로 거래 이벤트 한 줄 로깅,
  `logback-spring.xml` 의 KST 자정 회전 + `stock-events.log` / `stock-sql.log` /
  `crypto-trading.log` (trading 모듈 전용, `stock-trading.log` 오염 방지) 분리,
  `BotStatus` 에 `lastTradingTickAt` / `lastScreeningResult` / `apiCallsLast5min` 노출.
  스크리닝 결과 메일 첨부는 발송 시점(09:20)까지의 로그 스냅샷
  (`stock-screening-snapshot-YYYY-MM-DD.log`) — 하루 전체 로그가 아님.

상세 결정 근거: [ADR stock/](docs/adr/stock/).

## Template Structure

`src/main/resources/templates/` 하위 Thymeleaf 템플릿:

| 디렉토리 | 내용 |
|---------|------|
| `fragments/` | `head.html` (SEO/meta), `header.html` (nav), `footer.html`, `scripts.html` (JS), `ad-slot.html` (AdSense 슬롯), `gtm-noscript.html` (GTM noscript 폴백), `login-banner.html` (카카오 로그인 배너), `create-schedule-modal.html` (공유 일정 생성 모달) |
| `schedule/` | `view.html` (참가자 일정 뷰), `create.html` |
| `owner/` | `dashboard.html` — 오너 대시보드 |
| `auth/` | `login.html` — 카카오 로그인 랜딩 |
| `me/` | `mypage.html` — 로그인 사용자 내 기록 |
| `recap/` | `recap.html` (연간 리캡), `share.html` (공개 공유) |
| `runners/` | 홈, 런 목록/상세/폼, 멤버 목록/상세, `announce.html` |
| `runners/admin/` | 어드민 대시보드, 로그인, 런 폼, `attendance-form.html` |
| `runners/fragments/` | `header.html`, `footer.html` (러너 전용 — 루트 `fragments/` 와 별개) |
| `trading/` | `dashboard.html`, `trades.html`, `settings.html`, `portfolio.html`, `verify.html` |
| `trading/fragments/` | `header.html` |
| `stock/` | `dashboard.html`, `history.html`, `settings.html` |
| `stock/fragments/` | `header.html`, `formats.html` |
| `tools/` | `date-diff.html` — 날짜 계산기 |
| `error/` | `4xx.html`, `5xx.html` |
| `insights/` | `trends.html` |
| `use-cases/` | `detail.html` (슬러그 기반 콘텐츠 페이지) |
| (루트) | `index.html`, `guide.html`, `privacy.html`, `terms.html`, `about.html`, `faq.html` |

## Database

- **Dev:** H2 file-based (`./data/scheduledb`), MySQL compatibility mode
- **Test:** H2 in-memory, create-drop DDL
- **Console:** http://localhost:8081/h2-console (user: sa, no password)

## Testing

JUnit 5 + Mockito. Tests mirror main source structure.
