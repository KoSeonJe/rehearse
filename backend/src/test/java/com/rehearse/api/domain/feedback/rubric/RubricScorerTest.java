package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.service.RubricLoader;
import com.rehearse.api.domain.feedback.rubric.service.RubricScorer;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.adapter.RubricScoringAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        questionSet = QuestionSet.builder()
                .interview(standardInterview)
                .category(InterviewType.RESUME_BASED)
                .orderIndex(0)
                .build();

        question = Question.builder()
                .questionType(QuestionType.TECH_MAIN)
                .questionText("테스트 질문")
                .orderIndex(0)
                .build();

        analysis = new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }

    @Test
    @DisplayName("on_intent_answer 차원으로 채점 요청 위임")
    void score_usesIntentAnswerDimensions() {
        Rubric csFundamentalRubric = createCsRubric();
        given(rubricLoader.resolveFor(any(), any(), any())).willReturn(csFundamentalRubric);
        List<String> expectedDims = List.of("technical_depth", "reasoning_communication", "conceptual_accuracy", "recovery_from_gaps");
        ChatRequest mockRequest = mockChatRequest();
        given(promptBuilder.build(any(), any(), any(), any(), eq(expectedDims), any()))
                .willReturn(mockRequest);
        given(adapter.adapt(any(), any(), any(), eq(expectedDims), any(), any(), any()))
                .willReturn(new RubricScoringResult("concept-cs-fundamental-v1", expectedDims,
                        Map.of("technical_depth", DimensionScore.of(2, "설명", "ev"),
                               "reasoning_communication", DimensionScore.of(2, "설명", "ev"),
                               "conceptual_accuracy", DimensionScore.of(3, "정확", "ev"),
                               "recovery_from_gaps", DimensionScore.of(2, "인정", "ev")), null));

        RubricScoringResult result = rubricScorer.score(
                question, questionSet, standardInterview, "상세한 기술 답변", analysis
        );

        assertThat(result.scoredDimensions()).containsExactlyInAnyOrderElementsOf(expectedDims);
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
