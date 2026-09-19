# Runner Module

Running crew (97 Runners) attendance management. Domain: `Run (1) -- (*) Attendance`.

> 결정 근거: [`docs/adr/runner/`](../../../../../../../docs/adr/runner/) — anonymous
> 런 생성 vs 어드민 전용 삭제 정책.

- 런 생성 / 출석 마킹은 인증 없이 가능 (서버 비용 절감 + 가입 마찰 제거,
  [ADR-0001](../../../../../../../docs/adr/runner/0001-anonymous-run-creation.md)).
- 출석 삭제 / 어드민 행위는 ROLE_ADMIN 폼 로그인
  ([ADR-0002](../../../../../../../docs/adr/runner/0002-attendance-deletion.md)).
- 어드민 자격증명: `runner.admin.username` / `runner.admin.password` 프로퍼티.
  운영 환경에선 환경변수로 주입.
