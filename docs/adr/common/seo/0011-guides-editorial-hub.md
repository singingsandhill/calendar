# ADR-0011: /guides 에디토리얼 허브 — 기사별 전용 템플릿 + 날짜 SSOT

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-23 |
| 도메인 | common (SEO) / datedate |
| 관심사 | SEO / 콘텐츠 아키텍처 |
| 관련 커밋 | (guides 세션 A~C 배치, Commit 161~) |
| 관련 문서 | [3차 통지 진단](../../../audit/adsense-low-value-content-diagnosis-2026-08-17.md) · [구현 계획](../../../superpowers/plans/2026-08-23-datedate-guides-editorial-hub.md) |

## Context — 무엇이 문제였나

AdSense "낮은 가치 콘텐츠" 3차 통지(2026-08-17)의 진단 결론은 editorial 이었다 — 색인
13페이지 중 11페이지가 서비스 자기서술이고, 유일한 에디토리얼 표면(use-case 5페이지)은
한 템플릿(`use-cases/detail.html`)이 생산해 "복제 템플릿 페이지" 신호까지 동반했다.
검색의도형 장문 콘텐츠 카테고리 자체가 없었다.

## Decision — 무엇을 골랐나

`/guides` "모임 노하우"(en "Tips & Guides") 에디토리얼 허브를 신설한다.

1. **기사별 전용 템플릿** — `GuidesController.article()` 이 `"guides/" + slug` 뷰를 반환.
   기사마다 골격이 다르다(① 7단계 체크리스트, ② 5×5 비교표, ③ 메시지 문구 템플릿 3종
   + 로케일 분기 섹션, ④ 10항목 체크리스트).
2. **날짜 SSOT** — `datedate/domain/guide/GuideSlugs`(record: slug, published, modified)가
   사이트맵 lastmod(`modified.atStartOfDay(KST)`), Article JSON-LD 의
   datePublished/dateModified, 화면 바이라인 3곳의 단일 원천. **기사 본문을 수정하면
   반드시 `modified` 를 함께 갱신한다.**
3. **미지 슬러그 HTTP 404** — `GuideNotFoundException`, ADR datedate/domain/0008 규칙 적용.
4. **네임스페이스 분리** — 기존 `/guide`(사용 가이드, `guide.*` 키, `.guide-*` CSS)와
   구분해 `guides.*` / `seo.guides.*` 키와 `.guides-*` CSS 를 쓴다. 표시명도
   "가이드"(nav.guide)와 겹치지 않는 "모임 노하우"로 확정.
5. **광고 정책** — 허브 인덱스는 `adsEnabled(false)`(내비게이션 페이지), 기사는
   `adsEnabled(true)` + 본문 뒤 infeed 1개.
6. **저자 신원** — Organization(DateDate 팀)으로만 표기. 실존하지 않는 개인
   프로필·sameAs 계정은 만들지 않는다(2026-08 기존 결정 유지 — 허위 신원이 더 나쁜 신호).

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| use-case 식 공용 템플릿 + 번호 키 | 5개 use-case 페이지가 받은 "한 템플릿 복제" 신호를 재생산. 비교표 같은 기사별 특수 블록이 기형적으로 들어감 |
| 동적 fragment (`~{${...}}`) 조립 | repo 에 전례 0건 — 모든 include 가 리터럴 경로라는 관례 위반. 이득도 불명확 |
| lastmod = 빌드 시각 (기존 정적 페이지 방식) | 기사 단위 갱신 신호가 죽는다 — ADR common/seo/0003 의 "신뢰 가능한 lastmod" 정책과 충돌 |
| **(선택) 전용 템플릿 + record SSOT** | 구조 다양성·정직한 날짜·기사 추가 시 라우팅/사이트맵/푸터 자동 반영(UseCaseSlugs 패턴) |

## Consequences — 영향

- **긍정:** 색인 표면 13→18페이지, 검색의도형 장문 4편(각 ko ≈3,900~4,000자 / en
  1,200~1,300단어)이 서비스 자기서술 비중을 실질적으로 낮춘다. SEO 가드 5종·사이트맵·
  푸터가 `GuideSlugs.ALL` 루프로 자동 커버 — 기사 추가 비용이 콘텐츠 자체로 수렴.
- **부정/트레이드오프:** 메시지 카탈로그가 866→1,100+키로 커졌다(기사당 60~90키×2로케일).
  기사별 전용 템플릿이라 공통 마크업 변경(바이라인·CTA 등) 시 4개 파일을 손봐야 한다 —
  기사 수가 10편을 넘으면 fragment 추출을 재검토.
- **함정 2건 (구현에서 실증):** ① SecurityConfig 에 2-세그먼트 catch-all 이 없어
  `/guides/{slug}` 는 명시 permitAll 필수(RED 에서 /login 302 실증, `DatedateAuthSecurityTest`
  가드). ② `ReservedOwnerIds` 에 "guides" 추가하지 않으면 오너 ID 선점 가능.
- **회귀 가드:** `GuidesLocaleRenderingTest`(분량 하한 ko 3,800자/en 1,100단어 + 로케일
  마커 + 미해석 키 부재 + 404), `GuideSlugsTest`(날짜 불변식), 사이트맵 정확 집합·
  hreflang 카운트(18×6=108)·기사 lastmod 단언.
