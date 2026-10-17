# /guides "모임 노하우" 에디토리얼 허브 — 집필 + 구현 계획 (실행 완료 기록)

| 항목 | 값 |
|---|---|
| 작성일 | 2026-08-23 (세션 A~C 실행과 동일 날짜, 완료 상태로 영속화) |
| 배경 | AdSense "낮은 가치 콘텐츠" 3차 통지 (2026-08-17) — [진단](../../audit/adsense-low-value-content-diagnosis-2026-08-17.md) 의 §5-1 콘텐츠 레이어 |
| 결정 기록 | [ADR common/seo/0011](../../adr/common/seo/0011-guides-editorial-hub.md) |
| 커밋 | git-commit.md Commit 161~165 (사용자 실행) |
| 상태 | **세션 A·B·C 구현 완료** — 배포·색인·재검토 요청은 §6 절차로 사용자 실행 |

## 사용자 확정 3건

① 기사별 전용 템플릿(view name = `guides/{slug}`) — "한 템플릿 복제" 신호 회피
② 표시명 ko **"모임 노하우"** / en **"Tips & Guides"** — /guide(사용 가이드)와 구분, 노출은 푸터 섹션
③ 기사 4편: how-to-pick-a-date / scheduling-methods-compared / scheduling-etiquette / group-poll-best-practices

## 편집 원칙

1. **검색의도형** — DateDate 몰라도 완결된 가치. 서비스 언급 기사당 최대 1회, CTA 는 템플릿 공통부만.
2. **구조적 다양성** — 기사별 특수 블록: ① 7단계 체크리스트 ② 5×5 비교표 ③ 메시지 문구 3종+로케일 분기 ④ 10항목 체크리스트.
3. **정직한 신뢰 신호** — 저자 Organization(가짜 프로필 금지), 게시/수정일 3곳 노출(GuideSlugs SSOT),
   운영 실데이터 근사 인용+insights 링크, 경쟁 도구(When2meet·Doodle) 정직 비교(한계 명시 포함).
4. **ko/en 병렬 집필** — 번역 아님. ③ §5 는 ko 회식·경조사·상견례 / en RSVP·plus-one 으로 원문 분기.
5. **하드 제약** — ko "준비 중"·"공사 중" 금지(하이진 테스트가 messages 스캔), en `''` 금지(인자 없는
   메시지), summary 120~160자 양 로케일(SeoServiceI18nTest 가드), 본문 내부 링크 ≤2.

## 주제 경계표 (기사 간 중복 방지)

| 소주제 | 소유 | 타 기사 |
|---|---|---|
| 선택지 설계·마감 원칙·확정 기준 | ① | ④는 가지치기·공지 절차만 |
| 도구 선택 | ② | ①④는 링크만 |
| 응답·변경·취소 매너 | ③ | ④는 무응답 처리 절차만 |
| 리마인드(투표 중)·교착 타이브레이크 | ④ | ①은 확정 후 공지·차선일 원칙만 |

## 구현 결과 (세션별)

- **A (Commit 161~162)**: SecurityConfig 2-세그먼트 permitAll(RED 에서 /login 302 실증) +
  ReservedOwnerIds "guides" / GuideSlug(s) SSOT / GuidesController·GuideNavAdvice·
  GuideNotFoundException / SeoService 2 빌더(기존 가드 5종에 `GuideSlugs.ALL` 루프 편입) /
  사이트맵(기사 lastmod = modified, ADR 0003 정합) / robots.txt·WebConfig Vary /
  footer-minimal 5번째 섹션 / style.css `.guides-*` 블록 / 기사 ① (ko 3,935자·en 1,295단어)
- **B (Commit 163)**: 기사 ② (비교표 30셀, ko 3,959자·en 1,252단어) + ③ (문구 템플릿·로케일 분기,
  ko 3,933자·en 1,212단어) — 인프라 코드·가드 수정 0 (루프 자동 커버 실증)
- **C (Commit 164~165)**: 기사 ④ (ko 3,908자·en 1,210단어) + ADR 0011 + README 두 뷰 +
  CLAUDE.md 3종 + fragments 광고 표 + 이 문서
- 최종 색인 표면: **18페이지 × ko/en = 36 URL**, hreflang 108 엔트리, 전체 스위트 GREEN.

## 검증 체계

`GuidesLocaleRenderingTest` — 슬러그×로케일 매트릭스(마커: 리드타임/lead time, 준비 비용/spreadsheet,
노쇼/RSVP, 가지치기/anchoring), 분량 하한(ko 3,800자·en 1,100단어 가시 텍스트), `??guides` 미해석 키,
Article JSON-LD, 404, 허브 summary 노출, 홈 푸터 전 기사 링크. + `GuideSlugsTest` 날짜 불변식,
사이트맵 정확 집합·카운트·lastmod, `DatedateAuthSecurityTest` 익명 인가, 패리티·하이진 자동 가드.

## 이후 절차 (사용자)

1. 커밋(161~165, 선행 배치와의 소유권은 커밋 시점 판단) → 배포 1회(평일 09:15~11:25 KST 회피,
   코인 봇 Start 재개) → 진단 문서 §7 curl 검증
2. GSC 사이트맵 재제출 + guides URL 색인 요청 → **색인 확인(~7일)까지 재검토 요청 금지**
3. 정책 센터 검토 요청 → 심사 중 색인 페이지 코드 변경 금지 → 광고는 승인 전까지 OFF 유지
4. 보류 항목: 5번째 기사(timezone-coordination), 슬러그별 OG 이미지, 광고 재개 전 ad-slot
   예약 높이(ADR 0010 후속)
