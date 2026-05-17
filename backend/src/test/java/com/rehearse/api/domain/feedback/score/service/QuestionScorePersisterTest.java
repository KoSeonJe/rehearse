package com.rehearse.api.domain.feedback.score.service;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreDimensionRepository;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
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
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionScorePersister - rubric_id 호출자 주입 단일 경로")
class QuestionScorePersisterTest extends ServiceIntegrationSupport {

    @Autowired
    private QuestionScorePersister persister;

    @Autowired
    private QuestionScoreRepository questionScoreRepository;

    @Autowired
    private QuestionScoreDimensionRepository dimensionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("rubric_id=nonverbal-v1 로 호출하면 question_score 1 row + dimension N row 적재")
    void saveRubric_with_nonverbal_v1_rubric_id() {
        Ids ids = persistEntities("nonverbal-v1-test");
        Map<String, DimensionScore> dims = new LinkedHashMap<>();
        dims.put("fluency", DimensionScore.of(3, "유창함", "발화 근거"));
        dims.put("eye_contact_posture", DimensionScore.of(2, "시선 안정", "시선 근거"));

        persister.saveRubric(ids.questionId(), ids.interviewId(), "nonverbal-v1", null, dims);

        QuestionScore saved = questionScoreRepository
                .findByQuestionIdAndRubricId(ids.questionId(), "nonverbal-v1")
                .orElseThrow();
        assertThat(saved.getRubricId()).isEqualTo("nonverbal-v1");
        assertThat(saved.getQuestionId()).isEqualTo(ids.questionId());
        assertThat(saved.getInterviewId()).isEqualTo(ids.interviewId());

        List<QuestionScoreDimension> persistedDims = dimensionRepository.findByQuestionScoreId(saved.getId());
        assertThat(persistedDims).extracting(QuestionScoreDimension::getDimensionRef)
                .containsExactlyInAnyOrder("fluency", "eye_contact_posture");
    }

    @Test
    @DisplayName("동일 questionId + rubricId 재호출은 idempotent 적재 (중복 row 생성 안 함)")
    void saveRubric_isIdempotent() {
        Ids ids = persistEntities("nonverbal-v1-idem");
        Map<String, DimensionScore> dims = Map.of(
                "fluency", DimensionScore.of(3, "유창함", "근거")
        );

        persister.saveRubric(ids.questionId(), ids.interviewId(), "nonverbal-v1", null, dims);
        persister.saveRubric(ids.questionId(), ids.interviewId(), "nonverbal-v1", null, dims);

        assertThat(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(ids.interviewId())).hasSize(1);
    }

    private Ids persistEntities(String slug) {
        User user = userRepository.saveAndFlush(User.builder()
                .email(slug + "@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-" + slug)
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
        questionSetRepository.saveAndFlush(questionSet);
        Question question = Question.resume(
                questionSet, QuestionType.RESUME_OPENER,
                "프로젝트 경험을 설명해주세요.", null, null, 0);
        questionSet.addQuestion(question);
        questionRepository.saveAndFlush(question);
        return new Ids(interview.getId(), question.getId());
    }

    private record Ids(Long interviewId, Long questionId) {
    }
}
