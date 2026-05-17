# 프로젝트 이슈 / 결정 로그

이 파일은 두 부분으로 구성된다.

1. **현행 결정 추적** — 최신 아키텍처·정책 결정은 ADR 로 관리. 이 파일에 개별 항목을
   복제하지 않는다 (사실 표류 방지).
2. **초기 셋업 히스토리** — 2025-12 MVP 이전 9개 셋업 작업 기록. 현재 코드와 더 이상
   1:1 대응되지 않으므로 *역사 자료* 로만 보존.

## 1. 현행 결정 추적

새 결정·정책 변경은 **`docs/adr/`** 에 ADR 로 작성한다. 인덱스: [`docs/adr/README.md`](docs/adr/README.md).

| 영역 | 진입점 |
|---|---|
| 도메인 모델 / 영속성 | `docs/adr/{datedate,stock,trading,runner}/` |
| SEO / i18n / 보안 / 에러 처리 | `docs/adr/common/` |
| 트레이딩 알고리즘 / 동시성 / 관측성 | `docs/adr/{trading,stock}/` |

PR 단위 작업·버그 수정은 commit 메시지와 GitHub Issue 에서 추적. 본 파일에 따로 옮기지
않는다.

이슈 번호 매핑 (대표 항목):

| GitHub Issue | 영역 | 관련 ADR |
|---|---|---|
| #9 | SEO 인프라 도입 | [common/seo/0001](docs/adr/common/seo/0001-seo-metadata-as-ssot.md) |
| #10 | AdSense 인식 (ads.txt) | [common/seo/0006](docs/adr/common/seo/0006-explicit-static-endpoints.md) |
| #14 | AdSense 정책 / 콘텐츠 강화 | [common/seo/0007](docs/adr/common/seo/0007-content-pages-for-adsense.md) |
| #16 | 에러 처리 강화 | [common/error-handling/0001](docs/adr/common/error-handling/0001-two-layer-exception-handling.md) |
| #17 | 멀티 도메인 분리 / 봇 구현 | [stock/](docs/adr/stock/), [trading/](docs/adr/trading/), [runner/](docs/adr/runner/) |
| #18 | GSC 색인 실패 / HTTPS | [common/seo/0002](docs/adr/common/seo/0002-http-to-https-unification.md) |
| #19 | UI/UX 회귀 (FAQ 애니메이션 등) | (개별 commit) |

---

## 2. 초기 셋업 히스토리 (2025-12 이전)

> **주의:** 아래 9개 항목은 프로젝트 *초기 MVP 단계* 의 작업 단위이며, 이후 모듈
> 분리(#17), SEO/i18n 고도화, 트레이딩/스톡 봇 추가 등으로 코드가 크게 변경되었다.
> 현재 코드와 1:1 대응되지 않는다. 역사 자료로만 보존.

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

자세한 초기 셋업 내용은 git history (`git log --before=2025-12-15 --oneline`) 와 초기
커밋 메시지 참고.
