# Data Analytics — DateDate

DateDate (`datedate.site`) 의 데이터 수집·파이프라인·분석 자산 모음.

이 폴더는 GA4 / GTM / Search Console / BigQuery / Looker Studio / IndexNow / 도메인 DB
가 어떻게 연결되어 있고, 어떤 분석이 가능하며, 무엇을 더 해야 하는지를 정리한다.
SEO 자체 결정은 [`docs/seo-evolution-playbook.md`](../seo-evolution-playbook.md) 에,
배포 후 회고는 [`docs/troubleshooting/`](../troubleshooting/) 에 있고, 본 폴더는 *데이터*
관점에 한정.

## 문서 가이드

| 파일 | 내용 |
|---|---|
| [01-current-state.md](01-current-state.md) | 활성 자산 인벤토리 — 모든 ID, 키, 연결 상태 |
| [02-data-inventory.md](02-data-inventory.md) | 분석 가능한 데이터 카탈로그 — GA4 이벤트 + DB 테이블 |
| [03-analysis-playbook.md](03-analysis-playbook.md) | 즉시 실행 가능한 분석 쿼리·차트 모음 |
| [04-todo.md](04-todo.md) | 우선순위 백로그 (P0~P3) |
| [05-expansion.md](05-expansion.md) | 확장 가능성·미래 아이디어 |

## 한 줄 요약

| 영역 | 상태 |
|---|---|
| GA4 속성 (`G-9QTMK4CDDF`, ID 516824378) | ✅ 활성 |
| GTM 컨테이너 (`GTM-PFPKQT7W`) | ✅ 활성, 모든 공개·UGC 페이지 적용 (private `/trading/` 의도적 제외) |
| Search Console ↔ GA4 | ✅ 2026-01-16 연결 (URL 접두사 `https://datedate.site/`) |
| AdSense ↔ GA4 | ⏸ AdSense 사이트 미승인 대기 (`pub-7334667748813914`) |
| BigQuery export | ✅ 활성 |
| Looker Studio 파이프라인 | ✅ 활성 |
| IndexNow (Bing/Yandex/Naver) | 🟡 코드 활성, 운영 환경 `INDEXNOW_ENABLED` 확인 필요 |
| Naver Search Advisor | ✅ 검증 파일 배포 (`naver52cf63f6fb22d9c9f017934c5d0b7d5c.html`) |
| dataLayer 비즈니스 이벤트 (코드) | ✅ 6종 / 7개 사이트 푸시 |
| GTM 트리거·태그 (이벤트 → GA4) | ⏳ 사용자 콘솔 작업 대기 |
| GA4 맞춤 측정기준 등록 | ⏳ 대기 |
| DB 미러링 to BigQuery | ⏳ 미설정 |

## 이전 분석/결정 위치

- [`docs/troubleshooting/lighthouse-performance-audit.md`](../troubleshooting/lighthouse-performance-audit.md) — Lighthouse 측정 결과, GTM 측정 ID 구조 (B 항목)
- [`docs/seo-evolution-playbook.md`](../seo-evolution-playbook.md) — SEO 단계별 발전 결정
- [`docs/adr/common/seo/`](../adr/common/seo/) — SitemapService / SeoMetadata SSOT / IndexNow ADR
