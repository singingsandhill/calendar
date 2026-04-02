# CLAUDE.md

Multi-domain Spring Boot 4.0.0 / Java 21 web application with four modules:

| Module | Package | Description |
|--------|---------|-------------|
| Schedule | `datedate` | Group scheduling - owners create schedules, participants mark availability |
| Runner | `runner` | Running crew (97 Runners) - attendance, rankings, admin dashboard |
| Trading | `trading` | Crypto trading bot - Bithumb, technical analysis, automated trading |
| Stock | `stock` | Korean stock Gap & Pullback bot - Korea Investment Securities API |

## Build Commands

```bash
./gradlew build                              # Full build with tests
./gradlew bootRun                            # Run application (http://localhost:8080)
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

## Database

- **Dev:** H2 file-based (`./data/scheduledb`), MySQL compatibility mode
- **Test:** H2 in-memory, create-drop DDL
- **Console:** http://localhost:8080/h2-console (user: sa, no password)

## Testing

JUnit 5 + Mockito. Tests mirror main source structure.
