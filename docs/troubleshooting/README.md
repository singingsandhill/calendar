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

## Quick Reference

### Build Errors
- `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`
  - See: [Spring Boot 4.0 Migration](spring-boot-4-migration.md#webmvctest-import-error)

### Runtime Errors
- `/api/schedules/undefined/participants` - 500 error
  - See: [Thymeleaf + Java Records](thymeleaf-javascript-records.md#undefined-property-in-javascript)
- `Query did not return a unique result: N results were returned`
  - See: [JPA NonUniqueResultException](jpa-non-unique-result-exception.md)

### SEO / Indexing Errors
- "리디렉션이 포함된 페이지" - Google Search Console 색인 실패
  - See: [Google Search Console Redirect](google-search-console-redirect.md)
- HTTP/HTTPS 또는 www/non-www URL 불일치
  - See: [Google Search Console Redirect](google-search-console-redirect.md#원인)

### Nginx Errors
- `the "listen ... http2" directive is deprecated`
  - See: [Nginx Configuration](nginx-configuration.md#http2-지시어-deprecated-경고)
- `conflicting server name "example.com" on 0.0.0.0:80, ignored`
  - See: [Nginx Configuration](nginx-configuration.md#중복-server_name-충돌-경고)
- www/non-www 리디렉션 설정
  - See: [Nginx Configuration](nginx-configuration.md#www--non-www-리디렉션-설정)
