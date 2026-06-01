package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// PR1 중립화: 루브릭 점수 테이블(question_score*) 제거로 입력 소스가 사라짐.
// PR2(plan-548)에서 timestamp_feedback 코멘트 기반으로 재작성 예정. 현재는 빈 입력 반환.
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackInputAssembler {

    private final InterviewFinder interviewFinder;

    public SessionFeedbackInput assemble(Long interviewId) {
        return emptyInput(interviewId);
    }

    public SessionFeedbackInput assembleWithDelivery(Long interviewId, String deliveryAnalysis,
                                                     String visionAnalysis, String nonverbalAggregate) {
        return emptyInput(interviewId);
    }

    private SessionFeedbackInput emptyInput(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        return new SessionFeedbackInput(
                buildSessionMetadata(interview),
                List.of(),
                Map.of(),
                List.of(),
                null,
                null,
                null,
                null,
                "0/0 turns scored",
                interview.getLevel()
        );
    }

    private SessionFeedbackInput.SessionMetadata buildSessionMetadata(Interview interview) {
        return new SessionFeedbackInput.SessionMetadata(
                interview.getId(),
                interview.getPosition() != null ? interview.getPosition().name() : "UNKNOWN",
                interview.getLevel() != null ? interview.getLevel().name() : "MID",
                interview.getInterviewTypes().stream()
                        .map(Enum::name)
                        .toList(),
                0,
                interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 0
        );
    }
}
