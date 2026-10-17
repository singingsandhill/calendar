# Troubleshooting Guide

This directory contains solutions to common issues encountered during development.

## Index

| Issue | Description |
|-------|-------------|
| [Spring Boot 4.0 Migration](spring-boot-4-migration.md) | Package changes, annotation moves |
| [Thymeleaf + Java Records](thymeleaf-javascript-records.md) | Inline JavaScript serialization issues |
| [Google Search Console Redirect](google-search-console-redirect.md) | GSC 색인 오류, 리디렉션 체인 문제 |
| [JPA NonUniqueResultException](jpa-non-unique-result-exception.md) | 다중 결과 쿼리에서 Optional 사용 시 예외 |
| [Nginx Configuration](nginx-configuration.md) | http2 deprecated, server_name 충돌, 리디렉션 설정 |
| [Lighthouse Performance Audit](lighthouse-performance-audit.md) | datedate.site 폰트/manifest 최적화 — Pretendard 동적 서브셋, Noto 로케일 분기, manifest i18n |
| [Spring Security @WebMvcTest](spring-security-webmvctest.md) | 보안 슬라이스 테스트 — 컨텍스트 로딩 실패(전이 의존성), @WithMockUser 미적용 → post-processor |
| [Thymeleaf SpEL 정수 나눗셈](thymeleaf-spel-integer-division.md) | 템플릿에서 계산한 평균이 항상 0.0 — long/long 절삭, formatDecimal 이 위장, th:if 가 예외 은폐 |
| [배포 CI/CD 구축](deploy-cicd.md) | 파이프라인 3단 구조 해설 + 구축 중 부딪힌 14건 — shellcheck directive 파싱, GCP authorized_keys 덮어쓰기, GHCR classic PAT, ephemeral IP |

## Quick Reference

### Build Errors
- `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`
  - See: [Spring Boot 4.0 Migration](spring-boot-4-migration.md#webmvctest-import-오류)

### Test Errors
- `@WebMvcTest` 컨텍스트 로딩 실패: `NoSuchBeanDefinitionException: OwnerRepository`
  - See: [Spring Security @WebMvcTest](spring-security-webmvctest.md#증상-1--컨텍스트-로딩-실패-nosuchbeandefinitionexception)
- `@WithMockUser` 인데 인증 사용자가 `Status expected:<200> but was:<302>`
  - See: [Spring Security @WebMvcTest](spring-security-webmvctest.md#증상-2--withmockuser-가-무시되고-인증-사용자가-302로그인-리다이렉트로-처리됨)

### Runtime Errors
- `/api/schedules/undefined/participants` - 500 error
  - See: [Thymeleaf + Java Records](thymeleaf-javascript-records.md#javascript에서-undefined-속성)
- `Query did not return a unique result: N results were returned`
  - See: [JPA NonUniqueResultException](jpa-non-unique-result-exception.md)

### 표시 값 오류 (예외·로그 없음)
- 원시 카운트는 정상인데 화면의 평균·비율만 `0.0` (또는 소수부가 항상 `.0`)
  - See: [Thymeleaf SpEL 정수 나눗셈](thymeleaf-spel-integer-division.md#원인)
- 템플릿 표현식 안에서 `/` 로 나눗셈하는 코드 전수 검사
  - See: [Thymeleaf SpEL 정수 나눗셈](thymeleaf-spel-integer-division.md#디버깅)

### SEO / Indexing Errors
- "리디렉션이 포함된 페이지" - Google Search Console 색인 실패
  - See: [Google Search Console Redirect](google-search-console-redirect.md)
- HTTP/HTTPS 또는 www/non-www URL 불일치
  - See: [Google Search Console Redirect](google-search-console-redirect.md#원인)

### Performance / Frontend
- LCP critical path 에서 manifest.json 이 2 s+ 점유, Pretendard woff2 2 MB
  - See: [Lighthouse Performance Audit](lighthouse-performance-audit.md)

### Deploy / CI-CD Errors
- `SC1073 (error): Couldn't parse this shellcheck directive` / `SC1072 (error): Expected '=' after directive key`
  - See: [배포 CI/CD 구축](deploy-cicd.md#1-ci-가-빌드도-못-해보고-죽는다--shellcheck-sc1072sc1073)
- CI 는 통과하는데 GHCR 에 이미지가 없다 / `docker manifest inspect` 실패
  - See: [3단 구조 — 발행은 deploy 브랜치에서만](deploy-cicd.md#3단-구조)
- `fatal: ambiguous argument 'deploy': both revision and filename`
  - See: [배포 CI/CD 구축](deploy-cicd.md#4-git-log-deploy-가-fatal-ambiguous-argument)
- `sudo cp .../calendar-deploy-gate.sh` 가 `No such file or directory`
  - See: [배포 CI/CD 구축](deploy-cicd.md#5-런북대로-하면-no-such-file--게이트-설치가-번들-반입보다-앞)
- 배포 워크플로가 `Permission denied (publickey)` 또는 `Host key verification failed`
  - See: [ephemeral IP 변경](deploy-cicd.md#9-외부-ip-가-바뀌면-시크릿-2개가-동시에-죽는다) · [게스트 에이전트 덮어쓰기](deploy-cicd.md#7-gcp-게스트-에이전트가-authorized_keys-를-덮어쓸-수-있다)
- `docker login ghcr.io` 가 안 되거나 어느 날 갑자기 모든 배포가 `exit 5`
  - See: [배포 CI/CD 구축](deploy-cicd.md#8-docker-login-ghcrio-가-fine-grained-pat-으로-안-된다)
- 실행 키는 되는데 rsync 키만 조용히 실패
  - See: [배포 CI/CD 구축](deploy-cicd.md#13-rrsync-가-path-에-없으면-rsync-키만-조용히-실패한다)
- 컨테이너가 root 그룹으로 돌거나 `data/`·`logs/` 를 sudo 없이 못 지운다
  - See: [배포 CI/CD 구축](deploy-cicd.md#14-sudo-id--g-는-0-이다)

### Nginx Errors
- `the "listen ... http2" directive is deprecated`
  - See: [Nginx Configuration](nginx-configuration.md#http2-지시어-deprecated-경고)
- `conflicting server name "example.com" on 0.0.0.0:80, ignored`
  - See: [Nginx Configuration](nginx-configuration.md#중복-server_name-충돌-경고)
- www/non-www 리디렉션 설정
  - See: [Nginx Configuration](nginx-configuration.md#www--non-www-리디렉션-설정)
