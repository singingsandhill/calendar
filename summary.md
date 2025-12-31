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

#### 3-1. Coin Trading Bot (코인 자동매매)

빗썸 거래소 연동 암호화폐 자동매매 봇입니다.

- **경로**: `/trading/`, `/api/trading/`
- **주요 엔티티**: Candle, Trade, Position, Signal

**매매 기준 (7가지 기술 지표 복합 점수):**

| 지표 | 매수 점수 | 매도 점수 |
|------|-----------|-----------|
| MA 교차 | 골든크로스 +30 | 데드크로스 -30 |
| MA 추세 | 가격 > MA60 +10 | 가격 < MA60 -10 |
| RSI 다이버전스 | 상승 +25 | 하락 -25 |
| RSI 레벨 | 과매도(<30) +10 | 과매수(>70) -10 |
| Stochastic 다이버전스 | 상승 +20 | 하락 -20 |
| Stochastic 레벨 | 과매도(<20) +10 | 과매수(>80) -10 |
| 거래량 다이버전스 | 상승 +15 | 하락 -15 |

- **매수 조건**: 점수 >= 30 AND 상승 다이버전스 AND 가격 > MA60
- **매도 조건**: 점수 <= -30 AND 하락 다이버전스 AND 가격 < MA60

**리스크 관리:**
- 손절: -10%
- 익절: +20%
- 동적 리밸런싱: 상승장 70%, 하락장 30%, 중립 50% 코인 비중

**주문 금액:**
- 주문 금액: 가용 KRW 잔고의 20% (분할 매수)
- 최소 주문: 5,000 KRW
- 최대 포지션: 1개 (마켓당)

**봇 시작 방법:**
```bash
# REST API
curl -X POST http://localhost:8080/api/trading/bot/start
curl -X POST http://localhost:8080/api/trading/bot/stop
curl -X POST http://localhost:8080/api/trading/bot/pause
curl -X POST http://localhost:8080/api/trading/bot/resume
curl -X POST http://localhost:8080/api/trading/bot/emergency-close  # 전량 청산
```
- 대시보드: http://localhost:8080/trading

**작동 주기:**
| 작업 | 주기 | 설명 |
|------|------|------|
| 메인 트레이딩 루프 | 1분 (매분 :05초) | 캔들 업데이트, 리스크 체크, 시그널 생성, 매매 실행 |
| 캔들 동기화 | 5분 | 누락된 캔들 데이터 보충 |
| 계정 스냅샷 | 5분 | 잔고 상태 기록 |
| 일일 정리 | 00:00 | 7일 이상 된 캔들 삭제 |
| 일일 요약 | 00:01 | 전일 거래 통계 생성 (승률, 손익) |

**환경 변수:**
```
TRADING_API_KEY=빗썸_API_키
TRADING_SECRET_KEY=빗썸_시크릿_키
```

---

#### 3-2. Stock Trading Bot (주식 자동매매)

한국투자증권 API 연동 국내 주식 자동매매 봇입니다. **Gap & Pullback (갭상승 후 눌림목)** 전략을 사용합니다.

- **경로**: `/stock/`, `/api/stock/`
- **주요 엔티티**: Stock, StockPosition, StockTrade, StockSignal

**스크리닝 기준:**
| 조건 | 값 |
|------|------|
| 갭상승률 | 1% ~ 10% |
| 시가총액 | >= 1,000억 원 |
| 거래대금 | >= 5억 원 |
| 체결강도 | >= 110 |
| 스프레드 | <= 0.3% |

**진입 조건 (4단계 상태 머신):**
1. **WATCHING**: 갭상승 종목 감시 중
2. **HIGH_FORMED**: 시가 대비 +1.5% 이상 고점 형성
3. **PULLBACK**: 고점 대비 -1.0% ~ -5.0% 눌림 발생
4. **ENTRY_READY**: 눌림 저점에서 +0.2% 반등 + 체결강도 >= 105 + 매수잔량비 > 1.2

**청산 조건:**
| 조건 | 청산 비율 | 설명 |
|------|-----------|------|
| TP1 | 50% | 진입가 +5% 도달 |
| TP2 | 잔량의 60% | 당일 고가 도달 |
| TP3 | 전량 | 당일 고가 +10% 도달 |
| 손절 | 100% | 진입가 -5% |
| 트레일링 스탑 | 100% | TP1 이후, 고점 대비 -3.8% |
| 시간 청산 | 100% | 15:20 KST |

**주문 금액:**
- 주문 금액: 가용 현금의 10%
- 최대 주문 금액: 5,000,000 KRW
- 최대 포지션: 5개 (동시 보유)

**봇 시작 방법:**
```bash
# REST API
curl -X POST http://localhost:8080/api/stock/bot/start
curl -X POST http://localhost:8080/api/stock/bot/stop
curl -X POST http://localhost:8080/api/stock/bot/pause
curl -X POST http://localhost:8080/api/stock/bot/resume
curl -X POST http://localhost:8080/api/stock/bot/emergency-close  # 전량 청산
```
- 대시보드: http://localhost:8080/stock

**작동 주기 (월-금, KST 기준):**
| 시간 | 작업 | 설명 |
|------|------|------|
| 08:30 | 프리마켓 | 전일 종가 데이터 수집 |
| 09:00 | 스크리닝 | 갭상승 종목 선별 (최대 10개) |
| 09:10-11:20 | 트레이딩 루프 | 5초마다 폴링, 상태 갱신, 매매 실행 |
| 11:20 | 최종 청산 | 모든 포지션 강제 청산 |

**환경 변수:**
```
KIS_APP_KEY=한국투자증권_앱키
KIS_APP_SECRET=한국투자증권_앱시크릿
KIS_ACCOUNT_NUMBER=계좌번호
```

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
| http://localhost:8080/trading | 코인 트레이딩 대시보드 |
| http://localhost:8080/stock | 주식 트레이딩 대시보드 |
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
