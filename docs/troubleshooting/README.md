# Troubleshooting Guide

This directory contains solutions to common issues encountered during development.

## Index

| Issue | Description |
|-------|-------------|
| [Spring Boot 4.0 Migration](spring-boot-4-migration.md) | Package changes, annotation moves |
| [Thymeleaf + Java Records](thymeleaf-javascript-records.md) | Inline JavaScript serialization issues |

## Quick Reference

### Build Errors
- `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`
  - See: [Spring Boot 4.0 Migration](spring-boot-4-migration.md#webmvctest-import-error)

### Runtime Errors
- `/api/schedules/undefined/participants` - 500 error
  - See: [Thymeleaf + Java Records](thymeleaf-javascript-records.md#undefined-property-in-javascript)
