package com.rehearse.api.domain.question.repository;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.global.config.JpaAuditingConfig;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@DisplayName("QuestionRepository.findAnsweredButUnscoredByInterviewId")
class QuestionRepositoryAnsweredButUnscoredTest extends AbstractMySqlContainerTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    private int questionOrderSeq = 0;

    @Test
    @DisplayName("transcript 가 있고 QuestionScore 가 없는 Question 만 반환한다 — RESUME_OPENER 포함")
    void returns_only_answered_but_unscored_questions_including_opener() {
        Interview interview = persistInterview();
        QuestionSet questionSet = persistQuestionSet(interview);
        QuestionSetFeedback qsf = persistQuestionSetFeedback(questionSet);

        Question openerAnsweredUnscored = persistQuestion(questionSet, QuestionType.RESUME_OPENER);
        persistTimestampFeedback(qsf, openerAnsweredUnscored, "이력서 오프너 답변");

        Question mainAnsweredUnscored = persistQuestion(questionSet, QuestionType.RESUME_MAIN);
        persistTimestampFeedback(qsf, mainAnsweredUnscored, "메인 답변");

        Question alreadyScored = persistQuestion(questionSet, QuestionType.RESUME_MAIN);
        persistTimestampFeedback(qsf, alreadyScored, "이미 채점됨 답변");
        persistScore(interview, alreadyScored);

        Question noAnswer = persistQuestion(questionSet, QuestionType.RESUME_MAIN);

        Question emptyTranscript = persistQuestion(questionSet, QuestionType.RESUME_MAIN);
        persistTimestampFeedback(qsf, emptyTranscript, "");

        entityManager.flush();
        entityManager.clear();

        List<Question> result = questionRepository.findAnsweredButUnscoredByInterviewId(interview.getId());

        assertThat(result)
                .extracting(Question::getId)
                .containsExactlyInAnyOrder(openerAnsweredUnscored.getId(), mainAnsweredUnscored.getId())
                .doesNotContain(alreadyScored.getId(), noAnswer.getId(), emptyTranscript.getId());
    }

    @Test
    @DisplayName("다른 interview 의 Question 은 반환하지 않는다")
    void does_not_return_questions_from_other_interview() {
        Interview interview = persistInterview();
        Interview otherInterview = persistInterview();
        QuestionSet questionSet = persistQuestionSet(interview);
        QuestionSet otherSet = persistQuestionSet(otherInterview);
        QuestionSetFeedback qsf = persistQuestionSetFeedback(questionSet);
        QuestionSetFeedback otherQsf = persistQuestionSetFeedback(otherSet);

        Question mine = persistQuestion(questionSet, QuestionType.RESUME_MAIN);
        persistTimestampFeedback(qsf, mine, "내 답변");

        Question others = persistQuestion(otherSet, QuestionType.RESUME_MAIN);
        persistTimestampFeedback(otherQsf, others, "다른 인터뷰 답변");

        entityManager.flush();
        entityManager.clear();

        List<Question> result = questionRepository.findAnsweredButUnscoredByInterviewId(interview.getId());

        assertThat(result).extracting(Question::getId).containsExactly(mine.getId());
    }

    private Interview persistInterview() {
        User user = User.builder()
                .email("test+" + System.nanoTime() + "@example.com")
                .name("tester")
                .provider(OAuthProvider.GOOGLE)
                .providerId("pid-" + System.nanoTime())
                .role(UserRole.USER)
                .build();
        entityManager.persist(user);

        Interview interview = Interview.builder()
                .userId(user.getId())
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        entityManager.persist(interview);
        return interview;
    }

    private QuestionSet persistQuestionSet(Interview interview) {
        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
        entityManager.persist(questionSet);
        return questionSet;
    }

    private QuestionSetFeedback persistQuestionSetFeedback(QuestionSet questionSet) {
        QuestionSetFeedback qsf = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment("test comment")
                .build();
        entityManager.persist(qsf);
        return qsf;
    }

    private Question persistQuestion(QuestionSet questionSet, QuestionType type) {
        Question question = Question.builder()
                .questionType(type)
                .questionText("질문")
                .orderIndex(questionOrderSeq++)
                .build();
        question.assignQuestionSet(questionSet);
        entityManager.persist(question);
        return question;
    }

    private void persistTimestampFeedback(QuestionSetFeedback qsf, Question question, String transcript) {
        TimestampFeedback tf = TimestampFeedback.builder()
                .question(question)
                .startMs(0)
                .endMs(1000)
                .transcript(transcript)
                .isAnalyzed(true)
                .build();
        qsf.addTimestampFeedback(tf);
        entityManager.persist(tf);
    }

    private void persistScore(Interview interview, Question question) {
        entityManager.flush();
        QuestionScore score = QuestionScore.builder()
                .questionId(question.getId())
                .interviewId(interview.getId())
                .rubricId("resume-v1")
                .levelFlag("L1")
                .build();
        entityManager.persist(score);
    }
}
