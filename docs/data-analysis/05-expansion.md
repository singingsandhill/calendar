# 05. Expansion — 확장 가능성 / 미래 아이디어

P3 보다 더 추상적인 가능성. "이게 가능한가?" 의 답을 모아둔다. 우선순위는 정하지 않음.

---

## A. 데이터 모델 확장

### A1. 시간대 분석

**가능성**: 현재 selections JSON 은 day index 만 저장. 시간대(예: 오전/오후/저녁) 까지
추가하면 사용자가 어느 시간대를 선호하는지 알 수 있다.

**구현**: `Participant.selections` 를 `Map<Integer, List<TimeSlot>>` 로 확장. JSON 컨버터 변경.
도메인 변경 비중 큼.

**가치**: 그룹 일정의 진짜 sweet spot 시간 → 운영 통계 + 마케팅 메시지 ("팀 회식의 70% 가
화요일 저녁 7시" 류).

### A2. 지역 클러스터링

**가능성**: location.name 은 자유 텍스트. 한국 도시·구·동 분류로 클러스터링하면 지역별
일정 분포·인기 장소를 알 수 있다.

**구현**:
- 단순: 키워드 매칭 (강남, 홍대, 종로 등)
- 정확: 카카오맵 API 또는 네이버 지도 API 로 geocoding
- ML: BQ ML 텍스트 분류

**가치**: `/insights/trends` 페이지에 "이번 달 강남에서 가장 인기 있는 일정" 섹션 → 자체
SEO 콘텐츠.

### A3. 그룹 다이내믹스

**가능성**: 한 owner 의 여러 일정에서 반복 등장하는 participant_name 추적 → 친구 그룹 식별.

**구현**: SQL window function 으로 same-name participant 의 schedule 시퀀스 추출.

**가치**: "친구 그룹 평균 사이즈", "그룹 간 합치는 패턴" 같은 사회적 분석.

> 주의: PII. 이름은 anonymize 후 분석.

### A4. 의사결정 시간 모델링

**가능성**: schedule.created_at → first selections_saved → first vote_cast → 이후 멈춤
까지의 시계열. 그룹의 "결정 속도" 를 측정.

**가치**: 알림 push 기능의 ROI 검증. "3일 이상 movement 없는 일정에 알림" 같은 기능.

---

## B. 실험 / A/B 테스트

| 가설 | 메트릭 |
|---|---|
| 기본 weeks 7 → 4 변경 시 일정 단순화로 conversion ↑ | schedule_created / page_view |
| onboarding 배너 강제 표시 | participant_added / schedule_created |
| /use-cases CTA 색상·위치 변경 | use-case → schedule_created funnel |
| 한국어 번역 톤 변경 (격식 → 캐주얼) | engagement time |
| 메뉴 추가 시 자동 검색 제안 (BQ 인기 데이터 기반) | menu add count |

**구현 방식**:
- 서버 분기 + GA4 user_property `experiment_id`
- GTM client-side 분기 (간단, SEO 위험)
- Optimizely / VWO (외부 도구)

---

## C. 자동 인사이트 / 콘텐츠 생성

### C1. Trending Topics 페이지

**가능성**: BQ 의 메뉴/장소 트렌드를 매일 새 페이지로 발행.

```
/insights/2026-05-09  ← 오늘 인기 메뉴/장소 + 가용 일자
```

**가치**: 매일 새 인덱싱 가능 페이지 → SEO long-tail 트래픽.

**주의**: 자동 생성 페이지의 thin content / spam 위험. Google 가이드라인 준수.

### C2. Use-case 자동 추천

**가능성**: 검색 키워드 (GSC) → 해당 키워드를 노린 새 use-case 페이지 후보 LLM 추천.

**구현**: BQ 의 GSC 데이터 → Anthropic API → 슬러그·title·description·step 5개 생성 → PR
초안 자동 생성.

**가치**: 콘텐츠 마케팅 자동화. 인간 검수 후 머지.

### C3. Personalized Recommendations

**가능성**: owner 의 과거 일정에서 참가자·메뉴·장소 패턴 → 새 일정 만들 때 자동 추천.

**구현**:
- 단순: SQL 으로 same owner 의 top-k 메뉴/장소
- ML: collaborative filtering (참가자 이름이 유사한 다른 owner 의 메뉴 추천)

**가치**: 일정 생성 마찰 ↓.

---

## D. 비즈니스 모델 데이터

### D1. 페이지별 광고 RPM 분석

AdSense 승인 후. 어느 페이지가 RPM 높은가 → 트래픽 유입 우선순위.

### D2. 콘텐츠 마케팅 ROI

`/use-cases/{slug}` 별 SEO 비용(시간·번역) vs 가져온 conversion · 광고 수익. 슬러그별
ROI 랭킹.

### D3. owner LTV 코호트

월별 코호트의 schedule 생성·활동 시퀀스. 본 서비스의 retention curve. SaaS 식 LTV 가
의미는 없지만 "재사용율" 은 product 가치 핵심 지표.

### D4. Premium 기능 가설 검증

가능한 유료 기능들:
- 일정당 참가자 한도 8 → 30
- 7주 → 12주 확장 모드
- 일정 자동 알림 (이메일/카톡)
- 광고 제거

→ 각 기능을 사용 빈도가 높은 owner 세그먼트에 노출 → 가격 수용성 가설 검증.

---

## E. 인프라 확장

### E1. 분석 도구 다양화

| 도구 | 용도 | 본 프로젝트 적합도 |
|---|---|---|
| **Looker Studio** | 일반 대시보드 | ✅ 현재 사용 |
| **Metabase** (self-host) | SQL 워크벤치 | △ 운영비, 서비스 규모 작음 |
| **Superset** (self-host) | 같음, BI 강화 | △ 같음 |
| **Hex / Mode** (SaaS) | notebook 형 분석 | △ 비용 |
| **Streamlit** (self-host) | Python 대시보드 | ○ 작은 도구 빠르게 배포 |
| **Jupyter / Colab** | ad-hoc 분석 | ✅ 무료, 즉시 사용 |

### E2. 데이터 처리 파이프라인

| 도구 | 용도 |
|---|---|
| **Cloud Functions** | 작은 ETL job |
| **Cloud Run** | 컨테이너 ETL |
| **Cloud Composer (Airflow)** | 복잡한 DAG |
| **Dataflow** | streaming + batch |
| **dbt Cloud** | SQL 데이터 모델링 |

본 프로젝트는 Spring Boot 스케줄러 + GCS dump 로 충분. dbt 만 도입하면 됨.

### E3. 데이터 거버넌스

| 항목 | 작업 |
|---|---|
| **PII 마스킹** | owner_id 해시, voter_name 미송신 (현재 정책) |
| **GDPR 준수** | Consent Mode v2, 사용자 삭제 요청 처리 절차 |
| **BQ 비용 가드** | 월 임계 알림, slot reservation 검토 |
| **IAM** | analyst (read-only), engineer (write), admin |
| **데이터 카탈로그** | Data Catalog 또는 dbt docs |

### E4. ML / AI 통합

| 응용 | 도구 |
|---|---|
| 메뉴/장소 카테고리 자동 분류 | BQ ML, Vertex AI |
| 이상 트래픽 탐지 | BQ ML anomaly detection |
| 검색 키워드 → 콘텐츠 추천 | Anthropic API (LLM) |
| 매뉴얼 데이터 라벨링 | Label Studio |

---

## F. 외부 통합

### F1. 마케팅 도구

- **Google Ads** + GA4 audience export → 리마케팅
- **Facebook/Meta Ads** + Meta Pixel (별도 설치 필요)
- **TikTok Pixel** — 한국 젊은 사용자

### F2. 커뮤니케이션

- **Slack / Discord webhook** — 일별 메트릭 자동 알림
- **이메일 다이제스트** — owner 에게 본인 일정 통계 정기 발송 (PII 동의 필요)
- **카카오톡 알림** — 한국 시장 특화

### F3. 데이터 공유

- **공개 트렌드 페이지** — `/insights/trends` 의 정적 BQ 결과를 서빙 (이미 SEO)
- **연구자용 dataset 공개** — anonymized data 를 Kaggle / GitHub 에 (보안 검토 필요)
- **API 공개** — 다른 서비스가 인기 메뉴/장소를 가져갈 수 있게

---

## G. 본 프로젝트의 제약 (현실 체크)

데이터 분석 인프라가 강력해도 다음 제약은 변하지 않음:

1. **트래픽 규모 작음** — 통계적 유의성 확보에 데이터 필요. 일 PV 가 수만 미만이면 A/B 테스트 N 부족 → 정량 분석보다 정성 분석 (UX 인터뷰) 비중 키워야 함.
2. **로그인 없음** — `user_id` 강제 매칭 불가. user_pseudo_id 의 cross-device 매칭만 가능.
3. **UGC noindex 정책** — owner 페이지 검색 트래픽 0. 분석 의미는 진입 후 행동에만.
4. **개발자 1인** — dbt, Airflow, ML 등 인프라 추가 시 운영 부담. ROI 명확한 것만.
5. **수익 모델이 광고 일변** — AdSense 미승인 동안 수익 모델 검증 막힘. P1-2 통과가 핵심.

---

## H. 우선 검토 추천

위의 모든 가능성 중, ROI 대비 작업량이 가장 좋은 5개:

1. **GTM 매핑 + 맞춤 측정기준** (P0-1, P0-2) — 이미 코드는 있고, GA4 가치 활성화의 게이트
2. **DB → BQ 미러링** (P1-1) — 결합 분석의 게이트
3. **Bing/Naver Webmaster Tools 등록** (P1-3) — 0 비용, 즉시 색인 데이터 확보
4. **/insights/trends 자동 갱신** (P2-5) — 자체 SEO 콘텐츠 자동화, content marketing 효과
5. **owner 코호트 retention 분석** (DB 단독) — 본 product 의 핵심 KPI 정의

나머지는 위 5개 결과 본 후 판단.
