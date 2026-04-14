package com.rehearse.api.domain.servicefeedback.service;

import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.servicefeedback.dto.AdminFeedbackResponse;
import com.rehearse.api.domain.servicefeedback.dto.CreateServiceFeedbackRequest;
import com.rehearse.api.domain.servicefeedback.dto.FeedbackNeedCheckResponse;
import com.rehearse.api.domain.servicefeedback.entity.FeedbackSource;
import com.rehearse.api.domain.servicefeedback.entity.ServiceFeedback;
import com.rehearse.api.domain.servicefeedback.repository.ServiceFeedbackRepository;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ServiceFeedbackServiceTest {

    @InjectMocks
    private ServiceFeedbackService serviceFeedbackService;

    @Mock
    private ServiceFeedbackRepository serviceFeedbackRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("checkNeedsFeedback 메서드")
    class CheckNeedsFeedback {

        @Test
        @DisplayName("완료된 면접이 0개이면 피드백 불필요")
        void checkNeedsFeedback_completedCount0_returnsFalse() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(0L);

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isFalse();
        }

        @Test
        @DisplayName("완료된 면접이 1개이고 이전 피드백 없으면 피드백 필요")
        void checkNeedsFeedback_completedCount1_noLastFeedback_returnsTrue() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(1L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isTrue();
        }

        @Test
        @DisplayName("완료된 면접이 1개이고 마지막 피드백 snapshot이 1이면 피드백 불필요")
        void checkNeedsFeedback_completedCount1_lastSnapshotAt1_returnsFalse() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(1L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(Optional.of(createFeedbackWithSnapshot(1)));

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isFalse();
        }

        @Test
        @DisplayName("완료된 면접이 3개이고 마지막 피드백 snapshot이 1이면 피드백 불필요")
        void checkNeedsFeedback_completedCount3_lastSnapshotAt1_returnsFalse() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(3L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(Optional.of(createFeedbackWithSnapshot(1)));

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isFalse();
        }

        @Test
        @DisplayName("완료된 면접이 4개이고 마지막 피드백 snapshot이 1이면 피드백 필요")
        void checkNeedsFeedback_completedCount4_lastSnapshotAt1_returnsTrue() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(4L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(Optional.of(createFeedbackWithSnapshot(1)));

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isTrue();
        }

        @Test
        @DisplayName("완료된 면접이 7개이고 마지막 피드백 snapshot이 4이면 피드백 필요")
        void checkNeedsFeedback_completedCount7_lastSnapshotAt4_returnsTrue() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(7L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(Optional.of(createFeedbackWithSnapshot(4)));

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isTrue();
        }

        @Test
        @DisplayName("완료된 면접이 10개이고 마지막 피드백 snapshot이 7이면 피드백 필요")
        void checkNeedsFeedback_completedCount10_lastSnapshotAt7_returnsTrue() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(10L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(Optional.of(createFeedbackWithSnapshot(7)));

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isTrue();
        }

        @Test
        @DisplayName("완료된 면접이 10개이고 마지막 피드백 snapshot이 10이면 피드백 불필요")
        void checkNeedsFeedback_completedCount10_lastSnapshotAt10_returnsFalse() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(10L);
            given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                    .willReturn(Optional.of(createFeedbackWithSnapshot(10)));

            // when
            FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

            // then
            assertThat(response.needsFeedback()).isFalse();
        }
    }

    @Nested
    @DisplayName("submitFeedback 메서드")
    class SubmitFeedback {

        @Test
        @DisplayName("피드백 제출 시 repository.save가 호출된다")
        void submitFeedback_callsRepositorySave() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(5L);
            CreateServiceFeedbackRequest request = new CreateServiceFeedbackRequest(
                    "서비스가 정말 도움이 많이 되었습니다.", 5, FeedbackSource.AUTO_POPUP);

            // when
            serviceFeedbackService.submitFeedback(1L, request);

            // then
            then(serviceFeedbackRepository).should().save(any(ServiceFeedback.class));
        }

        @Test
        @DisplayName("피드백 제출 시 completedCountSnapshot이 현재 completedCount와 일치한다")
        void submitFeedback_savedFeedbackHasCorrectCompletedCountSnapshot() {
            // given
            given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(6L);
            CreateServiceFeedbackRequest request = new CreateServiceFeedbackRequest(
                    "서비스가 정말 도움이 많이 되었습니다.", 4, FeedbackSource.VOLUNTARY);

            ArgumentCaptor<ServiceFeedback> captor = ArgumentCaptor.forClass(ServiceFeedback.class);

            // when
            serviceFeedbackService.submitFeedback(1L, request);

            // then
            then(serviceFeedbackRepository).should().save(captor.capture());
            assertThat(captor.getValue().getCompletedCountSnapshot()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("getAdminFeedbacks 메서드")
    class GetAdminFeedbacks {

        @Test
        @DisplayName("관리자 피드백 조회 시 Page<ServiceFeedback>이 AdminFeedbackResponse로 변환된다")
        void getAdminFeedbacks_convertsToAdminFeedbackResponse() {
            // given
            ServiceFeedback feedback = createFeedbackWithSnapshot(3);
            ReflectionTestUtils.setField(feedback, "id", 10L);
            ReflectionTestUtils.setField(feedback, "userId", 1L);
            ReflectionTestUtils.setField(feedback, "content", "좋은 서비스입니다. 계속 이용하겠습니다.");
            ReflectionTestUtils.setField(feedback, "rating", 5);
            ReflectionTestUtils.setField(feedback, "source", FeedbackSource.AUTO_POPUP);
            ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.of(2026, 4, 9, 12, 0, 0));

            Page<ServiceFeedback> feedbackPage = new PageImpl<>(List.of(feedback), PageRequest.of(0, 20), 1);
            given(serviceFeedbackRepository.findAllByOrderByCreatedAtDesc(any())).willReturn(feedbackPage);

            User user = createUser(1L, "홍길동", "hong@test.com");
            given(userRepository.findAllById(List.of(1L))).willReturn(List.of(user));

            // when
            Page<AdminFeedbackResponse> result = serviceFeedbackService.getAdminFeedbacks(PageRequest.of(0, 20));

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            AdminFeedbackResponse item = result.getContent().get(0);
            assertThat(item.id()).isEqualTo(10L);
            assertThat(item.userId()).isEqualTo(1L);
            assertThat(item.userName()).isEqualTo("홍길동");
            assertThat(item.userEmail()).isEqualTo("hong@test.com");
            assertThat(item.content()).isEqualTo("좋은 서비스입니다. 계속 이용하겠습니다.");
            assertThat(item.rating()).isEqualTo(5);
            assertThat(item.completedCountSnapshot()).isEqualTo(3);
        }

        @Test
        @DisplayName("탈퇴한 사용자의 피드백은 userName을 '탈퇴한 사용자'로 변환한다")
        void getAdminFeedbacks_deletedUser_showsWithdrawMessage() {
            // given
            ServiceFeedback feedback = createFeedbackWithSnapshot(3);
            ReflectionTestUtils.setField(feedback, "id", 20L);
            ReflectionTestUtils.setField(feedback, "userId", 999L);
            ReflectionTestUtils.setField(feedback, "content", "피드백 내용입니다. 충분히 길게 작성합니다.");
            ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.of(2026, 4, 9, 12, 0, 0));

            Page<ServiceFeedback> feedbackPage = new PageImpl<>(List.of(feedback), PageRequest.of(0, 20), 1);
            given(serviceFeedbackRepository.findAllByOrderByCreatedAtDesc(any())).willReturn(feedbackPage);
            given(userRepository.findAllById(List.of(999L))).willReturn(List.of());

            // when
            Page<AdminFeedbackResponse> result = serviceFeedbackService.getAdminFeedbacks(PageRequest.of(0, 20));

            // then
            AdminFeedbackResponse item = result.getContent().get(0);
            assertThat(item.userName()).isEqualTo("탈퇴한 사용자");
            assertThat(item.userEmail()).isEqualTo("");
        }
    }

    // ====== helpers ======

    private ServiceFeedback createFeedbackWithSnapshot(int snapshot) {
        ServiceFeedback feedback = ServiceFeedback.create(
                1L, "피드백 내용입니다.", null, FeedbackSource.AUTO_POPUP, snapshot);
        return feedback;
    }

    private User createUser(Long id, String name, String email) {
        User user = User.builder()
                .name(name)
                .email(email)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
