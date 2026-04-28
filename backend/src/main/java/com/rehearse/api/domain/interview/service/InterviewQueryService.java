package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.InterviewListResponse;
import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.dto.InterviewStatsResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewQueryService {

    private final InterviewFinder interviewFinder;
    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;

    public InterviewResponse getInterview(Long id, Long userId) {
        Interview interview = interviewFinder.findById(id);
        interview.validateOwner(userId);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(id);
        return InterviewResponse.from(interview, questionSets);
    }

    public InterviewResponse getInterviewByPublicId(String publicId, Long userId) {
        Interview interview = interviewFinder.findByPublicId(publicId);
        interview.validateOwner(userId);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(interview.getId());
        return InterviewResponse.from(interview, questionSets);
    }

    public Page<InterviewListResponse> getInterviews(Long userId, Pageable pageable) {
        Page<Interview> interviews = interviewRepository.findAllByUserId(userId, pageable);

        List<Long> interviewIds = interviews.getContent().stream()
                .map(Interview::getId)
                .toList();

        Map<Long, Long> answerCountMap = buildAnswerCountMap(interviewIds);

        return interviews.map(interview -> toListResponse(interview, answerCountMap));
    }

    public InterviewStatsResponse getStats(Long userId) {
        long totalCount = interviewRepository.countByUserId(userId);
        long completedCount = interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.COMPLETED);

        LocalDateTime weekStart = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();
        long thisWeekCount = interviewRepository.countByUserIdAndCreatedAtAfter(userId, weekStart);

        return InterviewStatsResponse.builder()
                .totalCount(totalCount)
                .completedCount(completedCount)
                .thisWeekCount(thisWeekCount)
                .build();
    }

    private Map<Long, Long> buildAnswerCountMap(List<Long> interviewIds) {
        if (interviewIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = interviewRepository.countAnswersByInterviewIds(interviewIds);
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private InterviewListResponse toListResponse(Interview interview, Map<Long, Long> answerCountMap) {
        return InterviewListResponse.builder()
                .id(interview.getId())
                .publicId(interview.getPublicId())
                .position(interview.getPosition())
                .positionDetail(interview.getPositionDetail())
                .interviewTypes(new ArrayList<>(interview.getInterviewTypes()))
                .csSubTopics(new ArrayList<>(interview.getCsSubTopics()))
                .durationMinutes(interview.getDurationMinutes())
                .status(interview.getStatus())
                .answerCount(answerCountMap.getOrDefault(interview.getId(), 0L))
                .createdAt(interview.getCreatedAt())
                .build();
    }
}
