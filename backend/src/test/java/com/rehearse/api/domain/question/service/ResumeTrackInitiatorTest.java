package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.QuestionGenerationStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.schema.GeneratedResumeQuestionsSchema;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResumeTrackInitiator — 이력서 트랙 면접 시작 (opener + main 일괄 LLM 생성 + DB 분리 적재)")
class ResumeTrackInitiatorTest extends ServiceIntegrationSupport {

    private static final String RESUME_TEXT = """
            경력 2년차 백엔드 개발자입니다. 주문 조회 API 성능 개선 프로젝트와
            결제 정합성 보장 프로젝트를 진행했습니다. 기술스택: Java, Spring, MySQL, Redis.
            """;

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
    private AiClient aiClient;

    @Test
    @DisplayName("opener 1 + main 3 응답 → QSet 4개 분리 적재 + orderIndex 0..3 + Interview 상태 COMPLETED")
    void initiate_persistsOpenerAndMains_andCompletesInterview() {
        Long interviewId = persistInterview();
        stubAiClient(skeletonJsonWithProjects(), questionsJson(1, 3));

        initiator.initiate(interviewId, "hash-success", RESUME_TEXT, 30);

        List<QuestionSet> persistedSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        assertThat(persistedSets).hasSize(4);
        assertThat(persistedSets).extracting(QuestionSet::getCategory)
                .containsOnly(InterviewType.RESUME_BASED);
        assertThat(persistedSets).extracting(QuestionSet::getOrderIndex)
                .containsExactly(0, 1, 2, 3);

        List<Question> openerQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(0).getId());
        assertThat(openerQuestions).hasSize(1);
        assertThat(openerQuestions.get(0).getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);

        for (int i = 1; i < persistedSets.size(); i++) {
            List<Question> mains = questionRepository
                    .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(i).getId());
            assertThat(mains).hasSize(1);
            assertThat(mains.get(0).getQuestionType()).isEqualTo(QuestionType.RESUME_MAIN);
        }

        Interview reloaded = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(reloaded.getQuestionGenerationStatus())
                .isEqualTo(QuestionGenerationStatus.COMPLETED);
    }

    @Test
    @DisplayName("skeleton.projects[0].projectName 이 LLM prompt 의 PRIMARY_PROJECT_NAME 라인으로 전달된다")
    void initiate_passes_primaryProjectName_to_llm_prompt() {
        Long interviewId = persistInterview();
        stubAiClient(skeletonJsonWithProjects(), questionsJson(1, 1));

        initiator.initiate(interviewId, "hash-primary", RESUME_TEXT, 21);

        String questionGenUserMessage = captureUserMessageForCallType("resume_question_generator");
        assertThat(questionGenUserMessage)
                .as("primaryProjectName = projects[0].projectName 이 prompt 에 주입된다")
                .contains("PRIMARY_PROJECT_NAME: 주문 캐싱 개선");
    }

    @Test
    @DisplayName("projects 가 비어 있으면 PRIMARY_PROJECT_NAME 자리는 '(없음)' 으로 채워진다 (generic fallback)")
    void initiate_falls_back_to_generic_when_projects_empty() {
        Long interviewId = persistInterview();
        stubAiClient(skeletonJsonNoProjects(), questionsJson(1, 1));

        initiator.initiate(interviewId, "hash-empty", RESUME_TEXT, 21);

        String questionGenUserMessage = captureUserMessageForCallType("resume_question_generator");
        assertThat(questionGenUserMessage)
                .as("projects 비어 있음 → primaryProjectName=null → FocusLayer 의 nz() 가 (없음) 출력")
                .contains("PRIMARY_PROJECT_NAME: (없음)");
    }

    @Test
    @DisplayName("resume_question_generator ChatRequest 는 strict JSON_SCHEMA 포맷으로 빌드된다 (openers / mains required)")
    void initiate_chatRequest_uses_strict_json_schema() {
        Long interviewId = persistInterview();
        stubAiClient(skeletonJsonWithProjects(), questionsJson(1, 1));

        initiator.initiate(interviewId, "hash-schema", RESUME_TEXT, 21);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(aiClient, atLeastOnce()).chat(captor.capture());
        ChatRequest generatorReq = captor.getAllValues().stream()
                .filter(r -> "resume_question_generator".equals(r.callType()))
                .findFirst()
                .orElseThrow();

        assertThat(generatorReq.responseFormat()).isEqualTo(ResponseFormat.JSON_SCHEMA);
        assertThat(generatorReq.jsonSchema()).isNotNull();
        assertThat(generatorReq.jsonSchema().name()).isEqualTo(GeneratedResumeQuestionsSchema.SCHEMA_NAME);
        assertThat(generatorReq.jsonSchema().schema()).containsEntry("type", "object");
        assertThat(generatorReq.jsonSchema().schema()).containsEntry("additionalProperties", false);
        assertThat(generatorReq.jsonSchema().schema().get("required"))
                .isEqualTo(List.of("openers", "mains"));
    }

    @Test
    @DisplayName("LLM 실패 시 Interview 상태 FAILED 로 전이 + 예외 재전파")
    void initiate_marksInterviewFailed_andRethrows_whenLlmFails() {
        Long interviewId = persistInterview();
        when(aiClient.chat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("LLM 호출 실패"));

        Assertions.assertThatThrownBy(() ->
                        initiator.initiate(interviewId, "hash-fail", RESUME_TEXT, 30))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM 호출 실패");

        Interview reloaded = interviewRepository.findById(interviewId).orElseThrow();
        assertThat(reloaded.getQuestionGenerationStatus())
                .isEqualTo(QuestionGenerationStatus.FAILED);
        assertThat(questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId)).isEmpty();
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("resume-initiator@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-resume-initiator")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        return interview.getId();
    }

    private void stubAiClient(String skeletonJson, String questionsJson) {
        when(aiClient.chat(any(ChatRequest.class))).thenAnswer(inv -> {
            ChatRequest req = inv.getArgument(0);
            String content = switch (req.callType()) {
                case "resume_extractor" -> skeletonJson;
                case "resume_question_generator" -> questionsJson;
                default -> "{}";
            };
            return new ChatResponse(content, ChatResponse.Usage.empty(), "mock", "mock-model", false, false);
        });
    }

    private String captureUserMessageForCallType(String callType) {
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(aiClient, atLeastOnce()).chat(captor.capture());
        return captor.getAllValues().stream()
                .filter(r -> callType.equals(r.callType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("callType=" + callType + " 호출 없음"))
                .messages().stream()
                .filter(m -> m.role() == com.rehearse.api.infra.ai.dto.ChatMessage.Role.USER)
                .map(com.rehearse.api.infra.ai.dto.ChatMessage::content)
                .reduce((a, b) -> a + "\n" + b)
                .orElseThrow(() -> new AssertionError("USER 메시지 없음"));
    }

    private String skeletonJsonWithProjects() {
        return """
                {
                  "resume_id": "resume-1",
                  "candidate_level": "MID",
                  "target_domain": "backend",
                  "projects": [
                    {
                      "project_id": "p1",
                      "project_name": "주문 캐싱 개선",
                      "tech_stack": ["Redis", "MySQL"],
                      "role": "백엔드",
                      "architecture": "Cache-Aside",
                      "decisions": ["TTL 5분"]
                    },
                    {
                      "project_id": "p2",
                      "project_name": "결제 정합성",
                      "tech_stack": ["Java"],
                      "role": "백엔드",
                      "architecture": "",
                      "decisions": []
                    }
                  ]
                }
                """;
    }

    private String skeletonJsonNoProjects() {
        return """
                {
                  "resume_id": "resume-2",
                  "candidate_level": "JUNIOR",
                  "target_domain": "backend",
                  "projects": []
                }
                """;
    }

    private String questionsJson(int openerCount, int mainCount) {
        StringBuilder sb = new StringBuilder("{\"openers\":[");
        for (int i = 0; i < openerCount; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"question\":\"메인 프로젝트 설명").append(i)
                    .append("\",\"tts_question\":\"TTS-O").append(i)
                    .append("\",\"best_answer\":\"best-O").append(i).append("\"}");
        }
        sb.append("],\"mains\":[");
        for (int i = 0; i < mainCount; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"question\":\"주요 의사결정 ").append(i)
                    .append("\",\"tts_question\":\"TTS-M").append(i)
                    .append("\",\"best_answer\":\"best-M").append(i).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
