-- plan-07: InterviewRuntimeState.startedAt 은 in-memory POJO 필드(Caffeine 캐시)이므로
-- DB 마이그레이션 불필요. 세션 시작 시각은 휘발성 데이터로 재시작 시 재계산 허용.
-- 이 파일은 V28 이후 버전 순서를 유지하기 위한 플레이스홀더이며, 실질적 DDL 변경 없음.
SELECT 1;
