package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.models.service.SessionFeedbackSynthesizer;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("SessionFeedback enrichment 은 readOnly 트랜잭션을 상속하지 않고 COMPLETE 로 영속화한다")
class SessionFeedbackPersistTxIntegrationTest extends ServiceIntegrationSupport {

    @Autowired private SessionFeedbackService sessionFeedbackService;
    @Autowired private UserRepository userRepository;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private SessionFeedbackRepository sessionFeedbackRepository;

    @MockitoBean private SessionFeedbackSynthesizer synthesizer;

    private static GeneratedSessionFeedback generatedFeedback() {
        return new GeneratedSessionFeedback(
                new OverallSection("L2", "총평", "all turns scored"),
                List.of(),
                List.of(),
                null,
                List.of()
        );
    }

    @Nested
    @DisplayName("enrichDeliveryFromScores")
    class EnrichDeliveryFromScores {

        @Test
        @DisplayName("PRELIMINARY 부재 시 신규 row 를 생성하고 Connection is read-only 없이 COMPLETE 로 저장한다")
        void completesWithoutReadOnlyConnectionError() {
            Long interviewId = persistInterview();
            given(synthesizer.synthesize(any())).willReturn(generatedFeedback());

            assertThatCode(() -> sessionFeedbackService.enrichDeliveryFromScores(interviewId))
                    .doesNotThrowAnyException();

            Optional<SessionFeedback> saved = sessionFeedbackRepository.findByInterviewId(interviewId);
            assertThat(saved).isPresent();
            assertThat(saved.get().getStatus()).isEqualTo(SessionFeedbackStatus.COMPLETE);
        }
    }

    @Nested
    @DisplayName("synthesizePreliminary")
    class SynthesizePreliminary {

        @Test
        @DisplayName("readOnly 트랜잭션 상속 없이 PRELIMINARY row 를 저장한다")
        void persistsPreliminaryWithoutReadOnlyConnectionError() {
            Long interviewId = persistInterview();
            given(synthesizer.synthesize(any())).willReturn(generatedFeedback());

            assertThatCode(() -> sessionFeedbackService.synthesizePreliminary(interviewId))
                    .doesNotThrowAnyException();

            Optional<SessionFeedback> saved = sessionFeedbackRepository.findByInterviewId(interviewId);
            assertThat(saved).isPresent();
            assertThat(saved.get().getStatus()).isEqualTo(SessionFeedbackStatus.PRELIMINARY);
        }
    }

    private Long persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("session-feedback-tx@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-session-feedback-tx")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(user.getId(), Position.BACKEND);
        return interviewRepository.saveAndFlush(interview).getId();
    }
}
