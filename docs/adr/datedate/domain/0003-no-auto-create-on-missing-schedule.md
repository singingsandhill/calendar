# ADR-0003: 일정 미존재 시 자동 생성 X → create 페이지 분기

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04-20 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 / 사용자 흐름 |
| 관련 커밋 | `7012cc0` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

이전 동작: `/{ownerId}` 접근 시 해당 owner 의 schedule 이 없으면 *자동으로 생성* 후
빈 일정 뷰로 안내. 의도는 "한 번도 생성 안 한 사용자도 즉시 사용" 이었지만 부작용:

1. **GSC 색인이 빈 일정을 콘텐츠 페이지로 인식** — 매 owner 별 빈 페이지가 각각
   *new content* 처럼 보여 sitemap 신호 오염.
2. **GET 요청이 mutation 발생** — `datedate-architecture-review.md` 의 GET mutation
   리스크. 검색 봇이 GET 만 호출해도 DB row 생성.
3. **owner 가 의도한 일정 생성 흐름 분리 불가** — 모달/폼이 따로 있는데, 자동 생성
   로직과 의미 충돌.

## Decision — 무엇을 골랐나

자동 생성 제거 + create 페이지로 명시 분기.

- **`/{ownerId}` 접근 시 schedule 미존재** → 자동 생성 X. 대신 *create 페이지로 리다이렉트* 또는 이미 존재하는 owner 의 dashboard 분기.
- **명시적 create 흐름** — 사용자가 모달 / 폼으로 생성 의도 표시 후 POST 로 생성.
- **`/api/owners/{ownerId}` 경로 사전 검증 인터셉터** — 잘못된 ownerId 형태(예: regex
  미충족) 시 400 응답.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 자동 생성 유지 | 즉시 사용 | GET mutation, sitemap 오염, 의도 불분명 |
| 자동 생성 + 검색 봇 차단 | UA 검사 | 봇 식별 어렵고 우회 가능 |
| **(선택) GET 무변형 + 명시적 create** | 표준 RESTful | — |

`/{ownerId}` 라우트가 `[a-z0-9-]{2,20}` regex 제약 → naver html 검증 파일 등 다른
정적 경로와 충돌 없음 (ADR-0006 common/seo).

## Consequences — 영향

- **긍정:**
  - GET 요청이 mutation 발생 안 함 → 캐싱/CDN 안전.
  - 빈 일정 페이지 색인 오염 방지.
  - sitemap (ADR-0003 common/seo) 의 lastmod 신뢰도 보강.
- **부정:**
  - 첫 방문 사용자가 한 번 더 "+ 일정 만들기" 클릭 필요 → 마찰 증가. ADR-0002
    (frontend 공유 모달) 가 모달 헤더 CTA 로 이 마찰을 최소화.
- **후속:**
  - ADR-0002 (frontend 공유 Create 모달) 이 헤더 CTA hijack 으로 같은 페이지 안에서
    모달 열기 가능.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/ScheduleController.java`
  - `src/main/java/me/singingsandhill/calendar/datedate/presentation/interceptor/OwnerPathInterceptor.java`
- 관련 docs: `docs/datedate-architecture-review.md` (E-1-c GET mutation)
- 관련 커밋: `git log -1 7012cc0`
