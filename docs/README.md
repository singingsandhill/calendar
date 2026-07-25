# docs 인덱스

프로젝트 문서 루트. 모든 문서는 **소문자 kebab-case** 이름 규칙을 따르고, 주제별 폴더로 분류한다.

## 구조

| 경로 | 내용 |
|------|------|
| [`architecture.md`](architecture.md) | 시스템 전체 아키텍처 (헥사고날 구조, 모듈 규약) |
| [`adr/`](adr/README.md) | 아키텍처 결정 기록 (도메인 × 관심사 매트릭스, 자체 인덱스) |
| [`audit/`](audit/) | 감사·운영 리뷰 보고서 |
| [`data-analysis/`](data-analysis/README.md) | 데이터 분석 (GA4/GTM 현황, 인벤토리, 플레이북) — 구 `DA/` |
| [`datedate/`](datedate/) | DateDate 도메인 문서 (아키텍처 리뷰, 카카오 로그인·Recap 체크리스트) |
| [`guides/`](guides/) | 작업 가이드 (`git-commit.md` 커밋 시퀀스, `ux-validation.md` 입력 검증 UX) |
| [`prompts/`](prompts/) | 재사용 프롬프트 모음 — 구 `prompt/` 병합 |
| [`seo/`](seo/) | SEO·AdSense (진화 플레이북, 회고 시리즈, 저가치 콘텐츠 대응) |
| [`stock/`](stock/) | 주식 봇 (전략·리스크 상세) |
| [`trading/`](trading/) | 코인 트레이딩 봇 (전략 상세, v2 마이그레이션, 백로그) |
| [`superpowers/`](superpowers/) | 기능 단위 설계 스펙(`specs/`)과 구현 계획(`plans/`) |
| [`troubleshooting/`](troubleshooting/README.md) | 트러블슈팅 기록 (자체 인덱스) |

## 이름 규칙

- **파일·폴더 모두 소문자 kebab-case** (`git-commit.md`, `data-analysis/`).
- 폴더가 주제를 나타내므로 파일명에서 주제 접두사는 생략한다 (`trading/bot.md`, ~~`trading-bot.md`~~).
- ADR 은 관심사 폴더 아래 `NNNN-제목.md` 번호 접두사 (`adr/README.md` 참고).
- `data-analysis/` 는 읽는 순서대로 `NN-제목.md` 번호 접두사.
- 감사 보고서는 날짜 **접미사** `-YYYY-MM-DD` (`audit/coin-trading-profit-audit-2026-05-30.md`).
- superpowers 스펙·계획은 날짜 **접두사** `YYYY-MM-DD-` (`superpowers/plans/2026-07-11-kakao-login-recap.md`).

## 새 문서를 추가할 때

1. 위 표에서 맞는 주제 폴더를 고른다. 없으면 폴더를 새로 만들고 이 표에 한 줄 추가한다.
2. 아키텍처 관련 *결정*은 문서가 아니라 `adr/` 에 ADR 로 남긴다.
3. 루트에는 새 파일을 두지 않는다 (`architecture.md` 와 이 인덱스만 예외).
