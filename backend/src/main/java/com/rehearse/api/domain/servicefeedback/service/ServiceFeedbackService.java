package com.rehearse.api.domain.servicefeedback.service;

import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.servicefeedback.dto.AdminFeedbackResponse;
import com.rehearse.api.domain.servicefeedback.dto.CreateServiceFeedbackRequest;
import com.rehearse.api.domain.servicefeedback.dto.FeedbackNeedCheckResponse;
import com.rehearse.api.domain.servicefeedback.entity.ServiceFeedback;
import com.rehearse.api.domain.servicefeedback.repository.ServiceFeedbackRepository;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceFeedbackService {

    private final ServiceFeedbackRepository serviceFeedbackRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public FeedbackNeedCheckResponse checkNeedsFeedback(Long userId) {
        long completedCount = interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.COMPLETED);
        if (completedCount < 3) {
            return new FeedbackNeedCheckResponse(false);
        }

        Optional<ServiceFeedback> lastFeedback = serviceFeedbackRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        if (lastFeedback.isEmpty()) {
            return new FeedbackNeedCheckResponse(true);
        }

        long lastSnapshot = lastFeedback.get().getCompletedCountSnapshot();
        boolean needs = (completedCount / 3) > (lastSnapshot / 3);
        return new FeedbackNeedCheckResponse(needs);
    }

    @Transactional
    public void submitFeedback(Long userId, CreateServiceFeedbackRequest request) {
        long completedCount = interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.COMPLETED);
        ServiceFeedback feedback = ServiceFeedback.create(
                userId, request.content(), request.rating(), request.source(), completedCount);
        serviceFeedbackRepository.save(feedback);
    }

    public Page<AdminFeedbackResponse> getAdminFeedbacks(Pageable pageable) {
        Page<ServiceFeedback> feedbacks = serviceFeedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<Long> userIds = feedbacks.getContent().stream()
                .map(ServiceFeedback::getUserId)
                .distinct()
                .toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return feedbacks.map(f -> {
            User user = userMap.get(f.getUserId());
            return new AdminFeedbackResponse(
                    f.getId(),
                    f.getUserId(),
                    user != null ? user.getName() : "탈퇴한 사용자",
                    user != null ? user.getEmail() : "",
                    f.getContent(),
                    f.getRating(),
                    f.getSource(),
                    f.getCompletedCountSnapshot(),
                    f.getCreatedAt()
            );
        });
    }
}
