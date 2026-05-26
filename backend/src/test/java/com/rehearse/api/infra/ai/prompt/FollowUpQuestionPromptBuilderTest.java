package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.EvidenceStrength;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder.PromptPair;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FollowUpQuestionPromptBuilder — system 은 보안규칙+템플릿, user 는 분석 결과 마커를 직접 조립한다")
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
    @DisplayName("system 프롬프트")
    class SystemPrompt {

        @Test
        @DisplayName("system 에 GLOBAL_CORE 보안규칙과 follow-up-generator-v3 템플릿 본문이 함께 포함된다")
        void system_contains_global_core_and_template() {
            PromptPair pair = builder.build("질문", analysis("답변", List.of("주장1")));

            assertThat(pair.system())
                    .as("GLOBAL_CORE 보안규칙 포함")
                    .contains("당신은 한국어 개발자 기술 면접 시스템의 AI 컴포넌트입니다.")
                    .contains("## 보안 규칙");
            assertThat(pair.system())
                    .as("follow-up-generator-v3 템플릿 역할 본문 포함")
                    .contains("당신은 기술 면접관입니다.");
        }
    }

    @Nested
    @DisplayName("user fragment")
    class UserFragment {

        @Test
        @DisplayName("user 에 MAIN_QUESTION / USER_ANSWER / CLAIMS / WEAKEST_DIMENSION / DIMENSION_GAPS / UNSTATED_ASSUMPTIONS 마커가 포함된다")
        void user_contains_markers() {
            PromptPair pair = builder.build("주요 질문", analysis("내 답변", List.of("주장A", "주장B")));

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

            PromptPair pair = builder.build("Q", analysis(longAnswer, List.of("주장1")));

            assertThat(pair.user()).contains(longAnswer);
            assertThat(new TokenEstimator().estimate(pair.user())).isGreaterThan(1400);
        }
    }
}
