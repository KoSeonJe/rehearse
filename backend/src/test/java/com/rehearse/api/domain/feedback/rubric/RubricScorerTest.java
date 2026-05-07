package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.service.RubricLoader;
import com.rehearse.api.domain.feedback.rubric.service.RubricScorer;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.adapter.RubricScoringAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
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
    @DisplayName("Resume Track mode-aware")
    class ResumeModeAware {

        @Test
        @DisplayName("PLAYGROUND mode → D6만 채점 요청")
        void score_playgroundMode_onlyD6() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(List.of("experience_concreteness")), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(List.of("experience_concreteness"))))
                    .willReturn(new RubricScoringResult("resume-v1", List.of("experience_concreteness"),
                            Map.of("experience_concreteness", DimensionScore.of(3, "구체적 수치 제시", "TPS 10000")), null));

            RubricScoringResult result = rubricScorer.score(
                    question, questionSet, resumeInterview, "TPS 10000을 달성했습니다",
                    analysis, ResumeMode.PLAYGROUND, 1, null
            );

            assertThat(result.scoredDimensions()).containsExactly("experience_concreteness");
        }

        @Test
        @DisplayName("INTERROGATION mode → D2/D3/D9/D10 채점 요청")
        void score_interrogationMode_correctDimensions() {
            Rubric rubric = createResumeRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(rubric);
            List<String> expectedDims = List.of("technical_depth", "reasoning_communication", "factual_consistency", "chain_depth");
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(expectedDims), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(expectedDims)))
                    .willReturn(new RubricScoringResult("resume-v1", expectedDims,
                            Map.of("technical_depth", DimensionScore.of(2, "설명", "evidence"),
                                   "reasoning_communication", DimensionScore.of(2, "설명", "evidence"),
                                   "factual_consistency", DimensionScore.of(3, "설명", "evidence"),
                                   "chain_depth", DimensionScore.of(2, "설명", "evidence")), null));

            RubricScoringResult result = rubricScorer.score(
                    question, questionSet, resumeInterview, "상세한 답변",
                    analysis, ResumeMode.INTERROGATION, 2, null
            );

            assertThat(result.scoredDimensions()).containsExactlyInAnyOrderElementsOf(expectedDims);
        }

    }

    @Nested
    @DisplayName("Standard Track")
    class StandardTrack {

        @Test
        @DisplayName("mode=null → on_intent_answer 차원 채점")
        void score_noMode_usesIntentAnswerDimensions() {
            Rubric csFundamentalRubric = createCsRubric();
            given(rubricLoader.resolveFor(any(), any(), any())).willReturn(csFundamentalRubric);
            List<String> expectedDims = List.of("technical_depth", "reasoning_communication", "conceptual_accuracy", "recovery_from_gaps");
            ChatRequest mockRequest = mockChatRequest();
            given(promptBuilder.build(any(), any(), any(), any(), eq(expectedDims), any(), any(), any(), any()))
                    .willReturn(mockRequest);
            given(adapter.adapt(any(), any(), any(), eq(expectedDims)))
                    .willReturn(new RubricScoringResult("concept-cs-fundamental-v1", expectedDims,
                            Map.of("technical_depth", DimensionScore.of(2, "설명", "ev"),
                                   "reasoning_communication", DimensionScore.of(2, "설명", "ev"),
                                   "conceptual_accuracy", DimensionScore.of(3, "정확", "ev"),
                                   "recovery_from_gaps", DimensionScore.of(2, "인정", "ev")), null));

            RubricScoringResult result = rubricScorer.score(
                    question, questionSet, standardInterview, "상세한 기술 답변",
                    analysis, null, null, null
            );

            assertThat(result.scoredDimensions()).containsExactlyInAnyOrderElementsOf(expectedDims);
        }
    }

    private Rubric createResumeRubric() {
        return new Rubric(
                "resume-v1",
                "Resume Track 루브릭",
                List.of(
                        new DimensionRef("technical_depth", 0.25),
                        new DimensionRef("reasoning_communication", 0.15),
                        new DimensionRef("experience_concreteness", 0.20),
                        new DimensionRef("factual_consistency", 0.20),
                        new DimensionRef("chain_depth", 0.20)
                ),
                Map.of(
                        "on_playground_mode", List.of("experience_concreteness"),
                        "on_interrogation_mode", List.of("technical_depth", "reasoning_communication", "factual_consistency", "chain_depth")
                ),
                Map.of()
        );
    }

    private Rubric createCsRubric() {
        return new Rubric(
                "concept-cs-fundamental-v1",
                "CS 기초 루브릭",
                List.of(
                        new DimensionRef("technical_depth", 0.30),
                        new DimensionRef("reasoning_communication", 0.25),
                        new DimensionRef("conceptual_accuracy", 0.30),
                        new DimensionRef("recovery_from_gaps", 0.15)
                ),
                Map.of(
                        "on_intent_answer", List.of("technical_depth", "reasoning_communication", "conceptual_accuracy", "recovery_from_gaps")
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
