package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.service.RubricLoader;
import com.rehearse.api.domain.feedback.rubric.service.RubricScorer;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.adapter.RubricScoringAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RubricScorer")
class RubricScorerTest {

    @InjectMocks
    private RubricScorer rubricScorer;

    @Mock
    private RubricLoader rubricLoader;

    @Mock
    private RubricScorerPromptBuilder promptBuilder;

    @Mock
    private RubricScoringAdapter adapter;

    @Mock
    private AiClient aiClient;

    private Interview standardInterview;
    private Interview resumeInterview;
    private QuestionSet questionSet;
    private Question question;
    private AnswerAnalysis analysis;

    @BeforeEach
    void setUp() {
        standardInterview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        resumeInterview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .build();

        questionSet = QuestionSet.builder()
                .interview(standardInterview)
                .category(QuestionSetCategory.RESUME_BASED)
                .orderIndex(0)
                .build();

        question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("테스트 질문")
                .feedbackPerspective(FeedbackPerspective.TECHNICAL)
                .orderIndex(0)
                .build();

        analysis = new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }

    @Nested
    @DisplayName("CLARIFY intent")
    class ClarifyIntent {

        @Test
        @DisplayName("CLARIFY intent 시 empty RubricScore 반환")
        void score_clarifyIntent_returnsEmpty() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);

            RubricScore result = rubricScorer.score(
                    question, questionSet, resumeInterview, "잘 모르겠어요",
                    analysis, IntentType.CLARIFY_REQUEST, ResumeMode.INTERROGATION, 2, null
            );

            assertThat(result.isEmpty()).isTrue();
            assertThat(result.rubricId()).isEqualTo("resume-v1");
        }
    }

    @Nested
    @DisplayName("GIVE_UP intent")
    class GiveUpIntent {

        @Test
        @DisplayName("GIVE_UP intent 시 D8만 채점")
        void score_giveUpIntent_onlyD8() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(List.of("D8")), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(List.of("D8"))))
                    .willReturn(new RubricScore("resume-v1", List.of("D8"),
                            Map.of("D8", DimensionScore.of(2, "모른다고 인정했음", "잘 모르겠어요")), null));

            RubricScore result = rubricScorer.score(
                    question, questionSet, resumeInterview, "잘 모르겠습니다",
                    analysis, IntentType.GIVE_UP, ResumeMode.INTERROGATION, 2, null
            );

            assertThat(result.scoredDimensions()).containsExactly("D8");
        }
    }

    @Nested
    @DisplayName("Resume Track mode-aware")
    class ResumeModeAware {

        @Test
        @DisplayName("PLAYGROUND mode → D6만 채점 요청")
        void score_playgroundMode_onlyD6() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(List.of("D6")), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(List.of("D6"))))
                    .willReturn(new RubricScore("resume-v1", List.of("D6"),
                            Map.of("D6", DimensionScore.of(3, "구체적 수치 제시", "TPS 10000")), null));

            RubricScore result = rubricScorer.score(
                    question, questionSet, resumeInterview, "TPS 10000을 달성했습니다",
                    analysis, IntentType.ANSWER, ResumeMode.PLAYGROUND, 1, null
            );

            assertThat(result.scoredDimensions()).containsExactly("D6");
        }

        @Test
        @DisplayName("INTERROGATION mode → D2/D3/D9/D10 채점 요청")
        void score_interrogationMode_correctDimensions() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);
            List<String> expectedDims = List.of("D2", "D3", "D9", "D10");
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(expectedDims), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(expectedDims)))
                    .willReturn(new RubricScore("resume-v1", expectedDims,
                            Map.of("D2", DimensionScore.of(2, "설명", "evidence"),
                                   "D3", DimensionScore.of(2, "설명", "evidence"),
                                   "D9", DimensionScore.of(3, "설명", "evidence"),
                                   "D10", DimensionScore.of(2, "설명", "evidence")), null));

            RubricScore result = rubricScorer.score(
                    question, questionSet, resumeInterview, "상세한 답변",
                    analysis, IntentType.ANSWER, ResumeMode.INTERROGATION, 2, null
            );

            assertThat(result.scoredDimensions()).containsExactlyInAnyOrderElementsOf(expectedDims);
        }

        @Test
        @DisplayName("WRAP_UP mode → D10만 채점 요청")
        void score_wrapUpMode_onlyD10() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(List.of("D10")), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(List.of("D10"))))
                    .willReturn(new RubricScore("resume-v1", List.of("D10"),
                            Map.of("D10", DimensionScore.of(3, "L4까지 도달", "트레이드오프 비교")), null));

            RubricScore result = rubricScorer.score(
                    question, questionSet, resumeInterview, "회고 답변",
                    analysis, IntentType.ANSWER, ResumeMode.WRAP_UP, 4, null
            );

            assertThat(result.scoredDimensions()).containsExactly("D10");
        }
    }

    @Nested
    @DisplayName("Standard Track")
    class StandardTrack {

        @Test
        @DisplayName("ANSWER intent, mode=null → on_intent_answer 차원 채점")
        void score_answerIntent_noMode_usesIntentAnswerDimensions() {
            Rubric csFundamentalRubric = createCsRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(csFundamentalRubric);
            List<String> expectedDims = List.of("D2", "D3", "D4", "D8");
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(expectedDims), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(expectedDims)))
                    .willReturn(new RubricScore("concept-cs-fundamental-v1", expectedDims,
                            Map.of("D2", DimensionScore.of(2, "설명", "ev"),
                                   "D3", DimensionScore.of(2, "설명", "ev"),
                                   "D4", DimensionScore.of(3, "정확", "ev"),
                                   "D8", DimensionScore.of(2, "인정", "ev")), null));

            RubricScore result = rubricScorer.score(
                    question, questionSet, standardInterview, "상세한 기술 답변",
                    analysis, IntentType.ANSWER, null, null, null
            );

            assertThat(result.scoredDimensions()).containsExactlyInAnyOrderElementsOf(expectedDims);
        }
    }

    private Rubric createResumeRubric() {
        return new Rubric(
                "resume-v1",
                "Resume Track 루브릭",
                List.of(
                        new DimensionRef("D2", 0.25),
                        new DimensionRef("D3", 0.15),
                        new DimensionRef("D6", 0.20),
                        new DimensionRef("D9", 0.20),
                        new DimensionRef("D10", 0.20)
                ),
                Map.of(
                        "on_intent_clarify", List.of(),
                        "on_intent_give_up", List.of("D8"),
                        "on_playground_mode", List.of("D6"),
                        "on_interrogation_mode", List.of("D2", "D3", "D9", "D10"),
                        "on_wrap_up_mode", List.of("D10")
                ),
                Map.of()
        );
    }

    private Rubric createCsRubric() {
        return new Rubric(
                "concept-cs-fundamental-v1",
                "CS 기초 루브릭",
                List.of(
                        new DimensionRef("D2", 0.30),
                        new DimensionRef("D3", 0.25),
                        new DimensionRef("D4", 0.30),
                        new DimensionRef("D8", 0.15)
                ),
                Map.of(
                        "on_intent_clarify", List.of(),
                        "on_intent_give_up", List.of("D8"),
                        "on_intent_answer", List.of("D2", "D3", "D4", "D8")
                ),
                Map.of()
        );
    }

    private ChatRequest mockChatRequest() {
        return ChatRequest.builder()
                .messages(List.of(
                        com.rehearse.api.infra.ai.dto.ChatMessage.of(
                                com.rehearse.api.infra.ai.dto.ChatMessage.Role.USER, "test")))
                .callType("rubric_scorer")
                .build();
    }
}
