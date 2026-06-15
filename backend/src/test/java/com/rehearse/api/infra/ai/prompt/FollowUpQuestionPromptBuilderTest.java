package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.EvidenceStrength;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder.PromptPair;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FollowUpQuestionPromptBuilder — category 에 따라 3종 템플릿을 선택하고 user 는 분석 결과 마커를 조립한다")
class FollowUpQuestionPromptBuilderTest {

    private FollowUpQuestionPromptBuilder builder;

    @BeforeEach
    void setUp() {
        PromptTemplateLoader loader = new PromptTemplateLoader();
        loader.init();
        builder = new FollowUpQuestionPromptBuilder(loader, new TokenEstimator());
    }

    private AnswerAnalysis analysis(String transcript, List<String> claimTexts) {
        List<Claim> claims = claimTexts.stream()
                .map(t -> new Claim(t, 3, EvidenceStrength.STRONG, "tag"))
                .toList();
        return new AnswerAnalysis(
                transcript,
                claims,
                Map.of("clarity", 2, "depth", 1),
                "depth",
                List.of("동시성 가정"),
                RecommendedNextAction.DEEP_DIVE
        );
    }

    @Nested
    @DisplayName("system 프롬프트 — category 별 템플릿 선택")
    class SystemPrompt {

        @Test
        @DisplayName("CONCEPT 은 개념 심화 템플릿을 선택하고 경험 전제 차원이 보완 차원에 등장하지 않는다")
        void concept_selectsConceptTemplate_withoutExperienceDimensions() {
            PromptPair pair = builder.build("질문", analysis("답변", List.of("주장1")), QuestionCategory.CONCEPT);

            assertThat(pair.system())
                    .as("GLOBAL_CORE 보안규칙 포함")
                    .contains("당신은 한국어 개발자 기술 면접 시스템의 AI 컴포넌트입니다.");
            assertThat(pair.system())
                    .as("CONCEPT 5종 차원만 명시 — 경험 전제 차원이 보완 차원 표에 없음")
                    .contains("평가 차원 (CONCEPT 5종)")
                    .doesNotContain("experience_concreteness")
                    .doesNotContain("collaboration_awareness");
            assertThat(pair.system())
                    .as("경험 구체화·이력서 정합 차원 표 미포함 (개념 심화 템플릿만 로드)")
                    .doesNotContain("평가 차원 (EXPERIENCE 4종)")
                    .doesNotContain("평가 차원 (RESUME 10종)");
        }

        @Test
        @DisplayName("EXPERIENCE 은 경험 구체화 템플릿을 선택하고 EXPERIENCE 4종 차원을 명시한다")
        void experience_selectsExperienceTemplate() {
            PromptPair pair = builder.build("질문", analysis("답변", List.of("주장1")), QuestionCategory.EXPERIENCE);

            assertThat(pair.system())
                    .contains("평가 차원 (EXPERIENCE 4종)")
                    .contains("experience_concreteness")
                    .contains("collaboration_awareness")
                    .doesNotContain("factual_consistency")
                    .doesNotContain("chain_depth");
        }

        @Test
        @DisplayName("RESUME 은 이력서 정합 템플릿을 선택하고 RESUME 10종 차원을 명시한다")
        void resume_selectsResumeTemplate() {
            PromptPair pair = builder.build("질문", analysis("답변", List.of("주장1")), QuestionCategory.RESUME);

            assertThat(pair.system())
                    .contains("평가 차원 (RESUME 10종)")
                    .contains("factual_consistency")
                    .contains("chain_depth");
        }
    }

    @Nested
    @DisplayName("user fragment")
    class UserFragment {

        @Test
        @DisplayName("user 에 MAIN_QUESTION / USER_ANSWER / CLAIMS / WEAKEST_DIMENSION / DIMENSION_GAPS / UNSTATED_ASSUMPTIONS 마커가 포함된다")
        void user_contains_markers() {
            PromptPair pair = builder.build("주요 질문", analysis("내 답변", List.of("주장A", "주장B")), QuestionCategory.CONCEPT);

            assertThat(pair.user())
                    .contains("<<<MAIN_QUESTION>>>")
                    .contains("주요 질문")
                    .contains("<<<USER_ANSWER>>>")
                    .contains("내 답변")
                    .contains("<<<CLAIMS>>>")
                    .contains("주장A | 주장B")
                    .contains("WEAKEST_DIMENSION: depth")
                    .contains("DIMENSION_GAPS:")
                    .contains("<<<UNSTATED_ASSUMPTIONS>>>")
                    .contains("동시성 가정");
        }

        @Test
        @DisplayName("cap(1400) 을 초과하는 긴 답변도 절단되지 않고 user 에 그대로 보존된다")
        void user_not_truncated_when_over_cap() {
            String longAnswer = "가".repeat(8000);

            PromptPair pair = builder.build("Q", analysis(longAnswer, List.of("주장1")), QuestionCategory.CONCEPT);

            assertThat(pair.user()).contains(longAnswer);
            assertThat(new TokenEstimator().estimate(pair.user())).isGreaterThan(1400);
        }
    }
}
