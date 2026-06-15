package com.rehearse.api.domain.feedback.repository;

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
@DisplayName("타임스탬프 피드백 테이블 마이그레이션 (V54 롤백)")
class TimestampFeedbackRepositoryTest extends AbstractMySqlContainerTest {

    // V54 가 코멘트형 응답을 위해 재추가한 14개 컬럼
    private static final List<String> RESTORED_COLUMNS = List.of(
            "verbal_comment",
            "accuracy_issues",
            "coaching_structure",
            "coaching_improvement",
            "nonverbal_comment",
            "overall_comment",
            "vocal_comment",
            "attitude_comment",
            "speech_pace",
            "tone_confidence_level",
            "emotion_label",
            "eye_contact_level",
            "posture_level",
            "expression_label"
    );

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("V54 마이그레이션 후 코멘트형 14개 컬럼이 모두 재추가된다")
    void flyway_v54_restores_14_columns() {
        Number restoredCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_SCHEMA = DATABASE() "
                                + "AND TABLE_NAME = 'timestamp_feedback' "
                                + "AND COLUMN_NAME IN (:cols)")
                .setParameter("cols", RESTORED_COLUMNS)
                .getSingleResult();

        assertThat(restoredCount.longValue()).isEqualTo(RESTORED_COLUMNS.size());
    }

    @Test
    @DisplayName("V54 마이그레이션 후 루브릭 점수 테이블(question_score*)이 제거된다")
    void flyway_v54_drops_question_score_tables() {
        Number tableCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                                + "WHERE TABLE_SCHEMA = DATABASE() "
                                + "AND TABLE_NAME IN ('question_score', 'question_score_dimension')")
                .getSingleResult();

        assertThat(tableCount.longValue()).isZero();
    }
}
