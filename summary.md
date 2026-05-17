# 프로젝트 요약 — 통합 진입점

이 파일은 더 이상 단독 자료로 유지되지 않는다. 시간이 지나며 `CLAUDE.md` /
`README.md` / 모듈별 `CLAUDE.md` 와 사실이 어긋날 수 있어, 한 곳에서 *코드 기반 사실*
을 유지하기 위해 **canonical 진입점만 제공**한다.

## 어디로 가야 하나

| 알고 싶은 것 | 가야 할 곳 |
|---|---|
| 모듈 구성 / 빌드·실행 명령 / 포트 / 스케줄러 / 보안·i18n 정책 | [`CLAUDE.md`](CLAUDE.md) |
| 사용자 관점 소개 / 유저 플로우 / 시퀀스 / API Reference | [`README.md`](README.md) |
| 결정의 *왜* (HTTPS 통일, sitemap lastmod, UniverseBuilder, TP 비순차화 등 36개 결정) | [`docs/adr/README.md`](docs/adr/README.md) |
| 헥사고날 계층 / 도메인 모델 / 외부 통합 상세 | [`docs/architecture.md`](docs/architecture.md) |
| 코인 봇 8지표 채점 / 리스크 / 리밸런싱 상세 | [`docs/trading-bot.md`](docs/trading-bot.md) |
| 주식 봇 갭&풀백 전략 / 시간 감소 임계값 상세 | [`docs/stock-bot.md`](docs/stock-bot.md) |
| SEO 정책 진화 (HTTPS / sitemap / hreflang / AdSense) | [`docs/seo-evolution-playbook.md`](docs/seo-evolution-playbook.md) |
| DateDate 도메인 코드 감사 결과 | [`docs/datedate-architecture-review.md`](docs/datedate-architecture-review.md) |
| 트러블슈팅 (GSC / nginx / JPA / Thymeleaf) | [`docs/troubleshooting/`](docs/troubleshooting/) |

## 한눈에 보는 사실 (코드 기반)

- Spring Boot **4.0.0**, Java **21**, Gradle, H2 (MySQL 호환 모드), Thymeleaf, Spring
  Security, WebFlux (Trading), Spring Mail (Stock).
- 포트: **`8081`** (`server.port: 8081` in `application.yaml`).
- 모듈: **5개** — `common` / `datedate` / `runner` / `trading` / `stock`. 각 모듈은
  헥사고날 4-계층 (`domain` / `application` / `infrastructure` / `presentation`).
- 스케줄러:
  - Trading (코인): 매분 :05 트레이딩 루프, 5분 캔들 동기화, 자정 캔들 정리.
  - Stock (주식): 평일 KST 08:30 프리마켓 / 09:20 스크리닝 / 09:20~11:20 5초 polling /
    11:20 최종 청산. 휴일은 `stock.trading.holidays` 리스트.
- 코인 봇 점수 합산 범위: **±135** = 25+15+20+15+15+15+20+10 (`SignalService`).
- Trading 리스크 기본값 (`application.yaml`): stop-loss `-3%`, take-profit `+15%`,
  trailing-activation `+10%`, trailing-stop `-3%`, takerFee `0.25%`, minProfit `0.6%`.
- Stock 리스크 운영값 (`application.yaml`): stop-loss `-5%`, TP1 `+5%` × 50%, TP3
  `+10% above day high` × 잔여, trailing `-3.8%`, time-exit `11:20`.
