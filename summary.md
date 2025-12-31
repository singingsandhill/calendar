# 프로젝트 요약

## 개요

Spring Boot 4.0.0과 Java 21로 구축된 멀티 도메인 웹 애플리케이션입니다. 세 개의 독립적인 도메인 모듈을 포함하고 있습니다.

## 도메인 모듈

### 1. Schedule (약속 잡기)
그룹 일정 조율 서비스입니다.

- **기능**: 오너가 일정을 생성하고, 참가자들이 가능한 날짜를 표시
- **경로**: `/api/`, `/owner/`
- **주요 엔티티**: Owner, Schedule, Participant

### 2. Runner (97 Runners)
러닝 크루 출석 관리 시스템입니다.

- **기능**: 런 일정 관리, 출석 기록, 멤버 랭킹
- **경로**: `/runners/`
- **주요 엔티티**: Run, Attendance, Admin
- **특징**:
  - 정규런/번개런 카테고리 구분
  - 출석 횟수 및 총 거리 기반 랭킹
  - 관리자 대시보드 (Spring Security 인증)

### 3. Trading (자동매매 봇)
빗썸 거래소 연동 암호화폐 자동매매 봇입니다.

- **기능**: 기술적 분석 기반 자동 매매, 리스크 관리, 포트폴리오 리밸런싱
- **경로**: `/trading/`, `/api/trading/`
- **주요 엔티티**: Candle, Trade, Position, Signal
- **기술 지표**: MA (5/20/60), RSI, Stochastic
- **특징**:
  - 다이버전스 감지 (RSI, Stochastic, Volume)
  - 손절/익절 자동 실행
  - 실시간 대시보드

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.0.0 |
| 빌드 | Gradle |
| 데이터베이스 | H2 (개발), MySQL 호환 모드 |
| 템플릿 | Thymeleaf |
| 보안 | Spring Security |
| 테스트 | JUnit 5, Mockito |

## 아키텍처

**헥사고날 아키텍처** (Ports & Adapters) 패턴을 따릅니다.

```
각 모듈/
├── domain/           # 순수 도메인 모델, 리포지토리 인터페이스 (포트)
├── application/      # 비즈니스 로직 서비스
├── infrastructure/   # JPA 엔티티, 리포지토리 어댑터, 외부 API
└── presentation/     # 컨트롤러 (MVC + REST), DTO
```

## 프로젝트 구조

```
src/main/java/me/singingsandhill/calendar/
├── common/        # 공통 컴포넌트 (예외, 설정, 유틸리티)
├── domain/        # Schedule 모듈 도메인
├── application/   # Schedule 모듈 서비스
├── infrastructure/# Schedule 모듈 인프라
├── presentation/  # Schedule 모듈 프레젠테이션
├── runner/        # Runner 모듈 (독립 헥사고날)
└── trading/       # Trading 모듈 (독립 헥사고날)
```

## 실행 방법

### 기본 실행
```bash
./gradlew bootRun
```
애플리케이션은 http://localhost:8080 에서 실행됩니다.

### WSL 환경
```bash
cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat bootRun"
```

### 빌드 및 테스트
```bash
./gradlew build   # 전체 빌드
./gradlew test    # 테스트 실행
```

## 주요 URL

| URL | 설명 |
|-----|------|
| http://localhost:8080 | 메인 페이지 |
| http://localhost:8080/runners | 러너 크루 홈 |
| http://localhost:8080/runners/admin | 러너 관리자 대시보드 |
| http://localhost:8080/trading | 트레이딩 대시보드 |
| http://localhost:8080/h2-console | H2 데이터베이스 콘솔 |

## 환경 설정

Trading 모듈 사용 시 환경 변수 설정 필요:
```
TRADING_API_KEY=빗썸_API_키
TRADING_SECRET_KEY=빗썸_시크릿_키
```

Runner 관리자 기본 계정:
- ID: `admin`
- PW: `admin123`
