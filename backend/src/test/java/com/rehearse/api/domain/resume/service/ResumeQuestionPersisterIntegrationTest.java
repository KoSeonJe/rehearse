package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionDepthType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.service.ResumeQuestionPersister.ResumeQuestionDraft;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeQuestionPersister — Resume 질문은 draft 당 QuestionSet 1개를 분리 생성한다")
class ResumeQuestionPersisterIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private ResumeQuestionPersister persister;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("opener 1 + main 3 드래프트 적재 시 QuestionSet 4개 생성, 각 QuestionSet 의 질문은 1개")
    void persistAll_creates_one_question_set_per_draft() {
        Long interviewId = persistInterview();
        List<ResumeQuestionDraft> drafts = List.of(
                new ResumeQuestionDraft(QuestionType.RESUME_OPENER,
                        "메인 프로젝트 설명과 본인의 역할을 말해주세요.", "TTS-O", "best-O", 0, null),
                new ResumeQuestionDraft(QuestionType.RESUME_MAIN,
                        "Redis 캐시 도입 의사결정 근거", "TTS-M1", "best-M1", 1,
                        QuestionDepthType.TRADEOFF),
                new ResumeQuestionDraft(QuestionType.RESUME_MAIN,
                        "Cache-Aside 정합성 정책", "TTS-M2", "best-M2", 2,
                        QuestionDepthType.PRINCIPLE),
                new ResumeQuestionDraft(QuestionType.RESUME_MAIN,
                        "TTL 만료 시 stampede 방어", "TTS-M3", "best-M3", 3,
                        QuestionDepthType.LIMITATION)
        );

        int saved = persister.persistAll(interviewId, drafts);

        assertThat(saved).isEqualTo(4);
        List<QuestionSet> persistedSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        assertThat(persistedSets).hasSize(4);
        assertThat(persistedSets).extracting(QuestionSet::getCategory)
                .containsOnly(InterviewType.RESUME_BASED);
        assertThat(persistedSets).extracting(QuestionSet::getOrderIndex)
                .containsExactly(0, 1, 2, 3);

        for (QuestionSet qs : persistedSets) {
            List<Question> questions = questionRepository.findByQuestionSetIdOrderByOrderIndex(qs.getId());
            assertThat(questions).hasSize(1);
            assertThat(questions.get(0).getOrderIndex()).isZero();
            assertThat(questions.get(0).getQuestionSet().getId())
                    .as("양방향 FK: question.question_set_id 가 부모 QSet 으로 정상 적재")
                    .isEqualTo(qs.getId());
        }

        QuestionType openerType = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(persistedSets.get(0).getId())
                .get(0).getQuestionType();
        assertThat(openerType).isEqualTo(QuestionType.RESUME_OPENER);
    }

    @Test
    @DisplayName("드래프트가 비어 있으면 QuestionSet 을 생성하지 않고 0 을 반환한다")
    void persistAll_returns_zero_for_empty_drafts() {
        Long interviewId = persistInterview();

        int saved = persister.persistAll(interviewId, List.of());

        assertThat(saved).isZero();
        assertThat(questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId)).isEmpty();
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("resume-persister@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-resume-persister")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        return interview.getId();
    }
}
