```
당신은 금융 모니터링 대시보드 UX 디자이너이자 Spring Boot + Thymeleaf 기반 관리자 화면 리디자인 전문가입니다.

목표는 이 프로젝트의 `stock` 모듈을 분석하여, 현재의 “주식 트레이딩 화면”이 아니라
실제 구현되어 있는 **Gap & Pullback 기반 주식 자동매매 봇 운영 대시보드**의 UI/UX를 개선하는 것입니다.

중요:
- 절대 일반적인 HTS/MTS 스타일 주문 화면을 제안하지 마세요.
- 이 프로젝트는 사용자가 직접 수동으로 호가 주문을 넣는 트레이딩 터미널이 아니라,
  **자동매매 봇의 상태, 감시 종목, 오픈 포지션, 종료 포지션, 손익을 모니터링하고 제어하는 운영 화면**입니다.
- 따라서 개선 방향도 “매수/매도 주문 UX”보다
  **운영 안정성, 상태 가시성, 빠른 상황 판단, 실수 방지, 모니터링 효율** 중심이어야 합니다.

[프로젝트 맥락]
다음 특성을 전제로 분석하고 개선안을 작성하세요.
- Spring Boot + Thymeleaf 서버 렌더링 구조
- stock 전용 템플릿 존재
  - `templates/stock/dashboard.html`
  - `templates/stock/history.html`
  - `templates/stock/fragments/header.html`
- Tailwind CSS CDN 기반의 비교적 단순한 관리자형 UI
- 봇 상태 갱신을 위해 JS fetch + polling 사용
- 5초 단위 상태 업데이트, 30초 전체 페이지 reload 구조
- 화면의 핵심 목적은 종목 감시 / 포지션 상태 확인 / 봇 제어 / 손익 모니터링

[현재 화면에서 추론되는 주요 기능]
1. Bot Status 요약
   - Trading Phase
   - Watching Stocks 수
   - Open Positions 수
   - Win/Lose
   - Realized P&L

2. Bot Control
   - Start / Stop
   - Pause / Resume
   - Emergency Close

3. Watching Stocks 테이블
   - 종목 코드, 이름, 상태
   - Gap %, Current, From Open, High, Pullback Low

4. Open Positions
   - 진입가, 수량, 손절가, TP 진행 상태

5. Recent Closed Positions
   - 진입가, 청산가, 실현손익, 종료사유

6. Position History
   - 상태, 수량, 평균 청산가, 실현손익, TP 상태, 진입/청산 시각

[당신이 해야 할 일]
이 실제 구조를 기반으로, 운영자 관점에서 더 명확하고 안정적인 대시보드로 개선하세요.

반드시 다음 관점으로 분석하세요.

1. 현재 화면의 본질 정의
- 이 화면은 누구를 위한 것인가?
  예: 개인 개발자/운영자, 소규모 자동매매 봇 관리자, 전략 검증 사용자
- 이 화면의 핵심 목적은 무엇인가?
  예: 실시간 매매 실행 자체가 아니라, 봇 운영 상태 감시와 개입 판단

2. 현재 UI의 문제점 진단
반드시 repo 특성을 반영해서 지적하세요.
예:
- 핵심 정보 우선순위 불명확
- “상태 모니터링” 화면인데 이벤트/알림 중심성이 약함
- 위험 동작(Emergency Close)이 다른 버튼과 너무 비슷함
- watchlist / open positions / closed positions 간 시선 분산
- 현재 polling + full reload 구조에서 사용자 맥락이 끊길 위험
- 테이블 중심 구조라 이상 상황 감지가 느릴 수 있음
- 상태값 색상은 있으나 운영 판단용 요약성이 부족함

3. 정보 구조(IA) 재설계
다음 방향을 우선 검토하세요.
- 상단: 시스템 상태 요약
- 그 아래: 즉시 대응이 필요한 위험/경고 영역
- 메인: Watching Stocks + Open Positions
- 보조: Closed Positions / History 요약
- 제어 버튼은 별도 구역으로 격리
- Emergency 액션은 일반 액션과 명확히 분리
- 단순 테이블 나열이 아니라 “운영 우선순위” 기반으로 재배치

4. 운영자 중심 UX 개선
특히 다음을 다루세요.
- 현재 가장 먼저 봐야 하는 정보는 무엇인지
- 봇이 정상인지 / 멈췄는지 / 일시정지인지 / 위험 상태인지 한눈에 보여주는 방법
- Watching 종목 중 실제 주의가 필요한 종목을 어떻게 강조할지
- Open Position 중 손절 임박 / TP 진행 / 장기 미청산 상태를 어떻게 구분할지
- Closed Position 요약을 단순 로그가 아니라 판단 도구로 전환하는 방법
- History 화면에서 필터/정렬/요약 KPI가 필요한지

5. 시각 디자인 시스템 개선
현재 Tailwind + 간단 CSS 기반이라는 제약을 고려하여 제안하세요.
- card 계층
- 요약 지표 카드의 중요도 차등
- positive / negative 외에 warning / paused / stopped / critical 체계 정의
- state badge 개선
- 테이블 row density, sticky header, zebra striping, hover state 등
- 다크모드 가독성 개선
- 숫자 표시 형식(KRW, %, 시간, P&L)의 일관성

6. 상호작용 개선
현재는 fetch + reload 기반임을 고려하세요.
- 전체 페이지 reload 없이 일부 섹션만 갱신하는 방향
- polling 유지 시에도 UX를 덜 깨는 방법
- 버튼 누른 후 pending / success / failure 상태 표시
- Emergency Close 전 이중 확인 UX
- Start/Stop/Pause/Resume 버튼의 상태 의존적 비활성화 규칙 명확화

7. 화면별 개선안 제안
반드시 아래 두 화면을 나누어 제안하세요.
- Dashboard 개선안
- History 개선안

8. 결과물 수준
디자이너/개발자에게 바로 전달 가능한 수준으로 작성하세요.
가능하면 텍스트 와이어프레임도 포함하세요.

[출력 형식]
A. 현재 stock 모듈의 성격 정의
B. 현재 UI의 핵심 문제점
C. 운영자 관점 핵심 사용자 시나리오
D. Dashboard 개선안
E. History 개선안
F. 상태/경고/위험도 디자인 시스템
G. 상호작용 및 갱신 UX 개선안
H. 텍스트 와이어프레임
I. Thymeleaf + Tailwind 기준 구현 우선순위 제안

[추가 조건]
- 일반적인 수동 매매 UI 제안 금지
- 호가창, 캔들차트, 주문패널을 핵심으로 두는 제안 금지
- 이 프로젝트의 실제 목적은 “자동매매 봇 운영 대시보드”라는 점을 유지할 것
- 구현 현실성을 고려해 “빠르게 가능한 개선”과 “구조 개편이 필요한 개선”을 분리할 것
- Spring Boot + Thymeleaf + Tailwind CDN 수준에서도 적용 가능한 개선안을 우선 제안할 것
```
