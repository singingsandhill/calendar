# AdSense ② 도구 중심 콘텐츠 — 신뢰 정보 · /guide 보강 · 모임 데이터 리포트 — 설계

| 항목 | 값 |
|---|---|
| 작성일 | 2026-10-14 (설계 대화 2026-10-13~14) |
| 배경 | AdSense "가치가 별로 없는 콘텐츠" 4번째 통지(2026-10-12). 진단: 사이트의 가치·사용자 관심은 도구(공유 링크 일정 페이지)에 있는데 심사 표면은 일반론 콘텐츠다. GA4 90일 — 조회 24,919 / 사용자 5,032, 콘텐츠 페이지는 각 97회 미만, 검색어 상위 10개는 전부 "약속잡기 사이트" 류 도구 검색 |
| 선행 | ① 확정 결함 수정 — `docs/guides/git-commit.md` Commit 198 (use-case 본문 렌더 복구·구조화 데이터 가드·인기 순위 표수순). Commit 197(참여자 10명·일정 설명)과 함께 먼저 커밋·배포 |
| 방향 | B — 승인 후 홈·일정 페이지에도 광고. 심사는 광고 위치를 볼 수 없으므로 ② 는 광고와 무관하게 "도구의 가치와 실제 사용을 보이게" 하는 작업이다 |
| 결정 기록 | ADR common/seo/0015, ADR datedate/domain/0013 (구현 때 작성) |

## 1. 결정 (설계 대화의 질문과 답)

| 질문 | 답 |
|---|---|
| 데이터 리포트 형태 | 라이브 페이지 — 렌더 시점 집계, 기준일 표기. 월간 스냅샷 URL·해설 글은 만들지 않는다 |
| 리포트 지표 | 요일별 가능 비율, 월별 일정 수 추이, 미리 잡는 기간(리드타임). 인원수별 전원 가능일 확률은 넣지 않는다 |
| 도구 보여주기 | `/guide` 보강 — 단계별 실제 화면 스크린샷 + 시간 투표 단계 신설 + 일정 설명 안내. 기능 전용 새 URL 은 만들지 않는다 |
| 운영자 신원 | GitHub 핸들(`singingsandhill`) + 소개를 About 화면에 노출. 실명은 쓰지 않는다 |
| 연락처 이메일 | 현재 주소 유지 — 변경·코드 정리 없음 |
| 기사 게시일 | 실제 첫 공개일로 정정 |
| 광고로 비용 충당 문구 | 사이트 전체에서 모두 제거 (§2-2). 개인정보처리방침의 제3자 광고 고지는 AdSense 필수 고지라 유지 |
| 리포트 계산 방식 | A안 — 요청 시 집계 + 읽기 전용 벌크 쿼리 2개, 캐시 없음 |

기각: B안(일 단위 스냅샷 테이블 + 스케줄러 — 현 규모에서 과설계), C안(기존 포트로 전체 일정을 읽고 참여자를 지연 로딩 — 일정마다 쿼리, 2,171+회).

## 2. ②-1 신뢰 정보

### 2-1. About — "만든 사람" 섹션 (새)

- 위치: 기존 "어떤 분께 유용한가요" 와 "연락 및 피드백" 사이.
- 문구는 검증 가능한 사실만 쓴다. 만든 이유 문장은 사용자가 스펙 리뷰에서 주면 넣고, 주지 않으면 넣지 않는다(지어내지 않는다).
  - ko 초안: "DateDate 는 1인 개발자가 만들고 운영하는 개인 프로젝트입니다. 2025년 12월에 공개했고, 이후 시간 투표(2026년 9월)와 일정 설명(2026년 10월) 같은 기능을 더해 왔습니다."
  - en 초안: "DateDate is a personal project built and run by a single developer. It launched in December 2025 and has since added features such as time voting (September 2026) and schedule descriptions (October 2026)."
  - 링크 한 줄: "운영자 GitHub: singingsandhill" → `https://github.com/singingsandhill` (`target="_blank" rel="me noopener noreferrer"`).
- 근거 날짜: 도메인 등록·첫 커밋 2025-12-14, 시간 투표 커밋 132562b(2026-09-26), 일정 설명 Commit 197.
- JSON-LD: Organization `sameAs` 는 이미 같은 계정 — 변경 없음. `founder`·`Person` 은 두지 않는다.
- ADR: GitHub 화면 노출은 ADR common/seo/0013 결정 3("UI 에는 노출하지 않는다")의 변경 → 새 ADR common/seo/0015, 0013 상태에 "결정 3 의 UI 비노출은 0015 로 대체" 표기.

### 2-2. 사실 점검으로 고치는 문구

| 키 | 변경 |
|---|---|
| `about.section.principles.list1` (ko·en) | 유지 + 한 문장 추가: 선택 사항인 카카오 로그인을 쓰면 카카오 회원번호·닉네임·프로필 이미지 주소를 저장한다 (`AppUser` 필드 기준) |
| `about.section.principles.list2` (ko·en) | 광고 문장 삭제 — "모든 기본 기능에 비용이 없습니다." 까지만 |
| `seo.about.description` (ko) | "광고 운영 기준," 구절 삭제 (en 에는 없음) |
| `terms.section.ads.body` (ko·en) | 앞머리 "서비스 운영을 위해" / "To keep the service running," 만 삭제. 조항 자체는 유지 |
| `about.updated`, `terms.updated` | 실제 반영일로 갱신 |

유지: `about.section.principles.list3`(GTM→GA4 만 로드 — 사실), 개인정보처리방침 "제3자 광고" 섹션.

### 2-3. 기사 게시일 정정

| 기사 | 현재 published / modified | 변경 |
|---|---|---|
| how-to-pick-a-date, scheduling-methods-compared, scheduling-etiquette, group-poll-best-practices | 2026-08-23 / 2026-08-23 | 2026-09-19 / 2026-09-19 |
| 나머지 8편 | 2026-09-24 / 2026-09-24 | 2026-10-04 / 2026-10-04 |

- 09-19 = 09-26 감사가 확인한 4편 포함 라이브 빌드(그 이전 배포일을 사용자가 알려주면 그 날짜). 10-04 = 사이트맵 lastmod 의 빌드 시각.
- `GuideSlugs.ALL` 만 바꾸면 바이라인·Article JSON-LD `datePublished/dateModified`·사이트맵 lastmod 가 함께 바뀐다.
- 바이라인 저자 표기: `guides.byline.author` ko "DateDate 팀" → "DateDate", en "The DateDate Team" → "DateDate". 1인 운영인데 "팀" 은 About 와 모순. Organization 저자(ADR common/seo/0011)는 그대로.

## 3. ②-2 /guide 보강

### 3-1. 단계 구조 (5 → 6)

| # | 단계 | 변경 |
|---|---|---|
| 1 | 페이지 만들기 | 스크린샷 |
| 2 | 일정 생성 | 스크린샷 + 안내 한 줄: 일정 설명(최대 500자)에 장소 후보·회비·링크를 적어 둘 수 있다 |
| 3 | 링크 공유 | 스크린샷 |
| 4 | 날짜 선택 & 투표 | 스크린샷 |
| 5 | **시간 정하기 (새)** | 누군가 가능하다고 고른 날에 30분 단위 시간 후보를 올리고 투표, 겹침 차트로 확인 (ADR datedate/domain/0009 기준) + 스크린샷 |
| 6 | 결과 확인 & 결정 | 기존 5단계 내용을 옮김 + 스크린샷 |

- 키: 기존 `guide.step5.*` 는 새 시간 투표 내용, 기존 5단계 문구는 `guide.step6.*` 로 옮긴다. `seo.guide.step5/6.name·text` 도 같은 순서.
- HowTo JSON-LD(`SeoService.getGuideSeo`)를 6단계로 — 단계 이름은 화면과 같은 문구(`StructuredDataVisibilityTest` 가 가드).
- 활용 사례 링크·문제 해결·FAQ 블록은 구조 유지, 낡은 사실만 고친다.

### 3-2. 스크린샷

- 단계마다 1장, ko·en 각 6장(en 페이지는 영어 UI). 모바일 폭 390px 에서 단계에 해당하는 영역만 잘라 찍는다.
- 형식·위치: WebP(quality 80) `static/image/guide/{ko,en}/step{1..6}.webp`, 장당 목표 80KB 이하.
- `<img>`: `width`·`height`(레이아웃 이동 방지), 1단계 외 `loading="lazy"`, 대체 텍스트 키 `guide.step{N}.imgAlt`(ko·en).
- 캡션 키 `guide.screenshot.caption`: "예시 화면 — 이름과 날짜는 예시입니다" / "Example screen — names and dates are samples". 실제 사용자 기록처럼 보이지 않게 한다.
- 예시 데이터: 2026년 11월 일정, 참여자 5명(ko 지민·서준·하은·도윤·수아 / en Alex·Jamie·Sam·Taylor·Jordan), 가능 날짜가 일부 겹치게, 시간 후보 2개(투표 포함), 장소 2개·메뉴 2개(투표 포함).
- 만드는 방법(1회성, `bootRun` 없이): 임시 테스트가 테스트 DB 에 예시 일정을 넣고 홈·생성 화면·일정 화면 HTML 을 덤프 → 실제 CSS·JS 를 붙인 하네스를 Windows Chrome 헤드리스(CDP)로 캡처. 도구는 커밋하지 않고 절차를 커밋 로그에 남긴다.

## 4. ②-3 모임 데이터 리포트 (`/insights/trends` 확장)

### 4-1. 배치·SEO

- 기존 페이지에 "모임 데이터" 섹션 추가 — 인기 순위 블록 다음, "이용 현황" 앞. 인기 장소·메뉴·이용 현황·상세 통계는 그대로. URL·사이트맵·광고 게이트(`adsEnabled(hasData)`)·noindex 게이트 그대로.
- 제목: ko "모임 데이터 & 인기 트렌드 | DateDate"(26자 ≤ 35), en "Meetup Data & Popular Trends | DateDate"(39자 ≤ 60). h1·breadcrumb 도 같은 이름. `seo.insights.description` 에 요일·리드타임을 언급.
- 섹션 머리에 "기준: YYYY-MM-DD" (렌더 시점 KST 날짜)와 집계 방법 한 문단.

### 4-2. 지표 정의

공통: 참여자가 고른 날짜(`Participant.selections` 의 인덱스)를 실제 날짜로 바꿔 쓴다.

- **날짜 변환:** 7주 확장 모드(`weeks == 7`)는 그리드 시작(1일이 속한 주의 일요일) + (인덱스 − 1), 기존 모드는 그 달의 인덱스일(1 ≤ 인덱스 ≤ 그 달 일수). 범위 밖 인덱스는 건너뛴다. 도메인 메서드 `YearMonth.dateOf(int index, int weeks)` — 웹 달력(`static/js/schedule/calendar.js`)의 두 렌더 경로와 같은 규칙. (`Schedule.dateOf` 위임은 이번 변경에서 쓰는 곳이 없어 만들지 않는다 — RecapService 수정 때 필요하면 추가.)

| 블록 | 정의 | 표시 조건 |
|---|---|---|
| 요일별 가능 비율 | 전체 선택 날짜를 요일(일~토)로 세어 비율(%) | 선택 날짜 300개 이상 |
| 월별 일정 수 | 일정 생성 시각(`createdAt`)의 연월별 개수. 첫 일정 달부터, 최근 12개월까지. 이번 달은 "진행 중" 표시 | 일정 30개 이상 |
| 미리 잡는 기간 | 유효한 날짜를 1개 이상 고른 참여자가 2명 이상인 일정마다 가장 많이 겹친 날(동점이면 이른 날) − 일정 생성일(일 단위). 음수(생성일보다 이전 날짜)는 그 일정을 제외. 구간 0~6일 / 7~13일 / 14~27일 / 28일 이상 비율 + 중앙값(짝수 개면 아래쪽 중앙값) | 해당 일정 30개 이상 |

- 비율은 최대 잉여 반올림으로 정수 %, 합계가 정확히 100.
- 표시 조건 미달 블록은 숨긴다(빈 상태 문구 없음). 하한은 코드 상수 — 설정 키 없음.
- 공개 범위: 집계 수치만. 이름·ID·자유 입력 문구는 없다.

### 4-3. 구조

- 포트(도메인) 2개 추가, 읽기 전용:
  - `ParticipantRepository.findAllAvailability()` → `List<ParticipantAvailability>` (record: scheduleId, year, month, weeks, scheduleCreatedAt, selections). JPA 어댑터는 `JOIN FETCH p.schedule` 쿼리 1회로 참여자와 일정을 함께 읽어 record 로 옮긴다 — `selections` 는 기존 `SelectionListConverter` 가 그대로 변환(생성자 프로젝션은 변환기 적용이 불확실해 쓰지 않는다).
  - `ScheduleRepository.findAllCreatedAt()` → `List<LocalDateTime>`.
- 계산: 스프링 의존 없는 순수 클래스(애플리케이션 계층)가 위 두 목록과 오늘 날짜를 받아 리포트 DTO(블록 3개, 미달 블록은 null)를 만든다. `InsightsService` 가 포트를 읽어 넘기고, `InsightsController` 가 모델에 싣는다.
- 렌더: 서버 HTML. 막대는 CSS 너비(%)이고 `aria-hidden`, 라벨과 값은 텍스트로 남겨 스크린리더가 "토요일 40%" 로 읽는다(같은 수치를 표로 한 번 더 그리지 않는다). JS·차트 라이브러리 없음. 새 CSS 는 소량 — `style.css` 는 한 파일 안 개행 혼재라 바이트 단위 삽입.

## 5. 테스트

| 파트 | 테스트 — 잡는 깨짐 |
|---|---|
| ②-1 | About 렌더(ko·en): "만든 사람" 섹션·GitHub 링크 존재 / 광고 비용 충당 문구가 About·Terms·About SEO 설명에 없다 / `GuideSlugs` 전체 published ≤ modified / 기사 1편 바이라인이 정정 날짜를 렌더 |
| ②-2 | /guide 렌더(ko·en): 6단계, 단계마다 `img` 의 width·height·alt 존재 + 참조 파일이 클래스패스에 실제로 있다 / HowTo JSON-LD 6단계 — 화면 일치는 `StructuredDataVisibilityTest` |
| ②-3 | `YearMonth.dateOf`: 확장·기존 모드, 범위 밖 / `ParticipantAvailability.availableDates`: 중복·null·범위 밖 인덱스 건너뜀 / 계산 클래스(손으로 만든 입력·손으로 계산한 기대값): 요일 비율(두 모드 혼합), 월별(이번 달 진행 중), 리드타임(최다 겹침일·동점 이른 날·음수 제외·구간·중앙값), 비율 합계 100, 하한 미달 → null / 프로젝션 쿼리: 필드·selections 변환 / insights 렌더(서비스 목 — 다른 테스트 데이터와 무관): 블록 있음 → 표시, null → 숨김 |

공통: 파트마다 RED 선확인 → GREEN, 마지막에 전체 스위트와 실제 실행 테스트 수 확인. `SerpTitleBudgetTest`·`StructuredDataVisibilityTest` 무회귀.

## 6. 순서·배포

1. 순서: ②-1 신뢰 정보 → ②-2 /guide → ②-3 리포트. 파트마다 커밋 1개 + 커밋 로그 섹션.
2. 197·198 이 먼저 커밋·배포돼 있어야 한다 — /guide 스크린샷이 참여자 10명·일정 설명 화면을 담는다.
3. 배포는 파트마다(평일 09:15~11:25 제외) — 꾸준한 갱신 자체가 "지속적인 유지관리" 신호. 배포 뒤 라이브 재실측(섹션·구조화 데이터·이미지), GSC 에서 `/about`·`/guide`·`/insights/trends`(ko·en) 색인 요청.
4. 마지막 파트 배포 후 8주 이상 지나 AdSense 재검토를 1회 요청. 그동안 GA4 로 `/guide`·`/insights/trends` 유입 확인.

## 7. 문서

- ADR common/seo/0015 (GitHub 화면 노출 — 0013 결정 3 일부 대체), ADR datedate/domain/0013 (모임 데이터 리포트 — 렌더 시 집계·표본 하한·집계만 공개). ADR README 두 뷰·폴더 수.
- 루트 `CLAUDE.md` DateDate 목록(인사이트 리포트, /guide 6단계), `datedate/application/CLAUDE.md`(InsightsService).

## 8. 위험

- 실제 수치가 기대와 다를 수 있다(예: 리드타임 중앙값이 며칠). 있는 그대로 보여주고, 작은 표본은 하한으로 숨긴다.
- 스크린샷은 UI 가 바뀌면 낡는다 — 재촬영 절차를 커밋 로그에 남긴다.
- `JOIN FETCH` 쿼리의 selections 변환과 필드 매핑은 프로젝션 테스트로 확인한다.

## 9. 범위 밖 (보고만)

- 기사·use-case 페이지에 데이터·스크린샷 보강
- `RecapService` 의 요일 집계가 기존 모드 일정도 `indexToDate`(확장 모드 규칙)로 바꿔 요일이 어긋나는 문제 — `YearMonth.dateOf` 로 고칠 수 있으나 별도 작업
- 캐시, 월간 스냅샷 페이지, 연락처 이메일 변경, 인원수별 전원 가능일 확률
