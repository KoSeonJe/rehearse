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
@DisplayName("타임스탬프 피드백 테이블 마이그레이션")
class TimestampFeedbackRepositoryTest extends AbstractMySqlContainerTest {

    private static final List<String> DROPPED_COLUMNS = List.of(
            "nonverbal_comment",
            "vocal_comment",
            "attitude_comment",
            "overall_comment",
            "eye_contact_level",
            "posture_level",
            "expression_label",
            "speech_pace",
            "tone_confidence_level",
            "emotion_label"
    );

    private static final List<String> RETAINED_COLUMNS = List.of(
            "filler_word_count",
            "filler_words",
            "transcript"
    );

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("V48 마이그레이션 후 자유서술 4 + 원시 6 = 10개 컬럼이 모두 제거된다")
    void flyway_v48_drops_10_columns() {
        Number droppedCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_SCHEMA = DATABASE() "
                                + "AND TABLE_NAME = 'timestamp_feedback' "
                                + "AND COLUMN_NAME IN (:cols)")
                .setParameter("cols", DROPPED_COLUMNS)
                .getSingleResult();

        assertThat(droppedCount.longValue()).isZero();
    }

    @Test
    @DisplayName("filler 컬럼 2종과 transcript 컬럼은 V48 이후에도 유지된다")
    void filler_and_transcript_columns_remain() {
        Number retainedCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                + "WHERE TABLE_SCHEMA = DATABASE() "
                                + "AND TABLE_NAME = 'timestamp_feedback' "
                                + "AND COLUMN_NAME IN (:cols)")
                .setParameter("cols", RETAINED_COLUMNS)
                .getSingleResult();

        assertThat(retainedCount.longValue()).isEqualTo(RETAINED_COLUMNS.size());
    }
}
