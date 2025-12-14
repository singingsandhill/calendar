# Calendar

약속 잡기 웹 어플리케이션 - 그룹 일정 조율 서비스

## Tech Stack

### Backend
| Category | Technology | Version |
|----------|------------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 4.0.0 |
| ORM | Spring Data JPA | - |
| Persistence | Hibernate | - |
| Validation | Jakarta Bean Validation | - |
| Utility | Lombok | - |

### Database
| Category | Technology | Note |
|----------|------------|------|
| RDBMS | H2 Database | MySQL 호환 모드 |
| Mode | File-based | `./data/scheduledb` |

### Frontend
| Category | Technology | Note |
|----------|------------|------|
| Template Engine | Thymeleaf | Server-side rendering |
| JavaScript | Vanilla JS (ES6+) | No framework |
| Styling | CSS3 | Custom properties, Flexbox, Grid |

### Build & Test
| Category | Technology |
|----------|------------|
| Build Tool | Gradle |
| Testing | JUnit 5 |
| Mocking | Mockito |
| Assertions | AssertJ |

### Architecture
- **Pattern**: Hexagonal Architecture (Ports & Adapters)
- **Layers**: Presentation → Application → Domain ← Infrastructure

## Quick Start

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test
```

- Application: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (user: sa, no password)
