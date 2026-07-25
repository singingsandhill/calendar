# 04. TODO — 우선순위 백로그

P0 (즉시) → P1 (단기, 1~2주) → P2 (중기, 1~2개월) → P3 (장기). 각 항목에 (1) 이유, (2) 작업,
(3) 차단 요건, (4) 검증 기준.

---

## P0 — 즉시 적용 (코드 또는 콘솔 작업, 의존성 없음)

### P0-1. GTM 트리거·태그 매핑 (이벤트 → GA4 송신 활성화)

**왜**: dataLayer 푸시 6종이 이미 코드에 들어갔지만 GTM 측 매핑 없으면 GA4 로 송신 안 됨.

**작업** (`tagmanager.google.com → GTM-PFPKQT7W → 작업공간`):

1. **사용자 정의 변수** (Data Layer Variable 유형):
   `DLV - owner_id`, `DLV - schedule_id`, `DLV - target`, `DLV - target_id`, `DLV - action`, `DLV - days_count`, `DLV - participant_count_after`, `DLV - category`, `DLV - run_id`, `DLV - distance`, `DLV - year`, `DLV - month`,
   `DLV - share_method`, `DLV - location_count_after`, `DLV - menu_count_after`, `DLV - is_owner`, `DLV - participant_count`, `DLV - owner_id_hash`, `DLV - schedule_count`

2. **트리거 1개** (Custom Event, 정규식):
   - 이름: `Custom - Business Events`
   - 이벤트 이름: `^(schedule_created|participant_added|selections_saved|vote_cast|run_created|attendance_marked|link_shared|location_added|menu_added|schedule_viewed|owner_dashboard_viewed)$`
   - "이벤트 이름 일치 정규식 사용" 체크

3. **태그 1개** (GA4 Event):
   - 구성 태그: 기존 Google Tag "datedate" (`G-9QTMK4CDDF`)
   - 이벤트 이름: `{{Event}}` (built-in, dataLayer 의 event 그대로 전달)
   - 이벤트 매개변수: 위 19개 DLV 매핑 (없는 키는 빈 문자열로 안전)
   - 트리거: `Custom - Business Events`

4. **DebugView 검증** → 사이트에서 실제 행동 → 이벤트 흐름 확인 → **게시**

**검증**: GA4 → 보고서 → Realtime 에서 이벤트 카운트가 0 이상으로 증가.

### P0-2. GA4 맞춤 측정기준 등록

**왜**: 이벤트 파라미터(`owner_id`, `target`, `action` 등)가 GA4 UI 와 BQ 컬럼으로
표시되려면 사전 등록 필요. 등록 후 **이후** 이벤트만 적용 (소급 X).

**작업** (`analytics.google.com → 관리 → 맞춤 정의 → 맞춤 측정기준`):

| 등록 이름 | 매개변수 | 범위 | 우선순위 |
|---|---|---|---|
| Owner ID | `owner_id` | event | P0 |
| Schedule ID | `schedule_id` | event | P0 |
| Vote Target | `target` | event | P0 |
| Vote Action | `action` | event | P0 |
| Days Count | `days_count` | event | P1 |
| Participant Count After | `participant_count_after` | event | P1 |
| Run Category | `category` | event | P1 |
| Run ID | `run_id` | event | P1 |
| Distance | `distance` | event | P1 |
| Vote Target ID | `target_id` | event | P2 |
| Schedule Year | `year` | event | P2 |
| Schedule Month | `month` | event | P2 |
| Share Method | `share_method` | event | P1 |
| Location Count After | `location_count_after` | event | P1 |
| Menu Count After | `menu_count_after` | event | P1 |
| Is Owner | `is_owner` | event | P0 |
| Participant Count | `participant_count` | event | P1 |
| Owner ID Hash | `owner_id_hash` | event | P0 |
| Schedule Count | `schedule_count` | event | P1 |

> 총 측정기준 수: 12 (기존) + 7 (본 작업) = 19. 무료 한도 50 내.

**제한**: 무료 GA4 속성은 이벤트 범위 맞춤 측정기준 50개 한도. 본 프로젝트엔 충분.

**검증**: 등록 24~48시간 후 Explorations 에서 측정기준 사용 가능.

### P0-3. troubleshooting 문서 정정 PR 머지

**왜**: 이미 코드 작업으로 정정 완료됨. 단계 5 dataLayer PR과 묶어서 머지.

**작업**: `docs/troubleshooting/lighthouse-performance-audit.md` B 항목 정정 (이미 적용).

---

## P1 — 단기 (1~2주)

### P1-1. DB → BigQuery 미러링

**왜**: GA4 events 와 도메인 데이터 결합 분석이 막혀있음. [03 §D](03-analysis-playbook.md#d-bq--db-결합--미러링-활성-후) 의 모든 쿼리가 이 작업에 의존.

**작업 (권장: GCS daily JSONL dump)**:

1. Spring Boot 스케줄러 `WarehouseExportScheduler` 추가:
   - 매일 KST 02:00 (BQ 적재 04:00 직전)
   - 모든 도메인 테이블 (owners, schedules, participants, locations, location_votes, menus, menu_votes, runs, attendances) 을 `JSONL` 로 직렬화
   - GCS 버킷 (`gs://datedate-warehouse/exports/{table}_{YYYYMMDD}.jsonl`) 업로드
2. BQ 외부 테이블 등록:
   ```sql
   CREATE EXTERNAL TABLE `proj.warehouse.schedules` OPTIONS (
     format = 'NEWLINE_DELIMITED_JSON',
     uris = ['gs://datedate-warehouse/exports/schedules_*.jsonl']
   );
   ```
3. 또는 daily snapshot 으로 나누려면 `warehouse.schedules_YYYYMMDD` partitioned table.

**차단 요건**:
- GCP 프로젝트 결제 활성 (BQ export 활성 시 이미 충족)
- GCS 버킷 + 서비스 계정 권한
- 운영 DB 접근 (현재 H2 file 인지, 운영 DB 가 다른 형태인지 확인)

**검증**: BQ 에서 `SELECT COUNT(*) FROM warehouse.schedules` 가 도메인 DB count 와 일치.

### P1-2. AdSense 사이트 승인 후 GA4 연결

**왜**: 광고 수익 페이지별 분석. 콘텐츠 페이지의 진짜 경제적 가치 측정.

**작업**:
1. AdSense 콘솔에서 사이트 검토 신청 + 통과 대기 (보통 며칠~수주)
2. 통과 후 GA4 → 관리 → 제품 링크 → AdSense → 연결 (이미 절차 정리됨)
3. `templates/fragments/ad-slot.html` 의 placeholder `XXXXXXXXXX` → 실 슬롯 ID 채우기 또는 슬롯 폐기

**차단 요건**: AdSense 측 사이트 승인 (외부 의존)

**검증**: GA4 → 수익 창출 → 게시자 광고 보고서 데이터 표시.

### P1-3. Bing / Naver Webmaster Tools 콘솔 등록

**왜**: 검증 파일과 IndexNow 인프라는 이미 있는데 콘솔 미등록이면 데이터 사용 불가.

**작업**:
1. Bing Webmaster Tools (`bing.com/webmasters`) 에 `https://datedate.site/` 추가 → IndexNow 키 자동 인식
2. Naver Search Advisor (`searchadvisor.naver.com`) 에 사이트 추가 → 검증 파일 (`naver52cf...html`) 자동 통과
3. (옵션) Yandex Webmaster — 한국 트래픽 위주면 후순위

**차단 요건**: 없음 (검증 파일 이미 배포됨)

**검증**: 각 콘솔에서 사이트맵 제출 + 색인 통계 표시.

### P1-4. IndexNow 운영 활성 확인 + 모니터링

**왜**: 코드는 있지만 `INDEXNOW_ENABLED` 환경변수 상태 미확인.

**작업**:
1. 운영 환경 `INDEXNOW_ENABLED=true` 설정 확인
2. 매일 03:30 KST 로그에서 `IndexNow submitted N urls, status=200/202` 확인
3. fail 시 패턴 모니터링 (403 → 키 파일 접근 안 됨, 422 → host 불일치)

**검증**: `IndexNowService.logResponse` 의 200/202 비율 90% 이상.

### P1-5. Looker Studio 통합 대시보드

**왜**: BQ + GSC + GA4 + (AdSense 승인 후) 광고 수익을 한 화면에.

**작업**: [03 §E](03-analysis-playbook.md#e-looker-studio-권장-대시보드-구성) 의 4개 대시보드 구축. P1-1 (DB 미러링) 완료 후 E3, E4 가능.

**검증**: 매주 자동 새로고침되는 대시보드 URL 공유.

---

## P2 — 중기 (1~2개월)

### P2-1. owner_id 해시 user_property 송신

**왜**: GA4 정책상 PII 우려. owner_id 가 사용자 입력 슬러그라 PII 가능성.

**작업**:
1. 클라이언트 측 SHA-256 해시 (Web Crypto API)
2. dataLayer 이벤트의 `owner_id` 값을 해시값으로 교체
3. 또는 user_property 로 송신 (cross-device 매칭에 더 적합)

```javascript
async function hashOwnerId(ownerId) {
  const buf = new TextEncoder().encode(ownerId);
  const hash = await crypto.subtle.digest('SHA-256', buf);
  return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
}
```

**검증**: BQ events 의 `event_params.value.string_value` 가 64자 hex 문자열인지.

### P2-2. AdSense 슬롯 placeholder 정리

**왜**: `templates/fragments/ad-slot.html` 의 `data-ad-slot="XXXXXXXXXX"` 가 헤드의 실 client `ca-pub-7334667748813914` 와 불일치. 광고가 실제로 어떻게 뜨는지 검증 필요.

**작업**:
1. AdSense 콘솔에서 슬롯 ID 발급 (Display ad / In-feed ad)
2. `templates/fragments/ad-slot.html` 의 placeholder 교체
3. 또는 슬롯 폐기 (AdSense Auto Ads 만 사용)

**차단 요건**: P1-2 (AdSense 승인) 완료

### P2-3. Consent Mode v2 (EU 트래픽 의미 있어지면)

**왜**: 2024.03 이후 EU/EEA/UK 사용자에 광고 표시하려면 GDPR 동의 필수. 미적용 시 EU 광고 매출 점진적 0.

**작업 옵션**:
- **Funding Choices** (Google 무료 CMP): GTM 한 줄 추가
- CookieYes / Cookiebot: 유료, 더 많은 옵션

**판단 기준**: BQ 의 `geo.country` 분포로 EU 트래픽 비중 측정. 5% 이상이면 즉시.

### P2-4. dbt 모델 작성

**왜**: BQ 쿼리가 늘어나면 DRY · 버전관리 · 테스트 가능한 데이터 모델 필요.

**작업**:
1. `dbt-bigquery` 프로젝트 생성
2. staging 모델: `stg_events`, `stg_owners`, `stg_schedules` 등
3. mart 모델: `mart_owner_ltv`, `mart_use_case_funnel`, `mart_schedule_lifecycle`

**검증**: dbt test 통과, 매일 자동 빌드.

### P2-5. 인사이트 페이지 자동 갱신

**왜**: `/insights/trends` 가 현재 PopularityService 의 시간 가중 점수만 노출. BQ 분석 결과를 SEO 페이지로 직접 노출하면 자체 검색 가치 ↑.

**작업**:
1. BQ 분석 결과 (요일별 가용성, 인기 메뉴 카테고리 등) 를 daily 로 캐싱
2. `InsightsService` 에서 캐시 조회
3. `/insights/trends` 에 서브 섹션 추가

---

## P3 — 장기 (선택)

### P3-1. Server-side GTM (`tag.datedate.site`)

**왜**: Safari ITP 대응, first-party 쿠키.

**작업**: GCP / Cloud Run 에 sGTM 컨테이너 배포, 서브도메인 매핑.

**비용**: 운영 비용 + 설정 복잡도. 트래픽 규모 임계 도달 후 검토.

### P3-2. ML 기반 메뉴/장소 자동 카테고리 분류

**왜**: 현재 메뉴/장소는 자유 텍스트. 카테고리 자동 분류로 트렌드 분석 정확도 ↑.

**작업**:
1. BigQuery ML 또는 Vertex AI 로 텍스트 분류 모델
2. 또는 LLM API (Anthropic / OpenAI) 로 일별 batch 분류
3. 결과를 `menu_category`, `location_category` 컬럼에 누적

### P3-3. A/B 테스트 인프라

**왜**: 기능 개선 효과를 측정 가능하게.

**작업**:
1. 서버 분기 + GA4 user_property 로 그룹 식별
2. 또는 GTM 기반 client-side 분기 (간단하나 SEO 영향)
3. BQ 에서 두 그룹 conversion 비교

**예시 실험**:
- 기본 weeks 4 vs 7 (현재 7)
- onboarding 배너 dismiss vs not
- /use-cases CTA 위치 변경

### P3-4. Real-time 모니터링 (BQ streaming + Cloud Run)

**왜**: 일정 생성 spike, 봇 트래픽 이상 감지.

**작업**:
1. BQ streaming export 활성화 (월 비용 추가)
2. Cloud Run 에 실시간 알림 서비스 (`events_intraday_*` 폴링 → Slack)

### P3-5. Data Warehouse 정식화

**왜**: 분석 사용자가 늘면 BQ 권한, 비용, 거버넌스 정리 필요.

**작업**:
1. IAM 역할 분리 (analyst, engineer, admin)
2. BQ 데이터 보존 정책 (이벤트 90일, mart 1년, agg 영구)
3. 비용 모니터링 알림 (월 임계값)

---

## 의존 그래프

```
P0-1 (GTM 매핑) ─── 필수 ───┐
P0-2 (맞춤 측정기준) ───────┤
                         ↓
                  [GA4 → BQ 데이터 흐름 활성]
                         ↓
P1-1 (DB → BQ) ─── 결합 분석 가능 ───┐
P1-5 (Looker Studio E1, E2) ───────┤
                                  ↓
P1-5 (Looker Studio E3, E4) [DB 결합 대시보드]
P2-4 (dbt) [데이터 모델링 본격화]

P1-2 (AdSense 승인) → P1-2 (GA4 연결) → P2-2 (슬롯 정리) → P3-1 (sGTM 검토)

P0-1 → 행동 데이터 누적 → P3-3 (A/B 테스트 가능)
```

## 차단·외부 의존

| 작업 | 차단 요인 |
|---|---|
| P1-2 | AdSense 사이트 승인 (Google 측) |
| P1-1 | 운영 DB 형태 (H2 file vs 다른 RDB) |
| P3-2 | LLM API 비용 + 카테고리 정의 합의 |
| P3-3 | UX 변경 의사결정 |
