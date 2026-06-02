# Known Issues — 554-loadtest-environment

## KI-1: dev/prod `/actuator/prometheus` 무인증 외부 노출 (OWASP A05)

- **발견**: 554 Task 1 위험4 보안 확인 중 (별개 기존 이슈).
- **현상**: base `application.yml` `management.endpoints.web.exposure.include: health, info, prometheus` = 전 프로파일 무조건 상속 + `SecurityConfig.java` `/actuator/**` = `permitAll()` → dev/prod 운영 서버의 `/actuator/prometheus` 가 인증 없이 외부 노출. JVM heap / DB 커넥션 / HTTP 통계 등 내부 메트릭 조회 가능.
- **미확인**: 인프라단(SG/ALB) 에서 actuator 경로 외부 차단 중이면 실노출은 막혀있을 수 있음.
- **결정 (2026-06-02)**: 별도 보안 PR 로 분리 처리 (보류). 사유 — 현 브랜치 = loadtest 환경 스코프. actuator 보안 수정은 기존 Grafana/모니터링 연동 영향 확인 필요 → 별도 추적이 안전.
- **후속 액션**: 별도 PR 에서 (a) 인프라 차단 여부 확인 → (b) 미차단 시 prometheus 엔드포인트를 내부망/인증 한정으로 제한.
