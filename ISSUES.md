# Project Issues

프로젝트 작업 내역을 Git issue 단위로 정리한 문서입니다.

## Summary

| # | Issue | Labels | Status |
|---|-------|--------|--------|
| 1 | Spring Boot 프로젝트 초기 설정 | setup, infrastructure | Done |
| 2 | Hexagonal Architecture 패키지 구조 | architecture, setup | Done |
| 3 | Owner 도메인 CRUD | feature, domain | Done |
| 4 | Schedule 도메인 CRUD | feature, domain | Done |
| 5 | Participant 도메인 및 선택 기능 | feature, domain | Done |
| 6 | GlobalExceptionHandler | feature, api | Done |
| 7 | Thymeleaf 웹 UI | feature, frontend | Done |
| 8 | 단위 테스트 | test | Done |
| 9 | CLAUDE.md 문서화 | documentation | Done |

---

## Issue Details

### 1. [Setup] Spring Boot 프로젝트 초기 설정

**Labels:** `setup`, `infrastructure`

Spring Boot 4.0.0 + Java 21 프로젝트 초기 설정

- Gradle 빌드 설정 (build.gradle, settings.gradle)
- Spring Boot 의존성 (Web, JPA, Thymeleaf, Validation)
- H2 Database 설정 (파일 기반, MySQL 호환 모드)
- application.yaml 설정
- .gitignore 설정 (Java, Gradle, IntelliJ, H2)

---

### 2. [Architecture] Hexagonal Architecture 패키지 구조 설정

**Labels:** `architecture`, `setup`

Hexagonal Architecture (Ports & Adapters) 기반 패키지 구조

- presentation/ - 컨트롤러, DTO
- application/ - 서비스, 비즈니스 예외
- domain/ - 도메인 모델, 리포지토리 인터페이스
- infrastructure/ - JPA 엔티티, 리포지토리 어댑터, 설정

---

### 3. [Feature] Owner 도메인 모델 및 CRUD 구현

**Labels:** `feature`, `domain`

일정 소유자(Owner) 도메인 구현

#### 도메인
- Owner 엔티티 (ownerId 검증: 2-20자, 소문자/숫자/하이픈)
- OwnerRepository 인터페이스

#### 인프라
- OwnerJpaEntity, OwnerJpaRepository
- OwnerRepositoryAdapter

#### 애플리케이션
- OwnerService (getOrCreateOwner, getOwner, getOwnerSchedules)
- OwnerNotFoundException, InvalidOwnerIdException

#### 프레젠테이션
- OwnerApiController (REST API)
- OwnerCreateRequest, OwnerResponse DTO

---

### 4. [Feature] Schedule 도메인 모델 및 CRUD 구현

**Labels:** `feature`, `domain`

월별 일정(Schedule) 도메인 구현

#### 도메인
- Schedule 엔티티 (ownerId, year, month, weeks)
- YearMonth 값 객체 (년도 2024-2100, 월 1-12 검증)
- ScheduleRepository 인터페이스

#### 인프라
- ScheduleJpaEntity (unique: owner_id + year + month)
- ScheduleJpaRepository, ScheduleRepositoryAdapter

#### 애플리케이션
- ScheduleService (CRUD by ownerId/year/month)
- ScheduleNotFoundException, DuplicateScheduleException

#### 프레젠테이션
- ScheduleApiController (REST API)
- ScheduleCreateRequest, ScheduleUpdateRequest
- ScheduleResponse, ScheduleDetailResponse DTO

---

### 5. [Feature] Participant 도메인 모델 및 선택 기능 구현

**Labels:** `feature`, `domain`

일정 참가자(Participant) 도메인 구현

#### 도메인
- Participant 엔티티 (name, color, selections)
- ParticipantColor 값 객체 (8색 프리셋, hex 검증)
- ParticipantRepository 인터페이스
- 제약: 스케줄당 최대 8명

#### 인프라
- ParticipantJpaEntity
- SelectionConverter (List<Integer> ↔ JSON)
- ParticipantJpaRepository, ParticipantRepositoryAdapter

#### 애플리케이션
- ParticipantService (addParticipant, updateSelections, delete)
- ParticipantNotFoundException, DuplicateParticipantException
- ParticipantLimitExceededException, InvalidSelectionException

#### 프레젠테이션
- ParticipantApiController (REST API)
- ParticipantCreateRequest, SelectionUpdateRequest
- ParticipantResponse DTO

---

### 6. [Feature] GlobalExceptionHandler 구현

**Labels:** `feature`, `api`

REST API 전역 예외 처리

- BusinessException 기반 커스텀 예외 처리
- Validation 에러 처리 (MethodArgumentNotValidException)
- IllegalArgumentException 처리
- 예상치 못한 예외 처리 (500 Internal Error)
- ErrorResponse DTO (code, message)

---

### 7. [Feature] Thymeleaf 기반 웹 UI 구현

**Labels:** `feature`, `frontend`

Thymeleaf 템플릿 기반 웹 인터페이스

#### 템플릿
- index.html - 랜딩 페이지 (Owner ID 입력)
- owner/dashboard.html - 스케줄 목록, 생성/삭제
- schedule/view.html - 캘린더 뷰, 참가자 선택
- fragments/ - header, footer 공통 프래그먼트

#### MVC 컨트롤러
- HomeController (/, /start)
- OwnerController (/{ownerId})
- ScheduleController (/{ownerId}/{year}/{month})

#### 정적 리소스
- css/style.css - 스타일시트
- js/api.js - REST API 클라이언트
- js/calendar.js - 캘린더 유틸리티

---

### 8. [Test] 단위 테스트 작성

**Labels:** `test`

JUnit 5 + Mockito 기반 단위 테스트

#### 도메인 테스트
- OwnerTest, ScheduleTest, ParticipantTest
- YearMonthTest, ParticipantColorTest

#### 서비스 테스트
- OwnerServiceTest
- ScheduleServiceTest
- ParticipantServiceTest

#### API 테스트
- ScheduleApiControllerTest

---

### 9. [Docs] CLAUDE.md 문서 작성

**Labels:** `documentation`

Claude Code를 위한 프로젝트 문서화

#### 루트 문서
- CLAUDE.md - 프로젝트 개요, 빌드 명령어, 아키텍처

#### 패키지별 문서
- application/CLAUDE.md - 서비스, 예외
- domain/CLAUDE.md - 도메인 모델, 리포지토리
- infrastructure/CLAUDE.md - JPA, 어댑터
- presentation/CLAUDE.md - 컨트롤러, DTO, 엔드포인트

#### 리소스 문서
- static/css/CLAUDE.md - CSS 변수, 컴포넌트
- static/js/CLAUDE.md - API 클라이언트, 유틸리티
- templates/fragments/CLAUDE.md - 공통 프래그먼트
- templates/owner/CLAUDE.md - 대시보드 템플릿
- templates/schedule/CLAUDE.md - 캘린더 뷰 템플릿
