package com.rehearse.api.domain.integrity;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.global.config.JpaAuditingConfig;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@DisplayName("V40 DB 무결성 패치 제약 검증")
class V40IntegrityPatchTest extends AbstractMySqlContainerTest {

    @PersistenceContext
    private EntityManager em;

    private Interview savedInterview;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-test")
                .role(UserRole.USER)
                .build();
        em.persist(user);
        em.flush();

        savedInterview = Interview.builder()
                .userId(user.getId())
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        em.persist(savedInterview);
        em.flush();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 블록 1: ElementCollection PK
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블록 1: interview_interview_types PK 중복 삽입 거부")
    class ElementCollectionPk {

        @Test
        @DisplayName("동일 (interview_id, interview_type) 쌍 중복 삽입 시 DataIntegrityViolationException 발생")
        void duplicateInterviewType_rejected() {
            Long id = savedInterview.getId();
            // @BeforeEach already persists CS_FUNDAMENTAL — direct duplicate insert triggers PK constraint
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO interview_interview_types (interview_id, interview_type) VALUES (?, ?)"
                ).setParameter(1, id).setParameter(2, "CS_FUNDAMENTAL").executeUpdate();
                em.flush();
            }).isInstanceOf(ConstraintViolationException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 블록 2: question_pool.cache_key UNIQUE → V43으로 분리
    // Why: 기존 question 70건이 non-MAX id 행 참조 중이라 단순 dedup 불가.
    //      참조 정리 전략 확정 후 별도 마이그레이션에서 처리.
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // 블록 3: ON DELETE CASCADE
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블록 3: interview 삭제 시 하위 레코드 CASCADE 삭제")
    class CascadeDelete {

        @Test
        @DisplayName("interview 삭제 시 question_set, question, question_set_feedback, timestamp_feedback 일괄 삭제")
        void deleteInterview_cascadesAllChildren() {
            // given: 전체 계층 구성
            QuestionSet questionSet = QuestionSet.builder()
                    .interview(savedInterview)
                    .category(InterviewType.CS_FUNDAMENTAL)
                    .orderIndex(0)
                    .build();
            em.persist(questionSet);

            Question question = Question.builder()
                    .questionType(QuestionType.TECH_MAIN)
                    .questionText("Spring IoC에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();
            questionSet.addQuestion(question);
            em.persist(question);

            QuestionSetFeedback qsFeedback = QuestionSetFeedback.builder()
                    .questionSet(questionSet)
                    .questionSetComment("전반적으로 양호합니다.")
                    .build();
            em.persist(qsFeedback);

            TimestampFeedback tsFeedback = TimestampFeedback.builder()
                    .startMs(0L)
                    .endMs(5000L)
                    .isAnalyzed(true)
                    .build();
            qsFeedback.addTimestampFeedback(tsFeedback);
            em.persist(tsFeedback);

            em.flush();
            em.clear();

            Long interviewId = savedInterview.getId();
            Long questionSetId = questionSet.getId();
            Long questionId = question.getId();
            Long qsFeedbackId = qsFeedback.getId();
            Long tsFeedbackId = tsFeedback.getId();

            // when
            em.createNativeQuery("DELETE FROM interview WHERE id = ?")
                    .setParameter(1, interviewId)
                    .executeUpdate();
            em.flush();
            em.clear();

            // then: 모든 자식 레코드 삭제 확인
            assertThat(countById("question_set", questionSetId)).isZero();
            assertThat(countById("question", questionId)).isZero();
            assertThat(countById("question_set_feedback", qsFeedbackId)).isZero();
            assertThat(countById("timestamp_feedback", tsFeedbackId)).isZero();
        }

        private long countById(String table, Long id) {
            return ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE id = ?"
            ).setParameter(1, id).getSingleResult()).longValue();
        }
    }

}
