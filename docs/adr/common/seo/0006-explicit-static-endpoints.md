# ADR-0006: ads.txt / naver-verification 명시적 정적 엔드포인트

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2025-12-20 ~ 2026-05-01 |
| 도메인 | common |
| 관심사 | SEO / 외부 검증 |
| 관련 커밋 | `5b65e53`, `e8423ac` |
| 관련 이슈 | #10 |

## Context — 무엇이 문제였나

외부 검증 서비스(AdSense, Naver Search Advisor) 가 *루트 경로의 특정 파일* 을 정확히
인식해야 했지만 Spring 정적 리소스 핸들러는 두 종류의 사고를 낼 수 있었다.

1. **ads.txt 문제 (`5b65e53`)** — 브라우저로 `/ads.txt` 접근 시 200 OK 였지만 AdSense
   크롤러는 인식 실패. 원인 추정: Spring 정적 리소스 핸들러가 `Content-Type:
   application/octet-stream` 또는 잘못된 캐시 헤더로 응답. AdSense 는 `text/plain`
   기대.
2. **Naver 검증 (`e8423ac`)** — `/naver{hash}.html` 파일을 루트에 두고 정적 핸들러가
   서빙 — 다른 라우팅(예: `/{ownerId}` 글로브 매칭) 과 충돌 가능성.

## Decision — 무엇을 골랐나

각 검증 파일을 명시적으로 노출.

- **ads.txt** — `@GetMapping("/ads.txt", produces=TEXT_PLAIN)` 명시 컨트롤러로 노출 →
  Content-Type 보장.
- **naver{hash}.html** — `src/main/resources/static/naver{hash}.html` 정적 파일로 두고,
  `/{ownerId}` 라우트가 `[a-z0-9-]{2,20}` regex 제약을 가져 충돌 없음 검증 (curl 200
  검증 완료 코멘트 commit 메시지에 기록).
- 환경변수: `NAVER_SITE_VERIFICATION` 으로 메타 태그 값도 동시 주입 (ADR-0004 와 같은
  PR에서 묶임).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 정적 리소스만으로 처리 | 단순 | AdSense Content-Type 사고로 ads.txt 인식 실패 (직접 확인됨) |
| nginx 레벨 location 처리 | 인프라 격리 | Spring 라우팅과 분리 — 환경 의존성 ↑ |
| **(선택) 명시적 컨트롤러 + 정적 파일 혼용** | 각각의 트레이드오프에 맞는 처리 | — |

ads.txt 는 *Content-Type 명시가 핵심* 이라 컨트롤러 필요. naver html 은 *경로 충돌
검증* 만 하면 정적 파일로 충분.

## Consequences — 영향

- **긍정:**
  - AdSense 가 ads.txt 정상 인식.
  - 네이버 검증 통과.
- **부정:**
  - 두 종류의 노출 방식이 공존 → 신규 검증 파일 추가 시 어느 방식을 쓸지 매번 결정.
- **후속:**
  - 라우팅 우선순위 매칭 사고는 ADR-0001 (SeoMetadata SSOT) 의 `StaticResourceController`
    원칙과 동일.

## References

- 관련 코드:
  - `src/main/resources/static/ads.txt` 또는 `StaticResourceController` (분리된 컨트롤러)
  - `src/main/resources/static/naver52cf63f6fb22d9c9f017934c5d0b7d5c.html`
- 관련 docs: `docs/seo-evolution-playbook.md#22-adsense-도입의-부작용`
- 관련 커밋: `git log -1 5b65e53`, `git log -1 e8423ac`
