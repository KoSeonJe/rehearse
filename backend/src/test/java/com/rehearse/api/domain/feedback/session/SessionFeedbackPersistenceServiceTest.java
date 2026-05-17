package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler;
import com.rehearse.api.domain.feedback.session.vo.DeliverySection;
import com.rehearse.api.domain.feedback.session.vo.GapItem;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import com.rehearse.api.domain.feedback.session.vo.StrengthItem;
import com.rehearse.api.domain.feedback.session.vo.WeekPlanItem;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackPersistenceServiceTest {

    @Mock private SessionFeedbackRepository sessionFeedbackRepository;
    @Mock private SessionFeedbackInputAssembler inputAssembler;

    private SessionFeedbackPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new SessionFeedbackPersistenceService(
                sessionFeedbackRepository, inputAssembler, new ObjectMapper());
    }

    @Nested
    @DisplayName("persistEnriched")
    class PersistEnriched {

        @Test
        @DisplayName("row 가 없으면 5섹션 전부 + COMPLETE 상태로 신규 INSERT 한다")
        void inserts_complete_row_when_absent() {
            given(sessionFeedbackRepository.findByInterviewId(1L)).willReturn(Optional.empty());

            service.persistEnriched(1L, samplePayload(), "FULL");

            ArgumentCaptor<SessionFeedback> captor = ArgumentCaptor.forClass(SessionFeedback.class);
            then(sessionFeedbackRepository).should(times(1)).save(captor.capture());
            SessionFeedback saved = captor.getValue();
            assertThat(saved.getInterviewId()).isEqualTo(1L);
            assertThat(saved.getStatus()).isEqualTo(SessionFeedbackStatus.COMPLETE);
            assertThat(saved.getOverallJson()).isNotNull();
            assertThat(saved.getStrengthsJson()).isNotNull();
            assertThat(saved.getGapsJson()).isNotNull();
            assertThat(saved.getDeliveryJson()).isNotNull();
            assertThat(saved.getWeekPlanJson()).isNotNull();
            assertThat(saved.getCoverage()).isEqualTo("FULL");
        }

        @Test
        @DisplayName("row 가 있으면 5섹션 전체를 갱신하고 COMPLETE 로 전환한다")
        void updates_all_sections_when_row_exists() {
            SessionFeedback existing = SessionFeedback.builder()
                    .interviewId(2L)
                    .overallJson("{\"old\":true}")
                    .strengthsJson(null)
                    .gapsJson(null)
                    .deliveryJson(null)
                    .weekPlanJson(null)
                    .coverage("PRELIMINARY")
                    .build();
            given(sessionFeedbackRepository.findByInterviewId(2L)).willReturn(Optional.of(existing));

            service.persistEnriched(2L, samplePayload(), "FULL");

            assertThat(existing.getStatus()).isEqualTo(SessionFeedbackStatus.COMPLETE);
            assertThat(existing.getOverallJson()).doesNotContain("old");
            assertThat(existing.getStrengthsJson()).isNotNull();
            assertThat(existing.getGapsJson()).isNotNull();
            assertThat(existing.getDeliveryJson()).isNotNull();
            assertThat(existing.getWeekPlanJson()).isNotNull();
            assertThat(existing.getCoverage()).isEqualTo("FULL");
            then(sessionFeedbackRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("SessionFeedback.applyEnrichedPayload")
    class ApplyEnrichedPayload {

        @Test
        @DisplayName("5섹션 모두 채우고 COMPLETE 전환하며 실패 사유를 비운다")
        void fills_all_sections_and_clears_failure() {
            SessionFeedback feedback = SessionFeedback.builder()
                    .interviewId(3L)
                    .overallJson(null).strengthsJson(null).gapsJson(null)
                    .deliveryJson(null).weekPlanJson(null).coverage(null)
                    .build();
            feedback.recordFailure("TIMEOUT", true);

            feedback.applyEnrichedPayload("{\"o\":1}", "[]", "[]", "{\"d\":1}", "[]");

            assertThat(feedback.getOverallJson()).isEqualTo("{\"o\":1}");
            assertThat(feedback.getStrengthsJson()).isEqualTo("[]");
            assertThat(feedback.getGapsJson()).isEqualTo("[]");
            assertThat(feedback.getDeliveryJson()).isEqualTo("{\"d\":1}");
            assertThat(feedback.getWeekPlanJson()).isEqualTo("[]");
            assertThat(feedback.getStatus()).isEqualTo(SessionFeedbackStatus.COMPLETE);
            assertThat(feedback.getLastFailureReason()).isNull();
            assertThat(feedback.isDeliveryRetryable()).isFalse();
        }
    }

    private GeneratedSessionFeedback samplePayload() {
        return new GeneratedSessionFeedback(
                new OverallSection(Map.of("clarity", 4.0), "MID", "전반 코멘트", "FULL"),
                List.of(new StrengthItem("clarity", "강점 관찰", "왜 중요한가")),
                List.of(new GapItem("structure", "개선 관찰", "L3→L4", "구체 액션")),
                new DeliverySection("음...", "차분함", "필러 줄이기"),
                List.of(new WeekPlanItem(1, "주차1 토픽", List.of("리소스1"), "실습"))
        );
    }
}
