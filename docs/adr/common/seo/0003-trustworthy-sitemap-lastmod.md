# ADR-0003: Sitemap lastmod 신뢰도 회복 — BuildProperties + repo MAX(createdAt) + ISO 8601 + XML escape

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | common |
| 관심사 | SEO |
| 관련 커밋 | `docs/git_commit.md` Commit 8 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

기존 `SitemapService` 는 lastmod 를 `LocalDate.now()` 로 채우거나 정적 `LocalDate`
상수를 사용. Google 은 2023년부터 lastmod 신호의 *신뢰도가 낮은 사이트맵을 점진적으로
무시* 한다고 밝혔다 — 매일 모든 페이지 lastmod 가 오늘 날짜면 신호가 무의미.

추가 결함:

- LocalDate 만 사용해 시각 정밀도 0 → ISO 8601 풀 형식 미달.
- `<loc>` 에 쿼리 결합 시 (`?utm_*` 등) `&` 미escape → invalid XML 위험.

## Decision — 무엇을 골랐나

lastmod 의 *원천(source of truth)* 을 코드 외부 신호로 옮기고 출력 형식을 ISO 8601
KST 로 통일.

- **정적 페이지 lastmod = `BuildProperties.getTime()`** (배포 시각). `build.gradle`
  의 `springBoot { buildInfo() }` 활성화로 `META-INF/build-info.properties` 자동
  생성, `BuildProperties` 빈 주입.
- **인사이트 페이지 lastmod = `Location/MenuRepository.findLatestActivity()`** —
  `SELECT MAX(createdAt)` 쿼리. 데이터 없으면 buildTime fallback.
- **`SitemapEntry.lastmod` 타입** `LocalDate` → `OffsetDateTime`. 출력 형식 `2026-05-01`
  → `2026-05-01T11:20:06.997+09:00` (KST).
- **`escapeXml()` 헬퍼** — `& < > " '` 5개 미리 정의 엔티티 escape. 모든 `<loc>` /
  `href` 에 적용.
- **회귀 테스트 6종:** ISO 형식 / 결정성 (재호출 동일) / well-formed XML / escape /
  모든 loc 은 https / hreflang ko+en+x-default 항상 함께.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| `LocalDate.now()` 유지 | 단순 | Google 무시 정책 직접 위반 |
| 페이지별 콘텐츠 hash 변경 시각 추적 | 가장 정확 | 콘텐츠 정의 광범위 → 인프라 추가 (Hash 저장 테이블) |
| **(선택) BuildProperties + repo MAX** | 신호 의미 명확, 무효 갱신 0 | — |

ISO 8601 풀 정밀도 + KST offset 은 sitemap 표준 권장 형식. XML escape 는 사고 발생
전 예방 — UTM 같은 쿼리가 추가될 때 유효 XML 보장.

## Consequences — 영향

- **긍정:**
  - 배포 안 한 정적 페이지의 lastmod 는 같은 시각 — 신호 안정성.
  - 인사이트 페이지는 실제 활동(새 Location/Menu 추가) 시점에만 갱신.
  - 회귀 테스트 6종으로 ADR 0002 (https), 0004 (hreflang) 의 정합성도 보장.
- **부정:**
  - `BuildProperties` 의존 추가 — 빌드 환경 따라 build-info.properties 미생성 시 빈
    주입 실패 (gradle 설정으로 강제).
  - `findLatestActivity()` 가 매 sitemap 응답마다 SQL 1회 — 캐시 미적용 (빈도 낮음).
- **후속:**
  - ADR-0004 (hreflang + 토글 rel) 가 같은 sitemap 위에 hreflang xhtml:link 추가.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/application/service/SitemapService.java`
  - `src/main/java/me/singingsandhill/calendar/common/application/dto/SitemapEntry.java`
  - `src/main/java/me/singingsandhill/calendar/datedate/domain/location/LocationRepository.java`
  - `src/main/java/me/singingsandhill/calendar/datedate/domain/menu/MenuRepository.java`
  - `build.gradle`
  - `src/test/java/me/singingsandhill/calendar/common/application/service/SitemapServiceHreflangTest.java`
- 관련 docs: `docs/seo-evolution-playbook.md` (sitemap 성숙도 모델 L0~L5)
- 관련 커밋: `docs/git_commit.md` Commit 8
