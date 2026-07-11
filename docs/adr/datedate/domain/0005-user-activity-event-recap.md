# 0005. 활동 이벤트 테이블 기반 연간 recap (voters 구조 무변경)

- Status: Accepted
- Date: 2026-07-11

## Context

연간 recap ("만든 일정·참여·투표 돌아보기") 에는 사용자별 기록이 필요하다.
그러나 기존 구조에서 참여자는 이름 문자열, 투표는 Location/Menu 의 voters
문자열 리스트라 사용자 연결 지점이 없다. 요구사항은 "카카오 로그인 상태에서
한 참여·투표 기록만 연결" (소급 없음, 익명 병행 유지).

## Decision

1. **append-only `UserActivity` 이벤트 테이블.** (userId, type, scheduleId,
   targetId, detail, occurredAt). 기존 voters/Participant 구조는 그대로 두고,
   로그인 세션의 컨트롤러 성공 경로에서만 1행 기록한다. 대안이었던 "기존
   테이블에 user_id 컬럼 추가"는 투표가 JSON/문자열 구조라 대수술이 필요해
   기각.
2. **중복 방지 키 (userId, type, targetId).** targetId 는
   PARTICIPATION=participantId, *_VOTE=location/menu id. participantId 를
   보존해 recap 이 Participant.selections 를 조인, "선택한 날짜 수"를 집계.
   중복 방지는 DB unique 제약이 아니라 **서비스 레이어의 exists-check** 로
   구현한다 — 동시 요청이 겹치면 중복 기록이 남을 가능성을 수용한다(§Consequences).
3. **기록 실패는 본 동작을 막지 않는다.** REQUIRES_NEW + 예외 삼킴(WARN).
   공유 토큰 생성 경로에도 연도 범위 검증(2024~현재)을 동일하게 적용해,
   존재할 수 없는 연도의 고아 토큰이 발급되지 않도록 한다.
4. **오너 연결은 first-claim.** ownerId 는 공개 슬러그로 소유 증명이 원천
   불가 → 선점 정책 수용, 타 유저 선점 시 409. 악의 선점 구제는 수동(DB).
   같은 이유로 **동시성 race 가 존재한다**: 두 사용자가 동시에 미연결 오너를
   연결 시도하면 낙관적 락이 없어 나중에 커밋된 요청이 조용히 승자가 된다.
   소유 증명이 원천 불가한 구조에서 선점 정책의 하위 케이스로 수용하며,
   문제가 보고되면 관리자가 DB 를 수동으로 해제한다.
5. **recap 은 on-the-fly 집계** (스냅샷 없음). 오너 계열(내 오너들의 해당
   연도 일정)+활동 계열(내 이벤트). 데이터 규모상 충분, 필요 시 캐시는 후속.

## Consequences

- 익명 사용자 경로는 바이트 단위로 무변경 (회귀 가드:
  `ParticipantApiActivityRecordingTest`).
- 로그인 이전 기록은 recap 에 나타나지 않는다 (의도된 제약).
- 공유 페이지(/recap/share/{token})는 공개 URL — 닉네임·집계 수치만 노출,
  프로필 이미지 미노출, noindex.
- `(userId, type, targetId)` 중복 방지는 DB unique 제약이 아닌 서비스 레이어
  exists-check 이므로, 동시 요청으로 인한 중복 활동 기록 가능성이 이론상
  남는다 — recap 은 집계용 참고 지표이며 결제·정산과 무관해 리스크를 수용.
- 오너-사용자 연결 동시 요청 race (Decision 4) 도 같은 이유로 수용하며, 별도
  낙관적 락/트랜잭션 격리 강화는 하지 않는다.
