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

    // ====== checkNeedsFeedback 경계값 테스트 ======

    @Test
    @DisplayName("완료된 면접이 0개이면 피드백 불필요")
    void checkNeedsFeedback_completedCount0_returnsFalse() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(0L);

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isFalse();
    }

    @Test
    @DisplayName("완료된 면접이 2개이면 피드백 불필요")
    void checkNeedsFeedback_completedCount2_returnsFalse() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(2L);

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isFalse();
    }

    @Test
    @DisplayName("완료된 면접이 3개이고 이전 피드백 없으면 피드백 필요")
    void checkNeedsFeedback_completedCount3_noLastFeedback_returnsTrue() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(3L);
        given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isTrue();
    }

    @Test
    @DisplayName("완료된 면접이 3개이고 마지막 피드백 snapshot이 3이면 피드백 불필요")
    void checkNeedsFeedback_completedCount3_lastSnapshotAt3_returnsFalse() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(3L);
        given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(Optional.of(createFeedbackWithSnapshot(3)));

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isFalse();
    }

    @Test
    @DisplayName("완료된 면접이 5개이고 마지막 피드백 snapshot이 3이면 피드백 불필요")
    void checkNeedsFeedback_completedCount5_lastSnapshotAt3_returnsFalse() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(5L);
        given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(Optional.of(createFeedbackWithSnapshot(3)));

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isFalse();
    }

    @Test
    @DisplayName("완료된 면접이 6개이고 마지막 피드백 snapshot이 3이면 피드백 필요")
    void checkNeedsFeedback_completedCount6_lastSnapshotAt3_returnsTrue() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(6L);
        given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(Optional.of(createFeedbackWithSnapshot(3)));

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isTrue();
    }

    @Test
    @DisplayName("완료된 면접이 9개이고 마지막 피드백 snapshot이 6이면 피드백 필요")
    void checkNeedsFeedback_completedCount9_lastSnapshotAt6_returnsTrue() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(9L);
        given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(Optional.of(createFeedbackWithSnapshot(6)));

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isTrue();
    }

    @Test
    @DisplayName("완료된 면접이 9개이고 마지막 피드백 snapshot이 9이면 피드백 불필요")
    void checkNeedsFeedback_completedCount9_lastSnapshotAt9_returnsFalse() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(9L);
        given(serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(Optional.of(createFeedbackWithSnapshot(9)));

        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(1L);

        assertThat(response.needsFeedback()).isFalse();
    }

    // ====== submitFeedback 테스트 ======

    @Test
    @DisplayName("피드백 제출 시 repository.save가 호출된다")
    void submitFeedback_callsRepositorySave() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(5L);
        CreateServiceFeedbackRequest request = new CreateServiceFeedbackRequest(
                "서비스가 정말 도움이 많이 되었습니다.", 5, FeedbackSource.AUTO_POPUP);

        serviceFeedbackService.submitFeedback(1L, request);

        then(serviceFeedbackRepository).should().save(any(ServiceFeedback.class));
    }

    @Test
    @DisplayName("피드백 제출 시 completedCountSnapshot이 현재 completedCount와 일치한다")
    void submitFeedback_savedFeedbackHasCorrectCompletedCountSnapshot() {
        given(interviewRepository.countByUserIdAndStatus(1L, InterviewStatus.COMPLETED)).willReturn(6L);
        CreateServiceFeedbackRequest request = new CreateServiceFeedbackRequest(
                "서비스가 정말 도움이 많이 되었습니다.", 4, FeedbackSource.VOLUNTARY);

        ArgumentCaptor<ServiceFeedback> captor = ArgumentCaptor.forClass(ServiceFeedback.class);
        serviceFeedbackService.submitFeedback(1L, request);

        then(serviceFeedbackRepository).should().save(captor.capture());
        assertThat(captor.getValue().getCompletedCountSnapshot()).isEqualTo(6);
    }

    // ====== getAdminFeedbacks 테스트 ======

    @Test
    @DisplayName("관리자 피드백 조회 시 Page<ServiceFeedback>이 AdminFeedbackResponse로 변환된다")
    void getAdminFeedbacks_convertsToAdminFeedbackResponse() {
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

        Page<AdminFeedbackResponse> result = serviceFeedbackService.getAdminFeedbacks(PageRequest.of(0, 20));

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
        ServiceFeedback feedback = createFeedbackWithSnapshot(3);
        ReflectionTestUtils.setField(feedback, "id", 20L);
        ReflectionTestUtils.setField(feedback, "userId", 999L);
        ReflectionTestUtils.setField(feedback, "content", "피드백 내용입니다. 충분히 길게 작성합니다.");
        ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.of(2026, 4, 9, 12, 0, 0));

        Page<ServiceFeedback> feedbackPage = new PageImpl<>(List.of(feedback), PageRequest.of(0, 20), 1);
        given(serviceFeedbackRepository.findAllByOrderByCreatedAtDesc(any())).willReturn(feedbackPage);
        given(userRepository.findAllById(List.of(999L))).willReturn(List.of());

        Page<AdminFeedbackResponse> result = serviceFeedbackService.getAdminFeedbacks(PageRequest.of(0, 20));

        AdminFeedbackResponse item = result.getContent().get(0);
        assertThat(item.userName()).isEqualTo("탈퇴한 사용자");
        assertThat(item.userEmail()).isEqualTo("");
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
