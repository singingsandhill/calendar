# Repository Guidelines

## Project Structure & Module Organization

This Java 21/Spring Boot 4 monolith is organized under `src/main/java/me/singingsandhill/calendar/` by domain: `common`, `datedate`, `runner`, `trading`, and `stock`. Domains use a hexagonal layout: `domain` holds business rules and ports, `application` contains services, `infrastructure` implements persistence and integrations, and `presentation` contains controllers and DTOs. Templates and assets live in `src/main/resources/{templates,static}`. Tests mirror production packages in `src/test/java`. Decisions belong in `docs/adr`; deployment assets live in `deploy/`.

## Build, Test, and Development Commands

- `./gradlew bootRun` starts the application at `http://localhost:8081`.
- `./gradlew test` runs the complete JUnit Platform test suite.
- `./gradlew test --tests "*ServiceTest"` runs a focused class-name pattern.
- `./gradlew build` compiles, tests, and creates the executable JAR in `build/libs/`.
- `npx tailwindcss@3.4 -c tailwind.trading.config.js -i tailwind.trading.input.css -o src/main/resources/static/css/trading-tw.css --minify` regenerates committed bot-dashboard CSS after utility-class changes.

Copy `.env.example` to `.env` for local configuration; never commit real credentials.

## Coding Style & Naming Conventions

Follow existing Java formatting: tabs for indentation, same-line braces, and lowercase packages. Use `PascalCase` for types, `camelCase` for methods and fields, and suffixes such as `Service`, `Controller`, `Repository`, and `Test`. Keep business rules in domain objects, preserve layer dependency direction, and reuse established exception, transaction, security, and Thymeleaf-fragment patterns. Avoid broad formatting or unrelated refactors. No Java formatter or linter is configured.

## Testing Guidelines

Tests use JUnit 5, Spring Boot Test, Mockito, AssertJ, Spring Security Test, and MockWebServer. Name tests `*Test.java` and mirror the production package. Add regression tests for fixes, confirming failure first when practical. Run the narrowest relevant test, then `./gradlew build`. CI also runs ShellCheck on deployment scripts.

## Commit & Pull Request Guidelines

Recent history follows Conventional Commit-style subjects such as `fix(trading): ...`, `feat(datedate): ...`, and `docs(stock): ...`. Keep commits scoped to one coherent change and explain operational or testing implications in the body. PRs should summarize behavior, list verification commands, link relevant issues or ADRs, and include screenshots for template or CSS changes. Decision changes require a new ADR or a superseding status update; keep `CLAUDE.md` synchronized with current architectural facts.

## Security & Operations

Treat authentication rules and trading/stock order paths as high risk. Preserve PAPER-safe defaults, ordering guards, and concurrency controls. Do not commit `.env`, API keys, database files, or logs, and do not deploy without following `docs/operations/deployment.md`.
