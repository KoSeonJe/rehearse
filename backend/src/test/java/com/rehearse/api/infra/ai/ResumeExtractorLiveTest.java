package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.ResumeExtractionService;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Resume Extractor Live LLM 통합 검증.
 *
 * <h3>실행 방법</h3>
 * <pre>
 * export OPENAI_API_KEY=sk-...
 * RUN_LIVE_API=true ./gradlew test --tests "ResumeExtractorLiveTest"
 * </pre>
 *
 * <p>비용 발생 + 비결정성 → CI 미실행. 추출 프롬프트 / 스키마 회귀 의심 시 수동 실행.</p>
 *
 * <p>검증 목표: 신 스키마 (`projects[].project_name`) 가 실 LLM 호출에서 모두 채워지는지 확인.
 * 명시 명칭 / 명칭 부재 → 요약 명칭 양 케이스 분리.</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
@SpringBootTest
@ActiveProfiles({"test", "llm-e2e"})
class ResumeExtractorLiveTest extends AbstractMySqlContainerTest {

    @Autowired
    private ResumeExtractionService extractionService;

    @BeforeAll
    static void requireApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        assumeTrue(key != null && !key.isBlank() && !"disabled".equals(key),
                "OPENAI_API_KEY 환경변수 누락 — Live extractor 테스트 스킵");
    }

    @Nested
    @DisplayName("Live extractor — projectName 적재")
    class ProjectNameExtraction {

        @Test
        @DisplayName("다수 명시 프로젝트 + 명칭 부재 프로젝트 혼합 → 모두 non-blank projectName 채움")
        void extractsProjectName_for_explicit_and_missing_cases() {
            String resumeText = """
                    경력 3년차 백엔드 개발자

                    ## 프로젝트 경험

                    ### 1. 주문 캐싱 개선 프로젝트
                    - 주문 조회 API p95 응답시간을 800ms → 120ms 로 개선 (Redis Cache-Aside 도입)
                    - Cache stampede 방지를 위해 single-flight 패턴 적용 (Redis SETNX)
                    - 기간: 2024.01 - 2024.06

                    ### 2. 결제 시스템 마이그레이션
                    - 모놀리식 결제 모듈을 Kafka 기반 이벤트 드리븐 아키텍처로 분리
                    - SAGA 패턴 도입으로 결제/적립금/포인트 분산 트랜잭션 처리
                    - 기간: 2024.07 - 2024.12

                    ### (프로젝트명 없는 항목)
                    - 사내 인증 토큰 발급 시스템 리팩토링 — JWT refresh 토큰 회전 도입
                    - rate limiting 도입 (Bucket4j + Redis) 으로 무차별 대입 방어
                    """;
            String fileHash = "live-test-hash-" + System.currentTimeMillis();

            ResumeSkeleton skeleton = extractionService.extract(resumeText, fileHash);

            assertThat(skeleton).isNotNull();
            assertThat(skeleton.projects())
                    .as("이력서에서 최소 1개 이상의 project 가 추출되어야 한다")
                    .isNotEmpty();

            for (Project project : skeleton.projects()) {
                assertThat(project.projectName())
                        .as("projectId=%s 의 projectName 은 non-blank 여야 한다", project.projectId())
                        .isNotBlank();
                assertThat(project.projectName().length())
                        .as("projectId=%s 의 projectName 길이는 1자 이상 60자 이하여야 한다", project.projectId())
                        .isBetween(1, 60);
            }

            // 명시 명칭 케이스가 1건 이상 LLM 결과에 등장해야 한다 (정확 매칭 대신 핵심 어휘 부분 검증).
            // 비결정성 회피: "캐싱" / "결제" 둘 중 하나는 어느 project 명에 포함되어야 한다.
            boolean explicitNamePresent = skeleton.projects().stream()
                    .anyMatch(p -> p.projectName().contains("캐싱")
                            || p.projectName().contains("결제")
                            || p.projectName().contains("주문"));
            assertThat(explicitNamePresent)
                    .as("이력서 명시 명칭의 핵심 어휘 (캐싱 / 결제 / 주문) 가 추출 결과에 포함되어야 한다")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Live extractor — 메타 4종 + claim 적재")
    class MetaAndClaimExtraction {

        @Test
        @DisplayName("백엔드 이력서 → tech_stack / role / architecture / decisions / claims 모두 의미 있게 채워진다")
        void extracts_meta_fields_and_claims_for_backend_resume() {
            String resumeText = """
                    경력 2년차 백엔드 개발자 (Java/Spring)

                    ## 주문 조회 캐싱 개선 프로젝트 (2024.01 - 2024.06)
                    - 역할: 백엔드 단독 / API 레이어 + 캐시 레이어 설계
                    - 기술: Spring Boot 3.x, Redis 7, MySQL 8, JPA, JUnit 5
                    - 아키텍처: Spring Boot REST API + Redis Cache-Aside + MySQL primary

                    ### 성과
                    - 주문 조회 API p95 응답시간 800ms → 120ms 단축 (Redis Cache-Aside 도입)
                    - JPA fetch join 으로 N+1 문제 해결
                    - TTL 5분 정책 + cache stampede 방어 (single-flight)

                    ### 의사결정
                    - Memcached vs Redis → Redis 채택 (TTL 정책 / Lua 스크립트 필요)
                    - Cache-Aside vs Write-Through → Cache-Aside 채택 (쓰기 빈도 낮음, 일관성 단순화)
                    """;
            String fileHash = "live-meta-test-hash-" + System.currentTimeMillis();

            ResumeSkeleton skeleton = extractionService.extract(resumeText, fileHash);

            assertThat(skeleton).isNotNull();
            assertThat(skeleton.projects()).isNotEmpty();

            Project project = skeleton.projects().get(0);

            assertThat(project.techStack())
                    .as("tech_stack 은 이력서 명시 어휘를 1개 이상 포함해야 한다 (Spring / Redis / MySQL / JPA)")
                    .anyMatch(s -> s.toLowerCase().contains("spring")
                            || s.toLowerCase().contains("redis")
                            || s.toLowerCase().contains("mysql")
                            || s.toLowerCase().contains("jpa"));

            assertThat(project.role())
                    .as("role 은 non-blank 이며 '백엔드' 어휘를 포함해야 한다")
                    .isNotBlank()
                    .contains("백엔드");

            assertThat(project.architecture())
                    .as("architecture 는 non-blank 이며 'Redis' 또는 'Cache' 어휘를 포함해야 한다")
                    .isNotBlank()
                    .matches(s -> s.toLowerCase().contains("redis") || s.toLowerCase().contains("cache"));

            assertThat(project.decisions())
                    .as("decisions 는 1개 이상 'Redis' 또는 'Cache-Aside' 결정을 포함해야 한다")
                    .anyMatch(d -> d.toLowerCase().contains("redis") || d.toLowerCase().contains("cache-aside"));

            assertThat(project.claims())
                    .as("claims 는 1개 이상 추출되어야 하며 모두 non-blank text 보유")
                    .isNotEmpty()
                    .allSatisfy(c -> assertThat(c.text()).isNotBlank());

            boolean impactClaimPresent = project.claims().stream()
                    .map(ResumeClaim::text)
                    .anyMatch(t -> t.contains("800") || t.contains("120") || t.contains("응답"));
            assertThat(impactClaimPresent)
                    .as("성과 claim text 에 정량 어휘 (800 / 120 / 응답) 중 하나가 포함되어야 한다")
                    .isTrue();
        }
    }
}
