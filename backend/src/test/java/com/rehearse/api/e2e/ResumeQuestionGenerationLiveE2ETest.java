package com.rehearse.api.e2e;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionDepthType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.question.service.ResumeTrackInitiator;
import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Disabled("Live LLM E2E — RUN_LIVE_API=true 환경변수로만 활성")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
@DisplayName("Resume 질문 생성 Live LLM E2E — 실제 LLM 호출로 깊이/직무/표층 가드 검증")
class ResumeQuestionGenerationLiveE2ETest extends ServiceIntegrationSupport {

    private static final byte[] DUMMY_PDF = "pdf".getBytes();
    private static final int EXPECTED_MAIN_COUNT = 7;
    private static final int DEPTH_DOMINANCE_THRESHOLD = 4;
    private static final int FORBIDDEN_PATTERN_THRESHOLD = 1;

    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("왜\\s.{1,30}\\s?(사용|선택|채택)"),
            Pattern.compile(".{1,30}\\s?의\\s?장점")
    );

    @Autowired
    private ResumeTrackInitiator initiator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @MockitoBean
    private ResumeSkeletonExtractor resumeSkeletonExtractor;

    @Test
    @DisplayName("BACKEND/JAVA_SPRING 실제 LLM 호출 → main 7개 / depth_type enum 유효 / opener depthType null / 편중·표층 가드 통과")
    void liveCall_satisfiesDepthAndForbiddenGuards_forBackend() {
        Long interviewId = persistInterview(Position.BACKEND);
        stubExtractor(backendSkeleton(), "live-be-hash");

        initiator.initiate(interviewId, "live-be-hash", DUMMY_PDF, 15,
                Position.BACKEND, TechStack.JAVA_SPRING);

        List<QuestionSet> sets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        assertThat(sets).as("opener 1 + main 7 = 8 QuestionSet").hasSize(1 + EXPECTED_MAIN_COUNT);

        Question opener = firstQuestion(sets.get(0));
        assertThat(opener.getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);
        assertThat(opener.getDepthType()).as("opener depthType 는 null").isNull();

        List<Question> mains = mainsFrom(sets);
        assertThat(mains).hasSize(EXPECTED_MAIN_COUNT);
        for (Question main : mains) {
            assertThat(main.getDepthType()).as("main depthType enum 유효").isNotNull();
        }

        Map<QuestionDepthType, Integer> counts = countDepthTypes(mains);
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        assertThat(max)
                .as("편중 가드: 5 유형 중 한 유형이 %d 건 미만 점유", DEPTH_DOMINANCE_THRESHOLD)
                .isLessThan(DEPTH_DOMINANCE_THRESHOLD);

        long forbiddenHits = mains.stream()
                .map(Question::getQuestionText)
                .filter(this::matchesForbiddenPattern)
                .count();
        assertThat(forbiddenHits)
                .as("표층 패턴 가드: 매칭 main %d 건 이하", FORBIDDEN_PATTERN_THRESHOLD)
                .isLessThanOrEqualTo(FORBIDDEN_PATTERN_THRESHOLD);
    }

    private boolean matchesForbiddenPattern(String text) {
        if (text == null) {
            return false;
        }
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private Map<QuestionDepthType, Integer> countDepthTypes(List<Question> mains) {
        Map<QuestionDepthType, Integer> counts = new EnumMap<>(QuestionDepthType.class);
        for (Question main : mains) {
            QuestionDepthType type = main.getDepthType();
            if (type == null) {
                continue;
            }
            counts.merge(type, 1, Integer::sum);
        }
        return counts;
    }

    private Question firstQuestion(QuestionSet set) {
        return questionRepository
                .findByQuestionSetIdOrderByOrderIndex(set.getId()).get(0);
    }

    private List<Question> mainsFrom(List<QuestionSet> sets) {
        return sets.stream()
                .skip(1)
                .map(this::firstQuestion)
                .toList();
    }

    private Long persistInterview(Position position) {
        User user = userRepository.saveAndFlush(User.builder()
                .email("live-resume-" + position.name().toLowerCase() + "@example.com")
                .name("Live 테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-live-" + position.name().toLowerCase())
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), position, InterviewLevel.MID, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        return interview.getId();
    }

    private void stubExtractor(GeneratedResumeSkeleton skeleton, String fileHash) {
        when(resumeSkeletonExtractor.extract(any(byte[].class), eq(fileHash))).thenReturn(skeleton);
    }

    private GeneratedResumeSkeleton backendSkeleton() {
        return new GeneratedResumeSkeleton(
                "r_live01",
                "mid",
                "backend",
                List.of(new GeneratedResumeSkeleton.GeneratedProject(
                        "p1",
                        "주문 캐싱 개선",
                        List.of("Spring Boot", "Redis", "MySQL", "JPA"),
                        "백엔드 단독 / API + 캐시 레이어 설계",
                        "Spring Boot REST API + Redis Cache-Aside + MySQL",
                        List.of("Memcached vs Redis → Redis 채택, TTL 정책 필요"),
                        new GeneratedResumeSkeleton.GeneratedDepthSignals(
                                List.of("TTL 5분 채택 → 캐시 무효화 비용 vs 신선도 비용"),
                                List.of("Memcached vs Redis → Redis 채택"),
                                List.of("주문 조회 API p95 800ms → 120ms"),
                                List.of("Redis TTL 정책 필요 → Memcached 대신 채택"))
                ))
        );
    }
}
