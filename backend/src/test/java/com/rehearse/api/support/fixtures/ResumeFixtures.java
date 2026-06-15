package com.rehearse.api.support.fixtures;

import java.util.List;
import java.util.Set;

public final class ResumeFixtures {

    private ResumeFixtures() {
    }

    public record ResumeFixture(
            String label,
            String resumeText,
            Set<String> vocabulary,
            int passThreshold,
            String injectAnswer,
            Set<String> injectVocabulary
    ) {
    }

    public static List<ResumeFixture> all() {
        return List.of(backend(), fullstack(), embedded());
    }

    public static ResumeFixture backend() {
        String resume = """
                경력 2년차 백엔드 개발자 (Java/Spring)

                ## 주문 조회 캐싱 개선 프로젝트
                - 역할: 백엔드 단독 / API 레이어 + 캐시 레이어 설계
                - 기술: Spring Boot, Redis, MySQL, JPA
                - 아키텍처: Spring Boot REST API + Redis Cache-Aside + MySQL primary

                ### 성과
                - 주문 조회 API p95 응답시간 800ms → 120ms 단축 (Redis Cache-Aside 도입)
                - JPA fetch join 으로 N+1 문제 해결
                - TTL 5분 정책 + cache stampede 방어 (single-flight)

                ### 의사결정
                - Memcached vs Redis → Redis 채택 (TTL 정책 / Lua 스크립트 필요)
                - Cache-Aside vs Write-Through → Cache-Aside 채택 (쓰기 빈도 낮음)
                """;
        return new ResumeFixture(
                "backend",
                resume,
                Set.of("Redis", "캐시", "Cache-Aside", "TTL", "MySQL"),
                4,
                "처음엔 Memcached 도 검토했지만 TTL 정책 필요해서 Redis 채택했어요",
                Set.of("Memcached", "TTL")
        );
    }

    public static ResumeFixture fullstack() {
        String resume = """
                경력 3년차 풀스택 개발자 (React/Node)

                ## 실시간 협업 보드 프로젝트
                - 역할: 프론트 + 백엔드 양방향 / WebSocket 채널 설계
                - 기술: React, Node.js, Express, WebSocket, MongoDB, Redis Pub/Sub
                - 아키텍처: React SPA + Node WebSocket Gateway + Redis Pub/Sub + MongoDB

                ### 성과
                - 동시 접속 1000명 환경에서 메시지 지연 200ms 이내 유지
                - Redis Pub/Sub 으로 다중 인스턴스 메시지 브로드캐스트
                - React Query 로 낙관적 업데이트 + 충돌 해결 적용

                ### 의사결정
                - Socket.IO vs native WebSocket → native WebSocket 채택 (의존성 최소화)
                - 단일 서버 in-memory vs Redis Pub/Sub → Redis Pub/Sub 채택 (수평 확장)
                """;
        return new ResumeFixture(
                "fullstack",
                resume,
                Set.of("WebSocket", "React", "Pub/Sub", "Node", "MongoDB"),
                4,
                "처음엔 Socket.IO 고려했지만 의존성 줄이려고 native WebSocket 으로 갔어요",
                Set.of("Socket.IO", "의존성")
        );
    }

    public static ResumeFixture embedded() {
        String resume = """
                경력 4년차 임베디드 개발자 (C/C++)

                ## 산업용 IoT 게이트웨이 펌웨어 프로젝트
                - 역할: 펌웨어 단독 / 센서 드라이버 + 통신 스택 구현
                - 기술: C, FreeRTOS, MQTT, Modbus RTU, ARM Cortex-M4
                - 아키텍처: FreeRTOS task 기반 + Modbus RTU 시리얼 + MQTT over TLS uplink

                ### 성과
                - 센서 폴링 주기 100ms 안정화 (인터럽트 기반 DMA 적용)
                - MQTT QoS 1 + 로컬 큐 보관으로 패킷 유실 0건
                - 펌웨어 OTA 업데이트 dual bank 적용으로 brick 위험 차단

                ### 의사결정
                - bare-metal vs FreeRTOS → FreeRTOS 채택 (다중 task 우선순위 필요)
                - MQTT QoS 0 vs QoS 1 → QoS 1 채택 (산업 환경 패킷 유실 방지)
                """;
        return new ResumeFixture(
                "embedded",
                resume,
                Set.of("FreeRTOS", "MQTT", "Modbus", "센서", "OTA"),
                4,
                "처음엔 bare-metal 검토했지만 task 우선순위 필요해서 FreeRTOS 로 갔어요",
                Set.of("bare-metal", "task")
        );
    }
}
