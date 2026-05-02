package com.rehearse.api.domain.integrity;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
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
import org.hibernate.JDBCException;
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
    // 블록 2: question_pool cache_key UNIQUE
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블록 2: question_pool.cache_key UNIQUE 제약")
    class QuestionPoolCacheKeyUnique {

        @Test
        @DisplayName("동일 cache_key로 두 번 삽입하면 DataIntegrityViolationException 발생")
        void duplicateCacheKey_rejected() {
            QuestionPool first = QuestionPool.create("backend:cs:junior:q1", "질문 내용 A", null, null, null, null);
            em.persist(first);
            em.flush();

            assertThatThrownBy(() -> {
                QuestionPool duplicate = QuestionPool.create("backend:cs:junior:q1", "질문 내용 B", null, null, null, null);
                em.persist(duplicate);
                em.flush();
            }).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("서로 다른 cache_key는 정상 삽입된다")
        void distinctCacheKeys_persisted() {
            QuestionPool first = QuestionPool.create("key:unique:1", "질문 A", null, null, null, null);
            QuestionPool second = QuestionPool.create("key:unique:2", "질문 B", null, null, null, null);
            em.persist(first);
            em.persist(second);
            em.flush();

            assertThat(first.getId()).isNotNull();
            assertThat(second.getId()).isNotNull();
        }
    }

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
                    .category(QuestionSetCategory.CS_FUNDAMENTAL)
                    .orderIndex(0)
                    .build();
            em.persist(questionSet);

            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
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

    // ─────────────────────────────────────────────────────────────────────────
    // 블록 4: question CHECK 5-way
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블록 4: question CHECK 5-way 제약")
    class QuestionCheckConstraint {

        private QuestionSet questionSet;

        @BeforeEach
        void setUpQuestionSet() {
            questionSet = QuestionSet.builder()
                    .interview(savedInterview)
                    .category(QuestionSetCategory.CS_FUNDAMENTAL)
                    .orderIndex(0)
                    .build();
            em.persist(questionSet);
            em.flush();
        }

        @Test
        @DisplayName("RESUME_INTERROGATION + chain_id NULL 삽입 시 CHECK 위반으로 거부된다")
        void resumeInterrogation_withNullChainId_rejected() {
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO question (question_set_id, question_type, question_text, order_index, " +
                        "chain_id, chain_step_type, project_id) VALUES (?, 'RESUME_INTERROGATION', '질문', 0, NULL, 'INTERROGATION', 'proj-1')"
                ).setParameter(1, questionSet.getId()).executeUpdate();
                em.flush();
            }).isInstanceOf(JDBCException.class);
        }

        @Test
        @DisplayName("RESUME_INTERROGATION + 필수 필드 모두 존재 시 정상 삽입된다")
        void resumeInterrogation_withAllFields_accepted() {
            em.createNativeQuery(
                    "INSERT INTO question (question_set_id, question_type, question_text, order_index, " +
                    "chain_id, chain_step_type, project_id) VALUES (?, 'RESUME_INTERROGATION', '질문', 0, 'chain-uuid', 'INTERROGATION', 'proj-1')"
            ).setParameter(1, questionSet.getId()).executeUpdate();
            em.flush();

            long count = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM question WHERE question_set_id = ? AND question_type = 'RESUME_INTERROGATION'"
            ).setParameter(1, questionSet.getId()).getSingleResult()).longValue();
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("MAIN 타입에 chain_id 존재 시 CHECK 위반으로 거부된다")
        void main_withChainId_rejected() {
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO question (question_set_id, question_type, question_text, order_index, " +
                        "chain_id, chain_step_type, project_id) VALUES (?, 'MAIN', '질문', 0, 'chain-uuid', NULL, NULL)"
                ).setParameter(1, questionSet.getId()).executeUpdate();
                em.flush();
            }).isInstanceOf(JDBCException.class);
        }

        @Test
        @DisplayName("RESUME_OPENER + project_id 존재 시 정상 삽입된다")
        void resumeOpener_withProjectId_accepted() {
            em.createNativeQuery(
                    "INSERT INTO question (question_set_id, question_type, question_text, order_index, " +
                    "chain_id, chain_step_type, project_id) VALUES (?, 'RESUME_OPENER', '질문', 0, NULL, NULL, 'proj-1')"
            ).setParameter(1, questionSet.getId()).executeUpdate();
            em.flush();

            long count = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM question WHERE question_set_id = ? AND question_type = 'RESUME_OPENER'"
            ).setParameter(1, questionSet.getId()).getSingleResult()).longValue();
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("RESUME_OPENER + project_id NULL 삽입 시 CHECK 위반으로 거부된다")
        void resumeOpener_withNullProjectId_rejected() {
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO question (question_set_id, question_type, question_text, order_index, " +
                        "chain_id, chain_step_type, project_id) VALUES (?, 'RESUME_OPENER', '질문', 0, NULL, NULL, NULL)"
                ).setParameter(1, questionSet.getId()).executeUpdate();
                em.flush();
            }).isInstanceOf(JDBCException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 블록 5: timestamp_feedback level CHECK
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블록 5: timestamp_feedback level 컬럼 CHECK 제약")
    class TimestampFeedbackLevelCheck {

        private Long qsFeedbackId;

        @BeforeEach
        void setUpFeedback() {
            QuestionSet questionSet = QuestionSet.builder()
                    .interview(savedInterview)
                    .category(QuestionSetCategory.CS_FUNDAMENTAL)
                    .orderIndex(0)
                    .build();
            em.persist(questionSet);

            QuestionSetFeedback qsFeedback = QuestionSetFeedback.builder()
                    .questionSet(questionSet)
                    .questionSetComment("코멘트")
                    .build();
            em.persist(qsFeedback);
            em.flush();
            qsFeedbackId = qsFeedback.getId();
        }

        @Test
        @DisplayName("eye_contact_level에 허용되지 않는 값 삽입 시 CHECK 위반으로 거부된다")
        void invalidEyeContactLevel_rejected() {
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO timestamp_feedback (question_set_feedback_id, start_ms, end_ms, is_analyzed, eye_contact_level) " +
                        "VALUES (?, 0, 5000, false, 'INVALID_VALUE')"
                ).setParameter(1, qsFeedbackId).executeUpdate();
                em.flush();
            }).isInstanceOf(JDBCException.class);
        }

        @Test
        @DisplayName("posture_level에 허용되지 않는 값 삽입 시 CHECK 위반으로 거부된다")
        void invalidPostureLevel_rejected() {
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO timestamp_feedback (question_set_feedback_id, start_ms, end_ms, is_analyzed, posture_level) " +
                        "VALUES (?, 0, 5000, false, 'BAD')"
                ).setParameter(1, qsFeedbackId).executeUpdate();
                em.flush();
            }).isInstanceOf(JDBCException.class);
        }

        @Test
        @DisplayName("tone_confidence_level에 허용되지 않는 값 삽입 시 CHECK 위반으로 거부된다")
        void invalidToneConfidenceLevel_rejected() {
            assertThatThrownBy(() -> {
                em.createNativeQuery(
                        "INSERT INTO timestamp_feedback (question_set_feedback_id, start_ms, end_ms, is_analyzed, tone_confidence_level) " +
                        "VALUES (?, 0, 5000, false, 'EXCELLENT')"
                ).setParameter(1, qsFeedbackId).executeUpdate();
                em.flush();
            }).isInstanceOf(JDBCException.class);
        }

        @Test
        @DisplayName("허용 값(GOOD, AVERAGE, NEEDS_IMPROVEMENT)으로 삽입 시 정상 처리된다")
        void validLevelValues_accepted() {
            em.createNativeQuery(
                    "INSERT INTO timestamp_feedback (question_set_feedback_id, start_ms, end_ms, is_analyzed, " +
                    "eye_contact_level, posture_level, tone_confidence_level) VALUES (?, 0, 5000, false, 'GOOD', 'AVERAGE', 'NEEDS_IMPROVEMENT')"
            ).setParameter(1, qsFeedbackId).executeUpdate();
            em.flush();

            long count = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM timestamp_feedback WHERE question_set_feedback_id = ?"
            ).setParameter(1, qsFeedbackId).getSingleResult()).longValue();
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("level 컬럼이 NULL이면 CHECK를 통과한다")
        void nullLevelValues_accepted() {
            em.createNativeQuery(
                    "INSERT INTO timestamp_feedback (question_set_feedback_id, start_ms, end_ms, is_analyzed) " +
                    "VALUES (?, 1000, 6000, false)"
            ).setParameter(1, qsFeedbackId).executeUpdate();
            em.flush();

            long count = ((Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM timestamp_feedback WHERE question_set_feedback_id = ?"
            ).setParameter(1, qsFeedbackId).getSingleResult()).longValue();
            assertThat(count).isEqualTo(1);
        }
    }
}
